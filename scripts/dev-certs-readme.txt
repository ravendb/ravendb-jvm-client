Please run gen-test-certs.sh to generate fake https certificates.

In Java Test export environment variables:
RAVENDB_JAVA_TEST_HTTPS_SERVER_URL -> https://a.javatest11.development.run:8085

RAVENDB_JAVA_TEST_SERVER_PATH -> ${RAVEN_ROOT_DIR}\Server\Raven.Server.exe

RAVENDB_JAVA_TEST_CERTIFICATE_PATH -> scripts/certs/server.pfx

RAVENDB_JAVA_TEST_CA_PATH -> scripts/certs/ca.crt


Install CA certificate to system store.