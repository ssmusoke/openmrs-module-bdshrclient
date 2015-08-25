package org.openmrs.module.fhir.mapper.emr;


import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import org.apache.commons.collections4.CollectionUtils;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.ProviderAttribute;
import org.openmrs.Visit;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.utils.ProviderLookupService;
import org.openmrs.module.fhir.utils.VisitLookupService;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.openmrs.module.fhir.utils.Constants.ORGANIZATION_ATTRIBUTE_TYPE_NAME;

@Component
public class FHIREncounterMapper {
    @Autowired
    private EncounterService encounterService;

    @Autowired
    private List<FHIRResourceMapper> fhirResourceMappers;

    @Autowired
    public IdMappingsRepository idMappingsRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    public VisitLookupService visitLookupService;
    @Autowired
    private ProviderLookupService providerLookupService;

    public org.openmrs.Encounter map(Encounter fhirEncounter, Date encounterDate, Patient emrPatient, Bundle bundle) throws ParseException {
        Map<String, List<String>> processedList = new HashMap<>();
        org.openmrs.Encounter emrEncounter = new org.openmrs.Encounter();
        emrEncounter.setEncounterDatetime(encounterDate);

        emrEncounter.setPatient(emrPatient);
        for (Bundle.Entry bundleEntry : bundle.getEntry()) {
            final IResource resource = bundleEntry.getResource();
            for (FHIRResourceMapper fhirResourceMapper : fhirResourceMappers) {
                if (fhirResourceMapper.canHandle(resource)) {
                    fhirResourceMapper.map(bundle, resource, emrPatient, emrEncounter, processedList);
                }
            }
        }

        final String encounterTypeName = fhirEncounter.getType().get(0).getText();
        final EncounterType encounterType = encounterService.getEncounterType(encounterTypeName);
        emrEncounter.setEncounterType(encounterType);

        ResourceReferenceDt serviceProvider = fhirEncounter.getServiceProvider();
        if (!serviceProvider.isEmpty()) {
            setInternalFacilityId(emrEncounter, new EntityReference().parse(Location.class, serviceProvider.getReference().getValue()));
        } else {
            setFacilityIdFromProvider(fhirEncounter, emrEncounter);
        }
        Visit visit = visitLookupService.findOrInitializeVisit(emrPatient, encounterDate, fhirEncounter.getClassElement());
        emrEncounter.setVisit(visit);
        visit.addEncounter(emrEncounter);
        return emrEncounter;
    }

    private void setFacilityIdFromProvider(Encounter fhirEncounter, org.openmrs.Encounter emrEncounter) {
        List<Encounter.Participant> participant = fhirEncounter.getParticipant();
        if (CollectionUtils.isEmpty(participant)) return;

        String providerUrl = participant.get(0).getIndividual().getReference().getValue();
        Provider provider = providerLookupService.getProviderByReferenceUrl(providerUrl);
        Set<ProviderAttribute> attributes = provider.getAttributes();
        for (ProviderAttribute attribute : attributes) {
            if (attribute.getAttributeType().getName().equals(ORGANIZATION_ATTRIBUTE_TYPE_NAME)) {
                String facilityId = attribute.getValueReference();
                setInternalFacilityId(emrEncounter, facilityId);
            }
        }
    }

    private void setInternalFacilityId(org.openmrs.Encounter emrEncounter, String facilityId) {
        IdMapping idMapping = idMappingsRepository.findByExternalId(facilityId);
        if (idMapping == null) return;
        Location location = locationService.getLocationByUuid(idMapping.getInternalId());
        emrEncounter.setLocation(location);
    }
}
