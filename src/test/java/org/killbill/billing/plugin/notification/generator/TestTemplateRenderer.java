package org.killbill.billing.plugin.notification.generator;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.killbill.billing.account.api.AccountData;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.invoice.api.InvoiceItemType;
import org.killbill.billing.invoice.api.InvoicePayment;
import org.killbill.billing.plugin.notification.email.EmailContent;
import org.killbill.billing.plugin.notification.templates.MustacheTemplateEngine;
import org.killbill.billing.plugin.notification.templates.TemplateEngine;
import org.killbill.billing.util.callcontext.TenantContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
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
        items.add(createInvoiceItem(InvoiceItemType.RECURRING, new LocalDate(), new BigDecimal("123.45"), "chocolate-monthly"));
        items.add(createInvoiceItem(InvoiceItemType.TAX, new LocalDate(), new BigDecimal("7.55"), "chocolate-monthly"));
        final Invoice invoice = createInvoice(234, new LocalDate(), new BigDecimal("131.00"), BigDecimal.ZERO, account.getCurrency(), items);

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForSuccessfulPayment(account, invoice, tenantContext);

        System.err.println(email.getBody());
    }

    private TenantContext createTenantContext() {
        return new TenantContext() {
            @Override
            public UUID getTenantId() {
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
                return null;
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


}
