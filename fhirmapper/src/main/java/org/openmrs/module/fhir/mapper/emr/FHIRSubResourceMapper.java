package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IResource;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.module.fhir.mapper.model.ShrEncounterComposition;
import org.openmrs.module.fhir.utils.FHIRBundleHelper;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class FHIRSubResourceMapper {
    @Autowired
    private List<FHIRResourceMapper> fhirResourceMappers;

    public void map(Encounter emrEncounter, ShrEncounterComposition encounterComposition, SystemProperties systemProperties) {
        voidExistingObs(emrEncounter);
        List<IResource> topLevelResources = FHIRBundleHelper.identifyTopLevelResources(encounterComposition.getBundle());
        for (IResource resource : topLevelResources) {
            for (FHIRResourceMapper fhirResourceMapper : fhirResourceMappers) {
                if (fhirResourceMapper.canHandle(resource)) {
                    fhirResourceMapper.map(resource, emrEncounter, encounterComposition, systemProperties);
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
