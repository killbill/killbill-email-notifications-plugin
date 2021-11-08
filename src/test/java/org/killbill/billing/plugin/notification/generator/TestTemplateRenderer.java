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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

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
import org.killbill.billing.invoice.api.formatters.InvoiceFormatter;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionStatus;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.notification.TestBase;
import org.killbill.billing.plugin.notification.api.InvoiceFormatterFactory;
import org.killbill.billing.plugin.notification.email.EmailContent;
import org.killbill.billing.plugin.notification.templates.MustacheTemplateEngine;
import org.killbill.billing.plugin.notification.templates.TemplateEngine;
import org.killbill.billing.plugin.notification.util.LocaleUtils;
import org.killbill.billing.tenant.api.Tenant;
import org.killbill.billing.tenant.api.TenantApiException;
import org.killbill.billing.tenant.api.TenantData;
import org.killbill.billing.tenant.api.TenantUserApi;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.eq;

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

    @BeforeClass(groups = "fast")
    @SuppressWarnings("rawtypes")
    public void beforeClass() throws Exception {
        final TemplateEngine templateEngine = new MustacheTemplateEngine();
        final ResourceBundleFactory bundleFactory = new ResourceBundleFactory(getMockTenantUserApi());
        renderer = new TemplateRenderer(templateEngine, bundleFactory, getMockTenantUserApi());
    }

    @BeforeMethod(groups = "fast")
    public void beforeMethod() {
    	MockitoAnnotations.initMocks(this);
    }
    
    @Test(groups = "fast", enabled = false, description = "JDK dependent")
    public void testSuccessfulPaymentUSD() throws Exception {

        final AccountData account = createAccount();
        final List<InvoiceItem> items = new ArrayList<InvoiceItem>();
        items.add(createInvoiceItem(InvoiceItemType.RECURRING, new LocalDate("2015-04-06"), new BigDecimal("123.45"), account.getCurrency(), "chocolate-monthly"));
        items.add(createInvoiceItem(InvoiceItemType.TAX, new LocalDate("2015-04-06"), new BigDecimal("7.55"), account.getCurrency(), "chocolate-monthly"));
        final Invoice invoice = createInvoice(234, new LocalDate("2015-04-06"), new BigDecimal("131.00"), BigDecimal.ZERO, account.getCurrency(), items);

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForSuccessfulPayment(account, invoice, tenantContext);

        final String expectedBody ="<!doctype html>\r\n" + 
        		"<html>\r\n" + 
        		"<head>\r\n" + 
        		"    <meta charset=\"utf-8\">\r\n" + 
        		"    <title>Invoice</title>\r\n" + 
        		"    <style>\r\n" + 
        		"        /*!\r\n" + 
        		"         * https://www.sparksuite.com/open-source/invoice.html\r\n" + 
        		"         * Licensed under MIT (https://github.com/twbs/bootstrap/blob/master/LICENSE)\r\n" + 
        		"         */\r\n" + 
        		"        .invoice-box{max-width:800px;margin:auto;padding:30px;border:1px solid #eee;box-shadow:0 0 10px rgba(0,0,0,.15);font-size:16px;line-height:24px;font-family:'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif;color:#555}.invoice-box table{width:100%;line-height:inherit;text-align:left}.invoice-box table td{padding:5px;vertical-align:top}.invoice-box table tr td:nth-child(3){text-align:right}.invoice-box table tr.top table td{padding-bottom:20px}.invoice-box table tr.top table td.title{font-size:45px;line-height:45px;color:#333}.invoice-box table tr.information table td{padding-bottom:40px}.invoice-box table tr.heading td{background:#eee;border-bottom:1px solid #ddd;font-weight:700}.invoice-box table tr.details td{padding-bottom:20px}.invoice-box table tr.item td{border-bottom:1px solid #eee}.invoice-box table tr.item.last td{border-bottom:none}.invoice-box table tr.total td:nth-child(3){border-top:2px solid #eee;font-weight:700}@media only screen and (max-width:600px){.invoice-box table tr.top table td{width:100%;display:block;text-align:center}.invoice-box table tr.information table td{width:100%;display:block;text-align:center}}.rtl{direction:rtl;font-family:Tahoma,'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif}.rtl table{text-align:right}.rtl table tr td:nth-child(3){text-align:left}\r\n" + 
        		"    </style>\r\n" + 
        		"</head>\r\n" + 
        		"<body>\r\n" + 
        		"<div class=\"invoice-box\">\r\n" + 
        		"    <table cellpadding=\"0\" cellspacing=\"0\">\r\n" + 
        		"        <tr class=\"top\">\r\n" + 
        		"            <td colspan=\"3\">\r\n" + 
        		"                <table>\r\n" + 
        		"                    <tr>\r\n" + 
        		"                        <td class=\"title\">\r\n" + 
        		"                            <img src=\"https://raw.githubusercontent.com/killbill/killbill-docs/v3/userguide/assets/img/logo.png\" style=\"width:100%; max-width:300px;\">\r\n" + 
        		"                        </td>\r\n" + 
        		"                        <td></td>\r\n" + 
        		"                        <td>\r\n" + 
        		"                            Invoice INV# 234<br>\r\n" + 
        		"                            Invoice Date: Apr 6, 2015\r\n" + 
        		"                        </td>\r\n" + 
        		"                    </tr>\r\n" + 
        		"                </table>\r\n" + 
        		"            </td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"information\">\r\n" + 
        		"            <td colspan=\"3\">\r\n" + 
        		"                <table>\r\n" + 
        		"                    <tr>\r\n" + 
        		"                        <td>\r\n" + 
        		"                            Acme Corporation<br>\r\n" + 
        		"                            57 Academy Drive<br>\r\n" + 
        		"                            Oak Creek, WI 53154<br>\r\n" + 
        		"                            US\r\n" + 
        		"                        </td>\r\n" + 
        		"                        <td></td>\r\n" + 
        		"                        <td>\r\n" + 
        		"                            Sylvie Dupond<br>\r\n" + 
        		"                            SauvonsLaTerre<br>\r\n" + 
        		"                            1234 Trumpet street<br>\r\n" + 
        		"                            San Francisco, CA 94110<br>\r\n" + 
        		"                            USA\r\n" + 
        		"                        </td>\r\n" + 
        		"                    </tr>\r\n" + 
        		"                </table>\r\n" + 
        		"            </td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"heading\">\r\n" + 
        		"            <td>Thank you for your recent payment!</td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"details\">\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"heading\">\r\n" + 
        		"            <td>Service Period</td>\r\n" + 
        		"            <td>Plan</td>\r\n" + 
        		"            <td>Amount</td>\r\n" + 
        		"        </tr>\r\n" + 
        		"            <tr class=\"item last\">\r\n" + 
        		"                <td>Apr 6, 2015</td>\r\n" + 
        		"                <td>chocolate-monthly</td>\r\n" + 
        		"                <td>$123.45</td>\r\n" + 
        		"            </tr>\r\n" + 
        		"            <tr class=\"item last\">\r\n" + 
        		"                <td>Apr 6, 2015</td>\r\n" + 
        		"                <td>chocolate-monthly</td>\r\n" + 
        		"                <td>$7.55</td>\r\n" + 
        		"            </tr>\r\n" + 
        		"        <tr class=\"total\">\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td>Total: $0.00</td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"total\">\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td>Amount Paid: $131.00</td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"total\">\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td>Balance: $0.00</td>\r\n" + 
        		"        </tr>\r\n" + 
        		"    </table>\r\n" + 
        		"</div>\r\n" + 
        		"</body>\r\n" + 
        		"</html>";
        //System.err.println(email.getBody());
        Assert.assertEquals(email.getSubject(), "Your recent payment");
        Assert.assertEquals(email.getBody(), expectedBody);
    }
    
    @Test(groups = "fast", enabled = false, description = "JDK dependent")
    public void testSuccessfulPaymentGBP() throws Exception {

        final AccountData account = createAccount(Currency.GBP, "en_GB");
        final List<InvoiceItem> items = new ArrayList<InvoiceItem>();
        items.add(createInvoiceItem(InvoiceItemType.RECURRING, new LocalDate("2015-04-06"), new BigDecimal("123.45"), account.getCurrency(), "chocolate-monthly"));
        items.add(createInvoiceItem(InvoiceItemType.TAX, new LocalDate("2015-04-06"), new BigDecimal("7.55"), account.getCurrency(), "chocolate-monthly"));
        final Invoice invoice = createInvoice(234, new LocalDate("2015-04-06"), new BigDecimal("131.00"), BigDecimal.ZERO, account.getCurrency(), items);

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForSuccessfulPayment(account, invoice, tenantContext);

        final String expectedBody = "<!doctype html>\r\n" + 
        		"<html>\r\n" + 
        		"<head>\r\n" + 
        		"    <meta charset=\"utf-8\">\r\n" + 
        		"    <title>Invoice</title>\r\n" + 
        		"    <style>\r\n" + 
        		"        /*!\r\n" + 
        		"         * https://www.sparksuite.com/open-source/invoice.html\r\n" + 
        		"         * Licensed under MIT (https://github.com/twbs/bootstrap/blob/master/LICENSE)\r\n" + 
        		"         */\r\n" + 
        		"        .invoice-box{max-width:800px;margin:auto;padding:30px;border:1px solid #eee;box-shadow:0 0 10px rgba(0,0,0,.15);font-size:16px;line-height:24px;font-family:'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif;color:#555}.invoice-box table{width:100%;line-height:inherit;text-align:left}.invoice-box table td{padding:5px;vertical-align:top}.invoice-box table tr td:nth-child(3){text-align:right}.invoice-box table tr.top table td{padding-bottom:20px}.invoice-box table tr.top table td.title{font-size:45px;line-height:45px;color:#333}.invoice-box table tr.information table td{padding-bottom:40px}.invoice-box table tr.heading td{background:#eee;border-bottom:1px solid #ddd;font-weight:700}.invoice-box table tr.details td{padding-bottom:20px}.invoice-box table tr.item td{border-bottom:1px solid #eee}.invoice-box table tr.item.last td{border-bottom:none}.invoice-box table tr.total td:nth-child(3){border-top:2px solid #eee;font-weight:700}@media only screen and (max-width:600px){.invoice-box table tr.top table td{width:100%;display:block;text-align:center}.invoice-box table tr.information table td{width:100%;display:block;text-align:center}}.rtl{direction:rtl;font-family:Tahoma,'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif}.rtl table{text-align:right}.rtl table tr td:nth-child(3){text-align:left}\r\n" + 
        		"    </style>\r\n" + 
        		"</head>\r\n" + 
        		"<body>\r\n" + 
        		"<div class=\"invoice-box\">\r\n" + 
        		"    <table cellpadding=\"0\" cellspacing=\"0\">\r\n" + 
        		"        <tr class=\"top\">\r\n" + 
        		"            <td colspan=\"3\">\r\n" + 
        		"                <table>\r\n" + 
        		"                    <tr>\r\n" + 
        		"                        <td class=\"title\">\r\n" + 
        		"                            <img src=\"https://raw.githubusercontent.com/killbill/killbill-docs/v3/userguide/assets/img/logo.png\" style=\"width:100%; max-width:300px;\">\r\n" + 
        		"                        </td>\r\n" + 
        		"                        <td></td>\r\n" + 
        		"                        <td>\r\n" + 
        		"                            Invoice INV# 234<br>\r\n" + 
        		"                            Invoice Date: 6 Apr 2015\r\n" + 
        		"                        </td>\r\n" + 
        		"                    </tr>\r\n" + 
        		"                </table>\r\n" + 
        		"            </td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"information\">\r\n" + 
        		"            <td colspan=\"3\">\r\n" + 
        		"                <table>\r\n" + 
        		"                    <tr>\r\n" + 
        		"                        <td>\r\n" + 
        		"                            Acme Corporation<br>\r\n" + 
        		"                            57 Academy Drive<br>\r\n" + 
        		"                            Oak Creek, WI 53154<br>\r\n" + 
        		"                            US\r\n" + 
        		"                        </td>\r\n" + 
        		"                        <td></td>\r\n" + 
        		"                        <td>\r\n" + 
        		"                            Sylvie Dupond<br>\r\n" + 
        		"                            SauvonsLaTerre<br>\r\n" + 
        		"                            1234 Trumpet street<br>\r\n" + 
        		"                            San Francisco, CA 94110<br>\r\n" + 
        		"                            USA\r\n" + 
        		"                        </td>\r\n" + 
        		"                    </tr>\r\n" + 
        		"                </table>\r\n" + 
        		"            </td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"heading\">\r\n" + 
        		"            <td>Thank you for your recent payment!</td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"details\">\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"heading\">\r\n" + 
        		"            <td>Service Period</td>\r\n" + 
        		"            <td>Plan</td>\r\n" + 
        		"            <td>Amount</td>\r\n" + 
        		"        </tr>\r\n" + 
        		"            <tr class=\"item last\">\r\n" + 
        		"                <td>6 Apr 2015</td>\r\n" + 
        		"                <td>chocolate-monthly</td>\r\n" + 
        		"                <td>£123.45</td>\r\n" + 
        		"            </tr>\r\n" + 
        		"            <tr class=\"item last\">\r\n" + 
        		"                <td>6 Apr 2015</td>\r\n" + 
        		"                <td>chocolate-monthly</td>\r\n" + 
        		"                <td>£7.55</td>\r\n" + 
        		"            </tr>\r\n" + 
        		"        <tr class=\"total\">\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td>Total: £0.00</td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"total\">\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td>Paid£131.00</td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"total\">\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td>Balance: £0.00</td>\r\n" + 
        		"        </tr>\r\n" + 
        		"    </table>\r\n" + 
        		"</div>\r\n" + 
        		"</body>\r\n" + 
        		"</html>";
        //System.err.println(email.getBody());
       
        
        Assert.assertEquals(email.getSubject(), "Payment Confirmation, Old Boy");
        Assert.assertEquals(email.getBody(), expectedBody);
        
    }
    
    @Test(groups = "fast", enabled = false, description = "JDK dependent")
    public void testFailedPayment() throws Exception {

        final AccountData account = createAccount();
        final List<InvoiceItem> items = new ArrayList<InvoiceItem>();
        items.add(createInvoiceItem(InvoiceItemType.RECURRING, new LocalDate("2015-04-06"), new BigDecimal("123.45"), account.getCurrency(), "chocolate-monthly"));
        items.add(createInvoiceItem(InvoiceItemType.TAX, new LocalDate("2015-04-06"), new BigDecimal("7.55"), account.getCurrency(), "chocolate-monthly"));
        final Invoice invoice = createInvoice(234, new LocalDate("2015-04-06"), new BigDecimal("131.00"), BigDecimal.ZERO, account.getCurrency(), items);

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForFailedPayment(account, invoice, tenantContext);

        final String expectedBody = "<!doctype html>\r\n" + 
        		"<html>\r\n" + 
        		"<head>\r\n" + 
        		"    <meta charset=\"utf-8\">\r\n" + 
        		"    <title>Invoice</title>\r\n" + 
        		"    <style>\r\n" + 
        		"        /*!\r\n" + 
        		"         * https://www.sparksuite.com/open-source/invoice.html\r\n" + 
        		"         * Licensed under MIT (https://github.com/twbs/bootstrap/blob/master/LICENSE)\r\n" + 
        		"         */\r\n" + 
        		"        .invoice-box{max-width:800px;margin:auto;padding:30px;border:1px solid #eee;box-shadow:0 0 10px rgba(0,0,0,.15);font-size:16px;line-height:24px;font-family:'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif;color:#555}.invoice-box table{width:100%;line-height:inherit;text-align:left}.invoice-box table td{padding:5px;vertical-align:top}.invoice-box table tr td:nth-child(3){text-align:right}.invoice-box table tr.top table td{padding-bottom:20px}.invoice-box table tr.top table td.title{font-size:45px;line-height:45px;color:#333}.invoice-box table tr.information table td{padding-bottom:40px}.invoice-box table tr.heading td{background:#eee;border-bottom:1px solid #ddd;font-weight:700}.invoice-box table tr.details td{padding-bottom:20px}.invoice-box table tr.item td{border-bottom:1px solid #eee}.invoice-box table tr.item.last td{border-bottom:none}.invoice-box table tr.total td:nth-child(3){border-top:2px solid #eee;font-weight:700}@media only screen and (max-width:600px){.invoice-box table tr.top table td{width:100%;display:block;text-align:center}.invoice-box table tr.information table td{width:100%;display:block;text-align:center}}.rtl{direction:rtl;font-family:Tahoma,'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif}.rtl table{text-align:right}.rtl table tr td:nth-child(3){text-align:left}\r\n" + 
        		"    </style>\r\n" + 
        		"</head>\r\n" + 
        		"<body>\r\n" + 
        		"<div class=\"invoice-box\">\r\n" + 
        		"    <table cellpadding=\"0\" cellspacing=\"0\">\r\n" + 
        		"        <tr class=\"top\">\r\n" + 
        		"            <td colspan=\"3\">\r\n" + 
        		"                <table>\r\n" + 
        		"                    <tr>\r\n" + 
        		"                        <td class=\"title\">\r\n" + 
        		"                            <img src=\"https://raw.githubusercontent.com/killbill/killbill-docs/v3/userguide/assets/img/logo.png\" style=\"width:100%; max-width:300px;\">\r\n" + 
        		"                        </td>\r\n" + 
        		"                        <td></td>\r\n" + 
        		"                        <td>\r\n" + 
        		"                            Invoice INV# 234<br>\r\n" + 
        		"                            Invoice Date: Apr 6, 2015\r\n" + 
        		"                        </td>\r\n" + 
        		"                    </tr>\r\n" + 
        		"                </table>\r\n" + 
        		"            </td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"information\">\r\n" + 
        		"            <td colspan=\"3\">\r\n" + 
        		"                <table>\r\n" + 
        		"                    <tr>\r\n" + 
        		"                        <td>\r\n" + 
        		"                            Acme Corporation<br>\r\n" + 
        		"                            57 Academy Drive<br>\r\n" + 
        		"                            Oak Creek, WI 53154<br>\r\n" + 
        		"                            US\r\n" + 
        		"                        </td>\r\n" + 
        		"                        <td></td>\r\n" + 
        		"                        <td>\r\n" + 
        		"                            Sylvie Dupond<br>\r\n" + 
        		"                            SauvonsLaTerre<br>\r\n" + 
        		"                            1234 Trumpet street<br>\r\n" + 
        		"                            San Francisco, CA 94110<br>\r\n" + 
        		"                            USA\r\n" + 
        		"                        </td>\r\n" + 
        		"                    </tr>\r\n" + 
        		"                </table>\r\n" + 
        		"            </td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"heading\">\r\n" + 
        		"            <td>We were not able to process your payment!</td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"details\">\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"heading\">\r\n" + 
        		"            <td>Service Period</td>\r\n" + 
        		"            <td>Plan</td>\r\n" + 
        		"            <td>Amount</td>\r\n" + 
        		"        </tr>\r\n" + 
        		"            <tr class=\"item last\">\r\n" + 
        		"                <td>Apr 6, 2015</td>\r\n" + 
        		"                <td>chocolate-monthly</td>\r\n" + 
        		"                <td>$123.45</td>\r\n" + 
        		"            </tr>\r\n" + 
        		"            <tr class=\"item last\">\r\n" + 
        		"                <td>Apr 6, 2015</td>\r\n" + 
        		"                <td>chocolate-monthly</td>\r\n" + 
        		"                <td>$7.55</td>\r\n" + 
        		"            </tr>\r\n" + 
        		"        <tr class=\"total\">\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td>Total: $0.00</td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"total\">\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td>Amount Paid: $131.00</td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"total\">\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td>Balance: $0.00</td>\r\n" + 
        		"        </tr>\r\n" + 
        		"    </table>\r\n" + 
        		"</div>\r\n" + 
        		"</body>\r\n" + 
        		"</html>";

        //System.err.println(email.getBody());
        Assert.assertEquals(email.getSubject(), "Your recent payment");
        Assert.assertEquals(email.getBody(), expectedBody);
    }

    @Test(groups = "fast", enabled = false, description = "JDK dependent")
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
    
    @Test(groups = "fast", enabled = false, description = "JDK dependent")
    public void testSubscriptionCancellationRequested() throws Exception {

        final AccountData account = createAccount();
        final Subscription cancelledSubscription = createFutureCancelledSubscription(new LocalDate("2015-04-06"), "myPlanName");

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForSubscriptionCancellationRequested(account, cancelledSubscription, tenantContext);

        final String expectedBody = "<!doctype html>\r\n" + 
        		"<html>\r\n" + 
        		"<head>\r\n" + 
        		"    <meta charset=\"utf-8\">\r\n" + 
        		"    <title>Subscription</title>\r\n" + 
        		"    <style>\r\n" + 
        		"        /*!\r\n" + 
        		"         * https://www.sparksuite.com/open-source/invoice.html\r\n" + 
        		"         * Licensed under MIT (https://github.com/twbs/bootstrap/blob/master/LICENSE)\r\n" + 
        		"         */\r\n" + 
        		"        .invoice-box{max-width:800px;margin:auto;padding:30px;border:1px solid #eee;box-shadow:0 0 10px rgba(0,0,0,.15);font-size:16px;line-height:24px;font-family:'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif;color:#555}.invoice-box table{width:100%;line-height:inherit;text-align:left}.invoice-box table td{padding:5px;vertical-align:top}.invoice-box table tr td:nth-child(3){text-align:right}.invoice-box table tr.top table td{padding-bottom:20px}.invoice-box table tr.top table td.title{font-size:45px;line-height:45px;color:#333}.invoice-box table tr.information table td{padding-bottom:40px}.invoice-box table tr.heading td{background:#eee;border-bottom:1px solid #ddd;font-weight:700}.invoice-box table tr.details td{padding-bottom:20px}.invoice-box table tr.item td{border-bottom:1px solid #eee}.invoice-box table tr.item.last td{border-bottom:none}.invoice-box table tr.total td:nth-child(3){border-top:2px solid #eee;font-weight:700}@media only screen and (max-width:600px){.invoice-box table tr.top table td{width:100%;display:block;text-align:center}.invoice-box table tr.information table td{width:100%;display:block;text-align:center}}.rtl{direction:rtl;font-family:Tahoma,'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif}.rtl table{text-align:right}.rtl table tr td:nth-child(3){text-align:left}\r\n" + 
        		"    </style>\r\n" + 
        		"</head>\r\n" + 
        		"<body>\r\n" + 
        		"<div class=\"invoice-box\">\r\n" + 
        		"    <table cellpadding=\"0\" cellspacing=\"0\">\r\n" + 
        		"        <tr class=\"top\">\r\n" + 
        		"            <td colspan=\"3\">\r\n" + 
        		"                <table>\r\n" + 
        		"                    <tr>\r\n" + 
        		"                        <td class=\"title\">\r\n" + 
        		"                            <img src=\"https://raw.githubusercontent.com/killbill/killbill-docs/v3/userguide/assets/img/logo.png\" style=\"width:100%; max-width:300px;\">\r\n" + 
        		"                        </td>\r\n" + 
        		"                        <td></td>\r\n" + 
        		"                        <td>\r\n" + 
        		"                            Subscription <br>\r\n" + 
        		"                            End Date: 2015-04-06\r\n" + 
        		"                        </td>\r\n" + 
        		"                    </tr>\r\n" + 
        		"                </table>\r\n" + 
        		"            </td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"information\">\r\n" + 
        		"            <td colspan=\"3\">\r\n" + 
        		"                <table>\r\n" + 
        		"                    <tr>\r\n" + 
        		"                        <td>\r\n" + 
        		"                            Acme Corporation<br>\r\n" + 
        		"                            57 Academy Drive<br>\r\n" + 
        		"                            Oak Creek, WI 53154<br>\r\n" + 
        		"                            US\r\n" + 
        		"                        </td>\r\n" + 
        		"                        <td></td>\r\n" + 
        		"                        <td>\r\n" + 
        		"                            Sylvie Dupond<br>\r\n" + 
        		"                            SauvonsLaTerre<br>\r\n" + 
        		"                            1234 Trumpet street<br>\r\n" + 
        		"                            San Francisco, CA 94110<br>\r\n" + 
        		"                            USA\r\n" + 
        		"                        </td>\r\n" + 
        		"                    </tr>\r\n" + 
        		"                </table>\r\n" + 
        		"            </td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"heading\">\r\n" + 
        		"            <td>The following subscription will be cancelled</td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"details\">\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"total\">\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td>Plan: myPlanName</td>\r\n" + 
        		"        </tr>\r\n" + 
        		"    </table>\r\n" + 
        		"</div>\r\n" + 
        		"</body>\r\n" + 
        		"</html>";

        //System.err.println(email.getBody());
        Assert.assertEquals(email.getSubject(), "Your subscription will be cancelled");
        Assert.assertEquals(email.getBody(), expectedBody);
    }

    
    @Test(groups = "fast", enabled = false, description = "JDK dependent")
    public void testSubscriptionCancellationEffective() throws Exception {

        final AccountData account = createAccount();
        final Subscription cancelledSubscription = createFutureCancelledSubscription(new LocalDate("2015-04-06"), "myPlanName");

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForSubscriptionCancellationEffective(account, cancelledSubscription, tenantContext);

        final String expectedBody = "<!doctype html>\r\n" + 
        		"<html>\r\n" + 
        		"<head>\r\n" + 
        		"    <meta charset=\"utf-8\">\r\n" + 
        		"    <title>Subscription</title>\r\n" + 
        		"    <style>\r\n" + 
        		"        /*!\r\n" + 
        		"         * https://www.sparksuite.com/open-source/invoice.html\r\n" + 
        		"         * Licensed under MIT (https://github.com/twbs/bootstrap/blob/master/LICENSE)\r\n" + 
        		"         */\r\n" + 
        		"        .invoice-box{max-width:800px;margin:auto;padding:30px;border:1px solid #eee;box-shadow:0 0 10px rgba(0,0,0,.15);font-size:16px;line-height:24px;font-family:'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif;color:#555}.invoice-box table{width:100%;line-height:inherit;text-align:left}.invoice-box table td{padding:5px;vertical-align:top}.invoice-box table tr td:nth-child(3){text-align:right}.invoice-box table tr.top table td{padding-bottom:20px}.invoice-box table tr.top table td.title{font-size:45px;line-height:45px;color:#333}.invoice-box table tr.information table td{padding-bottom:40px}.invoice-box table tr.heading td{background:#eee;border-bottom:1px solid #ddd;font-weight:700}.invoice-box table tr.details td{padding-bottom:20px}.invoice-box table tr.item td{border-bottom:1px solid #eee}.invoice-box table tr.item.last td{border-bottom:none}.invoice-box table tr.total td:nth-child(3){border-top:2px solid #eee;font-weight:700}@media only screen and (max-width:600px){.invoice-box table tr.top table td{width:100%;display:block;text-align:center}.invoice-box table tr.information table td{width:100%;display:block;text-align:center}}.rtl{direction:rtl;font-family:Tahoma,'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif}.rtl table{text-align:right}.rtl table tr td:nth-child(3){text-align:left}\r\n" + 
        		"    </style>\r\n" + 
        		"</head>\r\n" + 
        		"<body>\r\n" + 
        		"<div class=\"invoice-box\">\r\n" + 
        		"    <table cellpadding=\"0\" cellspacing=\"0\">\r\n" + 
        		"        <tr class=\"top\">\r\n" + 
        		"            <td colspan=\"3\">\r\n" + 
        		"                <table>\r\n" + 
        		"                    <tr>\r\n" + 
        		"                        <td class=\"title\">\r\n" + 
        		"                            <img src=\"https://raw.githubusercontent.com/killbill/killbill-docs/v3/userguide/assets/img/logo.png\" style=\"width:100%; max-width:300px;\">\r\n" + 
        		"                        </td>\r\n" + 
        		"                        <td></td>\r\n" + 
        		"                        <td>\r\n" + 
        		"                            Subscription <br>\r\n" + 
        		"                            End Date: 2015-04-06\r\n" + 
        		"                        </td>\r\n" + 
        		"                    </tr>\r\n" + 
        		"                </table>\r\n" + 
        		"            </td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"information\">\r\n" + 
        		"            <td colspan=\"3\">\r\n" + 
        		"                <table>\r\n" + 
        		"                    <tr>\r\n" + 
        		"                        <td>\r\n" + 
        		"                            Acme Corporation<br>\r\n" + 
        		"                            57 Academy Drive<br>\r\n" + 
        		"                            Oak Creek, WI 53154<br>\r\n" + 
        		"                            US\r\n" + 
        		"                        </td>\r\n" + 
        		"                        <td></td>\r\n" + 
        		"                        <td>\r\n" + 
        		"                            Sylvie Dupond<br>\r\n" + 
        		"                            SauvonsLaTerre<br>\r\n" + 
        		"                            1234 Trumpet street<br>\r\n" + 
        		"                            San Francisco, CA 94110<br>\r\n" + 
        		"                            USA\r\n" + 
        		"                        </td>\r\n" + 
        		"                    </tr>\r\n" + 
        		"                </table>\r\n" + 
        		"            </td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"heading\">\r\n" + 
        		"            <td>The following subscription has been cancelled</td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"details\">\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"total\">\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td>Plan: myPlanName</td>\r\n" + 
        		"        </tr>\r\n" + 
        		"    </table>\r\n" + 
        		"</div>\r\n" + 
        		"</body>\r\n" + 
        		"</html>";

        //System.err.println(email.getBody());
        Assert.assertEquals(email.getSubject(), "Your subscription has been cancelled");
        Assert.assertEquals(email.getBody(), expectedBody);
    }

    @Test(groups = "fast", enabled = false, description = "JDK dependent")
    public void testUpComingInvoice() throws Exception {

        final AccountData account = createAccount();
        final List<InvoiceItem> items = new ArrayList<InvoiceItem>();
        items.add(createInvoiceItem(InvoiceItemType.RECURRING, new LocalDate("2015-04-06"), new BigDecimal("123.45"), account.getCurrency(), "chocolate-monthly"));
        items.add(createInvoiceItem(InvoiceItemType.TAX, new LocalDate("2015-04-06"), new BigDecimal("7.5500"), account.getCurrency(), "chocolate-monthly"));
        final Invoice invoice = createInvoice(234, new LocalDate("2015-04-06"), new BigDecimal("131.00"), BigDecimal.ZERO, account.getCurrency(), items);

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForUpComingInvoice(account, invoice, tenantContext);
        
        final String expectedBody = "<!doctype html>\r\n" + 
        		"<html>\r\n" + 
        		"<head>\r\n" + 
        		"    <meta charset=\"utf-8\">\r\n" + 
        		"    <title>Invoice</title>\r\n" + 
        		"    <style>\r\n" + 
        		"        /*!\r\n" + 
        		"         * https://www.sparksuite.com/open-source/invoice.html\r\n" + 
        		"         * Licensed under MIT (https://github.com/twbs/bootstrap/blob/master/LICENSE)\r\n" + 
        		"         */\r\n" + 
        		"        .invoice-box{max-width:800px;margin:auto;padding:30px;border:1px solid #eee;box-shadow:0 0 10px rgba(0,0,0,.15);font-size:16px;line-height:24px;font-family:'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif;color:#555}.invoice-box table{width:100%;line-height:inherit;text-align:left}.invoice-box table td{padding:5px;vertical-align:top}.invoice-box table tr td:nth-child(3){text-align:right}.invoice-box table tr.top table td{padding-bottom:20px}.invoice-box table tr.top table td.title{font-size:45px;line-height:45px;color:#333}.invoice-box table tr.information table td{padding-bottom:40px}.invoice-box table tr.heading td{background:#eee;border-bottom:1px solid #ddd;font-weight:700}.invoice-box table tr.details td{padding-bottom:20px}.invoice-box table tr.item td{border-bottom:1px solid #eee}.invoice-box table tr.item.last td{border-bottom:none}.invoice-box table tr.total td:nth-child(3){border-top:2px solid #eee;font-weight:700}@media only screen and (max-width:600px){.invoice-box table tr.top table td{width:100%;display:block;text-align:center}.invoice-box table tr.information table td{width:100%;display:block;text-align:center}}.rtl{direction:rtl;font-family:Tahoma,'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif}.rtl table{text-align:right}.rtl table tr td:nth-child(3){text-align:left}\r\n" + 
        		"    </style>\r\n" + 
        		"</head>\r\n" + 
        		"<body>\r\n" + 
        		"<div class=\"invoice-box\">\r\n" + 
        		"    <table cellpadding=\"0\" cellspacing=\"0\">\r\n" + 
        		"        <tr class=\"top\">\r\n" + 
        		"            <td colspan=\"3\">\r\n" + 
        		"                <table>\r\n" + 
        		"                    <tr>\r\n" + 
        		"                        <td class=\"title\">\r\n" + 
        		"                            <img src=\"https://raw.githubusercontent.com/killbill/killbill-docs/v3/userguide/assets/img/logo.png\" style=\"width:100%; max-width:300px;\">\r\n" + 
        		"                        </td>\r\n" + 
        		"                        <td></td>\r\n" + 
        		"                        <td>\r\n" + 
        		"                            Invoice INV# 234<br>\r\n" + 
        		"                            Invoice Date: Apr 6, 2015\r\n" + 
        		"                        </td>\r\n" + 
        		"                    </tr>\r\n" + 
        		"                </table>\r\n" + 
        		"            </td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"information\">\r\n" + 
        		"            <td colspan=\"3\">\r\n" + 
        		"                <table>\r\n" + 
        		"                    <tr>\r\n" + 
        		"                        <td>\r\n" + 
        		"                            Acme Corporation<br>\r\n" + 
        		"                            57 Academy Drive<br>\r\n" + 
        		"                            Oak Creek, WI 53154<br>\r\n" + 
        		"                            US\r\n" + 
        		"                        </td>\r\n" + 
        		"                        <td></td>\r\n" + 
        		"                        <td>\r\n" + 
        		"                            Sylvie Dupond<br>\r\n" + 
        		"                            SauvonsLaTerre<br>\r\n" + 
        		"                            1234 Trumpet street<br>\r\n" + 
        		"                            San Francisco, CA 94110<br>\r\n" + 
        		"                            USA\r\n" + 
        		"                        </td>\r\n" + 
        		"                    </tr>\r\n" + 
        		"                </table>\r\n" + 
        		"            </td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"heading\">\r\n" + 
        		"            <td>Here&#39;s a preview of your upcoming invoice</td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"details\">\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"heading\">\r\n" + 
        		"            <td>Service Period</td>\r\n" + 
        		"            <td>Plan</td>\r\n" + 
        		"            <td>Amount</td>\r\n" + 
        		"        </tr>\r\n" + 
        		"            <tr class=\"item last\">\r\n" + 
        		"                <td>Apr 6, 2015</td>\r\n" + 
        		"                <td>chocolate-monthly</td>\r\n" + 
        		"                <td>$123.45</td>\r\n" + 
        		"            </tr>\r\n" + 
        		"            <tr class=\"item last\">\r\n" + 
        		"                <td>Apr 6, 2015</td>\r\n" + 
        		"                <td>chocolate-monthly</td>\r\n" + 
        		"                <td>$7.55</td>\r\n" + 
        		"            </tr>\r\n" + 
        		"        <tr class=\"total\">\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td>Total: $0.00</td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"total\">\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td>Amount Paid: $131.00</td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"total\">\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td>Balance: $0.00</td>\r\n" + 
        		"        </tr>\r\n" + 
        		"    </table>\r\n" + 
        		"</div>\r\n" + 
        		"</body>\r\n" + 
        		"</html>";
        //System.err.println(email.getBody());
        Assert.assertEquals(email.getSubject(), "Your upcoming invoice");

        Assert.assertEquals(email.getBody(), expectedBody);
    }
    
    @Test(groups = "fast", enabled = false, description = "JDK dependent")
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

        given(invoiceFormatter.getTargetDate()).willReturn(new LocalDate(2020,7,16));
        given(invoiceFormatter.getFormattedBalance()).willReturn("FOO$ 9.99");

        // WHEN
        tracker.open();
        final EmailContent email = renderer.generateEmailForInvoiceCreation(account, invoice, tenantContext);

        // THEN
        final String expectedBody = "<!doctype html>\r\n" + 
        		"<html>\r\n" + 
        		"<head>\r\n" + 
        		"    <meta charset=\"utf-8\">\r\n" + 
        		"    <title>Invoice</title>\r\n" + 
        		"    <style>\r\n" + 
        		"        /*!\r\n" + 
        		"         * https://www.sparksuite.com/open-source/invoice.html\r\n" + 
        		"         * Licensed under MIT (https://github.com/twbs/bootstrap/blob/master/LICENSE)\r\n" + 
        		"         */\r\n" + 
        		"        .invoice-box{max-width:800px;margin:auto;padding:30px;border:1px solid #eee;box-shadow:0 0 10px rgba(0,0,0,.15);font-size:16px;line-height:24px;font-family:'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif;color:#555}.invoice-box table{width:100%;line-height:inherit;text-align:left}.invoice-box table td{padding:5px;vertical-align:top}.invoice-box table tr td:nth-child(3){text-align:right}.invoice-box table tr.top table td{padding-bottom:20px}.invoice-box table tr.top table td.title{font-size:45px;line-height:45px;color:#333}.invoice-box table tr.information table td{padding-bottom:40px}.invoice-box table tr.heading td{background:#eee;border-bottom:1px solid #ddd;font-weight:700}.invoice-box table tr.details td{padding-bottom:20px}.invoice-box table tr.item td{border-bottom:1px solid #eee}.invoice-box table tr.item.last td{border-bottom:none}.invoice-box table tr.total td:nth-child(3){border-top:2px solid #eee;font-weight:700}@media only screen and (max-width:600px){.invoice-box table tr.top table td{width:100%;display:block;text-align:center}.invoice-box table tr.information table td{width:100%;display:block;text-align:center}}.rtl{direction:rtl;font-family:Tahoma,'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif}.rtl table{text-align:right}.rtl table tr td:nth-child(3){text-align:left}\r\n" + 
        		"    </style>\r\n" + 
        		"</head>\r\n" + 
        		"<body>\r\n" + 
        		"<div class=\"invoice-box\">\r\n" + 
        		"    <table cellpadding=\"0\" cellspacing=\"0\">\r\n" + 
        		"        <tr class=\"top\">\r\n" + 
        		"            <td colspan=\"3\">\r\n" + 
        		"                <table>\r\n" + 
        		"                    <tr>\r\n" + 
        		"                        <td class=\"title\">\r\n" + 
        		"                            <img src=\"https://raw.githubusercontent.com/killbill/killbill-docs/v3/userguide/assets/img/logo.png\" style=\"width:100%; max-width:300px;\">\r\n" + 
        		"                        </td>\r\n" + 
        		"                        <td></td>\r\n" + 
        		"                        <td>\r\n" + 
        		"                            Invoice INV# 0<br>\r\n" + 
        		"                            Invoice Date: \r\n" + 
        		"                        </td>\r\n" + 
        		"                    </tr>\r\n" + 
        		"                </table>\r\n" + 
        		"            </td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"information\">\r\n" + 
        		"            <td colspan=\"3\">\r\n" + 
        		"                <table>\r\n" + 
        		"                    <tr>\r\n" + 
        		"                        <td>\r\n" + 
        		"                            Acme Corporation<br>\r\n" + 
        		"                            57 Academy Drive<br>\r\n" + 
        		"                            Oak Creek, WI 53154<br>\r\n" + 
        		"                            US\r\n" + 
        		"                        </td>\r\n" + 
        		"                        <td></td>\r\n" + 
        		"                        <td>\r\n" + 
        		"                            Sylvie Dupond<br>\r\n" + 
        		"                            SauvonsLaTerre<br>\r\n" + 
        		"                            1234 Trumpet street<br>\r\n" + 
        		"                            San Francisco, CA 94110<br>\r\n" + 
        		"                            USA\r\n" + 
        		"                        </td>\r\n" + 
        		"                    </tr>\r\n" + 
        		"                </table>\r\n" + 
        		"            </td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"heading\">\r\n" + 
        		"            <td>Thank you for your prompt payment!</td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"details\">\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"heading\">\r\n" + 
        		"            <td>Service Period</td>\r\n" + 
        		"            <td>Plan</td>\r\n" + 
        		"            <td>Amount</td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"total\">\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td>Total: </td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"total\">\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td>Amount Paid: </td>\r\n" + 
        		"        </tr>\r\n" + 
        		"        <tr class=\"total\">\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td></td>\r\n" + 
        		"            <td>Balance: FOO$ 9.99</td>\r\n" + 
        		"        </tr>\r\n" + 
        		"    </table>\r\n" + 
        		"</div>\r\n" + 
        		"</body>\r\n" + 
        		"</html>";
      
        Assert.assertEquals(email.getSubject(), "Your recent invoice");
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
                return planName;
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
                return DateTime.now();
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
                return new BigDecimal(20.0);
            }

            @Override
            public Currency getProcessedCurrency() {
                return Currency.USD;
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
