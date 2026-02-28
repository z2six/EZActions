package org.z2six.ezactions.api.internal;

import org.z2six.ezactions.api.InputOps;
import org.z2six.ezactions.helper.InputInjector;
import org.z2six.ezactions.util.CommandSequencer;

final class InputOpsImpl implements InputOps {

    @Override
    public boolean deliver(String mappingNameOrLabel, boolean toggle, Mode mode) {
        InputInjector.DeliveryMode dm = switch (mode == null ? Mode.AUTO : mode) {
            case INPUT -> InputInjector.DeliveryMode.INPUT;
            case TICK -> InputInjector.DeliveryMode.TICK;
            case AUTO -> InputInjector.DeliveryMode.AUTO;
        };
        return InputInjector.deliver(mappingNameOrLabel, toggle, dm);
    }

    @Override
    public void enqueueCommands(String[] commands, int perLineDelayTicks) {
        CommandSequencer.enqueue(commands, Math.max(0, perLineDelayTicks));
    }
}

