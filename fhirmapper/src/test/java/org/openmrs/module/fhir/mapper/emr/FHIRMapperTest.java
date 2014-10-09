package org.openmrs.module.fhir.mapper.emr;

import org.hl7.fhir.instance.model.AtomFeed;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.PatientService;
import org.openmrs.module.fhir.TestHelper;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.Set;

import static org.junit.Assert.assertEquals;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class FHIRMapperTest extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private ApplicationContext springContext;

    @Autowired
    private FHIRMapper fhirMapper;

    @Autowired
    private PatientService patientService;

    @Autowired
    private ConceptService conceptService;

    @Test
    public void shouldMapObservations() throws Exception {
        executeDataSet("shrClientObservationsTestDs.xml");
        AtomFeed encounterBundle = new TestHelper().loadSampleFHIREncounter("classpath:testFHIRObservation.xml", springContext).getFeed();

        Patient patient = patientService.getPatient(1);

        Encounter encounter = fhirMapper.map(patient, encounterBundle);
        assertEquals(4, encounter.getAllObs().size());

        Set<Obs> topLevelObs = encounter.getObsAtTopLevel(false);
        assertEquals(1, topLevelObs.size());

        Obs vitalsObs = topLevelObs.iterator().next();
        assertEquals(conceptService.getConcept(301), vitalsObs.getConcept());
    }
}