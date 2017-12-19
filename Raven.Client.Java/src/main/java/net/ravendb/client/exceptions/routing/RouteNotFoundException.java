package net.ravendb.client.exceptions.routing;

import net.ravendb.client.exceptions.RavenException;

public class RouteNotFoundException extends RavenException {
    public RouteNotFoundException() {
    }

    public RouteNotFoundException(String message) {
        super(message);
    }

    public RouteNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
