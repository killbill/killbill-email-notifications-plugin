/*
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

package org.killbill.billing.plugin.notification;

import java.util.UUID;

import javax.sql.DataSource;

import org.killbill.billing.account.api.Account;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillClock;
import org.killbill.billing.plugin.TestUtils;
import org.killbill.billing.plugin.notification.dao.ConfigurationDao;
import org.killbill.billing.util.api.CustomFieldUserApi;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.clock.ClockMock;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

public class TestBase {

    public static final Currency DEFAULT_CURRENCY = Currency.USD;
    public static final String DEFAULT_COUNTRY = "US";

    protected ConfigurationDao dao;
    protected OSGIKillbillClock osgiClock;
    protected ClockMock clock;
    protected CallContext context;
    protected Account account;
    protected OSGIKillbillAPI killbillApi;
    protected CustomFieldUserApi customFieldUserApi;
    protected OSGIConfigPropertiesService configPropertiesService;

    @BeforeMethod(groups = {"fast", "slow"})
    public void setUp() throws Exception {
        clock = new ClockMock();

        osgiClock = Mockito.mock(OSGIKillbillClock.class);
        Mockito.when(osgiClock.getClock()).thenReturn(clock);

        context = Mockito.mock(CallContext.class);
        Mockito.when(context.getTenantId()).thenReturn(UUID.randomUUID());

        account = TestUtils.buildAccount(DEFAULT_CURRENCY, DEFAULT_COUNTRY);
        killbillApi = TestUtils.buildOSGIKillbillAPI(account);
        customFieldUserApi = Mockito.mock(CustomFieldUserApi.class);
        Mockito.when(killbillApi.getCustomFieldUserApi()).thenReturn(customFieldUserApi);

        configPropertiesService = new OSGIConfigPropertiesService(Mockito.mock(BundleContext.class));
    }

    @BeforeMethod(groups = "slow")
    public void setUpDB() throws Exception {
        EmbeddedDbHelper.instance().resetDB();

        dao = EmbeddedDbHelper.instance().getConfigurationDao();
    }

    @BeforeSuite(groups = "slow")
    public void setUpBeforeSuite() throws Exception {
        EmbeddedDbHelper.instance().startDb();
    }

    @AfterSuite(groups = "slow")
    public void tearDownAfterSuite() throws Exception {
        EmbeddedDbHelper.instance().stopDB();
    }
}
