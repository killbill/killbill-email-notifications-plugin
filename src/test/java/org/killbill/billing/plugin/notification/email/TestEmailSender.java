/*
 * Copyright 2015-2020 Groupon, Inc
 * Copyright 2015-2021 The Billing Project, LLC
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

import org.apache.commons.mail.EmailException;
import org.killbill.billing.plugin.notification.exception.EmailNotificationException;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

public class TestEmailSender {

    private static final String TEST_SMTP_SERVER_NAME = "127.0.0.1";
    private static final int TEST_SMPT_SERVER_PORT = 2525;
    private static final String TEST_SMTP_USER = "foo";
    private static final String TEST_SMTP_PWD = "bar";
    private static final String TEST_SMTP_FROM = "caramel@mou.com";

    @Test(enabled = false)
    public void foo() throws IOException, EmailException, EmailNotificationException {
        EmailSender sender = new EmailSender(TEST_SMTP_SERVER_NAME, TEST_SMPT_SERVER_PORT, TEST_SMTP_USER, TEST_SMTP_PWD, TEST_SMTP_FROM, true, false, false);
        final String to = "something_that_works@gmail.com";
        sender.sendPlainTextEmail(ImmutableList.of(to), ImmutableList.<String>of(), "coucou", "body", Mockito.mock(SmtpProperties.class));
    }
}
