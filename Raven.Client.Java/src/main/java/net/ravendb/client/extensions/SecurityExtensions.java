package net.ravendb.client.extensions;

import net.ravendb.abstractions.basic.EventHandler;
import net.ravendb.abstractions.closure.Action0;
import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.connection.OperationCredentials;
import net.ravendb.abstractions.connection.WebRequestEventArgs;
import net.ravendb.abstractions.oauth.BasicAuthenticator;
import net.ravendb.abstractions.oauth.SecuredAuthenticator;
import net.ravendb.client.connection.implementation.HttpJsonRequestFactory;
import net.ravendb.client.delegates.HttpResponseWithMetaHandler;
import net.ravendb.client.document.Convention;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

public class SecurityExtensions {

    public static void initializeSecurity(Convention conventions, HttpJsonRequestFactory requestFactory, final String serverUrl) {
        if (conventions.getHandleUnauthorizedResponse() != null) {
            return ; // already setup by the user
        }

        final BasicAuthenticator basicAuthenticator = new BasicAuthenticator(requestFactory.getHttpClient(), requestFactory.isEnableBasicAuthenticationOverUnsecuredHttpEvenThoughPasswordsWouldBeSentOverTheWireInClearTextToBeStolenByHackers());
        final SecuredAuthenticator securedAuthenticator = new SecuredAuthenticator(requestFactory, true);

        requestFactory.setOnDispose(new Action0() {
            @Override
            public void apply() {
                securedAuthenticator.close();
            }
        });

        requestFactory.addConfigureRequestEventHandler(new EventHandler<WebRequestEventArgs>() {
            @Override
            public void handle(Object sender, WebRequestEventArgs event) {
                basicAuthenticator.configureRequest(sender, event);
            }
        });
        requestFactory.addConfigureRequestEventHandler(new EventHandler<WebRequestEventArgs>() {
            @Override
            public void handle(Object sender, WebRequestEventArgs event) {
                securedAuthenticator.configureRequest(sender, event);
            }
        });

        conventions.setHandleUnauthorizedResponse(new HttpResponseWithMetaHandler() {
            @SuppressWarnings({"null", "synthetic-access"})
            @Override
            public Action1<HttpRequest> handle(HttpResponse response, OperationCredentials credentials) {
                Header oauthSourceHeader = response.getFirstHeader("OAuth-Source");
                String oauthSource = null;
                if (oauthSourceHeader != null) {
                    oauthSource = oauthSourceHeader.getValue();
                }
                if (StringUtils.isNotEmpty(oauthSource) && !oauthSource.toLowerCase().endsWith("/OAuth/API-Key".toLowerCase())) {
                    return basicAuthenticator.doOAuthRequest(oauthSource, credentials.getApiKey());
                }
                if (credentials.getApiKey() == null) {
                    //AssertUnauthorizedCredentialSupportWindowsAuth(response);
                    return null;
                }
                if (StringUtils.isEmpty(oauthSource)) {
                    oauthSource = serverUrl + "/OAuth/API-Key";
                }
                return securedAuthenticator.doOAuthRequest(oauthSource, credentials.getApiKey());
            }
        });
    }
}
