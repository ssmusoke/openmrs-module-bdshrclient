package org.openmrs.module.shrclient.service.impl;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.*;
import org.openmrs.Patient;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.module.fhir.utils.Constants;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.*;
import org.openmrs.module.shrclient.service.MciPatientService;
import org.openmrs.module.shrclient.util.FhirBundleContextHolder;
import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MciPatientServiceImplIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private ApplicationContext springContext;

    @Autowired
    private PatientService patientService;

    @Autowired
    MciPatientService mciPatientService;

    @Autowired
    EncounterService encounterService;

    @Autowired
    private ProviderService providerService;

    @Autowired
    private IdMappingsRepository idMappingsRepository;

    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/omrsGlobalPropertyTestDS.xml");
    }

    @Test
    public void shouldSaveEncounter() throws Exception {
        executeDataSet("testDataSets/shrClientEncounterReverseSyncTestDS.xml");
        org.openmrs.Patient emrPatient = patientService.getPatient(1);
        String healthId = "HIDA764177";
        String shrEncounterId = "shr-enc-id";
        List<EncounterBundle> bundles = getEncounterBundles(healthId, shrEncounterId, "encounterBundles/dstu2/testFHIREncounter.xml");
        mciPatientService.createOrUpdateEncounters(emrPatient, bundles, healthId);

        IdMapping idMapping = idMappingsRepository.findByExternalId(shrEncounterId);
        assertNotNull(idMapping);
        Encounter encounter = encounterService.getEncounterByUuid(idMapping.getInternalId());
        assertEquals(1, encounter.getEncounterProviders().size());
        assertEquals(providerService.getProvider(22), encounter.getEncounterProviders().iterator().next().getProvider());
    }

    @Test
    public void shouldProcessDeathInfoOfPatientAfterEncounterSave() throws Exception {
        executeDataSet("testDataSets/patientDeathNoteDS.xml");

        Patient patient = patientService.getPatient(1);
        List<EncounterBundle> bundles = getEncounterBundles("healthId", "shrEncounterId", "classpath:encounterBundles/dstu2/encounterWithDiagnosticOrder.xml");

        assertEquals(true, patient.isDead());
        assertEquals("Unspecified Cause Of Death", patient.getCauseOfDeath().getName().getName());

        mciPatientService.createOrUpdateEncounter(patient, bundles.get(0), "healthId");

        assertEquals(true, patient.isDead());
        assertEquals("HIV", patient.getCauseOfDeath().getName().getName());
    }

    @Test
    public void shouldSaveTestOrders() throws Exception {
        executeDataSet("testDataSets/shrDiagnosticOrderSyncTestDS.xml");
        String healthId = "HIDA764177";
        String shrEncounterId = "shr-enc-id";

        List<EncounterBundle> bundles = getEncounterBundles(healthId, shrEncounterId, "classpath:encounterBundles/dstu2/encounterWithDiagnosticOrder.xml");
        Patient emrPatient = patientService.getPatient(1);
        mciPatientService.createOrUpdateEncounters(emrPatient, bundles, healthId);

        IdMapping idMapping = idMappingsRepository.findByExternalId(shrEncounterId);
        assertNotNull(idMapping);
        Encounter encounter = encounterService.getEncounterByUuid(idMapping.getInternalId());
        Set<Order> orders = encounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
    }

