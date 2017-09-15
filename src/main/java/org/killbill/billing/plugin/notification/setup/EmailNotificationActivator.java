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

package org.killbill.billing.plugin.notification.setup;

import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillEventDispatcher;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.plugin.notification.http.EmailNotificationServlet;
import org.osgi.framework.BundleContext;

public class EmailNotificationActivator extends KillbillActivatorBase {

    public static final String PLUGIN_NAME = "killbill-email-notifications";

    private OSGIKillbillEventDispatcher.OSGIKillbillEventHandler emailNotificationListener;

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        // Register an event listener (optional)
        emailNotificationListener = new EmailNotificationListener(clock, logService, killbillAPI, configProperties);
        dispatcher.registerEventHandlers(emailNotificationListener);


        // Register a servlet (optional)
        final EmailNotificationServlet analyticsServlet = new EmailNotificationServlet(logService);
        registerServlet(context, analyticsServlet);
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
