# killbill-email-notifications-plugin

Release builds are available on [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.kill-bill.billing.plugin.java%22%20AND%20a%3A%22killbill-email-notifications-plugin%22) with coordinates `org.kill-bill.billing.plugin.java:killbill-email-notifications-plugin`.

Kill Bill compatibility
-----------------------

| Plugin version | Kill Bill version |
| -------------: | ----------------: |
| 0.1.y          | 0.14.z            |
| 0.2.y          | 0.16.z            |
| 0.3.y          | 0.18.z            |
| 0.4.y          | 0.20.z            |

## Overview

The plugin will listen to specific system bus events and notify customers through emails. The following events are currently processed and emails are sent to all the emails associated with the account:

* Upcoming invoices: the customer will receive an email about upcoming invoices (the time at which to send the email is configured through the Kill Bill system property `org.killbill.invoice.dryRunNotificationSchedule`)
* Successful Payment: the customer will receive an email after each successful payment
* Payment Failure: the customer will receive an email after each failed payment
* Payment Refund: the customer will receive an email after a payment refund was completed
* Subscription Cancellation: the customer will receive an email at the time a subscription was requested to be canceled
* Subscription Cancellation: the customer will receive an email at the effective date of the subscription cancellation

## Multi-tenancy Configuration

### Supported Keys And Resources

The plugin can be ran on a set of Kill Bill multi-tenant instances. The various templates and translation files can be uploaded on a per tenant basis using the following keys (for instance with a Locale `en_US`):

Note that the approcah taken here has been to create on template per locale and per type (as opposed to one template per type with an additional set of translation string bundles for each locale):

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

### CURL Examples

For instnace to upload a template for the next upcoming invoice and for a locale `en_US`:

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


