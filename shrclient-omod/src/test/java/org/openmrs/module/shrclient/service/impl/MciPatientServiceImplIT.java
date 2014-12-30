package org.openmrs.module.shrclient.service.impl;

import org.hl7.fhir.instance.model.Date;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.TestOrder;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.module.shrclient.TestHelper;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.service.MciPatientService;
import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
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
    ConceptService conceptService;

    @Autowired
    private ProviderService providerService;

    @Autowired
    private IdMappingsRepository idMappingsRepository;

    private TestHelper testHelper;

    @Before
    public void setUp() throws Exception {
        testHelper = new TestHelper();
    }

    @Test
    public void shouldSaveEncounter() throws Exception {
        executeDataSet("testDataSets/shrClientEncounterReverseSyncTestDS.xml");
        org.openmrs.Patient emrPatient = patientService.getPatient(1);
        String healthId = "HIDA764177";
        List<EncounterBundle> bundles = getEncounterBundles(healthId, "shr-enc-id", "classpath:encounterBundles/testFHIREncounter.xml");
        mciPatientService.createOrUpdateEncounters(emrPatient, bundles, healthId);

        List<org.openmrs.Encounter> encountersByPatient = encounterService.getEncountersByPatient(emrPatient);
        assertEquals(1, encountersByPatient.size());
        Encounter encounter = encountersByPatient.iterator().next();
        assertEquals(1, encounter.getEncounterProviders().size());
        assertEquals(providerService.getProvider(22), encounter.getEncounterProviders().iterator().next().getProvider());
    }

    @Test
    public void shouldSaveTestOrders() throws Exception {
        executeDataSet("testDataSets/shrDiagnosticOrderSyncTestDS.xml");
        String healthId = "5915668841731457025";
        List<EncounterBundle> bundles = getEncounterBundles(healthId, "shr-enc-id", "classpath:encounterBundles/encounterWithDiagnosticOrder.xml");
        Patient emrPatient = patientService.getPatient(1);
        mciPatientService.createOrUpdateEncounters(emrPatient, bundles, healthId);

        List<org.openmrs.Encounter> encountersByPatient = encounterService.getEncountersByPatient(emrPatient);
        assertEquals(1, encountersByPatient.size());
        Encounter encounter = encountersByPatient.get(0);
        Set<Order> orders = encounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        assertTrue(orders.iterator().next() instanceof TestOrder);
    }

    @Test
    public void shouldSaveDrugOrders() throws Exception {
        executeDataSet("testDataSets/drugOrderDS.xml");
        String healthId = "5947482439084408833";
        String shrEncounterId = "shr-enc-id";
        List<EncounterBundle> bundles = getEncounterBundles(healthId, shrEncounterId, "encounterBundles/encounterWithMedicationPrescription.xml");
        Patient emrPatient = patientService.getPatient(110);
        assertEquals(0, encounterService.getEncountersByPatient(emrPatient).size());

        mciPatientService.createOrUpdateEncounters(emrPatient, bundles, healthId);

        IdMapping idMapping = idMappingsRepository.findByExternalId(shrEncounterId);
        assertNotNull(idMapping);
        Encounter encounter = encounterService.getEncounterByUuid(idMapping.getInternalId());
        Set<Order> orders = encounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        assertTrue(orders.iterator().next() instanceof DrugOrder);
    }

    private List<EncounterBundle> getEncounterBundles(String healthId, String shrEncounterId, String encounterBundleFilePath) throws Exception {
        List<EncounterBundle> bundles = new ArrayList<>();
        EncounterBundle bundle = new EncounterBundle();
        bundle.setEncounterId(shrEncounterId);
        bundle.setPublishedDate(new Date().toString());
        bundle.setHealthId(healthId);
        bundle.addContent(testHelper.loadSampleFHIREncounter(encounterBundleFilePath, springContext));
        bundles.add(bundle);
        return bundles;
    }

}
