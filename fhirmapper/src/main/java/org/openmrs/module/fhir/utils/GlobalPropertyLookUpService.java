package org.openmrs.module.fhir.utils;

import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.openmrs.module.fhir.utils.Constants.CACHE_TTL;

@Component
public class GlobalPropertyLookUpService {

    private Map<String, Object> globalProperties = new PassiveExpiringMap<>(CACHE_TTL);

    public Integer getGlobalPropertyValue(String propertyName) {
        if (globalProperties.get(propertyName) == null) {
            AdministrationService administrationService = Context.getAdministrationService();
            String propertyValue = administrationService.getGlobalProperty(propertyName);

            if (propertyValue != null && !propertyValue.isEmpty()) {
                globalProperties.put(propertyName, Integer.valueOf(propertyValue));
            }
        }
        return (Integer) globalProperties.get(propertyName);
    }
}
