package org.killbill.billing.plugin.notification.generator;

import com.google.common.base.Strings;
import org.killbill.billing.account.api.AccountData;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.plugin.notification.email.EmailContent;
import org.killbill.billing.plugin.notification.templates.TemplateEngine;
import org.killbill.billing.plugin.notification.templates.TemplateType;
import org.killbill.billing.plugin.notification.util.IOUtils;
import org.killbill.billing.plugin.notification.util.LocaleUtils;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.xmlloader.UriAccessor;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
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

    public EmailContent generateEmailForSuccessfulPayment(final AccountData account, @Nullable final Invoice invoice, final TenantContext context) throws IOException {
        // Don't do anything if the invoice is null
        if (invoice == null) {
            return null;
        }

        final String accountLocale = Strings.emptyToNull(account.getLocale());
        final Locale locale = accountLocale == null ? Locale.getDefault() : LocaleUtils.toLocale(accountLocale);

        final Map<String, Object> data = new HashMap<String, Object>();

        final ResourceBundle translationBundle = accountLocale != null ?
                bundleFactory.createBundle(LocaleUtils.toLocale(accountLocale), ResourceBundleFactory.ResourceBundleType.TEMPLATE_TRANSLATION, context) : null;

        final Map<String, Object> text = new HashMap<String, Object>();
        Enumeration<String> keys = translationBundle.getKeys();
        while (keys.hasMoreElements()) {
            final String key = keys.nextElement();
            text.put(key, translationBundle.getString(key));
        }
        data.put("text", text);
        data.put("account", account);
        data.put("invoice", invoice);


        final String templateText = getTemplateText(locale, TemplateType.SUCCESSFUL_PAYMENT, context);
        final String body = templateEngine.executeTemplateText(templateText, data);
        return new EmailContent(translationBundle.getString("successfulPaymentSubject"), body);

    }

    private String getTemplateText(final Locale locale, final TemplateType templateType, final TenantContext context) throws IOException {

        final String defaultTemplateName = DEFAULT_TEMPLATE_PATH_PREFIX + templateType.getTemplateName();
        if (context.getTenantId() == null) {
            return getDefaultTemplate(defaultTemplateName);
        }
        // Multi-tenant...
        return getDefaultTemplate(defaultTemplateName);

    }

    private String getDefaultTemplate(final String templateName) throws IOException {
        try {
            final InputStream templateStream = UriAccessor.accessUri(templateName);
            return IOUtils.toString(templateStream);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }
}
