package org.openmrs.module.fhir.mapper.emr;

import org.apache.commons.lang.StringUtils;
import org.hl7.fhir.instance.model.*;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import static org.openmrs.module.fhir.mapper.MRSProperties.*;

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
        familyHistoryObs.setConcept(conceptService.getConceptByName(MRS_CONCEPT_NAME_FAMILY_HISTORY));
        mapRelationships(familyHistoryObs, familyHistory.getRelation());
        newEmrEncounter.addObs(familyHistoryObs);
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
        mapOnsetDate(result, (Age) component.getOnset());
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
        Obs notes = new Obs();
        Concept onsetDateConcept = conceptService.getConceptByName(MRS_CONCEPT_NAME_RELATIONSHIP_NOTES);
        notes.setConcept(onsetDateConcept);
        notes.setValueText(component.getNoteSimple());
        result.addGroupMember(notes);
    }

    private void mapOnsetDate(Obs result, Age onset) {
        Obs ageValue = new Obs();
        Concept onsetDateConcept = conceptService.getConceptByName(MRS_CONCEPT_NAME_ONSET_AGE);
        ageValue.setConcept(onsetDateConcept);
        ageValue.setValueNumeric(onset.getValue().getValue().doubleValue());
        result.addGroupMember(ageValue);
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
        Obs bornOnObs = new Obs();
        java.util.Date observationValue = null;
        Concept bornOnConcept = conceptService.getConceptByName(MRS_CONCEPT_NAME_BORN_ON);
        String date = ((Date) relation.getBorn()).getValue().toString();
        final SimpleDateFormat ISODateFomat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        try {
            observationValue = ISODateFomat.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        bornOnObs.setValueDate(observationValue);
        bornOnObs.setConcept(bornOnConcept);
        return bornOnObs;
    }
}
