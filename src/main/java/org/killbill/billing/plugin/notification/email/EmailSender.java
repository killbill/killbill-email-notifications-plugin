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

import java.io.IOException;
import java.util.List;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailSender {

    private final boolean useSmtpAuth;
    private final int useSmtpPort;
    private final String smtpUserName;
    private final String smtpUserPassword;
    private final String smtpServerName;
    private final String from;
    private final boolean useSSL;


    private final Logger log = LoggerFactory.getLogger(EmailSender.class);

    public EmailSender(String from, String smtpUserName, String smtpUserPassword, String smtpServerName, int useSmtpPort, boolean useSmtpAuth, boolean useSSL) {
        this.useSmtpAuth = useSmtpAuth;
        this.useSmtpPort = useSmtpPort;
        this.smtpUserName = smtpUserName;
        this.smtpUserPassword = smtpUserPassword;
        this.smtpServerName = smtpServerName;
        this.from = from;
        this.useSSL = useSSL;
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
