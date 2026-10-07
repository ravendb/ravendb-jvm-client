package net.ravendb.client.serverwide.operations.certificates;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum CertificateUsage {
    RAVEN_SERVER,
    RAVEN_SERVER_FOR_COMMUNICATION,
    CLIENT,
    SSO_SERVER,
    SSO_CLIENT,
    WELL_KNOWN_ISSUER
}
