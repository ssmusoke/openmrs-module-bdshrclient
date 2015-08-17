package org.openmrs.module.fhir.mapper.bundler;

import org.hl7.fhir.instance.model.Age;
import org.hl7.fhir.instance.model.Coding;
import org.hl7.fhir.instance.model.Date;
import org.hl7.fhir.instance.model.Encounter;
import org.hl7.fhir.instance.model.FamilyHistory;
import org.hl7.fhir.instance.model.ResourceReference;
import org.hl7.fhir.instance.model.ResourceType;
import org.junit.Test;
import org.openmrs.api.EncounterService;
import org.openmrs.module.fhir.TestFhirFeedHelper;
import org.openmrs.module.fhir.mapper.FHIRProperties;
import org.openmrs.module.fhir.utils.DateUtil;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.openmrs.module.fhir.MapperTestHelper.getSystemProperties;
import static org.openmrs.module.fhir.mapper.FHIRProperties.UCUM_UNIT_FOR_YEARS;
import static org.openmrs.module.fhir.mapper.FHIRProperties.UCUM_URL;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FamilyHistoryMapperIT extends BaseModuleWebContextSensitiveTest {
    @Autowired
    private EncounterService encounterService;

    @Autowired
    private FamilyHistoryMapper familyHistoryMapper;

    @Test
    public void shouldCreateFHIRFamilyHistoryFromOpenMrsFamilyHistory() throws Exception {
        executeDataSet("testDataSets/shrClientFamilyHistoryTestDS.xml");
        Encounter encounter = new Encounter();
        encounter.setIndication(new ResourceReference());
        ResourceReference subject = new ResourceReference();
        subject.setReferenceSimple("http://mci.com/patient/hid");
        encounter.setSubject(subject);
        encounter.addParticipant().setIndividual(new ResourceReference());
        org.openmrs.Encounter openMrsEncounter = encounterService.getEncounter(36);

        List<FHIRResource> familyHistoryResources = familyHistoryMapper.map(openMrsEncounter.getObsAtTopLevel(false).iterator().next(), encounter, getSystemProperties("1"));
        assertFalse(familyHistoryResources.isEmpty());
        assertEquals(1, familyHistoryResources.size());

        FamilyHistory familyHistoryResource = (FamilyHistory) TestFhirFeedHelper.getResourceByType(ResourceType.FamilyHistory, familyHistoryResources).getResource();
        assertEquals(subject, familyHistoryResource.getSubject());
        assertFalse(familyHistoryResource.getIdentifier().isEmpty());

        assertEquals(1, familyHistoryResource.getRelation().size());
        FamilyHistory.FamilyHistoryRelationComponent familyHistoryRelationComponent = familyHistoryResource.getRelation().get(0);

        assertRelationship(familyHistoryRelationComponent);
        assertBornOn(familyHistoryRelationComponent);
        assertRelationshipCondition(familyHistoryRelationComponent);
    }

    private void assertRelationshipCondition(FamilyHistory.FamilyHistoryRelationComponent familyHistoryRelationComponent) {
        assertEquals(1, familyHistoryRelationComponent.getCondition().size());
        FamilyHistory.FamilyHistoryRelationConditionComponent familyHistoryRelationConditionComponent = familyHistoryRelationComponent.getCondition().get(0);
        assertEquals("some notes", familyHistoryRelationConditionComponent.getNoteSimple());
        assertOnsetAge(familyHistoryRelationConditionComponent);
        assertConditionType(familyHistoryRelationConditionComponent);
    }

    private void assertConditionType(FamilyHistory.FamilyHistoryRelationConditionComponent familyHistoryRelationConditionComponent) {
        assertEquals(1, familyHistoryRelationConditionComponent.getType().getCoding().size());
        Coding type = familyHistoryRelationConditionComponent.getType().getCoding().get(0);
        assertEquals("3", type.getCodeSimple());
        assertEquals("http://tr.com/openmrs/ws/rest/v1/tr/concept/3", type.getSystemSimple());
    }

    private void assertOnsetAge(FamilyHistory.FamilyHistoryRelationConditionComponent familyHistoryRelationConditionComponent) {
        Age onset = (Age) familyHistoryRelationConditionComponent.getOnset();
        assertEquals(12, onset.getValueSimple().intValue());
        assertEquals(UCUM_UNIT_FOR_YEARS, onset.getUnitsSimple());
        assertEquals(UCUM_URL, onset.getSystemSimple());
    }

    private void assertBornOn(FamilyHistory.FamilyHistoryRelationComponent familyHistoryRelationComponent) {
        Date bornOn = (Date) familyHistoryRelationComponent.getBorn();
        assertEquals(DateUtil.parseDate("1978-02-15 00:00:00"), DateUtil.parseDate(bornOn.getValue().toString()));
    }

    private void assertRelationship(FamilyHistory.FamilyHistoryRelationComponent familyHistoryRelationComponent) {
        assertEquals(1, familyHistoryRelationComponent.getRelationship().getCoding().size());

        Coding relationship = familyHistoryRelationComponent.getRelationship().getCoding().get(0);
        assertEquals("FTH", relationship.getCodeSimple());
        assertEquals(FHIRProperties.FHIR_SYSTEM_RELATIONSHIP_ROLE, relationship.getSystemSimple());
    }
}