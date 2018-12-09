/*
 * Copyright 2015-2015 Groupon, Inc
 * Copyright 2015-2015 The Billing Project, LLC
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

package org.killbill.billing.plugin.notification.generator;

import com.google.common.base.Charsets;
import org.killbill.billing.plugin.notification.setup.EmailNotificationActivator;
import org.killbill.billing.plugin.notification.util.LocaleUtils;
import org.killbill.billing.tenant.api.TenantApiException;
import org.killbill.billing.tenant.api.TenantKV;
import org.killbill.billing.tenant.api.TenantUserApi;
import org.killbill.billing.util.callcontext.TenantContext;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class ResourceBundleFactory {

    private final String DEFAULT_TRANSLATION_PATH_PREFIX = "org/killbill/billing/plugin/notification/translations/";

    private final LogService logService;
    private final TenantUserApi tenantApi;

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

        public String getTranslationKey() {
            return new StringBuffer(EmailNotificationActivator.PLUGIN_NAME)
                    .append(":")
                    .append(this).toString();
        }
    }

    public ResourceBundleFactory(final TenantUserApi tenantApi, final LogService logService) {
        this.tenantApi = tenantApi;
        this.logService = logService;
    }



    public ResourceBundle createBundle(final Locale locale, final ResourceBundleType type, final TenantContext tenantContext) throws TenantApiException {
        if (tenantContext.getTenantId() == null) {
            return getGlobalBundle(locale, type);
        }
        final String bundle = getTenantBundleForType(locale, type, tenantContext);
        if (bundle != null) {
            try {
                return new PropertyResourceBundle(new InputStreamReader(new ByteArrayInputStream(bundle.getBytes(Charsets.UTF_8)), "UTF-8"));
            } catch (IOException e) {
                logService.log(LogService.LOG_WARNING, String.format("Failed to de-serialize the property bundle for tenant %s and locale %s", tenantContext.getTenantId(), locale));
                // Fall through...
            }
        }
        return getGlobalBundle(locale, type);
    }

    private String getTenantBundleForType(final Locale locale, final ResourceBundleType type, final TenantContext tenantContext) throws TenantApiException {
        String tenantKey;
        switch (type) {
            case CATALOG_TRANSLATION:
                tenantKey = LocaleUtils.localeString(locale, TenantKV.TenantKey.CATALOG_TRANSLATION_.name());
                break;
            case TEMPLATE_TRANSLATION:
                tenantKey = LocaleUtils.localeString(locale, type.getTranslationKey());
                break;
            default:
                return null;
        }
        if (tenantKey !=  null) {
            final List<String> result = tenantApi.getTenantValuesForKey(tenantKey, tenantContext);
            if (result.size() == 1) {
                return result.get(0);
            }
        }
        return null;
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
            final URL url = this.getClass().getClassLoader().getResource(propertiesFileName);

            if (url == null){
                return null;
            }

            final InputStream inputStream = url.openStream();
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
