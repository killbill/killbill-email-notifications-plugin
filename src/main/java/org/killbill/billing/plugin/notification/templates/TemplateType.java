package org.killbill.billing.plugin.notification.templates;


import org.killbill.billing.plugin.notification.setup.EmailNotificationActivator;

public enum TemplateType {

    UPCOMING_INVOICE("UpcomingInvoice.mustache", "upcomingInvoiceSubject"),
    SUCCESSFUL_PAYMENT("SuccessfulPayment.mustache", "successfulPaymentSubject"),
    FAILED_PAYMENT("FailedPayment.mustache", "failedPaymentSubject"),
    PAYMENT_REFUND("PaymentRefund.mustache", "paymentRefundSubject"),
    SUBSCRIPTION_CANCELLATION_REQUESTED("SubscriptionCancellationRequested.mustache", "subscriptionCancellationRequestedSubject"),
    SUBSCRIPTION_CANCELLATION_EFFECTIVE("SubscriptionCancellationEffective.mustache", "subscriptionCancellationEffectiveSubject");

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
