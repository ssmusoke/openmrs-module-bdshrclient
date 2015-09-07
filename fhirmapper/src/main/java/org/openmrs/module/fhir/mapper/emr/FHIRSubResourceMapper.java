package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FHIRSubResourceMapper {
    @Autowired
    private List<FHIRResourceMapper> fhirResourceMappers;

    public void map(Patient emrPatient, Bundle bundle, Encounter emrEncounter) {
        List<IResource> topLevelResources = FHIRFeedHelper.identifyTopLevelResources(bundle);
        for (IResource resource : topLevelResources) {
            for (FHIRResourceMapper fhirResourceMapper : fhirResourceMappers) {
                if (fhirResourceMapper.canHandle(resource)) {
                    fhirResourceMapper.map(bundle, resource, emrPatient, emrEncounter);
                }
            }
        }
    }
}
