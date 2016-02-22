package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Condition;
import ca.uhn.fhir.model.dstu2.valueset.ConditionCategoryCodesEnum;
import ca.uhn.fhir.model.dstu2.valueset.ConditionVerificationStatusEnum;
import ca.uhn.fhir.model.primitive.StringDt;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Obs;
import org.openmrs.api.ObsService;
import org.openmrs.module.fhir.FHIRProperties;
import org.openmrs.module.fhir.mapper.model.FHIREncounter;
import org.openmrs.module.fhir.mapper.model.FHIRResource;
import org.openmrs.module.shrclient.dao.IdMappingRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.model.IdMappingType;
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

    @Autowired
    private IdMappingRepository idMappingRepository;


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

        assertDiagnosisCondition(fhirEncounter, diagnosisCondition,
                "101", "http://tr.com/ws/concepts/101", "Ankylosis of joint",
                ConditionVerificationStatusEnum.CONFIRMED, "Some Comment");
    }

    @Test
    public void shouldNotMapNonCodedDiagnosis() throws Exception {
        Obs visitDiagnosisObs = obsService.getObs(61);

        FHIREncounter fhirEncounter = createFhirEncounter();

        List<FHIRResource> fhirResources = diagnosisMapper.map(visitDiagnosisObs, fhirEncounter, getSystemProperties("1"));
        assertEquals(0, fhirResources.size());
    }

    @Test
    public void shouldNotMapDiagnosisObsIfNotTrDiagnosisConcept() throws Exception {
        Obs visitDiagnosisObs = obsService.getObs(8);

        FHIREncounter fhirEncounter = createFhirEncounter();

        List<FHIRResource> fhirResources = diagnosisMapper.map(visitDiagnosisObs, fhirEncounter, getSystemProperties("1"));
        assertEquals(0, fhirResources.size());
    }

    @Test
    public void shouldMapDiagnosisEditedInTheSameEncounter() throws Exception {
        Obs visitDiagnosisObs = obsService.getObs(21);

        FHIREncounter fhirEncounter = createFhirEncounter();

        List<FHIRResource> fhirResources = diagnosisMapper.map(visitDiagnosisObs, fhirEncounter, getSystemProperties("1"));
        assertEquals(1, fhirResources.size());
        Condition diagnosisCondition = (Condition) fhirResources.get(0).getResource();
        assertNotNull(diagnosisCondition);

        assertDiagnosisCondition(fhirEncounter, diagnosisCondition,
                "101", "http://tr.com/ws/concepts/101", "Ankylosis of joint",
                ConditionVerificationStatusEnum.PROVISIONAL, "Some Comment");
    }

    @Test
    public void shouldMapDiagnosisEditedInTheDifferentEncounters() throws Exception {
        Obs visitDiagnosisObs = obsService.getObs(31);
        Obs updatedDiagnosisObs = obsService.getObs(41);

        FHIREncounter fhirEncounter = createFhirEncounter();

        List<FHIRResource> fhirResources = diagnosisMapper.map(visitDiagnosisObs, fhirEncounter, getSystemProperties("1"));
        assertEquals(1, fhirResources.size());
        Condition initialDiagnosisCondition = (Condition) fhirResources.get(0).getResource();
        assertNotNull(initialDiagnosisCondition);

        assertDiagnosisCondition(fhirEncounter, initialDiagnosisCondition,
                "101", "http://tr.com/ws/concepts/101", "Ankylosis of joint",
                ConditionVerificationStatusEnum.CONFIRMED, "Some Comment");

        fhirResources = diagnosisMapper.map(updatedDiagnosisObs, fhirEncounter, getSystemProperties("1"));
        assertEquals(1, fhirResources.size());
        Condition updatedDiagnosisCondition = (Condition) fhirResources.get(0).getResource();
        assertNotNull(updatedDiagnosisCondition);

        assertDiagnosisCondition(fhirEncounter, updatedDiagnosisCondition,
                "101", "http://tr.com/ws/concepts/101", "Ankylosis of joint",
                ConditionVerificationStatusEnum.PROVISIONAL, "Some Updated Comment");
        final List<ExtensionDt> extensions = updatedDiagnosisCondition.getUndeclaredExtensionsByUrl(
                FHIRProperties.getFhirExtensionUrl(FHIRProperties.PREVIOUS_CONDITION_EXTENSION_NAME));
        assertEquals(1, extensions.size());
        IBaseDatatype extension = extensions.get(0).getValue();
        assertTrue(extension instanceof StringDt);
        String previousDiagnosisUri = ((StringDt) extension).getValue();
        IdMapping diagnosisIdMapping = idMappingRepository.findByInternalId(visitDiagnosisObs.getUuid(), IdMappingType.DIAGNOSIS);
        assertEquals(diagnosisIdMapping.getUri(), previousDiagnosisUri);
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