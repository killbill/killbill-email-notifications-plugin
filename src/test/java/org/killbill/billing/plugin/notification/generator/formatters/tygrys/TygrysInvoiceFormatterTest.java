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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.UUID;
import java.net.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;
import org.mockito.Mock;
import org.mockito.InjectMocks;

import org.killbill.billing.client.KillBillHttpClient;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.currency.api.CurrencyConversionApi;

class TygrysInvoiceFormatterTest {

    private static final Logger logger = LoggerFactory.getLogger(TygrysInvoiceFormatterTest.class);

    @Mock private KillBillHttpClient mockKillBillClient;
    @Mock private Invoice mockInvoice;
    @InjectMocks private TygrysInvoiceFormatter formatter;
    @BeforeEach void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Mock Invoice ID
        when(mockInvoice.getId()).thenReturn(UUID.randomUUID());

        // Initialize the TygrysInvoiceFormatter
        formatter = new TygrysInvoiceFormatter(
                "en_US",
                "/path/to/catalogBundle",
                mockInvoice,
                Locale.US,
                mock(CurrencyConversionApi.class),
                ResourceBundle.getBundle("defaultBundle"),
                ResourceBundle.getBundle("customBundle"),
                mockKillBillClient
        );
    }

    private String customAttributeUrl(final String invoiceId) {
        return String.format("%s?objectId=%s&objectType=INVOICE", TygrysInvoiceFormatter.killbillENDPOINT, invoiceId);
    }

    private String createFakeStream(final String invoiceName) {
        return createFakeStream(invoiceName, false);
    }

    private String createFakeStream(final String invoiceName, final Boolean generateSecondaryAttrs) {
      Gson gson = new Gson();
      List<Map<String, String>> attributesList = new ArrayList<>();
      logger.info("createFakeStream: invoiceName = " + (invoiceName == null ? "<null>":invoiceName));

    
      if (invoiceName != null) {
         // Add invoice attributes
         Map<String, String> attribute1 = new HashMap<>();
         attribute1.put("name", TygrysInvoiceFormatter.killbillCUSTOM_FIELD_INVOICE_NAME);
         attribute1.put("value", invoiceName);
         attributesList.add(attribute1);
      }
  
      if (generateSecondaryAttrs) {

        // Generate a random number of additional attributes
        int numberOfRandomAttributes = ThreadLocalRandom.current().nextInt(3, 10); // Random number between 3 and 10
        for (int i = 0; i < numberOfRandomAttributes; i++) {
            Map<String, String> randomAttribute = new HashMap<>();
            randomAttribute.put("name", "randomName" + i); // Unique random name
            randomAttribute.put("value", "randomValue" + ThreadLocalRandom.current().nextInt(1, 100)); // Random value
            attributesList.add(randomAttribute);
        }
      }

      // Return JSon format..
      return gson.toJson(attributesList);
    }

    @Test
    void testGetCustomAttributesFromKillBill_withStubbedCustomAttributes() throws Exception {
      // Mock the InputStream to simulate KillBill API response
      final String invoiceName = "Test-Invoice";
      final String mockResponse = createFakeStream(invoiceName);
      InputStream mockStream = new ByteArrayInputStream(mockResponse.getBytes());

      // Mock the Invoice ID
      UUID invoiceId = UUID.randomUUID();
      when(mockInvoice.getId()).thenReturn(invoiceId);

      // Stub the getCustomAttributesFromKillBill method
      when(mockKillBillClient.doGet(anyString(), any()))
            .thenReturn(mock(HttpResponse.class));
      when(mockKillBillClient.doGet(anyString(), any()).body()).thenReturn(mockStream);

      // Expected endpoint
      final String expectedEndpoint = customAttributeUrl(invoiceId.toString());

      // Call the method under test
      InputStream attributeStream = formatter.getCustomAttributesFromKillBill(invoiceId.toString());

      // Verify and assert results
      verify(mockKillBillClient).doGet(eq(expectedEndpoint), any()); // Verify the expected endpoint
    }

    @Test void testExtractAttributesFromStream_withStubbedCustomAttributes() throws Exception {
      // Create parameterized mock stream
      final String givenInvoiceName = "CocaCola-Invoice";
      final String mockResponse = createFakeStream(givenInvoiceName);
      InputStream mockStream = new ByteArrayInputStream(mockResponse.getBytes());

      // Mock the Invoice ID
      UUID invoiceId = UUID.randomUUID();
      when(mockInvoice.getId()).thenReturn(invoiceId);

      // Stub the getCustomAttributesFromKillBill method
      when(mockKillBillClient.doGet(anyString(), any()))
            .thenReturn(mock(HttpResponse.class));
      when(mockKillBillClient.doGet(anyString(), any()).body()).thenReturn(mockStream);

      // Expected endpoint
      final String expectedEndpoint = customAttributeUrl(invoiceId.toString());

      // Call the method under test
      InputStream attributeStream = formatter.getCustomAttributesFromKillBill(invoiceId.toString());
      final String attrs = formatter.extractAttributesFromStream(attributeStream);

      // Verify the expected endpoint
      verify(mockKillBillClient).doGet(eq(expectedEndpoint), any()); 

      logger.info("testExtractAttributesFromStream_withStubbedCustomAttributes: attrs = " + attrs);
      logger.info("testExtractAttributesFromStream_withStubbedCustomAttributes: mockResponse = " + mockResponse);
      assertEquals(mockResponse, attrs);
    }

    @Test
    void testGetInvoiceName_withNullInvoice() throws Exception {
        // Test behaviour when the invoice is null - negative test cases
        formatter.setInvoice(null);

        // Create parameterized mock stream
        final String givenInvoiceName = "CocaCola-Invoice";
        final String mockResponse = createFakeStream(givenInvoiceName);
        logger.info("testGetInvoiceName_withNullInvoice:  mockResponse = " + mockResponse);
        logger.info("testGetInvoiceName_withNullInvoice:  mockResponse bytes = " + mockResponse.getBytes());

        InputStream mockStream = new ByteArrayInputStream(mockResponse.getBytes());
        logger.info("testGetInvoiceName_withNullInvoice:  is null mockResponse? = " + TygrysInvoiceFormatter.isNullInputStream(mockStream));

        // Call the method under test
        final String retrievedInvoiceName = formatter.getSubscriptionName();
        logger.info("testGetInvoiceName_withNullInvoice:  retrieved invoiceName = " + retrievedInvoiceName);

        // Verify and assert results
        verifyNoInteractions(mockKillBillClient); // Because invoice is null, formatter does not interact with killbill at all.
        assertEquals(TygrysInvoiceFormatter.invoiceNameUNNAMED_INVOICE, retrievedInvoiceName);
    }

    // Test behaviour when the invoice tag is missing - negative test cases
    @Test
    void testGetInvoiceName_withMissingInvoiceNameTag() throws Exception {
        // Create parameterized mock stream
        final String givenInvoiceName = null;

        // Mock Invoice ID
        final UUID randomUuid = UUID.randomUUID();
        when(mockInvoice.getId()).thenReturn(randomUuid);

        // Expected endpoint
        final String expectedEndpoint = customAttributeUrl(randomUuid.toString());

        final String mockResponse = createFakeStream(givenInvoiceName);
        InputStream mockStream = new ByteArrayInputStream(mockResponse.getBytes());

        // Call the method under test
        final String retrievedInvoiceName = formatter.getSubscriptionName();
        logger.info("testGetInvoiceName_withNullInvoice:  retrieved invoiceName = " + retrievedInvoiceName);

        // Verify and assert results
        verify(mockKillBillClient).doGet(eq(expectedEndpoint), any()); // Verify the expected endpoint
        assertEquals(TygrysInvoiceFormatter.invoiceNameUNNAMED_INVOICE, retrievedInvoiceName);
    }

    @Test
    void testGetInvoiceName_withStubbedCustomAttributes() throws Exception {
        // Create parameterized mock stream
        final String givenInvoiceName = "AlwaysUpNetworks-Invoice";
        final String mockResponse = createFakeStream(givenInvoiceName, true);
        InputStream mockStream = new ByteArrayInputStream(mockResponse.getBytes());

        // Stub the getCustomAttributesFromKillBill method
        when(mockKillBillClient.doGet(anyString(), any()))
                .thenReturn(mock(HttpResponse.class));
        when(mockKillBillClient.doGet(anyString(), any()).body()).thenReturn(mockStream);

        // Call the method under test
        final String retrievedInvoiceName = formatter.getSubscriptionName();
        logger.info("testGetInvoiceName_withStubbedCustomAttributes:  retrieved invoiceName = " + retrievedInvoiceName);

        // Verify and assert results
        verify(mockKillBillClient).doGet(contains(TygrysInvoiceFormatter.killbillENDPOINT), any());
        assertEquals(givenInvoiceName, retrievedInvoiceName);
    }
}
