package org.openmrs.module.fhir.mapper.bundler;

import org.openmrs.module.fhir.mapper.FHIRProperties;
import org.openmrs.module.fhir.mapper.MRSProperties;
import org.openmrs.module.fhir.utils.*;
import org.hl7.fhir.instance.model.*;
import org.hl7.fhir.instance.model.Date;
import org.hl7.fhir.instance.model.Enumeration;
import org.openmrs.Concept;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.Obs;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component("fhirDiagnosisMapper")
public class DiagnosisMapper implements EmrResourceHandler {

    @Autowired
    private IdMappingsRepository idMappingsRepository;

    private final Map<String,Condition.ConditionStatus> diaConditionStatus = new HashMap<String, Condition.ConditionStatus>();
    private final Map<String,String> diaConditionSeverity = new HashMap<String, String>();
    private final FHIRProperties fhirProperties;

    public DiagnosisMapper() {
        fhirProperties = new FHIRProperties();
        diaConditionStatus.put(MRSProperties.MRS_DIAGNOSIS_STATUS_PRESUMED, Condition.ConditionStatus.provisional);
        diaConditionStatus.put(MRSProperties.MRS_DIAGNOSIS_STATUS_CONFIRMED, Condition.ConditionStatus.confirmed);

        diaConditionSeverity.put(MRSProperties.MRS_DIAGNOSIS_SEVERITY_PRIMARY, FHIRProperties.FHIR_SEVERITY_MODERATE);
        diaConditionSeverity.put(MRSProperties.MRS_DIAGNOSIS_SEVERITY_SECONDARY, FHIRProperties.FHIR_SEVERITY_SEVERE);

    }

    @Override
    public boolean handles(Obs observation) {
        return observation.getConcept().getName().getName().equalsIgnoreCase(MRSProperties.MRS_CONCEPT_NAME_VISIT_DIAGNOSES);
    }

    @Override
    public List<EmrResource> map(Obs obs, Encounter fhirEncounter) {
        List<EmrResource> diagnoses = new ArrayList<EmrResource>();
        diagnoses.add(createFHIRCondition(fhirEncounter, obs));
        return diagnoses;
    }

    private EmrResource createFHIRCondition(Encounter encounter, Obs obs) {
        Condition condition = new Condition();
        condition.setEncounter(encounter.getIndication());
        condition.setSubject(encounter.getSubject());
        condition.setAsserter(getParticipant(encounter));
        condition.setCategory(getDiagnosisCategory());

        final Set<Obs> obsMembers = obs.getGroupMembers(false);
        for (Obs member : obsMembers) {
            Concept memberConcept = member.getConcept();
            if (isCodedDiagnosisObservation(memberConcept)) {
                condition.setCode(getDiagnosisCode(member.getValueCoded()));
            }
            else if (isDiagnosisCertaintyObservation(memberConcept)) {
                condition.setStatus(getConditionStatus(member));
            }
            else if (isDiagnosisOrderObservation(memberConcept)) {
                condition.setSeverity(getDiagnosisSeverity(member.getValueCoded()));
            }
        }

        Date onsetDate = new Date();
        onsetDate.setValue(new DateAndTime(obs.getObsDatetime()));
        condition.setDateAsserted(onsetDate);

        Identifier identifier = condition.addIdentifier();
        identifier.setValueSimple(obs.getUuid());

        return new EmrResource("Diagnosis", condition.getIdentifier(), condition);
    }

    private boolean isDiagnosisOrderObservation(Concept concept) {
        String conceptName = concept.getName().getName();
        return conceptName.equalsIgnoreCase(MRSProperties.MRS_CONCEPT_NAME_DIAGNOSIS_ORDER);
    }

    private boolean isDiagnosisCertaintyObservation(Concept concept) {
        String conceptName = concept.getName().getName();
        return conceptName.equalsIgnoreCase(MRSProperties.MRS_CONCEPT_NAME_DIAGNOSIS_CERTAINTY);
    }

    private boolean isCodedDiagnosisObservation(Concept concept) {
        String conceptName = concept.getName().getName();
        return conceptName.equalsIgnoreCase(MRSProperties.MRS_CONCEPT_NAME_CODED_DIAGNOSIS);
    }

    private ResourceReference getParticipant(Encounter encounter) {
        List<Encounter.EncounterParticipantComponent> participants = encounter.getParticipant();
        if ((participants != null) && !participants.isEmpty()) {
            return participants.get(0).getIndividual();
        }
        return null;
    }

    private Enumeration<Condition.ConditionStatus> getConditionStatus(Obs member) {
        Concept diagnosisStatus = member.getValueCoded();
        Condition.ConditionStatus status = diaConditionStatus.get(diagnosisStatus.getName().getName());
        if (status != null) {
            return new Enumeration<Condition.ConditionStatus>(status);
        } else {
            return new Enumeration<Condition.ConditionStatus>(Condition.ConditionStatus.confirmed);
        }
    }

    private CodeableConcept getDiagnosisCode(Concept obsConcept) {
        //TODO to change to reference term code
        ConceptCoding refCoding = getReferenceCode(obsConcept);
        if(refCoding == null) {
            CodeableConcept codeableConcept = new CodeableConcept();
            Coding coding = codeableConcept.addCoding();
            coding.setDisplaySimple(obsConcept.getName().getName());
        }
        return FHIRFeedHelper.getFHIRCodeableConcept(refCoding.getCode(), refCoding.getSource(), obsConcept.getName().getName());
    }


    private ConceptCoding getReferenceCode(Concept obsConcept) {
        Collection<org.openmrs.ConceptMap> conceptMappings = obsConcept.getConceptMappings();
        for (org.openmrs.ConceptMap mapping : conceptMappings) {
            //TODO right now returning the first matching term
            ConceptReferenceTerm conceptReferenceTerm = mapping.getConceptReferenceTerm();
            ConceptCoding coding = new ConceptCoding();
            coding.setCode(conceptReferenceTerm.getCode());
            coding.setSource(conceptReferenceTerm.getConceptSource().getName());
            return coding;
        }
        ConceptCoding defaultCoding = null;
        //TODO: put in the right URL. To be mapped
        //TODO temporary. To read TR concept URL from mapping
        IdMapping idMapping = idMappingsRepository.findByInternalId(obsConcept.getUuid());
        if(idMapping != null) {
            defaultCoding = new ConceptCoding();
            defaultCoding.setCode(obsConcept.getUuid());
            defaultCoding.setSource(org.openmrs.module.fhir.utils.Constants.TERMINOLOGY_SERVER_CONCEPT_URL + idMapping.getExternalId());
        }
        return defaultCoding;
    }

    private CodeableConcept getDiagnosisSeverity(Concept valueCoded) {
        String severity = diaConditionSeverity.get(valueCoded.getName().getName());
        if(severity == null) {
            severity = FHIRProperties.FHIR_SEVERITY_MODERATE;
        }
        return FHIRFeedHelper.getFHIRCodeableConcept(fhirProperties.getSeverityCode(severity), severity, FHIRProperties.FHIR_CONDITION_SEVERITY_URL);
    }

    private CodeableConcept getDiagnosisCategory() {
        return FHIRFeedHelper.getFHIRCodeableConcept(FHIRProperties.FHIR_CONDITION_CODE_DIAGNOSIS,
                FHIRProperties.FHIR_CONDITION_CATEGORY_URL, FHIRProperties.FHIR_CONDITION_CODE_DIAGNOSIS);
    }
}
