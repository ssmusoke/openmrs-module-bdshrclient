package org.openmrs.module.fhir.mapper.bundler;

import org.apache.commons.collections.CollectionUtils;
import org.hl7.fhir.instance.model.*;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.Obs;
import org.openmrs.module.fhir.mapper.FHIRProperties;
import org.openmrs.module.fhir.mapper.bundler.condition.ObservationValueMapper;
import org.openmrs.module.fhir.mapper.model.CompoundObservation;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.mapper.model.ObservationType;
import org.openmrs.module.fhir.utils.CodableConceptService;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.openmrs.module.fhir.mapper.FHIRProperties.UCUM_UNIT_FOR_YEARS;
import static org.openmrs.module.fhir.mapper.FHIRProperties.UCUM_URL;
import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_CONCEPT_NAME_BORN_ON;
import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_CONCEPT_NAME_ONSET_AGE;
import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_CONCEPT_NAME_RELATIONSHIP;
import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_CONCEPT_NAME_RELATIONSHIP_CONDITION;
import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_CONCEPT_NAME_RELATIONSHIP_DIAGNOSIS;
import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_CONCEPT_NAME_RELATIONSHIP_NOTES;

@Component
public class FamilyHistoryMapper implements EmrObsResourceHandler {

    @Autowired
    IdMappingsRepository idMappingsRepository;

    @Autowired
    ObservationValueMapper observationValueMapper;

    @Autowired
    private CodableConceptService codableConceptService;

    @Override
    public boolean canHandle(Obs observation) {
        CompoundObservation obs = new CompoundObservation(observation);
        return obs.isOfType(ObservationType.FAMILY_HISTORY);
    }

    @Override
    public List<FHIRResource> map(Obs obs, Encounter fhirEncounter, SystemProperties systemProperties) {
        List<FHIRResource> FHIRResources = new ArrayList<>();
        for (Obs person : obs.getGroupMembers()) {
            FamilyHistory familyHistory = createFamilyHistory(person, fhirEncounter, systemProperties);
            FHIRResources.add(new FHIRResource(obs.getConcept().getName().getName(), familyHistory.getIdentifier(), familyHistory));
        }
        return FHIRResources;
    }

    private FamilyHistory createFamilyHistory(Obs person, Encounter fhirEncounter, SystemProperties systemProperties) {
        FamilyHistory familyHistory = new FamilyHistory();
        familyHistory.setSubject(fhirEncounter.getSubject());
        familyHistory.addIdentifier().setValueSimple(new EntityReference().build(Obs.class, systemProperties, person.getUuid()));
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

    private String getConceptCode(Concept mrsConcept) {
        Collection<ConceptName> shortNames = mrsConcept.getShortNames();
        return shortNames.isEmpty() ? mrsConcept.getName().getName() : ((ConceptName) shortNames.toArray()[0]).getName();
    }

    private String getConceptDisplay(Concept mrsConcept) {
        return mrsConcept.getName().getName().toLowerCase();
    }

    private void mapRelationship(FamilyHistory.FamilyHistoryRelationComponent relationComponent, Obs relationship) {
        CodeableConcept codeableConcept = new CodeableConcept();
        Concept relationshipConcept = relationship.getValueCoded();
        codableConceptService.addFHIRCoding(
                codeableConcept,
                getConceptCode(relationshipConcept),
                FHIRProperties.FHIR_SYSTEM_RELATIONSHIP_ROLE,
                getConceptDisplay(relationshipConcept));
        relationComponent.setRelationship(codeableConcept);
    }

    private CodeableConcept readValue(Obs obs) {
        Concept valueCoded = obs.getValueCoded();
        if (null != valueCoded) {
            CodeableConcept concept = codableConceptService.addTRCoding(valueCoded, idMappingsRepository);
            if (CollectionUtils.isEmpty(concept.getCoding())) {
                Coding coding = concept.addCoding();
                coding.setDisplaySimple(valueCoded.getName().getName());
            }
            return concept;
        }
        return null;
    }

    //TODO : how do we identify this individual?
    protected ResourceReference getParticipant(Encounter encounter) {
        List<Encounter.EncounterParticipantComponent> participants = encounter.getParticipant();
        if ((participants != null) && !participants.isEmpty()) {
            return participants.get(0).getIndividual();
        }
        return null;
    }
}
