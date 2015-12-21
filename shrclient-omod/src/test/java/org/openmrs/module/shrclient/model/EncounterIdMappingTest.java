package org.openmrs.module.shrclient.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EncounterIdMappingTest {

    @Test
    public void shouldSetHealthIdFromUri() {
        EncounterIdMapping encounterIdMapping = new EncounterIdMapping("internalId","externalId","http://shr.com/patients/123456789/encounters/encounter-id",null,null);
        assertEquals("123456789", encounterIdMapping.getHealthId());

    }
}