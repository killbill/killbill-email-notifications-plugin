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

package org.killbill.billing.plugin.notification.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.jooby.Result;
import org.killbill.billing.notification.plugin.api.ExtBusEventType;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillClock;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillDataSource;
import org.killbill.billing.plugin.TestWithEmbeddedDBBase;
import org.killbill.billing.plugin.notification.dao.gen.tables.pojos.EmailNotificationsConfiguration;
import org.killbill.billing.tenant.api.Tenant;
import org.killbill.clock.ClockMock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestEmailNotificationServlet extends TestWithEmbeddedDBBase {

    private final Logger log = LoggerFactory.getLogger(TestEmailNotificationServlet.class);

    protected OSGIKillbillDataSource dataSource;
    protected OSGIKillbillClock clock;

    @BeforeMethod(groups = "slow")
    public void setup() throws IOException {

        dataSource = buildOSGIKillbillDataSource(embeddedDB.getDataSource());
        clock = buildOSGIKillbillClock(new ClockMock());

    }

    @Test(groups = "slow")
    public void updateConfiguration() throws IOException {
        final UUID kbTenantId = UUID.randomUUID();
        final UUID kbAccountId = UUID.randomUUID();
        Optional<ExtBusEventType> event = Optional.of(ExtBusEventType.INVOICE_PAYMENT_SUCCESS);

        List<ExtBusEventType> eventTypes = new ArrayList<ExtBusEventType>();
        eventTypes.add(ExtBusEventType.INVOICE_PAYMENT_SUCCESS);
        eventTypes.add(ExtBusEventType.SUBSCRIPTION_CANCEL);

        EmailNotificationServlet servlet =  new EmailNotificationServlet(dataSource,clock);
        Result result = servlet.hello();
        Assert.assertEquals(result.status().get().value(),200);

        result = servlet.getEventTypes(kbAccountId, buildTenant(kbTenantId), event);
        Assert.assertEquals(result.status().get().value(),404);

        result = servlet.doUpdateEventTypePerAccount(kbAccountId, buildTenant(kbTenantId), eventTypes);
        Assert.assertEquals(result.status().get().value(),201);

        result = servlet.getEventTypes(kbAccountId, buildTenant(kbTenantId), event);
        EmailNotificationsConfiguration configuration = result.get();
        Assert.assertEquals(configuration.getKbAccountId(),kbAccountId.toString());
        Assert.assertEquals(configuration.getKbTenantId(),kbTenantId.toString());
        Assert.assertEquals(configuration.getEventType(),ExtBusEventType.INVOICE_PAYMENT_SUCCESS.toString());
        Assert.assertEquals(result.status().get().value(),200);
    }

    private Tenant buildTenant(UUID kbTenantId) throws IOException {
        final Tenant tenant = (Tenant) Mockito.mock(Tenant.class);
        Mockito.when(tenant.getId()).thenReturn(kbTenantId);

        return tenant;
    }

    private OSGIKillbillDataSource buildOSGIKillbillDataSource(DataSource _dataSource) throws IOException {
        final OSGIKillbillDataSource dataSource = (OSGIKillbillDataSource) Mockito.mock(OSGIKillbillDataSource.class);
        Mockito.when(dataSource.getDataSource()).thenReturn(_dataSource);

        return dataSource;
    }

    private OSGIKillbillClock buildOSGIKillbillClock(ClockMock _clock) {
        final OSGIKillbillClock clock = (OSGIKillbillClock) Mockito.mock(OSGIKillbillClock.class);
        Mockito.when(clock.getClock()).thenReturn(_clock);

        return clock;
    }
}
