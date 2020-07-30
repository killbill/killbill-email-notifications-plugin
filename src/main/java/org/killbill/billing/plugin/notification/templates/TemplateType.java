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

package org.killbill.billing.plugin.notification.templates;


import org.killbill.billing.plugin.notification.setup.EmailNotificationActivator;

public enum TemplateType {

    UPCOMING_INVOICE("UpcomingInvoice.mustache", "upcomingInvoiceSubject"),
    SUCCESSFUL_PAYMENT("SuccessfulPayment.mustache", "successfulPaymentSubject"),
    FAILED_PAYMENT("FailedPayment.mustache", "failedPaymentSubject"),
    PAYMENT_REFUND("PaymentRefund.mustache", "paymentRefundSubject"),
    SUBSCRIPTION_CANCELLATION_REQUESTED("SubscriptionCancellationRequested.mustache", "subscriptionCancellationRequestedSubject"),
    SUBSCRIPTION_CANCELLATION_EFFECTIVE("SubscriptionCancellationEffective.mustache", "subscriptionCancellationEffectiveSubject"),
    INVOICE_CREATION("InvoiceCreation.mustache", "invoiceCreationSubject");

    final String defaultTemplateName;
    final String subjectKeyName;

    TemplateType(String templateName, String subjectKeyName) {
        this.defaultTemplateName = templateName;
        this.subjectKeyName =subjectKeyName;
    }

    public String getDefaultTemplateName() {
        return defaultTemplateName;
    }

    public String getTemplateKey() {
        return new StringBuffer(EmailNotificationActivator.PLUGIN_NAME)
                .append(":")
                .append(this).toString();
    }
    public String getSubjectKeyName() {
        return subjectKeyName;
    }
}
