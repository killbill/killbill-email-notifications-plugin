/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2020-2020 Equinix, Inc
 * Copyright 2014-2020 The Billing Project, LLC
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
import java.util.Locale;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.payment.api.TransactionStatus;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;

import com.google.common.base.MoreObjects;

import static org.killbill.billing.plugin.notification.generator.formatters.Formatter.getFormattedAmountByLocaleAndInvoiceCurrency;

public class PaymentFormatter implements PaymentTransaction {

    private final PaymentTransaction paymentTransaction;
    private final Locale locale;
    private final DateTimeFormatter dateFormatter;

    public PaymentFormatter(final PaymentTransaction paymentTransaction, final Locale locale) {
        this.paymentTransaction = paymentTransaction;
        this.dateFormatter = DateTimeFormat.mediumDate().withLocale(locale);
        this.locale = locale;
    }

    @Override
    public UUID getPaymentId() {
        return paymentTransaction.getPaymentId();
    }

    @Override
    public String getExternalKey() {
        return paymentTransaction.getExternalKey();
    }

    @Override
    public TransactionType getTransactionType() {
        return paymentTransaction.getTransactionType();
    }

    @Override
    public DateTime getEffectiveDate() {
        return paymentTransaction.getEffectiveDate();
    }

    public String getFormattedEffectiveDate() {
        return getEffectiveDate().toString(dateFormatter);
    }

    @Override
    public BigDecimal getAmount() {
        return MoreObjects.firstNonNull(paymentTransaction.getAmount(), BigDecimal.ZERO);
    }

    public String getFormattedAmount() {
        return getFormattedAmountByLocaleAndInvoiceCurrency(getAmount(), getCurrency().toString(), locale);
    }

    @Override
    public Currency getCurrency() {
        return paymentTransaction.getCurrency();
    }

    @Override
    public BigDecimal getProcessedAmount() {
        return MoreObjects.firstNonNull(paymentTransaction.getProcessedAmount(), BigDecimal.ZERO);
    }

    public String getFormattedProcessedAmount() {
        return getFormattedAmountByLocaleAndInvoiceCurrency(getProcessedAmount(), getProcessedCurrency().toString(), locale);
    }

    @Override
    public Currency getProcessedCurrency() {
        return paymentTransaction.getProcessedCurrency();
    }

    @Override
    public String getGatewayErrorCode() {
        return paymentTransaction.getGatewayErrorCode();
    }

    @Override
    public String getGatewayErrorMsg() {
        return paymentTransaction.getGatewayErrorMsg();
    }

    @Override
    public TransactionStatus getTransactionStatus() {
        return paymentTransaction.getTransactionStatus();
    }

    @Override
    public PaymentTransactionInfoPlugin getPaymentInfoPlugin() {
        return paymentTransaction.getPaymentInfoPlugin();
    }

    @Override
    public UUID getId() {
        return paymentTransaction.getId();
    }

    @Override
    public DateTime getCreatedDate() {
        return paymentTransaction.getCreatedDate();
    }

    public String getFormattedCreatedDate() {
        return getCreatedDate().toString(dateFormatter);
    }

    @Override
    public DateTime getUpdatedDate() {
        return paymentTransaction.getUpdatedDate();
    }

    public String getFormattedUpdatedDate() {
        return getUpdatedDate().toString(dateFormatter);
    }
}
