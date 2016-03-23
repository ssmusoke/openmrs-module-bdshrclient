package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticReport;
import org.junit.After;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir.MRSProperties;
import org.openmrs.module.fhir.MapperTestHelper;
import org.openmrs.module.fhir.ObsHelper;
import org.openmrs.module.fhir.mapper.model.EmrEncounter;
import org.openmrs.module.fhir.mapper.model.ShrEncounterBundle;
import org.openmrs.module.fhir.utils.FHIRBundleHelper;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Set;

import static org.junit.Assert.*;
import static org.openmrs.module.fhir.MapperTestHelper.getSystemProperties;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FHIRDiagnosticReportMapperIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private FHIRDiagnosticReportMapper fhirDiagnosticReportMapper;
    @Autowired
    private ApplicationContext springContext;
    @Autowired
    private OrderService orderService;

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }

    @Test
    public void shouldNotHandleReportWithoutCategory() throws Exception {
        assertFalse(fhirDiagnosticReportMapper.canHandle(new DiagnosticReport()));
    }

    @Test
    public void shouldHandleDiagnosticReportWithRadiologyCategory() throws Exception {
        Bundle bundle = (Bundle) new MapperTestHelper().loadSampleFHIREncounter("encounterBundles/dstu2/diagnosticReportWithRadiologyReport.xml", springContext);
        IResource resource = FHIRBundleHelper.identifyFirstResourceWithName(bundle, new DiagnosticReport().getResourceName());
        fhirDiagnosticReportMapper.canHandle(resource);
    }

    @Test
    public void shouldMapRadiologyReport() throws Exception {
        executeDataSet("testDataSets/radiologyFulfillmentDS.xml");
        Bundle bundle = (Bundle) new MapperTestHelper().loadSampleFHIREncounter("encounterBundles/dstu2/diagnosticReportWithRadiologyReport.xml", springContext);
        IResource report = FHIRBundleHelper.identifyFirstResourceWithName(bundle, new DiagnosticReport().getResourceName());
        Order order = orderService.getOrder(19);

        ShrEncounterBundle shrEncounterBundle = new ShrEncounterBundle(bundle, "98101039678", "shr-enc-id-1");
        EmrEncounter emrEncounter = new EmrEncounter(new Encounter());
        fhirDiagnosticReportMapper.map(report, emrEncounter, shrEncounterBundle, getSystemProperties("1"));

        Set<Obs> topLevelObs = emrEncounter.getTopLevelObs();
        assertEquals(1, topLevelObs.size());

        Obs radiologyFullfillmentObs = topLevelObs.iterator().next();
        assertEquals("Radiology Order Fulfillment Form", radiologyFullfillmentObs.getConcept().getName().getName());
        assertEquals(3, radiologyFullfillmentObs.getGroupMembers().size());
        assertEquals(order, radiologyFullfillmentObs.getOrder());

        ObsHelper obsHelper = new ObsHelper();
        Obs typeOfRadiologyObs = obsHelper.findMemberObsByConceptName(radiologyFullfillmentObs, "Type of Radiology" + MRSProperties.UNVERIFIED_BY_TR);
        assertNotNull(typeOfRadiologyObs);
        assertEquals("X-Ray Right Chest", typeOfRadiologyObs.getValueText());
        assertEquals(order, typeOfRadiologyObs.getOrder());

        Obs findingsObs = obsHelper.findMemberObsByConceptName(radiologyFullfillmentObs, "Radiology order findings" + MRSProperties.UNVERIFIED_BY_TR);
        assertEquals("crack head", findingsObs.getValueText());
        assertEquals(order, findingsObs.getOrder());

        Obs dateObs = obsHelper.findMemberObsByConceptName(radiologyFullfillmentObs, "Date of Radiology test" + MRSProperties.UNVERIFIED_BY_TR);
        assertEquals("22 Mar 2016", dateObs.getValueText());
        assertEquals(order, dateObs.getOrder());
    }

    @Test
    public void shouldMapLocalRadiologyReport() throws Exception {
        executeDataSet("testDataSets/radiologyFulfillmentDS.xml");
        Bundle bundle = (Bundle) new MapperTestHelper().loadSampleFHIREncounter("encounterBundles/dstu2/diagnosticReportWithLocalRadiologyOrder.xml", springContext);
        IResource report = FHIRBundleHelper.identifyFirstResourceWithName(bundle, new DiagnosticReport().getResourceName());

        ShrEncounterBundle shrEncounterBundle = new ShrEncounterBundle(bundle, "98101039678", "shr-enc-id-1");
        EmrEncounter emrEncounter = new EmrEncounter(new Encounter());
        fhirDiagnosticReportMapper.map(report, emrEncounter, shrEncounterBundle, getSystemProperties("1"));

        Set<Obs> topLevelObs = emrEncounter.getTopLevelObs();
        assertEquals(1, topLevelObs.size());

        Obs radiologyFulfillmentObs = topLevelObs.iterator().next();
        assertEquals("Radiology Order Fulfillment Form", radiologyFulfillmentObs.getConcept().getName().getName());
        assertEquals(1, radiologyFulfillmentObs.getGroupMembers().size());
        assertNull(radiologyFulfillmentObs.getOrder());

        ObsHelper obsHelper = new ObsHelper();
        Obs complaintObs = obsHelper.findMemberObsByConceptName(radiologyFulfillmentObs, "Radiology order Complaint" + MRSProperties.UNVERIFIED_BY_TR);
        assertNotNull(complaintObs);
        assertEquals(2, complaintObs.getGroupMembers().size());

        Obs typeOfRadiologyObs = obsHelper.findMemberObsByConceptName(complaintObs, "Type of Radiology" + MRSProperties.UNVERIFIED_BY_TR);
        assertNotNull(typeOfRadiologyObs);
        assertEquals("X-Ray Left Chest", typeOfRadiologyObs.getValueText());
        assertNull(typeOfRadiologyObs.getOrder());

        Obs dateObs = obsHelper.findMemberObsByConceptName(complaintObs, "Date of Radiology test" + MRSProperties.UNVERIFIED_BY_TR);
        assertEquals("22 Mar 2016", dateObs.getValueText());
        assertNull(dateObs.getOrder());
    }
}