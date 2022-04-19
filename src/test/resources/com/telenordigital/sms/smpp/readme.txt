Test certificates were generated like this:

openssl req -x509 -sha256 -nodes -days 3650 -newkey rsa:2048 -keyout cloudhopper-key.pem -out cloudhopper-cert.pem
openssl pkcs12 -export -in cloudhopper-cert.pem -inkey cloudhopper-key.pem -name cloudhopper > cloudhopper.p12
keytool -importkeystore -srckeystore cloudhopper.p12 -destkeystore cloudhopper.jks -srcstoretype pkcs12 -alias cloudhopper -srcstorepass '' -deststorepass 'changeme'
