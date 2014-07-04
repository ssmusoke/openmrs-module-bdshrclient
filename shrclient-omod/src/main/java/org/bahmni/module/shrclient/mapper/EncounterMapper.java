package org.bahmni.module.shrclient.mapper;


import org.bahmni.module.shrclient.model.Encounter;

public class EncounterMapper {
    public Encounter map(org.openmrs.Encounter openMrsEncounter) {
        return new Encounter();
    }
}
