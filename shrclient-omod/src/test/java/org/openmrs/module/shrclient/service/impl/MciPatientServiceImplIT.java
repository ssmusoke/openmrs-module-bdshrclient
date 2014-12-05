package org.openmrs.module.shrclient.service.impl;

import org.hl7.fhir.instance.model.Date;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.module.shrclient.TestHelper;
import org.openmrs.module.shrclient.service.MciPatientService;
import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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
    private TestHelper testHelper;

    @Before
    public void setUp() throws Exception {
        testHelper = new TestHelper();
    }

    @Test
    public void shouldSaveEncounter() throws Exception {
        executeDataSet("shrClientEncounterReverseSyncTestDS.xml");
        org.openmrs.Patient emrPatient = patientService.getPatient(1);
        List<EncounterBundle> bundles = new ArrayList<>();
        EncounterBundle bundle = new EncounterBundle();
        bundle.setEncounterId("shr-enc-id");
        bundle.setPublishedDate(new Date().toString());
        String healthId = "HIDA764177";
        bundle.setHealthId(healthId);
        bundle.addContent(testHelper.loadSampleFHIREncounter("classpath:testFHIREncounter.xml", springContext));
        bundles.add(bundle);
        mciPatientService.createOrUpdateEncounters(emrPatient, bundles, healthId);

        List<org.openmrs.Encounter> encountersByPatient = encounterService.getEncountersByPatient(emrPatient);
        assertEquals(1, encountersByPatient.size());
    }

    @Test
    public void shouldSaveOrders() throws Exception {
        executeDataSet("shrDiagnosticOrderSyncTestDS.xml");
        String shrEncounterId = "shr-enc-id";
        String healthId = "5915668841731457025";
        org.openmrs.Patient emrPatient = patientService.getPatient(1);
        List<EncounterBundle> bundles = new ArrayList<>();
        EncounterBundle bundle = new EncounterBundle();
        bundle.setEncounterId(shrEncounterId);
        bundle.setPublishedDate(new Date().toString());
        bundle.setHealthId(healthId);
        bundle.addContent(testHelper.loadSampleFHIREncounter("classpath:encounterWithDiagnosticOrder.xml", springContext));
        bundles.add(bundle);
        mciPatientService.createOrUpdateEncounters(emrPatient, bundles, healthId);

        List<org.openmrs.Encounter> encountersByPatient = encounterService.getEncountersByPatient(emrPatient);
        assertEquals(1, encountersByPatient.size());
        Encounter encounter = encountersByPatient.get(0);
        Set<Order> orders = encounter.getOrders();
        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
    }
}
