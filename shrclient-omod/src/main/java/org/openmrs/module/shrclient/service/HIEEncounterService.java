package org.openmrs.module.shrclient.service;

import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface HIEEncounterService {
    void createOrUpdateEncounters(org.openmrs.Patient emrPatient, List<EncounterBundle> bundles, String healthId);

    void createOrUpdateEncounter(org.openmrs.Patient emrPatient, EncounterBundle encounterBundle, String healthId) throws Exception;
}
