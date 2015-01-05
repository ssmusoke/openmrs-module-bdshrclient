package org.openmrs.module.fhir.mapper.emr;


import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.hl7.fhir.instance.model.*;
import org.joda.time.DateTime;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.VisitType;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.VisitService;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_IN_PATIENT_VISIT_TYPE;
import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_OUT_PATIENT_VISIT_TYPE;
import static org.openmrs.module.fhir.utils.DateUtil.parseDate;

@Component
public class FHIREncounterMapper {

    private Logger logger = Logger.getLogger(FHIREncounterMapper.class);

    @Autowired
    private EncounterService encounterService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private VisitService visitService;

    @Autowired
    private List<FHIRResourceMapper> fhirResourceMappers;

    @Autowired
    private IdMappingsRepository idMappingsRepository;

    @Autowired
    private LocationService locationService;

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

        VisitType visitType = getVisitType(fhirEncounter.getClass_());
        Visit visit = findOrInitializeVisit(emrPatient, encounterDate, visitType);
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

    private VisitType getVisitType(Enumeration<Encounter.EncounterClass> cls) {
        List<VisitType> allVisitTypes = visitService.getAllVisitTypes();
        Encounter.EncounterClass encounterClass = cls.getValue();
        VisitType encVisitType = identifyVisitTypeByName(allVisitTypes, encounterClass.toString());
        if (encVisitType != null) {
            return encVisitType;
        }

        if (encounterClass.equals(Encounter.EncounterClass.inpatient)) {
            return identifyVisitTypeByName(allVisitTypes, MRS_IN_PATIENT_VISIT_TYPE);
        } else if (encounterClass.equals(Encounter.EncounterClass.outpatient)) {
            return identifyVisitTypeByName(allVisitTypes, MRS_OUT_PATIENT_VISIT_TYPE);
        }

        return null;
    }

    private VisitType identifyVisitTypeByName(List<VisitType> allVisitTypes, String visitTypeName) {
        VisitType encVisitType = null;
        for (VisitType visitType : allVisitTypes) {
            if (visitType.getName().toLowerCase().equals(visitTypeName)) {
                encVisitType = visitType;
                break;
            }
        }
        return encVisitType;
    }

    public Visit findOrInitializeVisit(Patient patient, Date visitDate, VisitType visitType) {
        Visit applicableVisit = getVisitForPatientWithinDates(patient, visitDate);
        if (applicableVisit != null) {
            return applicableVisit;
        }
        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setVisitType(visitType);
        visit.setStartDatetime(visitDate);
        visit.setEncounters(new HashSet<org.openmrs.Encounter>());
        visit.setUuid(UUID.randomUUID().toString());

        Visit nextVisit = getVisitForPatientForNearestStartDate(patient, visitDate);
        DateTime startTime = new DateTime(visitDate);
        if (nextVisit == null) {
            if (!DateUtils.isSameDay(visitDate, new Date())) {
                Date stopTime = startTime.withTime(23, 59, 59, 000).toDate();
                visit.setStopDatetime(stopTime);
            }
        } else {
            DateTime nextVisitStartTime = new DateTime(nextVisit.getStartDatetime());
            DateTime visitStopDate = startTime.withTime(23, 59, 59, 000);
            boolean isEndTimeBeforeNextVisitStart = visitStopDate.isBefore(nextVisitStartTime);
            if (!isEndTimeBeforeNextVisitStart) {
                visitStopDate = nextVisitStartTime.minusSeconds(1);
            }
            visit.setStopDatetime(visitStopDate.toDate());
        }
        return visit;
    }

    protected Visit getVisitForPatientWithinDates(Patient patient, Date startTime) {
        List<Visit> visits = visitService.getVisits(null, Arrays.asList(patient), null, null, null, startTime, startTime, null, null, true, false);
        return visits.isEmpty() ? null : visits.get(0);
    }

    protected Visit getVisitForPatientForNearestStartDate(Patient patient, Date startTime) {
        List<Visit> visits = visitService.getVisits(null, Arrays.asList(patient), null, null, startTime, null, null, null, null, true, false);
        if (visits.isEmpty()) {
            return null;
        }
        Collections.sort(visits, new Comparator<Visit>() {
            @Override
            public int compare(Visit v1, Visit v2) {
                return v1.getStartDatetime().compareTo(v2.getStartDatetime());
            }
        });
        return visits.get(0);
    }

    private VisitType getVisitTypeByName(String visitTypeName) {
        List<VisitType> visitTypes = visitService.getVisitTypes(visitTypeName);
        return visitTypes.isEmpty() ? null : visitTypes.get(0);
    }
}
