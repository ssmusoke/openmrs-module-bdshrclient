package org.openmrs.module.shrclient.service;

import org.joda.time.DateTime;
import org.openmrs.*;
import org.openmrs.api.VisitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static org.joda.time.DateTime.now;

@Component
public class VisitLookupService {

    private VisitService visitService;

    @Autowired
    public VisitLookupService(VisitService visitService) {
        this.visitService = visitService;
    }

    public Visit findOrInitializeVisit(Patient patient, Date visitDate, VisitType visitType, Location location) {
        Visit applicableVisit = getVisitForPatientWithinDates(visitType, patient, location, visitDate);
        if (applicableVisit != null) {
            return applicableVisit;
        }
        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setVisitType(visitType);
        visit.setStartDatetime(visitDate);
        visit.setEncounters(new HashSet<Encounter>());
        visit.setUuid(UUID.randomUUID().toString());
        visit.setLocation(location);

        Visit nextVisit = getVisitForPatientForNearestStartDate(patient, visitDate);
        DateTime startTime = new DateTime(visitDate);
        if (nextVisit == null) {
            stopVisitAtEndOfDay(visit, startTime);
        } else {
            stopVisitBeforeStartOfNextVisit(visit, nextVisit, startTime);
        }
        return visit;
    }

    private void stopVisitBeforeStartOfNextVisit(Visit visit, Visit nextVisit, DateTime startTime) {
        DateTime nextVisitStartTime = new DateTime(nextVisit.getStartDatetime());
        DateTime visitStopDate = startTime.withTime(23, 59, 59, 000);
        boolean isEndTimeBeforeNextVisitStart = visitStopDate.isBefore(nextVisitStartTime);
        if (!isEndTimeBeforeNextVisitStart) {
            visitStopDate = nextVisitStartTime.minusSeconds(1);
        }
        visit.setStopDatetime(visitStopDate.toDate());
    }

    private void stopVisitAtEndOfDay(Visit visit, DateTime startTime) {
        Date stopTime = startTime.withTime(23, 59, 59, 000).toDate();
        visit.setStopDatetime(stopTime);
    }

    private Visit getVisitForPatientForNearestStartDate(Patient patient, Date startTime) {
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

    private Visit getVisitForPatientWithinDates(VisitType visitType, Patient patient, Location location, Date startTime) {
        List<Visit> visits = visitService.getVisits(Arrays.asList(visitType), Arrays.asList(patient),
                Arrays.asList(location), null, null, startTime, startTime, null, null, true, false);
        return visits.isEmpty() ? null : visits.get(0);
    }
}
