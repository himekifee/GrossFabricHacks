package net.devtech.grossfabrichacks.util;

public class TestInfo {
    public final String format;
    public final int iterations;
    public final ThrowingIntConsumer test;

    public TestInfo(int iterations, ThrowingIntConsumer test) {
        this.format = null;
        this.iterations = iterations;
        this.test = test;
    }

    public TestInfo(String format, int iterations, ThrowingIntConsumer test) {
        this.format = format;
        this.iterations = iterations;
        this.test = test;
    }
}
