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

package org.killbill.billing.plugin.notification.exception;

public enum EmailNotificationErrorCode {

    UNKNOWN_ERROR(-1,"Unknown error occurs"),
    TRANSLATION_INVALID(100001, "Translation for locale [%s] isn't found"),
    TEMPLATE_INVALID(1000002, "Template for locale [%s] isn't found"),;

    private final int code;
    private final String format;

    EmailNotificationErrorCode(final int code, final String format) {
        this.code = code;
        this.format = format;
    }

    public String getFormat(){ return getFormat(null); }

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
