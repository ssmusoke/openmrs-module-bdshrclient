package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Immunization;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.MapperTestHelper;
import org.openmrs.module.fhir.ObsHelper;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Date;
import java.util.Set;
import java.util.TimeZone;

import static org.junit.Assert.*;
import static org.openmrs.module.fhir.mapper.MRSProperties.*;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FHIRImmunizationMapperIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private ApplicationContext springContext;

    @Autowired
    FHIRImmunizationMapper mapper;

    @Autowired
    private ConceptService conceptService;

    private IResource resource;
    private Bundle bundle;
    private ObsHelper obsHelper;


    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/immunizationDS.xml");
        bundle = (Bundle) new MapperTestHelper().loadSampleFHIREncounter("encounterBundles/dstu2/encounterWithImmunization.xml", springContext);
        resource = FHIRFeedHelper.identifyResource(bundle.getEntry(), new Immunization().getResourceName());
        obsHelper = new ObsHelper();
    }

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }

    @Test
    public void shouldHandleResourceOfTypeImmnunization() throws Exception {
        assertTrue(mapper.canHandle(resource));
    }

    @Test
    public void shouldMapDateOfVaccination() throws Exception {
        Obs obs = mapImmunizationIncidentObs();
        DateTime dateTime = new DateTime(DateTimeZone.forTimeZone(TimeZone.getTimeZone("IST")));
        Date expectedDate = dateTime.withDate(2015, 8, 17).toDateMidnight().toDate();

        Obs vaccineDateObs = obsHelper.findMemberObsByConceptName(obs, MRS_CONCEPT_VACCINATION_DATE);
        assertEquals(expectedDate, vaccineDateObs.getValueDate());
    }

    @Test
    public void shouldMapVaccinationReported() throws Exception {
        Obs obs = mapImmunizationIncidentObs();

        Obs vaccinationReported = obsHelper.findMemberObsByConceptName(obs, MRS_CONCEPT_VACCINATION_REPORTED);
        assertTrue(vaccinationReported.getValueAsBoolean());
    }

    @Test
    public void shouldMapImmunizationStatus() throws Exception {
        Obs obs = mapImmunizationIncidentObs();

        Obs vaccinationStatus = obsHelper.findMemberObsByConceptName(obs, TR_VALUESET_IMMUNIZATION_STATUS);
        assertEquals(conceptService.getConcept(604), vaccinationStatus.getValueCoded());
    }

    @Test
    public void shouldMapRefusedIndicator() throws Exception {
        Obs obs = mapImmunizationIncidentObs();
        Obs refusedIndicator = obsHelper.findMemberObsByConceptName(obs, MRS_CONCEPT_VACCINATION_REFUSED);
        assertFalse(refusedIndicator.getValueAsBoolean());

    }

    @Test
    public void shouldMapQuantityUnits() throws Exception {
        Obs obs = mapImmunizationIncidentObs();
        Obs quantityUnits = obsHelper.findMemberObsByConceptName(obs, TR_VALUESET_QUANTITY_UNITS);

        int mgConceptCode = 50;
        assertEquals(conceptService.getConcept(mgConceptCode), quantityUnits.getValueCoded());
    }

    @Test
    public void shouldMapDosage() throws Exception {
        Obs obs = mapImmunizationIncidentObs();
        Obs dosage = obsHelper.findMemberObsByConceptName(obs, MRS_CONCEPT_DOSAGE);

        assertTrue(dosage.getValueNumeric() == 100.0);
    }

    @Test
    public void shouldMapVaccineType() throws Exception {
        Obs obs = mapImmunizationIncidentObs();
        Obs vaccineType = obsHelper.findMemberObsByConceptName(obs, MRS_CONCEPT_VACCINE);

        int paracetemol500 = 550;
        assertEquals(vaccineType.getValueCoded(), conceptService.getConcept(paracetemol500));


    }

    @Test
    public void shouldMapImmunizationReason() throws Exception {
        Obs obs = mapImmunizationIncidentObs();
        Obs immunizationReason = obsHelper.findMemberObsByConceptName(obs, TR_VALUESET_IMMUNIZATION_REASON);

        int travelVaccination = 501;
        assertEquals(immunizationReason.getValueCoded(), conceptService.getConcept(travelVaccination));
    }

    @Test
    public void shouldMapImmunizationRefusalReason() throws Exception {
        Obs obs = mapImmunizationIncidentObs();
        Obs immunizationReason = obsHelper.findMemberObsByConceptName(obs, TR_VALUESET_IMMUNIZATION_REFUSAL_REASON);

        int patientObjection = 502;
        assertEquals(immunizationReason.getValueCoded(), conceptService.getConcept(patientObjection));
    }

    @Test
    public void shouldMapRoute() throws Exception {
        Obs obs = mapImmunizationIncidentObs();
        Obs route = obsHelper.findMemberObsByConceptName(obs, TR_VALUESET_ROUTE_OF_ADMINSTRATION);

        int oral = 503;
        assertEquals(route.getValueCoded(), conceptService.getConcept(oral));
    }

    private Obs mapImmunizationIncidentObs() {
        Encounter mrsEncounter = new Encounter();
        mapper.map(bundle, resource, null, mrsEncounter);

        Set<Obs> allObs = mrsEncounter.getAllObs();
        assertEquals(1, allObs.size());
        Obs obs = allObs.iterator().next();
        assertEquals(MRS_CONCEPT_IMMUNIZATION_INCIDENT_TEMPLATE, obs.getConcept().getName().getName());

        return obs;
    }
}