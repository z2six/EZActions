package org.z2six.ezactions.api.internal;

import org.z2six.ezactions.api.EzActionsApi;

/** Public bridge so api package can obtain the impl without exposing the impl class. */
public final class ApiFactory {
    private ApiFactory() {}
    public static EzActionsApi create() {
        return new EzActionsApiImpl(); // EzActionsApiImpl remains package-private
    }
}
