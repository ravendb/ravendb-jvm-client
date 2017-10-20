package net.ravendb;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class RemoteTestBase {


    public void withServer(Consumer<String> consumer) {
        //TODO:

        consumer.accept("http://localhost:8080");
    }

    public void withDatabase(Consumer<String> action) {

        //TODO :
        action.accept("db1");
    }

}
