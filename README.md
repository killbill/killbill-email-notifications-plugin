# killbill-email-notifications-plugin

Release builds are available on [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.kill-bill.billing.plugin.java%22%20AND%20a%3A%22killbill-email-notifications-plugin%22) with coordinates `org.kill-bill.billing.plugin.java:killbill-email-notifications-plugin`.

Kill Bill compatibility
-----------------------

| Plugin version | Kill Bill version |
| -------------: | ----------------: |
| 0.1.y          | 0.14.z            |
| 0.2.y          | 0.16.z            |
| 0.3.y          | 0.18.z            |
| 0.4.y          | 0.19.z            |
| 0.5.y          | 0.20.z            |

Requirements
------------

The plugin needs a database. The latest version of the schema can be found [here](https://github.com/killbill/killbill-email-notifications-plugin/tree/master/src/main/resources/org/killbill/billing/plugin/notification/ddl.sql).

## Overview

The plugin will listen to specific system bus events and notify customers through emails. The following events are currently processed and emails are sent to all the email addresses associated with the account:

* Invoice Creation: the customer will receive an email informing that a new invoice is available.
* Upcoming invoices: the customer will receive an email about upcoming invoices (the time at which to send the email is configured through the Kill Bill system property `org.killbill.invoice.dryRunNotificationSchedule`)
* Successful Payment: the customer will receive an email after each successful payment
* Payment Failure: the customer will receive an email after each failed payment
* Payment Refund: the customer will receive an email after a payment refund was completed
* Subscription Cancellation: the customer will receive an email at the time a subscription was requested to be canceled
* Subscription Cancellation: the customer will receive an email at the effective date of the subscription cancellation

Note that in order to be able to be notified via email the account must be configured to permit such event(s). 

### Configuring SMTP properties
SMTP properties for the email notification plugin should be configured using the following call:

```
curl -v \
-X POST \
-u admin:password \
-H 'X-Killbill-ApiKey: bob' \
-H 'X-Killbill-ApiSecret: lazar' \
-H 'X-Killbill-CreatedBy: admin' \
-H 'Content-Type: text/plain' \
-d 'org.killbill.billing.plugin.email-notifications.defaultEvents=INVOICE_PAYMENT_SUCCESS,SUBSCRIPTION_CANCEL
org.killbill.billing.plugin.email-notifications.smtp.host=127.0.0.1
org.killbill.billing.plugin.email-notifications.smtp.port=25
org.killbill.billing.plugin.email-notifications.smtp.useAuthentication=true
org.killbill.billing.plugin.email-notifications.smtp.userName=uuuuuu
org.killbill.billing.plugin.email-notifications.smtp.password=zzzzzz
org.killbill.billing.plugin.email-notifications.smtp.useSSL=false
org.killbill.billing.plugin.email-notifications.smtp.defaultSender=xxx@yyy.com' \
http://127.0.0.1:8080/1.0/kb/tenants/uploadPluginConfig/killbill-email-notifications
```

### Configuring permitted events

There are two ways to configure the permitted event(s):

* Can be specified on a per tenant basis
* Can be specified per account.

#### Per tenant

```
curl -v \
-X POST \
-u admin:password \
-H 'X-Killbill-ApiKey: bob' \
-H 'X-Killbill-ApiSecret: lazar' \
-H 'X-Killbill-CreatedBy: admin' \
-H 'Content-Type: text/plain' \
-d 'org.killbill.billing.plugin.email-notifications.defaultEvents=INVOICE_PAYMENT_SUCCESS,SUBSCRIPTION_CANCEL
org.killbill.billing.plugin.email-notifications.smtp.host=127.0.0.1
org.killbill.billing.plugin.email-notifications.smtp.port=25
org.killbill.billing.plugin.email-notifications.smtp.useAuthentication=true
org.killbill.billing.plugin.email-notifications.smtp.userName=uuuuuu
org.killbill.billing.plugin.email-notifications.smtp.password=zzzzzz
org.killbill.billing.plugin.email-notifications.smtp.useSSL=false
org.killbill.billing.plugin.email-notifications.smtp.defaultSender=xxx@yyy.com' \
http://127.0.0.1:8080/1.0/kb/tenants/uploadPluginConfig/killbill-email-notifications
```

#### Per account

```
curl -v \
     -X POST \
     -u admin:password \
     -H 'X-Killbill-ApiKey: bob' \
     -H 'X-Killbill-ApiSecret: lazar' \
     -H 'X-Killbill-CreatedBy: admin' \
     -H 'Content-Type: application/json' \
     -d '["INVOICE_NOTIFICATION","INVOICE_CREATION","INVOICE_PAYMENT_SUCCESS","INVOICE_PAYMENT_FAILED","SUBSCRIPTION_CANCEL"]' \
     http://127.0.0.1:8080/plugins/killbill-email-notifications/v1/accounts/{accountId}
```

## Multi-tenancy Configuration

### Supported Keys And Resources

The plugin can be ran on a set of Kill Bill multi-tenant instances. The various templates and translation files can be uploaded on a per tenant basis using the following keys (for instance with a Locale `en_US`):

Note that the approach taken here has been to create one template per locale and per type (as opposed to one template per type with an additional set of translation string bundles for each locale):

* Template for invoice creation: `killbill-email-notifications:INVOICE_CREATION_en_US` 
* Template for upcoming invoices: `killbill-email-notifications:UPCOMING_INVOICE_en_US` 
* Template for failed payments: `killbill-email-notifications:FAILED_PAYMENT_en_US`
* Template for subscription cancellation (requested date): `killbill-email-notifications:SUBSCRIPTION_CANCELLATION_REQUESTED_en_US`
* Template for subscription cancellation (effective date): `killbill-email-notifications:SUBSCRIPTION_CANCELLATION_EFFECTIVE_en_US`
* Template for payment refunds: `killbill-email-notifications:PAYMENT_REFUND_en_US`
* Template for translation strings: `killbill-email-notifications:TEMPLATE_TRANSLATION_en_US`

The following Kill Bill endpoints can be used to upload the templates:

* Upload a new per-tenant template for a specific locale: `POST /1.0/kb/tenants/userKeyValue/<KEY_NAME>`
* Retrieve a per-tenant template for a specific locale: `GET /1.0/kb/tenants/userKeyValue/<KEY_NAME>`
* Delete a per-tenant template for a specific locale: `DELETE /1.0/kb/tenants/userKeyValue/<KEY_NAME>`

Currently, there is no caching for these templates within Kill Bill, but the plugin *could* cache those (but then the multi-node scenario requires some great care with respect to cache invalidation).

### Email Templates

One can upload per-tenant email templates for various events using KB apis. At runtime the plugin will look at the configured templates and based on the `locale` associated with a given account, decide which one to take; the administrator should upload one template per event and type of `locale` supported. If a given `Account` does not have a `locale` specified, this will fail with a exception `Translation for locale XXX isn't found`.


Let's look at an example to upload a templare for the next upcoming invoice and for a locale `en_US`:

1. Create the template `/tmp/UpcomingInvoice.mustache`:

  ```
*** You Have a New Invoice ***

You have a new invoice from {{text.merchantName}}, due on {{invoice.targetDate}}.

{{#invoice.invoiceItems}}
{{startDate}} {{planName}} : {{invoice.formattedAmount}}
{{/invoice.invoiceItems}}

{{text.invoiceAmountTotal}}: {{invoice.formattedBalance}}

{{text.billedTo}}:
{{account.companyName}}
{{account.name}}
{{account.address1}}
{{account.city}}, {{account.stateOrProvince}} {{account.postalCode}}
{{account.country}}

If you have any questions about your account, please reply to this email or contact {{text.merchantName}} Support at: {{text.merchantContactPhone}}
  ```

2. Upload the template for your tenant 

  ```
curl -v \
-u admin:password \
-H "X-Killbill-ApiKey: bob" \
-H "X-Killbill-ApiSecret: lazar" \
-H 'X-Killbill-CreatedBy: admin' \
-H "Content-Type: text/plain" \
-X POST \
--data-binary @/tmp/UpcomingInvoice.mustache \
http://127.0.0.1:8080/1.0/kb/tenants/userKeyValue/killbill-email-notifications:UPCOMING_INVOICE_en_US
  ```


