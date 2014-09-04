package org.openmrs.module.fhir.mapper.emr;

import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.Composition;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;

@Component
public class FHIRMapper {

    @Autowired
    FHIREncounterMapper fhirEncounterMapper;

    public Encounter map(Patient emrPatient, AtomFeed feed) throws ParseException {
        Composition composition = FHIRFeedHelper.getComposition(feed);
        final org.hl7.fhir.instance.model.Encounter encounter = FHIRFeedHelper.getEncounter(feed);
        org.openmrs.Encounter newEmrEncounter = fhirEncounterMapper.map(encounter, composition.getDateSimple().toString(), emrPatient, feed);
        return newEmrEncounter;
    }
}
