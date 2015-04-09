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

package org.killbill.billing.plugin.notification.email;

import com.google.common.base.Joiner;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.killbill.killbill.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.osgi.service.log.LogService;

import java.io.IOException;
import java.util.List;

public class EmailSender {

    private static final Joiner JOINER_ON_COMMA = Joiner.on(", ");

    /* Reuse Kill Bill email properties; if needed we can have a different set for the plugin */
    private static final String SERVER_NAME_PROP = "org.killbill.mail.smtp.host";
    private static final String SERVER_PORT_PROP = "org.killbill.mail.smtp.port";
    private static final String IS_SMTP_AUTH_PROP = "org.killbill.mail.smtp.auth";
    private static final String SMTP_USER_PROP = "org.killbill.mail.smtp.user";
    private static final String SMTP_PWD_PROP = "org.killbill.mail.smtp.password";
    private static final String IS_USE_SSL_PROP = "org.killbill.mail.useSSL";
    private static final String SMTP_FROM_PROP = "org.killbill.mail.from";

    private static final String DEBUG_LOG_ONLY = "org.killbill.billing.plugin.notification.email.logOnly";

    private final boolean useSmtpAuth;
    private final int useSmtpPort;
    private final String smtpUserName;
    private final String smtpUserPassword;
    private final String smtpServerName;
    private final String from;
    private final boolean useSSL;
    private final LogService logService;
    private final boolean logOnly;

    public EmailSender(final OSGIConfigPropertiesService configProperties, final LogService logService) {
        this(configProperties.getString(SERVER_NAME_PROP),
                (configProperties.getString(SERVER_PORT_PROP) != null ? Integer.valueOf(configProperties.getString(SERVER_PORT_PROP)) : 25),
                configProperties.getString(SMTP_USER_PROP),
                configProperties.getString(SMTP_PWD_PROP),
                configProperties.getString(SMTP_FROM_PROP),
                (configProperties.getString(IS_SMTP_AUTH_PROP) != null ? Boolean.valueOf(configProperties.getString(IS_SMTP_AUTH_PROP)) : false),
                (configProperties.getString(IS_USE_SSL_PROP) != null ? Boolean.valueOf(configProperties.getString(IS_USE_SSL_PROP)) : false),
                logService,
                (configProperties.getString(DEBUG_LOG_ONLY) != null ? Boolean.valueOf(configProperties.getString(DEBUG_LOG_ONLY)) : false));
    }

    public EmailSender(final String smtpServerName, final int useSmtpPort, final String smtpUserName, final String smtpUserPassword, final String from, final boolean useSmtpAuth, final boolean useSSL, final LogService logService, final boolean logOnly) {
        this.useSmtpAuth = useSmtpAuth;
        this.useSmtpPort = useSmtpPort;
        this.smtpUserName = smtpUserName;
        this.smtpUserPassword = smtpUserPassword;
        this.smtpServerName = smtpServerName;
        this.from = from;
        this.useSSL = useSSL;
        this.logService = logService;
        this.logOnly = logOnly;

        logService.log(LogService.LOG_INFO, String.format("EmailSender configured with serverName = %s, serverPort = %d, from = %s, logOnly = %s",
                smtpServerName, useSmtpPort, from, logOnly));

    }

    public void sendHTMLEmail(final List<String> to, final List<String> cc, final String subject, final String htmlBody) throws EmailException {
        final HtmlEmail email = new HtmlEmail();
        email.setHtmlMsg(htmlBody);
        sendEmail(to, cc, subject, email);
    }

    public void sendPlainTextEmail(final List<String> to, final List<String> cc, final String subject, final String body) throws IOException, EmailException {

        logService.log(LogService.LOG_INFO, String.format("Sending email to = %s, cc= %s, subject = %s body = [%s]",
                to,
                JOINER_ON_COMMA.join(cc),
                subject,
                body));

        final SimpleEmail email = new SimpleEmail();
        email.setMsg(body);
        sendEmail(to, cc, subject, email);
    }

    private void sendEmail(final List<String> to, final List<String> cc, final String subject, final Email email) throws EmailException {

        if (logOnly) {
            return;
        }

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

        logService.log(LogService.LOG_INFO, String.format("Sending email to %s, cc %s, subject %s", to, cc, subject));
        email.send();
    }
}
