package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.ProcedureRequest;
import ca.uhn.fhir.model.dstu2.valueset.ProcedureRequestStatusEnum;
import ca.uhn.fhir.model.primitive.StringDt;
import org.apache.commons.collections4.CollectionUtils;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Order;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir.FHIRProperties;
import org.openmrs.module.fhir.mapper.model.FHIREncounter;
import org.openmrs.module.fhir.mapper.model.FHIRResource;
import org.openmrs.module.fhir.utils.DateUtil;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.openmrs.module.fhir.MapperTestHelper.containsCoding;
import static org.openmrs.module.fhir.MapperTestHelper.getSystemProperties;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ProcedureOrderMapperIT extends BaseModuleWebContextSensitiveTest {
    @Autowired
    private ProcedureOrderMapper procedureOrderMapper;

    @Autowired
    private OrderService orderService;

    private final String patientRef = "http://mci.com/patients/HID-123";
    private final String fhirEncounterId = "SHR-ENC1";


    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/procedureOrderDS.xml");
    }

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }

    @Test
    public void shouldHandleAProcedureOrder() throws Exception {
        Order order = orderService.getOrder(17);
        assertTrue(procedureOrderMapper.canHandle(order));
    }

    @Test
    public void shouldNotHandleOrdersOtherThanProcedureOrder() throws Exception {
        Order order = orderService.getOrder(16);
        assertFalse(procedureOrderMapper.canHandle(order));
    }

    @Test
    public void shouldMapAProcedureOrder() throws Exception {
        Order order = orderService.getOrder(17);
        List<FHIRResource> mappedResources = procedureOrderMapper.map(order, createFhirEncounter(), new Bundle(), getSystemProperties("1"));
        assertEquals(1, mappedResources.size());
        ProcedureRequest procedureRequest = (ProcedureRequest) mappedResources.get(0).getResource();
        assertProcedureRequest(procedureRequest, "101", "http://tr.com/ws/concepts/101", "Colposcopy",
                "2008-08-08 00:00:00", ProcedureRequestStatusEnum.REQUESTED);
    }

    @Test
    public void shouldMapAProcedureOrderWithLocalConcept() throws Exception {
        Order order = orderService.getOrder(18);
        List<FHIRResource> mappedResources = procedureOrderMapper.map(order, createFhirEncounter(), new Bundle(), getSystemProperties("1"));
        assertEquals(1, mappedResources.size());
        ProcedureRequest procedureRequest = (ProcedureRequest) mappedResources.get(0).getResource();
        assertProcedureRequest(procedureRequest, null, null, "Division of Brain",
                "2008-08-08 00:00:00", ProcedureRequestStatusEnum.REQUESTED);
    }

    @Test
    public void shouldNotMapAStoppedProcedureOrder() throws Exception {
        Order order = orderService.getOrder(19);
        List<FHIRResource> mappedResources = procedureOrderMapper.map(order, createFhirEncounter(), new Bundle(), getSystemProperties("1"));
        assertTrue(mappedResources.isEmpty());
    }

    @Test
    public void shouldMapADiscontinuedProcedureOrder() throws Exception {
        Order order = orderService.getOrder(20);

        List<FHIRResource> mappedResources = procedureOrderMapper.map(order, createFhirEncounter(), new Bundle(), getSystemProperties("1"));
        assertEquals(1, mappedResources.size());
        ProcedureRequest procedureRequest = (ProcedureRequest) mappedResources.get(0).getResource();
        assertProcedureRequest(procedureRequest, "101", "http://tr.com/ws/concepts/101",
                "Colposcopy", "2008-08-19 12:22:22", ProcedureRequestStatusEnum.SUSPENDED);
        final List<ExtensionDt> extensions = procedureRequest.getUndeclaredExtensionsByUrl(
                FHIRProperties.getFhirExtensionUrl(FHIRProperties.PROCEDURE_REQUEST_PREVIOUS_REQUEST_EXTENSION_NAME));
        assertEquals(1, extensions.size());
        IBaseDatatype extension = extensions.get(0).getValue();
        assertTrue(extension instanceof StringDt);
        String actualPreviousOrderUri = ((StringDt) extension).getValue();
        String expectedPreviousOrderUri = "urn:uuid:" + order.getPreviousOrder().getUuid();
        assertEquals(expectedPreviousOrderUri, actualPreviousOrderUri);
    }

    private FHIREncounter createFhirEncounter() {
        ca.uhn.fhir.model.dstu2.resource.Encounter encounter = new ca.uhn.fhir.model.dstu2.resource.Encounter();
        encounter.setPatient(new ResourceReferenceDt(patientRef));
        encounter.setId(fhirEncounterId);
        return new FHIREncounter(encounter);
    }

    public void assertProcedureRequest(ProcedureRequest procedureRequest, String code, String system, String display, String orderedOn, ProcedureRequestStatusEnum procedureStatus) throws Exception {
        assertEquals(patientRef, procedureRequest.getSubject().getReference().getValue());
        assertFalse(procedureRequest.getId().isEmpty());
        assertTrue(CollectionUtils.isNotEmpty(procedureRequest.getIdentifier()));
        assertFalse(procedureRequest.getIdentifier().get(0).isEmpty());
        assertEquals(fhirEncounterId, procedureRequest.getEncounter().getReference().getValue());
        assertEquals(procedureStatus.getCode(), procedureRequest.getStatus());
        assertTrue(containsCoding(procedureRequest.getCode().getCoding(), code, system, display));
        Date expectedDate = DateUtil.parseDate(orderedOn);
        assertEquals(expectedDate, procedureRequest.getOrderedOn());
        assertTrue(procedureRequest.getOrderer().getReference().getValue().endsWith("812.json"));
        assertEquals("Some Notes", procedureRequest.getNotesFirstRep().getText());
    }
}