package org.killbill.billing.plugin.notification.email;

public class EmailContent {

    private String subject;
    private String body;

    public EmailContent(String subject, String body) {
        this.subject = subject;
        this.body = body;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }
}
