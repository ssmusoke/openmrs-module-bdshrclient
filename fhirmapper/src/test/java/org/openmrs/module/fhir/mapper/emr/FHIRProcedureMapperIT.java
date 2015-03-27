package org.openmrs.module.fhir.mapper.emr;

import org.hl7.fhir.instance.formats.ParserBase;
import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.ResourceType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.MapperTestHelper;
import org.openmrs.module.fhir.ObsHelper;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.openmrs.module.fhir.mapper.MRSProperties.*;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class FHIRProcedureMapperIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    FHIRProcedureMapper fhirProcedureMapper;
    @Autowired
    ConceptService conceptService;
    @Autowired
    private ApplicationContext springContext;
    private Resource resource;
    private AtomFeed feed;
    private ObsHelper obsHelper;

    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/procedureDS.xml");

        ParserBase.ResourceOrFeed resourceOrFeed = new MapperTestHelper().loadSampleFHIREncounter("encounterBundles/encounterWithProcedure.xml", springContext);
        feed = resourceOrFeed.getFeed();
        resource = FHIRFeedHelper.identifyResource(feed.getEntryList(), ResourceType.Procedure);
        obsHelper = new ObsHelper();
    }

    @Test
    public void shouldHandleResourceTypeOfProcedure() {
        fhirProcedureMapper.canHandle(resource);
    }

    @Test
    public void shouldMapStartDate() throws Exception {
        Obs proceduresObs = mapProceduresObs();
        Obs startDate = obsHelper.findMemberObsByConceptName(proceduresObs, MRS_CONCEPT_PROCEDURE_START_DATE);
        DateTime dateTime = new DateTime(DateTimeZone.forTimeZone(TimeZone.getTimeZone("IST")));
        Date expectedDate = dateTime.withDate(2014, 12, 31).toDateMidnight().toDate();
        assertEquals(expectedDate, startDate.getValueDatetime());
    }

    @Test
    public void shouldMapEndDate() throws Exception {
        Obs proceduresObs = mapProceduresObs();
        Obs endDate = obsHelper.findMemberObsByConceptName(proceduresObs, MRS_CONCEPT_PROCEDURE_END_DATE);
        DateTime dateTime = new DateTime(DateTimeZone.forTimeZone(TimeZone.getTimeZone("IST")));
        Date expectedDate = dateTime.withDate(2015, 1, 13).toDateMidnight().toDate();
        assertEquals(expectedDate, endDate.getValueDatetime());

    }

    @Test
    public void shouldMapOutComeText() throws Exception {

        Obs proceduresObs = mapProceduresObs();
        Obs outcomeObs = obsHelper.findMemberObsByConceptName(proceduresObs, MRS_CONCEPT_PROCEDURE_OUTCOME);
        assertEquals("Little johny wants to play", outcomeObs.getValueText());

    }

    @Test
    public void shouldMapFollowUpText() throws Exception {

        Obs proceduresObs = mapProceduresObs();
        Obs followUpObs = obsHelper.findMemberObsByConceptName(proceduresObs, MRS_CONCEPT_PROCEDURE_FOLLOW_UP);
        assertEquals("Come again another day", followUpObs.getValueText());

    }

    @Test
    public void shouldMapProcedureType() throws Exception {

        Obs procedureObs= mapProceduresObs();
        Obs procedureTypeObs= obsHelper.findMemberObsByConceptName(procedureObs, MRS_CONCEPT_PROCEDURE_TYPE);
        Concept valueCoded = procedureTypeObs.getValueCoded();
        int procedureType= 601;
        assertEquals(valueCoded, conceptService.getConcept(procedureType));
    }

    @Test
    public void shouldMapDiagnosticReport() throws  Exception {

        Obs procedureObs= mapProceduresObs();
        Obs diagnosticStudyObs= obsHelper.findMemberObsByConceptName(procedureObs, MRS_CONCEPT_PROCEDURE_DIAGNOSTIC_STUDY);

        Obs diagnosticTestName = obsHelper.findMemberObsByConceptName(diagnosticStudyObs, MRS_CONCEPT_PROCEDURE_DIAGNOSTIC_TEST);
        Concept diagnosticTestCode = diagnosticTestName.getValueCoded();
        int testAConceptId = 602;
        assertEquals(diagnosticTestCode, conceptService.getConcept(testAConceptId));

        Obs result = obsHelper.findMemberObsByConceptName(diagnosticStudyObs, MRS_CONCEPT_PROCEDURE_DIAGNOSTIC_RESULT);
        assertEquals("positive", result.getValueText());

        Obs diagnosis = obsHelper.findMemberObsByConceptName(diagnosticStudyObs, MRS_CONCEPT_PROCEDURE_DIAGNOSIS);
        Concept diagnosisConcept = diagnosis.getValueCoded();
        int diagnosisConceptId = 603;
        assertEquals(diagnosisConcept, conceptService.getConcept(diagnosisConceptId));
    }

    private Obs mapProceduresObs() {
        Encounter mrsEncounter = new Encounter();
        fhirProcedureMapper.map(feed, resource, null, mrsEncounter, new HashMap<String, List<String>>());

        Set<Obs> allObs = mrsEncounter.getAllObs();
        assertEquals(1, allObs.size());
        Obs obs = allObs.iterator().next();
        assertEquals(MRS_CONCEPT_PROCEDURES, obs.getConcept().getName().getName());

        return obs;
    }

}