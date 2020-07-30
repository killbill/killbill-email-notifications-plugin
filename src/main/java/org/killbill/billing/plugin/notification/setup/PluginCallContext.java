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

package org.killbill.billing.plugin.notification.setup;

import org.joda.time.DateTime;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.CallOrigin;
import org.killbill.billing.util.callcontext.UserType;

import java.util.UUID;

public class PluginCallContext implements CallContext {

    protected final UUID userToken;
    protected final String userName;
    protected final CallOrigin callOrigin;
    protected final UserType userType;
    protected final String reasonCode;
    protected final String comments;
    protected final DateTime createdDate;
    protected final DateTime updatedDate;
    protected final UUID accountId;
    protected final UUID tenantId;

    public PluginCallContext(final String pluginName, final DateTime utcNow, final UUID accountId, final UUID tenantId) {
        this(UUID.randomUUID(),
                pluginName,
                CallOrigin.EXTERNAL,
                UserType.SYSTEM,
                null,
                null,
                utcNow,
                utcNow,
                accountId,
                tenantId);
    }

    public PluginCallContext(final UUID userToken,
                             final String userName,
                             final CallOrigin callOrigin,
                             final UserType userType,
                             final String reasonCode,
                             final String comments,
                             final DateTime createdDate,
                             final DateTime updatedDate,
                             final UUID accountId,
                             final UUID tenantId) {
        this.userToken = userToken;
        this.userName = userName;
        this.callOrigin = callOrigin;
        this.userType = userType;
        this.reasonCode = reasonCode;
        this.comments = comments;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        this.accountId = accountId;
        this.tenantId = tenantId;
    }

    @Override
    public UUID getUserToken() {
        return userToken;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    @Override
    public CallOrigin getCallOrigin() {
        return callOrigin;
    }

    @Override
    public UserType getUserType() {
        return userType;
    }

    @Override
    public String getReasonCode() {
        return reasonCode;
    }

    @Override
    public String getComments() {
        return comments;
    }

    @Override
    public DateTime getCreatedDate() {
        return createdDate;
    }

    @Override
    public DateTime getUpdatedDate() {
        return updatedDate;
    }

    @Override
    public UUID getAccountId() {
        return accountId;
    }

    @Override
    public UUID getTenantId() {
        return tenantId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PluginCallContext{");
        sb.append("userToken=").append(userToken);
        sb.append(", userName='").append(userName).append('\'');
        sb.append(", callOrigin=").append(callOrigin);
        sb.append(", userType=").append(userType);
        sb.append(", reasonCode='").append(reasonCode).append('\'');
        sb.append(", comments='").append(comments).append('\'');
        sb.append(", createdDate=").append(createdDate);
        sb.append(", updatedDate=").append(updatedDate);
        sb.append(", accountId=").append(accountId);
        sb.append(", tenantId=").append(tenantId);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final PluginCallContext that = (PluginCallContext) o;

        if (callOrigin != that.callOrigin) {
            return false;
        }
        if (comments != null ? !comments.equals(that.comments) : that.comments != null) {
            return false;
        }
        if (createdDate != null ? !createdDate.equals(that.createdDate) : that.createdDate != null) {
            return false;
        }
        if (reasonCode != null ? !reasonCode.equals(that.reasonCode) : that.reasonCode != null) {
            return false;
        }
        if (accountId != null ? !accountId.equals(that.accountId) : that.accountId != null) {
            return false;
        }
        if (tenantId != null ? !tenantId.equals(that.tenantId) : that.tenantId != null) {
            return false;
        }
        if (updatedDate != null ? !updatedDate.equals(that.updatedDate) : that.updatedDate != null) {
            return false;
        }
        if (userName != null ? !userName.equals(that.userName) : that.userName != null) {
            return false;
        }
        if (userToken != null ? !userToken.equals(that.userToken) : that.userToken != null) {
            return false;
        }
        if (userType != that.userType) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = userToken != null ? userToken.hashCode() : 0;
        result = 31 * result + (userName != null ? userName.hashCode() : 0);
        result = 31 * result + (callOrigin != null ? callOrigin.hashCode() : 0);
        result = 31 * result + (userType != null ? userType.hashCode() : 0);
        result = 31 * result + (reasonCode != null ? reasonCode.hashCode() : 0);
        result = 31 * result + (comments != null ? comments.hashCode() : 0);
        result = 31 * result + (createdDate != null ? createdDate.hashCode() : 0);
        result = 31 * result + (updatedDate != null ? updatedDate.hashCode() : 0);
        result = 31 * result + (accountId != null ? accountId.hashCode() : 0);
        result = 31 * result + (tenantId != null ? tenantId.hashCode() : 0);
        return result;
    }
}
