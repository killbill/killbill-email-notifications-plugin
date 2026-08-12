/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2020-2020 Equinix, Inc
 * Copyright 2014-2021 The Billing Project, LLC
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

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.killbill.billing.account.api.AccountData;
import org.killbill.billing.account.api.boilerplate.AccountDataImp;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.catalog.api.boilerplate.PlanImp;
import org.killbill.billing.entitlement.api.Subscription;
import org.killbill.billing.entitlement.api.boilerplate.SubscriptionImp;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.invoice.api.InvoiceItemType;
import org.killbill.billing.invoice.api.boilerplate.InvoiceImp;
import org.killbill.billing.invoice.api.boilerplate.InvoiceItemImp;
import org.killbill.billing.invoice.api.formatters.InvoiceFormatter;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.payment.api.boilerplate.PaymentTransactionImp;
import org.killbill.billing.plugin.notification.TestBase;
import org.killbill.billing.plugin.notification.api.InvoiceFormatterFactory;
import org.killbill.billing.plugin.notification.email.EmailContent;
import org.killbill.billing.plugin.notification.templates.MustacheTemplateEngine;
import org.killbill.billing.plugin.notification.templates.TemplateEngine;
import org.killbill.billing.plugin.notification.util.LocaleUtils;
import org.killbill.billing.tenant.api.TenantApiException;
import org.killbill.billing.tenant.api.TenantUserApi;
import org.killbill.billing.tenant.api.boilerplate.TenantUserApiImp;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.billing.util.callcontext.boilerplate.TenantContextImp;
import org.killbill.commons.utils.io.Resources;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

@Test(groups = "fast")
public class TestTemplateRenderer extends TestBase {

    private final Logger log = LoggerFactory.getLogger(TestTemplateRenderer.class);

    @Mock
    private BundleContext bundleContext;

    @Mock
    private Bundle bundle;

    @Mock
    private ServiceReference<InvoiceFormatterFactory> invoiceFormatterFactoryRef;

    @Mock
    private InvoiceFormatterFactory invoiceFormatterFactory;

    @Mock
    private InvoiceFormatter invoiceFormatter;

    private TemplateRenderer renderer;

    private TenantUserApi tenantUserApi;

    @BeforeMethod
    public void beforeMethod() throws TenantApiException {
        MockitoAnnotations.initMocks(this);

        final TemplateEngine templateEngine = new MustacheTemplateEngine();
        tenantUserApi = Mockito.mock(TenantUserApi.class);
        when(tenantUserApi.getTenantValuesForKey(Mockito.any(), Mockito.any())).thenReturn(Collections.emptyList());
        final ResourceBundleFactory bundleFactory = new ResourceBundleFactory(tenantUserApi);
        renderer = new TemplateRenderer(templateEngine, bundleFactory, tenantUserApi);
    }

    public void testSuccessfulPaymentUSD() throws Exception {
        final AccountData account = createAccount();
        final List<InvoiceItem> items = new ArrayList<InvoiceItem>();
        items.add(createInvoiceItem(InvoiceItemType.RECURRING, new LocalDate("2015-04-06"), new BigDecimal("123.45"), account.getCurrency(), "chocolate-monthly"));
        items.add(createInvoiceItem(InvoiceItemType.TAX, new LocalDate("2015-04-06"), new BigDecimal("7.55"), account.getCurrency(), "chocolate-monthly"));
        final Invoice invoice = createInvoice(234, new LocalDate("2015-04-06"), new BigDecimal("131.00"), BigDecimal.ZERO, account.getCurrency(), items);

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForSuccessfulPayment(account, invoice, tenantContext);

        Assert.assertEquals(email.getSubject(), "Your recent payment");
        Assert.assertTrue(email.getBody().contains("Thank you for your recent payment!"));

        Assert.assertTrue(email.getBody().contains("$123.45"));
        Assert.assertFalse(email.getBody().contains("£123.45"));
    }

