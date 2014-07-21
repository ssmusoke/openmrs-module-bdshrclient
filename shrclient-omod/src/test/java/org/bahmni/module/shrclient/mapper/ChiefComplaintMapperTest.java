package org.bahmni.module.shrclient.mapper;

import org.hl7.fhir.instance.model.*;
import org.junit.Assert;
import static org.junit.Assert.*;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class ChiefComplaintMapperTest extends BaseModuleWebContextSensitiveTest {

    @Autowired
    ChiefComplaintMapper chiefComplaintMapper;

    @Autowired
    EncounterService encounterService;

    @Autowired
    ConceptService conceptService;

    @Test
    public void shouldCreateFHIRConditionFromChiefComplaint() throws Exception {
        executeDataSet("shrClientChiefComplaintTestDS.xml");
        Encounter encounter = new Encounter();
        encounter.setIndication(new ResourceReference());
        encounter.setSubject(new ResourceReference());
        encounter.addParticipant().setIndividual(new ResourceReference());
        org.openmrs.Encounter openMrsEncounter = encounterService.getEncounter(36);

        List<Condition> conditions = chiefComplaintMapper.map(openMrsEncounter, encounter);
        assertNotNull(conditions);
        assertEquals(1,conditions.size());

        Condition condition = conditions.get(0);
        assertNotNull(condition.getEncounter());
        assertNotNull(condition.getSubject());
        assertNotNull(condition.getAsserter());
        CodeableConcept conditionCategory = condition.getCategory();
        assertNotNull(conditionCategory);
        Coding conditionCategoryCoding = conditionCategory.getCoding().get(0);
        assertEquals(FHIRProperties.FHIR_CONDITION_CODE_CHIEF_COMPLAINT, conditionCategoryCoding.getCodeSimple());
        assertEquals(FHIRProperties.FHIR_CONDITION_CATEGORY_URL, conditionCategoryCoding.getSystemSimple());
        assertEquals("Complaint", conditionCategoryCoding.getDisplaySimple());
        CodeableConcept conditionCode = condition.getCode();
        assertNotNull(conditionCode);
        assertEquals("Right arm pain",conditionCode.getCoding().get(0).getDisplaySimple());
        CodeableConcept conditionSeverity = condition.getSeverity();
        assertNotNull(conditionSeverity);
        Coding conditionSevitityCode = conditionSeverity.getCoding().get(0);
        assertEquals(FHIRProperties.FHIR_SEVERITY_MODERATE, conditionSevitityCode.getDisplaySimple());
        assertEquals(FHIRProperties.SNOMED_VALUE_MODERATE_SEVERTY, conditionSevitityCode.getCodeSimple());
        assertEquals(FHIRProperties.FHIR_CONDITION_SEVERITY_URL, conditionSevitityCode.getSystemSimple());
        assertEquals(Condition.ConditionStatus.confirmed, condition.getStatusSimple());
        assertEquals("2008-08-18T15:09:05+05:30", condition.getDateAsserted().getValue().toString());
        DateTime onsetDateTime = (DateTime) condition.getOnset();
        assertEquals("2008-08-18T13:09:05+05:30", onsetDateTime.getValue().toString());
    }
}
