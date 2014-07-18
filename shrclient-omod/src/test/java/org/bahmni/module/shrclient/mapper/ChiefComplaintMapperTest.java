package org.bahmni.module.shrclient.mapper;

import org.hl7.fhir.instance.model.Coding;
import org.hl7.fhir.instance.model.Condition;
import org.hl7.fhir.instance.model.Encounter;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
        org.openmrs.Encounter openMrsEncounter = encounterService.getEncounter(36);

        List<Condition> conditions = chiefComplaintMapper.map(openMrsEncounter, encounter);

        assertEquals(1, conditions.size());
        final Condition condition = conditions.get(0);
        final Coding chiefComplaintCategory = condition.getCategory().getCoding().get(0);
        assertEquals(FHIRProperties.FHIR_CONDITION_CODE_CHIEF_COMPLAINT, chiefComplaintCategory.getCodeSimple());
        assertEquals(FHIRProperties.FHIR_CONDITION_CATEGORY_URL, chiefComplaintCategory.getSystemSimple());
        assertEquals("Complaint", chiefComplaintCategory.getDisplaySimple());
        final Coding chiefComplaintSevirity = condition.getSeverity().getCoding().get(0);
        assertEquals(FHIRProperties.FHIR_SEVERITY_MODERATE, chiefComplaintSevirity.getDisplaySimple());
        assertEquals(FHIRProperties.SNOMED_VALUE_MODERATE_SEVERTY, chiefComplaintSevirity.getCodeSimple());
        assertEquals(FHIRProperties.FHIR_CONDITION_SEVERITY_URL, chiefComplaintSevirity.getSystemSimple());
        assertEquals(Condition.ConditionStatus.confirmed, condition.getStatus().getValue());
        assertEquals("2008-08-18T15:09:05+05:30", condition.getDateAssertedSimple().toString());
        Concept chiefComplaintAnswer = conceptService.getConcept(301);
        String conceptName = chiefComplaintAnswer.getName().getName();
        ConceptMap mapping = chiefComplaintAnswer.getConceptMappings().iterator().next();
        ConceptReferenceTerm chiefComplaintReferenceTerm = mapping.getConceptReferenceTerm();
        Coding chiefComplaintCode = condition.getCode().getCoding().get(0);
        assertEquals(conceptName, chiefComplaintCode.getDisplaySimple());
        assertEquals(chiefComplaintReferenceTerm.getCode(), chiefComplaintCode.getCodeSimple());
        assertEquals(chiefComplaintReferenceTerm.getConceptSource().getName(), chiefComplaintCode.getSystemSimple());
        assertNull(condition.getEncounter());
        assertNull(condition.getIdentifier());
    }
}
