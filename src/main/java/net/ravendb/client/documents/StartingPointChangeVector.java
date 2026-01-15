package net.ravendb.client.documents;

public final class StartingPointChangeVector {

    private final String value;

    private StartingPointChangeVector(String startingPoint) {
        this.value = startingPoint;
    }

    public String getValue() {
        return value;
    }

    public static StartingPointChangeVector from(String changeVector) {
        return new StartingPointChangeVector(changeVector);
    }

    public static final StartingPointChangeVector DoNotChange =
            from("DoNotChange");

    public static final StartingPointChangeVector LastDocument =
            from("LastDocument");

    public static final StartingPointChangeVector BeginningOfTime =
            from("BeginningOfTime");
}
