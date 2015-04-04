package org.killbill.billing.plugin.notification.templates;



public enum TemplateType {

    UPCOMING_INVOICE("UpcomingInvoice.mustache"),
    SUCCESSFUL_PAYMENT("SuccessfulPayment.mustache"),
    FAILED_PAYMENT(""),
    PAYMENT_REFUND(""),
    SUBSCRIPTION_CANCELLATION_REQUESTED(""),
    SUBSCRIPTION_CANCELLATION_EFFECTIVE("");

    final String templateName;

    TemplateType(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateName() {
        return templateName;
    }
}
