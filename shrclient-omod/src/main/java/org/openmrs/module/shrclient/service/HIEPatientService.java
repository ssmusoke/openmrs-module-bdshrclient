package org.openmrs.module.shrclient.service;

import org.openmrs.module.shrclient.model.Patient;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface HIEPatientService {
    org.openmrs.Patient createOrUpdatePatient(Patient mciPatient);
}
