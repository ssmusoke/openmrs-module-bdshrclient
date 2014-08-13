package org.openmrs.module.fhir.mapper.emr;

import org.apache.commons.lang.StringUtils;
import org.hl7.fhir.instance.model.Age;
import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.FamilyHistory;
import org.hl7.fhir.instance.model.Resource;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.mapper.MRSProperties;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FHIRFamilyHistoryMapper implements FHIRResource {
    @Autowired
    private IdMappingsRepository idMappingsRepository;
    @Autowired
    private ConceptService conceptService;

    @Override
    public boolean handles(Resource resource) {
        return (resource instanceof FamilyHistory);
    }

    @Override
    public void map(AtomFeed feed, Resource resource, Patient emrPatient, Encounter newEmrEncounter) {
        FamilyHistory familyHistory = (FamilyHistory) resource;
        Obs familyHistoryObs = new Obs();
        familyHistoryObs.setConcept(conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_FAMILY_HISTORY));
        mapRelationships(familyHistoryObs, familyHistory.getRelation());
        newEmrEncounter.addObs(familyHistoryObs);
    }

    private void mapRelationships(Obs familyHistoryObs, List<FamilyHistory.FamilyHistoryRelationComponent> relations) {
        for (FamilyHistory.FamilyHistoryRelationComponent relation : relations) {
            Obs personObs = new Obs();
            personObs.setConcept(conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_PERSON));
            mapRelation(personObs, relation);
            familyHistoryObs.addGroupMember(personObs);
        }
    }

    private void mapRelation(Obs personObs, FamilyHistory.FamilyHistoryRelationComponent relation) {
        personObs.addGroupMember(setBornOnObs(relation));
        personObs.addGroupMember(mapRelationship(relation.getRelationship().getCoding().get(0).getCodeSimple()));
        for (FamilyHistory.FamilyHistoryRelationConditionComponent component : relation.getCondition()) {
            personObs.addGroupMember(mapRelationCondition(component));
        }
    }

    private Obs mapRelationCondition(FamilyHistory.FamilyHistoryRelationConditionComponent component) {
        Obs result = new Obs();
        result.setConcept(conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_RELATIONSHIP_CONDITION));
        mapOnsetDate(result, (Age) component.getOnset());
        mapNotes(result, component);
        mapCondition(component, result);
        return result;
    }

    private void mapCondition(FamilyHistory.FamilyHistoryRelationConditionComponent component, Obs result) {
        Obs value = new Obs();
        Concept answerConcept = conceptService.getConceptByUuid(idMappingsRepository.findByExternalId(component.getType().getCoding().get(0).getCodeSimple()).getInternalId());
        value.setConcept(conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_RELATIONSHIP_DIAGNOSIS));
        value.setValueCoded(answerConcept);
        result.addGroupMember(value);
    }

    private void mapNotes(Obs result, FamilyHistory.FamilyHistoryRelationConditionComponent component) {
        Obs notes = new Obs();
        Concept onsetDateConcept = conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_RELATIONSHIP_NOTES);
        notes.setConcept(onsetDateConcept);
        notes.setValueText(component.getNoteSimple());
        result.addGroupMember(notes);
    }

    private void mapOnsetDate(Obs result, Age onset) {
        Obs ageValue = new Obs();
        Concept onsetDateConcept = conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_ONSET_AGE);
        ageValue.setConcept(onsetDateConcept);
        ageValue.setValueNumeric(onset.getValue().getValue().doubleValue());
        result.addGroupMember(ageValue);
    }


    private Obs mapRelationship(String code) {
        Obs result = new Obs();
        if (StringUtils.isNotBlank(code)) {
            IdMapping mapping = idMappingsRepository.findByExternalId(code);
            result.setConcept(conceptService.getConceptByUuid(mapping.getInternalId()));
        }
        return result;
    }

    private Obs setBornOnObs(FamilyHistory.FamilyHistoryRelationComponent relation) {
        Obs bornOnObs = new Obs();
        Concept bornOnConcept = conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_BORN_ON);
        bornOnObs.setValueDate(((org.hl7.fhir.instance.model.Date) relation.getBorn()).getValue().toCalendar().getTime());
        bornOnObs.setConcept(bornOnConcept);
        return bornOnObs;
    }
}
