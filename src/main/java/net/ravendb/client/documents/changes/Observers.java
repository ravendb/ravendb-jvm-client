package net.ravendb.client.documents.changes;

import java.util.function.Consumer;

public class Observers {
    public static <T> IObserver<T> create(Consumer<T> action) {
        return new ActionBasedObserver<>(action);
    }

    public static class ActionBasedObserver<T> implements IObserver<T> {
        private Consumer<T> action;

        public ActionBasedObserver(Consumer<T> action) {
            super();
            this.action = action;
        }
        @Override
        public void onNext(T value) {
            action.accept(value);
        }

        @Override
        public void onError(Exception error) {
            //empty
        }

        @Override
        public void onCompleted() {
            //empty
        }

        @Override
        public int compareTo(Object o) {
            return this == o ? 0 : -1;
        }
    }
}
