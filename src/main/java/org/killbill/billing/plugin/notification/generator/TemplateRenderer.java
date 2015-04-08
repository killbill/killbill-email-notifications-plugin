package org.killbill.billing.plugin.notification.generator;

import com.google.common.base.Strings;
import org.killbill.billing.account.api.AccountData;
import org.killbill.billing.entitlement.api.Subscription;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.plugin.notification.email.EmailContent;
import org.killbill.billing.plugin.notification.templates.TemplateEngine;
import org.killbill.billing.plugin.notification.templates.TemplateType;
import org.killbill.billing.plugin.notification.util.IOUtils;
import org.killbill.billing.plugin.notification.util.LocaleUtils;
import org.killbill.billing.util.callcontext.TenantContext;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class TemplateRenderer {

    private final String DEFAULT_TEMPLATE_PATH_PREFIX = "org/killbill/billing/plugin/notification/templates/";

    private final TemplateEngine templateEngine;
    private final ResourceBundleFactory bundleFactory;

    public TemplateRenderer(final TemplateEngine templateEngine,
                            final ResourceBundleFactory bundleFactory) {
        this.templateEngine = templateEngine;
        this.bundleFactory = bundleFactory;
    }


    public EmailContent generateEmailForUpComingInvoice(final AccountData account, final Invoice invoice, final TenantContext context) throws IOException {
        return getEmailContent(TemplateType.UPCOMING_INVOICE, account, null, invoice, null, context);
    }

    public EmailContent generateEmailForSuccessfulPayment(final AccountData account, final Invoice invoice, final TenantContext context) throws IOException {
        return getEmailContent(TemplateType.SUCCESSFUL_PAYMENT, account, null, invoice, null, context);
    }

    public EmailContent generateEmailForFailedPayment(final AccountData account, final Invoice invoice, final TenantContext context) throws IOException {
        return getEmailContent(TemplateType.FAILED_PAYMENT, account, null, invoice, null, context);
    }

    public EmailContent generateEmailForPaymentRefund(final AccountData account, final PaymentTransaction paymentTransaction, final TenantContext context) throws IOException {
        return getEmailContent(TemplateType.PAYMENT_REFUND, account, null, null, paymentTransaction, context);
    }

    public EmailContent generateEmailForSubscriptionCancellationRequested(final AccountData account, final Subscription subscription, final TenantContext context) throws IOException {
        return getEmailContent(TemplateType.SUBSCRIPTION_CANCELLATION_REQUESTED, account, subscription, null, null, context);
    }

    public EmailContent generateEmailForSubscriptionCancellationEffective(final AccountData account, final Subscription subscription, final TenantContext context) throws IOException {
        return getEmailContent(TemplateType.SUBSCRIPTION_CANCELLATION_EFFECTIVE, account, subscription, null, null, context);
    }

    private EmailContent getEmailContent(final TemplateType templateType, final AccountData account, @Nullable Subscription subscription, @Nullable final Invoice invoice, @Nullable final PaymentTransaction paymentTransaction, final TenantContext context) throws IOException {

        final String accountLocale = Strings.emptyToNull(account.getLocale());
        final Locale locale = accountLocale == null ? Locale.getDefault() : LocaleUtils.toLocale(accountLocale);

        final Map<String, Object> data = new HashMap<String, Object>();
        final Map<String, String> text = getTranslationMap(accountLocale, ResourceBundleFactory.ResourceBundleType.TEMPLATE_TRANSLATION, context);
        data.put("text", text);
        data.put("account", account);
        if (subscription != null) {
            data.put("subscription", subscription);
        }
        if (invoice != null) {
            data.put("invoice", invoice);
        }
        if (paymentTransaction != null) {
            data.put("payment", paymentTransaction);
        }

        final String templateText = getTemplateText(locale, templateType, context);
        final String body = templateEngine.executeTemplateText(templateText, data);
        final String subject = new StringBuffer((String) text.get("merchantName")).append(": ").append(text.get(templateType.getSubjectKeyName())).toString();
        return new EmailContent(subject, body);
    }

    private Map<String, String> getTranslationMap(final String accountLocale, final ResourceBundleFactory.ResourceBundleType bundleType, final TenantContext context) {
        final ResourceBundle translationBundle = accountLocale != null ?
                bundleFactory.createBundle(LocaleUtils.toLocale(accountLocale), bundleType, context) : null;

        final Map<String, String> text = new HashMap<String, String>();
        Enumeration<String> keys = translationBundle.getKeys();
        while (keys.hasMoreElements()) {
            final String key = keys.nextElement();
            text.put(key, translationBundle.getString(key));
        }
        return text;
    }

    private String getTemplateText(final Locale locale, final TemplateType templateType, final TenantContext context) throws IOException {

        final String defaultTemplateName = DEFAULT_TEMPLATE_PATH_PREFIX + templateType.getTemplateName();
        if (context.getTenantId() == null) {
            return getDefaultTemplate(defaultTemplateName);
        }
        // STEPH Multi-tenant...
        return getDefaultTemplate(defaultTemplateName);

    }

    private String getDefaultTemplate(final String templateName) throws IOException {
        final InputStream templateStream = this.getClass().getClassLoader().getResource(templateName).openStream();
        return IOUtils.toString(templateStream);
    }
}
