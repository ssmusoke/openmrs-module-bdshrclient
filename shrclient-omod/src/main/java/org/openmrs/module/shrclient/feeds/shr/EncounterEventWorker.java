package org.openmrs.module.shrclient.feeds.shr;

import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;

public interface EncounterEventWorker {
    void process(EncounterBundle encounterBundle);
}
