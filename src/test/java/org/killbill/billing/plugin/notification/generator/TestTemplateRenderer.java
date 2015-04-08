package org.killbill.billing.plugin.notification.generator;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.killbill.billing.account.api.AccountData;
import org.killbill.billing.catalog.api.BillingActionPolicy;
import org.killbill.billing.catalog.api.BillingPeriod;
import org.killbill.billing.catalog.api.CatalogApiException;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.catalog.api.PhaseType;
import org.killbill.billing.catalog.api.Plan;
import org.killbill.billing.catalog.api.PlanPhase;
import org.killbill.billing.catalog.api.PlanPhasePriceOverride;
import org.killbill.billing.catalog.api.PriceList;
import org.killbill.billing.catalog.api.Product;
import org.killbill.billing.catalog.api.ProductCategory;
import org.killbill.billing.entitlement.api.Entitlement;
import org.killbill.billing.entitlement.api.EntitlementApiException;
import org.killbill.billing.entitlement.api.Subscription;
import org.killbill.billing.entitlement.api.SubscriptionEvent;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.invoice.api.InvoiceItemType;
import org.killbill.billing.invoice.api.InvoicePayment;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.payment.api.TransactionStatus;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.notification.email.EmailContent;
import org.killbill.billing.plugin.notification.templates.MustacheTemplateEngine;
import org.killbill.billing.plugin.notification.templates.TemplateEngine;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class TestTemplateRenderer {

    private TemplateRenderer renderer;

    @BeforeClass(groups = "fast")
    public void beforeClass() throws Exception {
        final TemplateEngine templateEngine = new MustacheTemplateEngine();
        final ResourceBundleFactory bundleFactory = new ResourceBundleFactory();
        renderer = new TemplateRenderer(templateEngine, bundleFactory);
    }


    @Test(groups = "fast")
    public void testSuccessfulPayment() throws Exception {

        final AccountData account = createAccount();
        final List<InvoiceItem> items = new ArrayList<InvoiceItem>();
        items.add(createInvoiceItem(InvoiceItemType.RECURRING, new LocalDate("2015-04-06"), new BigDecimal("123.45"), "chocolate-monthly"));
        items.add(createInvoiceItem(InvoiceItemType.TAX, new LocalDate("2015-04-06"), new BigDecimal("7.55"), "chocolate-monthly"));
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
                "2015-04-06 chocolate-monthly : USD 123.45\n" +
                "2015-04-06 chocolate-monthly : USD 7.55\n" +
                "\n" +
                "Paid:  131.00\n" +
                "Total: 0\n" +
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
        Assert.assertEquals(email.getSubject(), "MERCHANT_NAME: Payment Confirmation");
        Assert.assertEquals(email.getBody(), expectedBody);
    }

    @Test(groups = "fast")
    public void testFailedPayment() throws Exception {

        final AccountData account = createAccount();
        final List<InvoiceItem> items = new ArrayList<InvoiceItem>();
        items.add(createInvoiceItem(InvoiceItemType.RECURRING, new LocalDate("2015-04-06"), new BigDecimal("123.45"), "chocolate-monthly"));
        items.add(createInvoiceItem(InvoiceItemType.TAX, new LocalDate("2015-04-06"), new BigDecimal("7.55"), "chocolate-monthly"));
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
                "2015-04-06 chocolate-monthly : USD 123.45\n" +
                "2015-04-06 chocolate-monthly : USD 7.55\n" +
                "\n" +
                "Paid:  131.00\n" +
                "Total: 0\n" +
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
        Assert.assertEquals(email.getSubject(), "MERCHANT_NAME: Failed Payment");
        Assert.assertEquals(email.getBody(), expectedBody);
    }


    @Test(groups = "fast")
    public void testPaymentrefund() throws Exception {

        final AccountData account = createAccount();
        final PaymentTransaction paymentTransaction = createPaymentTransaction(BigDecimal.TEN, Currency.USD);

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForPaymentRefund(account, paymentTransaction, tenantContext);

        final String expectedBody = "*** Your payment has been refunded ***\n" +
                "\n" +
                "We have processed a refund in the amount of USD 10.\n" +
                "\n" +
                "This refund will appear on your next credit card statement in approximately 3-5 business days.\n" +
                "\n" +
                "If you have any questions about your account, please reply to this email or contact MERCHANT_NAME Support at: (888) 555-1234";

        // System.err.println(email.getBody());
        Assert.assertEquals(email.getSubject(), "MERCHANT_NAME: Refund Receipt");
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
        Assert.assertEquals(email.getSubject(), "MERCHANT_NAME: Subscription Canceled");
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
        Assert.assertEquals(email.getSubject(), "MERCHANT_NAME: Subscription Ended");
        Assert.assertEquals(email.getBody(), expectedBody);
    }


    @Test(groups = "fast")
    public void testUpComingInvoice() throws Exception {

        final AccountData account = createAccount();
        final List<InvoiceItem> items = new ArrayList<InvoiceItem>();
        items.add(createInvoiceItem(InvoiceItemType.RECURRING, new LocalDate("2015-04-06"), new BigDecimal("123.45"), "chocolate-monthly"));
        items.add(createInvoiceItem(InvoiceItemType.TAX, new LocalDate("2015-04-06"), new BigDecimal("7.55"), "chocolate-monthly"));
        final Invoice invoice = createInvoice(234, new LocalDate("2015-04-06"), new BigDecimal("131.00"), BigDecimal.ZERO, account.getCurrency(), items);

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForUpComingInvoice(account, invoice, tenantContext);

        final String expectedBody = "*** You Have a New Invoice ***\n" +
                "\n" +
                "You have a new invoice from MERCHANT_NAME, due on 2015-04-06.\n" +
                "\n" +
                "2015-04-06 chocolate-monthly : USD 123.45\n" +
                "2015-04-06 chocolate-monthly : USD 7.55\n" +
                "\n" +
                "Total: 0\n" +
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
        Assert.assertEquals(email.getSubject(), "MERCHANT_NAME: You Have a New Invoice");

        Assert.assertEquals(email.getBody(), expectedBody);
    }


    private TenantContext createTenantContext() {
        return new TenantContext() {
            @Override
            public UUID getTenantId() {
                return null;
            }
        };
    }

    private Subscription createFutureCancelledSubscription(final LocalDate chargedThroughDate, final String planName) {
        final Plan lastActivePlan = new Plan() {
            @Override
            public PlanPhase[] getInitialPhases() {
                return new PlanPhase[0];
            }

            @Override
            public Product getProduct() {
                return null;
            }

            @Override
            public String getName() {
                return planName;
            }

            @Override
            public boolean isRetired() {
                return false;
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
            public Date getEffectiveDateForExistingSubscriptons() {
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
            public String getCurrentStateForService(String serviceName) {
                return null;
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
            public Entitlement cancelEntitlementWithDate(LocalDate effectiveDate, boolean overrideBillingEffectiveDate, CallContext context) throws EntitlementApiException {
                return null;
            }

            @Override
            public Entitlement cancelEntitlementWithPolicy(EntitlementActionPolicy policy, CallContext context) throws EntitlementApiException {
                return null;
            }

            @Override
            public Entitlement cancelEntitlementWithDateOverrideBillingPolicy(LocalDate effectiveDate, BillingActionPolicy billingPolicy, CallContext context) throws EntitlementApiException {
                return null;
            }

            @Override
            public Entitlement cancelEntitlementWithPolicyOverrideBillingPolicy(EntitlementActionPolicy policy, BillingActionPolicy billingPolicy, CallContext context) throws EntitlementApiException {
                return null;
            }

            @Override
            public void uncancelEntitlement(CallContext context) throws EntitlementApiException {

            }

            @Override
            public Entitlement changePlan(String productName, BillingPeriod billingPeriod, String priceList, List<PlanPhasePriceOverride> overrides, CallContext context) throws EntitlementApiException {
                return null;
            }

            @Override
            public Entitlement changePlanWithDate(String productName, BillingPeriod billingPeriod, String priceList, List<PlanPhasePriceOverride> overrides, LocalDate effectiveDate, CallContext context) throws EntitlementApiException {
                return null;
            }

            @Override
            public Entitlement changePlanOverrideBillingPolicy(String productName, BillingPeriod billingPeriod, String priceList, List<PlanPhasePriceOverride> overrides, LocalDate effectiveDate, BillingActionPolicy billingPolicy, CallContext context) throws EntitlementApiException {
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


    private InvoiceItem createInvoiceItem(final InvoiceItemType type, final LocalDate startDate, final BigDecimal amount, final String planName) {
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
                return null;
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
            public String getPlanName() {
                return planName;
            }

            @Override
            public String getPhaseName() {
                return null;
            }

            @Override
            public String getUsageName() {
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
                return Currency.USD;
            }

            @Override
            public UUID getPaymentMethodId() {
                return null;
            }

            @Override
            public DateTimeZone getTimeZone() {
                return DateTimeZone.UTC;
            }

            @Override
            public String getLocale() {
                return "en_US";
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
            public Boolean isNotifiedForInvoices() {
                return true;
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
}
