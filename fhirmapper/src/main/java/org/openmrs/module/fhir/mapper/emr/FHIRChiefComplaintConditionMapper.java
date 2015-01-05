package org.openmrs.module.fhir.mapper.emr;

import org.apache.commons.collections.CollectionUtils;
import org.hl7.fhir.instance.model.*;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.mapper.FHIRProperties;
import org.openmrs.module.fhir.mapper.MRSProperties;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

@Component
public class FHIRChiefComplaintConditionMapper implements FHIRResourceMapper {

    @Autowired
    private ConceptService conceptService;
    @Autowired
    private OMRSConceptLookup omrsConceptLookup;

    private static final int CONVERTION_PARAMETER_FOR_MINUTES = (60 * 1000);

    @Override
    public boolean canHandle(Resource resource) {
        if (resource instanceof Condition) {
            final List<Coding> resourceCoding = ((Condition) resource).getCategory().getCoding();
            if (resourceCoding == null || resourceCoding.isEmpty()) {
                return false;
            }
            return resourceCoding.get(0).getCodeSimple().equalsIgnoreCase(FHIRProperties.FHIR_CONDITION_CODE_CHIEF_COMPLAINT);
        }
        return false;
    }

    @Override
    public void map(AtomFeed feed, Resource resource, Patient emrPatient, Encounter newEmrEncounter, Map<String, List<String>> processedList) {
        Condition condition = (Condition) resource;

        if (isAlreadyProcessed(condition, processedList))
            return;
        Concept historyAndExaminationConcept = conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_HISTORY_AND_EXAMINATION);
        Concept chiefComplaintDataConcept = conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_CHIEF_COMPLAINT_DATA);
        Concept chiefComplaintDurationConcept = conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_CHIEF_COMPLAINT_DURATION);

        Obs chiefComplaintObs = new Obs();
        List<Coding> conditionCoding = condition.getCode().getCoding();
        Concept conceptAnswer = omrsConceptLookup.findConcept(conditionCoding);
        if (conceptAnswer == null) {
            if(CollectionUtils.isNotEmpty(conditionCoding)) {
                String displayName = conditionCoding.get(0).getDisplaySimple();
                Concept nonCodedChiefComplaintConcept = conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_NON_CODED_CHIEF_COMPLAINT);
                chiefComplaintObs.setConcept(nonCodedChiefComplaintConcept);
                chiefComplaintObs.setValueText(displayName);
            }
            else {
                return;
            }
        } else {
            Concept chiefComplaintConcept = conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_CHIEF_COMPLAINT);
            chiefComplaintObs.setConcept(chiefComplaintConcept);
            chiefComplaintObs.setValueCoded(conceptAnswer);
        }

        Obs chiefComplaintDurationObs = new Obs();
        chiefComplaintDurationObs.setConcept(chiefComplaintDurationConcept);

        chiefComplaintDurationObs.setValueNumeric(getComplaintDuration(condition));

        Obs chiefComplaintDataObs = new Obs();
        chiefComplaintDataObs.setConcept(chiefComplaintDataConcept);
        chiefComplaintDataObs.addGroupMember(chiefComplaintObs);
        chiefComplaintDataObs.addGroupMember(chiefComplaintDurationObs);

        //TODO : don't create history and examination obs if it already exists in encounter
        Obs historyExaminationObs = new Obs();
        historyExaminationObs.setConcept(historyAndExaminationConcept);
        historyExaminationObs.addGroupMember(chiefComplaintDataObs);
        newEmrEncounter.addObs(historyExaminationObs);

        processedList.put(condition.getIdentifier().get(0).getValueSimple(), Arrays.asList(chiefComplaintDataObs.getUuid()));
    }

    private boolean isAlreadyProcessed(Condition condition, Map<String, List<String>> processedList) {
        return processedList.containsKey(condition.getIdentifier().get(0).getValueSimple());
    }

    private Double getComplaintDuration(Condition condition) {
        final SimpleDateFormat ISODateFomat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        DateTime onsetDateTime = (DateTime) condition.getOnset();
        Date onsetDate = null;
        Date dateAsserted = null;
        try {
            onsetDate = ISODateFomat.parse(onsetDateTime.getValue().toString());
            dateAsserted = ISODateFomat.parse(condition.getDateAsserted().getValue().toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        long differenceInMinutes = (dateAsserted.getTime() - onsetDate.getTime()) / CONVERTION_PARAMETER_FOR_MINUTES;
        return Double.valueOf(differenceInMinutes);
    }
}