    public void testSuccessfulPaymentGBP() throws Exception {
        final AccountData account = createAccount(Currency.GBP, "en_GB");
        final List<InvoiceItem> items = new ArrayList<InvoiceItem>();
        items.add(createInvoiceItem(InvoiceItemType.RECURRING, new LocalDate("2015-04-06"), new BigDecimal("123.45"), account.getCurrency(), "chocolate-monthly"));
        items.add(createInvoiceItem(InvoiceItemType.TAX, new LocalDate("2015-04-06"), new BigDecimal("7.55"), account.getCurrency(), "chocolate-monthly"));
        final Invoice invoice = createInvoice(234, new LocalDate("2015-04-06"), new BigDecimal("131.00"), BigDecimal.ZERO, account.getCurrency(), items);

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForSuccessfulPayment(account, invoice, tenantContext);

        Assert.assertEquals(email.getSubject(), "Payment Confirmation, Old Boy");
        Assert.assertTrue(email.getBody().contains("Thank you for your recent payment!"));
        Assert.assertTrue(email.getBody().contains("£123.45"));
        Assert.assertFalse(email.getBody().contains("$123.45"));
    }

    public void testFailedPayment() throws Exception {
        final AccountData account = createAccount();
        final List<InvoiceItem> items = new ArrayList<InvoiceItem>();
        items.add(createInvoiceItem(InvoiceItemType.RECURRING, new LocalDate("2015-04-06"), new BigDecimal("123.45"), account.getCurrency(), "chocolate-monthly"));
        items.add(createInvoiceItem(InvoiceItemType.TAX, new LocalDate("2015-04-06"), new BigDecimal("7.55"), account.getCurrency(), "chocolate-monthly"));
        final Invoice invoice = createInvoice(234, new LocalDate("2015-04-06"), new BigDecimal("131.00"), BigDecimal.ZERO, account.getCurrency(), items);

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForFailedPayment(account, invoice, tenantContext);

        Assert.assertEquals(email.getSubject(), "Your recent payment");
        Assert.assertTrue(email.getBody().contains("We were not able to process your payment!"));
    }

    public void testPaymentRefund() throws Exception {
        final AccountData account = createAccount();
        final PaymentTransaction paymentTransaction = createPaymentTransaction(new BigDecimal("937.070000000"), Currency.USD);

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForPaymentRefund(account, paymentTransaction, tenantContext);

        Assert.assertEquals(email.getSubject(), "Refund Receipt");
        Assert.assertTrue(email.getBody().contains("Your refund has been processed!"));
    }

    public void testSubscriptionCancellationRequested() throws Exception {
        final AccountData account = createAccount();
        final Subscription cancelledSubscription = createFutureCancelledSubscription(new LocalDate("2015-04-06"), "myPlanName");

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForSubscriptionCancellationRequested(account, cancelledSubscription, tenantContext);

        Assert.assertEquals(email.getSubject(), "Your subscription will be cancelled");
        Assert.assertTrue(email.getBody().contains("The following subscription will be cancelled<"));
    }

    public void testSubscriptionCancellationEffective() throws Exception {
        final AccountData account = createAccount();
        final Subscription cancelledSubscription = createFutureCancelledSubscription(new LocalDate("2015-04-06"), "myPlanName");

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForSubscriptionCancellationEffective(account, cancelledSubscription, tenantContext);

        Assert.assertEquals(email.getSubject(), "Your subscription has been cancelled");
        Assert.assertTrue(email.getBody().contains("The following subscription has been cancelled"));
    }

    public void testUpComingInvoice() throws Exception {
        final AccountData account = createAccount();
        final List<InvoiceItem> items = new ArrayList<InvoiceItem>();
        items.add(createInvoiceItem(InvoiceItemType.RECURRING, new LocalDate("2015-04-06"), new BigDecimal("123.45"), account.getCurrency(), "chocolate-monthly"));
        items.add(createInvoiceItem(InvoiceItemType.TAX, new LocalDate("2015-04-06"), new BigDecimal("7.5500"), account.getCurrency(), "chocolate-monthly"));
        final Invoice invoice = createInvoice(234, new LocalDate("2015-04-06"), new BigDecimal("131.00"), BigDecimal.ZERO, account.getCurrency(), items);

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForUpComingInvoice(account, invoice, tenantContext);

        Assert.assertEquals(email.getSubject(), "Your upcoming invoice");
        Assert.assertTrue(email.getBody().contains("Here&#39;s a preview of your upcoming invoice"));
    }

