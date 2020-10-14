package net.devtech.grossfabrichacks.instrumentation;

import java.lang.instrument.Instrumentation;

public class InstrumentationAgent {
    public static Instrumentation instrumentation;

    public static void premain(final String argument, final Instrumentation instrumentation) {
        InstrumentationAgent.instrumentation = instrumentation;
    }

    public static void agentmain(final String argument, final Instrumentation instrumentation) {
        InstrumentationAgent.instrumentation = instrumentation;
    }
}
