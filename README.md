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
| 0.6.y          | 0.22.z            |

Requirements
------------

The plugin needs a database. The latest version of the schema can be found [here](src/main/resources/ddl.sql).

## Overview

The plugin will listen to specific system bus events and notify customers through emails. The following events are currently processed and emails are sent to all the email addresses associated with the account:

* `INVOICE_CREATION` : the customer will receive an email informing that a new invoice is available.
* `UPCOMING_INVOICE` : the customer will receive an email about upcoming invoices (the time at which to send the email is configured through the Kill Bill system property `org.killbill.invoice.dryRunNotificationSchedule`)
* `SUCCESSFUL_PAYMENT `: the customer will receive an email after each successful payment
* `FAILED_PAYMENT` : the customer will receive an email after each failed payment
* `PAYMENT_REFUND` : the customer will receive an email after a payment refund was completed
* `SUBSCRIPTION_CANCELLATION_REQUESTED` : the customer will receive an email at the time a subscription was requested to be canceled
* `SUBSCRIPTION_CANCELLATION_EFFECTIVE` : the customer will receive an email at the effective date of the subscription cancellation

Note that in order to send an email, the account must be configured to permit such event(s).

The plugin will typically extract some per account information:
* The `locale` is used to determine which translation to use
* The account `email` address is obviously requested to be able to send the email
* In addition to this, and dependeing on which information the templates require, some other fiedls may be needed (e.g `address1`, `city`,..). Those are obviously not mandatory unless requested from the template.


## SMTP & Email Type Configuration

### Tenant Configuration

Each tenant that requires the use of the plugin must be configured with the SMTP properties, and it can also specify the default set of emails that should be set. As indicated above, the plugin allows to be used to react to the following events: 
`INVOICE_CREATION`, `UPCOMING_INVOICE`, `SUCCESSFUL_PAYMENT`, `FAILED_PAYMENT`, `PAYMENT_REFUND`, `SUBSCRIPTION_CANCELLATION_REQUESTED`, `SUBSCRIPTION_CANCELLATION_EFFECTIVE`:


The following curl command can be used to configure a particular tenant:

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

#### Account Configuartion

In addition to the per-tenant configuration, we also allow a more granular configuration for the set of emails at the account level:

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

## Templates & Resources Configuration

The plugin comes with a set of [default templates](src/main/resources/org/killbill/billing/plugin/notification/templates) but one will typically want to upload his own templates. We are relying on the [mustache engine](https://mustache.github.io/) for the templating mechanism.

In addition to the templates, we all allow to upload some resources files to allow for string translations in different languages, e.g to have different trnslation for the catalog prodcut names, ...

### Supported Keys And Resources

The various templates and translation files can be uploaded on a per tenant basis using the following keys (for instance with a Locale `en_US`):

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

### Email Template Example

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

# Testing

## SMTP Server

In order to test the plugin, the easiest route is to start a local SMTP server. We are typically relying on the `namshi/smtp` docker image:

```
# Start the SMTP server on port 25
docker run -tid --name smtp_server -p 25:25  -e DISABLE_IPV6=true namshi/smtp
```

## Scenario

1. [Create a tenant](https://killbill.github.io/slate/#tenant-create-a-tenant)
2. Configure the tenant as specififed above
3. [Create an account](https://killbill.github.io/slate/#account-create-an-account) and specifify at least the `locale` and `email`. Our default template will also require the following fields to be set on the `Account`:  `company_name`, `address1`, `city`, `state_or_province`, `postal_code`, `country`.
4. [Add a default payment method](https://killbill.github.io/slate/#account-add-a-payment-method) and set it as default.
5. [Create a external charge](https://killbill.github.io/slate/#invoice-create-external-charge-s) to trigger an invoice

=> You should see an email sent for the invoice and succesuful payment
