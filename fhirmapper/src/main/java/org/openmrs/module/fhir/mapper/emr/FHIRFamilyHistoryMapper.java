package org.openmrs.module.fhir.mapper.emr;

import org.apache.commons.lang.StringUtils;
import org.hl7.fhir.instance.model.Age;
import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.CodeableConcept;
import org.hl7.fhir.instance.model.Coding;
import org.hl7.fhir.instance.model.Date;
import org.hl7.fhir.instance.model.FamilyHistory;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.Type;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.utils.DateUtil;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.openmrs.module.fhir.mapper.MRSProperties.*;

@Component
public class FHIRFamilyHistoryMapper implements FHIRResourceMapper {
    @Autowired
    private IdMappingsRepository idMappingsRepository;
    @Autowired
    private ConceptService conceptService;

    @Override
    public boolean canHandle(Resource resource) {
        return (resource instanceof FamilyHistory);
    }

    @Override
    public void map(AtomFeed feed, Resource resource, Patient emrPatient, Encounter newEmrEncounter, Map<String, List<String>> processedList) {
        FamilyHistory familyHistory = (FamilyHistory) resource;
        if (isAlreadyProcessed(familyHistory, processedList))
            return;
        Obs familyHistoryObs = new Obs();
        familyHistoryObs.setConcept(conceptService.getConceptByName(MRS_CONCEPT_NAME_FAMILY_HISTORY));
        mapRelationships(familyHistoryObs, familyHistory.getRelation());
        newEmrEncounter.addObs(familyHistoryObs);

        processedList.put(familyHistory.getIdentifier().get(0).getValueSimple(), Arrays.asList(familyHistoryObs.getUuid()));
    }


    private boolean isAlreadyProcessed(FamilyHistory familyHistory, Map<String, List<String>> processedList) {
        return processedList.containsKey(familyHistory.getIdentifier().get(0).getValueSimple());
    }

    private void mapRelationships(Obs familyHistoryObs, List<FamilyHistory.FamilyHistoryRelationComponent> relations) {
        for (FamilyHistory.FamilyHistoryRelationComponent relation : relations) {
            Obs personObs = new Obs();
            personObs.setConcept(conceptService.getConceptByName(MRS_CONCEPT_NAME_PERSON));
            mapRelation(personObs, relation);
            familyHistoryObs.addGroupMember(personObs);
        }
    }

    private void mapRelation(Obs personObs, FamilyHistory.FamilyHistoryRelationComponent relation) {
        personObs.addGroupMember(setBornOnObs(relation));
        mapRelationship(personObs, relation);
        for (FamilyHistory.FamilyHistoryRelationConditionComponent component : relation.getCondition()) {
            personObs.addGroupMember(mapRelationCondition(component));
        }
    }

    private void mapRelationship(Obs personObs, FamilyHistory.FamilyHistoryRelationComponent relation) {
        Obs relationship = mapRelationship(getCodeSimple(relation));
        if (null != relationship) {
            personObs.addGroupMember(relationship);
        }
    }

    private String getCodeSimple(FamilyHistory.FamilyHistoryRelationComponent relation) {
        CodeableConcept relationship = relation.getRelationship();
        if (null == relationship) {
            return null;
        }
        List<Coding> coding = relationship.getCoding();
        if (null == coding) {
            return null;
        }
        return coding.get(0).getCodeSimple();
    }

    private Obs mapRelationCondition(FamilyHistory.FamilyHistoryRelationConditionComponent component) {
        Obs result = new Obs();
        result.setConcept(conceptService.getConceptByName(MRS_CONCEPT_NAME_RELATIONSHIP_CONDITION));
        mapOnsetDate(result, component.getOnset());
        mapNotes(result, component);
        mapCondition(component, result);
        return result;
    }

    private void mapCondition(FamilyHistory.FamilyHistoryRelationConditionComponent component, Obs result) {
        Obs value = new Obs();
        Concept answerConcept = getAnswer(component);
        value.setConcept(conceptService.getConceptByName(MRS_CONCEPT_NAME_RELATIONSHIP_DIAGNOSIS));
        value.setValueCoded(answerConcept);
        result.addGroupMember(value);
    }

    private Concept getAnswer(FamilyHistory.FamilyHistoryRelationConditionComponent component) {
        List<Coding> coding = component.getType().getCoding();
        for (Coding code : coding) {
            IdMapping mapping = idMappingsRepository.findByExternalId(code.getCodeSimple());
            if (null != mapping) {
                return conceptService.getConceptByUuid(mapping.getInternalId());
            }
        }
        return null;
    }

    private void mapNotes(Obs result, FamilyHistory.FamilyHistoryRelationConditionComponent component) {
        if (null != component.getNote()) {
            Obs notes = new Obs();
            Concept onsetDateConcept = conceptService.getConceptByName(MRS_CONCEPT_NAME_RELATIONSHIP_NOTES);
            notes.setConcept(onsetDateConcept);
            notes.setValueText(component.getNoteSimple());
            result.addGroupMember(notes);
        }
    }

    private void mapOnsetDate(Obs result, Type onset) {
        if (null != onset && onset instanceof Age) {
            Obs ageValue = new Obs();
            Concept onsetDateConcept = conceptService.getConceptByName(MRS_CONCEPT_NAME_ONSET_AGE);
            ageValue.setConcept(onsetDateConcept);
            ageValue.setValueNumeric(((Age) onset).getValue().getValue().doubleValue());
            result.addGroupMember(ageValue);
        }
    }


    private Obs mapRelationship(String code) {
        if (StringUtils.isNotBlank(code)) {
            Obs result = new Obs();
            result.setConcept(conceptService.getConceptByName(MRS_CONCEPT_NAME_RELATIONSHIP));
            result.setValueCoded(conceptService.getConceptByName(code));
            return result;
        } else {
            return null;
        }
    }

    private Obs setBornOnObs(FamilyHistory.FamilyHistoryRelationComponent relation) {
        if (null != relation.getBorn() && relation.getBorn() instanceof Date) {
            Obs bornOnObs = new Obs();
            java.util.Date observationValue = null;
            Concept bornOnConcept = conceptService.getConceptByName(MRS_CONCEPT_NAME_BORN_ON);
            String bornOnDate = ((Date) relation.getBorn()).getValue().toString();
            observationValue = DateUtil.parseDate(bornOnDate);
            bornOnObs.setValueDate(observationValue);
            bornOnObs.setConcept(bornOnConcept);
            return bornOnObs;
        }
        return null;
    }
}
