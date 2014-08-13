package org.openmrs.module.fhir.mapper.bundler;

import org.apache.commons.collections.CollectionUtils;
import org.hl7.fhir.instance.model.Age;
import org.hl7.fhir.instance.model.CodeableConcept;
import org.hl7.fhir.instance.model.Coding;
import org.hl7.fhir.instance.model.Date;
import org.hl7.fhir.instance.model.DateAndTime;
import org.hl7.fhir.instance.model.Decimal;
import org.hl7.fhir.instance.model.Encounter;
import org.hl7.fhir.instance.model.FamilyHistory;
import org.openmrs.Concept;
import org.openmrs.Obs;
import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_CONCEPT_NAME_BORN_ON;
import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_CONCEPT_NAME_FAMILY_HISTORY;
import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_CONCEPT_NAME_ONSET_AGE;
import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_CONCEPT_NAME_RELATIONSHIP;
import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_CONCEPT_NAME_RELATIONSHIP_CONDITION;
import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_CONCEPT_NAME_RELATIONSHIP_DIAGNOSIS;
import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_CONCEPT_NAME_RELATIONSHIP_NOTES;
import org.openmrs.module.fhir.mapper.bundler.condition.ObservationValueMapper;
import static org.openmrs.module.fhir.utils.FHIRFeedHelper.addReferenceCodes;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class FamilyHistoryMapper implements EmrResourceHandler {

    @Autowired
    IdMappingsRepository idMappingsRepository;

    @Autowired
    ObservationValueMapper observationValueMapper;

    @Override
    public boolean handles(Obs observation) {
        return MRS_CONCEPT_NAME_FAMILY_HISTORY.equals(observation.getConcept().getName().getName());
    }

    @Override
    public List<EmrResource> map(Obs obs, Encounter fhirEncounter) {
        List<EmrResource> emrResources = new ArrayList<EmrResource>();
        for (Obs person : obs.getGroupMembers()) {
            FamilyHistory familyHistory = createFamilyHistory(person, fhirEncounter);
            emrResources.add(new EmrResource(obs.getConcept().getName().getName(), familyHistory.getIdentifier(), familyHistory));
        }
        return emrResources;
    }

    private FamilyHistory createFamilyHistory(Obs person, Encounter fhirEncounter) {
        FamilyHistory familyHistory = new FamilyHistory();
        familyHistory.setSubject(fhirEncounter.getSubject());
        familyHistory.addIdentifier().setValueSimple(person.getUuid());
        FamilyHistory.FamilyHistoryRelationComponent familyHistoryRelationComponent = familyHistory.addRelation();
        for (Obs member : person.getGroupMembers()) {
            if (MRS_CONCEPT_NAME_RELATIONSHIP.equalsIgnoreCase(member.getConcept().getName().getName())) {
                mapRelationship(familyHistoryRelationComponent, member);
            } else if (MRS_CONCEPT_NAME_BORN_ON.equalsIgnoreCase(member.getConcept().getName().getName())) {
                mapBornDate(familyHistoryRelationComponent, member);
            } else if (MRS_CONCEPT_NAME_RELATIONSHIP_CONDITION.equalsIgnoreCase(member.getConcept().getName().getName())) {
                mapRelationshipCondition(familyHistoryRelationComponent, member);
            }
        }
        return familyHistory;
    }

    private void mapRelationshipCondition(FamilyHistory.FamilyHistoryRelationComponent familyHistoryRelationComponent, Obs relationCondition) {
        FamilyHistory.FamilyHistoryRelationConditionComponent conditionComponent = familyHistoryRelationComponent.addCondition();
        for (Obs member : relationCondition.getGroupMembers()) {
            if (MRS_CONCEPT_NAME_ONSET_AGE.equalsIgnoreCase(member.getConcept().getName().getName())) {
                Age age = new Age();
                Decimal decimal = new Decimal();
                decimal.setValue(new BigDecimal(member.getValueNumeric()));
                age.setValue(decimal);
                conditionComponent.setOnset(age);
            } else if (MRS_CONCEPT_NAME_RELATIONSHIP_NOTES.equalsIgnoreCase(member.getConcept().getName().getName())) {
                conditionComponent.setNoteSimple(member.getValueText());
            } else if (MRS_CONCEPT_NAME_RELATIONSHIP_DIAGNOSIS.equalsIgnoreCase(member.getConcept().getName().getName())) {
                final CodeableConcept codeableConcept = readValue(member);
                if (null != codeableConcept) {
                    conditionComponent.setType(codeableConcept);
                }
            }
        }
    }

    private void mapBornDate(FamilyHistory.FamilyHistoryRelationComponent relationComponent, Obs member) {
        Date bornOn = new Date();
        bornOn.setValue(new DateAndTime(member.getValueDate()));
        relationComponent.setBorn(bornOn);
    }

    private void mapRelationship(FamilyHistory.FamilyHistoryRelationComponent relationComponent, Obs relationship) {
        final CodeableConcept codeableConcept = readValue(relationship);
        if (null != codeableConcept) {
            relationComponent.setRelationship(codeableConcept);
        }
    }

    private CodeableConcept readValue(Obs obs) {
        Concept valueCoded = obs.getValueCoded();
        if (null != valueCoded) {
            CodeableConcept concept = addReferenceCodes(valueCoded, idMappingsRepository);
            if (CollectionUtils.isEmpty(concept.getCoding())) {
                Coding coding = concept.addCoding();
                coding.setDisplaySimple(valueCoded.getName().getName());
            }
            return concept;
        }
        return null;
    }
}
