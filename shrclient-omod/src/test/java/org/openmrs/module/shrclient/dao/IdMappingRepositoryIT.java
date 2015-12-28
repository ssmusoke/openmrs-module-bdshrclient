package org.openmrs.module.shrclient.dao;

import org.junit.After;
import org.junit.Test;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.model.IdMappingType;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;


public class IdMappingRepositoryIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    IdMappingRepository idMappingRepository;

    @Test
    public void shouldGetAllIdMappingsByHealthId() throws Exception {
        executeDataSet("testDataSets/idMappingDS.xml");

        assertIdMapping(idMappingRepository.findByHealthId("Health_id", IdMappingType.ENCOUNTER).get(0), new IdMapping("75e04d42-3ca8-11e3-bf2b-0800271c1b75", "75e04d42-3ca8-11e3-bf2b-0800271c1b76", "encounter", "/patients/Health_id/encounters"));
        assertIdMapping(idMappingRepository.findByHealthId("Health_id", IdMappingType.MEDICATION_ORDER).get(0), new IdMapping("juydg81f-1yz9-4xv3-bz88-8z22a1dx1zt","151", "medication_order", "/patients/Health_id/encounters/597d18f5-bc92-4c43-b0fa-526fe6181d0e#MedicationOrder/e30e5355-cca5-4eb5-947f-0"));

    }

    @Test
    public void shouldUpdateAllIdMappingsByHealthId() throws Exception {
        executeDataSet("testDataSets/idMappingDS.xml");
        idMappingRepository.replaceHealthId("Health_id", "new_HID");

        assertIdMapping(idMappingRepository.findByHealthId("new_HID", IdMappingType.ENCOUNTER).get(0), new IdMapping("75e04d42-3ca8-11e3-bf2b-0800271c1b75", "75e04d42-3ca8-11e3-bf2b-0800271c1b76", "encounter", "/patients/new_HID/encounters"));
        assertIdMapping(idMappingRepository.findByHealthId("new_HID", IdMappingType.MEDICATION_ORDER).get(0), new IdMapping("juydg81f-1yz9-4xv3-bz88-8z22a1dx1zt", "151", "medication_order", "/patients/new_HID/encounters/597d18f5-bc92-4c43-b0fa-526fe6181d0e#MedicationOrder/e30e5355-cca5-4eb5-947f-0"));

    }
    
    private void assertIdMapping(IdMapping idMapping, IdMapping expectedIdMapping) {
        assertEquals(expectedIdMapping.getInternalId(), idMapping.getInternalId());
        assertEquals(expectedIdMapping.getExternalId(), idMapping.getExternalId());
        assertEquals(expectedIdMapping.getType(), idMapping.getType());
        assertEquals(expectedIdMapping.getUri(), idMapping.getUri());
    }
    @After
    public void tearDown() throws Exception {
        deleteAllData();

    }
}