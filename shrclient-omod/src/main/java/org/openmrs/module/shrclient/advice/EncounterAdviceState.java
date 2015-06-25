package org.openmrs.module.shrclient.advice;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a hack!
 * This is done so that multiple events is not created for the same encounter during the encounter processing.
 *  EmrEncounterService calls EncounterService multiple times during a transaction. Specifically while creating
 *  orders. This results in creation of multiple encounter events.
 *  because the SHREncounterAdvice intercepts EncounterService.save()
 */
public class EncounterAdviceState {
    private static final ThreadLocal<List<String>> processedEncounters = new ThreadLocal<List<String>>() {
        @Override
        protected List<String> initialValue() {
            return new ArrayList<String>();
        }
    };

    public static void addProcessedEncounter(String encounterId) {
        List<String> encounterIds = processedEncounters.get();
        encounterIds.add(encounterId);
    }
    public static boolean hasAlreadyProcessedEncounter(String encounterId) {
        List<String> encounterIds = processedEncounters.get();
        return encounterIds.contains(encounterId);
    }
}
