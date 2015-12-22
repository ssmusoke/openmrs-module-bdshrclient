package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.IdentifierDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.mapper.model.FHIREncounter;
import org.openmrs.module.fhir.mapper.model.FHIRResource;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.stereotype.Component;

@Component("observationBuilder")
public class ObservationBuilder {
    public FHIRResource buildObservationResource(FHIREncounter fhirEncounter, String resourceId, String resourceName, SystemProperties systemProperties) {
        Observation fhirObservation = new Observation();
        fhirObservation.setSubject(fhirEncounter.getPatient());
        fhirObservation.setEncounter(new ResourceReferenceDt().setReference(fhirEncounter.getId()));
        String id = new EntityReference().build(IResource.class, systemProperties, resourceId);
        fhirObservation.setId(id);
        fhirObservation.addIdentifier(new IdentifierDt().setValue(id));
        fhirObservation.setPerformer(fhirEncounter.getParticipantReferences());
        return buildFhirResource(fhirObservation, resourceName);
    }

    private FHIRResource buildFhirResource(Observation observation, String resourceName) {
        return new FHIRResource(resourceName, observation.getIdentifier(), observation);
    }
}
