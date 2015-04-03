killbill-email-notifications-plugin
==================================

The plugin will listen to specific system bus events and notify customers through emails:

* Upcoming invoices : The customer will receive an email about upcoming invoices (the time at which to send the email is configured through the Kill Bill system property `org.killbill.invoice.dryRunNotificationSchedule`
* Successful Payment : The customer will receive an email after each successful payment
* Payment Failure : The customer will receive an email after each failed payment
* Payment Refund : The customer will receive an email after a payment refund was completed
* Subscription Cancellation : The customer will receive an email at the time a subscription was requested to be canceled
* Subscription Cancellation : The customer will receive an email at the effective date of the subscription cancellation



