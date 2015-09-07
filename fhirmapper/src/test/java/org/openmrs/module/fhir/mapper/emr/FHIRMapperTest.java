package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.dstu2.resource.Bundle;
import org.junit.After;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.PatientService;
import org.openmrs.module.fhir.MapperTestHelper;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FHIRMapperTest extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private ApplicationContext springContext;

    @Autowired
    private FHIRMapper fhirMapper;

    @Autowired
    private PatientService patientService;

    @Autowired
    private ConceptService conceptService;

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }


    @Test
    public void shouldMapObservations() throws Exception {
        executeDataSet("testDataSets/shrClientObservationsTestDs.xml");
        Bundle encounterBundle = (Bundle) new MapperTestHelper().loadSampleFHIREncounter("encounterBundles/dstu2/encounterWithObservations.xml", springContext);

        Patient patient = patientService.getPatient(1);

        Encounter encounter = fhirMapper.map(patient, encounterBundle);
        assertEquals(4, encounter.getAllObs().size());

        Set<Obs> topLevelObs = encounter.getObsAtTopLevel(false);
        assertEquals(1, topLevelObs.size());

        Obs vitalsObs = topLevelObs.iterator().next();
        assertEquals(conceptService.getConcept(301), vitalsObs.getConcept());

        assertEquals(2, vitalsObs.getGroupMembers().size());

        Obs pulseObs = identifyObsByConcept(vitalsObs.getGroupMembers(), conceptService.getConcept(303));
        assertTrue(75 == pulseObs.getValueNumeric());
        Concept bpConcept = conceptService.getConcept(302);
        Obs bpObs = identifyObsByConcept(vitalsObs.getGroupMembers(), bpConcept);
        assertEquals(1, bpObs.getGroupMembers().size());
        Obs diastolicObs = identifyObsByConcept(bpObs.getGroupMembers(), conceptService.getConcept(305));
        assertTrue(70 == diastolicObs.getValueNumeric());

    }

    private Obs identifyObsByConcept(Set<Obs> obses, Concept concept) {
        for (Obs obs : obses) {
            if(obs.getConcept().equals(concept)) {
                return obs;
            }
        }
        return null;
    }
}