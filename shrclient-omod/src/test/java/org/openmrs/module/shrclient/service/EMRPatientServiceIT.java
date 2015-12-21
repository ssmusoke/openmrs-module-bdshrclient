package org.openmrs.module.shrclient.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;
import org.openmrs.*;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.module.fhir.Constants;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class EMRPatientServiceIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private PatientService patientService;

    @Autowired
    private EncounterService encounterService;

    @Autowired
    EMRPatientService emrPatientService;

    @Test
    public void shouldMapRelationsToPatientAttributesWhenPresent() throws Exception {
        executeDataSet("testDataSets/patientUpdateDS.xml");
        org.openmrs.module.shrclient.model.Patient patient = getPatientFromJson("patients_response/patientWithRelations.json");

        emrPatientService.createOrUpdatePatient(patient);

        Patient savedPatient = patientService.getPatient(1);

        PersonAttribute fatherName = savedPatient.getAttribute(Constants.FATHER_NAME_ATTRIBUTE_TYPE);
        assertNotNull(fatherName);
        assertEquals(Constants.FATHER_NAME_ATTRIBUTE_TYPE, fatherName.getAttributeType().getName());
        assertEquals("Md. Sakib Ali Khan", fatherName.getValue());

        PersonAttribute spouseName = savedPatient.getAttribute(Constants.SPOUSE_NAME_ATTRIBUTE_TYPE);
        assertNotNull(spouseName);
        assertEquals(Constants.SPOUSE_NAME_ATTRIBUTE_TYPE, spouseName.getAttributeType().getName());
        assertEquals("Azad", spouseName.getValue());
    }

    @Test
    public void shouldUpdatePatientAttributesOnDownloadIfPresentAlready() throws Exception {
        executeDataSet("testDataSets/patientUpdateDS.xml");
        org.openmrs.module.shrclient.model.Patient patient = getPatientFromJson("patients_response/patientWithRelations.json");

        emrPatientService.createOrUpdatePatient(patient);
        //updated twice
        emrPatientService.createOrUpdatePatient(patient);

        Patient savedPatient = patientService.getPatient(1);

        List<PersonAttribute> fatherName = savedPatient.getAttributes(Constants.FATHER_NAME_ATTRIBUTE_TYPE);
        assertEquals(1, fatherName.size());

        List<PersonAttribute> spouseName = savedPatient.getAttributes(Constants.SPOUSE_NAME_ATTRIBUTE_TYPE);
        assertEquals(1, spouseName.size());
    }

    private org.openmrs.module.shrclient.model.Patient getPatientFromJson(String patientJson) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        URL resource = URLClassLoader.getSystemResource(patientJson);
        final String patientResponse = FileUtils.readFileToString(new File(resource.getPath()));

        return mapper.readValue(patientResponse, org.openmrs.module.shrclient.model.Patient.class);
    }

    @Test
    public void shouldMergePatientAttributesAndVoidToBeRetiredPatient() throws Exception {
        executeDataSet("testDataSets/attributeTypesDS.xml");
        executeDataSet("testDataSets/dhakaAddressHierarchy.xml");
        executeDataSet("testDataSets/patientMergeDS.xml");
        org.openmrs.module.shrclient.model.Patient patientToBeRetired = getPatientFromJson("patients_response/patientWithRelations.json");
        org.openmrs.module.shrclient.model.Patient patientToBeRetained = getPatientFromJson("patients_response/p12341467785.json");

        emrPatientService.createOrUpdatePatient(patientToBeRetained);
        emrPatientService.createOrUpdatePatient(patientToBeRetired);
        String retainedHealthId = patientToBeRetained.getHealthId();

        emrPatientService.mergePatients(retainedHealthId, patientToBeRetired.getHealthId());

        Patient retiredPatient = patientService.getPatient(11);
        Patient retainedPatient = patientService.getPatient(21);

        assertTrue(retiredPatient.getVoided());
//        assertEquals(String.format("Merged with %s", retainedHealthId), retiredPatient.getVoidReason());
        assertFalse(retainedPatient.getVoided());

        assertEquals(retainedHealthId, retainedPatient.getAttribute(Constants.HEALTH_ID_ATTRIBUTE).getValue());
        assertEquals("7654376543777", retainedPatient.getAttribute(Constants.NATIONAL_ID_ATTRIBUTE).getValue());
        assertEquals("54098599985409999", retainedPatient.getAttribute(Constants.BIRTH_REG_NO_ATTRIBUTE).getValue());
        assertEquals("121", retainedPatient.getAttribute(Constants.HOUSE_HOLD_CODE_ATTRIBUTE).getValue());
        assertEquals("123", retainedPatient.getAttribute(Constants.PHONE_NUMBER).getValue());

        assertEquals("F", retainedPatient.getGender());
        assertEquals("Abdul", retainedPatient.getGivenName());
        assertEquals("Khan", retainedPatient.getFamilyName());
        assertEquals("Rahman Khan", retainedPatient.getAttribute(Constants.FATHER_NAME_ATTRIBUTE_TYPE).getValue());
        assertEquals("Bismillah", retainedPatient.getAttribute(Constants.SPOUSE_NAME_ATTRIBUTE_TYPE).getValue());
        assertEquals(2, retainedPatient.getAddresses().size());
        Iterator<PersonAddress> addressIterator = retainedPatient.getAddresses().iterator();
        PersonAddress retainedPatientAddress = addressIterator.next();
        assertEquals("Dhaka", retainedPatientAddress.getStateProvince());
        assertEquals("Dhaka", retainedPatientAddress.getCountyDistrict());
        assertEquals("Adabor", retainedPatientAddress.getAddress5());
        assertEquals("Dhaka Uttar City Corp.", retainedPatientAddress.getAddress4());
        assertEquals("Urban Ward No-30 (43)", retainedPatientAddress.getAddress3());
        assertEquals("house", retainedPatientAddress.getAddress1());

        assertFalse(addressIterator.next().getPreferred());
    }

    @Test
    public void shouldMergeEncountersWhenPatientsAreMerged() throws Exception {
        executeDataSet("testDataSets/attributeTypesDS.xml");
        executeDataSet("testDataSets/dhakaAddressHierarchy.xml");
        executeDataSet("testDataSets/patientMergeDS.xml");
        org.openmrs.module.shrclient.model.Patient patientToBeRetired = getPatientFromJson("patients_response/patientWithRelations.json");
        org.openmrs.module.shrclient.model.Patient patientToBeRetained = getPatientFromJson("patients_response/p12341467785.json");

        emrPatientService.createOrUpdatePatient(patientToBeRetained);
        emrPatientService.createOrUpdatePatient(patientToBeRetired);
        String retainedHealthId = patientToBeRetained.getHealthId();

        List<Encounter> encountersOfToBeRetiredPatient = encounterService.getEncountersByPatientId(11);
        List<Encounter> encountersOfToBeRetainedPatient = encounterService.getEncountersByPatientId(21);

        Integer visitIdOfRetiredPatient = encountersOfToBeRetiredPatient.get(0).getVisit().getId();
        Integer visitIdOfRetainedPatient = encountersOfToBeRetainedPatient.get(0).getVisit().getId();
        Date startDatetimeForVisitOfToBeRetiredPatient = encountersOfToBeRetiredPatient.get(0).getVisit().getStartDatetime();
        Date stopDatetimeForVisitOfToBeRetiredPatient = encountersOfToBeRetiredPatient.get(0).getVisit().getStopDatetime();
        Date startDatetimeForVisitOfToBeRetainedPatient = encountersOfToBeRetainedPatient.get(0).getVisit().getStartDatetime();
        Date stopDatetimeForVisitOfToBeRetainedPatient = encountersOfToBeRetainedPatient.get(0).getVisit().getStopDatetime();


        assertEquals(1, encountersOfToBeRetiredPatient.size());
        assertEquals(2, encountersOfToBeRetiredPatient.get(0).getOrders().size());
        assertEquals(4, encountersOfToBeRetiredPatient.get(0).getAllObs(true).size());
        assertEquals(1, encountersOfToBeRetainedPatient.size());

        emrPatientService.mergePatients(retainedHealthId, patientToBeRetired.getHealthId());

        List<Encounter> encountersOfRetainedPatients = encounterService.getEncountersByPatientId(21);
        Encounter mergedEncounterOne = encountersOfRetainedPatients.get(0);
        Encounter mergedEncounterTwo = encountersOfRetainedPatients.get(1);

        assertEquals(0, encounterService.getEncountersByPatientId(11).size());
        assertEquals(2, encountersOfRetainedPatients.size());
        assertEquals(2, mergedEncounterOne.getOrders().size());
        assertEquals(4, mergedEncounterOne.getAllObs(true).size());

        assertEquals(visitIdOfRetiredPatient, mergedEncounterOne.getVisit().getId());
        assertEquals(visitIdOfRetainedPatient, mergedEncounterTwo.getVisit().getId());
        assertEquals(startDatetimeForVisitOfToBeRetiredPatient, mergedEncounterOne.getVisit().getStartDatetime());
        assertEquals(stopDatetimeForVisitOfToBeRetiredPatient, mergedEncounterOne.getVisit().getStopDatetime());
        assertEquals(startDatetimeForVisitOfToBeRetainedPatient, mergedEncounterTwo.getVisit().getStartDatetime());
        assertEquals(stopDatetimeForVisitOfToBeRetainedPatient, mergedEncounterTwo.getVisit().getStopDatetime());

        Iterator<Order> orderIterator =  mergedEncounterOne.getOrders().iterator();
        assertTrue(orderIterator.next().isVoided());
        assertFalse(orderIterator.next().isVoided());

    }

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }
}
