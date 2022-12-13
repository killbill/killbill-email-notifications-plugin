/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2020-2020 Equinix, Inc
 * Copyright 2014-2021 The Billing Project, LLC
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

import java.io.IOException;
import java.util.List;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.killbill.billing.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.billing.plugin.notification.exception.EmailNotificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

import static org.killbill.billing.plugin.notification.exception.EmailNotificationErrorCode.EMAIL_ADDRESS_INVALID;
import static org.killbill.billing.plugin.notification.exception.EmailNotificationErrorCode.RECIPIENT_EMAIL_ADDRESS_REQUIRED;
import static org.killbill.billing.plugin.notification.exception.EmailNotificationErrorCode.SENDER_EMAIL_ADDRESS_REQUIRED;
import static org.killbill.billing.plugin.notification.exception.EmailNotificationErrorCode.SMTP_AUTHENTICATION_REQUIRED;
import static org.killbill.billing.plugin.notification.exception.EmailNotificationErrorCode.SMTP_HOSTNAME_REQUIRED;
import static org.killbill.billing.plugin.notification.exception.EmailNotificationErrorCode.SUBJECT_REQUIRED;

public class EmailSender {

    private static final Joiner JOINER_ON_COMMA = Joiner.on(", ");

    private static final Logger logger = LoggerFactory.getLogger(EmailSender.class);

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

    private final boolean logOnly;

    public EmailSender(final OSGIConfigPropertiesService configProperties) {
        this(configProperties.getString(SERVER_NAME_PROP),
             (configProperties.getString(SERVER_PORT_PROP) != null ? Integer.parseInt(configProperties.getString(SERVER_PORT_PROP)) : 25),
             configProperties.getString(SMTP_USER_PROP),
             configProperties.getString(SMTP_PWD_PROP),
             configProperties.getString(SMTP_FROM_PROP),
             (configProperties.getString(IS_SMTP_AUTH_PROP) != null ? Boolean.valueOf(configProperties.getString(IS_SMTP_AUTH_PROP)) : false),
             (configProperties.getString(IS_USE_SSL_PROP) != null ? Boolean.valueOf(configProperties.getString(IS_USE_SSL_PROP)) : false),
             (configProperties.getString(DEBUG_LOG_ONLY) != null ? Boolean.valueOf(configProperties.getString(DEBUG_LOG_ONLY)) : false));
    }

    public EmailSender(final String smtpServerName, final int useSmtpPort, final String smtpUserName, final String smtpUserPassword, final String from, final boolean useSmtpAuth, final boolean useSSL, final boolean logOnly) {
        this.useSmtpAuth = useSmtpAuth;
        this.useSmtpPort = useSmtpPort;
        this.smtpUserName = smtpUserName;
        this.smtpUserPassword = smtpUserPassword;
        this.smtpServerName = smtpServerName;
        this.from = from;
        this.useSSL = useSSL;
        this.logOnly = logOnly;
    }

    // Backward compatibility. If no configuration exists, then reuse Kill Bill email properties
    public SmtpProperties precheckSmtp(SmtpProperties smtp) {

        if (smtp.getHost() == null && smtpServerName != null){
            smtp.setHost(smtpServerName);
            smtp.setDefaultSender(this.from);
            smtp.setPort(this.useSmtpPort);
            smtp.setPassword(this.smtpUserPassword);
            smtp.setUserName(this.smtpUserName);
            smtp.setUseAuthentication(this.useSmtpAuth);
            smtp.setUseSSL(this.useSSL);
        }

        logger.info("EmailSender configured with serverName={}, serverPort={}, from={}, logOnly={}",
                    smtp.getHost(), smtp.getPort(), smtp.getFrom(), logOnly);

        return smtp;
    }

    public void sendHTMLEmail(final List<String> to, final List<String> cc, final String subject, final String htmlBody, final SmtpProperties smtp) throws EmailException, EmailNotificationException {
        logger.debug("Sending email to={}, cc={}, subject={}, body=[{}]",
                                                           to,
                                                           JOINER_ON_COMMA.join(cc),
                                                           subject,
                                                           htmlBody);
        final HtmlEmail email = new HtmlEmail();
        email.setHtmlMsg(htmlBody);
        email.setCharset("utf-8");
        sendEmail(to, cc, subject, email, precheckSmtp(smtp));
    }

    public void sendPlainTextEmail(final List<String> to, final List<String> cc, final String subject, final String body, final SmtpProperties smtp) throws IOException, EmailException, EmailNotificationException {
        logger.debug("Sending email to={}, cc={}, subject={}, body=[{}]",
                     to,
                     JOINER_ON_COMMA.join(cc),
                     subject,
                     body);

        final SimpleEmail email = new SimpleEmail();
        email.setCharset("utf-8");
        email.setMsg(body);
        sendEmail(to, cc, subject, email, precheckSmtp(smtp));
    }

    private void sendEmail(final List<String> to, final List<String> cc, final String subject, final Email email, final SmtpProperties smtp) throws EmailException, EmailNotificationException {

        if (logOnly) {
            return;
        }

        validateEmailFields(to, cc, subject, smtp);

        email.setSmtpPort(smtp.getPort());
        if (smtp.isUseAuthentication()) {
            email.setAuthentication(smtp.getUserName(), smtp.getPassword());
        }
        email.setHostName(smtp.getHost());
        email.setFrom(smtp.getFrom());

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

        email.setSSLOnConnect(smtp.isUseSSL());

        logger.info("Sending email to={}, cc={}, subject={}", to, cc, subject);
        email.send();
    }

    private void validateEmailFields(final List<String> to, final List<String> cc, final String subject, final SmtpProperties smtp) throws EmailNotificationException {

        if (to == null || to.size() == 0 || to.get(0).trim().isEmpty()){
            throw new EmailNotificationException(RECIPIENT_EMAIL_ADDRESS_REQUIRED);
        }

        if (smtp.getFrom() == null || smtp.getFrom().trim().isEmpty()){
            throw new EmailNotificationException(SENDER_EMAIL_ADDRESS_REQUIRED);
        }

        if (smtp.isUseAuthentication() && ( (smtp.getUserName() == null || smtp.getUserName().trim().isEmpty()) || (smtp.getPassword() == null || smtp.getPassword().trim().isEmpty()) )){
            throw new EmailNotificationException(SMTP_AUTHENTICATION_REQUIRED);
        }

        if (subject == null || subject.trim().isEmpty()){
            throw new EmailNotificationException(SUBJECT_REQUIRED);
        }

        if (smtp.getHost() == null || smtp.getHost().trim().isEmpty()){
            throw new EmailNotificationException(SMTP_HOSTNAME_REQUIRED);
        }

        validateEmailAddress(smtp.getFrom());

        for(String recipient: to){
            validateEmailAddress(recipient);
        }

        for(String recipient: cc){
            validateEmailAddress(recipient);
        }
    }

    private void validateEmailAddress(String email) throws EmailNotificationException {
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException ex) {
            throw new EmailNotificationException(ex, EMAIL_ADDRESS_INVALID, email);
        }
    }

}
