/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2020-2020 Equinix, Inc
 * Copyright 2014-2020 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.notification.setup;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.commons.mail.EmailException;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.account.api.AccountEmail;
import org.killbill.billing.catalog.api.BillingActionPolicy;
import org.killbill.billing.entitlement.api.Entitlement;
import org.killbill.billing.entitlement.api.EntitlementSpecifier;
import org.killbill.billing.entitlement.api.Subscription;
import org.killbill.billing.entitlement.api.SubscriptionApiException;
import org.killbill.billing.entitlement.api.SubscriptionEventType;
import org.killbill.billing.invoice.api.DryRunArguments;
import org.killbill.billing.invoice.api.DryRunType;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceApiException;
import org.killbill.billing.invoice.api.InvoicePayment;
import org.killbill.billing.invoice.api.formatters.InvoiceFormatter;
import org.killbill.billing.notification.plugin.api.ExtBusEvent;
import org.killbill.billing.notification.plugin.api.ExtBusEventType;
import org.killbill.billing.notification.plugin.api.NotificationPluginApiRetryException;
import org.killbill.billing.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillClock;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillDataSource;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillEventDispatcher;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentApiException;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionStatus;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.plugin.notification.dao.ConfigurationDao;
import org.killbill.billing.plugin.notification.dao.gen.tables.pojos.EmailNotificationsConfiguration;
import org.killbill.billing.plugin.notification.email.EmailContent;
import org.killbill.billing.plugin.notification.email.EmailSender;
import org.killbill.billing.plugin.notification.exception.EmailNotificationException;
import org.killbill.billing.plugin.notification.generator.ResourceBundleFactory;
import org.killbill.billing.plugin.notification.generator.TemplateRenderer;
import org.killbill.billing.plugin.notification.templates.MustacheTemplateEngine;
import org.killbill.billing.tenant.api.TenantApiException;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.billing.plugin.notification.api.InvoiceFormatterFactory;
import org.osgi.util.tracker.ServiceTracker;
import org.skife.config.TimeSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.samskivert.mustache.MustacheException;

