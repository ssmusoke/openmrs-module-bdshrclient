package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticReport;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.StringDt;
import org.junit.After;
import org.junit.Test;
import org.openmrs.Obs;
import org.openmrs.api.ObsService;
import org.openmrs.module.fhir.MRSProperties;
import org.openmrs.module.fhir.mapper.model.FHIREncounter;
import org.openmrs.module.fhir.mapper.model.FHIRResource;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.junit.Assert.*;
import static org.openmrs.module.fhir.MapperTestHelper.getSystemProperties;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class RadiologyFulfillmentMapperIT extends BaseModuleWebContextSensitiveTest {
    @Autowired
    private RadiologyFulfillmentMapper radiologyFulfillmentMapper;
    @Autowired
    private ObsService obsService;

    private final String patientRef = "http://mci.com/patients/HID-123";
    private final String fhirEncounterId = "SHR-ENC1";

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }

    @Test
    public void shouldHandleRadiologyFulfillment() throws Exception {
        executeDataSet("testDataSets/radiologyFulfillmentDS.xml");
        Obs observation = obsService.getObs(501);
        assertTrue(radiologyFulfillmentMapper.canHandle(observation));
    }

    @Test
    public void shouldNotHandleProcedureFulfillment() throws Exception {
        executeDataSet("testDataSets/procedureFulfillmentDS.xml");
        Obs fulfilmentObs = obsService.getObs(1011);
        assertFalse(radiologyFulfillmentMapper.canHandle(fulfilmentObs));
    }

    @Test
    public void shouldNotHandleRadiologyFulfillment() throws Exception {
        executeDataSet("testDataSets/labResultDS.xml");
        Obs observation = obsService.getObs(1);
        assertFalse(radiologyFulfillmentMapper.canHandle(observation));
    }

    @Test
    public void shouldMapRadiologyFulfillment() throws Exception {
        executeDataSet("testDataSets/radiologyFulfillmentDS.xml");
        Obs observation = obsService.getObs(501);
        List<FHIRResource> resources = radiologyFulfillmentMapper.map(observation, createFhirEncounter(), getSystemProperties("1"));
        assertFalse(resources == null);


        FHIRResource report = getResourceByName(resources, "Diagnostic Report");
        DiagnosticReport resource = (DiagnosticReport) report.getResource();
        assertEquals("X-Ray Right Chest", resource.getCode().getCoding().get(0).getDisplay());

        FHIRResource typeOfOrder = getResourceByName(resources, MRSProperties.MRS_CONCEPT_TYPE_OF_RADIOLOGY_ORDER);
        Observation type = (Observation) typeOfOrder.getResource();
        CodeableConceptDt value = (CodeableConceptDt) type.getValue();
        assertEquals("X-Ray Right Chest", value.getCodingFirstRep().getDisplay());

        FHIRResource dateOfOrder = getResourceByName(resources, MRSProperties.MRS_CONCEPT_DATE_OF_RADIOLOGY_ORDER);
        Observation date = (Observation) dateOfOrder.getResource();
        DateTimeDt actualDateTime = (DateTimeDt) date.getValue();
        DateTimeDt expectedDateTime = new DateTimeDt("2008-08-18");
        assertEquals(expectedDateTime.getValue(), actualDateTime.getValue());


        FHIRResource finding = getResourceByName(resources, MRSProperties.MRS_CONCEPT_FINDINGS_OF_RADIOLOGY_ORDER);
        Observation findings = (Observation) finding.getResource();
        assertEquals(new StringDt("Findings"), findings.getValue());

    }

    private FHIRResource getResourceByName(List<FHIRResource> resources, String type) {
        for (FHIRResource resource : resources) {
            if (resource.getResourceName().equals(type)) return resource;
        }
        return null;
    }

    private FHIREncounter createFhirEncounter() {
        ca.uhn.fhir.model.dstu2.resource.Encounter encounter = new ca.uhn.fhir.model.dstu2.resource.Encounter();
        encounter.setPatient(new ResourceReferenceDt(patientRef));
        encounter.setId(fhirEncounterId);
        return new FHIREncounter(encounter);
    }

}