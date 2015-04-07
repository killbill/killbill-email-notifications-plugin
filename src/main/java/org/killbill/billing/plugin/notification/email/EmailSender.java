/*
 * Copyright 2010-2011 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
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

package org.killbill.billing.plugin.notification.email;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.killbill.killbill.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class EmailSender {

    /* Reuse Kill Bill email properties; if needed we can have a different set for the plugin */
    private static final String SERVER_NAME_PROP = "org.killbill.mail.smtp.host";
    private static final String SERVER_PORT_PROP = "org.killbill.mail.smtp.port";
    private static final String IS_SMTP_AUTH_PROP = "org.killbill.mail.smtp.auth";
    private static final String SMTP_USER_PROP = "org.killbill.mail.smtp.user";
    private static final String SMTP_PWD_PROP = "org.killbill.mail.smtp.password";
    private static final String IS_USE_SSL_PROP = "org.killbill.mail.useSSL";
    private static final String SMTP_FROM_PROP = "org.killbill.mail.from";

    private final boolean useSmtpAuth;
    private final int useSmtpPort;
    private final String smtpUserName;
    private final String smtpUserPassword;
    private final String smtpServerName;
    private final String from;
    private final boolean useSSL;


    private final Logger log = LoggerFactory.getLogger(EmailSender.class);

    public EmailSender(final OSGIConfigPropertiesService configProperties) {
        this.smtpServerName = configProperties.getString(SERVER_NAME_PROP);
        this.useSmtpPort = configProperties.getString(SERVER_PORT_PROP) != null ? Integer.valueOf(configProperties.getString(SERVER_PORT_PROP)) : 25;
        this.smtpUserName = configProperties.getString(SMTP_USER_PROP);
        this.smtpUserPassword = configProperties.getString(SMTP_PWD_PROP);
        this.from = configProperties.getString(SMTP_FROM_PROP);
        this.useSSL = configProperties.getString(IS_USE_SSL_PROP) != null ? Boolean.valueOf(configProperties.getString(IS_USE_SSL_PROP)) : false;
        this.useSmtpAuth = configProperties.getString(IS_SMTP_AUTH_PROP) != null ? Boolean.valueOf(configProperties.getString(IS_SMTP_AUTH_PROP)) : false;
    }


    public void sendHTMLEmail(final List<String> to, final List<String> cc, final String subject, final String htmlBody) throws EmailException {
        final HtmlEmail email = new HtmlEmail();
        email.setHtmlMsg(htmlBody);
        sendEmail(to, cc, subject, email);
    }

    public void sendPlainTextEmail(final List<String> to, final List<String> cc, final String subject, final String body) throws IOException, EmailException {
        final SimpleEmail email = new SimpleEmail();
        email.setMsg(body);
        sendEmail(to, cc, subject, email);
    }

    private void sendEmail(final List<String> to, final List<String> cc, final String subject, final Email email) throws EmailException {
        email.setSmtpPort(useSmtpPort);
        if (useSmtpAuth) {
            email.setAuthentication(smtpUserName, smtpUserPassword);
        }
        email.setHostName(smtpServerName);
        email.setFrom(from);

        email.setSubject(subject);

        if (to != null) {
            for (final String recipient : to) {
                email.addTo(recipient);
            }
        }

        if (cc != null) {
            for (final String recipient : cc) {
                email.addCc(recipient);
            }
        }

        email.setSSL(useSSL);

        log.info("Sending email to {}, cc {}, subject {}", new Object[]{to, cc, subject});
        email.send();
    }
}
