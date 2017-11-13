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

package org.killbill.billing.plugin.notification.setup;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailNotificationConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationConfiguration.class);

    private final Set<String> eventTypes;

    public EmailNotificationConfiguration(){
        eventTypes = new HashSet<String>();
    }

    public EmailNotificationConfiguration(final Properties properties)
    {
        String defaultEvents = properties.getProperty(EmailNotificationActivator.PROPERTY_PREFIX + "defaultEvents");

        eventTypes = new HashSet<String>();
        if (defaultEvents != null && !defaultEvents.isEmpty())
        {
            for( String eventType : defaultEvents.split(","))
            {
                eventTypes.add(eventType);
            }
        }
    }

    public final Set<String> getEventTypes() {
        return eventTypes;
    }

}
