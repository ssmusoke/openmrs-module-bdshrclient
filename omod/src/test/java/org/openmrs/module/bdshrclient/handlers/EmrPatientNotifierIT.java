package org.openmrs.module.bdshrclient.handlers;

import org.junit.Before;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class EmrPatientNotifierIT extends BaseModuleWebContextSensitiveTest {

    @Before
    public void setup() throws Exception {
        executeDataSet("bdShrClientTestDS.xml");
    }

    //@Test
    public void shouldProcessInternalPatientsEvent() {
        EmrPatientNotifier emrPatientNotifier = new EmrPatientNotifier();
        emrPatientNotifier.process();
    }

   
}
