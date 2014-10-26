package org.openmrs.module.shrclient.service.impl;

import org.hl7.fhir.instance.formats.ParserBase;
import org.hl7.fhir.instance.formats.XmlParser;
import org.hl7.fhir.instance.model.Date;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.module.shrclient.service.MciPatientService;
import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class MciPatientServiceImplTest extends BaseModuleWebContextSensitiveTest {
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

    public ParserBase.ResourceOrFeed loadSampleFHIREncounter(String filePath) throws Exception {
        Resource resource = springContext.getResource(filePath);
        final ParserBase.ResourceOrFeed parsedResource =
                new XmlParser().parseGeneral(resource.getInputStream());
        return parsedResource;
    }

    @Test
    public void shouldSaveEncounter() throws Exception {
        executeDataSet("shrClientEncounterReverseSyncTestDS.xml");
        org.openmrs.Patient emrPatient = patientService.getPatient(1);
        List<EncounterBundle> bundles = new ArrayList<EncounterBundle>();
        EncounterBundle bundle = new EncounterBundle();
        bundle.setEncounterId("shr-enc-id");
        bundle.setPublishedDate(new Date().toString());
        String healthId = "HIDA764177";
        bundle.setHealthId(healthId);
        bundle.addContent(loadSampleFHIREncounter("classpath:testFHIREncounter.xml"));
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
        List<EncounterBundle> bundles = new ArrayList<EncounterBundle>();
        EncounterBundle bundle = new EncounterBundle();
        bundle.setEncounterId(shrEncounterId);
        bundle.setPublishedDate(new Date().toString());
        bundle.setHealthId(healthId);
        bundle.addContent(loadSampleFHIREncounter("classpath:encounterWithDiagnosticOrder.xml"));
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