public class EmailNotificationListener implements OSGIKillbillEventDispatcher.OSGIKillbillEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationListener.class);

    private static final String INVOICE_DRY_RUN_TIME_PROPERTY = "org.killbill.invoice.dryRunNotificationSchedule";

    private static final NullDryRunArguments NULL_DRY_RUN_ARGUMENTS = new NullDryRunArguments();

    private final OSGIKillbillAPI osgiKillbillAPI;
    private final TemplateRenderer templateRenderer;
    private final OSGIConfigPropertiesService configProperties;
    private final EmailSender emailSender;
    private final OSGIKillbillClock clock;
    private final ConfigurationDao dao;
    private final EmailNotificationConfigurationHandler emailNotificationConfigurationHandler;

    public static final ImmutableList<ExtBusEventType> EVENTS_TO_CONSIDER = new ImmutableList.Builder()
            .add(ExtBusEventType.INVOICE_NOTIFICATION)
            .add(ExtBusEventType.INVOICE_CREATION)
            .add(ExtBusEventType.INVOICE_PAYMENT_SUCCESS)
            .add(ExtBusEventType.INVOICE_PAYMENT_FAILED)
            .add(ExtBusEventType.SUBSCRIPTION_CANCEL)
            .build();


    public EmailNotificationListener(final OSGIKillbillClock clock, final OSGIKillbillAPI killbillAPI, final OSGIConfigPropertiesService configProperties,
                                     OSGIKillbillDataSource dataSource, EmailNotificationConfigurationHandler emailNotificationConfigurationHandler,
                                     final ServiceTracker<InvoiceFormatterFactory, InvoiceFormatterFactory> invoiceFormatterTracker) throws SQLException {
        this.osgiKillbillAPI = killbillAPI;
        this.configProperties = configProperties;
        this.clock = clock;
        this.emailSender = new EmailSender(configProperties);
        this.templateRenderer = new TemplateRenderer(new MustacheTemplateEngine(), new ResourceBundleFactory(killbillAPI.getTenantUserApi()), killbillAPI.getTenantUserApi());
        this.templateRenderer.setInvoiceFormatterTracker(invoiceFormatterTracker);
        this.dao = new ConfigurationDao(dataSource.getDataSource());
        this.emailNotificationConfigurationHandler = emailNotificationConfigurationHandler;
    }

    @Override
    public void handleKillbillEvent(final ExtBusEvent killbillEvent) {

        if (!EVENTS_TO_CONSIDER.contains(killbillEvent.getEventType())) {
            return;
        }

        if(!isEventTypeAllowed(killbillEvent.getAccountId(),killbillEvent.getTenantId(),killbillEvent.getEventType()))
        {
            return;
        }

        // TODO see https://github.com/killbill/killbill-platform/issues/5
        final ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        try {
            final Account account = osgiKillbillAPI.getAccountUserApi().getAccountById(killbillEvent.getAccountId(), new EmailNotificationContext(killbillEvent.getAccountId(), killbillEvent.getTenantId()));
            final String to = account.getEmail();
            if (to == null) {
                logger.debug("Account {} does not have an email address configured, skip...", account.getId());
                return;
            }

            final EmailNotificationContext context = new EmailNotificationContext(killbillEvent.getAccountId(), killbillEvent.getTenantId());
            switch (killbillEvent.getEventType()) {
                case INVOICE_NOTIFICATION:
                    sendEmailForUpComingInvoice(account, killbillEvent, context);
                    break;

                case INVOICE_PAYMENT_SUCCESS:
                case INVOICE_PAYMENT_FAILED:
                    sendEmailForPayment(account, killbillEvent, context);
                    break;

                case SUBSCRIPTION_CANCEL:
                    sendEmailForCancelledSubscription(account, killbillEvent, context);
                    break;

                case INVOICE_CREATION:
                    sendEmailForInvoiceCreation(account, killbillEvent, context);
                    break;
                default:
                    break;
            }

            logger.debug("Received event {} for object type = {}, id = {}",
                         killbillEvent.getEventType(), killbillEvent.getObjectType(), killbillEvent.getObjectId());

        } catch (final EmailNotificationException e) {
            logger.warn(e.getMessage(), e);
        } catch (final AccountApiException e) {
            logger.warn("Unable to find account: {}", killbillEvent.getAccountId(), e);
        } catch (InvoiceApiException e) {
            logger.warn("Fail to retrieve invoice for account {}", killbillEvent.getAccountId(), e);
        } catch (SubscriptionApiException e) {
            logger.warn("Fail to retrieve subscription for account {}", killbillEvent.getAccountId(), e);
        } catch (PaymentApiException e) {
            logger.warn("Fail to send email for account {}", killbillEvent.getAccountId(), e);
        } catch (EmailException e) {
            logger.warn("Fail to send email for account {}", killbillEvent.getAccountId(), e);
            // Attempt a retry
            throw new NotificationPluginApiRetryException(e);
        } catch (IOException e) {
            logger.warn("Fail to send email for account {}", killbillEvent.getAccountId(), e);
        } catch (TenantApiException e) {
            logger.warn("Fail to send email for account {}", killbillEvent.getAccountId(), e);
        } catch (IllegalArgumentException e) {
            logger.warn(e.getMessage(), e);
        } catch (MustacheException e) {
            logger.warn(e.getMessage(), e);
        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
        }
    }

    private boolean isEventTypeAllowed(final UUID kbAccountId, final UUID kbTenantId, final ExtBusEventType eventType)
    {
        final EmailNotificationsConfiguration registeredEventType;
        final EmailNotificationConfiguration configuration = emailNotificationConfigurationHandler.getConfigurable(kbTenantId);

        if (configuration.getEventTypes().contains(eventType.toString()))
        {
            return true;
        }

        try {
            registeredEventType = this.dao.getEventTypePerAccount(kbAccountId,kbTenantId,eventType);
        } catch (SQLException e) {
            logger.warn("Error retrieving email notification event registry: {}",e.getMessage());
            return false;
        }

        if (registeredEventType == null) {
           logger.warn("Registration of event {} is not available for account {}",eventType.toString(),kbAccountId);
            return false;
        }

        return true;
    }

    private void sendEmailForUpComingInvoice(final Account account, final ExtBusEvent killbillEvent, final TenantContext context) throws IOException, InvoiceApiException, EmailException, TenantApiException, EmailNotificationException {

        Preconditions.checkArgument(killbillEvent.getEventType() == ExtBusEventType.INVOICE_NOTIFICATION, String.format("Unexpected event %s", killbillEvent.getEventType()));

        final String dryRunTimePropValue = configProperties.getString(INVOICE_DRY_RUN_TIME_PROPERTY);
        Preconditions.checkArgument(dryRunTimePropValue != null, String.format("Cannot find property %s", INVOICE_DRY_RUN_TIME_PROPERTY));

        final TimeSpan span = new TimeSpan(dryRunTimePropValue);

        final DateTime now = clock.getClock().getUTCNow();
        final DateTime targetDateTime = now.plus(span.getMillis());

        final PluginCallContext callContext = new PluginCallContext(EmailNotificationActivator.PLUGIN_NAME, now, context.getAccountId(), context.getTenantId());
        final Invoice invoice = osgiKillbillAPI.getInvoiceUserApi().triggerDryRunInvoiceGeneration(account.getId(), new LocalDate(targetDateTime, account.getTimeZone()), NULL_DRY_RUN_ARGUMENTS, callContext);
        if (invoice != null) {
            final EmailContent emailContent = templateRenderer.generateEmailForUpComingInvoice(account, invoice, context);
            sendEmail(account, emailContent, context);
        }
    }

    private void sendEmailForCancelledSubscription(final Account account, final ExtBusEvent killbillEvent, final TenantContext context) throws SubscriptionApiException, IOException, EmailException, TenantApiException, EmailNotificationException {
        Preconditions.checkArgument(killbillEvent.getEventType() == ExtBusEventType.SUBSCRIPTION_CANCEL, String.format("Unexpected event %s", killbillEvent.getEventType()));
        final UUID subscriptionId = killbillEvent.getObjectId();

        final Subscription subscription = osgiKillbillAPI.getSubscriptionApi().getSubscriptionForEntitlementId(subscriptionId, context);
        if (subscription != null) {
            final EmailContent emailContent = subscription.getState() == Entitlement.EntitlementState.CANCELLED ?
                    templateRenderer.generateEmailForSubscriptionCancellationEffective(account, subscription, context) :
                    templateRenderer.generateEmailForSubscriptionCancellationRequested(account, subscription, context);
            sendEmail(account, emailContent, context);
        }
    }

    private void sendEmailForPayment(final Account account, final ExtBusEvent killbillEvent, final TenantContext context) throws InvoiceApiException, IOException, EmailException, PaymentApiException, TenantApiException, EmailNotificationException {
        final UUID invoiceId = killbillEvent.getObjectId();
        if (invoiceId == null) {
            return;
        }

        Preconditions.checkArgument(killbillEvent.getEventType() == ExtBusEventType.INVOICE_PAYMENT_FAILED || killbillEvent.getEventType() == ExtBusEventType.INVOICE_PAYMENT_SUCCESS, String.format("Unexpected event %s", killbillEvent.getEventType()));

        final Invoice invoice = osgiKillbillAPI.getInvoiceUserApi().getInvoice(invoiceId, context);
        if (invoice.getNumberOfPayments() == 0) {
            // Aborted payment? Maybe no default payment method...
            return;
        }
        final InvoicePayment invoicePayment = invoice.getPayments().get(invoice.getNumberOfPayments() - 1);

        final Payment payment = osgiKillbillAPI.getPaymentApi().getPayment(invoicePayment.getPaymentId(), false, false, ImmutableList.<PluginProperty>of(), context);
        final PaymentTransaction lastTransaction = payment.getTransactions().get(payment.getTransactions().size() - 1);

        if (lastTransaction.getTransactionType() != TransactionType.PURCHASE &&
                lastTransaction.getTransactionType() != TransactionType.REFUND) {
            // Ignore for now, but this is easy to add...
            return;
        }

        EmailContent emailContent = null;
        if (lastTransaction.getTransactionType() == TransactionType.REFUND && lastTransaction.getTransactionStatus() == TransactionStatus.SUCCESS) {
            emailContent = templateRenderer.generateEmailForPaymentRefund(account, lastTransaction, context);
        } else {
            if (lastTransaction.getTransactionType() == TransactionType.PURCHASE && lastTransaction.getTransactionStatus() == TransactionStatus.SUCCESS) {
                emailContent = templateRenderer.generateEmailForSuccessfulPayment(account, invoice, context);
            } else if (lastTransaction.getTransactionType() == TransactionType.PURCHASE && lastTransaction.getTransactionStatus() == TransactionStatus.PAYMENT_FAILURE) {
                emailContent = templateRenderer.generateEmailForFailedPayment(account, invoice, context);
            }
        }
        if (emailContent != null) {
            sendEmail(account, emailContent, context);
        }
    }

    private void sendEmailForInvoiceCreation(final Account account, final ExtBusEvent killbillEvent, final TenantContext context) throws InvoiceApiException, IOException, TenantApiException, EmailException, EmailNotificationException {
        Preconditions.checkArgument(killbillEvent.getEventType() == ExtBusEventType.INVOICE_CREATION, String.format("Unexpected event %s", killbillEvent.getEventType()));

        final Invoice invoice = osgiKillbillAPI.getInvoiceUserApi().getInvoice(killbillEvent.getObjectId(), context);
        if (invoice != null) {
            final EmailContent emailContent = templateRenderer.generateEmailForInvoiceCreation(account, invoice, context);
            sendEmail(account, emailContent, context);
        } else {
           logger.warn("Fail to send email for account {}. Invoice not found for object {}",killbillEvent.getAccountId().toString(),
                                                                 killbillEvent.getObjectId().toString());
        }
    }

    private void sendEmail(final Account account, final EmailContent emailContent, final TenantContext context) throws IOException, EmailException, EmailNotificationException {
        final Iterable<String> cc = Iterables.transform(osgiKillbillAPI.getAccountUserApi().getEmails(account.getId(), context), new Function<AccountEmail, String>() {
            @Nullable
            @Override
            public String apply(AccountEmail input) {
                return input.getEmail();
            }
        });

        final EmailNotificationConfiguration emailNotificationConfiguration = getConfiguration(context);
        if (emailNotificationConfiguration.sendHTMLEmail()) {
            emailSender.sendHTMLEmail(ImmutableList.of(account.getEmail()), ImmutableList.copyOf(cc), emailContent.getSubject(), emailContent.getBody(), emailNotificationConfiguration.getSmtp());
        } else {
            emailSender.sendPlainTextEmail(ImmutableList.of(account.getEmail()), ImmutableList.copyOf(cc), emailContent.getSubject(), emailContent.getBody(), emailNotificationConfiguration.getSmtp());
        }
    }

    private static final class EmailNotificationContext implements TenantContext {

        private final UUID accountId;
        private final UUID tenantId;

        private EmailNotificationContext(final UUID accountId, final UUID tenantId) {
            this.accountId = accountId;
            this.tenantId = tenantId;
        }

        @Override
        public UUID getAccountId() {
            return accountId;
        }

        @Override
        public UUID getTenantId() {
            return tenantId;
        }
    }

    private final static class NullDryRunArguments implements DryRunArguments {
        @Override
        public DryRunType getDryRunType() {
            return null;
        }

        @Override
        public EntitlementSpecifier getEntitlementSpecifier() {
            return null;
        }

        @Override
        public SubscriptionEventType getAction() {
            return null;
        }

        @Override
        public UUID getSubscriptionId() {
            return null;
        }

        @Override
        public LocalDate getEffectiveDate() {
            return null;
        }

        @Override
        public UUID getBundleId() {
            return null;
        }

        @Override
        public BillingActionPolicy getBillingActionPolicy() {
            return null;
        }

    }

    private EmailNotificationConfiguration getConfiguration(final TenantContext context){
        return emailNotificationConfigurationHandler.getConfigurable(context.getTenantId());
    }
}
