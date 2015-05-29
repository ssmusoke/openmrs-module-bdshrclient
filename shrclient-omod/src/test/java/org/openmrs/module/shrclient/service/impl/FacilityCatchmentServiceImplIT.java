package org.openmrs.module.shrclient.service.impl;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.shrclient.dao.FacilityCatchmentRepository;
import org.openmrs.module.shrclient.model.FacilityCatchment;
import org.openmrs.module.shrclient.service.FacilityCatchmentService;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertEquals;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class FacilityCatchmentServiceImplIT extends BaseModuleWebContextSensitiveTest {
    @Autowired
    FacilityCatchmentRepository facilityCatchmentRepository;

    FacilityCatchmentService facilityCatchmentService;

    @Before
    public void setUp() throws Exception {
        facilityCatchmentService = new FacilityCatchmentServiceImpl(facilityCatchmentRepository);
    }


    @Test
    public void shouldGetNoCatchmentsForFacilityWithoutCatchments() throws Exception {
        executeDataSet("testDataSets/facilityCatchmentsDS.xml");
        List<FacilityCatchment> facilityCatchments = facilityCatchmentService.getCatchmentsForFacility(2);
        assertEquals(0, facilityCatchments.size());
    }

    @Test
    public void shouldGetCatchmentsForFacility() throws Exception {
        executeDataSet("testDataSets/facilityCatchmentsDS.xml");
        List<FacilityCatchment> facilityCatchments = facilityCatchmentService.getCatchmentsForFacility(1);
        assertEquals(3, facilityCatchments.size());
    }

    @Test
    public void shouldGetFacilitiesForCatchment() throws Exception {
        executeDataSet("testDataSets/facilityCatchmentsDS.xml");
        List<FacilityCatchment> facilityCatchments = facilityCatchmentService.getFacilitiesForCatchment("3020");
        assertEquals(2, facilityCatchments.size() );
    }


}
