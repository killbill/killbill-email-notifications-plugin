package org.killbill.billing.plugin.notification.generator;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.killbill.billing.util.callcontext.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class ResourceBundleFactory {

    private final String DEFAULT_TRANSLATION_PATH_PREFIX = "org/killbill/billing/plugin/notification/translations/";

    private static final Logger logger = LoggerFactory.getLogger(ResourceBundleFactory.class);

    //private final TenantInternalApi tenantApi;

    public enum ResourceBundleType {
        TEMPLATE_TRANSLATION("Translation"),
        CATALOG_TRANSLATION("");

        private final String resourceName;

        ResourceBundleType(String resourceName) {
            this.resourceName = resourceName;
        }

        public String getResourceName() {
            return resourceName;
        }
    }


    public ResourceBundleFactory() {
    }

    public ResourceBundle createBundle(final Locale locale, final ResourceBundleType type, final TenantContext tenantContext) {
        if (tenantContext.getTenantId() == null) {
            return getGlobalBundle(locale, type);
        }
        final String bundle = getTenantBundleForType(locale, type, tenantContext);
        if (bundle != null) {
            try {
                return new PropertyResourceBundle(new ByteArrayInputStream(bundle.getBytes(Charsets.UTF_8)));
            } catch (IOException e) {
                logger.warn("Failed to de-serialize the property bundle for tenant {} and locale {}", tenantContext.getTenantId(), locale);
                // Fall through...
            }
        }
        return getGlobalBundle(locale, type);
    }

    private String getTenantBundleForType(final Locale locale, final ResourceBundleType type, final TenantContext tenantContext) {
        switch (type) {
            case CATALOG_TRANSLATION:
                //return tenantApi.getCatalogTranslation(locale, tenantContext);

            case TEMPLATE_TRANSLATION:
                //return tenantApi.getInvoiceTranslation(locale, tenantContext);

            default:
                //logger.warn("Unexpected bundle type {} ", type);
                return null;
        }
    }

    private ResourceBundle getGlobalBundle(final Locale locale, final ResourceBundleType bundleType) {
        final String bundlePath = DEFAULT_TRANSLATION_PATH_PREFIX + bundleType.getResourceName();
        try {
            // Try to loadDefaultCatalog the bundle from the classpath first
            return ResourceBundle.getBundle(bundlePath, locale);
        } catch (MissingResourceException ignored) {
        }
        // Try to loadDefaultCatalog it from a properties file
        final String propertiesFileNameWithCountry = bundlePath + "_" + locale.getLanguage() + "_" + locale.getCountry() + ".properties";
        ResourceBundle bundle = getBundleFromPropertiesFile(propertiesFileNameWithCountry);
        if (bundle != null) {
            return bundle;
        } else {
            final String propertiesFileName = bundlePath + "_" + locale.getLanguage() + ".properties";
            bundle = getBundleFromPropertiesFile(propertiesFileName);
        }

        return bundle;
    }

    private ResourceBundle getBundleFromPropertiesFile(final String propertiesFileName) {
        try {
            final InputStream inputStream = this.getClass().getClassLoader().getResource(propertiesFileName).openStream();
            if (inputStream == null) {
                return null;
            } else {
                return new PropertyResourceBundle(inputStream);
            }
        } catch (IllegalArgumentException iae) {
            return null;
        } catch (MissingResourceException mrex) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }
}
