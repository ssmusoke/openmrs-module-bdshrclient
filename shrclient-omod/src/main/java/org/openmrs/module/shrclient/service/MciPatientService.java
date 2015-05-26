package org.openmrs.module.shrclient.service;

import org.openmrs.Concept;
import org.openmrs.module.shrclient.model.Patient;
import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;
import org.openmrs.api.OpenmrsService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Transactional
public interface MciPatientService extends OpenmrsService {
     org.openmrs.Patient createOrUpdatePatient(Patient mciPatient, Map<String, Concept> conceptCache);
     org.openmrs.PatientIdentifier generateIdentifier();
     void createOrUpdateEncounters(org.openmrs.Patient emrPatient, List<EncounterBundle> bundles, String healthId, Map<String, Concept> conceptCache);
     void createOrUpdateEncounter(org.openmrs.Patient emrPatient, EncounterBundle encounterBundle, String healthId, Map<String, Concept> conceptCache) throws Exception;
     Concept getCauseOfDeath(org.openmrs.Patient emrPatient, Map<String, Concept> conceptCache);
}
