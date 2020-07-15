/*
 * Copyright 2015-2016 Groupon, Inc
 * Copyright 2015-2016 The Billing Project, LLC
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

package org.killbill.billing.plugin.notification.generator;

import com.google.common.base.Strings;
import org.killbill.billing.account.api.AccountData;
import org.killbill.billing.entitlement.api.Subscription;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.formatters.InvoiceFormatter;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.plugin.notification.api.InvoiceFormatterFactory;
import org.killbill.billing.plugin.notification.email.EmailContent;
import org.killbill.billing.plugin.notification.exception.EmailNotificationException;
import org.killbill.billing.plugin.notification.exception.EmailNotificationException;
import org.killbill.billing.plugin.notification.generator.formatters.DefaultInvoiceFormatter;
import org.killbill.billing.plugin.notification.generator.formatters.PaymentFormatter;
import org.killbill.billing.plugin.notification.templates.TemplateEngine;
import org.killbill.billing.plugin.notification.templates.TemplateType;
import org.killbill.billing.plugin.notification.util.IOUtils;
import org.killbill.billing.plugin.notification.util.LocaleUtils;
import org.killbill.billing.tenant.api.TenantApiException;
import org.killbill.billing.tenant.api.TenantUserApi;
import org.killbill.billing.util.callcontext.TenantContext;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import static org.killbill.billing.plugin.notification.exception.EmailNotificationErrorCode.TEMPLATE_INVALID;
import static org.killbill.billing.plugin.notification.exception.EmailNotificationErrorCode.TRANSLATION_INVALID;

public class TemplateRenderer {

    private final String DEFAULT_TEMPLATE_PATH_PREFIX = "org/killbill/billing/plugin/notification/templates/";

    private final TemplateEngine templateEngine;
    private final ResourceBundleFactory bundleFactory;
    private final TenantUserApi tenantApi;
    private final LogService logService;
    private ServiceTracker<InvoiceFormatterFactory, InvoiceFormatterFactory> invoiceFormatterTracker;

    public TemplateRenderer(final TemplateEngine templateEngine,
                            final ResourceBundleFactory bundleFactory,
                            final TenantUserApi tenantApi,
                            final LogService logService) {
        this.templateEngine = templateEngine;
        this.bundleFactory = bundleFactory;
        this.tenantApi = tenantApi;
        this.logService = logService;
    }


    public EmailContent generateEmailForUpComingInvoice(final AccountData account, final Invoice invoice, final TenantContext context) throws IOException, TenantApiException, EmailNotificationException {
        return getEmailContent(TemplateType.UPCOMING_INVOICE, account, null, invoice, null, context);
    }

    public EmailContent generateEmailForSuccessfulPayment(final AccountData account, final Invoice invoice, final TenantContext context) throws IOException, TenantApiException, EmailNotificationException {
        return getEmailContent(TemplateType.SUCCESSFUL_PAYMENT, account, null, invoice, null, context);
    }

    public EmailContent generateEmailForFailedPayment(final AccountData account, final Invoice invoice, final TenantContext context) throws IOException, TenantApiException, EmailNotificationException {
        return getEmailContent(TemplateType.FAILED_PAYMENT, account, null, invoice, null, context);
    }

    public EmailContent generateEmailForPaymentRefund(final AccountData account, final PaymentTransaction paymentTransaction, final TenantContext context) throws IOException, TenantApiException, EmailNotificationException {
        return getEmailContent(TemplateType.PAYMENT_REFUND, account, null, null, paymentTransaction, context);
    }

    public EmailContent generateEmailForSubscriptionCancellationRequested(final AccountData account, final Subscription subscription, final TenantContext context) throws IOException, TenantApiException, EmailNotificationException {
        return getEmailContent(TemplateType.SUBSCRIPTION_CANCELLATION_REQUESTED, account, subscription, null, null, context);
    }

    public EmailContent generateEmailForSubscriptionCancellationEffective(final AccountData account, final Subscription subscription, final TenantContext context) throws IOException, TenantApiException, EmailNotificationException {
        return getEmailContent(TemplateType.SUBSCRIPTION_CANCELLATION_EFFECTIVE, account, subscription, null, null, context);
    }

    public EmailContent generateEmailForInvoiceCreation(final AccountData account, final Invoice invoice, final TenantContext context)throws IOException, TenantApiException, EmailNotificationException {
        return getEmailContent(TemplateType.INVOICE_CREATION, account, null, invoice, null, context);
    }

    private EmailContent getEmailContent(final TemplateType templateType, final AccountData account, @Nullable Subscription subscription, @Nullable final Invoice invoice, @Nullable final PaymentTransaction paymentTransaction, final TenantContext context) throws IOException, TenantApiException, EmailNotificationException {

        final String accountLocale = Strings.emptyToNull(account.getLocale());
        final Locale locale = accountLocale == null ? Locale.getDefault() : LocaleUtils.toLocale(accountLocale);

        final Map<String, Object> data = new HashMap<String, Object>();
        final Map<String, String> text = getTranslationMap(accountLocale, ResourceBundleFactory.ResourceBundleType.TEMPLATE_TRANSLATION, context);
        data.put("text", text);
        data.put("account", account);
        if (subscription != null) {
            data.put("subscription", subscription);
        }
        if (invoice != null) {
            // look for a custom InvoiceFormatter via our factory service tracker, if available
            final InvoiceFormatterFactory formatterFactory = (invoiceFormatterTracker != null ? invoiceFormatterTracker.getService() : null);
            InvoiceFormatter formattedInvoice = (formatterFactory != null 
            		? formatterFactory.createInvoiceFormatter(text, invoice, locale, context) : null);
            if ( formattedInvoice == null ) {
                formattedInvoice = new DefaultInvoiceFormatter(text, invoice, locale);
            }
            data.put("invoice", formattedInvoice);
        }
        if (paymentTransaction != null) {
            final PaymentFormatter formattedPayment = new PaymentFormatter(paymentTransaction, locale);
            data.put("payment", formattedPayment);
        }

        final String templateText = getTemplateText(locale, templateType, context);
        if (templateText == null){
            throw new EmailNotificationException(TEMPLATE_INVALID, accountLocale);
        }

        final String body = templateEngine.executeTemplateText(templateText, data);
        final String subject = text.get(templateType.getSubjectKeyName());
        return new EmailContent(subject, body);
    }

    private Map<String, String> getTranslationMap(final String accountLocale, final ResourceBundleFactory.ResourceBundleType bundleType, final TenantContext context) throws TenantApiException, EmailNotificationException {
        final ResourceBundle translationBundle = accountLocale != null ?
                bundleFactory.createBundle(LocaleUtils.toLocale(accountLocale), bundleType, context) : null;

        if (translationBundle == null){
            throw new EmailNotificationException(TRANSLATION_INVALID, accountLocale);
        }

        final Map<String, String> text = new HashMap<String, String>();
        Enumeration<String> keys = translationBundle.getKeys();
        while (keys.hasMoreElements()) {
            final String key = keys.nextElement();
            text.put(key, translationBundle.getString(key));
        }
        return text;
    }


    private String getTemplateText(final Locale locale, final TemplateType templateType, final TenantContext context) throws TenantApiException {

        final String defaultTemplateName = DEFAULT_TEMPLATE_PATH_PREFIX + templateType.getDefaultTemplateName();
        if (context.getTenantId() == null) {
            return getDefaultTemplate(defaultTemplateName);
        }

        // TODO Caching strategy
        final String templateTenantKey = LocaleUtils.localeString(locale, templateType.getTemplateKey());
        final List<String> result = tenantApi.getTenantValuesForKey(templateTenantKey, context);
        if (result.size() == 1) {

            return result.get(0);
        }
        return getDefaultTemplate(defaultTemplateName);

    }

    private String getDefaultTemplate(final String templateName) {
        try{
            final URL url = this.getClass().getClassLoader().getResource(templateName);

            if (url == null){
                return null;
            }

            final InputStream templateStream = url.openStream();

            if (templateStream == null){
                return null;
            }

            return IOUtils.toString(templateStream);
        } catch (IllegalArgumentException iae) {
            return null;
        } catch (MissingResourceException mrex) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Configure a custom {@link InvoiceFormatterFactory} via some other plugin.
     * 
     * <p>If this service is not configured, or the tracker does not return a service via {@link ServiceTracker#getService()},
     * then a {@link DefaultInvoiceFormatter} instance will be used.</p>
     * 
     * @param invoiceFormatterTracker the service tracker to use
     */
    public void setInvoiceFormatterTracker(ServiceTracker<InvoiceFormatterFactory, InvoiceFormatterFactory> invoiceFormatterTracker) {
        this.invoiceFormatterTracker = invoiceFormatterTracker;
    }

}
