package org.openmrs.module.bdshrclient.service;

import org.openmrs.api.OpenmrsService;
import org.openmrs.module.bdshrclient.model.Patient;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface MciPatientService extends OpenmrsService {
     org.openmrs.Patient createOrUpdatePatient(Patient mciPatient);
     org.openmrs.PatientIdentifier generateIdentifier();
}
