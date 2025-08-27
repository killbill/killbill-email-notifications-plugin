package org.killbill.billing.plugin.notification.generator.formatters.tygrys;

/*
 * Copyright 2025 Tigase Inc. 
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

import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.net.http.HttpResponse;
import java.util.ResourceBundle;
import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.killbill.billing.client.KillBillHttpClient;
import org.killbill.billing.client.model.gen.CustomField;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.killbill.billing.invoice.api.formatters.InvoiceFormatter;
import org.killbill.billing.invoice.template.formatters.DefaultInvoiceFormatter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.killbill.billing.client.RequestOptions;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.client.KillBillClientException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.killbill.billing.currency.api.CurrencyConversionApi;

public class TygrysInvoiceFormatter extends DefaultInvoiceFormatter {

    public static final String killbillLOCAL_URL = "http://localhost:8080";
    public static final String killbillENDPOINT = "/1.0/kb/customFields";
    public static final String killbillINVOICE_ENDPOINT = "/1.0/kb/invoices";
    public static final String killbillCUSTOM_FIELD_INVOICE_NAME = "name";
    public static final String invoiceNameUNNAMED_INVOICE = "<Unnamed Invoice>";
    public static final String tygrysKILLBILL_DEFAULT_USER = "tygrys";
    public static final String tygrysKILLBILL_DEFAULT_PASSWD = "TBD";
    public static final String tygrysKILLBILL_API_KEY = "TBD";
    public static final String tygrysKILLBILL_API_SECRET = "TBD";

    private static final DateTimeFormatter DEFAULT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Locale DEFAULT_LOCALE = Locale.US;

    private KillBillHttpClient killBillClient;
    private static final Logger logger = LoggerFactory.getLogger(TygrysInvoiceFormatter.class);

    private Invoice invoice;
    public Invoice getInvoice() { return this.invoice; }
    public void setInvoice(Invoice givenInvoice) { this.invoice = givenInvoice; }

    // Custom Invoice Attributes
    private String subscriptionName;

    public TygrysInvoiceFormatter(final String defaultLocale, final String catalogBundlePath, final Invoice invoice, Locale locale, final CurrencyConversionApi currencyConversionApi, final ResourceBundle bundle, final ResourceBundle defaultBundle, KillBillHttpClient killBillClient) throws Exception {
        super(defaultLocale, catalogBundlePath, invoice, locale, currencyConversionApi, bundle, defaultBundle);
        this.killBillClient = killBillClient;
        setInvoice(invoice);
        logger.info("Instantiating TygrysInvoiceFormatter");
    }

    public TygrysInvoiceFormatter(final String defaultLocale, final String catalogBundlePath, final Invoice invoice, Locale locale, final CurrencyConversionApi currencyConversionApi, final ResourceBundle bundle, final ResourceBundle defaultBundle, final String killbillUrl, final String user, final String pwd, final String apiKey, final String apiSecret) throws Exception {
        this(defaultLocale, defaultLocale, invoice, locale, currencyConversionApi, bundle, defaultBundle, new KillBillHttpClient(killbillUrl, user, pwd, apiKey, apiSecret));
    }

    public TygrysInvoiceFormatter(final String defaultLocale, final String catalogBundlePath, final Invoice invoice, Locale locale, final CurrencyConversionApi currencyConversionApi, final ResourceBundle bundle, final ResourceBundle defaultBundle, final String user, final String pwd, final String apiKey, final String apiSecret) throws Exception {
        this(defaultLocale, catalogBundlePath, invoice, locale, currencyConversionApi, bundle, defaultBundle, killbillLOCAL_URL, user, pwd, apiKey, apiSecret);
    }

    public TygrysInvoiceFormatter(final String defaultLocale, final String catalogBundlePath, final Invoice invoice, final CurrencyConversionApi currencyConversionApi, Locale locale,  final ResourceBundle bundle, final ResourceBundle defaultBundle)  throws Exception {
        this(defaultLocale, catalogBundlePath, 
              invoice, locale, currencyConversionApi, bundle, defaultBundle, 
                tygrysKILLBILL_DEFAULT_USER, tygrysKILLBILL_DEFAULT_PASSWD, tygrysKILLBILL_API_KEY, tygrysKILLBILL_API_SECRET);
    }

     static InputStream createNullInputStream() {
        return new ByteArrayInputStream(new byte[0]);
     }

     static boolean isNullInputStream(InputStream stream) {
        // Check if the stream is a ByteArrayInputStream and its content length is zero
        if (stream instanceof ByteArrayInputStream) {
            ByteArrayInputStream byteArrayInputStream = (ByteArrayInputStream) stream;
            return byteArrayInputStream.available() == 0;
        }
        return false; // Not a ByteArrayInputStream or not empty
    }

    /*
     * getCustomAttributesFromKillBill() gets the custo attributes associated with the
     * given invoice (identified by invoide id) from KillBill. The implementation can use
     * REST or JDBC to dip into the killbill database.
     *
     * @param invoiceId the Identity of the invoice in question
     * @returns stream of custom attributes.
     */
    public InputStream getCustomAttributesFromKillBill(final String invoiceId) throws KillBillClientException {
        Multimap<String, String> queryParms = ArrayListMultimap.create();
          queryParms.put("objectId", invoiceId);
          queryParms.put("objectType", "INVOICE");
            
          RequestOptions requestOptions = RequestOptions.builder()
            .withQueryParams(queryParms.asMap())
            .build();
             
          logger.info("getCustomAttributesFromKillBill: requestOptions = " + requestOptions);
          final String endpoint = killbillENDPOINT + "?objectId=" + invoiceId + "&objectType=INVOICE";
          logger.info("getCustomAttributesFromKillBill: endpoint = " + endpoint);

          final HttpResponse<InputStream> httpResp = killBillClient.doGet(endpoint, requestOptions);
          logger.info("getCustomAttributesFromKillBill: httpResp status = " + httpResp.statusCode());
         
          // Be prepared to yield a null input stream...
          return httpResp.body();
    }

    public String extractAttributesFromStream(InputStream attributeStream) {
      String attributes = "";
      try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(attributeStream, StandardCharsets.UTF_8))) {
            attributes = reader.lines().collect(Collectors.joining("\n"));
      } catch (Exception e) {
          e.printStackTrace();
          logger.error("Exception while extracting invoice name from the input stream: ", e);
      }


      logger.info("extractAttributesFromStream : attributes = " + attributes);
      return attributes; // Empty string of attributes...
    }

    /*
     * Extract the invoice name fro the list of custom attributes returned from KillBill.
     *
     * @param customFields the list of custom attributes.
     * @returns name of the invoice or invoiceNameUNNAMED_INVOICE, if none is found.
     */
    public String extractInvoiceName(final List<CustomField> customFields) {
          logger.info("extractInvoiceName : customFields = " + customFields);
          return customFields.stream()
                      .filter(field -> killbillCUSTOM_FIELD_INVOICE_NAME.equals(field.getName()))
                      .map(CustomField::getValue)
                      .findFirst()
                      .orElse(invoiceNameUNNAMED_INVOICE);
    }

    /*
     * Method to retrieve the custom invoice atribute, per the guidelines in
     * customization of email notification template.
     */
    public String getSubscriptionName() {
        if (this.subscriptionName != null && !this.subscriptionName.isBlank()) {
	   // Subscrition name already computed - yield cached value
	   return this.subscriptionName;
        }

        if (invoice == null)  {
            logger.error("getInvoiceName: invoiceId is invoiceNameUNNAMED_INVOICE");
            return invoiceNameUNNAMED_INVOICE;
        }

        final String invoiceId = invoice.getId().toString();
        logger.info("getInvoiceName: invoiceId =  " + invoiceId);
        try {
	  InputStream attributeStream = getCustomAttributesFromKillBill(invoiceId);
          if (isNullInputStream(attributeStream)) {
	      return invoiceNameUNNAMED_INVOICE;
          }

          // Convert the InputStream to a String
          final String attributesStr = extractAttributesFromStream(attributeStream);

          // Parse the JSON response into a list of CustomField objects
          ObjectMapper objectMapper = new ObjectMapper();
          final List<CustomField> customFields = objectMapper.readValue(attributesStr, new TypeReference<List<CustomField>>() {});
	  this.subscriptionName = extractInvoiceName(customFields);
          logger.info("Successfully extracted '" + this.subscriptionName + "' as the name of the invoice with ID: " + invoiceId);
          return this.subscriptionName;
      } catch (Exception e) {
          e.printStackTrace();
          logger.error("Exception while retrieving invoice name: ", e);
      }

      return invoiceNameUNNAMED_INVOICE;
    }
}
