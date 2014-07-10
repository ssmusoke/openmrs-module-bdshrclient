package org.bahmni.module.shrclient.mapper;


import org.hl7.fhir.instance.model.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.api.EncounterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class FHIREncounterMapper {

    @Autowired
    EncounterService encounterService;

    public org.openmrs.Encounter map(Encounter fhirEncounter, String date) throws ParseException {
        org.openmrs.Encounter emrEncounter = new org.openmrs.Encounter();
        final SimpleDateFormat ISODateFomat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        Date encounterDate = ISODateFomat.parse(date);
        emrEncounter.setEncounterDatetime(encounterDate);
        emrEncounter.setUuid(fhirEncounter.getIdentifier().get(0).getValueSimple());
        final String encounterTypeName = fhirEncounter.getType().get(0).getTextSimple();
        final EncounterType encounterType = encounterService.getEncounterType(encounterTypeName);
        emrEncounter.setEncounterType(encounterType);
        return emrEncounter;
    }
}
