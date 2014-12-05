package org.openmrs.module.fhir.mapper.emr;

import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.Composition;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;

import static org.openmrs.module.fhir.utils.FHIRFeedHelper.getComposition;
import static org.openmrs.module.fhir.utils.FHIRFeedHelper.getEncounter;

@Component
public class FHIRMapper {

    @Autowired
    FHIREncounterMapper fhirEncounterMapper;

    public Encounter map(Patient emrPatient, AtomFeed feed) throws ParseException {
        Composition composition = getComposition(feed);
        final org.hl7.fhir.instance.model.Encounter encounter = getEncounter(feed);
        org.openmrs.Encounter newEmrEncounter = fhirEncounterMapper.map(encounter, composition.getDateSimple().toString(), emrPatient, feed);
        return newEmrEncounter;
    }
}