    public void testInvoiceCreationNewTemplateAndNewTenantVariables() throws Exception {
        final AccountData account = createAccount();
        final List<InvoiceItem> items = new ArrayList<InvoiceItem>();
        items.add(createInvoiceItem(InvoiceItemType.RECURRING, new LocalDate("2015-04-06"), new BigDecimal("123.45"), account.getCurrency(), "chocolate-monthly"));
        items.add(createInvoiceItem(InvoiceItemType.TAX, new LocalDate("2015-04-06"), new BigDecimal("7.5500"), account.getCurrency(), "chocolate-monthly"));
        final Invoice invoice = createInvoice(234, new LocalDate("2015-04-06"), new BigDecimal("131.00"), BigDecimal.ZERO, account.getCurrency(), items);

        final UUID tenantId = UUID.randomUUID();
        final TenantContext tenantContext = new TenantContextImp.Builder<>().withTenantId(tenantId).build();
        final String templateWithNewFields = getResourceBodyString("org/killbill/billing/plugin/notification/templates/InvoiceCreation-new-fields.mustache");
        when(tenantUserApi.getTenantValuesForKey(Mockito.eq("killbill-email-notifications:INVOICE_CREATION_en_US"), Mockito.any())).thenReturn(List.of(templateWithNewFields));
        final String companyInfo = getResourceBodyString("org/killbill/billing/plugin/notification/templates/companyInfo.json");
        when(tenantUserApi.getTenantValuesForKey(Mockito.eq("COMPANY_INFO"), Mockito.any())).thenReturn(List.of(companyInfo));
        final String logoInfo = getResourceBodyString("org/killbill/billing/plugin/notification/templates/logoInfo.json");
        when(tenantUserApi.getTenantValuesForKey(Mockito.eq("EMAIL_TEMPLATE_LOGO_INFO"), Mockito.any())).thenReturn(List.of(logoInfo));
        final String brandInfo = getResourceBodyString("org/killbill/billing/plugin/notification/templates/brandInfo.json");
        when(tenantUserApi.getTenantValuesForKey(Mockito.eq("EMAIL_TEMPLATE_BRAND_INFO"), Mockito.any())).thenReturn(List.of(brandInfo));

        final EmailContent email = renderer.generateEmailForInvoiceCreation(account, invoice, tenantContext);

        Assert.assertEquals(email.getSubject(), "Your recent invoice");
        Assert.assertTrue(email.getBody().contains("Thank you for your prompt payment!"));
        Assert.assertTrue(email.getBody().contains("CloudSprout"));
        Assert.assertTrue(email.getBody().contains("/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAMCAgMCAgMDA"));
        Assert.assertTrue(email.getBody().contains("--text-color: red"));
    }

    public void testCreateInvoiceWithCustomFormatterFactory() throws Exception {
        // GIVEN
        given(invoiceFormatterFactoryRef.getProperty(Constants.SERVICE_ID)).willReturn("foo.bar");
        given(invoiceFormatterFactoryRef.getBundle()).willReturn(bundle);
        given(bundleContext.getService(invoiceFormatterFactoryRef)).willReturn(invoiceFormatterFactory);

        final ServiceTracker<InvoiceFormatterFactory, InvoiceFormatterFactory> tracker = new ServiceTracker<>(
                bundleContext, invoiceFormatterFactoryRef, null);
        renderer.setInvoiceFormatterTracker(tracker);

        final AccountData account = createAccount();
        final Locale accountLocale = LocaleUtils.toLocale(account.getLocale());
        final List<InvoiceItem> items = new ArrayList<InvoiceItem>();
        items.add(createInvoiceItem(InvoiceItemType.RECURRING, new LocalDate("2015-04-06"), new BigDecimal("123.45"), account.getCurrency(), "chocolate-monthly"));
        items.add(createInvoiceItem(InvoiceItemType.TAX, new LocalDate("2015-04-06"), new BigDecimal("7.5500"), account.getCurrency(), "chocolate-monthly"));
        final Invoice invoice = createInvoice(234, new LocalDate("2015-04-06"), new BigDecimal("131.00"), BigDecimal.ZERO, account.getCurrency(), items);
        final TenantContext tenantContext = createTenantContext();

        @SuppressWarnings("unchecked")
        Map<String, String> anyMap = anyMap();
        given(invoiceFormatterFactory.createInvoiceFormatter(anyMap, eq(invoice),
                                                             eq(accountLocale), eq(tenantContext))).willReturn(invoiceFormatter);

        given(invoiceFormatter.getTargetDate()).willReturn(new LocalDate(2020, 7, 16));
        given(invoiceFormatter.getFormattedBalance()).willReturn("FOO$ 9.99");

        // WHEN
        tracker.open();
        final EmailContent email = renderer.generateEmailForInvoiceCreation(account, invoice, tenantContext);

        // THEN
        Assert.assertEquals(email.getSubject(), "Your recent invoice");
        Assert.assertTrue(email.getBody().contains("FOO$ 9.99"));
    }

