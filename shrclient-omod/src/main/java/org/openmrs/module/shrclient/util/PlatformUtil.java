package org.openmrs.module.shrclient.util;


import org.openmrs.api.context.Context;
import org.openmrs.module.shrclient.identity.IdentityStore;

import java.util.List;

public class PlatformUtil {
    public static <T> T getRegisteredComponent(Class<T> clazz) {
        List<T> registeredComponents = Context.getRegisteredComponents(clazz);
        if (!registeredComponents.isEmpty()) {
            return registeredComponents.get(0);
        }
        return null;
    }

    public static PropertiesReader getPropertiesReader() {
        return getRegisteredComponent(PropertiesReader.class);
    }

    public static IdentityStore getIdentityStore() {
        return getRegisteredComponent(IdentityStore.class);
    }
}
