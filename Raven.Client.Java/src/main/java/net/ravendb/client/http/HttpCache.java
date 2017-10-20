package net.ravendb.client.http;

import net.ravendb.client.primitives.CleanCloseable;

import java.io.InputStream;

//TODO:
public class HttpCache implements CleanCloseable {

    @Override
    public void close() {

    }

    public void set(String url, String changeVector, InputStream result) {
        //TODO:
    }
}
