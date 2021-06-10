
# killbill-email-notifications-plugin

The Kill Bill email notification plugin is a plugin that can be used to send emails when certain events occur. The easiest way to get started with the email notification plugin is to take a look at our [Email Notification Plugin Document](https://docs.killbill.io/latest/email-notification-plugin.html) which provides detailed instructions for installing, configuring and using the plugin.

## Requirements

- The plugin requires Kill Bill and Kaui installed. The [Getting Started](https://docs.killbill.io/latest/getting_started.htm) document explains how to set this up. 

- In addition, the plugin also needs a database. The latest version of the schema can be found [here](https://github.com/killbill/killbill-email-notifications-plugin/blob/master/src/main/resources/ddl.sql).

## Versions

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




## Build

The email notification plugin can be built by running the following Maven command:

```
mvn clean install -DskipTests=true
```

## Installation

The email notification plugin can be installed by running the following [kpm](https://github.com/killbill/killbill-cloud/blob/master/kpm) command:

```
kpm install_java_plugin 'email-notifications' --from-source-file=<jar_file_path> --destination=<path_to_install_plugin>

```

Replace `<jar_file_path>` with the path of the email notification plugin jar file and `<path_to_install_plugin>` with the path where you want to install the plugin. This path should match the path specified by the `org.killbill.osgi.bundle.install.dir` property in the Kill Bill configuration file

