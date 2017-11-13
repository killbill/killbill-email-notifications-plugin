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
import java.util.UUID;

import org.joda.time.DateTime;
import org.jooby.Result;
import org.jooby.Results;
import org.jooby.Status;
import org.killbill.billing.notification.plugin.api.ExtBusEventType;
import org.killbill.billing.plugin.notification.dao.gen.tables.pojos.EmailNotificationsConfiguration;
import org.killbill.billing.plugin.notification.dao.ConfigurationDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EmailNotificationService
{
    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    private EmailNotificationService(){}

    public static Result getEventTypes(final ConfigurationDao dao, final List<UUID> kbAccountId, final UUID kbTenantId)
    {
        logger.debug(String.format("Enters get event types for %s accounts - %s",kbAccountId.size(),kbTenantId));

        List<EmailNotificationsConfiguration> eventTypes = null;
        try {
            eventTypes = dao.getEventTypes(kbAccountId, kbTenantId);
        } catch (SQLException e) {
            logger.error(e.getMessage());
            return Results.with(e.getMessage(), Status.SERVER_ERROR);
        }

        return eventTypes == null  || eventTypes.size() == 0? Results.with(Status.NOT_FOUND) : Results.json(eventTypes);
    }

    public static Result getEventTypesPerAccount(final ConfigurationDao dao, final UUID kbAccountId, final UUID kbTenantId)
    {
        logger.debug(String.format("Enters get event types %s - %s",kbAccountId,kbTenantId));

        List<EmailNotificationsConfiguration> eventTypes = null;
        try {
            eventTypes = dao.getEventTypesPerAccount(kbAccountId, kbTenantId);
        } catch (SQLException e) {
            logger.error(e.getMessage());
            return Results.with(e.getMessage(), Status.SERVER_ERROR);
        }

        return eventTypes == null  || eventTypes.size() == 0? Results.with(Status.NOT_FOUND) : Results.json(eventTypes);
    }

    public static Result getEventTypePerAccount(final ConfigurationDao dao, final UUID kbAccountId, final UUID kbTenantId, final ExtBusEventType eventType )
    {
        logger.debug(String.format("Enters get event type %s - %s",kbAccountId,kbTenantId));

        EmailNotificationsConfiguration eventTypeRsp = null;
        try {
            eventTypeRsp = dao.getEventTypePerAccount(kbAccountId, kbTenantId, eventType);
        } catch (SQLException e) {
            logger.error(e.getMessage());
            return Results.with(e.getMessage(), Status.SERVER_ERROR);
        }

        return eventTypeRsp == null ? Results.with(Status.NOT_FOUND) : Results.json(eventTypeRsp);
    }

    public static Result doUpdateEventTypePerAccount(final ConfigurationDao dao, final UUID kbAccountId,
                                                     final UUID kbTenantId, final List<ExtBusEventType> eventTypes,
                                                     final DateTime utcNow)
    {
        logger.debug(String.format("Enters update event types per account %s",kbAccountId));

        if (kbTenantId == null)
        {
            return Results.with("No tenant specified",Status.NOT_FOUND);
        }
        if (kbAccountId == null)
        {
            return Results.with("No account specified",Status.NOT_FOUND);
        }

        try {
            dao.updateConfigurationPerAccount(kbAccountId,kbTenantId,eventTypes,utcNow);
        } catch (SQLException e) {
            logger.error(e.getMessage());
            return Results.with(e.getMessage(), Status.SERVER_ERROR);
        }

        return Results.with(Status.CREATED);
    }

}
