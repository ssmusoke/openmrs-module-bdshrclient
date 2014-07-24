package org.openmrs.module.shrclient.service.impl;

import org.hl7.fhir.instance.formats.JsonParser;
import org.hl7.fhir.instance.formats.ParserBase;
import org.hl7.fhir.instance.model.Date;
import org.junit.Assert;
import org.junit.Test;
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

    public ParserBase.ResourceOrFeed loadSampleFHIREncounter() throws Exception {
        Resource resource = springContext.getResource("classpath:testFHIREncounter.json");
        final ParserBase.ResourceOrFeed parsedResource =
                new JsonParser().parseGeneral(resource.getInputStream());
        return parsedResource;
    }

    @Test
    public void shouldSaveEncounter() throws Exception {
        executeDataSet("shrClientEncounterReverseSyncTestDS.xml");
        org.openmrs.Patient emrPatient = patientService.getPatient(1);
        List<EncounterBundle> bundles = new ArrayList<EncounterBundle>();
        EncounterBundle bundle = new EncounterBundle();
        bundle.setEncounterId("shr-enc-id");
        bundle.setDate(new Date().toString());
        bundle.setHealthId("HIDA764177");
        bundle.addContent(loadSampleFHIREncounter());
        bundles.add(bundle);
        mciPatientService.createOrUpdateEncounters(emrPatient, bundles);

        List<org.openmrs.Encounter> encountersByPatient = encounterService.getEncountersByPatient(emrPatient);
        Assert.assertEquals(1, encountersByPatient.size());
    }


}
