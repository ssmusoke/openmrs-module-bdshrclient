package org.bahmni.module.shrclient.mapper;

import org.bahmni.module.shrclient.service.MciPatientService;
import org.bahmni.module.shrclient.util.FHIRFeedHelper;
import org.bahmni.module.shrclient.web.controller.dto.EncounterBundle;
import org.hl7.fhir.instance.formats.JsonParser;
import org.hl7.fhir.instance.formats.ParserBase;
import org.hl7.fhir.instance.model.*;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class FHIREncounterMapperIntegrationTest extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private ApplicationContext springContext;
    @Autowired
    private FHIREncounterMapper fhirEncounterMapper;

    @Autowired
    private FHIRConditionsMapper fhirConditionsMapper;

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

    @Before
    public void setUp() throws Exception {
        executeDataSet("shrClientEncounterReverseSyncTestDS.xml");
    }

    @Test
    public void shouldMapFhirEncounter() throws Exception {
        final AtomFeed encounterBundle = loadSampleFHIREncounter().getFeed();
        assertEquals("dc1f5f99-fb2f-4ba8-bf24-14ccdee498f9", encounterBundle.getId());

        FHIRFeedHelper.getComposition(encounterBundle);
        final Composition composition = FHIRFeedHelper.getComposition(encounterBundle);
        assertNotNull(composition);

        assertEquals("2014-07-10T16:05:09+05:30", composition.getDateSimple().toString());
        final Encounter encounter = FHIRFeedHelper.getEncounter(encounterBundle);
        assertNotNull(encounter);
        assertEquals("26504add-2d96-44d0-a2f6-d849dc090254", encounter.getIndication().getReferenceSimple());

        org.openmrs.Patient emrPatient = patientService.getPatient(1);
        org.openmrs.Encounter emrEncounter = fhirEncounterMapper.map(encounter, composition.getDateSimple().toString(), emrPatient);

        assertNotNull(emrEncounter);
        assertEquals(emrPatient, emrEncounter.getPatient());
        assertEquals("26504add-2d96-44d0-a2f6-d849dc090254", emrEncounter.getUuid());
        assertNotNull(emrEncounter.getEncounterDatetime());
        assertEquals(encounter.getType().get(0).getTextSimple(), emrEncounter.getEncounterType().getName());
        assertNotNull(emrEncounter.getEncounterProviders());

        assertNotNull(emrEncounter.getVisit());
        assertEquals("ad41fb41-a41a-4ad6-8835-2f59099acf5a", emrEncounter.getVisit().getUuid());

//        Assert.assertNotNull(emrEncounter.getVisit());
//        Assert.assertNotNull(emrEncounter.getEncounterDatetime());
//        Assert.assertEquals("uuid", emrEncounter.getUuid());
//        Assert.assertNotNull(emrEncounter.getEncounterProviders());
//        Assert.assertNotNull(emrEncounter.getEncounterType());
    }

    @Test
    public void shouldMapFhirCondition() throws Exception {
        executeDataSet("shrClientEncounterReverseSyncTestDS.xml");
        final AtomFeed encounterBundle = loadSampleFHIREncounter().getFeed();

        List<Condition> conditions = FHIRFeedHelper.getConditions(encounterBundle);
        assertEquals(2, conditions.size());
        assertEquals("HIDA764177", conditions.get(0).getSubject().getReferenceSimple());

        org.openmrs.Patient emrPatient = patientService.getPatient(1);
        org.openmrs.Encounter emrEncounter = new org.openmrs.Encounter();

        fhirConditionsMapper.map(emrPatient, emrEncounter, conditions);

        final Set<Obs> visitObs = emrEncounter.getObsAtTopLevel(false);
        Obs firstObs = visitObs.iterator().next();
        assertEquals(2, visitObs.size());
        assertNotNull(firstObs.getGroupMembers());
        assertNotNull(firstObs.getPerson());
        assertNotNull(firstObs.getEncounter());
    }

    @Test
    public void shouldSaveEncounter() throws Exception {
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
        assertEquals(1, encountersByPatient.size());
    }


}
