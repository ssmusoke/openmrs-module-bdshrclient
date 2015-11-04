package org.openmrs.module.fhir.utils;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.ProviderService;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir.MRSProperties;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.shrclient.util.SystemProperties;
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

    public Provider getProviderByReferenceUrlOrDefault(String providerReferenceUrl) {
        Provider provider = null;
        if (providerReferenceUrl != null) {
            String providerId = new EntityReference().parse(Provider.class, providerReferenceUrl);
            provider = getProviderById(providerId);
        }
        if (provider == null) {
            provider = getShrClientSystemProvider();
        }
        return provider;
    }

    public Provider getProviderByReferenceUrl(String providerReferenceUrl) {
        String providerId = new EntityReference().parse(Provider.class, providerReferenceUrl);
        return getProviderById(providerId);
    }

    public String getProviderRegistryUrl(SystemProperties systemProperties, Provider provider) {
        if (provider == null) return null;
        String identifier = provider.getIdentifier();
        if (!isHIEProvider(identifier)) return null;
        return new EntityReference().build(Provider.class, systemProperties, identifier);
    }

    private boolean isHIEProvider(String identifier) {
        try {
            Integer.parseInt(identifier);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private Provider getProviderById(String providerId) {
        Provider provider = null;
        if (!StringUtils.isEmpty(providerId)) {
            provider = providerService.getProviderByIdentifier(providerId);
        }
        return provider;
    }

    private User getShrClientSystemUser() {
        UserService userService = Context.getUserService();
        return userService.getUserByUsername(MRSProperties.SHR_CLIENT_SYSTEM_NAME);
    }
}
