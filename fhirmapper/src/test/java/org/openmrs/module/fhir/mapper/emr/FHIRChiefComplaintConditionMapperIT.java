package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Condition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.MapperTestHelper;
import org.openmrs.module.fhir.TestFhirFeedHelper;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.openmrs.module.fhir.MRSProperties.*;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FHIRChiefComplaintConditionMapperIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private ApplicationContext springContext;

    @Autowired
    private ConceptService conceptService;

    @Autowired
    private FHIRChiefComplaintConditionMapper fhirChiefComplaintConditionMapper;

    public Bundle loadSampleFHIREncounter(String filePath) throws Exception {
        return (Bundle) new MapperTestHelper().loadSampleFHIREncounter(filePath, springContext);
    }

    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/shrChiefComplaintReverseSyncTestDS.xml");
    }

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }


    @Test
    public void shouldMapFHIRComplaint() throws Exception {
        final Bundle bundle = loadSampleFHIREncounter("encounterBundles/dstu2/encounterWithChiefComplaints.xml");
        final List<IResource> conditions = TestFhirFeedHelper.getResourceByType(bundle, new Condition().getResourceName());
        Patient emrPatient = new Patient();
        Encounter emrEncounter = new Encounter();
        emrEncounter.setPatient(emrPatient);
        for (IResource condition : conditions) {
            if (fhirChiefComplaintConditionMapper.canHandle(condition)) {
                fhirChiefComplaintConditionMapper.map(bundle, condition, emrPatient, emrEncounter);
            }
        }
        final Set<Obs> visitObs = emrEncounter.getObsAtTopLevel(false);
        assertEquals(1, visitObs.size());
        Obs historyAndExaminationObs = visitObs.iterator().next();
        assertTrue(historyAndExaminationObs.getConcept().getName().getName().equalsIgnoreCase(MRS_CONCEPT_NAME_COMPLAINT_CONDITION_TEMPLATE));
        Set<Obs> historyAndExaminationMembers = historyAndExaminationObs.getGroupMembers();
        assertEquals(1, historyAndExaminationMembers.size());
        final Obs chiefComplaintDataObs = historyAndExaminationMembers.iterator().next();
        assertTrue(chiefComplaintDataObs.getConcept().getName().getName().equalsIgnoreCase(MRS_CONCEPT_NAME_CHIEF_COMPLAINT_DATA));
        final Set<Obs> chiefComplaintDataMembers = chiefComplaintDataObs.getGroupMembers();
        assertEquals(2, chiefComplaintDataMembers.size());
        for (Obs groupMember : chiefComplaintDataMembers) {
            if (groupMember.getConcept().getName().getName().equalsIgnoreCase(MRS_CONCEPT_NAME_CHIEF_COMPLAINT)) {
                final Concept valueCoded = groupMember.getValueCoded();
                assertNotNull(valueCoded);
                assertEquals(valueCoded, conceptService.getConceptByName("Pain in left leg"));
            } else if (groupMember.getConcept().getName().getName().equalsIgnoreCase(MRS_CONCEPT_NAME_NON_CODED_CHIEF_COMPLAINT)) {
                String valueText = groupMember.getValueText();
                assertNotNull(valueText);
            } else if (groupMember.getConcept().getName().getName().equalsIgnoreCase(MRS_CONCEPT_NAME_CHIEF_COMPLAINT_DURATION)) {
                assertEquals(120, groupMember.getValueNumeric(), 0);
            }
        }
    }

    @Test
    public void shouldNotCreateDurationObsIfDurationNotGiven() throws Exception {
        final Bundle bundle = loadSampleFHIREncounter("encounterBundles/dstu2/encounterWithChiefComplaintWithoutDuration.xml");
        final List<IResource> conditions = TestFhirFeedHelper.getResourceByType(bundle, new Condition().getResourceName());
        Patient emrPatient = new Patient();
        Encounter emrEncounter = new Encounter();
        emrEncounter.setPatient(emrPatient);
        for (IResource condition : conditions) {
            if (fhirChiefComplaintConditionMapper.canHandle(condition)) {
                fhirChiefComplaintConditionMapper.map(bundle, condition, emrPatient, emrEncounter);
            }
        }
        final Set<Obs> observations = emrEncounter.getAllObs();
        Concept durationConcept = conceptService.getConceptByName(MRS_CONCEPT_NAME_CHIEF_COMPLAINT_DURATION);
        assertNull(identifyObsByConcept(observations, durationConcept));
    }

    @Test
    public void shouldCreateOneHistoryAndExaminationObsForAllComplaints() throws Exception {
        final Bundle bundle = loadSampleFHIREncounter("classpath:encounterBundles/dstu2/encounterWithMultipleChiefComplaints.xml");
        final List<IResource> conditions = TestFhirFeedHelper.getResourceByType(bundle, new Condition().getResourceName());
        Patient emrPatient = new Patient();
        Encounter emrEncounter = new Encounter();
        emrEncounter.setPatient(emrPatient);
        for (IResource condition : conditions) {
            if (fhirChiefComplaintConditionMapper.canHandle(condition)) {
                fhirChiefComplaintConditionMapper.map(bundle, condition, emrPatient, emrEncounter);
            }
        }
        final Set<Obs> topLevelObs = emrEncounter.getObsAtTopLevel(false);
        assertEquals(1, topLevelObs.size());
        assertNotNull(identifyObsByConcept(topLevelObs, conceptService.getConceptByName(MRS_CONCEPT_NAME_COMPLAINT_CONDITION_TEMPLATE)));
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
