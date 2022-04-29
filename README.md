# SMPP client

A smpp client library implementing SMPP specification from https://smpp.org.

Uses Netty I/0 asynchronous framework. Requires Java 16.

## Features

- Supports SMS MT, delivery receipts and SMS MO
- Modern and simple implementation with limited third-party dependencies
- Performant and reliable, used by Telenor Digital in production services since 2021

## Usage

At first, you need to create an instance
of [SmppConnectionConfig](src/main/java/com/telenordigital/sms/smpp/config/SmppConnectionConfig.java)
type with necessary configuration parameters. Afterwards, you create an instance
of [SmppConnectionConfig](src/main/java/com/telenordigital/sms/smpp/SmppConnectionGroup.java). To
providing it with a config instance and the handlers for delivery receipts and SMS MO messages.

See the [complete example](src/test/java/com/telenordigital/sms/smpp/SmppConnectionTester.java) for
more details.
