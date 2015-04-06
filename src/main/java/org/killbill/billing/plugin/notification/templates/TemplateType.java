package org.killbill.billing.plugin.notification.templates;



public enum TemplateType {

    UPCOMING_INVOICE("UpcomingInvoice.mustache", "upcomingInvoiceSubject"),
    SUCCESSFUL_PAYMENT("SuccessfulPayment.mustache", "successfulPaymentSubject"),
    FAILED_PAYMENT("FailedPayment.mustache", "failedPaymentSubject"),
    PAYMENT_REFUND("PaymentRefund.mustache", "paymentRefundSubject"),
    SUBSCRIPTION_CANCELLATION_REQUESTED("SubscriptionCancellationRequested.mustache", "subscriptionCancellationRequestedSubject"),
    SUBSCRIPTION_CANCELLATION_EFFECTIVE("SubscriptionCancellationEffective.mustache", "subscriptionCancellationEffectiveSubject");

    final String templateName;
    final String subjectKeyName;

    TemplateType(String templateName, String subjectKeyName) {
        this.templateName = templateName;
        this.subjectKeyName =subjectKeyName;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getSubjectKeyName() {
        return subjectKeyName;
    }
}
