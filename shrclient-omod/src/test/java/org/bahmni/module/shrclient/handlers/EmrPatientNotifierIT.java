package org.bahmni.module.shrclient.handlers;

import org.junit.Before;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class EmrPatientNotifierIT extends BaseModuleWebContextSensitiveTest {

    @Before
    public void setup() throws Exception {
        executeDataSet("shrClientTestDS.xml");
    }

    //@Test
    public void shouldProcessInternalPatientsEvent() {
        ShrNotifier shrNotifier = new ShrNotifier();
        shrNotifier.processPatient();
    }

   
}