    private TenantContext createTenantContext() {
        return new TenantContextImp.Builder<>().build();
    }

    private Subscription createFutureCancelledSubscription(final LocalDate chargedThroughDate, final String planName) {
        return new SubscriptionImp.Builder<>().withChargedThroughDate(chargedThroughDate)
                                              .withLastActivePlan(new PlanImp.Builder<>().withName(planName).build())
                                              .build();
    }

    private InvoiceItem createInvoiceItem(final InvoiceItemType type, final LocalDate startDate, final BigDecimal amount, final Currency currency, final String planName) {
        return new InvoiceItemImp.Builder<>().withInvoiceItemType(type)
                                             .withStartDate(startDate)
                                             .withAmount(amount)
                                             .withCurrency(currency)
                                             .withPlanName(planName)
                                             .withPrettyPlanName(planName)
                                             .build();
    }

    private Invoice createInvoice(final Integer invoiceNumber, final LocalDate invoiceDate, final BigDecimal paidAmount, final BigDecimal balance, final Currency currency, final List<InvoiceItem> items) {
        return new InvoiceImp.Builder<>().withInvoiceItems(items)
                                         .withNumberOfItems(items.size())
                                         .withInvoiceNumber(invoiceNumber)
                                         .withInvoiceDate(invoiceDate)
                                         .withTargetDate(invoiceDate)
                                         .withCurrency(currency)
                                         .withPaidAmount(paidAmount)
                                         .withBalance(balance)
                                         .build();
    }

    private AccountData createAccount() {
        return createAccount(Currency.USD, "en_US");
    }

    private AccountData createAccount(final Currency currency, final String locale) {
        return new AccountDataImp.Builder<>().withExternalKey("foo")
                                             .withName("Sylvie Dupond")
                                             .withFirstNameLength(7)
                                             .withEmail("sylvie@banquedefrance.fr")
                                             .withBillCycleDayLocal(1)
                                             .withCurrency(currency)
                                             .withTimeZone(DateTimeZone.UTC)
                                             .withLocale(locale)
                                             .withAddress1("1234 Trumpet street")
                                             .withCompanyName("SauvonsLaTerre")
                                             .withCity("San Francisco")
                                             .withStateOrProvince("CA")
                                             .withPostalCode("94110")
                                             .withCountry("USA")
                                             .withPhone("(415) 255-7654")
                                             .build();
    }

    private PaymentTransaction createPaymentTransaction(final BigDecimal amount, final Currency currency) {
        return new PaymentTransactionImp.Builder<>().withEffectiveDate(DateTime.now())
                                                    .withAmount(amount)
                                                    .withCurrency(currency)
                                                    .withProcessedAmount(new BigDecimal("20.0"))
                                                    .withProcessedCurrency(Currency.USD)
                                                    .build();
    }

    private TenantUserApi getMockTenantUserApi() {
        return new TenantUserApiImp.Builder<>().build();
    }

    private String getResourceBodyString(final String resource) throws IOException {
        final Path resourcePath;
        try {
            resourcePath = Paths.get(Resources.getResource(resource).toURI());
            return Files.readString(resourcePath);
        } catch (final URISyntaxException e) {
            throw new IOException(e);
        }
    }
}
