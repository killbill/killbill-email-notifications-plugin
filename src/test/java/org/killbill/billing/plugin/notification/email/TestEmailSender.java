package org.killbill.billing.plugin.notification.email;

import com.google.common.collect.ImmutableList;
import org.apache.commons.mail.EmailException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

public class TestEmailSender {

    private final Logger log = LoggerFactory.getLogger(TestEmailSender.class);

    private static final String TEST_SMTP_SERVER_NAME = "smt.perver";
    private static final int TEST_SMPT_SERVER_PORT = 25;
    private static final String TEST_SMTP_USER = "foo";
    private static final String TEST_SMTP_PWD = "bar";
    private static final String TEST_SMTP_FROM = "caramel@mou.com";

    @BeforeClass(groups = "fast")
    public void setup() {

    }

    @Test(enabled=false)
    public void foo() throws IOException, EmailException {
        LogService logService = new LogService() {
            @Override
            public void log(int i, String s) {
                log.info(s);
            }
            @Override
            public void log(int i, String s, Throwable throwable) {
                log.info(s, throwable);
            }
            @Override
            public void log(ServiceReference serviceReference, int i, String s) {
            }
            @Override
            public void log(ServiceReference serviceReference, int i, String s, Throwable throwable) {
            }
        };

        EmailSender sender = new EmailSender(TEST_SMTP_SERVER_NAME, TEST_SMPT_SERVER_PORT, TEST_SMTP_USER, TEST_SMTP_PWD, TEST_SMTP_FROM, true, false, logService);
        final String to = "<something_that_works>@gmail.com";
        sender.sendPlainTextEmail(ImmutableList.of(to), null, "coucou", "body");
    }
}
