package org.openmrs.module.fhir.mapper.emr;

import org.hl7.fhir.instance.model.CodeableConcept;
import org.hl7.fhir.instance.model.Coding;
import org.hl7.fhir.instance.model.Condition;
import org.hl7.fhir.instance.model.DateTime;
import org.hl7.fhir.instance.model.Resource;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.mapper.FHIRProperties;
import org.openmrs.module.fhir.mapper.MRSProperties;
import org.openmrs.module.fhir.utils.OMRSHelpers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Component
public class FHIRChiefComplaintConditionMapper implements FHIRResource{

    @Autowired
    ConceptService conceptService;
    private static final int CONVERTION_PARAMETER_FOR_MINUTES = (60 * 1000);

    @Override
    public boolean handles(Resource resource) {
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
    public void map(Resource resource, Patient emrPatient, Encounter newEmrEncounter) {
        Condition condition = (Condition) resource;

        Concept historyAndExaminationConcept = conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_HISTORY_AND_EXAMINATION);
        final Concept chiefComplaintDataConcept = conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_CHIEF_COMPLAINT_DATA);
        final Concept chiefComplaintConcept = conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_CHIEF_COMPLAINT);
        final Concept chiefComplaintDurationConcept = conceptService.getConceptByName(MRSProperties.MRS_CONCEPT_NAME_CHIEF_COMPLAINT_DURATION);

        Obs chiefComplaintObs = new Obs();
        chiefComplaintObs.setConcept(chiefComplaintConcept);
        Concept conceptAnswer = identifyComplaintConcept(condition);
        chiefComplaintObs.setValueCoded(conceptAnswer);

        Obs chiefComplaintDurationObs = new Obs();
        chiefComplaintDurationObs.setConcept(chiefComplaintDurationConcept);

        chiefComplaintDurationObs.setValueNumeric(getComplaintDuration(condition));

        Obs chiefComplaintDataObs = new Obs();
        chiefComplaintDataObs.setConcept(chiefComplaintDataConcept);
        chiefComplaintDataObs.addGroupMember(chiefComplaintObs);
        chiefComplaintDataObs.addGroupMember(chiefComplaintDurationObs);

        Obs historyExaminationObs = new Obs();
        historyExaminationObs.setConcept(historyAndExaminationConcept);
        historyExaminationObs.addGroupMember(chiefComplaintDataObs);
        newEmrEncounter.addObs(historyExaminationObs);
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

    private Concept identifyComplaintConcept(Condition condition) {
        CodeableConcept codeableConcept = condition.getCode();
        List<Coding> codeList = codeableConcept.getCoding();

        if ((codeList != null) && !codeList.isEmpty()) {
            Coding coding = codeList.get(0);
            String complaintCode = coding.getCodeSimple();
            String systemSimple = coding.getSystemSimple();
            String complaintName = coding.getDisplaySimple();
            return OMRSHelpers.identifyConceptFromReferenceCodes(complaintCode, systemSimple, complaintName, conceptService);
        }
        return null;
    }
}
