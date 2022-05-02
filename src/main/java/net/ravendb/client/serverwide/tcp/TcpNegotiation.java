package net.ravendb.client.serverwide.tcp;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.primitives.Reference;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TcpNegotiation {

    private static final Log logger = LogFactory.getLog(TcpNegotiation.class);

    public final static int OUT_OF_RANGE_STATUS = -1;
    public final static int DROP_STATUS = -2;

    public static TcpConnectionHeaderMessage.SupportedFeatures negotiateProtocolVersion(Socket socket, TcpNegotiateParameters parameters) throws IOException {
        if (logger.isInfoEnabled()) {
            logger.info("Start negotiation for " + parameters.getOperation() + " operation with " + ObjectUtils.firstNonNull(parameters.getDestinationNodeTag(), parameters.getDestinationUrl()));
        }

        Reference<Integer> currentRef = new Reference<>(parameters.getVersion());
        while (true) {
            sendTcpVersionInfo(socket.getOutputStream(), parameters, currentRef.value);
            Integer version = parameters.getReadResponseAndGetVersionCallback().apply(parameters.getDestinationUrl(), socket);
            if (logger.isInfoEnabled()) {
                logger.info("Read response from " + ObjectUtils.firstNonNull(parameters.getSourceNodeTag(), parameters.getDestinationUrl()) + " for " + parameters.getOperation() + ", received version is '" + version + "'");
            }

            if (version.equals(currentRef.value)) {
                break;
            }

            //In this case we usually throw internally but for completeness we better handle it
            if (version == DROP_STATUS) {
                return TcpConnectionHeaderMessage.getSupportedFeaturesFor(TcpConnectionHeaderMessage.OperationTypes.DROP, TcpConnectionHeaderMessage.DROP_BASE_LINE);
            }

            TcpConnectionHeaderMessage.SupportedStatus status = TcpConnectionHeaderMessage.operationVersionSupported(parameters.getOperation(), version, currentRef);
            if (status == TcpConnectionHeaderMessage.SupportedStatus.OUT_OF_RANGE) {
                sendTcpVersionInfo(socket.getOutputStream(), parameters, OUT_OF_RANGE_STATUS);
                throw new IllegalArgumentException("The " + parameters.getOperation() + " version " + parameters.getVersion() + " is out of range, out lowest version is " + currentRef.value);
            }

            if (logger.isInfoEnabled()) {
                logger.info("The version " +  version + " is " + status + ", will try to agree on '"
                        + currentRef.value + "' for " + parameters.getOperation() + " with "
                        + ObjectUtils.firstNonNull(parameters.getDestinationNodeTag(), parameters.getDestinationUrl()));
            }
        }

        if (logger.isInfoEnabled()) {
            logger.info(ObjectUtils.firstNonNull(parameters.getDestinationNodeTag(), parameters.getDestinationUrl()) + " agreed on version " + currentRef.value + " for " + parameters.getOperation());
        }

        return TcpConnectionHeaderMessage.getSupportedFeaturesFor(parameters.getOperation(), currentRef.value);
    }

    private static void sendTcpVersionInfo(OutputStream stream, TcpNegotiateParameters parameters, int currentVersion) throws IOException {
        if (logger.isInfoEnabled()) {
            logger.info("Send negotiation for " + parameters.getOperation() + " in version " + currentVersion);
        }

        try (JsonGenerator generator =  JsonExtensions.getDefaultMapper().getFactory().createGenerator(new OutputStreamWriter(stream, StandardCharsets.UTF_8))) {
            generator.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);

            generator.writeStartObject();
            generator.writeStringField("DatabaseName", parameters.getDatabase());
            generator.writeObjectField("Operation", parameters.getOperation());
            generator.writeStringField("SourceNodeTag", parameters.getSourceNodeTag());
            generator.writeNumberField("OperationVersion", currentVersion);
            generator.writeFieldName("AuthorizeInfo");
            if (parameters.getAuthorizeInfo() != null) {
                parameters.getAuthorizeInfo().toJson(generator);
            } else {
                generator.writeNull();
            }
            generator.writeEndObject();
        }
        stream.flush();
    }

}
