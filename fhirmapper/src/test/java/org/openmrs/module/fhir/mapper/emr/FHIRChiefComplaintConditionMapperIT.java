package org.openmrs.module.fhir.mapper.emr;

import static org.junit.Assert.assertEquals;
import org.hl7.fhir.instance.formats.JsonParser;
import org.hl7.fhir.instance.formats.ParserBase;
import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.Condition;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Set;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class FHIRChiefComplaintConditionMapperIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private ApplicationContext springContext;

    @Autowired
    private ConceptService conceptService;

    @Autowired
    private FHIRChiefComplaintConditionMapper fhirChiefComplaintConditionMapper;

    public ParserBase.ResourceOrFeed loadSampleFHIREncounter() throws Exception {
        Resource resource = springContext.getResource("classpath:testFHIREncounter.json");
        ParserBase.ResourceOrFeed parsedResource =
                new JsonParser().parseGeneral(resource.getInputStream());
        return parsedResource;
    }

    @Before
    public void setUp() throws Exception {
        executeDataSet("shrChiefComplaintReverseSyncTestDS.xml");
    }

    @Test
    public void shouldMapFHIRComplaint() throws Exception {
        final AtomFeed bundle = loadSampleFHIREncounter().getFeed();
        final List<Condition> conditions = FHIRFeedHelper.getConditions(bundle);
        Patient emrPatient = new Patient();
        Encounter emrEncounter = new Encounter();
        emrEncounter.setPatient(emrPatient);
        for (Condition condition : conditions) {
            if (fhirChiefComplaintConditionMapper.handles(condition)) {
                fhirChiefComplaintConditionMapper.map(condition, emrPatient, emrEncounter);
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
            } else if (groupMember.getConcept().getName().getName().equalsIgnoreCase("Chief Complaint Duration")) {
                assertEquals(120, groupMember.getValueNumeric(), 0);
            }
        }
    }
}
