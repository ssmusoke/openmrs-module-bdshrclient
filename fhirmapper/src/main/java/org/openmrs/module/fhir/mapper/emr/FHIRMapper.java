package org.openmrs.module.fhir.mapper.emr;

import org.hl7.fhir.instance.model.AtomEntry;
import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.Composition;
import org.hl7.fhir.instance.model.Resource;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
public class FHIRMapper {

    @Autowired
    FHIREncounterMapper fhirEncounterMapper;

    @Autowired
    private List<FHIRResource> fhirResources;

    private HashMap<String, String> processedList;

    public Encounter map(Patient emrPatient, AtomFeed feed) throws ParseException {
        processedList = new HashMap<String, String>();

        Composition composition = FHIRFeedHelper.getComposition(feed);
        final org.hl7.fhir.instance.model.Encounter encounter = FHIRFeedHelper.getEncounter(feed);
        org.openmrs.Encounter newEmrEncounter = fhirEncounterMapper.map(encounter, composition.getDateSimple().toString(), emrPatient);

        for (AtomEntry<? extends Resource> atomEntry : feed.getEntryList()) {
            final Resource resource = atomEntry.getResource();
            for (FHIRResource fhirResource : fhirResources) {
                if (fhirResource.canHandle(resource)) {
                    fhirResource.map(feed, resource, emrPatient, newEmrEncounter, processedList);
                }
            }
        }
        return newEmrEncounter;
    }
}
