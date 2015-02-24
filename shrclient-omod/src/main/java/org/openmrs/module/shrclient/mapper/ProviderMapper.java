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

import java.util.List;

@Component
public class ProviderMapper {
    public final static String ORGANIZATION_ATTRIBUTE_TYPE_NAME = "Organization";
    public final static String RETIRE_REASON = "Upstream Deletion";
    private final static String NOT_ACTIVE = "0";
    private ProviderService providerService;

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
            ProviderAttribute providerAttribute = getProviderAttribute(provider);
            String facilityUrl = providerEntry.getOrganization().getReference();
            String facilityId = new EntityReference().parse(Location.class, facilityUrl);
            providerAttribute.setValue(facilityId);
            provider.setAttribute(providerAttribute);
        }
    }

    private ProviderAttribute getProviderAttribute(Provider provider) {
        ProviderAttributeType organizationAttributeType = findOrganizationProviderAttributeType();
        ProviderAttribute providerAttribute = findInExistingAttributes(provider, organizationAttributeType);
        if(providerAttribute == null) {
            providerAttribute = createNewProviderAttribute(provider, organizationAttributeType);
        }
        return providerAttribute;
    }

    private ProviderAttribute findInExistingAttributes(Provider provider, ProviderAttributeType organizationAttributeType) {
        List<ProviderAttribute> providerAttributes = provider.getActiveAttributes(organizationAttributeType);
        if(!providerAttributes.isEmpty()) return providerAttributes.get(0);
        return null;
    }

    private ProviderAttribute createNewProviderAttribute(Provider provider, ProviderAttributeType organizationAttributeType) {
        ProviderAttribute providerAttribute;
        providerAttribute = new ProviderAttribute();
        providerAttribute.setProvider(provider);
        providerAttribute.setAttributeType(organizationAttributeType);
        return providerAttribute;
    }

    private ProviderAttributeType findOrganizationProviderAttributeType() {
        for (ProviderAttributeType providerAttributeType : providerService.getAllProviderAttributeTypes(false)) {
            if (providerAttributeType.getName().equals(ORGANIZATION_ATTRIBUTE_TYPE_NAME)) {
                return providerAttributeType;
            }
        }
        return null;
    }
}
