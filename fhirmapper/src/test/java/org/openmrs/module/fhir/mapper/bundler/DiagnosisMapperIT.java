package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Condition;
import ca.uhn.fhir.model.dstu2.valueset.ConditionCategoryCodesEnum;
import ca.uhn.fhir.model.dstu2.valueset.ConditionVerificationStatusEnum;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Obs;
import org.openmrs.api.ObsService;
import org.openmrs.module.fhir.mapper.model.FHIREncounter;
import org.openmrs.module.fhir.mapper.model.FHIRResource;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.junit.Assert.*;
import static org.openmrs.module.fhir.MapperTestHelper.containsCoding;
import static org.openmrs.module.fhir.MapperTestHelper.getSystemProperties;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DiagnosisMapperIT extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private DiagnosisMapper diagnosisMapper;

    @Autowired
    private ObsService obsService;
    

    @Before
    public void setUp() throws Exception {
        executeDataSet("testDataSets/diagnosisTestDS.xml");
    }

    @After
    public void tearDown() throws Exception {
        deleteAllData();
    }

    @Test
    public void shouldHandleDiagnosisObs() throws Exception {
        Obs visitDiagnosisObs = obsService.getObs(1);
        assertTrue(diagnosisMapper.canHandle(visitDiagnosisObs));
    }

    @Test
    public void shouldMapDiagnosisObsToFHIRDiagnosisCondition() throws Exception {
        Obs visitDiagnosisObs = obsService.getObs(1);

        FHIREncounter fhirEncounter = createFhirEncounter();

        List<FHIRResource> fhirResources = diagnosisMapper.map(visitDiagnosisObs, fhirEncounter, getSystemProperties("1"));
        assertEquals(1, fhirResources.size());
        Condition diagnosisCondition = (Condition) fhirResources.get(0).getResource();
        assertNotNull(diagnosisCondition);

        assertDiagnosisCondition(fhirEncounter, diagnosisCondition, "101", "http://tr.com/ws/concepts/101", "Ankylosis of joint", ConditionVerificationStatusEnum.CONFIRMED, "Some Comment");
    }

    @Test
    public void shouldNotMapDiagnosisObsIfNotTrDiagnosisConcept() throws Exception {
        Obs visitDiagnosisObs = obsService.getObs(8);

        FHIREncounter fhirEncounter = createFhirEncounter();

        List<FHIRResource> fhirResources = diagnosisMapper.map(visitDiagnosisObs, fhirEncounter, getSystemProperties("1"));
        assertEquals(0, fhirResources.size());
    }

    private void assertDiagnosisCondition(FHIREncounter fhirEncounter, Condition diagnosisCondition, String code, String system, String display, ConditionVerificationStatusEnum verificationStatus, String comments) {
        assertFalse(diagnosisCondition.getId().isEmpty());
        assertFalse(diagnosisCondition.getIdentifier().isEmpty());
        assertFalse(diagnosisCondition.getIdentifierFirstRep().isEmpty());

        assertEquals(fhirEncounter.getPatient().getReference().getValue(), diagnosisCondition.getPatient().getReference().getValue());
        assertEquals(fhirEncounter.getId(), diagnosisCondition.getEncounter().getReference().getValue());
        assertEquals(ConditionCategoryCodesEnum.DIAGNOSIS, diagnosisCondition.getCategory().getValueAsEnum().iterator().next());
        assertEquals(fhirEncounter.getFirstParticipantReference(), diagnosisCondition.getAsserter());

        assertEquals(1, diagnosisCondition.getCode().getCoding().size());
        assertTrue(containsCoding(diagnosisCondition.getCode().getCoding(), code, system, display));
        assertEquals(verificationStatus.getCode(), diagnosisCondition.getVerificationStatus());
        assertEquals(comments, diagnosisCondition.getNotes());
    }

    private FHIREncounter createFhirEncounter() {
        ca.uhn.fhir.model.dstu2.resource.Encounter encounter = new ca.uhn.fhir.model.dstu2.resource.Encounter();
        String fhirEncounterId = "SHR-ENC1";
        String patientUrl = "http://mci.com/patients/HID-123";
        String providerUrl = "http://pr.com/providers/812.json";
        encounter.setPatient(new ResourceReferenceDt(patientUrl));
        encounter.setId(fhirEncounterId);
        encounter.addParticipant().setIndividual(new ResourceReferenceDt(providerUrl));
        return new FHIREncounter(encounter);
    }
}