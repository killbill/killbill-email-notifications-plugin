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

package org.killbill.billing.plugin.notification.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooby.Result;
import org.killbill.billing.notification.plugin.api.ExtBusEventType;
import org.killbill.billing.plugin.notification.TestBase;
import org.killbill.billing.plugin.notification.dao.gen.tables.pojos.EmailNotificationsConfiguration;
import org.killbill.billing.tenant.api.Tenant;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestEmailNotificationServlet extends TestBase {

    @Test(groups = "slow")
    public void updateConfiguration() throws IOException {
        final UUID kbTenantId = UUID.randomUUID();
        final UUID kbAccountId = UUID.randomUUID();
        final Optional<ExtBusEventType> event = Optional.of(ExtBusEventType.INVOICE_PAYMENT_SUCCESS);

        final List<ExtBusEventType> eventTypes = new ArrayList<ExtBusEventType>();
        eventTypes.add(ExtBusEventType.INVOICE_PAYMENT_SUCCESS);
        eventTypes.add(ExtBusEventType.SUBSCRIPTION_CANCEL);

        final EmailNotificationServlet servlet = new EmailNotificationServlet(dao, osgiClock);
        Result result = servlet.isListening();
        Assert.assertEquals(result.status().get().value(), 200);

        result = servlet.getEventTypesPerAccount(kbAccountId, buildTenant(kbTenantId), event);
        Assert.assertEquals(result.status().get().value(), 404);

        result = servlet.doUpdateEventTypePerAccount(kbAccountId, buildTenant(kbTenantId), eventTypes);
        Assert.assertEquals(result.status().get().value(), 201);

        result = servlet.getEventTypesPerAccount(kbAccountId, buildTenant(kbTenantId), event);
        final EmailNotificationsConfiguration configuration = result.get();
        Assert.assertEquals(configuration.getKbAccountId(), kbAccountId.toString());
        Assert.assertEquals(configuration.getKbTenantId(), kbTenantId.toString());
        Assert.assertEquals(configuration.getEventType(), ExtBusEventType.INVOICE_PAYMENT_SUCCESS.toString());
        Assert.assertEquals(result.status().get().value(), 200);

    }

    @Test(groups = "slow")
    public void updateMulipleConfiguration() throws IOException {
        Result result = null;
        final List<UUID> kbAccountIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            kbAccountIds.add(UUID.randomUUID());
        }

        final UUID kbTenantId = UUID.randomUUID();
        final Tenant tenant = buildTenant(kbTenantId);

        final List<ExtBusEventType> eventTypes = new ArrayList<ExtBusEventType>();
        eventTypes.add(ExtBusEventType.INVOICE_PAYMENT_SUCCESS);
        eventTypes.add(ExtBusEventType.SUBSCRIPTION_CANCEL);
        final EmailNotificationServlet servlet = new EmailNotificationServlet(dao, osgiClock);

        for (final UUID kbAccountId : kbAccountIds) {
            result = servlet.doUpdateEventTypePerAccount(kbAccountId, tenant, eventTypes);
            Assert.assertEquals(result.status().get().value(), 201);
        }

        result = servlet.getEventTypes(kbAccountIds, buildTenant(kbTenantId));
        final List<EmailNotificationsConfiguration> configurations = result.get();
        Assert.assertEquals(configurations.size(), kbAccountIds.size() * eventTypes.size());
        Assert.assertEquals(result.status().get().value(), 200);
    }

    private Tenant buildTenant(final UUID kbTenantId) throws IOException {
        final Tenant tenant = (Tenant) Mockito.mock(Tenant.class);
        Mockito.when(tenant.getId()).thenReturn(kbTenantId);

        return tenant;
    }
}
