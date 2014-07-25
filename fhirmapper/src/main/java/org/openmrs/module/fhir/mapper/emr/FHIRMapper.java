package org.openmrs.module.fhir.mapper.emr;

import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.Coding;
import org.hl7.fhir.instance.model.Composition;
import org.hl7.fhir.instance.model.Condition;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.module.fhir.mapper.FHIRProperties;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.List;

@Component
public class FHIRMapper {

    @Autowired
    FHIRDiagnosisConditionsMapper fhirDiagnosisConditionsMapper;

    @Autowired
    FHIRChiefComplaintConditionMapper fhirChiefComplaintConditionMapper;

    @Autowired
    FHIREncounterMapper fhirEncounterMapper;

    public Encounter map(Patient emrPatient, AtomFeed feed, org.hl7.fhir.instance.model.Encounter encounter) throws ParseException {
        Composition composition = FHIRFeedHelper.getComposition(feed);
        org.openmrs.Encounter newEmrEncounter = fhirEncounterMapper.map(encounter, composition.getDateSimple().toString(), emrPatient);

        List<Condition> conditions = FHIRFeedHelper.getConditions(feed);
        for (Condition condition : conditions) {
            final List<Coding> codings = condition.getCategory().getCoding();
            if (!codings.isEmpty()) {
                String codeSimple = codings.get(0).getCodeSimple();
                if (codeSimple.equalsIgnoreCase(FHIRProperties.FHIR_CONDITION_CODE_DIAGNOSIS)) {
                    fhirDiagnosisConditionsMapper.map(emrPatient, newEmrEncounter, condition);
                }
                else if (codeSimple.equalsIgnoreCase(FHIRProperties.FHIR_CONDITION_CODE_CHIEF_COMPLAINT)) {
                    fhirChiefComplaintConditionMapper.map(emrPatient, newEmrEncounter, condition);
                }
            }

        }
        return newEmrEncounter;

    }
}
