package org.openmrs.module.fhir.mapper.emr;

import org.hl7.fhir.instance.formats.ParserBase;
import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.Composition;
import org.hl7.fhir.instance.model.Condition;
import org.hl7.fhir.instance.model.Encounter;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.ResourceType;
import org.junit.After;
import org.junit.Test;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
import org.openmrs.api.PatientService;
import org.openmrs.module.fhir.MapperTestHelper;
import org.openmrs.module.fhir.TestFhirFeedHelper;
import org.openmrs.module.fhir.utils.DateUtil;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FHIREncounterMapperIntegrationTest extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private ApplicationContext springContext;

    @Autowired
    private FHIREncounterMapper fhirEncounterMapper;

    @Autowired
    private PatientService patientService;

    @Autowired
    ConceptService conceptService;

    public ParserBase.ResourceOrFeed loadSampleFHIREncounter() throws Exception {
        ParserBase.ResourceOrFeed parsedResource = new MapperTestHelper().loadSampleFHIREncounter("classpath:encounterBundles/testFHIREncounter.xml", springContext);
        return parsedResource;
    }

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }


    @Test
    public void shouldMapFhirEncounter() throws Exception {
        executeDataSet("testDataSets/shrClientEncounterReverseSyncTestDS.xml");
        final AtomFeed encounterBundle = loadSampleFHIREncounter().getFeed();
        assertEquals("urn:dc1f5f99-fb2f-4ba8-bf24-14ccdee498f9", encounterBundle.getId());

        FHIRFeedHelper.getComposition(encounterBundle);
        final Composition composition = FHIRFeedHelper.getComposition(encounterBundle);
        assertNotNull(composition);

        assertEquals("2014-07-10T16:05:09+05:30", composition.getDateSimple().toString());
        final Encounter encounter = FHIRFeedHelper.getEncounter(encounterBundle);
        assertNotNull(encounter);
        assertEquals("urn:26504add-2d96-44d0-a2f6-d849dc090254", encounter.getIndication().getReferenceSimple());

        org.openmrs.Patient emrPatient = patientService.getPatient(1);
        org.openmrs.Encounter emrEncounter = fhirEncounterMapper.map(encounter, composition.getDateSimple().toString(), emrPatient, encounterBundle);

        assertNotNull(emrEncounter);
        assertEquals(emrPatient, emrEncounter.getPatient());
        assertEquals(DateUtil.parseDate("2014-07-10T16:05:09+05:30"), emrEncounter.getEncounterDatetime());
        assertEquals(encounter.getType().get(0).getTextSimple(), emrEncounter.getEncounterType().getName());
        assertNotNull(emrEncounter.getEncounterProviders());
        assertEquals("Bahmni", emrEncounter.getLocation().getName());

        assertNotNull(emrEncounter.getVisit());
        assertEquals("ad41fb41-a41a-4ad6-8835-2f59099acf5a", emrEncounter.getVisit().getUuid());
        assertEquals("50ab30be-98af-4dfd-bd04-5455937c443f", emrEncounter.getLocation().getUuid());
    }

    @Test
    public void shouldMapFhirConditions() throws Exception {
        executeDataSet("testDataSets/shrClientEncounterReverseSyncTestDS.xml");
        final AtomFeed encounterBundle = loadSampleFHIREncounter().getFeed();

        List<Resource> conditions = TestFhirFeedHelper.getResourceByType(encounterBundle, ResourceType.Condition);
        final Composition composition = FHIRFeedHelper.getComposition(encounterBundle);
        assertEquals(2, conditions.size());
        assertEquals("http://mci.com//api/default/patients/HIDA764177", ((Condition)conditions.get(0)).getSubject().getReferenceSimple());

        org.openmrs.Patient emrPatient = patientService.getPatient(1);
        final Encounter encounter = FHIRFeedHelper.getEncounter(encounterBundle);
        final org.openmrs.Encounter emrEncounter = fhirEncounterMapper.map(encounter, composition.getDateSimple().toString(), emrPatient, encounterBundle);

        final Set<Obs> visitObs = emrEncounter.getObsAtTopLevel(false);
        assertEquals(2, visitObs.size());
        Obs firstObs = visitObs.iterator().next();
        assertNotNull(firstObs.getGroupMembers());
        assertNotNull(firstObs.getPerson());
        assertNotNull(firstObs.getEncounter());
    }

    @Test
    public void shouldMapAnEncounterWhichDoesNotHaveServiceProvider() throws Exception {
        executeDataSet("testDataSets/shrClientEncounterReverseSyncTestDS.xml");
        ParserBase.ResourceOrFeed resourceOrFeed = new MapperTestHelper().loadSampleFHIREncounter("classpath:encounterBundles/testFHIREncounterWithExternalProvider.xml", springContext);
        AtomFeed encounterBundle = resourceOrFeed.getFeed();
        Composition composition = FHIRFeedHelper.getComposition(encounterBundle);
        Encounter encounter = FHIRFeedHelper.getEncounter(encounterBundle);
        encounter.setServiceProvider(null);
        org.openmrs.Patient emrPatient = patientService.getPatient(1);
        org.openmrs.Encounter emrEncounter = fhirEncounterMapper.map(encounter, composition.getDateSimple().toString(), emrPatient, encounterBundle);

        assertNotNull(emrEncounter);
        assertEquals(emrPatient, emrEncounter.getPatient());
        assertEquals(DateUtil.parseDate("2014-07-10T16:05:09+05:30"), emrEncounter.getEncounterDatetime());
        assertEquals(encounter.getType().get(0).getTextSimple(), emrEncounter.getEncounterType().getName());
        assertNotNull(emrEncounter.getEncounterProviders());

        assertNotNull(emrEncounter.getVisit());
        assertNotNull(emrEncounter.getLocation());
        assertEquals("ad41fb41-a41a-4ad6-8835-2f59099acf5a", emrEncounter.getVisit().getUuid());
        assertEquals("50ab30be-98af-4dfd-bd04-5455937c443f", emrEncounter.getLocation().getUuid());
    }
}
