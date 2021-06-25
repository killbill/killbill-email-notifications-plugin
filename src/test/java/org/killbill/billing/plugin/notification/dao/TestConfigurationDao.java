/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014-2020 Groupon, Inc
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

package org.killbill.billing.plugin.notification.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.joda.time.DateTime;
import org.killbill.billing.notification.plugin.api.ExtBusEventType;
import org.killbill.billing.plugin.TestWithEmbeddedDBBase;
import org.killbill.billing.plugin.notification.TestBase;
import org.killbill.billing.plugin.notification.dao.gen.tables.pojos.EmailNotificationsConfiguration;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestConfigurationDao extends TestBase {

    @Test(groups = "slow")
    public void testUpdateEventTypePerAccount() throws Exception {
        final UUID kbTenantId = UUID.randomUUID();
        final UUID kbAccountId = UUID.randomUUID();

        List<ExtBusEventType> eventTypes = new ArrayList<ExtBusEventType>();
        eventTypes.add(ExtBusEventType.INVOICE_PAYMENT_SUCCESS);
        eventTypes.add(ExtBusEventType.SUBSCRIPTION_CANCEL);

        dao.updateConfigurationPerAccount(kbAccountId,kbTenantId,eventTypes,DateTime.now());

        List<EmailNotificationsConfiguration> configuration = dao.getEventTypesPerAccount(kbAccountId, kbTenantId);
        Assert.assertNotNull(configuration);
        Assert.assertEquals(configuration.size(),2);
        Assert.assertEquals(dao.getEventTypesPerAccount(kbAccountId, UUID.randomUUID()).size(),0);
        Assert.assertTrue(eventTypes.contains(ExtBusEventType.valueOf(configuration.get(0).getEventType())));
        Assert.assertTrue(eventTypes.contains(ExtBusEventType.valueOf(configuration.get(1).getEventType())));
        Assert.assertEquals(configuration.get(0).getKbAccountId(),kbAccountId.toString());
        Assert.assertEquals(configuration.get(0).getKbTenantId(),kbTenantId.toString());

        EmailNotificationsConfiguration event = dao.getEventTypePerAccount(kbAccountId, kbTenantId, ExtBusEventType.INVOICE_PAYMENT_SUCCESS);
        Assert.assertNotNull(event);
        Assert.assertEquals(event.getKbAccountId(),kbAccountId.toString());
        Assert.assertEquals(event.getKbTenantId(),kbTenantId.toString());
        Assert.assertEquals(event.getEventType(), ExtBusEventType.INVOICE_PAYMENT_SUCCESS.toString());

        eventTypes = new ArrayList<ExtBusEventType>();
        eventTypes.add(ExtBusEventType.INVOICE_PAYMENT_SUCCESS);
        eventTypes.add(ExtBusEventType.INVOICE_PAYMENT_FAILED);

        dao.updateConfigurationPerAccount(kbAccountId,kbTenantId,eventTypes,DateTime.now());

        configuration = dao.getEventTypesPerAccount(kbAccountId, kbTenantId);
        Assert.assertNotNull(configuration);
        Assert.assertEquals(configuration.size(),2);
        Assert.assertEquals(dao.getEventTypesPerAccount(kbAccountId, UUID.randomUUID()).size(),0);
        Assert.assertTrue(eventTypes.contains(ExtBusEventType.valueOf(configuration.get(0).getEventType())));
        Assert.assertTrue(eventTypes.contains(ExtBusEventType.valueOf(configuration.get(1).getEventType())));
        Assert.assertEquals(configuration.get(0).getKbAccountId(),kbAccountId.toString());
        Assert.assertEquals(configuration.get(0).getKbTenantId(),kbTenantId.toString());

        event = dao.getEventTypePerAccount(kbAccountId, kbTenantId, ExtBusEventType.INVOICE_PAYMENT_FAILED);
        Assert.assertNotNull(event);
        Assert.assertEquals(event.getKbAccountId(),kbAccountId.toString());
        Assert.assertEquals(event.getKbTenantId(),kbTenantId.toString());
        Assert.assertEquals(event.getEventType(), ExtBusEventType.INVOICE_PAYMENT_FAILED.toString());
    }
}
