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

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.killbill.billing.plugin.notification.email.SmtpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailNotificationConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationConfiguration.class);

    private static final String SMTP_PROPERTY_PREFIX = EmailNotificationActivator.PROPERTY_PREFIX + "smtp.";

    // SMTP related properties
    private final SmtpProperties smtp;

    // Default events permitted
    private final Set<String> eventTypes;

    private boolean sendHTMLEmail;

    public EmailNotificationConfiguration(){
        eventTypes = new HashSet<String>();
        smtp = null;
    }

    public EmailNotificationConfiguration(final Properties properties)
    {
        String defaultEvents = properties.getProperty(EmailNotificationActivator.PROPERTY_PREFIX + "defaultEvents");

        this.eventTypes = new HashSet<String>();
        if (defaultEvents != null && !defaultEvents.isEmpty())
        {
            for( String eventType : defaultEvents.split(","))
            {
                this.eventTypes.add(eventType);
            }
        }

        final String smtpServerName = properties.getProperty(SMTP_PROPERTY_PREFIX + "host");
        final String smtpPort = properties.getProperty(SMTP_PROPERTY_PREFIX + "port");
        final String smtpAuth = properties.getProperty(SMTP_PROPERTY_PREFIX + "useAuthentication");
        final String smtpUserName = properties.getProperty(SMTP_PROPERTY_PREFIX + "userName");
        final String smtpPassword = properties.getProperty(SMTP_PROPERTY_PREFIX + "password");
        final String smtpUseSSL = properties.getProperty(SMTP_PROPERTY_PREFIX + "useSSL");
        final String defaultSender = properties.getProperty(SMTP_PROPERTY_PREFIX + "defaultSender");

        this.smtp = new SmtpProperties(smtpServerName, smtpPort, parseBoolean(smtpAuth),
                                       smtpUserName, smtpPassword, parseBoolean(smtpUseSSL),defaultSender);
        this.sendHTMLEmail = parseBoolean(properties.getProperty(SMTP_PROPERTY_PREFIX + "sendHTMLEmail"));
    }

    public final Set<String> getEventTypes() {

        return this.eventTypes;
    }

    public SmtpProperties getSmtp() {
        return smtp;
    }

    public boolean sendHTMLEmail() {
        return sendHTMLEmail;
    }

    private final boolean parseBoolean(String s){
        if (s != null && s.equalsIgnoreCase("yes")){
            return true;
        }

        return Boolean.parseBoolean(s);
    }

}
