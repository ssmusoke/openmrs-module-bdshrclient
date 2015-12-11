package org.openmrs.module.shrclient.service;

import org.apache.log4j.Logger;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
import org.openmrs.api.ObsService;
import org.openmrs.module.fhir.utils.GlobalPropertyLookUpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.openmrs.module.fhir.MRSProperties.*;

@Service
public class EMRPatientDeathService {

    private static final Logger logger = Logger.getLogger(EMRPatientDeathService.class);

    private ObsService obsService;
    private ConceptService conceptService;
    private GlobalPropertyLookUpService globalPropertyLookUpService;

    @Autowired
    public EMRPatientDeathService(ObsService obsService, ConceptService conceptService, GlobalPropertyLookUpService globalPropertyLookUpService) {
        this.obsService = obsService;
        this.conceptService = conceptService;
        this.globalPropertyLookUpService = globalPropertyLookUpService;
    }

    public Concept getCauseOfDeath(org.openmrs.Patient emrPatient) {
        String causeOfDeathConceptId = globalPropertyLookUpService.getGlobalPropertyValue(GLOBAL_PROPERTY_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH);
        Concept unspecifiedCauseOfDeathConcept = causeOfDeathConceptId != null ? conceptService.getConcept(Integer.parseInt(causeOfDeathConceptId)) : conceptService.getConceptByName(TR_CONCEPT_CAUSE_OF_DEATH);
        String unspecifiedCauseOfDeathConceptId = globalPropertyLookUpService.getGlobalPropertyValue(GLOBAL_PROPERTY_CONCEPT_CAUSE_OF_DEATH);
        Concept causeOfDeathConcept = unspecifiedCauseOfDeathConceptId != null ? conceptService.getConcept(Integer.parseInt(unspecifiedCauseOfDeathConceptId)) : conceptService.getConceptByName(TR_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH);
        String error = null;
        if (emrPatient.isDead() && unspecifiedCauseOfDeathConcept == null) {
            error = String.format("Global Property %s is not set & Concept with name %s is not found", GLOBAL_PROPERTY_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH, TR_CONCEPT_UNSPECIFIED_CAUSE_OF_DEATH);
            logger.error(error);
            throw new RuntimeException(error);
        }
        if (emrPatient.isDead() && causeOfDeathConcept == null) {
            error = String.format("Global Property %s is not set & Concept with name %s is not found", GLOBAL_PROPERTY_CONCEPT_CAUSE_OF_DEATH, TR_CONCEPT_CAUSE_OF_DEATH);
            logger.error(error);
            throw new RuntimeException(error);
        }
        if (emrPatient.isDead() && emrPatient.getCauseOfDeath() != null && emrPatient.getCauseOfDeath() != unspecifiedCauseOfDeathConcept) {
            return emrPatient.getCauseOfDeath();
        }
        Concept causeOfDeath = unspecifiedCauseOfDeathConcept;
        if (emrPatient.getId() != null) {
            List<Obs> obsForCauseOfDeath = obsService.getObservationsByPersonAndConcept(emrPatient, causeOfDeathConcept);
            if ((obsForCauseOfDeath != null) && !obsForCauseOfDeath.isEmpty()) {
                for (Obs obs : obsForCauseOfDeath) {
                    if (!obs.isVoided()) {
                        causeOfDeath = obs.getValueCoded();
                    }
                }
            }
        }
        return causeOfDeath;
    }

}
