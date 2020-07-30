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

public class EmailNotificationException extends Exception{

    private static final long serialVersionUID = 7842747221740742120L;

    private final int code;
    private final String message;
    private final Throwable cause;

    public EmailNotificationException(final Throwable cause, final int code, final String msg) {
        this.message = msg;
        this.code = code;
        this.cause = cause;
    }

    public EmailNotificationException(final Throwable cause, final EmailNotificationErrorCode errorCode, final Object... args){
        this(cause, errorCode.getCode(), errorCode.getFormat(args));
    }

    public EmailNotificationException(final EmailNotificationErrorCode errorCode, final Object... args){
        this(null, errorCode.getCode(), errorCode.getFormat(args));
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{cause=").append(cause);
        sb.append(", code=").append(code);
        sb.append(", formattedMsg='").append(this.getMessage()).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
