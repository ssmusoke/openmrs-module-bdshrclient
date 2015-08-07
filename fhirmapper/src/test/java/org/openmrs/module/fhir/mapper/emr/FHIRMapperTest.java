package org.openmrs.module.fhir.mapper.emr;

import org.hl7.fhir.instance.model.AtomFeed;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.PatientService;
import org.openmrs.module.fhir.MapperTestHelper;
import org.openmrs.module.fhir.utils.GlobalPropertyLookUpService;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Set;

import static org.junit.Assert.assertEquals;

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
    
    @Autowired
    private GlobalPropertyLookUpService globalPropertyLookUpService;

    @Test
    public void shouldMapObservations() throws Exception {
        executeDataSet("testDataSets/shrClientObservationsTestDs.xml");
        AtomFeed encounterBundle = new MapperTestHelper().loadSampleFHIREncounter("classpath:encounterBundles/testFHIRObservation.xml", springContext).getFeed();

        Patient patient = patientService.getPatient(1);

        Encounter encounter = fhirMapper.map(patient, encounterBundle);
        assertEquals(4, encounter.getAllObs().size());

        Set<Obs> topLevelObs = encounter.getObsAtTopLevel(false);
        assertEquals(1, topLevelObs.size());

        Obs vitalsObs = topLevelObs.iterator().next();
        assertEquals(conceptService.getConcept(301), vitalsObs.getConcept());
    }
}