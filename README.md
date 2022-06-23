# SMPP client

[![Build](https://github.com/telenordigital/sms-smpp/actions/workflows/build.yml/badge.svg)](https://github.com/telenordigital/sms-smpp/actions/workflows/build.yml)

A smpp client library implementing SMPP specification from https://smpp.org.

Uses Netty I/0 asynchronous framework. Requires Java 16.

If you have any questions please raise create a GitHub issue or send an e-mail to this
[group](mailto:sms-smpp-library@telenordigital.com)

## Features

- Supports SMS MT, delivery receipts and SMS MO
- Modern and simple implementation with limited third-party dependencies
- Performant and reliable, used by Telenor Digital in production services since 2021

## Usage

At first, you need to create an instance
of [SmppConnectionConfig](src/main/java/com/telenordigital/sms/smpp/config/SmppConnectionConfig.java)
type with necessary configuration parameters. Afterwards, you create an instance
of [SmppConnectionGroup](src/main/java/com/telenordigital/sms/smpp/SmppConnectionGroup.java)
using the config instance and handler functions for delivery receipts and SMS MO messages as
constructor parameters

See the [complete example](src/test/java/com/telenordigital/sms/smpp/SmppConnectionTester.java) for
more details.

## Release

Run Maven release plugin and accept suggested version updates.

    > mvn release:prepare

Once it is done, go to [Releases](https://github.com/telenordigital/sms-smpp/releases) and create a
new release. This [action](.github/workflows/publish.yaml) will automatically build and upload the
artifacts to the Maven central. Note: it can take up to an hour for the new release to become
visible

## License

Code licensed under the Apache License 2.0.
See [LICENSE file](https://github.com/telenordigital/sms-smpp/blob/master/LICENSE) for terms.
