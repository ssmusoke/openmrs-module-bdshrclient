package org.openmrs.module.shrclient.service;

import org.openmrs.Patient;
import org.openmrs.module.shrclient.web.controller.dto.EncounterEvent;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface EMREncounterService {
    public void createOrUpdateEncounters(Patient emrPatient, List<EncounterEvent> encounterEvents);
    public void createOrUpdateEncounter(Patient emrPatient, EncounterEvent encounterEvent) throws Exception;
}
