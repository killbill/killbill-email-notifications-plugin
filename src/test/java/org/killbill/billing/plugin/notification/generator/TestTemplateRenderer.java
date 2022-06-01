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
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import org.killbill.billing.tenant.api.TenantUserApi;
import org.killbill.billing.tenant.api.boilerplate.TenantUserApiImp;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.billing.util.callcontext.boilerplate.TenantContextImp;
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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.eq;

@Test(groups = "fast", enabled = false, description = "JDK dependent")
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

    @BeforeMethod
    public void beforeMethod() {
        MockitoAnnotations.initMocks(this);

        final TemplateEngine templateEngine = new MustacheTemplateEngine();
        final ResourceBundleFactory bundleFactory = new ResourceBundleFactory(getMockTenantUserApi());
        renderer = new TemplateRenderer(templateEngine, bundleFactory, getMockTenantUserApi());
    }

    public void testSuccessfulPaymentUSD() throws Exception {
        final AccountData account = createAccount();
        final List<InvoiceItem> items = new ArrayList<InvoiceItem>();
        items.add(createInvoiceItem(InvoiceItemType.RECURRING, new LocalDate("2015-04-06"), new BigDecimal("123.45"), account.getCurrency(), "chocolate-monthly"));
        items.add(createInvoiceItem(InvoiceItemType.TAX, new LocalDate("2015-04-06"), new BigDecimal("7.55"), account.getCurrency(), "chocolate-monthly"));
        final Invoice invoice = createInvoice(234, new LocalDate("2015-04-06"), new BigDecimal("131.00"), BigDecimal.ZERO, account.getCurrency(), items);

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForSuccessfulPayment(account, invoice, tenantContext);

        final String expectedBody = "<!doctype html>\n" +
                                    "<html>\n" +
                                    "<head>\n" +
                                    "    <meta charset=\"utf-8\">\n" +
                                    "    <title>Invoice</title>\n" +
                                    "    <style>\n" +
                                    "        /*!\n" +
                                    "         * https://www.sparksuite.com/open-source/invoice.html\n" +
                                    "         * Licensed under MIT (https://github.com/twbs/bootstrap/blob/master/LICENSE)\n" +
                                    "         */\n" +
                                    "        .invoice-box{max-width:800px;margin:auto;padding:30px;border:1px solid #eee;box-shadow:0 0 10px rgba(0,0,0,.15);font-size:16px;line-height:24px;font-family:'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif;color:#555}.invoice-box table{width:100%;line-height:inherit;text-align:left}.invoice-box table td{padding:5px;vertical-align:top}.invoice-box table tr td:nth-child(3){text-align:right}.invoice-box table tr.top table td{padding-bottom:20px}.invoice-box table tr.top table td.title{font-size:45px;line-height:45px;color:#333}.invoice-box table tr.information table td{padding-bottom:40px}.invoice-box table tr.heading td{background:#eee;border-bottom:1px solid #ddd;font-weight:700}.invoice-box table tr.details td{padding-bottom:20px}.invoice-box table tr.item td{border-bottom:1px solid #eee}.invoice-box table tr.item.last td{border-bottom:none}.invoice-box table tr.total td:nth-child(3){border-top:2px solid #eee;font-weight:700}@media only screen and (max-width:600px){.invoice-box table tr.top table td{width:100%;display:block;text-align:center}.invoice-box table tr.information table td{width:100%;display:block;text-align:center}}.rtl{direction:rtl;font-family:Tahoma,'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif}.rtl table{text-align:right}.rtl table tr td:nth-child(3){text-align:left}\n" +
                                    "    </style>\n" +
                                    "</head>\n" +
                                    "<body>\n" +
                                    "<div class=\"invoice-box\">\n" +
                                    "    <table cellpadding=\"0\" cellspacing=\"0\">\n" +
                                    "        <tr class=\"top\">\n" +
                                    "            <td colspan=\"3\">\n" +
                                    "                <table>\n" +
                                    "                    <tr>\n" +
                                    "                        <td class=\"title\">\n" +
                                    "                            <img src=\"https://raw.githubusercontent.com/killbill/killbill-docs/v3/userguide/assets/img/logo.png\" style=\"width:100%; max-width:300px;\">\n" +
                                    "                        </td>\n" +
                                    "                        <td></td>\n" +
                                    "                        <td>\n" +
                                    "                            Invoice INV# 234<br>\n" +
                                    "                            Invoice Date: Apr 6, 2015\n" +
                                    "                        </td>\n" +
                                    "                    </tr>\n" +
                                    "                </table>\n" +
                                    "            </td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"information\">\n" +
                                    "            <td colspan=\"3\">\n" +
                                    "                <table>\n" +
                                    "                    <tr>\n" +
                                    "                        <td>\n" +
                                    "                            Acme Corporation<br>\n" +
                                    "                            57 Academy Drive<br>\n" +
                                    "                            Oak Creek, WI 53154<br>\n" +
                                    "                            US\n" +
                                    "                        </td>\n" +
                                    "                        <td></td>\n" +
                                    "                        <td>\n" +
                                    "                            Sylvie Dupond<br>\n" +
                                    "                            SauvonsLaTerre<br>\n" +
                                    "                            1234 Trumpet street<br>\n" +
                                    "                            San Francisco, CA 94110<br>\n" +
                                    "                            USA\n" +
                                    "                        </td>\n" +
                                    "                    </tr>\n" +
                                    "                </table>\n" +
                                    "            </td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"heading\">\n" +
                                    "            <td>Thank you for your recent payment!</td>\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"details\">\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"heading\">\n" +
                                    "            <td>Service Period</td>\n" +
                                    "            <td>Plan</td>\n" +
                                    "            <td>Amount</td>\n" +
                                    "        </tr>\n" +
                                    "            <tr class=\"item last\">\n" +
                                    "                <td>Apr 6, 2015</td>\n" +
                                    "                <td>chocolate-monthly</td>\n" +
                                    "                <td>$123.45</td>\n" +
                                    "            </tr>\n" +
                                    "            <tr class=\"item last\">\n" +
                                    "                <td>Apr 6, 2015</td>\n" +
                                    "                <td>chocolate-monthly</td>\n" +
                                    "                <td>$7.55</td>\n" +
                                    "            </tr>\n" +
                                    "        <tr class=\"total\">\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "            <td>Total: $0.00</td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"total\">\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "            <td>Amount Paid: $131.00</td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"total\">\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "            <td>Balance: $0.00</td>\n" +
                                    "        </tr>\n" +
                                    "    </table>\n" +
                                    "</div>\n" +
                                    "</body>\n" +
                                    "</html>";
        //System.err.println(email.getBody());
        Assert.assertEquals(email.getSubject(), "Your recent payment");
        Assert.assertEquals(email.getBody(), expectedBody);
    }

    public void testSuccessfulPaymentGBP() throws Exception {
        final AccountData account = createAccount(Currency.GBP, "en_GB");
        final List<InvoiceItem> items = new ArrayList<InvoiceItem>();
        items.add(createInvoiceItem(InvoiceItemType.RECURRING, new LocalDate("2015-04-06"), new BigDecimal("123.45"), account.getCurrency(), "chocolate-monthly"));
        items.add(createInvoiceItem(InvoiceItemType.TAX, new LocalDate("2015-04-06"), new BigDecimal("7.55"), account.getCurrency(), "chocolate-monthly"));
        final Invoice invoice = createInvoice(234, new LocalDate("2015-04-06"), new BigDecimal("131.00"), BigDecimal.ZERO, account.getCurrency(), items);

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForSuccessfulPayment(account, invoice, tenantContext);

        final String expectedBody = "<!doctype html>\n" +
                                    "<html>\n" +
                                    "<head>\n" +
                                    "    <meta charset=\"utf-8\">\n" +
                                    "    <title>Invoice</title>\n" +
                                    "    <style>\n" +
                                    "        /*!\n" +
                                    "         * https://www.sparksuite.com/open-source/invoice.html\n" +
                                    "         * Licensed under MIT (https://github.com/twbs/bootstrap/blob/master/LICENSE)\n" +
                                    "         */\n" +
                                    "        .invoice-box{max-width:800px;margin:auto;padding:30px;border:1px solid #eee;box-shadow:0 0 10px rgba(0,0,0,.15);font-size:16px;line-height:24px;font-family:'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif;color:#555}.invoice-box table{width:100%;line-height:inherit;text-align:left}.invoice-box table td{padding:5px;vertical-align:top}.invoice-box table tr td:nth-child(3){text-align:right}.invoice-box table tr.top table td{padding-bottom:20px}.invoice-box table tr.top table td.title{font-size:45px;line-height:45px;color:#333}.invoice-box table tr.information table td{padding-bottom:40px}.invoice-box table tr.heading td{background:#eee;border-bottom:1px solid #ddd;font-weight:700}.invoice-box table tr.details td{padding-bottom:20px}.invoice-box table tr.item td{border-bottom:1px solid #eee}.invoice-box table tr.item.last td{border-bottom:none}.invoice-box table tr.total td:nth-child(3){border-top:2px solid #eee;font-weight:700}@media only screen and (max-width:600px){.invoice-box table tr.top table td{width:100%;display:block;text-align:center}.invoice-box table tr.information table td{width:100%;display:block;text-align:center}}.rtl{direction:rtl;font-family:Tahoma,'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif}.rtl table{text-align:right}.rtl table tr td:nth-child(3){text-align:left}\n" +
                                    "    </style>\n" +
                                    "</head>\n" +
                                    "<body>\n" +
                                    "<div class=\"invoice-box\">\n" +
                                    "    <table cellpadding=\"0\" cellspacing=\"0\">\n" +
                                    "        <tr class=\"top\">\n" +
                                    "            <td colspan=\"3\">\n" +
                                    "                <table>\n" +
                                    "                    <tr>\n" +
                                    "                        <td class=\"title\">\n" +
                                    "                            <img src=\"https://raw.githubusercontent.com/killbill/killbill-docs/v3/userguide/assets/img/logo.png\" style=\"width:100%; max-width:300px;\">\n" +
                                    "                        </td>\n" +
                                    "                        <td></td>\n" +
                                    "                        <td>\n" +
                                    "                            Invoice INV# 234<br>\n" +
                                    "                            Invoice Date: 6 Apr 2015\n" +
                                    "                        </td>\n" +
                                    "                    </tr>\n" +
                                    "                </table>\n" +
                                    "            </td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"information\">\n" +
                                    "            <td colspan=\"3\">\n" +
                                    "                <table>\n" +
                                    "                    <tr>\n" +
                                    "                        <td>\n" +
                                    "                            Acme Corporation<br>\n" +
                                    "                            57 Academy Drive<br>\n" +
                                    "                            Oak Creek, WI 53154<br>\n" +
                                    "                            US\n" +
                                    "                        </td>\n" +
                                    "                        <td></td>\n" +
                                    "                        <td>\n" +
                                    "                            Sylvie Dupond<br>\n" +
                                    "                            SauvonsLaTerre<br>\n" +
                                    "                            1234 Trumpet street<br>\n" +
                                    "                            San Francisco, CA 94110<br>\n" +
                                    "                            USA\n" +
                                    "                        </td>\n" +
                                    "                    </tr>\n" +
                                    "                </table>\n" +
                                    "            </td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"heading\">\n" +
                                    "            <td>Thank you for your recent payment!</td>\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"details\">\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"heading\">\n" +
                                    "            <td>Service Period</td>\n" +
                                    "            <td>Plan</td>\n" +
                                    "            <td>Amount</td>\n" +
                                    "        </tr>\n" +
                                    "            <tr class=\"item last\">\n" +
                                    "                <td>6 Apr 2015</td>\n" +
                                    "                <td>chocolate-monthly</td>\n" +
                                    "                <td>£123.45</td>\n" +
                                    "            </tr>\n" +
                                    "            <tr class=\"item last\">\n" +
                                    "                <td>6 Apr 2015</td>\n" +
                                    "                <td>chocolate-monthly</td>\n" +
                                    "                <td>£7.55</td>\n" +
                                    "            </tr>\n" +
                                    "        <tr class=\"total\">\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "            <td>Total: £0.00</td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"total\">\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "            <td>Paid£131.00</td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"total\">\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "            <td>Balance: £0.00</td>\n" +
                                    "        </tr>\n" +
                                    "    </table>\n" +
                                    "</div>\n" +
                                    "</body>\n" +
                                    "</html>";
        //System.err.println(email.getBody());

        Assert.assertEquals(email.getSubject(), "Payment Confirmation, Old Boy");
        Assert.assertEquals(email.getBody(), expectedBody);
    }

    public void testFailedPayment() throws Exception {
        final AccountData account = createAccount();
        final List<InvoiceItem> items = new ArrayList<InvoiceItem>();
        items.add(createInvoiceItem(InvoiceItemType.RECURRING, new LocalDate("2015-04-06"), new BigDecimal("123.45"), account.getCurrency(), "chocolate-monthly"));
        items.add(createInvoiceItem(InvoiceItemType.TAX, new LocalDate("2015-04-06"), new BigDecimal("7.55"), account.getCurrency(), "chocolate-monthly"));
        final Invoice invoice = createInvoice(234, new LocalDate("2015-04-06"), new BigDecimal("131.00"), BigDecimal.ZERO, account.getCurrency(), items);

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForFailedPayment(account, invoice, tenantContext);

        final String expectedBody = "<!doctype html>\n" +
                                    "<html>\n" +
                                    "<head>\n" +
                                    "    <meta charset=\"utf-8\">\n" +
                                    "    <title>Invoice</title>\n" +
                                    "    <style>\n" +
                                    "        /*!\n" +
                                    "         * https://www.sparksuite.com/open-source/invoice.html\n" +
                                    "         * Licensed under MIT (https://github.com/twbs/bootstrap/blob/master/LICENSE)\n" +
                                    "         */\n" +
                                    "        .invoice-box{max-width:800px;margin:auto;padding:30px;border:1px solid #eee;box-shadow:0 0 10px rgba(0,0,0,.15);font-size:16px;line-height:24px;font-family:'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif;color:#555}.invoice-box table{width:100%;line-height:inherit;text-align:left}.invoice-box table td{padding:5px;vertical-align:top}.invoice-box table tr td:nth-child(3){text-align:right}.invoice-box table tr.top table td{padding-bottom:20px}.invoice-box table tr.top table td.title{font-size:45px;line-height:45px;color:#333}.invoice-box table tr.information table td{padding-bottom:40px}.invoice-box table tr.heading td{background:#eee;border-bottom:1px solid #ddd;font-weight:700}.invoice-box table tr.details td{padding-bottom:20px}.invoice-box table tr.item td{border-bottom:1px solid #eee}.invoice-box table tr.item.last td{border-bottom:none}.invoice-box table tr.total td:nth-child(3){border-top:2px solid #eee;font-weight:700}@media only screen and (max-width:600px){.invoice-box table tr.top table td{width:100%;display:block;text-align:center}.invoice-box table tr.information table td{width:100%;display:block;text-align:center}}.rtl{direction:rtl;font-family:Tahoma,'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif}.rtl table{text-align:right}.rtl table tr td:nth-child(3){text-align:left}\n" +
                                    "    </style>\n" +
                                    "</head>\n" +
                                    "<body>\n" +
                                    "<div class=\"invoice-box\">\n" +
                                    "    <table cellpadding=\"0\" cellspacing=\"0\">\n" +
                                    "        <tr class=\"top\">\n" +
                                    "            <td colspan=\"3\">\n" +
                                    "                <table>\n" +
                                    "                    <tr>\n" +
                                    "                        <td class=\"title\">\n" +
                                    "                            <img src=\"https://raw.githubusercontent.com/killbill/killbill-docs/v3/userguide/assets/img/logo.png\" style=\"width:100%; max-width:300px;\">\n" +
                                    "                        </td>\n" +
                                    "                        <td></td>\n" +
                                    "                        <td>\n" +
                                    "                            Invoice INV# 234<br>\n" +
                                    "                            Invoice Date: Apr 6, 2015\n" +
                                    "                        </td>\n" +
                                    "                    </tr>\n" +
                                    "                </table>\n" +
                                    "            </td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"information\">\n" +
                                    "            <td colspan=\"3\">\n" +
                                    "                <table>\n" +
                                    "                    <tr>\n" +
                                    "                        <td>\n" +
                                    "                            Acme Corporation<br>\n" +
                                    "                            57 Academy Drive<br>\n" +
                                    "                            Oak Creek, WI 53154<br>\n" +
                                    "                            US\n" +
                                    "                        </td>\n" +
                                    "                        <td></td>\n" +
                                    "                        <td>\n" +
                                    "                            Sylvie Dupond<br>\n" +
                                    "                            SauvonsLaTerre<br>\n" +
                                    "                            1234 Trumpet street<br>\n" +
                                    "                            San Francisco, CA 94110<br>\n" +
                                    "                            USA\n" +
                                    "                        </td>\n" +
                                    "                    </tr>\n" +
                                    "                </table>\n" +
                                    "            </td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"heading\">\n" +
                                    "            <td>We were not able to process your payment!</td>\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"details\">\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"heading\">\n" +
                                    "            <td>Service Period</td>\n" +
                                    "            <td>Plan</td>\n" +
                                    "            <td>Amount</td>\n" +
                                    "        </tr>\n" +
                                    "            <tr class=\"item last\">\n" +
                                    "                <td>Apr 6, 2015</td>\n" +
                                    "                <td>chocolate-monthly</td>\n" +
                                    "                <td>$123.45</td>\n" +
                                    "            </tr>\n" +
                                    "            <tr class=\"item last\">\n" +
                                    "                <td>Apr 6, 2015</td>\n" +
                                    "                <td>chocolate-monthly</td>\n" +
                                    "                <td>$7.55</td>\n" +
                                    "            </tr>\n" +
                                    "        <tr class=\"total\">\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "            <td>Total: $0.00</td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"total\">\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "            <td>Amount Paid: $131.00</td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"total\">\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "            <td>Balance: $0.00</td>\n" +
                                    "        </tr>\n" +
                                    "    </table>\n" +
                                    "</div>\n" +
                                    "</body>\n" +
                                    "</html>";

        //System.err.println(email.getBody());
        Assert.assertEquals(email.getSubject(), "Your recent payment");
        Assert.assertEquals(email.getBody(), expectedBody);
    }

    public void testPaymentRefund() throws Exception {
        final AccountData account = createAccount();
        final PaymentTransaction paymentTransaction = createPaymentTransaction(new BigDecimal("937.070000000"), Currency.USD);

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForPaymentRefund(account, paymentTransaction, tenantContext);

        final String expectedBody = "<!doctype html>\n" +
                                    "<html>\n" +
                                    "<head>\n" +
                                    "    <meta charset=\"utf-8\">\n" +
                                    "    <title>Payment</title>\n" +
                                    "    <style>\n" +
                                    "        /*!\n" +
                                    "         * https://www.sparksuite.com/open-source/invoice.html\n" +
                                    "         * Licensed under MIT (https://github.com/twbs/bootstrap/blob/master/LICENSE)\n" +
                                    "         */\n" +
                                    "        .invoice-box{max-width:800px;margin:auto;padding:30px;border:1px solid #eee;box-shadow:0 0 10px rgba(0,0,0,.15);font-size:16px;line-height:24px;font-family:'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif;color:#555}.invoice-box table{width:100%;line-height:inherit;text-align:left}.invoice-box table td{padding:5px;vertical-align:top}.invoice-box table tr td:nth-child(3){text-align:right}.invoice-box table tr.top table td{padding-bottom:20px}.invoice-box table tr.top table td.title{font-size:45px;line-height:45px;color:#333}.invoice-box table tr.information table td{padding-bottom:40px}.invoice-box table tr.heading td{background:#eee;border-bottom:1px solid #ddd;font-weight:700}.invoice-box table tr.details td{padding-bottom:20px}.invoice-box table tr.item td{border-bottom:1px solid #eee}.invoice-box table tr.item.last td{border-bottom:none}.invoice-box table tr.total td:nth-child(3){border-top:2px solid #eee;font-weight:700}@media only screen and (max-width:600px){.invoice-box table tr.top table td{width:100%;display:block;text-align:center}.invoice-box table tr.information table td{width:100%;display:block;text-align:center}}.rtl{direction:rtl;font-family:Tahoma,'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif}.rtl table{text-align:right}.rtl table tr td:nth-child(3){text-align:left}\n" +
                                    "    </style>\n" +
                                    "</head>\n" +
                                    "<body>\n" +
                                    "<div class=\"invoice-box\">\n" +
                                    "    <table cellpadding=\"0\" cellspacing=\"0\">\n" +
                                    "        <tr class=\"top\">\n" +
                                    "            <td colspan=\"3\">\n" +
                                    "                <table>\n" +
                                    "                    <tr>\n" +
                                    "                        <td class=\"title\">\n" +
                                    "                            <img src=\"https://raw.githubusercontent.com/killbill/killbill-docs/v3/userguide/assets/img/logo.png\" style=\"width:100%; max-width:300px;\">\n" +
                                    "                        </td>\n" +
                                    "                        <td></td>\n" +
                                    "                        <td>\n" +
                                    "                            Payment <br>\n" +
                                    "                            Payment Date: Jun 1, 2022\n" +
                                    "                        </td>\n" +
                                    "                    </tr>\n" +
                                    "                </table>\n" +
                                    "            </td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"information\">\n" +
                                    "            <td colspan=\"3\">\n" +
                                    "                <table>\n" +
                                    "                    <tr>\n" +
                                    "                        <td>\n" +
                                    "                            Acme Corporation<br>\n" +
                                    "                            57 Academy Drive<br>\n" +
                                    "                            Oak Creek, WI 53154<br>\n" +
                                    "                            US\n" +
                                    "                        </td>\n" +
                                    "                        <td></td>\n" +
                                    "                        <td>\n" +
                                    "                            Sylvie Dupond<br>\n" +
                                    "                            SauvonsLaTerre<br>\n" +
                                    "                            1234 Trumpet street<br>\n" +
                                    "                            San Francisco, CA 94110<br>\n" +
                                    "                            USA\n" +
                                    "                        </td>\n" +
                                    "                    </tr>\n" +
                                    "                </table>\n" +
                                    "            </td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"heading\">\n" +
                                    "            <td>Your refund has been processed!</td>\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"details\">\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"total\">\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "            <td>Total: $20.00</td>\n" +
                                    "        </tr>\n" +
                                    "    </table>\n" +
                                    "</div>\n" +
                                    "</body>\n" +
                                    "</html>";

        // System.err.println(email.getBody());
        Assert.assertEquals(email.getSubject(), "Refund Receipt");
        Assert.assertEquals(email.getBody(), expectedBody);
    }

    public void testSubscriptionCancellationRequested() throws Exception {
        final AccountData account = createAccount();
        final Subscription cancelledSubscription = createFutureCancelledSubscription(new LocalDate("2015-04-06"), "myPlanName");

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForSubscriptionCancellationRequested(account, cancelledSubscription, tenantContext);

        final String expectedBody = "<!doctype html>\n" +
                                    "<html>\n" +
                                    "<head>\n" +
                                    "    <meta charset=\"utf-8\">\n" +
                                    "    <title>Subscription</title>\n" +
                                    "    <style>\n" +
                                    "        /*!\n" +
                                    "         * https://www.sparksuite.com/open-source/invoice.html\n" +
                                    "         * Licensed under MIT (https://github.com/twbs/bootstrap/blob/master/LICENSE)\n" +
                                    "         */\n" +
                                    "        .invoice-box{max-width:800px;margin:auto;padding:30px;border:1px solid #eee;box-shadow:0 0 10px rgba(0,0,0,.15);font-size:16px;line-height:24px;font-family:'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif;color:#555}.invoice-box table{width:100%;line-height:inherit;text-align:left}.invoice-box table td{padding:5px;vertical-align:top}.invoice-box table tr td:nth-child(3){text-align:right}.invoice-box table tr.top table td{padding-bottom:20px}.invoice-box table tr.top table td.title{font-size:45px;line-height:45px;color:#333}.invoice-box table tr.information table td{padding-bottom:40px}.invoice-box table tr.heading td{background:#eee;border-bottom:1px solid #ddd;font-weight:700}.invoice-box table tr.details td{padding-bottom:20px}.invoice-box table tr.item td{border-bottom:1px solid #eee}.invoice-box table tr.item.last td{border-bottom:none}.invoice-box table tr.total td:nth-child(3){border-top:2px solid #eee;font-weight:700}@media only screen and (max-width:600px){.invoice-box table tr.top table td{width:100%;display:block;text-align:center}.invoice-box table tr.information table td{width:100%;display:block;text-align:center}}.rtl{direction:rtl;font-family:Tahoma,'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif}.rtl table{text-align:right}.rtl table tr td:nth-child(3){text-align:left}\n" +
                                    "    </style>\n" +
                                    "</head>\n" +
                                    "<body>\n" +
                                    "<div class=\"invoice-box\">\n" +
                                    "    <table cellpadding=\"0\" cellspacing=\"0\">\n" +
                                    "        <tr class=\"top\">\n" +
                                    "            <td colspan=\"3\">\n" +
                                    "                <table>\n" +
                                    "                    <tr>\n" +
                                    "                        <td class=\"title\">\n" +
                                    "                            <img src=\"https://raw.githubusercontent.com/killbill/killbill-docs/v3/userguide/assets/img/logo.png\" style=\"width:100%; max-width:300px;\">\n" +
                                    "                        </td>\n" +
                                    "                        <td></td>\n" +
                                    "                        <td>\n" +
                                    "                            Subscription <br>\n" +
                                    "                            End Date: 2015-04-06\n" +
                                    "                        </td>\n" +
                                    "                    </tr>\n" +
                                    "                </table>\n" +
                                    "            </td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"information\">\n" +
                                    "            <td colspan=\"3\">\n" +
                                    "                <table>\n" +
                                    "                    <tr>\n" +
                                    "                        <td>\n" +
                                    "                            Acme Corporation<br>\n" +
                                    "                            57 Academy Drive<br>\n" +
                                    "                            Oak Creek, WI 53154<br>\n" +
                                    "                            US\n" +
                                    "                        </td>\n" +
                                    "                        <td></td>\n" +
                                    "                        <td>\n" +
                                    "                            Sylvie Dupond<br>\n" +
                                    "                            SauvonsLaTerre<br>\n" +
                                    "                            1234 Trumpet street<br>\n" +
                                    "                            San Francisco, CA 94110<br>\n" +
                                    "                            USA\n" +
                                    "                        </td>\n" +
                                    "                    </tr>\n" +
                                    "                </table>\n" +
                                    "            </td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"heading\">\n" +
                                    "            <td>The following subscription will be cancelled</td>\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"details\">\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"total\">\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "            <td>Plan: myPlanName</td>\n" +
                                    "        </tr>\n" +
                                    "    </table>\n" +
                                    "</div>\n" +
                                    "</body>\n" +
                                    "</html>";

        //System.err.println(email.getBody());
        Assert.assertEquals(email.getSubject(), "Your subscription will be cancelled");
        Assert.assertEquals(email.getBody(), expectedBody);
    }

    public void testSubscriptionCancellationEffective() throws Exception {
        final AccountData account = createAccount();
        final Subscription cancelledSubscription = createFutureCancelledSubscription(new LocalDate("2015-04-06"), "myPlanName");

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForSubscriptionCancellationEffective(account, cancelledSubscription, tenantContext);

        final String expectedBody = "<!doctype html>\n" +
                                    "<html>\n" +
                                    "<head>\n" +
                                    "    <meta charset=\"utf-8\">\n" +
                                    "    <title>Subscription</title>\n" +
                                    "    <style>\n" +
                                    "        /*!\n" +
                                    "         * https://www.sparksuite.com/open-source/invoice.html\n" +
                                    "         * Licensed under MIT (https://github.com/twbs/bootstrap/blob/master/LICENSE)\n" +
                                    "         */\n" +
                                    "        .invoice-box{max-width:800px;margin:auto;padding:30px;border:1px solid #eee;box-shadow:0 0 10px rgba(0,0,0,.15);font-size:16px;line-height:24px;font-family:'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif;color:#555}.invoice-box table{width:100%;line-height:inherit;text-align:left}.invoice-box table td{padding:5px;vertical-align:top}.invoice-box table tr td:nth-child(3){text-align:right}.invoice-box table tr.top table td{padding-bottom:20px}.invoice-box table tr.top table td.title{font-size:45px;line-height:45px;color:#333}.invoice-box table tr.information table td{padding-bottom:40px}.invoice-box table tr.heading td{background:#eee;border-bottom:1px solid #ddd;font-weight:700}.invoice-box table tr.details td{padding-bottom:20px}.invoice-box table tr.item td{border-bottom:1px solid #eee}.invoice-box table tr.item.last td{border-bottom:none}.invoice-box table tr.total td:nth-child(3){border-top:2px solid #eee;font-weight:700}@media only screen and (max-width:600px){.invoice-box table tr.top table td{width:100%;display:block;text-align:center}.invoice-box table tr.information table td{width:100%;display:block;text-align:center}}.rtl{direction:rtl;font-family:Tahoma,'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif}.rtl table{text-align:right}.rtl table tr td:nth-child(3){text-align:left}\n" +
                                    "    </style>\n" +
                                    "</head>\n" +
                                    "<body>\n" +
                                    "<div class=\"invoice-box\">\n" +
                                    "    <table cellpadding=\"0\" cellspacing=\"0\">\n" +
                                    "        <tr class=\"top\">\n" +
                                    "            <td colspan=\"3\">\n" +
                                    "                <table>\n" +
                                    "                    <tr>\n" +
                                    "                        <td class=\"title\">\n" +
                                    "                            <img src=\"https://raw.githubusercontent.com/killbill/killbill-docs/v3/userguide/assets/img/logo.png\" style=\"width:100%; max-width:300px;\">\n" +
                                    "                        </td>\n" +
                                    "                        <td></td>\n" +
                                    "                        <td>\n" +
                                    "                            Subscription <br>\n" +
                                    "                            End Date: 2015-04-06\n" +
                                    "                        </td>\n" +
                                    "                    </tr>\n" +
                                    "                </table>\n" +
                                    "            </td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"information\">\n" +
                                    "            <td colspan=\"3\">\n" +
                                    "                <table>\n" +
                                    "                    <tr>\n" +
                                    "                        <td>\n" +
                                    "                            Acme Corporation<br>\n" +
                                    "                            57 Academy Drive<br>\n" +
                                    "                            Oak Creek, WI 53154<br>\n" +
                                    "                            US\n" +
                                    "                        </td>\n" +
                                    "                        <td></td>\n" +
                                    "                        <td>\n" +
                                    "                            Sylvie Dupond<br>\n" +
                                    "                            SauvonsLaTerre<br>\n" +
                                    "                            1234 Trumpet street<br>\n" +
                                    "                            San Francisco, CA 94110<br>\n" +
                                    "                            USA\n" +
                                    "                        </td>\n" +
                                    "                    </tr>\n" +
                                    "                </table>\n" +
                                    "            </td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"heading\">\n" +
                                    "            <td>The following subscription has been cancelled</td>\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"details\">\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"total\">\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "            <td>Plan: myPlanName</td>\n" +
                                    "        </tr>\n" +
                                    "    </table>\n" +
                                    "</div>\n" +
                                    "</body>\n" +
                                    "</html>";

        //System.err.println(email.getBody());
        Assert.assertEquals(email.getSubject(), "Your subscription has been cancelled");
        Assert.assertEquals(email.getBody(), expectedBody);
    }

    public void testUpComingInvoice() throws Exception {
        final AccountData account = createAccount();
        final List<InvoiceItem> items = new ArrayList<InvoiceItem>();
        items.add(createInvoiceItem(InvoiceItemType.RECURRING, new LocalDate("2015-04-06"), new BigDecimal("123.45"), account.getCurrency(), "chocolate-monthly"));
        items.add(createInvoiceItem(InvoiceItemType.TAX, new LocalDate("2015-04-06"), new BigDecimal("7.5500"), account.getCurrency(), "chocolate-monthly"));
        final Invoice invoice = createInvoice(234, new LocalDate("2015-04-06"), new BigDecimal("131.00"), BigDecimal.ZERO, account.getCurrency(), items);

        final TenantContext tenantContext = createTenantContext();
        final EmailContent email = renderer.generateEmailForUpComingInvoice(account, invoice, tenantContext);

        final String expectedBody = "<!doctype html>\n" +
                                    "<html>\n" +
                                    "<head>\n" +
                                    "    <meta charset=\"utf-8\">\n" +
                                    "    <title>Invoice</title>\n" +
                                    "    <style>\n" +
                                    "        /*!\n" +
                                    "         * https://www.sparksuite.com/open-source/invoice.html\n" +
                                    "         * Licensed under MIT (https://github.com/twbs/bootstrap/blob/master/LICENSE)\n" +
                                    "         */\n" +
                                    "        .invoice-box{max-width:800px;margin:auto;padding:30px;border:1px solid #eee;box-shadow:0 0 10px rgba(0,0,0,.15);font-size:16px;line-height:24px;font-family:'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif;color:#555}.invoice-box table{width:100%;line-height:inherit;text-align:left}.invoice-box table td{padding:5px;vertical-align:top}.invoice-box table tr td:nth-child(3){text-align:right}.invoice-box table tr.top table td{padding-bottom:20px}.invoice-box table tr.top table td.title{font-size:45px;line-height:45px;color:#333}.invoice-box table tr.information table td{padding-bottom:40px}.invoice-box table tr.heading td{background:#eee;border-bottom:1px solid #ddd;font-weight:700}.invoice-box table tr.details td{padding-bottom:20px}.invoice-box table tr.item td{border-bottom:1px solid #eee}.invoice-box table tr.item.last td{border-bottom:none}.invoice-box table tr.total td:nth-child(3){border-top:2px solid #eee;font-weight:700}@media only screen and (max-width:600px){.invoice-box table tr.top table td{width:100%;display:block;text-align:center}.invoice-box table tr.information table td{width:100%;display:block;text-align:center}}.rtl{direction:rtl;font-family:Tahoma,'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif}.rtl table{text-align:right}.rtl table tr td:nth-child(3){text-align:left}\n" +
                                    "    </style>\n" +
                                    "</head>\n" +
                                    "<body>\n" +
                                    "<div class=\"invoice-box\">\n" +
                                    "    <table cellpadding=\"0\" cellspacing=\"0\">\n" +
                                    "        <tr class=\"top\">\n" +
                                    "            <td colspan=\"3\">\n" +
                                    "                <table>\n" +
                                    "                    <tr>\n" +
                                    "                        <td class=\"title\">\n" +
                                    "                            <img src=\"https://raw.githubusercontent.com/killbill/killbill-docs/v3/userguide/assets/img/logo.png\" style=\"width:100%; max-width:300px;\">\n" +
                                    "                        </td>\n" +
                                    "                        <td></td>\n" +
                                    "                        <td>\n" +
                                    "                            Invoice INV# 234<br>\n" +
                                    "                            Invoice Date: Apr 6, 2015\n" +
                                    "                        </td>\n" +
                                    "                    </tr>\n" +
                                    "                </table>\n" +
                                    "            </td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"information\">\n" +
                                    "            <td colspan=\"3\">\n" +
                                    "                <table>\n" +
                                    "                    <tr>\n" +
                                    "                        <td>\n" +
                                    "                            Acme Corporation<br>\n" +
                                    "                            57 Academy Drive<br>\n" +
                                    "                            Oak Creek, WI 53154<br>\n" +
                                    "                            US\n" +
                                    "                        </td>\n" +
                                    "                        <td></td>\n" +
                                    "                        <td>\n" +
                                    "                            Sylvie Dupond<br>\n" +
                                    "                            SauvonsLaTerre<br>\n" +
                                    "                            1234 Trumpet street<br>\n" +
                                    "                            San Francisco, CA 94110<br>\n" +
                                    "                            USA\n" +
                                    "                        </td>\n" +
                                    "                    </tr>\n" +
                                    "                </table>\n" +
                                    "            </td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"heading\">\n" +
                                    "            <td>Here&#39;s a preview of your upcoming invoice</td>\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"details\">\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"heading\">\n" +
                                    "            <td>Service Period</td>\n" +
                                    "            <td>Plan</td>\n" +
                                    "            <td>Amount</td>\n" +
                                    "        </tr>\n" +
                                    "            <tr class=\"item last\">\n" +
                                    "                <td>Apr 6, 2015</td>\n" +
                                    "                <td>chocolate-monthly</td>\n" +
                                    "                <td>$123.45</td>\n" +
                                    "            </tr>\n" +
                                    "            <tr class=\"item last\">\n" +
                                    "                <td>Apr 6, 2015</td>\n" +
                                    "                <td>chocolate-monthly</td>\n" +
                                    "                <td>$7.55</td>\n" +
                                    "            </tr>\n" +
                                    "        <tr class=\"total\">\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "            <td>Total: $0.00</td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"total\">\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "            <td>Amount Paid: $131.00</td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"total\">\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "            <td>Balance: $0.00</td>\n" +
                                    "        </tr>\n" +
                                    "    </table>\n" +
                                    "</div>\n" +
                                    "</body>\n" +
                                    "</html>";
        //System.err.println(email.getBody());
        Assert.assertEquals(email.getSubject(), "Your upcoming invoice");

        Assert.assertEquals(email.getBody(), expectedBody);
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
        final String expectedBody = "<!doctype html>\n" +
                                    "<html>\n" +
                                    "<head>\n" +
                                    "    <meta charset=\"utf-8\">\n" +
                                    "    <title>Invoice</title>\n" +
                                    "    <style>\n" +
                                    "        /*!\n" +
                                    "         * https://www.sparksuite.com/open-source/invoice.html\n" +
                                    "         * Licensed under MIT (https://github.com/twbs/bootstrap/blob/master/LICENSE)\n" +
                                    "         */\n" +
                                    "        .invoice-box{max-width:800px;margin:auto;padding:30px;border:1px solid #eee;box-shadow:0 0 10px rgba(0,0,0,.15);font-size:16px;line-height:24px;font-family:'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif;color:#555}.invoice-box table{width:100%;line-height:inherit;text-align:left}.invoice-box table td{padding:5px;vertical-align:top}.invoice-box table tr td:nth-child(3){text-align:right}.invoice-box table tr.top table td{padding-bottom:20px}.invoice-box table tr.top table td.title{font-size:45px;line-height:45px;color:#333}.invoice-box table tr.information table td{padding-bottom:40px}.invoice-box table tr.heading td{background:#eee;border-bottom:1px solid #ddd;font-weight:700}.invoice-box table tr.details td{padding-bottom:20px}.invoice-box table tr.item td{border-bottom:1px solid #eee}.invoice-box table tr.item.last td{border-bottom:none}.invoice-box table tr.total td:nth-child(3){border-top:2px solid #eee;font-weight:700}@media only screen and (max-width:600px){.invoice-box table tr.top table td{width:100%;display:block;text-align:center}.invoice-box table tr.information table td{width:100%;display:block;text-align:center}}.rtl{direction:rtl;font-family:Tahoma,'Helvetica Neue',Helvetica,Helvetica,Arial,sans-serif}.rtl table{text-align:right}.rtl table tr td:nth-child(3){text-align:left}\n" +
                                    "    </style>\n" +
                                    "</head>\n" +
                                    "<body>\n" +
                                    "<div class=\"invoice-box\">\n" +
                                    "    <table cellpadding=\"0\" cellspacing=\"0\">\n" +
                                    "        <tr class=\"top\">\n" +
                                    "            <td colspan=\"3\">\n" +
                                    "                <table>\n" +
                                    "                    <tr>\n" +
                                    "                        <td class=\"title\">\n" +
                                    "                            <img src=\"https://raw.githubusercontent.com/killbill/killbill-docs/v3/userguide/assets/img/logo.png\" style=\"width:100%; max-width:300px;\">\n" +
                                    "                        </td>\n" +
                                    "                        <td></td>\n" +
                                    "                        <td>\n" +
                                    "                            Invoice INV# 0<br>\n" +
                                    "                            Invoice Date: \n" +
                                    "                        </td>\n" +
                                    "                    </tr>\n" +
                                    "                </table>\n" +
                                    "            </td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"information\">\n" +
                                    "            <td colspan=\"3\">\n" +
                                    "                <table>\n" +
                                    "                    <tr>\n" +
                                    "                        <td>\n" +
                                    "                            Acme Corporation<br>\n" +
                                    "                            57 Academy Drive<br>\n" +
                                    "                            Oak Creek, WI 53154<br>\n" +
                                    "                            US\n" +
                                    "                        </td>\n" +
                                    "                        <td></td>\n" +
                                    "                        <td>\n" +
                                    "                            Sylvie Dupond<br>\n" +
                                    "                            SauvonsLaTerre<br>\n" +
                                    "                            1234 Trumpet street<br>\n" +
                                    "                            San Francisco, CA 94110<br>\n" +
                                    "                            USA\n" +
                                    "                        </td>\n" +
                                    "                    </tr>\n" +
                                    "                </table>\n" +
                                    "            </td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"heading\">\n" +
                                    "            <td>Thank you for your prompt payment!</td>\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"details\">\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"heading\">\n" +
                                    "            <td>Service Period</td>\n" +
                                    "            <td>Plan</td>\n" +
                                    "            <td>Amount</td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"total\">\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "            <td>Total: </td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"total\">\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "            <td>Amount Paid: </td>\n" +
                                    "        </tr>\n" +
                                    "        <tr class=\"total\">\n" +
                                    "            <td></td>\n" +
                                    "            <td></td>\n" +
                                    "            <td>Balance: FOO$ 9.99</td>\n" +
                                    "        </tr>\n" +
                                    "    </table>\n" +
                                    "</div>\n" +
                                    "</body>\n" +
                                    "</html>";

        Assert.assertEquals(email.getSubject(), "Your recent invoice");
        Assert.assertEquals(email.getBody(), expectedBody);
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
}
