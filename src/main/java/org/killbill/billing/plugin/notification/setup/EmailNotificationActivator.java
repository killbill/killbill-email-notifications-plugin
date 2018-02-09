/*
 * Copyright 2014-2017 Groupon, Inc
 * Copyright 2014-2017 The Billing Project, LLC
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

import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillEventDispatcher;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.plugin.api.notification.PluginConfigurationEventHandler;
import org.killbill.billing.plugin.core.config.PluginEnvironmentConfig;
import org.killbill.billing.plugin.core.resources.jooby.PluginApp;
import org.killbill.billing.plugin.core.resources.jooby.PluginAppBuilder;
import org.killbill.billing.plugin.notification.http.EmailNotificationServlet;
import org.osgi.framework.BundleContext;

public class EmailNotificationActivator extends KillbillActivatorBase {

    public static final String PLUGIN_NAME = "killbill-email-notifications";
    public static final String PROPERTY_PREFIX = "org.killbill.billing.plugin.email-notifications.";

    private OSGIKillbillEventDispatcher.OSGIKillbillEventHandler emailNotificationListener;
    private EmailNotificationConfigurationHandler emailNotificationConfigurationHandler;

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        final String region = PluginEnvironmentConfig.getRegion(configProperties.getProperties());

        // Register an event listener for plugin configuration (optional)
        emailNotificationConfigurationHandler = new EmailNotificationConfigurationHandler(region, PLUGIN_NAME, killbillAPI, logService, dataSource);
        final EmailNotificationConfiguration globalConfiguration = emailNotificationConfigurationHandler.createConfigurable(configProperties.getProperties());
        emailNotificationConfigurationHandler.setDefaultConfigurable(globalConfiguration);

        final PluginConfigurationEventHandler handler = new PluginConfigurationEventHandler(emailNotificationConfigurationHandler);
        dispatcher.registerEventHandlers(handler);

        // Register an event listener (optional)
        emailNotificationListener = new EmailNotificationListener(clock, logService, killbillAPI, configProperties, dataSource, emailNotificationConfigurationHandler);
        dispatcher.registerEventHandlers(emailNotificationListener);

        // Register a servlet (optional)
        final PluginApp pluginApp = new PluginAppBuilder(PLUGIN_NAME,
                                                         killbillAPI,
                                                         logService,
                                                         dataSource,
                                                         super.clock,
                                                         configProperties).withRouteClass(EmailNotificationServlet.class)
                                                                          .build();
        final HttpServlet httpServlet = PluginApp.createServlet(pluginApp);
        registerServlet(context, httpServlet);
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        super.stop(context);

        // Do additional work on shutdown (optional)
    }


    private void registerServlet(final BundleContext context, final HttpServlet servlet) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, Servlet.class, servlet, props);
    }

}
