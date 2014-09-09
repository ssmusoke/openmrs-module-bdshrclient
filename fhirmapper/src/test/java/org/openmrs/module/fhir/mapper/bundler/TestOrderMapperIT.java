package org.openmrs.module.fhir.mapper.bundler;

import org.hl7.fhir.instance.model.AtomFeed;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.api.EncounterService;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class TestOrderMapperIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    TestOrderMapper testOrderMapper;

    @Autowired
    EncounterService encounterService;

    @Test
    public void shouldMapTestOrder() throws Exception {
        //TODO : add care setting ot laborder.
        executeDataSet("labOrder.xml");
        Encounter encounter = encounterService.getEncounter(36);
        org.hl7.fhir.instance.model.Encounter fhirEncounter = new org.hl7.fhir.instance.model.Encounter();
        EmrResource mappedResource;
        assertEquals(2, encounter.getOrders().size());
        for (Order order : encounter.getOrders()) {
            mappedResource = testOrderMapper.map(order, fhirEncounter, new AtomFeed());
            assertNotNull(mappedResource);
        }
    }
}