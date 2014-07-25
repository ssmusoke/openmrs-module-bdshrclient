package org.openmrs.module.fhir.mapper.emr;

import org.hl7.fhir.instance.formats.JsonParser;
import org.hl7.fhir.instance.formats.ParserBase;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class FHIRChiefComplaintConditionMapperIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private ApplicationContext springContext;

    public ParserBase.ResourceOrFeed loadSampleFHIREncounter() throws Exception {
        Resource resource = springContext.getResource("classpath:testFHIREncounter.json");
        final ParserBase.ResourceOrFeed parsedResource =
                new JsonParser().parseGeneral(resource.getInputStream());
        return parsedResource;
    }

    @Before
    public void setUp() throws Exception {
        executeDataSet("shrChiefComplaintReverseSyncTestDS.xml");
    }

    @Test
    public void testName() throws Exception {


    }
}
