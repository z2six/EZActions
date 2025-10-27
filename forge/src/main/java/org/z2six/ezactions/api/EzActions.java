package org.z2six.ezactions.api;

import org.z2six.ezactions.api.internal.EzActionsApiImpl;

public final class EzActions {
    private EzActions() {}
    private static final EzActionsApi INSTANCE = new EzActionsApiImpl();
    public static EzActionsApi get() { return INSTANCE; }
}
