package org.openmrs.module.fhir.mapper.emr;


import org.apache.log4j.Logger;
import org.hl7.fhir.instance.model.AtomEntry;
import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.Encounter;
import org.hl7.fhir.instance.model.Resource;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.VisitService;
import org.openmrs.module.fhir.mapper.model.EntityReference;
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

import static org.openmrs.module.fhir.utils.DateUtil.parseDate;

@Component
public class FHIREncounterMapper {

    private Logger logger = Logger.getLogger(FHIREncounterMapper.class);

    @Autowired
    private EncounterService encounterService;

    @Autowired
    private VisitService visitService;

    @Autowired
    private List<FHIRResourceMapper> fhirResourceMappers;

    @Autowired
    public IdMappingsRepository idMappingsRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    public VisitLookupService visitLookupService;

    public org.openmrs.Encounter map(Encounter fhirEncounter, String date, Patient emrPatient, AtomFeed feed) throws ParseException {
        Map<String, List<String>> processedList = new HashMap<>();
        org.openmrs.Encounter emrEncounter = new org.openmrs.Encounter();

        Date encounterDate = parseDate(date.toString());
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

        setInternalFacilityId(emrEncounter, new EntityReference().parse(Location.class, fhirEncounter.getServiceProvider().getReferenceSimple()));

        Visit visit = visitLookupService.findOrInitializeVisit(emrPatient, encounterDate, fhirEncounter.getClass_());
        emrEncounter.setVisit(visit);
        visit.addEncounter(emrEncounter);
        return emrEncounter;
    }

    private void setInternalFacilityId(org.openmrs.Encounter emrEncounter, String facilityId) {
        IdMapping idMapping = idMappingsRepository.findByExternalId(facilityId);
        if (idMapping == null) return;
        Location location = locationService.getLocationByUuid(idMapping.getInternalId());
        emrEncounter.setLocation(location);
    }
}
