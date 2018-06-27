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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.invoice.api.InvoiceItemType;
import org.killbill.billing.invoice.api.formatters.InvoiceItemFormatter;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;

import static org.killbill.billing.plugin.notification.generator.formatters.Formatter.getFormattedAmountByLocaleAndInvoiceCurrency;

/**
 * Format invoice item fields
 */
public class DefaultInvoiceItemFormatter implements InvoiceItemFormatter {

    private final Map<String, String> translator;
    private final InvoiceItem item;
    private final DateTimeFormatter dateFormatter;
    private final Locale locale;

    public DefaultInvoiceItemFormatter(final Map<String, String> translator,
                                       final InvoiceItem item,
                                       final DateTimeFormatter dateFormatter,
                                       final Locale locale) {
        this.translator = translator;
        this.item = item;
        this.dateFormatter = dateFormatter;
        this.locale = locale;
    }

    @Override
    public BigDecimal getAmount() {
        return MoreObjects.firstNonNull(item.getAmount(), BigDecimal.ZERO);
    }

    @Override
    public String getFormattedAmount() {
        return getFormattedAmountByLocaleAndInvoiceCurrency(getAmount(), getCurrency().toString(), locale);
    }

    @Override
    public Currency getCurrency() {
        return item.getCurrency();
    }

    @Override
    public InvoiceItemType getInvoiceItemType() {
        return item.getInvoiceItemType();
    }

    @Override
    public String getDescription() {
        return Strings.nullToEmpty(item.getDescription());
    }

    @Override
    public LocalDate getStartDate() {
        return item.getStartDate();
    }

    @Override
    public String getFormattedStartDate() {
        return item.getStartDate().toString(dateFormatter);
    }

    @Override
    public LocalDate getEndDate() {
        return item.getEndDate();
    }

    @Override
    public String getFormattedEndDate() {
        return getEndDate().toString(dateFormatter);
    }

    @Override
    public UUID getInvoiceId() {
        return item.getInvoiceId();
    }

    @Override
    public UUID getAccountId() {
        return item.getAccountId();
    }

    @Override
    public UUID getChildAccountId() {
        return item.getChildAccountId();
    }

    @Override
    public UUID getBundleId() {
        return item.getBundleId();
    }

    @Override
    public UUID getSubscriptionId() {
        return item.getSubscriptionId();
    }

    @Override
    public String getPlanName() {
        return MoreObjects.firstNonNull(Strings.emptyToNull(translator.get(item.getPlanName())), Strings.nullToEmpty(item.getPlanName()));
    }

    @Override
    public String getPrettyPlanName() {
        return MoreObjects.firstNonNull(Strings.emptyToNull(translator.get(item.getPhaseName())), Strings.nullToEmpty( item.getPrettyPlanName()));
    }

    @Override
    public String getPhaseName() {
        return MoreObjects.firstNonNull(Strings.emptyToNull(translator.get(item.getPhaseName())), Strings.nullToEmpty(item.getPhaseName()));
    }

    @Override
    public String getPrettyPhaseName() {
        return MoreObjects.firstNonNull(Strings.emptyToNull(translator.get(item.getPhaseName())), Strings.nullToEmpty( item.getPrettyPhaseName()));
    }

    @Override
    public String getUsageName() {
        return MoreObjects.firstNonNull(Strings.emptyToNull(translator.get(item.getUsageName())), Strings.nullToEmpty(item.getUsageName()));
    }

    @Override
    public String getPrettyUsageName() {
        return MoreObjects.firstNonNull(Strings.emptyToNull(translator.get(item.getPhaseName())), Strings.nullToEmpty( item.getPrettyUsageName()));
    }

    @Override
    public UUID getId() {
        return item.getId();
    }

    @Override
    public DateTime getCreatedDate() {
        return item.getCreatedDate();
    }

    public String getFormattedCreatedDate() {
        return getCreatedDate().toString(dateFormatter);
    }

    @Override
    public DateTime getUpdatedDate() {
        return item.getUpdatedDate();
    }

    public String getFormattedUpdatedDate() {
        return getUpdatedDate().toString(dateFormatter);
    }

    @Override
    public BigDecimal getRate() {
        return BigDecimal.ZERO;
    }

    @Override
    public UUID getLinkedItemId() {
        return null;
    }

    @Override
    public Integer getQuantity() {
        return item.getQuantity();
    }

    @Override
    public String getItemDetails() {
        return item.getItemDetails();
    }

    @Override
    public boolean matches(final Object other) {
        throw new UnsupportedOperationException();
    }
}
