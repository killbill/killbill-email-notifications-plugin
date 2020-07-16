/*
 * Copyright 2015-2016 Groupon, Inc
 * Copyright 2015-2016 The Billing Project, LLC
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.invoice.api.InvoicePayment;
import org.killbill.billing.invoice.api.InvoiceStatus;
import org.killbill.billing.invoice.api.formatters.InvoiceFormatter;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import static org.killbill.billing.plugin.notification.generator.formatters.Formatter.getFormattedAmountByLocaleAndInvoiceCurrency;

/**
 * Format invoice fields
 */
public class DefaultInvoiceFormatter implements InvoiceFormatter {

    private final Map<String, String> translator;
    private final Invoice invoice;
    private final Locale locale;
    private final DateTimeFormatter dateFormatter;

    public DefaultInvoiceFormatter(final Map<String, String> translator,
                                   final Invoice invoice,
                                   final Locale locale) {
        this.translator = translator;
        this.invoice = invoice;
        this.dateFormatter = DateTimeFormat.mediumDate().withLocale(locale);
        this.locale = locale;
    }

    @Override
    public Integer getInvoiceNumber() {
        return MoreObjects.firstNonNull(invoice.getInvoiceNumber(), 0);
    }

    @Override
    public List<InvoiceItem> getInvoiceItems() {
        final List<InvoiceItem> formatters = new ArrayList<InvoiceItem>();
        for (final InvoiceItem item : invoice.getInvoiceItems()) {
            formatters.add(new DefaultInvoiceItemFormatter(translator, item, dateFormatter, locale));
        }
        return formatters;
    }

    @Override
    public List<String> getTrackingIds() {
        return invoice.getTrackingIds();
    }

    @Override
    public boolean addTrackingIds(final Collection<String> trackingIds) {
        return invoice.addTrackingIds(trackingIds);
    }

    @Override
    public boolean addInvoiceItem(final InvoiceItem item) {
        return invoice.addInvoiceItem(item);
    }

    @Override
    public boolean addInvoiceItems(final Collection<InvoiceItem> items) {
        return invoice.addInvoiceItems(items);
    }

    @Override
    public <T extends InvoiceItem> List<InvoiceItem> getInvoiceItems(final Class<T> clazz) {
        return MoreObjects.firstNonNull(invoice.getInvoiceItems(clazz), ImmutableList.<InvoiceItem>of());
    }

    @Override
    public int getNumberOfItems() {
        return invoice.getNumberOfItems();
    }

    @Override
    public boolean addPayment(final InvoicePayment payment) {
        return invoice.addPayment(payment);
    }

    @Override
    public boolean addPayments(final Collection<InvoicePayment> payments) {
        return invoice.addPayments(payments);
    }

    @Override
    public List<InvoicePayment> getPayments() {
        return MoreObjects.firstNonNull(invoice.getPayments(), ImmutableList.<InvoicePayment>of());
    }

    @Override
    public int getNumberOfPayments() {
        return invoice.getNumberOfPayments();
    }

    @Override
    public UUID getAccountId() {
        return invoice.getAccountId();
    }

    @Override
    public BigDecimal getChargedAmount() {
        return MoreObjects.firstNonNull(invoice.getChargedAmount(), BigDecimal.ZERO);
    }

    @Override
    public String getFormattedChargedAmount() {
        return getFormattedAmountByLocaleAndInvoiceCurrency(getChargedAmount(), getCurrency().toString(), locale);
    }

    @Override
    public BigDecimal getOriginalChargedAmount() {
        return MoreObjects.firstNonNull(invoice.getOriginalChargedAmount(), BigDecimal.ZERO);
    }

    public String getFormattedOriginalChargedAmount() {
        return getFormattedAmountByLocaleAndInvoiceCurrency(getOriginalChargedAmount(), getCurrency().toString(), locale);
    }

    @Override
    public BigDecimal getBalance() {
        return MoreObjects.firstNonNull(invoice.getBalance(), BigDecimal.ZERO);
    }

    @Override
    public String getFormattedBalance() {
        return getFormattedAmountByLocaleAndInvoiceCurrency(getBalance(), getCurrency().toString(), locale);
    }

    @Override
    public Currency getProcessedCurrency() {
        return invoice.getCurrency();
    }

    @Override
    public String getProcessedPaymentRate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isMigrationInvoice() {
        return invoice.isMigrationInvoice();
    }

    @Override
    public InvoiceStatus getStatus() {
        return invoice.getStatus();
    }

    @Override
    public boolean isParentInvoice() {
        return invoice.isParentInvoice();
    }

    @Override
    public UUID getParentAccountId() {
        return invoice.getParentAccountId();
    }

    @Override
    public UUID getParentInvoiceId() {
        return invoice.getParentInvoiceId();
    }

    @Override
    public LocalDate getInvoiceDate() {
        return invoice.getInvoiceDate();
    }

    @Override
    public String getFormattedInvoiceDate() {
        final LocalDate invoiceDate = invoice.getInvoiceDate();
        if (invoiceDate == null) {
            return "";
        } else {
            return Strings.nullToEmpty(invoiceDate.toString(dateFormatter));
        }
    }

    @Override
    public LocalDate getTargetDate() {
        return invoice.getTargetDate();
    }

    public String getFormattedTargetDate() {
        return getTargetDate().toString(dateFormatter);
    }

    @Override
    public Currency getCurrency() {
        return invoice.getCurrency();
    }

    @Override
    public BigDecimal getPaidAmount() {
        return MoreObjects.firstNonNull(invoice.getPaidAmount(), BigDecimal.ZERO);
    }

    @Override
    public String getFormattedPaidAmount() {
        return getFormattedAmountByLocaleAndInvoiceCurrency(getPaidAmount(), getCurrency().toString(), locale);
    }

    @Override
    public UUID getId() {
        return invoice.getId();
    }

    @Override
    public DateTime getCreatedDate() {
        return invoice.getCreatedDate();
    }

    public String getFormattedCreatedDate() {
        return getCreatedDate().toString(dateFormatter);
    }

    @Override
    public DateTime getUpdatedDate() {
        return invoice.getUpdatedDate();
    }

    public String getFormattedUpdatedDate() {
        return getUpdatedDate().toString(dateFormatter);
    }

    @Override
    public BigDecimal getCreditedAmount() {
        return MoreObjects.firstNonNull(invoice.getCreditedAmount(), BigDecimal.ZERO);
    }

    public String getFormattedCreditedAmount() {
        return getFormattedAmountByLocaleAndInvoiceCurrency(getCreditedAmount(), getCurrency().toString(), locale);
    }

    @Override
    public BigDecimal getRefundedAmount() {
        return MoreObjects.firstNonNull(invoice.getRefundedAmount(), BigDecimal.ZERO);
    }

    public String getFormattedRefundedAmount() {
        return getFormattedAmountByLocaleAndInvoiceCurrency(getRefundedAmount(), getCurrency().toString(), locale);
    }

    // Expose the fields for children classes. This is useful for further customization of the invoices

    protected Map<String, String> getTranslator() {
        return translator;
    }

    protected Invoice getInvoice() {
        return invoice;
    }

    protected Locale getLocale() {
        return locale;
    }

    protected DateTimeFormatter getDateFormatter() {
        return dateFormatter;
    }
    
}
