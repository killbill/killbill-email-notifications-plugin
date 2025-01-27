package org.killbill.billing.plugin.notification.generator.formatters.tygrys.factory;

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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.killbill.billing.currency.api.CurrencyConversionApi;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.formatters.InvoiceFormatter;
import org.killbill.billing.invoice.plugin.api.InvoiceFormatterFactory;
import org.killbill.billing.plugin.notification.generator.formatters.tygrys.TygrysInvoiceFormatter;
import org.killbill.billing.invoice.template.formatters.DefaultInvoiceFormatter;

public class TygrysInvoiceFormatterFactory implements InvoiceFormatterFactory {

	private static final Logger logger = LoggerFactory.getLogger(TygrysInvoiceFormatterFactory.class);

	public TygrysInvoiceFormatterFactory() {
		super();
		logger.info("Initialize TygrysInvoiceFormatterFactory");
	}

         @Override
         public InvoiceFormatter createInvoiceFormatter(final String defaultLocale, final String catalogBundlePath,
                                                        final Invoice invoice, final Locale locale,
                                                        final CurrencyConversionApi currencyConversionApi,
                                                         ResourceBundle bundle, ResourceBundle defaultBundle) {
            try {
               return new TygrysInvoiceFormatter(defaultLocale, catalogBundlePath, invoice, currencyConversionApi, locale,
                                                       bundle, defaultBundle);
            }
            catch (Exception exc) {
		logger.error("Failed to instantiate TygrysInvoiceFormatter due to exception: %s", getStackTraceAsString(exc));
            }

            /*
             * If we fail to instantiate TygrysInvoiceFormatter, yield an instance of DefaltInvoiceFormatter - this will
             * not yield the custom attributes, but will print the basic invoice.
             */
	    logger.warn("Yielding an instance of DefaultInvoiceFormatter - custom attributes will be elided.");
            return new DefaultInvoiceFormatter(defaultLocale, catalogBundlePath, invoice, locale, currencyConversionApi, bundle, defaultBundle);
        }

        public static String getStackTraceAsString(final Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return sw.toString();
        }
}
