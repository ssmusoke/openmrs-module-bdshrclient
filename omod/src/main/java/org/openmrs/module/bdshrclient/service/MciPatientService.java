package org.openmrs.module.bdshrclient.service;

import org.openmrs.Patient;
import org.openmrs.api.OpenmrsService;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface MciPatientService extends OpenmrsService {
     Patient createOrUpdatePatient(org.openmrs.module.bdshrclient.model.Patient mciPatient);
     org.openmrs.PatientIdentifier generateIdentifier();
}
