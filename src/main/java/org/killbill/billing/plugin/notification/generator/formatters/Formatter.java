/*
 * Copyright 2015-2020 Groupon, Inc
 * Copyright 2015-2021 The Billing Project, LLC
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

package org.killbill.billing.plugin.notification.generator.formatters;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.joda.money.CurrencyUnit;

public abstract class Formatter {

    // Returns the formatted amount with the correct currency symbol
    public static String getFormattedAmountByLocaleAndInvoiceCurrency(final BigDecimal amount, final String currencyCode, final Locale locale) {
        final CurrencyUnit currencyUnit = CurrencyUnit.of(currencyCode);

        final DecimalFormat numberFormatter = (DecimalFormat) DecimalFormat.getCurrencyInstance(locale);
        final DecimalFormatSymbols dfs = numberFormatter.getDecimalFormatSymbols();
        dfs.setInternationalCurrencySymbol(currencyUnit.getCode());

        try {
            final java.util.Currency currency = java.util.Currency.getInstance(currencyCode);
            dfs.setCurrencySymbol(currency.getSymbol(locale));
        } catch (final IllegalArgumentException e) {
            dfs.setCurrencySymbol(currencyUnit.getSymbol(locale));
        }

        numberFormatter.setDecimalFormatSymbols(dfs);
        numberFormatter.setMinimumFractionDigits(currencyUnit.getDecimalPlaces());
        numberFormatter.setMaximumFractionDigits(currencyUnit.getDecimalPlaces());

        return numberFormatter.format(amount.doubleValue());
    }
}
