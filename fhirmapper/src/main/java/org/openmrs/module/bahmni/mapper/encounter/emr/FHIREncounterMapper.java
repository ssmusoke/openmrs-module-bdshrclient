package org.openmrs.module.bahmni.mapper.encounter.emr;


import org.apache.commons.lang.time.DateUtils;
import org.hl7.fhir.instance.model.*;
import org.joda.time.DateTime;
import org.openmrs.EncounterType;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.VisitType;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.VisitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

@Component
public class FHIREncounterMapper {

    @Autowired
    EncounterService encounterService;

    @Autowired
    PatientService patientService;

    @Autowired
    VisitService visitService;

    public org.openmrs.Encounter map(Encounter fhirEncounter, String date, Patient emrPatient) throws ParseException {
        org.openmrs.Encounter emrEncounter = new org.openmrs.Encounter();

        final SimpleDateFormat ISODateFomat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        Date encounterDate = ISODateFomat.parse(date);
        emrEncounter.setEncounterDatetime(encounterDate);
        emrEncounter.setUuid(fhirEncounter.getIdentifier().get(0).getValueSimple());

        final String encounterTypeName = fhirEncounter.getType().get(0).getTextSimple();
        final EncounterType encounterType = encounterService.getEncounterType(encounterTypeName);
        emrEncounter.setEncounterType(encounterType);
        org.hl7.fhir.instance.model.Enumeration<Encounter.EncounterClass> fhirEncounterClass = fhirEncounter.getClass_();

        String visitType = "OPD";
        if (fhirEncounterClass.getValue().equals(Encounter.EncounterClass.inpatient)) {
           visitType = "IPD";
        }
        Visit visit = findOrInitializeVisit(emrPatient, encounterDate, visitType);
        emrEncounter.setPatient(emrPatient);
        emrEncounter.setVisit(visit);
        visit.addEncounter(emrEncounter);
        return emrEncounter;
    }

    public Visit findOrInitializeVisit(Patient patient, Date visitDate, String visitType) {
        Visit applicableVisit = getVisitForPatientWithinDates(patient, visitDate);
        if (applicableVisit != null){
            return applicableVisit;
        }
        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setVisitType(getVisitTypeByName(visitType));
        visit.setStartDatetime(visitDate);
        visit.setEncounters(new HashSet<org.openmrs.Encounter>());
        visit.setUuid(UUID.randomUUID().toString());

        Visit nextVisit = getVisitForPatientForNearestStartDate(patient, visitDate);
        DateTime startTime = new DateTime(visitDate);
        if (nextVisit == null) {
            if (!DateUtils.isSameDay(visitDate, new Date())) {
                Date stopTime = startTime.withTime(23,59, 59, 000).toDate();
                visit.setStopDatetime(stopTime);
            }
        } else {
            DateTime nextVisitStartTime = new DateTime(nextVisit.getStartDatetime());
            DateTime visitStopDate = startTime.withTime(23,59, 59, 000);
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
