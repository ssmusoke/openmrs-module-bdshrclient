package org.openmrs.module.fhir.utils;

import org.apache.commons.lang.StringUtils;
import org.openmrs.EncounterProvider;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.ProviderService;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProviderLookupService {

    @Autowired
    private ProviderService providerService;

    public Provider getShrClientSystemProvider() {
        User systemUser = getShrClientSystemUser();
        return providerService.getProvidersByPerson(systemUser.getPerson()).iterator().next();
    }

    private User getShrClientSystemUser() {
        UserService userService = Context.getUserService();
        return userService.getUserByUsername(Constants.SHR_CLIENT_SYSTEM_NAME);
    }

    private Provider getProviderById(String providerId) {
        Provider provider;
        if (StringUtils.isEmpty(providerId) || !providerId.matches("[0-9]+")) {
            provider = getShrClientSystemProvider();
        } else {
            provider = providerService.getProviderByIdentifier(providerId);
        }
        if (provider == null) {
            provider = getShrClientSystemProvider();
        }
        return provider;
    }

    public Provider getProviderByReferenceUrl(String providerReferenceUrl){
        String providerId = new EntityReference().parse(EncounterProvider.class, providerReferenceUrl);
        return getProviderById(providerId);
    }
}
