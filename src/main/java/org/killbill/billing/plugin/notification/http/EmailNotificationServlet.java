/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2020-2020 Equinix, Inc
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

package org.killbill.billing.plugin.notification.http;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.jooby.Result;
import org.jooby.Results;
import org.jooby.mvc.Body;
import org.jooby.mvc.GET;
import org.jooby.mvc.Local;
import org.jooby.mvc.POST;
import org.jooby.mvc.Path;
import org.killbill.billing.notification.plugin.api.ExtBusEventType;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillClock;
import org.killbill.billing.plugin.notification.dao.ConfigurationDao;
import org.killbill.billing.plugin.notification.setup.EmailNotificationListener;
import org.killbill.billing.tenant.api.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Path("/v1")
public class EmailNotificationServlet {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationServlet.class);

    private ConfigurationDao dao;
    private OSGIKillbillClock clock;

    @Inject
    public EmailNotificationServlet(final ConfigurationDao dao, final OSGIKillbillClock clock) {
        this.clock = clock;
        this.dao = dao;
    }

    @GET
    @Path("/")
    public Result isListening() {
        logger.debug("Acknowledge from email notification plugin!!!, I am active and listening");
        return Results.ok();
    }

    @GET
    @Path("/eventsToConsider")
    public Result getEventsToConsider() {
        return Results.json(EmailNotificationListener.EVENTS_TO_CONSIDER);
    }

    @GET
    @Path("/accounts")
    public Result getEventTypes(@Named("kbAccountId") final List<UUID> kbAccountId,
                                @Local @Named("killbill_tenant") final Tenant tenant) {
        final UUID kbTenantId = tenant.getId();

        return EmailNotificationService.getEventTypes(this.dao, kbAccountId, kbTenantId);
    }

    @GET
    @Path("/accounts/:kbAccountId")
    public Result getEventTypesPerAccount(@Named("kbAccountId") final UUID kbAccountId,
                                          @Local @Named("killbill_tenant") final Tenant tenant,
                                          final Optional<ExtBusEventType> eventType) {
        final UUID kbTenantId = tenant.getId();

        if (!eventType.isPresent()) {
            return EmailNotificationService.getEventTypesPerAccount(this.dao, kbAccountId, kbTenantId);
        } else {
            return EmailNotificationService.getEventTypePerAccount(this.dao, kbAccountId, kbTenantId, eventType.get());
        }
    }

    @POST
    @Path("/accounts/:kbAccountId")
    public Result doUpdateEventTypePerAccount(@Named("kbAccountId") final UUID kbAccountId,
                                              @Local @Named("killbill_tenant") final Tenant tenant,
                                              @Body final List<ExtBusEventType> eventTypes) {
        final UUID kbTenantId = tenant.getId();

        return EmailNotificationService.doUpdateEventTypePerAccount(this.dao, kbAccountId, kbTenantId, eventTypes, this.clock.getClock().getUTCNow());
    }
}
