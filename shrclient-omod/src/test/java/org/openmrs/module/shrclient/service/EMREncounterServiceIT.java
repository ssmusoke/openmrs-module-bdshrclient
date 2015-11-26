package org.openmrs.module.shrclient.service;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.*;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.util.FhirBundleContextHolder;
import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class EMREncounterServiceIT extends BaseModuleWebContextSensitiveTest {
    @Autowired
    private ApplicationContext springContext;

    @Autowired
    private PatientService patientService;

    @Autowired
    EMREncounterService emrEncounterService;

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
        emrEncounterService.createOrUpdateEncounters(emrPatient, bundles, healthId);

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

        emrEncounterService.createOrUpdateEncounter(patient, bundles.get(0), "healthId");

        assertEquals(true, patient.isDead());
        assertEquals("HIV", patient.getCauseOfDeath().getName().getName());
    }

    @Test
    public void shouldSaveTestOrders() throws Exception {
        executeDataSet("testDataSets/shrDiagnosticOrderSyncTestDS.xml");
        String healthId = "HIDA764177";
        String shrEncounterId = "shr-enc-id";

        List<EncounterBundle> bundles = getEncounterBundles(healthId, shrEncounterId, "encounterBundles/dstu2/encounterWithDiagnosticOrder.xml");
        Patient emrPatient = patientService.getPatient(1);
        emrEncounterService.createOrUpdateEncounters(emrPatient, bundles, healthId);

        IdMapping idMapping = idMappingsRepository.findByExternalId(shrEncounterId);
        assertNotNull(idMapping);
        Encounter encounter = encounterService.getEncounterByUuid(idMapping.getInternalId());
        Set<Order> orders = encounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
    }

    @Test
    public void shouldSaveDrugOrders() throws Exception {
        executeDataSet("testDataSets/drugOrderDS.xml");
        String healthId = "98001080756";
        String shrEncounterId = "shr-enc-id";

        List<EncounterBundle> bundles = getEncounterBundles(healthId, shrEncounterId, "encounterBundles/dstu2/encounterWithMedicationOrder.xml");
        Patient emrPatient = patientService.getPatient(110);

        emrEncounterService.createOrUpdateEncounters(emrPatient, bundles, healthId);

        IdMapping idMapping = idMappingsRepository.findByExternalId(shrEncounterId);
        assertNotNull(idMapping);
        Encounter encounter = encounterService.getEncounterByUuid(idMapping.getInternalId());
        Set<Order> orders = encounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        assertTrue(orders.iterator().next() instanceof DrugOrder);
    }

    @Test
    public void shouldSaveDrugOrderWithCustomDosageAndStoppedOrder() throws Exception {
        executeDataSet("testDataSets/drugOrderDS.xml");
        String healthId = "98104750156";
        String shrEncounterId = "shr-enc-id";

        List<EncounterBundle> bundles = getEncounterBundles(healthId, shrEncounterId, "encounterBundles/dstu2/encounterWithStoppedMedicationOrderAndCustomDosage.xml");
        Patient emrPatient = patientService.getPatient(110);

        emrEncounterService.createOrUpdateEncounters(emrPatient, bundles, healthId);

        IdMapping idMapping = idMappingsRepository.findByExternalId(shrEncounterId);
        assertNotNull(idMapping);
        Encounter encounter = encounterService.getEncounterByUuid(idMapping.getInternalId());
        Set<Order> orders = encounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        assertTrue(orders.iterator().next() instanceof DrugOrder);
    }

    @Test
    public void shouldSaveDrugOrderWithScheduledDate() throws Exception {
        executeDataSet("testDataSets/drugOrderDS.xml");
        String healthId = "98104750156";
        String shrEncounterId = "shr-enc-id";

        List<EncounterBundle> bundles = getEncounterBundles(healthId, shrEncounterId, "encounterBundles/dstu2/encounterWithMedicationOrderWithScheduledDate.xml");
        Patient emrPatient = patientService.getPatient(110);

        emrEncounterService.createOrUpdateEncounters(emrPatient, bundles, healthId);

        IdMapping idMapping = idMappingsRepository.findByExternalId(shrEncounterId);
        assertNotNull(idMapping);
        Encounter encounter = encounterService.getEncounterByUuid(idMapping.getInternalId());
        Set<Order> orders = encounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        assertTrue(orders.iterator().next() instanceof DrugOrder);
    }

    @Test
    @Ignore("Ignored because of a bug on OpenMRS which doesn't let you revise a retrospective drug order edit")
    public void shouldSaveDrugOrderEditedInDifferentEncounter() throws Exception {
        executeDataSet("testDataSets/drugOrderDS.xml");
        String healthId = "98104750156";
        String shrEncounterId = "shr-enc-id";

        List<EncounterBundle> bundles = getEncounterBundles(healthId, shrEncounterId, "encounterBundles/dstu2/encounterWithMedicationOrderEditedInDifferentEncounter.xml");
        Patient emrPatient = patientService.getPatient(110);

        emrEncounterService.createOrUpdateEncounters(emrPatient, bundles, healthId);

        IdMapping idMapping = idMappingsRepository.findByExternalId(shrEncounterId);
        assertNotNull(idMapping);
        Encounter encounter = encounterService.getEncounterByUuid(idMapping.getInternalId());
        Set<Order> orders = encounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        assertTrue(orders.iterator().next() instanceof DrugOrder);
    }

    @Test
    @Ignore("Ignored because of a bug on OpenMRS which doesn't let you revise a retrospective drug order edit")
    public void shouldSaveDrugOrderEditedInSameEncounter() throws Exception {
        executeDataSet("testDataSets/drugOrderDS.xml");
        String healthId = "98104750156";
        String shrEncounterId = "shr-enc-id";

        List<EncounterBundle> bundles = getEncounterBundles(healthId, shrEncounterId, "encounterBundles/dstu2/encounterWithMedicationOrderEditedInSameEncounter.xml");
        Patient emrPatient = patientService.getPatient(110);

        emrEncounterService.createOrUpdateEncounters(emrPatient, bundles, healthId);

        IdMapping idMapping = idMappingsRepository.findByExternalId(shrEncounterId);
        assertNotNull(idMapping);
        Encounter encounter = encounterService.getEncounterByUuid(idMapping.getInternalId());
        Set<Order> orders = encounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(2, orders.size());
        assertTrue(orders.iterator().next() instanceof DrugOrder);
    }

    @Test
    public void shouldSaveAMedicationOrderWithoutDoseRoutes() throws Exception {
        executeDataSet("testDataSets/drugOrderDS.xml");
        String healthId = "98104750156";
        String shrEncounterId = "shr-enc-id";

        List<EncounterBundle> bundles = getEncounterBundles(healthId, shrEncounterId, "encounterBundles/dstu2/medicationOrderWithoutDoseRouteAndAdditionalInstructions.xml");
        Patient emrPatient = patientService.getPatient(110);

        emrEncounterService.createOrUpdateEncounters(emrPatient, bundles, healthId);

        IdMapping idMapping = idMappingsRepository.findByExternalId(shrEncounterId);
        assertNotNull(idMapping);
        Encounter encounter = encounterService.getEncounterByUuid(idMapping.getInternalId());
        Set<Order> orders = encounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        assertTrue(orders.iterator().next() instanceof DrugOrder);
    }

    @Test
    public void shouldUpdateTheSameEncounter() throws Exception {
        executeDataSet("testDataSets/shrClientEncounterReverseSyncTestDS.xml");
        String healthId = "HID123";
        Patient patient = patientService.getPatient(1);
        String shrEncounterId = "shr-enc-id";
        List<EncounterBundle> bundles = getEncounterBundles(healthId, shrEncounterId, "encounterBundles/dstu2/testFHIREncounter.xml");

        hieEncounterService.createOrUpdateEncounters(patient, bundles, healthId);
        IdMapping mapping = idMappingsRepository.findByExternalId(shrEncounterId);
        String encounterUUID = mapping.getInternalId();
        Date lastSyncDateTime = mapping.getLastSyncDateTime();

        hieEncounterService.createOrUpdateEncounters(patient, bundles, healthId);
        mapping = idMappingsRepository.findByExternalId(shrEncounterId);
        Encounter encounter2 = encounterService.getEncounterByUuid(mapping.getInternalId());

        assertEquals(encounterUUID, encounter2.getUuid());
        assertTrue(mapping.getLastSyncDateTime().after(lastSyncDateTime));
    }

    @Test
    public void shouldVoidOlderObservationsAndRecreateWithNewValues() throws Exception {
        executeDataSet("testDataSets/shrClientEncounterWithObservationTestDs.xml");
        String healthId = "HID123";
        Patient patient = patientService.getPatient(1);
        String shrEncounterId = "shr-enc-id";

        List<EncounterBundle> bundles1 = getEncounterBundles(healthId, shrEncounterId, "encounterBundles/dstu2/encounterWithObservations.xml");
        hieEncounterService.createOrUpdateEncounters(patient, bundles1, healthId);
        IdMapping mapping = idMappingsRepository.findByExternalId(shrEncounterId);
        Encounter encounter = encounterService.getEncounterByUuid(mapping.getInternalId());

        Set<Obs> topLevelObs = encounter.getObsAtTopLevel(true);
        Set<Obs> allObs = encounter.getAllObs(true);
        assertEquals(1, topLevelObs.size());
        assertEquals(3, allObs.size());
        Obs diastolicBp = topLevelObs.iterator().next().getGroupMembers().iterator().next().getGroupMembers().iterator().next();
        assertEquals(new Double(70.0), diastolicBp.getValueNumeric());

        List<EncounterBundle> bundles2 = getEncounterBundles(healthId, shrEncounterId, "encounterBundles/dstu2/encounterWithUpdatedObservations.xml");
        hieEncounterService.createOrUpdateEncounters(patient, bundles2, healthId);
        mapping = idMappingsRepository.findByExternalId(shrEncounterId);
        encounter = encounterService.getEncounterByUuid(mapping.getInternalId());

        assertEquals(2, encounter.getObsAtTopLevel(true).size());
        assertEquals(6, encounter.getAllObs(true).size());
        topLevelObs = encounter.getObsAtTopLevel(false);
        assertEquals(1, topLevelObs.size());
        assertEquals(3, encounter.getAllObs(false).size());
        diastolicBp = topLevelObs.iterator().next().getGroupMembers().iterator().next().getGroupMembers().iterator().next();
        assertEquals(new Double(120.0), diastolicBp.getValueNumeric());
    }

    private List<EncounterBundle> getEncounterBundles(String healthId, String shrEncounterId, String encounterBundleFilePath) throws Exception {
        List<EncounterBundle> bundles = new ArrayList<>();
        EncounterBundle bundle = new EncounterBundle();
        bundle.setPublishedDate(new Date().toString());
        bundle.setHealthId(healthId);
        bundle.setLink("http://shr.com/patients/" + healthId + "/encounters/" + shrEncounterId);
        bundle.setTitle("Encounter:" + shrEncounterId);
        bundle.addContent((Bundle) loadSampleFHIREncounter(encounterBundleFilePath, springContext));
        bundles.add(bundle);
        return bundles;
    }

    public IResource loadSampleFHIREncounter(String filePath, ApplicationContext springContext) throws Exception {
        org.springframework.core.io.Resource resource = springContext.getResource(filePath);
        String bundleXML = org.apache.commons.io.IOUtils.toString(resource.getInputStream());
        return (IResource) FhirBundleContextHolder.getFhirContext().newXmlParser().parseResource(bundleXML);
    }

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }

}