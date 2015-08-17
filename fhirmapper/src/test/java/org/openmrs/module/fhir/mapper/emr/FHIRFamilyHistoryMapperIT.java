package org.openmrs.module.fhir.mapper.emr;

import org.hl7.fhir.instance.formats.ParserBase;
import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.FamilyHistory;
import org.hl7.fhir.instance.model.ResourceType;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.MapperTestHelper;
import org.openmrs.module.fhir.utils.DateUtil;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.openmrs.module.fhir.mapper.MRSProperties.*;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FHIRFamilyHistoryMapperIT extends BaseModuleWebContextSensitiveTest {
    @Autowired
    private ApplicationContext springContext;

    @Autowired
    private FHIRFamilyHistoryMapper familyHistoryMapper;

    @Autowired
    private ConceptService conceptService;

    @Test
    public void shouldMapFamilyHistoryResource() throws Exception {
        executeDataSet("testDataSets/shrClientFamilyHistoryTestDS.xml");
        ParserBase.ResourceOrFeed resourceOrFeed = new MapperTestHelper().loadSampleFHIREncounter("encounterBundles/encounterWithFamilyHistory.xml", springContext);
        AtomFeed feed = resourceOrFeed.getFeed();
        FamilyHistory familyHistory = (FamilyHistory) FHIRFeedHelper.identifyResource(feed.getEntryList(), ResourceType.FamilyHistory);
        Encounter newEmrEncounter = new Encounter();
        familyHistoryMapper.map(feed, familyHistory, new Patient(), newEmrEncounter, new HashMap<String, List<String>>());

        assertEquals(1, newEmrEncounter.getObsAtTopLevel(false).size());
        Obs familyHistoryObs = newEmrEncounter.getObsAtTopLevel(false).iterator().next();
        assertEquals(conceptService.getConceptByName(MRS_CONCEPT_NAME_FAMILY_HISTORY), familyHistoryObs.getConcept());
        assertEquals(1, familyHistoryObs.getGroupMembers().size());

        Obs personObs = familyHistoryObs.getGroupMembers().iterator().next();
        assertEquals(conceptService.getConceptByName(MRS_CONCEPT_NAME_PERSON), personObs.getConcept());
        assertEquals(3, personObs.getGroupMembers().size());

        assertEquals(3, personObs.getGroupMembers().size());
        Obs relationshipTypeObs = identifyObsFromGlobalPropertyValue(TR_VALUESET_RELATIONSHIP_TYPE, personObs.getGroupMembers());
        assertEquals(conceptService.getConcept(400), relationshipTypeObs.getValueCoded());

        Obs bornOnObs = identifyObsFromGlobalPropertyValue(MRS_CONCEPT_NAME_BORN_ON, personObs.getGroupMembers());
        assertEquals(DateUtil.parseDate("2074-12-01T00:00:00+05:30"), bornOnObs.getValueDate());

        Obs relationshipConditionObs = identifyObsFromGlobalPropertyValue(MRS_CONCEPT_NAME_RELATIONSHIP_CONDITION, personObs.getGroupMembers());
        assertEquals(3, relationshipConditionObs.getGroupMembers().size());

        Obs relationshipNotesObs = identifyObsFromGlobalPropertyValue(MRS_CONCEPT_NAME_RELATIONSHIP_NOTES, relationshipConditionObs.getGroupMembers());
        assertEquals("Some notes", relationshipNotesObs.getValueText());

        Obs relationshipDiagnosisObs = identifyObsFromGlobalPropertyValue(MRS_CONCEPT_NAME_RELATIONSHIP_DIAGNOSIS, relationshipConditionObs.getGroupMembers());
        assertEquals(conceptService.getConcept(301), relationshipDiagnosisObs.getValueCoded());

        Obs onsetAgeObs = identifyObsFromGlobalPropertyValue(MRS_CONCEPT_NAME_ONSET_AGE, relationshipConditionObs.getGroupMembers());
        assertTrue(5 == onsetAgeObs.getValueNumeric());
    }

    private Obs identifyObsFromGlobalPropertyValue(String conceptName, Set<Obs> groupMembers) {
        for (Obs groupMember : groupMembers) {
            if(groupMember.getConcept().getName().getName().equals(conceptName)) {
                return groupMember;
            }
        }
        return null;
    }
}