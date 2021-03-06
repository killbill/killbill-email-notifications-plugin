/*
 * Copyright 2020 The Billing Project, LLC
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

package org.killbill.billing.plugin.notification.api;

import java.util.Locale;
import java.util.Map;

import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.formatters.InvoiceFormatter;
import org.killbill.billing.util.callcontext.TenantContext;

/**
 * API for a factory service that creates {@link InvoiceFormatter} instances.
 * 
 * @author matt
 */
public interface InvoiceFormatterFactory {

    /**
     * Create an {@link InvoiceFormatter} instance for a given {@link Invoice}.
     * 
     * @param translator the available translations
     * @param invoice the invoice
     * @param locale the desired locale
     * @param context the tenant context
     * @return the formatter instance, never {@literal null}
     */
    InvoiceFormatter createInvoiceFormatter(
            Map<String, String> translator,
            Invoice invoice,
            Locale locale,
            TenantContext context);

}
