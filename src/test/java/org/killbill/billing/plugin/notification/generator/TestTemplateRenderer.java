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

package org.killbill.billing.plugin.notification.generator;

import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.killbill.billing.account.api.AccountData;
import org.killbill.billing.catalog.api.BillingActionPolicy;
import org.killbill.billing.catalog.api.BillingMode;
import org.killbill.billing.catalog.api.BillingPeriod;
import org.killbill.billing.catalog.api.CatalogApiException;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.catalog.api.PhaseType;
import org.killbill.billing.catalog.api.Plan;
import org.killbill.billing.catalog.api.PlanPhase;
import org.killbill.billing.catalog.api.PlanPhasePriceOverride;
import org.killbill.billing.catalog.api.PlanPhaseSpecifier;
import org.killbill.billing.catalog.api.PlanSpecifier;
import org.killbill.billing.catalog.api.PriceList;
import org.killbill.billing.catalog.api.Product;
import org.killbill.billing.catalog.api.ProductCategory;
import org.killbill.billing.catalog.api.StaticCatalog;
import org.killbill.billing.entitlement.api.Entitlement;
import org.killbill.billing.entitlement.api.EntitlementApiException;
import org.killbill.billing.entitlement.api.EntitlementSpecifier;
import org.killbill.billing.entitlement.api.Subscription;
import org.killbill.billing.entitlement.api.SubscriptionEvent;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.invoice.api.InvoiceItemType;
import org.killbill.billing.invoice.api.InvoicePayment;
import org.killbill.billing.invoice.api.InvoiceStatus;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionStatus;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.notification.email.EmailContent;
import org.killbill.billing.plugin.notification.templates.MustacheTemplateEngine;
import org.killbill.billing.plugin.notification.templates.TemplateEngine;
import org.killbill.billing.tenant.api.Tenant;
import org.killbill.billing.tenant.api.TenantApiException;
import org.killbill.billing.tenant.api.TenantData;
import org.killbill.billing.tenant.api.TenantUserApi;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TestTemplateRenderer {

    private final Logger log = LoggerFactory.getLogger(TestTemplateRenderer.class);

    private TemplateRenderer renderer;

    @BeforeClass(groups = "fast")
    public void beforeClass() throws Exception {
        final TemplateEngine templateEngine = new MustacheTemplateEngine();
        final ResourceBundleFactory bundleFactory = new ResourceBundleFactory(getMockTenantUserApi());
        renderer = new TemplateRenderer(templateEngine, bundleFactory, getMockTenantUserApi());
    }

    @Test(groups = "fast")
    public void testSuccessfulPaymentUSD() throws Exception {

        final AccountData account = createAccount();
        final List<InvoiceItem> items = new ArrayList<InvoiceItem>();
        items.add(createInvoiceItem(InvoiceItemType.RECURRING, new LocalDate("2015-04-06"), new BigDecimal("123.45"), account.getCurrency(), "chocolate-monthly"));
        items.add(createInvoiceItem(InvoiceItemType.TAX, new LocalDate("2015-04-06"), new BigDecimal("7.55"), account.getCurrency(), "chocolate-monthly"));
        final Invoice invoice = createInvoice(234, new LocalDate("2015-04-06"), new BigDecimal("131.00"), BigDecimal.ZERO, account.getCurrency(), items);

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForSuccessfulPayment(account, invoice, tenantContext);

        final String expectedBody = "This email confirms your recent payment.\n" +
                "\n" +
                "Here are the details of your payment:\n" +
                "\n" +
                "Invoice #: 234\n" +
                "Payment Date: 2015-04-06\n" +
                "\n" +
                "2015-04-06 chocolate-monthly : $123.45\n" +
                "2015-04-06 chocolate-monthly : $7.55\n" +
                "\n" +
                "Paid:  $131.00\n" +
                "Total: $0.00\n" +
                "\n" +
                "Billed To::\n" +
                "SauvonsLaTerre\n" +
                "Sylvie Dupond\n" +
                "1234 Trumpet street\n" +
                "San Francisco, CA 94110\n" +
                "USA\n" +
                "\n" +
                "If you have any questions about your account, please reply to this email or contact MERCHANT_NAME Support at: (888) 555-1234";

        //System.err.println(email.getBody());
        Assert.assertEquals(email.getSubject(), "Payment Confirmation");
        Assert.assertEquals(email.getBody(), expectedBody);
    }

    @Test(groups = "fast")
    public void testSuccessfulPaymentGBP() throws Exception {

        final AccountData account = createAccount(Currency.GBP, "en_GB");
        final List<InvoiceItem> items = new ArrayList<InvoiceItem>();
        items.add(createInvoiceItem(InvoiceItemType.RECURRING, new LocalDate("2015-04-06"), new BigDecimal("123.45"), account.getCurrency(), "chocolate-monthly"));
        items.add(createInvoiceItem(InvoiceItemType.TAX, new LocalDate("2015-04-06"), new BigDecimal("7.55"), account.getCurrency(), "chocolate-monthly"));
        final Invoice invoice = createInvoice(234, new LocalDate("2015-04-06"), new BigDecimal("131.00"), BigDecimal.ZERO, account.getCurrency(), items);

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForSuccessfulPayment(account, invoice, tenantContext);

        final String expectedBody = "This email confirms your recent payment.\n" +
                "\n" +
                "Here are the details of your payment:\n" +
                "\n" +
                "Invoice #: 234\n" +
                "Payment Date: 2015-04-06\n" +
                "\n" +
                "2015-04-06 chocolate-monthly : £123.45\n" +
                "2015-04-06 chocolate-monthly : £7.55\n" +
                "\n" +
                "Paid:  £131.00\n" +
                "Total: £0.00\n" +
                "\n" +
                "Billed To::\n" +
                "SauvonsLaTerre\n" +
                "Sylvie Dupond\n" +
                "1234 Trumpet street\n" +
                "San Francisco, CA 94110\n" +
                "USA\n" +
                "\n" +
                "If you have any questions about your account, please reply to this email or contact MERCHANT_NAME Support at: (888) 555-1234";

        //System.err.println(email.getBody());
        Assert.assertEquals(email.getSubject(), "Payment Confirmation");
        Assert.assertEquals(email.getBody(), expectedBody);
    }

    @Test(groups = "fast")
    public void testFailedPayment() throws Exception {

        final AccountData account = createAccount();
        final List<InvoiceItem> items = new ArrayList<InvoiceItem>();
        items.add(createInvoiceItem(InvoiceItemType.RECURRING, new LocalDate("2015-04-06"), new BigDecimal("123.45"), account.getCurrency(), "chocolate-monthly"));
        items.add(createInvoiceItem(InvoiceItemType.TAX, new LocalDate("2015-04-06"), new BigDecimal("7.55"), account.getCurrency(), "chocolate-monthly"));
        final Invoice invoice = createInvoice(234, new LocalDate("2015-04-06"), new BigDecimal("131.00"), BigDecimal.ZERO, account.getCurrency(), items);

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForFailedPayment(account, invoice, tenantContext);

        final String expectedBody = "Oops, your most recent subscription payment was declined by your bank.\n" +
                "\n" +
                "Here are the details of your declined payment:\n" +
                "\n" +
                "Invoice #: 234\n" +
                "Payment Date: 2015-04-06\n" +
                "\n" +
                "2015-04-06 chocolate-monthly : $123.45\n" +
                "2015-04-06 chocolate-monthly : $7.55\n" +
                "\n" +
                "Paid:  $131.00\n" +
                "Total: $0.00\n" +
                "\n" +
                "\n" +
                "Billed To::\n" +
                "SauvonsLaTerre\n" +
                "Sylvie Dupond\n" +
                "1234 Trumpet street\n" +
                "San Francisco, CA 94110\n" +
                "USA\n" +
                "\n" +
                "You can update your payment information here:\n" +
                "http://paymentLink\n" +
                "\n" +
                "To ensure continued access to MERCHANT_NAME, please update your billing information within 14 days of your last statement.\n" +
                "\n" +
                "If you have any questions about your account, please reply to this email or contact MERCHANT_NAME Support at: (888) 555-1234";

        //System.err.println(email.getBody());
        Assert.assertEquals(email.getSubject(), "Failed Payment");
        Assert.assertEquals(email.getBody(), expectedBody);
    }

    @Test(groups = "fast")
    public void testPaymentRefund() throws Exception {
        final AccountData account = createAccount();
        final PaymentTransaction paymentTransaction = createPaymentTransaction(new BigDecimal("937.070000000"), Currency.USD);

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForPaymentRefund(account, paymentTransaction, tenantContext);

        final String expectedBody = "*** Your payment has been refunded ***\n" +
                "\n" +
                "We have processed a refund in the amount of $937.07.\n" +
                "\n" +
                "This refund will appear on your next credit card statement in approximately 3-5 business days.\n" +
                "\n" +
                "If you have any questions about your account, please reply to this email or contact MERCHANT_NAME Support at: (888) 555-1234";

        // System.err.println(email.getBody());
        Assert.assertEquals(email.getSubject(), "Refund Receipt");
        Assert.assertEquals(email.getBody(), expectedBody);
    }

    @Test(groups = "fast")
    public void testSubscriptionCancellationRequested() throws Exception {

        final AccountData account = createAccount();
        final Subscription cancelledSubscription = createFutureCancelledSubscription(new LocalDate("2015-04-06"), "myPlanName");

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForSubscriptionCancellationRequested(account, cancelledSubscription, tenantContext);

        final String expectedBody = "This email is to confirm your recent subscription cancellation. Your subscription will remain active until 2015-04-06.\n" +
                "\n" +
                "Here are the details of the subscription you canceled:\n" +
                "\n" +
                "Description: myPlanName\n" +
                "Expires at:  2015-04-06\n" +
                "\n" +
                "At the end of your subscription, your MERCHANT_NAME account will be disabled and you will not be able to access MERCHANT_NAME on your iPad.\n" +
                "\n" +
                "We're sorry to see you go.  If you have any questions or if you have received this message in error, please reply to this email or contact MERCHANT_NAME Support at (888) 555-1234.\n" +
                "\n" +
                "To reactivate your subscription before it expires, or to view previous invoices, please see your account at: http://subscriptionLink\n" +
                "\n" +
                "If you have any questions about your account, please reply to this email or contact MERCHANT_NAME Support at: (888) 555-1234";

        //System.err.println(email.getBody());
        Assert.assertEquals(email.getSubject(), "Subscription Canceled");
        Assert.assertEquals(email.getBody(), expectedBody);
    }


    @Test(groups = "fast")
    public void testSubscriptionCancellationEffective() throws Exception {

        final AccountData account = createAccount();
        final Subscription cancelledSubscription = createFutureCancelledSubscription(new LocalDate("2015-04-06"), "myPlanName");

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForSubscriptionCancellationEffective(account, cancelledSubscription, tenantContext);

        final String expectedBody = "Your Personal subscription to MERCHANT_NAME was canceled earlier and has now ended. Your access to MERCHANT_NAME on your iPad will end shortly.\n" +
                "\n" +
                "We're sorry to see you go.  To reactivate your MERCHANT_NAME service, contact the Support Team at (888) 555-1234.\n" +
                "\n" +
                "If you have any questions about your account, please reply to this email or contact MERCHANT_NAME Support at: (888) 555-1234";

        //System.err.println(email.getBody());
        Assert.assertEquals(email.getSubject(), "Subscription Ended");
        Assert.assertEquals(email.getBody(), expectedBody);
    }


    @Test(groups = "fast")
    public void testUpComingInvoice() throws Exception {

        final AccountData account = createAccount();
        final List<InvoiceItem> items = new ArrayList<InvoiceItem>();
        items.add(createInvoiceItem(InvoiceItemType.RECURRING, new LocalDate("2015-04-06"), new BigDecimal("123.45"), account.getCurrency(), "chocolate-monthly"));
        items.add(createInvoiceItem(InvoiceItemType.TAX, new LocalDate("2015-04-06"), new BigDecimal("7.5500"), account.getCurrency(), "chocolate-monthly"));
        final Invoice invoice = createInvoice(234, new LocalDate("2015-04-06"), new BigDecimal("131.00"), BigDecimal.ZERO, account.getCurrency(), items);

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForUpComingInvoice(account, invoice, tenantContext);

        final String expectedBody = "*** You Have a New Invoice ***\n" +
                "\n" +
                "You have a new invoice from MERCHANT_NAME, due on 2015-04-06.\n" +
                "\n" +
                "2015-04-06 chocolate-monthly : $123.45\n" +
                "2015-04-06 chocolate-monthly : $7.55\n" +
                "\n" +
                "Total: $0.00\n" +
                "\n" +
                "Billed To::\n" +
                "SauvonsLaTerre\n" +
                "Sylvie Dupond\n" +
                "1234 Trumpet street\n" +
                "San Francisco, CA 94110\n" +
                "USA\n" +
                "\n" +
                "If you have any questions about your account, please reply to this email or contact MERCHANT_NAME Support at: (888) 555-1234";

        //System.err.println(email.getBody());
        Assert.assertEquals(email.getSubject(), "You Have a New Invoice");

        Assert.assertEquals(email.getBody(), expectedBody);
    }


    private TenantContext createTenantContext() {
        return new TenantContext() {
            @Override
            public UUID getAccountId() {
                return null;
            }

            @Override
            public UUID getTenantId() {
                return null;
            }
        };
    }

    private Subscription createFutureCancelledSubscription(final LocalDate chargedThroughDate, final String planName) {
        final Plan lastActivePlan = new Plan() {
            @Override
            public StaticCatalog getCatalog() {
                return null;
            }

            @Override
            public BillingMode getRecurringBillingMode() {
                return null;
            }

            @Override
            public PlanPhase[] getInitialPhases() {
                return new PlanPhase[0];
            }

            @Override
            public Product getProduct() {
                return null;
            }

            @Override
            public PriceList getPriceList() {
                return null;
            }

            @Override
            public String getName() {
                return planName;
            }

            @Override
            public String getPrettyName() {
                return null;
            }

            @Override
            public Iterator<PlanPhase> getInitialPhaseIterator() {
                return null;
            }

            @Override
            public PlanPhase getFinalPhase() {
                return null;
            }

            @Override
            public BillingPeriod getRecurringBillingPeriod() {
                return null;
            }

            @Override
            public int getPlansAllowedInBundle() {
                return 0;
            }

            @Override
            public PlanPhase[] getAllPhases() {
                return new PlanPhase[0];
            }

            @Override
            public Date getEffectiveDateForExistingSubscriptions() {
                return null;
            }

            @Override
            public PlanPhase findPhase(String name) throws CatalogApiException {
                return null;
            }

            @Override
            public DateTime dateOfFirstRecurringNonZeroCharge(DateTime subscriptionStartDate, PhaseType intialPhaseType) {
                return null;
            }
        };
        return new Subscription() {
            @Override
            public LocalDate getBillingStartDate() {
                return null;
            }

            @Override
            public LocalDate getBillingEndDate() {
                return null;
            }

            @Override
            public LocalDate getChargedThroughDate() {
                return chargedThroughDate;
            }

            @Override
            public List<SubscriptionEvent> getSubscriptionEvents() {
                return null;
            }

            @Override
            public UUID getBaseEntitlementId() {
                return null;
            }

            @Override
            public UUID getBundleId() {
                return null;
            }

            @Override
            public String getBundleExternalKey() {
                return null;
            }

            @Override
            public UUID getAccountId() {
                return null;
            }

            @Override
            public String getExternalKey() {
                return null;
            }

            @Override
            public EntitlementState getState() {
                return null;
            }

            @Override
            public EntitlementSourceType getSourceType() {
                return null;
            }

            @Override
            public LocalDate getEffectiveStartDate() {
                return null;
            }

            @Override
            public LocalDate getEffectiveEndDate() {
                return null;
            }

            @Override
            public Product getLastActiveProduct() {
                return null;
            }

            @Override
            public Plan getLastActivePlan() {
                return lastActivePlan;
            }

            @Override
            public PlanPhase getLastActivePhase() {
                return null;
            }

            @Override
            public PriceList getLastActivePriceList() {
                return null;
            }

            @Override
            public ProductCategory getLastActiveProductCategory() {
                return null;
            }

            @Override
            public Integer getBillCycleDayLocal() {
                return null;
            }

            @Override
            public Entitlement cancelEntitlementWithDate(LocalDate effectiveDate, boolean overrideBillingEffectiveDate, Iterable<PluginProperty> properties, CallContext context) throws EntitlementApiException {
                return null;
            }

            @Override
            public Entitlement cancelEntitlementWithPolicy(EntitlementActionPolicy policy, Iterable<PluginProperty> properties, CallContext context) throws EntitlementApiException {
                return null;
            }

            @Override
            public Entitlement cancelEntitlementWithDateOverrideBillingPolicy(LocalDate effectiveDate, BillingActionPolicy billingPolicy, Iterable<PluginProperty> properties, CallContext context) throws EntitlementApiException {
                return null;
            }

            @Override
            public Entitlement cancelEntitlementWithPolicyOverrideBillingPolicy(EntitlementActionPolicy policy, BillingActionPolicy billingPolicy, Iterable<PluginProperty> properties, CallContext context) throws EntitlementApiException {
                return null;
            }

            @Override
            public void uncancelEntitlement(Iterable<PluginProperty> properties, CallContext context) throws EntitlementApiException {

            }

            @Override
            public Entitlement changePlan(final EntitlementSpecifier entitlementSpecifier, final Iterable<PluginProperty> iterable, final CallContext callContext) throws EntitlementApiException {
                return null;
            }

            @Override
            public void undoChangePlan(final Iterable<PluginProperty> iterable, final CallContext callContext) throws EntitlementApiException {
            }

            @Override
            public Entitlement changePlanWithDate(final EntitlementSpecifier entitlementSpecifier, final LocalDate localDate, final Iterable<PluginProperty> iterable, final CallContext callContext) throws EntitlementApiException {
                return null;
            }

            @Override
            public Entitlement changePlanOverrideBillingPolicy(final EntitlementSpecifier entitlementSpecifier, final LocalDate localDate, final BillingActionPolicy billingActionPolicy, final Iterable<PluginProperty> iterable, final CallContext callContext) throws EntitlementApiException {
                return null;
            }

            @Override
            public void updateBCD(int bcd, LocalDate effectiveFromDate, CallContext context) throws EntitlementApiException {

            }

            @Override
            public UUID getId() {
                return null;
            }

            @Override
            public DateTime getCreatedDate() {
                return null;
            }

            @Override
            public DateTime getUpdatedDate() {
                return null;
            }
        };
    }


    private InvoiceItem createInvoiceItem(final InvoiceItemType type, final LocalDate startDate, final BigDecimal amount, final Currency currency, final String planName) {
        return new InvoiceItem() {
            @Override
            public InvoiceItemType getInvoiceItemType() {
                return type;
            }

            @Override
            public UUID getInvoiceId() {
                return null;
            }

            @Override
            public UUID getAccountId() {
                return null;
            }

            @Override
            public UUID getChildAccountId() {
                return null;
            }

            @Override
            public LocalDate getStartDate() {
                return startDate;
            }

            @Override
            public LocalDate getEndDate() {
                return null;
            }

            @Override
            public BigDecimal getAmount() {
                return amount;
            }

            @Override
            public Currency getCurrency() {
                return currency;
            }

            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public UUID getBundleId() {
                return null;
            }

            @Override
            public UUID getSubscriptionId() {
                return null;
            }

            @Override
            public String getProductName() {
                return null;
            }

            @Override
            public String getPrettyProductName() {
                return null;
            }

            @Override
            public String getPlanName() {
                return planName;
            }

            @Override
            public String getPrettyPlanName() {
                return null;
            }

            @Override
            public String getPhaseName() {
                return null;
            }

            @Override
            public String getPrettyPhaseName() {
                return null;
            }

            @Override
            public String getUsageName() {
                return null;
            }

            @Override
            public String getPrettyUsageName() {
                return null;
            }

            @Override
            public BigDecimal getRate() {
                return null;
            }

            @Override
            public UUID getLinkedItemId() {
                return null;
            }

            @Override
            public Integer getQuantity() {
                return null;
            }

            @Override
            public String getItemDetails() {
                return null;
            }

            @Override
            public DateTime getCatalogEffectiveDate() {
                return null;
            }

            @Override
            public boolean matches(Object other) {
                return false;
            }

            @Override
            public UUID getId() {
                return null;
            }

            @Override
            public DateTime getCreatedDate() {
                return null;
            }

            @Override
            public DateTime getUpdatedDate() {
                return null;
            }
        };
    }

    private Invoice createInvoice(final Integer invoiceNumber, final LocalDate invoiceDate, final BigDecimal paidAmount, final BigDecimal balance, final Currency currency, final List<InvoiceItem> items) {

        return new Invoice() {
            @Override
            public boolean addInvoiceItem(InvoiceItem item) {
                return false;
            }

            @Override
            public boolean addInvoiceItems(Collection<InvoiceItem> items) {
                return false;
            }

            @Override
            public List<InvoiceItem> getInvoiceItems() {
                return items;
            }

            @Override
            public List<String> getTrackingIds() {
                return null;
            }

            @Override
            public boolean addTrackingIds(final Collection<String> trackingIds) {
                return false;
            }

            @Override
            public <T extends InvoiceItem> List<InvoiceItem> getInvoiceItems(Class<T> clazz) {
                return items;
            }

            @Override
            public int getNumberOfItems() {
                return items.size();
            }

            @Override
            public boolean addPayment(InvoicePayment payment) {
                return false;
            }

            @Override
            public boolean addPayments(Collection<InvoicePayment> payments) {
                return false;
            }

            @Override
            public List<InvoicePayment> getPayments() {
                return null;
            }

            @Override
            public int getNumberOfPayments() {
                return 0;
            }

            @Override
            public UUID getAccountId() {
                return null;
            }

            @Override
            public Integer getInvoiceNumber() {
                return invoiceNumber;
            }

            @Override
            public LocalDate getInvoiceDate() {
                return invoiceDate;
            }

            @Override
            public LocalDate getTargetDate() {
                return invoiceDate;
            }

            @Override
            public Currency getCurrency() {
                return currency;
            }

            @Override
            public BigDecimal getPaidAmount() {
                return paidAmount;
            }

            @Override
            public BigDecimal getOriginalChargedAmount() {
                return null;
            }

            @Override
            public BigDecimal getChargedAmount() {
                return null;
            }

            @Override
            public BigDecimal getCreditedAmount() {
                return null;
            }

            @Override
            public BigDecimal getRefundedAmount() {
                return null;
            }

            @Override
            public BigDecimal getBalance() {
                return balance;
            }

            @Override
            public boolean isMigrationInvoice() {
                return false;
            }

            @Override
            public InvoiceStatus getStatus() {
                return null;
            }

            @Override
            public boolean isParentInvoice() {
                return false;
            }

            @Override
            public UUID getParentAccountId() {
                return null;
            }

            @Override
            public UUID getParentInvoiceId() {
                return null;
            }

            @Override
            public UUID getId() {
                return null;
            }

            @Override
            public DateTime getCreatedDate() {
                return null;
            }

            @Override
            public DateTime getUpdatedDate() {
                return null;
            }
        };
    }

    private AccountData createAccount() {
        return createAccount(Currency.USD, "en_US");
    }

    private AccountData createAccount(final Currency currency, final String locale) {

        return new AccountData() {
            @Override
            public String getExternalKey() {
                return "foo";
            }

            @Override
            public String getName() {
                return "Sylvie Dupond";
            }

            @Override
            public Integer getFirstNameLength() {
                return 7;
            }

            @Override
            public String getEmail() {
                return "sylvie@banquedefrance.fr";
            }

            @Override
            public Integer getBillCycleDayLocal() {
                return 1;
            }

            @Override
            public Currency getCurrency() {
                return currency;
            }

            @Override
            public UUID getPaymentMethodId() {
                return null;
            }

            @Override
            public DateTime getReferenceTime() {
                return null;
            }

            @Override
            public DateTimeZone getTimeZone() {
                return DateTimeZone.UTC;
            }

            @Override
            public String getLocale() {
                return locale;
            }

            @Override
            public String getAddress1() {
                return "1234 Trumpet street";
            }

            @Override
            public String getAddress2() {
                return null;
            }

            @Override
            public String getCompanyName() {
                return "SauvonsLaTerre";
            }

            @Override
            public String getCity() {
                return "San Francisco";
            }

            @Override
            public String getStateOrProvince() {
                return "CA";
            }

            @Override
            public String getPostalCode() {
                return "94110";
            }

            @Override
            public String getCountry() {
                return "USA";
            }

            @Override
            public String getPhone() {
                return "(415) 255-7654";
            }

            @Override
            public Boolean isMigrated() {
                return false;
            }

            @Override
            public UUID getParentAccountId() {
                return null;
            }

            @Override
            public Boolean isPaymentDelegatedToParent() {
                return null;
            }

            @Override
            public String getNotes() {
                return null;
            }
        };
    }


    private PaymentTransaction createPaymentTransaction(final BigDecimal amount, final Currency currency) {
        return new PaymentTransaction() {
            @Override
            public UUID getPaymentId() {
                return null;
            }

            @Override
            public String getExternalKey() {
                return null;
            }

            @Override
            public TransactionType getTransactionType() {
                return null;
            }

            @Override
            public DateTime getEffectiveDate() {
                return null;
            }

            @Override
            public BigDecimal getAmount() {
                return amount;
            }

            @Override
            public Currency getCurrency() {
                return currency;
            }

            @Override
            public BigDecimal getProcessedAmount() {
                return null;
            }

            @Override
            public Currency getProcessedCurrency() {
                return null;
            }

            @Override
            public String getGatewayErrorCode() {
                return null;
            }

            @Override
            public String getGatewayErrorMsg() {
                return null;
            }

            @Override
            public TransactionStatus getTransactionStatus() {
                return null;
            }

            @Override
            public PaymentTransactionInfoPlugin getPaymentInfoPlugin() {
                return null;
            }

            @Override
            public UUID getId() {
                return null;
            }

            @Override
            public DateTime getCreatedDate() {
                return null;
            }

            @Override
            public DateTime getUpdatedDate() {
                return null;
            }
        };
    }

    final TenantUserApi getMockTenantUserApi() {
        return new TenantUserApi() {

            @Override
            public Tenant createTenant(TenantData data, CallContext context) throws TenantApiException {
                return null;
            }

            @Override
            public Tenant getTenantByApiKey(String key) throws TenantApiException {
                return null;
            }

            @Override
            public Tenant getTenantById(UUID tenantId) throws TenantApiException {
                return null;
            }

            @Override
            public List<String> getTenantValuesForKey(String key, TenantContext context) throws TenantApiException {
                return ImmutableList.of();
            }

            @Override
            public Map<String, List<String>> searchTenantKeyValues(String searchKey, TenantContext context) throws TenantApiException {
                return null;
            }

            @Override
            public void addTenantKeyValue(String key, String value, CallContext context) throws TenantApiException {

            }

            @Override
            public void updateTenantKeyValue(String key, String value, CallContext context) throws TenantApiException {

            }

            @Override
            public void deleteTenantKey(String key, CallContext context) throws TenantApiException {

            }
        };
    }
}
