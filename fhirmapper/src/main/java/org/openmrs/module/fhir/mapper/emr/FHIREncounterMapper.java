package org.openmrs.module.fhir.mapper.emr;


import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.Logger;
import org.hl7.fhir.instance.model.*;
import org.hl7.fhir.instance.model.Encounter;
import org.openmrs.*;
import org.openmrs.Location;
import org.openmrs.Patient;
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
import java.util.*;

import static org.openmrs.module.fhir.utils.Constants.ORGANIZATION_ATTRIBUTE_TYPE_NAME;
import static org.openmrs.module.fhir.utils.DateUtil.parseDate;

@Component
public class FHIREncounterMapper {

    private Logger logger = Logger.getLogger(FHIREncounterMapper.class);

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

    public org.openmrs.Encounter map(Encounter fhirEncounter, String date, Patient emrPatient, AtomFeed feed) throws ParseException {
        Map<String, List<String>> processedList = new HashMap<>();
        org.openmrs.Encounter emrEncounter = new org.openmrs.Encounter();
        Date encounterDate = parseDate(date);
        emrEncounter.setEncounterDatetime(encounterDate);

        emrEncounter.setPatient(emrPatient);
        for (AtomEntry<? extends Resource> atomEntry : feed.getEntryList()) {
            final Resource resource = atomEntry.getResource();
            for (FHIRResourceMapper fhirResourceMapper : fhirResourceMappers) {
                if (fhirResourceMapper.canHandle(resource)) {
                    fhirResourceMapper.map(feed, resource, emrPatient, emrEncounter, processedList);
                }
            }
        }

        final String encounterTypeName = fhirEncounter.getType().get(0).getTextSimple();
        final EncounterType encounterType = encounterService.getEncounterType(encounterTypeName);
        emrEncounter.setEncounterType(encounterType);

        ResourceReference serviceProvider = fhirEncounter.getServiceProvider();
        if (serviceProvider != null) {
            setInternalFacilityId(emrEncounter, new EntityReference().parse(Location.class, serviceProvider.getReferenceSimple()));
        } else {
            setFacilityIdFromProvider(fhirEncounter, emrEncounter);
        }
        Visit visit = visitLookupService.findOrInitializeVisit(emrPatient, encounterDate, fhirEncounter.getClass_());
        emrEncounter.setVisit(visit);
        visit.addEncounter(emrEncounter);
        return emrEncounter;
    }

    private void setFacilityIdFromProvider(Encounter fhirEncounter, org.openmrs.Encounter emrEncounter) {
        List<Encounter.EncounterParticipantComponent> participant = fhirEncounter.getParticipant();
        if (CollectionUtils.isEmpty(participant)) return;

        String providerUrl = participant.get(0).getIndividual().getReferenceSimple();
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
