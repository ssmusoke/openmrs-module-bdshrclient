package org.openmrs.module.fhir.mapper.bundler;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.api.EncounterService;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import static junit.framework.Assert.assertEquals;
import static org.openmrs.module.fhir.MapperTestHelper.getSystemProperties;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class EncounterMapperIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    EncounterService encounterService;

    @Autowired
    EncounterMapper encounterMapper;

    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/encounterServiceProviderDS.xml");
    }

    @Test
    public void shouldTakeTheLocationIdAsServiceProviderIdWhenLocationIsTaggedAsADGHSFacility() throws Exception {
        Encounter savedEncounter = encounterService.getEncounter(36);

        org.hl7.fhir.instance.model.Encounter fhirEncounter = encounterMapper.map(savedEncounter, getSystemProperties("1"));

        assertEquals("http://hrmtest.dghs.gov.bd/api/1.0/facilities/1300012.json",fhirEncounter.getServiceProvider().getReference().getValue());
    }

    @Test
    public void shouldTakeConfiguredFacilityIdAsServiceProviderIdWhenLocationIsNotTaggedAsADGHSFacility() throws Exception {
        Encounter savedEncounter = encounterService.getEncounter(37);

        org.hl7.fhir.instance.model.Encounter fhirEncounter = encounterMapper.map(savedEncounter, getSystemProperties("1"));

        assertEquals("http://hrmtest.dghs.gov.bd/api/1.0/facilities/1.json",fhirEncounter.getServiceProvider().getReference().getValue());

    }
}