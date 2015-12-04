package org.openmrs.module.shrclient.service;

import org.openmrs.Concept;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface HIEPatientDeathService {
    Concept getCauseOfDeath(org.openmrs.Patient emrPatient);

}
