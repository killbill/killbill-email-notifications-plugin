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

package org.killbill.billing.plugin.notification.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.joda.time.DateTime;
import org.killbill.billing.notification.plugin.api.ExtBusEventType;
import org.killbill.billing.plugin.TestWithEmbeddedDBBase;
import org.killbill.billing.plugin.notification.dao.gen.tables.pojos.EmailNotificationsConfiguration;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class TestConfigurationDao extends TestWithEmbeddedDBBase {

    private ConfigurationDao dao;

    @BeforeMethod(groups = "slow")
    public void setUp() throws Exception {
        dao = new ConfigurationDao(embeddedDB.getDataSource());
    }

    @Test(groups = "slow")
    public void testUpdateEventTypePerAccount() throws Exception {
        final UUID kbTenantId = UUID.randomUUID();
        final UUID kbAccountId = UUID.randomUUID();
        final List<ExtBusEventType> eventTypes = new ArrayList<ExtBusEventType>();
        eventTypes.add(ExtBusEventType.INVOICE_PAYMENT_SUCCESS);
        eventTypes.add(ExtBusEventType.SUBSCRIPTION_CANCEL);

        dao.updateEventTypePerAccount(kbAccountId,kbTenantId,eventTypes,DateTime.now());

        final List<EmailNotificationsConfiguration> configuration = dao.getEventTypes(kbAccountId, kbTenantId);

        Assert.assertNotNull(configuration);
        Assert.assertNotEquals(configuration.size(),0);
        Assert.assertEquals(dao.getEventTypes(kbAccountId, UUID.randomUUID()).size(),0);
        Assert.assertTrue(eventTypes.contains(configuration.get(0).getEventType()));
    }
}
