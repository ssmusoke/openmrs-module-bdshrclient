package org.openmrs.module.fhir.utils;

import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.springframework.stereotype.Component;

@Component
public class GlobalPropertyLookUpService {

    public String getGlobalPropertyValue(String propertyName) {
        AdministrationService administrationService = Context.getAdministrationService();
        String propertyValue = administrationService.getGlobalProperty(propertyName);

        if (propertyValue != null && !propertyValue.isEmpty()) {
            return propertyValue;
        }
        return null;
    }
}
