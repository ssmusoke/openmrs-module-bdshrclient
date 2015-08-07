package org.openmrs.module.fhir.mapper.emr;

import org.hl7.fhir.instance.formats.ParserBase;
import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.ResourceType;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.MapperTestHelper;
import org.openmrs.module.fhir.TestFhirFeedHelper;
import org.openmrs.module.fhir.utils.GlobalPropertyLookUpService;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FHIRChiefComplaintConditionMapperIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private ApplicationContext springContext;

    @Autowired
    private ConceptService conceptService;

    @Autowired
    private FHIRChiefComplaintConditionMapper fhirChiefComplaintConditionMapper;

    @Autowired
    private GlobalPropertyLookUpService globalPropertyLookUpService;

    public ParserBase.ResourceOrFeed loadSampleFHIREncounter(String filePath) throws Exception {
        return new MapperTestHelper().loadSampleFHIREncounter(filePath, springContext);
    }

    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/shrChiefComplaintReverseSyncTestDS.xml");
    }

    @Test
    public void shouldMapFHIRComplaint() throws Exception {
        final AtomFeed bundle = loadSampleFHIREncounter("classpath:encounterBundles/testFHIREncounter.xml").getFeed();
        final List<Resource> conditions = TestFhirFeedHelper.getResourceByType(bundle, ResourceType.Condition);
        Patient emrPatient = new Patient();
        Encounter emrEncounter = new Encounter();
        emrEncounter.setPatient(emrPatient);
        for (Resource condition : conditions) {
            if (fhirChiefComplaintConditionMapper.canHandle(condition)) {
                fhirChiefComplaintConditionMapper.map(bundle, condition, emrPatient, emrEncounter, new HashMap<String, List<String>>());
            }
        }
        final Set<Obs> visitObs = emrEncounter.getObsAtTopLevel(false);
        assertEquals(1, visitObs.size());
        Obs historyAndExaminationObs = visitObs.iterator().next();
        assertTrue(historyAndExaminationObs.getConcept().getName().getName().equalsIgnoreCase("History and Examination"));
        Set<Obs> historyAndExaminationMembers = historyAndExaminationObs.getGroupMembers();
        assertEquals(1, historyAndExaminationMembers.size());
        final Obs chiefComplaintDataObs = historyAndExaminationMembers.iterator().next();
        assertTrue(chiefComplaintDataObs.getConcept().getName().getName().equalsIgnoreCase("Chief Complaint Data"));
        final Set<Obs> chiefComplaintDataMembers = chiefComplaintDataObs.getGroupMembers();
        assertEquals(2, chiefComplaintDataMembers.size());
        for (Obs groupMember : chiefComplaintDataMembers) {
            if (groupMember.getConcept().getName().getName().equalsIgnoreCase("Chief Complaint")) {
                final Concept valueCoded = groupMember.getValueCoded();
                assertNotNull(valueCoded);
                assertEquals(valueCoded, conceptService.getConceptByName("Pain in left leg"));
            } else if (groupMember.getConcept().getName().getName().equalsIgnoreCase("Non-Coded Chief Complaint")) {
                String valueText = groupMember.getValueText();
                assertNotNull(valueText);
            } else if (groupMember.getConcept().getName().getName().equalsIgnoreCase("Chief Complaint Duration")) {
                assertEquals(120, groupMember.getValueNumeric(), 0);
            }
        }
    }

    @Test
    public void shouldNotCreateDurationObsIfDurationNotGiven() throws Exception {
        final AtomFeed bundle = loadSampleFHIREncounter("classpath:encounterBundles/chiefComplaintWithoutDuration.xml").getFeed();
        final List<Resource> conditions = TestFhirFeedHelper.getResourceByType(bundle, ResourceType.Condition);
        Patient emrPatient = new Patient();
        Encounter emrEncounter = new Encounter();
        emrEncounter.setPatient(emrPatient);
        for (Resource condition : conditions) {
            if (fhirChiefComplaintConditionMapper.canHandle(condition)) {
                fhirChiefComplaintConditionMapper.map(bundle, condition, emrPatient, emrEncounter, new HashMap<String, List<String>>());
            }
        }
        final Set<Obs> observations = emrEncounter.getAllObs();
        Concept durationConcept = conceptService.getConceptByName("Chief Complaint Duration");
        assertNull(identifyObsByConcept(observations, durationConcept));
    }

    @Test
    public void shouldCreateOneHistoryAndExaminationObsForAllComplaints() throws Exception {
        final AtomFeed bundle = loadSampleFHIREncounter("classpath:encounterBundles/encounterWithMultipleChiefComplaints.xml").getFeed();
        final List<Resource> conditions = TestFhirFeedHelper.getResourceByType(bundle, ResourceType.Condition);
        Patient emrPatient = new Patient();
        Encounter emrEncounter = new Encounter();
        emrEncounter.setPatient(emrPatient);
        for (Resource condition : conditions) {
            if (fhirChiefComplaintConditionMapper.canHandle(condition)) {
                fhirChiefComplaintConditionMapper.map(bundle, condition, emrPatient, emrEncounter, new HashMap<String, List<String>>());
            }
        }
        final Set<Obs> topLevelObs = emrEncounter.getObsAtTopLevel(false);
        assertEquals(1, topLevelObs.size());
        assertNotNull(identifyObsByConcept(topLevelObs, conceptService.getConceptByName("History and Examination")));
    }

    private Obs identifyObsByConcept(Set<Obs> observations, Concept concept) {
        for (Obs observation : observations) {
            if(observation.getConcept().equals(concept)) {
                return observation;
            }
        }
        return null;
    }
}
