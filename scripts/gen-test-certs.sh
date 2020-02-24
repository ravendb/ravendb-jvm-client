#!/bin/bash
mkdir certs
openssl genrsa -out certs/ca.key 2048
openssl req -new -x509 -key certs/ca.key -out certs/ca.crt -subj "/C=US/ST=Arizona/L=Nevada/O=RavenDB Test CA/OU=RavenDB test CA/CN=a.javatest11.development.run/emailAddress=support@ravendb.net"
openssl genrsa -out certs/localhost.key 2048
openssl req -new  -key certs/localhost.key -out certs/localhost.csr -subj "/C=US/ST=Arizona/L=Nevada/O=RavenDB Test/OU=RavenDB test/CN=a.javatest11.development.run/emailAddress=support@ravendb.net"
openssl x509 -req -extensions ext -extfile test.conf -in certs/localhost.csr -CA certs/ca.crt -CAkey certs/ca.key -CAcreateserial -out certs/localhost.crt
cat certs/localhost.key certs/localhost.crt > certs/jvm.pem
openssl pkcs12 -passout pass: -export -out certs/server.pfx -inkey certs/localhost.key -in certs/localhost.crt