package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.model.dstu2.composite.AgeDt;
import ca.uhn.fhir.model.dstu2.composite.AnnotationDt;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.resource.FamilyMemberHistory;
import ca.uhn.fhir.model.dstu2.valueset.FamilyHistoryStatusEnum;
import ca.uhn.fhir.model.primitive.DateDt;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.module.fhir.MRSProperties;
import org.openmrs.module.fhir.mapper.model.*;
import org.openmrs.module.fhir.utils.CodeableConceptService;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.openmrs.module.fhir.utils.TrValueSetType;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.openmrs.module.fhir.FHIRProperties.UCUM_UNIT_FOR_YEARS;
import static org.openmrs.module.fhir.FHIRProperties.UCUM_URL;
import static org.openmrs.module.fhir.MRSProperties.*;

@Component
public class FamilyMemberHistoryMapper implements EmrObsResourceHandler {

    @Autowired
    private CodeableConceptService codeableConceptService;

    @Autowired
    private OMRSConceptLookup omrsConceptLookup;

    @Override
    public boolean canHandle(Obs observation) {
        CompoundObservation obs = new CompoundObservation(observation);
        return obs.isOfType(ObservationType.FAMILY_HISTORY);
    }

    @Override
    public List<FHIRResource> map(Obs obs, FHIREncounter fhirEncounter, SystemProperties systemProperties) {
        List<FHIRResource> fhirResources = new ArrayList<>();
        for (Obs person : obs.getGroupMembers()) {
            FamilyMemberHistory familyMemberHistory = createFamilyMemberHistory(person, fhirEncounter, systemProperties);
            fhirResources.add(new FHIRResource("Family Member History", familyMemberHistory.getIdentifier(), familyMemberHistory));
        }
        return fhirResources;
    }

    private FamilyMemberHistory createFamilyMemberHistory(Obs person, FHIREncounter fhirEncounter, SystemProperties systemProperties) {
        FamilyMemberHistory familyMemberHistory = new FamilyMemberHistory();
        familyMemberHistory.setPatient(fhirEncounter.getPatient());
        familyMemberHistory.setStatus(FamilyHistoryStatusEnum.PARTIAL);
        String familyMemberHistoryId = new EntityReference().build(Obs.class, systemProperties, person.getUuid());
        familyMemberHistory.addIdentifier().setValue(familyMemberHistoryId);
        familyMemberHistory.setId(familyMemberHistoryId);

        mapRelationship(familyMemberHistory, person, systemProperties);
        mapBornDate(familyMemberHistory, person);
        mapRelationshipConditions(familyMemberHistory, person);
        return familyMemberHistory;
    }

    private void mapRelationshipConditions(FamilyMemberHistory familyMemberHistory, Obs person) {
        List<Obs> familyMemberConditionObservations = new CompoundObservation(person).getAllMemberObsForConceptName(MRSProperties.MRS_CONCEPT_NAME_RELATIONSHIP_CONDITION);
        for (Obs familyMemberConditionObs : familyMemberConditionObservations) {
            FamilyMemberHistory.Condition familyMemberCondition = familyMemberHistory.addCondition();
            CompoundObservation familyMemberConditonCompoundObs = new CompoundObservation(familyMemberConditionObs);
            mapConditionOnsetAge(familyMemberCondition, familyMemberConditonCompoundObs);
            mapConditionNotes(familyMemberCondition, familyMemberConditonCompoundObs);
            mapConditionDiagnosis(familyMemberCondition, familyMemberConditonCompoundObs);
        }
    }

    private void mapConditionDiagnosis(FamilyMemberHistory.Condition familyMemberCondition, CompoundObservation familyMemberConditonCompoundObs) {
        Obs familyMemberConditionDiagnosisObs = familyMemberConditonCompoundObs.getMemberObsForConceptName(MRS_CONCEPT_NAME_RELATIONSHIP_DIAGNOSIS);
        final CodeableConceptDt codeableConcept = getCodeableConceptFromObs(familyMemberConditionDiagnosisObs);
        if (null != codeableConcept) {
            familyMemberCondition.setCode(codeableConcept);
        }
    }

    private void mapConditionNotes(FamilyMemberHistory.Condition familyMemberCondition, CompoundObservation familyMemberConditonCompoundObs) {
        Obs familyMemberConditionNotes = familyMemberConditonCompoundObs.getMemberObsForConceptName(MRSProperties.MRS_CONCEPT_NAME_RELATIONSHIP_NOTES);
        if (familyMemberConditionNotes != null && StringUtils.isNotEmpty(familyMemberConditionNotes.getValueText())) {
            familyMemberCondition.setNote(new AnnotationDt().setText(familyMemberConditionNotes.getValueText()));
        }
    }

    private void mapConditionOnsetAge(FamilyMemberHistory.Condition familyMemberCondition, CompoundObservation familyMemberConditonCompoundObs) {
        Obs onsetAgeObs = familyMemberConditonCompoundObs.getMemberObsForConceptName(MRS_CONCEPT_NAME_ONSET_AGE);
        if (onsetAgeObs != null) {
            AgeDt age = new AgeDt();
            age.setValue(onsetAgeObs.getValueNumeric());
            age.setUnit(UCUM_UNIT_FOR_YEARS);
            age.setSystem(UCUM_URL);
            familyMemberCondition.setOnset(age);
        }
    }

    private void mapBornDate(FamilyMemberHistory familyMemberHistory, Obs person) {
        Obs bornOnObs = new CompoundObservation(person).getMemberObsForConceptName(MRS_CONCEPT_NAME_BORN_ON);
        if (bornOnObs != null) {
            DateDt bornOn = new DateDt();
            bornOn.setValue(bornOnObs.getValueDate(), TemporalPrecisionEnum.DAY);
            familyMemberHistory.setBorn(bornOn);
        }
    }

    private void mapRelationship(FamilyMemberHistory familyMemberHistory, Obs person, SystemProperties systemProperties) {
        Concept relationshipTypeConcept = omrsConceptLookup.findTRConceptOfType(TrValueSetType.RELATIONSHIP_TYPE);
        Obs relationshipTypeObs = new CompoundObservation(person).getMemberObsForConcept(relationshipTypeConcept);
        Concept relationshipConcept = relationshipTypeObs.getValueCoded();
        CodeableConceptDt codeableConcept = codeableConceptService.getTRValueSetCodeableConcept(relationshipConcept, TrValueSetType.RELATIONSHIP_TYPE.getTrPropertyValueSetUrl(systemProperties));
        familyMemberHistory.setRelationship(codeableConcept);
    }

    private CodeableConceptDt getCodeableConceptFromObs(Obs obs) {
        Concept valueCoded = obs.getValueCoded();
        if (null != valueCoded) {
            CodeableConceptDt concept = codeableConceptService.addTRCoding(valueCoded);
            if (CollectionUtils.isEmpty(concept.getCoding())) {
                CodingDt coding = concept.addCoding();
                coding.setDisplay(valueCoded.getName().getName());
            }
            return concept;
        }
        return null;
    }
}
