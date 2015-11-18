package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.IdentifierDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.stereotype.Component;

@Component("observationBuilder")
public class ObservationBuilder {
    public FHIRResource buildObservationResource(Encounter fhirEncounter, SystemProperties systemProperties, String resourceId, String resourceName) {
        Observation fhirObservation = new Observation();
        fhirObservation.setSubject(fhirEncounter.getPatient());
        fhirObservation.setEncounter(new ResourceReferenceDt().setReference(fhirEncounter.getId().getValue()));
        String id = new EntityReference().build(IResource.class, systemProperties, resourceId);
        fhirObservation.setId(id);
        fhirObservation.addIdentifier(new IdentifierDt().setValue(id));
        mapPerformer(fhirEncounter, fhirObservation);
        return buildFhirResource(fhirObservation, resourceName);
    }

    private void mapPerformer(Encounter fhirEncounter, Observation fhirObservation) {
        for (Encounter.Participant participant : fhirEncounter.getParticipant()) {
            ResourceReferenceDt individual = participant.getIndividual();
            ResourceReferenceDt performer = fhirObservation.addPerformer();
            performer.setReference(individual.getReference());
            performer.setDisplay(individual.getDisplay());
        }
    }

    private FHIRResource buildFhirResource(Observation observation, String resourceName) {
        return new FHIRResource(resourceName, observation.getIdentifier(), observation);
    }
}
