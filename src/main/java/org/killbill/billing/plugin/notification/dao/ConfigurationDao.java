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

package org.killbill.billing.plugin.notification.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.sql.DataSource;

import org.joda.time.DateTime;

import org.jooq.impl.DSL;
import org.killbill.billing.notification.plugin.api.ExtBusEventType;
import org.killbill.billing.plugin.dao.PluginDao;

import org.killbill.billing.plugin.notification.dao.gen.Tables;
import org.killbill.billing.plugin.notification.dao.gen.tables.pojos.EmailNotificationsConfiguration;

public class ConfigurationDao extends PluginDao
{
    protected static final String RECORD_ID = "RECORD_ID";
    protected static final String KB_ACCOUNT_ID = "KB_ACCOUNT_ID";
    protected static final String EVENT_TYPE = "EVENT_TYPE";
    protected static final String CREATED_AT = "CREATED_AT";
    protected static final String KB_TENANT_ID = "KB_TENANT_ID";
    
    public ConfigurationDao(final DataSource dataSource) throws SQLException {
        super(dataSource);
    }

    public ConfigurationDao(final DataSource dataSource, final Properties properties) throws SQLException {
        super(dataSource);
        Enumeration<?> eventType = properties.propertyNames();
    }

    public List<EmailNotificationsConfiguration> getEventTypes(final List<UUID> kbAccountId, final UUID kbTenantId) throws SQLException {
        return execute(dataSource.getConnection(),
                       new WithConnectionCallback<List<EmailNotificationsConfiguration>>() {
                           @Override
                           public List<EmailNotificationsConfiguration> withConnection(final Connection conn) throws SQLException {
                               return DSL.using(conn, dialect, settings)
                                         .selectFrom(Tables.EMAIL_NOTIFICATIONS_CONFIGURATION)
                                         .where(DSL.field(KB_TENANT_ID).equal(kbTenantId.toString()))
                                         .and(DSL.field(KB_ACCOUNT_ID).in(kbAccountId))
                                         .orderBy(DSL.field(RECORD_ID).asc())
                                         .fetch().into(EmailNotificationsConfiguration.class);
                           }
                       });
    }

    public List<EmailNotificationsConfiguration> getEventTypesPerAccount(final UUID kbAccountId, final UUID kbTenantId) throws SQLException {
        return execute(dataSource.getConnection(),
                       new WithConnectionCallback<List<EmailNotificationsConfiguration>>() {
                           @Override
                           public List<EmailNotificationsConfiguration> withConnection(final Connection conn) throws SQLException {
                               return DSL.using(conn, dialect, settings)
                                         .selectFrom(Tables.EMAIL_NOTIFICATIONS_CONFIGURATION)
                                         .where(DSL.field(KB_TENANT_ID).equal(kbTenantId.toString()))
                                         .and(DSL.field(KB_ACCOUNT_ID).equal(kbAccountId.toString()))
                                         .orderBy(DSL.field(RECORD_ID).asc())
                                         .fetch().into(EmailNotificationsConfiguration.class);
                           }
                       });
    }

    public List<EmailNotificationsConfiguration> getEventTypesPerAccount(final UUID kbAccountId, final UUID kbTenantId, final ExtBusEventType eventType) throws SQLException {
        return execute(dataSource.getConnection(),
                       new WithConnectionCallback<List<EmailNotificationsConfiguration>>() {
                           @Override
                           public List<EmailNotificationsConfiguration> withConnection(final Connection conn) throws SQLException {
                               return DSL.using(conn, dialect, settings)
                                         .selectFrom(Tables.EMAIL_NOTIFICATIONS_CONFIGURATION)
                                         .where(DSL.field(KB_TENANT_ID).equal(kbTenantId.toString()))
                                         .and(DSL.field(KB_ACCOUNT_ID).equal(kbAccountId.toString()))
                                         .and(DSL.field(EVENT_TYPE).equal(eventType.toString()))
                                         .orderBy(DSL.field(RECORD_ID).asc())
                                         .fetch().into(EmailNotificationsConfiguration.class);
                           }
                       });
    }

    public EmailNotificationsConfiguration getEventTypePerAccount(final UUID kbAccountId, final UUID kbTenantId, final ExtBusEventType eventType) throws SQLException {
        final List<EmailNotificationsConfiguration> foundEventType = getEventTypesPerAccount(kbAccountId,kbTenantId,eventType);
        return foundEventType.size() == 0 ? null : foundEventType.get(0);
    }

    public void updateConfigurationPerAccount(final UUID kbAccountId,
                                          final UUID kbTenantId,
                                          final List<ExtBusEventType> eventTypes,
                                          final DateTime utcNow) throws SQLException {
        this.updateConfiguration(kbAccountId,kbTenantId,eventTypes,utcNow);
    }

    private void updateConfiguration(final UUID kbAccountId,
                                final UUID kbTenantId,
                                final List<ExtBusEventType> eventTypes,
                                final DateTime utcNow) throws SQLException {

        execute(dataSource.getConnection(),
            new WithConnectionCallback<Void>() {
                @Override
                public Void withConnection(final Connection conn) throws SQLException {
                    DSL.using(conn, dialect, settings).transaction(context -> {
                                                    DSL.using(context)
                                                       .delete(Tables.EMAIL_NOTIFICATIONS_CONFIGURATION)
                                                       .where(DSL.field(KB_ACCOUNT_ID).equal(kbAccountId.toString()))
                                                       .and(DSL.field(KB_TENANT_ID).equal(kbTenantId.toString()))
                                                       .execute();

                                                    for (ExtBusEventType eventType : eventTypes) {
                                                        DSL.using(context)
                                                           .insertInto(Tables.EMAIL_NOTIFICATIONS_CONFIGURATION,
                                                                       DSL.field(KB_ACCOUNT_ID),
                                                                       DSL.field(EVENT_TYPE),
                                                                       DSL.field(CREATED_AT),
                                                                       DSL.field(KB_TENANT_ID))
                                                           .values(kbAccountId.toString(),
                                                                   eventType.toString(),
                                                                   toTimestamp(utcNow),
                                                                   kbTenantId.toString())
                                                           .execute();
                                                        }
                                                    }
                    );
                    return null;
                }
            });
    }

    public void deleteConfiguration(final UUID kbAccountId, final UUID kbTenantId) throws SQLException {
        execute(dataSource.getConnection(),
                new WithConnectionCallback<Void>() {
                    @Override
                    public Void withConnection(final Connection conn) throws SQLException {

                        DSL.using(conn, dialect, settings)
                           .delete(Tables.EMAIL_NOTIFICATIONS_CONFIGURATION)
                           .where(DSL.field(KB_ACCOUNT_ID).equal(kbAccountId.toString()))
                           .and(DSL.field(KB_TENANT_ID).equal(kbTenantId.toString()))
                           .execute();

                        return null;
                    }
                });
    }

}
