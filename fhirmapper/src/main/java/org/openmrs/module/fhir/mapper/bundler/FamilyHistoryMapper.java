package org.openmrs.module.fhir.mapper.bundler;

import org.apache.commons.collections.CollectionUtils;
import org.hl7.fhir.instance.model.*;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.Obs;
import org.openmrs.module.fhir.mapper.FHIRProperties;
import org.openmrs.module.fhir.mapper.bundler.condition.ObservationValueMapper;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.openmrs.module.fhir.mapper.FHIRProperties.UCUM_UNIT_FOR_YEARS;
import static org.openmrs.module.fhir.mapper.FHIRProperties.UCUM_URL;
import static org.openmrs.module.fhir.mapper.MRSProperties.*;
import static org.openmrs.module.fhir.utils.FHIRFeedHelper.addReferenceCodes;

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
                age.setUnitsSimple(UCUM_UNIT_FOR_YEARS);
                age.setSystemSimple(UCUM_URL);
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
        CodeableConcept codeableConcept = new CodeableConcept();
        String name = relationship.getValueCoded().getName().getName();
        FHIRFeedHelper.addFHIRCoding(codeableConcept, name, FHIRProperties.FHIR_SYSTEM_RELATIONSHIP_ROLE, new ArrayList<ConceptName>(relationship.getValueCoded().getShortNames()).get(0).getName());
        relationComponent.setRelationship(codeableConcept);
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
