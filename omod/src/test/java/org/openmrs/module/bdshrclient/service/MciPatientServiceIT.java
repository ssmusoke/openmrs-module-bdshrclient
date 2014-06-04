package org.openmrs.module.bdshrclient.service;


import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.bdshrclient.service.impl.MciPatientServiceImpl;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class MciPatientServiceIT extends BaseModuleWebContextSensitiveTest {

    @Before
    public void setup() throws Exception {
        //executeDataSet("bdShrClientPatientTestDS.xml");
    }

    @Test
    public void dummy() {
//        MciPatientServiceImpl mciPatientService = new MciPatientServiceImpl();
//        String identifier = mciPatientService.generateIdentifier();
//        System.out.println("identifier:" + identifier);
        IdentifierSourceService service = Context.getService(IdentifierSourceService.class);
        System.out.println("idgen service:" + service);
    }


}
