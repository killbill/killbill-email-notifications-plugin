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

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jooby.Results;
import org.jooby.Result;
import org.jooby.mvc.GET;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;
import org.killbill.billing.notification.plugin.api.ExtBusEventType;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillClock;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillDataSource;
import org.killbill.billing.plugin.notification.dao.ConfigurationDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Path("/")
public class EmailNotificationServlet {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationServlet.class);

    private OSGIKillbillDataSource dataSource;
    private ConfigurationDao dao;
    private OSGIKillbillClock clock;

    @Inject
    public EmailNotificationServlet(OSGIKillbillDataSource dataSource, OSGIKillbillClock clock)
    {
        this.dataSource = dataSource;
        this.clock = clock;

        try {
            this.dao = new ConfigurationDao(this.dataSource.getDataSource());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @GET
    public Result hello() {
        // Find me on http://127.0.0.1:8080/plugins/killbill-email-notifications
        logger.info("Hello from email notification plugin!!!");
        return Results.ok();
    }

    @GET
    @Path("/tenant/:kbTenantId/account/:kbAccountId")
    public Result getEventTypes(final UUID kbAccountId, final UUID kbTenantId, Optional<ExtBusEventType> eventType) {
        if (!eventType.isPresent())
        {
            return EmailNotificationService.getEventTypes(this.dao, kbAccountId, kbTenantId);
        }
        else
        {
            return EmailNotificationService.getEventType(this.dao, kbAccountId, kbTenantId, eventType.get());

        }
    }

    @GET
    @Path("/tenant/:kbTenantId")
    public Result getEventTypesPerTenant(final UUID kbTenantId) {
        return EmailNotificationService.getEventTypesPerTenant(this.dao, kbTenantId);
    }

    @POST
    @Path("/tenant/:kbTenantId/account/:kbAccountId")
    public Result doUpdateEventTypePerAccount(final UUID kbAccountId, final UUID kbTenantId, final List<ExtBusEventType> eventTypes){
        return EmailNotificationService.doUpdateEventTypePerAccount(this.dao, kbAccountId, kbTenantId, eventTypes,
                                                                    this.clock.getClock().getUTCNow());
    }

    @POST
    @Path("/tenant/:kbTenantId")
    public Result doUpdateEventTypePerTenant(final UUID kbTenantId, final List<ExtBusEventType> eventTypes){
        return EmailNotificationService.doUpdateEventTypePerTenant(this.dao, kbTenantId, eventTypes,
                                                                    this.clock.getClock().getUTCNow());
    }
}
