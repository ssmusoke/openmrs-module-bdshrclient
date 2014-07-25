package org.openmrs.module.fhir.mapper.emr;

import org.hl7.fhir.instance.model.AtomEntry;
import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.Coding;
import org.hl7.fhir.instance.model.Composition;
import org.hl7.fhir.instance.model.Condition;
import org.hl7.fhir.instance.model.Resource;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.module.fhir.mapper.FHIRProperties;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class FHIRMapper {

    @Autowired
    FHIREncounterMapper fhirEncounterMapper;

    @Autowired
    private List<FHIRResource> fhirResources = new ArrayList<FHIRResource>();

    public Encounter map(Patient emrPatient, AtomFeed feed, org.hl7.fhir.instance.model.Encounter encounter) throws ParseException {
        Composition composition = FHIRFeedHelper.getComposition(feed);
        org.openmrs.Encounter newEmrEncounter = fhirEncounterMapper.map(encounter, composition.getDateSimple().toString(), emrPatient);

        for (AtomEntry<? extends Resource> atomEntry : feed.getEntryList()) {
            final Resource resource = atomEntry.getResource();
            for (FHIRResource fhirResource : fhirResources) {
                if(fhirResource.handles(resource)) {
                    fhirResource.map(resource, emrPatient, newEmrEncounter);
                }
            }
        }
        return newEmrEncounter;
    }
}
