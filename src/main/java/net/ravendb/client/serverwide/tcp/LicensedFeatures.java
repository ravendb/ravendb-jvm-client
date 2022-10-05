package net.ravendb.client.serverwide.tcp;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;

public class LicensedFeatures {
    private boolean dataCompression;

    public boolean isDataCompression() {
        return dataCompression;
    }

    public void setDataCompression(boolean dataCompression) {
        this.dataCompression = dataCompression;
    }

    public void toJson(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        generator.writeBooleanField("DataCompression", dataCompression);
        generator.writeEndObject();
    }
}
