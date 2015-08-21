package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import ca.uhn.fhir.model.primitive.DecimalDt;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Obs;
import org.openmrs.api.ObsService;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.openmrs.module.fhir.MapperTestHelper.getSystemProperties;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ObservationMapperIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    ObsService obsService;

    @Autowired
    ObservationMapper observationMapper;

    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/shrClientObservationsTestDs.xml");
    }

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }

    @Test
    public void shouldMapOMRSObsToFhirObservation() throws Exception {
        Obs vitalsObs = obsService.getObs(11);
        assertTrue(observationMapper.canHandle(vitalsObs));

        List<FHIRResource> FHIRResources = observationMapper.map(vitalsObs, new Encounter(), getSystemProperties("1"));
        assertEquals(4, FHIRResources.size());
        for (FHIRResource FHIRResource : FHIRResources) {
            Observation observation = (Observation) FHIRResource.getResource();
            assertTrue(isNotEmpty(observation.getCode().getCoding()));
            if (FHIRResource.getResourceName().equals("Vitals")) {
                assertVitalsObservation(observation);
            } else if (FHIRResource.getResourceName().equals("Blood Pressure")) {
                assertBloodPressureObservation(observation);
            } else if (FHIRResource.getResourceName().equals("Diastolic")) {
                assertDiastolicObservation(observation);
            } else if (FHIRResource.getResourceName().equals("Pulse")) {
                assertPulseObservation(observation);
            }
        }
    }

    private void assertPulseObservation(Observation observation) {
        List<CodingDt> coding = observation.getCode().getCoding();
        assertEquals(2, coding.size());
        int flag = 0;
        for (CodingDt code : coding) {
            if (assertCoding(code, "103", "/concepts/103")) flag ++;
            if(assertCoding(code, "M54.418965", "referenceterms/201")) flag ++;
        }
        assertEquals(2, flag);
        assertTrue(133 == ((DecimalDt) observation.getValue()).getValue().doubleValue());
        assertTrue(isEmpty(observation.getRelated()));
    }

    private void assertDiastolicObservation(Observation observation) {
        List<CodingDt> coding = observation.getCode().getCoding();
        assertEquals(1, coding.size());
        assertTrue(assertCoding(coding.get(0), "105", "/concepts/105"));
        assertTrue(120 == ((DecimalDt) observation.getValue()).getValue().doubleValue());
        assertTrue(isEmpty(observation.getRelated()));
    }

    private void assertBloodPressureObservation(Observation observation) {
        List<CodingDt> coding = observation.getCode().getCoding();
        assertEquals(1, coding.size());
        assertTrue(null == coding.get(0).getCode());
        assertTrue(null == coding.get(0).getSystem());
        assertEquals("Blood Pressure", coding.get(0).getDisplay());
        assertEquals(1, observation.getRelated().size());
    }

    private void assertVitalsObservation(Observation observation) {
        List<CodingDt> coding = observation.getCode().getCoding();
        assertEquals(1, coding.size());
        assertTrue(assertCoding(coding.get(0), "101", "/concepts/101"));
        assertEquals(2, observation.getRelated().size());
    }

    private boolean assertCoding(CodingDt code, String expectedCode, String expectedSystem) {
        return ((expectedCode.equals(code.getCode())) && (expectedSystem.equals(code.getSystem())));
    }
}