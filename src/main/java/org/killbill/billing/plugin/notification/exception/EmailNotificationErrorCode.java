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

package org.killbill.billing.plugin.notification.exception;

public enum EmailNotificationErrorCode {

    UNKNOWN_ERROR(-1,"Unknown error occurs"),
    TRANSLATION_INVALID(100001, "Translation for locale [%s] isn't found"),
    TEMPLATE_INVALID(1000002, "Template for locale [%s] isn't found"),

    RECIPIENT_EMAIL_ADDRESS_REQUIRED(1000003, "Recipient address is required, but none is provided."),
    SENDER_EMAIL_ADDRESS_REQUIRED(1000004, "Sender address is required, but none is provided."),
    SUBJECT_REQUIRED(1000005, "Subject is required, but none is provided."),
    SMTP_HOSTNAME_REQUIRED(1000006, "Host name is required, but none is provided."),
    SMTP_AUTHENTICATION_REQUIRED(1000007, "Authentication credentials are required, but none is provided."),

    EMAIL_ADDRESS_INVALID(1000010, "Email address %s is malformed."),;


    private final int code;
    private final String format;

    EmailNotificationErrorCode(final int code, final String format) {
        this.code = code;
        this.format = format;
    }

    public String getFormat(Object... args) {
        String formattedMessage = this.format;

        if (args != null && this.format.contains("%")){
            formattedMessage = String.format(this.format, args);
        }

        return formattedMessage;
    }

    public int getCode() {
        return code;
    }

    public static EmailNotificationErrorCode fromCode(final int code) {
        for (final EmailNotificationErrorCode errorCode : EmailNotificationErrorCode.values()) {
            if (errorCode.getCode() == code) {
                return errorCode;
            }
        }
        return UNKNOWN_ERROR;
    }
}
