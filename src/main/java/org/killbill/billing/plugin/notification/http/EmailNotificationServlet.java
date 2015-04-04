package org.killbill.billing.plugin.notification.http;

import org.osgi.service.log.LogService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class EmailNotificationServlet extends HttpServlet {

    private final LogService logService;

    public EmailNotificationServlet(final LogService logService) {
        this.logService = logService;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        // Find me on http://127.0.0.1:8080/plugins/killbill-email-notifications
        logService.log(LogService.LOG_INFO, "HI!");
    }
}
