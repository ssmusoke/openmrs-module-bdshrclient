package org.openmrs.module.shrclient.service;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Test;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.VisitType;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.VisitService;
import org.openmrs.module.fhir.utils.DateUtil;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Date;

import static org.junit.Assert.*;
@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class VisitLookupServiceIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private VisitLookupService visitLookupService;
    @Autowired
    private VisitService visitService;
    @Autowired
    private LocationService locationService;
    @Autowired
    private PatientService patientService;

    @Test
    public void shouldReturnExistingVisit() throws Exception {
        executeDataSet("testDataSets/visitDs.xml");
        Patient patient = patientService.getPatient(100);
        VisitType visitType = visitService.getVisitType(11);
        Location location = locationService.getLocation(13);
        Date visitDate = DateUtil.parseDate("2014-07-10 13:00:00");
        Visit existingVisit = visitService.getVisit(100);

        Visit createdVisit = visitLookupService.findOrInitializeVisit(patient, visitDate, visitType, location);
        assertEquals(existingVisit.getUuid(), createdVisit.getUuid());
    }

    @Test
    public void shouldReturnNewVisitIfVisitTypeIsDifferent() throws Exception {
        executeDataSet("testDataSets/visitDs.xml");
        Patient patient = patientService.getPatient(100);
        VisitType visitType = visitService.getVisitType(12);
        Location location = locationService.getLocation(13);
        Date visitDate = DateUtil.parseDate("2014-07-10 13:00:00");
        Visit existingVisit = visitService.getVisit(100);

        Visit createdVisit = visitLookupService.findOrInitializeVisit(patient, visitDate, visitType, location);

        assertNotEquals(existingVisit, createdVisit);
        assertEquals(visitDate, createdVisit.getStartDatetime());
        Date expectedStopDate = DateUtil.parseDate("2014-07-10 23:59:59");
        assertEquals(expectedStopDate, createdVisit.getStopDatetime());
    }

    @Test
    public void shouldReturnNewVisitIfLocationIsDifferent() throws Exception {
        executeDataSet("testDataSets/visitDs.xml");
        Patient patient = patientService.getPatient(100);
        VisitType visitType = visitService.getVisitType(11);
        Location location = locationService.getLocation(14);
        Date visitDate = DateUtil.parseDate("2014-07-10 13:00:00");
        Visit existingVisit = visitService.getVisit(100);

        Visit createdVisit = visitLookupService.findOrInitializeVisit(patient, visitDate, visitType, location);

        assertNotEquals(existingVisit, createdVisit);
        assertEquals(visitDate, createdVisit.getStartDatetime());
        Date expectedStopDate = DateUtil.parseDate("2014-07-10 23:59:59");
        assertEquals(expectedStopDate, createdVisit.getStopDatetime());
    }

    @Test
    public void shouldReturnNewVisitIfPatientIsDifferent() throws Exception {
        executeDataSet("testDataSets/visitDs.xml");
        Patient patient = patientService.getPatient(101);
        VisitType visitType = visitService.getVisitType(11);
        Location location = locationService.getLocation(13);
        Visit existingVisit = visitService.getVisit(100);
        Date visitDate = DateUtil.parseDate("2014-07-10 13:00:00");

        Visit createdVisit = visitLookupService.findOrInitializeVisit(patient, visitDate, visitType, location);

        assertNotEquals(existingVisit, createdVisit);
        assertEquals(visitDate, createdVisit.getStartDatetime());
        Date expectedStopDate = DateUtil.parseDate("2014-07-10 23:59:59");
        assertEquals(expectedStopDate, createdVisit.getStopDatetime());
    }

    @Test
    public void shouldReturnNewVisitIfVisitDateIsBeforeExistingVisitStartDate() throws Exception {
        executeDataSet("testDataSets/visitDs.xml");
        Patient patient = patientService.getPatient(100);
        VisitType visitType = visitService.getVisitType(11);
        Location location = locationService.getLocation(13);
        Date visitDate = DateUtil.parseDate("2014-07-10 01:00:00");
        Visit existingVisit = visitService.getVisit(100);

        Visit createdVisit = visitLookupService.findOrInitializeVisit(patient, visitDate, visitType, location);
        assertNotEquals(existingVisit, createdVisit);
        assertEquals(visitDate, createdVisit.getStartDatetime());
        Date expectedStopDate = DateUtil.parseDate("2014-07-10 04:59:59");
        assertEquals(expectedStopDate, createdVisit.getStopDatetime());
    }

    @Test
    public void shouldReturnNewVisitIfVisitDateIsAfterExistingVisitStopDate() throws Exception {
        executeDataSet("testDataSets/visitDs.xml");
        Patient patient = patientService.getPatient(100);
        VisitType visitType = visitService.getVisitType(11);
        Location location = locationService.getLocation(13);
        Date visitDate = DateUtil.parseDate("2014-07-10 23:00:00");
        Visit existingVisit = visitService.getVisit(100);
        
        Visit createdVisit = visitLookupService.findOrInitializeVisit(patient, visitDate, visitType, location);
        
        assertNotEquals(existingVisit, createdVisit);
        assertEquals(visitDate, createdVisit.getStartDatetime());
        Date expectedStopDate = DateUtil.parseDate("2014-07-10 23:59:59");
        assertEquals(expectedStopDate, createdVisit.getStopDatetime());
    }

    @Test
    public void shouldStopANewVisitAtEndOfDay() throws Exception {
        executeDataSet("testDataSets/visitDs.xml");
        Patient patient = patientService.getPatient(100);
        VisitType visitType = visitService.getVisitType(11);
        Location location = locationService.getLocation(13);
        Date visitDate = new Date();

        Visit createdVisit = visitLookupService.findOrInitializeVisit(patient, visitDate, visitType, location);
        Date expectedStopDate = new DateTime(visitDate).withTime(23, 59, 59, 0).toDate();
        assertEquals(visitDate, createdVisit.getStartDatetime());
        assertEquals(expectedStopDate, createdVisit.getStopDatetime());
    }

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }
}