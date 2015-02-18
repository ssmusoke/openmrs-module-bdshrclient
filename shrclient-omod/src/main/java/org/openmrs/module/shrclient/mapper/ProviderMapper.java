package org.openmrs.module.shrclient.mapper;

import org.openmrs.Location;
import org.openmrs.Provider;
import org.openmrs.ProviderAttribute;
import org.openmrs.ProviderAttributeType;
import org.openmrs.api.ProviderService;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.shrclient.model.ProviderEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProviderMapper {
    private final static String ORGANIZATION_ATTRIBUTE_TYPE_NAME = "Organization";
    private final static String RETIRE_REASON = "Upstream Deletion";
    private final static String NOT_ACTIVE = "0";
    private ProviderService providerService;
    private ProviderAttributeType organizationAttributeType;

    @Autowired
    public ProviderMapper(ProviderService providerService) {
        this.providerService = providerService;
    }

    public void createOrUpdate(ProviderEntry providerEntry) {
        Provider provider = providerService.getProviderByIdentifier(providerEntry.getId());
        if(provider == null) {
            provider = new Provider();
        }
        provider.setName(providerEntry.getName());
        provider.setIdentifier(providerEntry.getId());
        mapActive(providerEntry, provider);
        mapOrganization(providerEntry, provider);
        providerService.saveProvider(provider);
    }

    private void mapActive(ProviderEntry providerEntry, Provider provider) {
        if (providerEntry.getActive().equals(NOT_ACTIVE)) {
            provider.setRetired(true);
            provider.setRetireReason(RETIRE_REASON);
        }
    }

    private void mapOrganization(ProviderEntry providerEntry, Provider provider) {
        if(providerEntry.getOrganization() != null) {
            if (organizationAttributeType == null) {
                findOrganizationProviderAttributeType();
            }
            ProviderAttribute providerAttribute = new ProviderAttribute();
            providerAttribute.setProvider(provider);
            providerAttribute.setAttributeType(organizationAttributeType);
            String facilityUrl = providerEntry.getOrganization().getReference();
            String facilityId = new EntityReference().parse(Location.class, facilityUrl);
            providerAttribute.setValue(facilityId);
            provider.setAttribute(providerAttribute);
        }
    }

    private void findOrganizationProviderAttributeType() {
        for (ProviderAttributeType providerAttributeType : providerService.getAllProviderAttributeTypes(false)) {
            if (providerAttributeType.getName().equals(ORGANIZATION_ATTRIBUTE_TYPE_NAME)) {
                organizationAttributeType = providerAttributeType;
                break;
            }
        }
    }
}
