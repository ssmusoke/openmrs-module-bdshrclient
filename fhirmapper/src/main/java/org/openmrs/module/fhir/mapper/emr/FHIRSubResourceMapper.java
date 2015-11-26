package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class FHIRSubResourceMapper {
    @Autowired
    private List<FHIRResourceMapper> fhirResourceMappers;

    public void map(Bundle bundle, Encounter emrEncounter) {
        voidExistingObs(emrEncounter);
        List<IResource> topLevelResources = FHIRFeedHelper.identifyTopLevelResources(bundle);
        for (IResource resource : topLevelResources) {
            for (FHIRResourceMapper fhirResourceMapper : fhirResourceMappers) {
                if (fhirResourceMapper.canHandle(resource)) {
                    fhirResourceMapper.map(bundle, resource, emrEncounter);
                }
            }
        }
    }

    private void voidExistingObs(Encounter newEmrEncounter) {
        Set<Obs> allObs = newEmrEncounter.getAllObs(false);
        for (Obs obs : allObs) {
            obs.setVoided(true);
        }
    }
}
