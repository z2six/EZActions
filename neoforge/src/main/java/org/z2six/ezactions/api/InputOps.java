package org.z2six.ezactions.api;

/** Helper operations for input + command sequencing. */
public interface InputOps {
    enum Mode { AUTO, INPUT, TICK }

    /** Deliver a key mapping by its translation-key or localized name. */
    boolean deliver(String mappingNameOrLabel, boolean toggle, Mode mode);

    /** Enqueue multiple slash-commands with per-line delay in ticks (client-side). */
    void enqueueCommands(String[] commands, int perLineDelayTicks);
}
