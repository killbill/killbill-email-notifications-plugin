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

package org.killbill.billing.plugin.notification.setup;

import java.util.Properties;

import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillDataSource;
import org.killbill.billing.plugin.api.notification.PluginTenantConfigurableConfigurationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * When per-tenant config changes are made, the plugin automatically gets notified (and prints a log trace)
 * <pre>
 * {@code
 * curl -v \
 *      -X POST \
 *      -u admin:password \
 *      -H "Content-Type: text/plain" \
 *      -H "X-Killbill-ApiKey: bob" \
 *      -H "X-Killbill-ApiSecret: lazar" \
 *      -H "X-Killbill-CreatedBy: demo" \
 *      -d 'org.killbill.billing.plugin.email-notifications.defaultEvents=events'
 *      "http://127.0.0.1:8080/1.0/kb/tenants/uploadPluginConfig/killbill-email-notifications"
 * }
 * </pre>
 */
public class EmailNotificationConfigurationHandler extends PluginTenantConfigurableConfigurationHandler<EmailNotificationConfiguration> {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationConfigurationHandler.class);

    private final String region;
    private final OSGIKillbillDataSource dataSource;

    public EmailNotificationConfigurationHandler(final String region,
                                                 final String pluginName,
                                                 final OSGIKillbillAPI osgiKillbillAPI,
                                                 final OSGIKillbillDataSource dataSource) {
        super(pluginName, osgiKillbillAPI);
        this.region = region;
        this.dataSource = dataSource;
    }

    @Override
    protected EmailNotificationConfiguration createConfigurable(final Properties properties) {
        logger.info("New properties for region {}: {}", region, properties);
        try {
            return new EmailNotificationConfiguration(properties);
        } catch (final Exception e) {
            logger.info(e.getMessage());
            return new EmailNotificationConfiguration();
        }

    }
}
