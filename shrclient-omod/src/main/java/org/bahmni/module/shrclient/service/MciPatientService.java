package org.bahmni.module.shrclient.service;

import org.bahmni.module.shrclient.model.Patient;
import org.openmrs.api.OpenmrsService;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface MciPatientService extends OpenmrsService {
     org.openmrs.Patient createOrUpdatePatient(Patient mciPatient);
     org.openmrs.PatientIdentifier generateIdentifier();
}
