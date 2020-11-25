package net.devtech.grossfabrichacks.util;

@FunctionalInterface
public interface ThrowingIntConsumer {
    void accept(int i) throws Throwable;
}
