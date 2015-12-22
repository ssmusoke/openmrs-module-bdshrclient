package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.dstu2.resource.Bundle;
import org.junit.After;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.api.EncounterService;
import org.openmrs.module.fhir.MapperTestHelper;
import org.openmrs.module.fhir.mapper.model.ShrEncounterBundle;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.Assert.assertEquals;
import static org.openmrs.module.fhir.MapperTestHelper.getSystemProperties;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FHIRSubResourceMapperIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private ApplicationContext springContext;

    @Autowired
    private FHIRSubResourceMapper fhirSubResourceMapper;

    @Autowired
    private EncounterService encounterService;

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }

    @Test
    public void shouldAddNewOrdersToEncounter() throws Exception {
        executeDataSet("testDataSets/labOrderDS.xml");
        Encounter existingEncounter = encounterService.getEncounter(42);
        assertEquals(1, existingEncounter.getOrders().size());
        Bundle bundle = loadSampleFHIREncounter("encounterBundles/dstu2/encounterWithUpdatedDiagnosticOrder.xml");
        fhirSubResourceMapper.map(existingEncounter, new ShrEncounterBundle(bundle, "HIDA764177", "SHR-ENC-1"), getSystemProperties("1"));
        assertEquals(2, existingEncounter.getOrders().size());
    }

    private Bundle loadSampleFHIREncounter(String filePath) throws Exception {
        return (Bundle) new MapperTestHelper().loadSampleFHIREncounter(filePath, springContext);
    }
}