//    @Test
//    public void shouldSaveDrugOrders() throws Exception {
//        executeDataSet("testDataSets/drugOrderDS.xml");
//        String healthId = "5947482439084408833";
//        String shrEncounterId = "shr-enc-id";
//
//        List<EncounterBundle> bundles = getEncounterBundles(healthId, shrEncounterId, "encounterBundles/encounterWithMedicationPrescription.xml");
//        Patient emrPatient = patientService.getPatient(110);
//        assertEquals(0, encounterService.getEncountersByPatient(emrPatient).size());
//
//        mciPatientService.createOrUpdateEncounters(emrPatient, bundles, healthId);
//
//        IdMapping idMapping = idMappingsRepository.findByExternalId(shrEncounterId);
//        assertNotNull(idMapping);
//        Encounter encounter = encounterService.getEncounterByUuid(idMapping.getInternalId());
//        Set<Order> orders = encounter.getOrders();
//        assertFalse(orders.isEmpty());
//        assertEquals(1, orders.size());
//        assertTrue(orders.iterator().next() instanceof DrugOrder);
//    }

    @Test
    public void shouldGetCauseOfDeathOfPatientIfAnyObservationCapturedCauseOfDeath() throws Exception {
        executeDataSet("testDataSets/patientDeathNoteDS.xml");
        Patient patient = patientService.getPatient(1);

        Concept actualCauseOfDeath = mciPatientService.getCauseOfDeath(patient);

        assertEquals("HIV", actualCauseOfDeath.getName().getName());

    }

    @Test
    public void shouldReturnUnspecifiedCauseOfDeathIfThereIsNoObservationCapturedAndPatientIsToBeMarkedDead() throws Exception {
        executeDataSet("testDataSets/patientDeathNoteDS.xml");
        Patient patient = patientService.getPatient(4);

        patient.setDead(true);
        Concept actualCauseOfDeath = mciPatientService.getCauseOfDeath(patient);

        assertEquals("Unspecified Cause Of Death", actualCauseOfDeath.getName().getName());
    }

    @Test
    public void shouldReturnUnspecifiedCauseOfDeathIfThePatientIsNewAndPatientIsToBeMarkedDead() throws Exception {
        executeDataSet("testDataSets/patientDeathNoteDS.xml");
        Patient patient = new Patient();

        patient.setDead(true);
        Concept actualCauseOfDeath = mciPatientService.getCauseOfDeath(patient);

        assertEquals("Unspecified Cause Of Death", actualCauseOfDeath.getName().getName());
    }

    @Test
    public void shouldReturnCauseOfDeathIfCauseOfDeathAttributeIsOtherThanUnspecifiedCauseOfDeath() throws Exception {
        executeDataSet("testDataSets/patientDeathNoteDS.xml");
        Patient patient = patientService.getPatient(3);

        Concept actualCauseOfDeath = mciPatientService.getCauseOfDeath(patient);

        assertEquals("CANCER", actualCauseOfDeath.getName().getName());


    }

    @Test
    public void shouldMapRelationsToPatientAttributesWhenPresent() throws Exception {
        executeDataSet("testDataSets/patientUpdateDS.xml");
        org.openmrs.module.shrclient.model.Patient patient = getPatientFromJson("patients_response/patientWithRelations.json");

        mciPatientService.createOrUpdatePatient(patient);

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

        mciPatientService.createOrUpdatePatient(patient);
        //updated twice
        mciPatientService.createOrUpdatePatient(patient);

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

    private List<EncounterBundle> getEncounterBundles(String healthId, String shrEncounterId, String encounterBundleFilePath) throws Exception {
        List<EncounterBundle> bundles = new ArrayList<>();
        EncounterBundle bundle = new EncounterBundle();
        bundle.setEncounterId(shrEncounterId);
        bundle.setPublishedDate(new Date().toString());
        bundle.setHealthId(healthId);
        bundle.setLink("http://shr.com/patients/" + healthId + "/encounters/" + shrEncounterId);
        bundle.setTitle("Encounter:" + shrEncounterId);
        bundle.addContent((Bundle) loadSampleFHIREncounter(encounterBundleFilePath, springContext));
        bundles.add(bundle);
        return bundles;
    }

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }

    public IResource loadSampleFHIREncounter(String filePath, ApplicationContext springContext) throws Exception {
        org.springframework.core.io.Resource resource = springContext.getResource(filePath);
        String bundleXML = org.apache.commons.io.IOUtils.toString(resource.getInputStream());
        return (IResource) FhirBundleContextHolder.getFhirContext().newXmlParser().parseResource(bundleXML);
    }
}
