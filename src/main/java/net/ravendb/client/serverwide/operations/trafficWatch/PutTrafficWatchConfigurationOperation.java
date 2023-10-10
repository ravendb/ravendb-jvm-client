package net.ravendb.client.serverwide.operations.trafficWatch;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.changes.TrafficWatchChangeType;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.operations.IVoidServerOperation;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.util.List;

public class PutTrafficWatchConfigurationOperation implements IVoidServerOperation {

    private final Parameters _parameters;

    public PutTrafficWatchConfigurationOperation(Parameters parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }
        this._parameters = parameters;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new SetTrafficWatchConfigurationCommand(_parameters);
    }

    private static class SetTrafficWatchConfigurationCommand extends VoidRavenCommand {
        private final Parameters _parameters;

        public SetTrafficWatchConfigurationCommand(Parameters parameters) {
            if (parameters == null) {
                throw new IllegalArgumentException("Parameters cannot be null");
            }
            this._parameters = parameters;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/admin/traffic-watch/configuration";

            HttpPost request = new HttpPost();
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.getCodec().writeValue(generator, _parameters);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON));

            return request;
        }
    }

    public static class Parameters {
        private TrafficWatchMode trafficWatchMode;
        private List<String> databases;

        private List<Integer> statusCodes;
        private long minimumResponseSizeInBytes;
        private long minimumRequestSizeInBytes;
        private long minimumDurationInMs;
        private List<String> httpMethods;
        private List<TrafficWatchChangeType> changeTypes;
        private List<String> certificateThumbprints;

        /**
         * @return Traffic Watch logging mode.
         */
        public TrafficWatchMode getTrafficWatchMode() {
            return trafficWatchMode;
        }

        /**
         * @param trafficWatchMode Traffic Watch logging mode.
         */
        public void setTrafficWatchMode(TrafficWatchMode trafficWatchMode) {
            this.trafficWatchMode = trafficWatchMode;
        }

        /**
         * @return Database names by which the Traffic Watch logging entities will be filtered.
         */
        public List<String> getDatabases() {
            return databases;
        }

        /**
         * @param databases Database names by which the Traffic Watch logging entities will be filtered.
         */
        public void setDatabases(List<String> databases) {
            this.databases = databases;
        }

        /**
         * @return Response status codes by which the Traffic Watch logging entities will be filtered.
         */
        public List<Integer> getStatusCodes() {
            return statusCodes;
        }

        /**
         * @param statusCodes Response status codes by which the Traffic Watch logging entities will be filtered.
         */
        public void setStatusCodes(List<Integer> statusCodes) {
            this.statusCodes = statusCodes;
        }

        /**
         * @return Minimum response size by which the Traffic Watch logging entities will be filtered.
         */
        public long getMinimumResponseSizeInBytes() {
            return minimumResponseSizeInBytes;
        }

        /**
         * @param minimumResponseSizeInBytes Minimum response size by which the Traffic Watch logging entities will be filtered.
         */
        public void setMinimumResponseSizeInBytes(long minimumResponseSizeInBytes) {
            this.minimumResponseSizeInBytes = minimumResponseSizeInBytes;
        }

        /**
         * @return Minimum request size by which the Traffic Watch logging entities will be filtered.
         */
        public long getMinimumRequestSizeInBytes() {
            return minimumRequestSizeInBytes;
        }

        /**
         * @param minimumRequestSizeInBytes Minimum request size by which the Traffic Watch logging entities will be filtered.
         */
        public void setMinimumRequestSizeInBytes(long minimumRequestSizeInBytes) {
            this.minimumRequestSizeInBytes = minimumRequestSizeInBytes;
        }

        /**
         * @return Minimum duration by which the Traffic Watch logging entities will be filtered.
         */
        public long getMinimumDurationInMs() {
            return minimumDurationInMs;
        }

        /**
         * @param minimumDurationInMs Minimum duration by which the Traffic Watch logging entities will be filtered.
         */
        public void setMinimumDurationInMs(long minimumDurationInMs) {
            this.minimumDurationInMs = minimumDurationInMs;
        }

        /**
         * @return Request HTTP methods by which the Traffic Watch logging entities will be filtered.
         */
        public List<String> getHttpMethods() {
            return httpMethods;
        }

        /**
         * @param httpMethods Request HTTP methods by which the Traffic Watch logging entities will be filtered.
         */
        public void setHttpMethods(List<String> httpMethods) {
            this.httpMethods = httpMethods;
        }

        /**
         * @return Traffic Watch change types by which the Traffic Watch logging entities will be filtered.
         */
        public List<TrafficWatchChangeType> getChangeTypes() {
            return changeTypes;
        }

        /**
         * @param changeTypes Traffic Watch change types by which the Traffic Watch logging entities will be filtered.
         */
        public void setChangeTypes(List<TrafficWatchChangeType> changeTypes) {
            this.changeTypes = changeTypes;
        }

        /**
         * @return Traffic Watch certificate thumbprints by which the Traffic Watch logging entities will be filtered.
         */
        public List<String> getCertificateThumbprints() {
            return certificateThumbprints;
        }

        /**
         * @param certificateThumbprints Traffic Watch certificate thumbprints by which the Traffic Watch logging entities will be filtered.
         */
        public void setCertificateThumbprints(List<String> certificateThumbprints) {
            this.certificateThumbprints = certificateThumbprints;
        }
    }
}
