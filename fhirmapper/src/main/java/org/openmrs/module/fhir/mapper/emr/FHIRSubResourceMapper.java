package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IResource;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.module.fhir.mapper.model.EmrEncounter;
import org.openmrs.module.fhir.mapper.model.ShrEncounter;
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

    public void map(Encounter openMrsEncounter, ShrEncounter encounterComposition, SystemProperties systemProperties) {
        EmrEncounter emrEncounter = new EmrEncounter(openMrsEncounter);
        List<IResource> topLevelResources = FHIRBundleHelper.identifyTopLevelResources(encounterComposition.getBundle());
        for (IResource resource : topLevelResources) {
            for (FHIRResourceMapper fhirResourceMapper : fhirResourceMappers) {
                if (fhirResourceMapper.canHandle(resource)) {
                    fhirResourceMapper.map(resource, emrEncounter, encounterComposition, systemProperties);
                }
            }
        }
        voidExistingObs(emrEncounter.getEncounter());
        addNewObsAndOrderToOpenMrsEncounter(emrEncounter);
    }

    private void addNewObsAndOrderToOpenMrsEncounter(EmrEncounter emrEncounter) {
        Encounter openmrsEncounter = emrEncounter.getEncounter();
        for (Order order : emrEncounter.getOrders()) {
            openmrsEncounter.addOrder(order);
        }
        for (Obs obs : emrEncounter.getObs()) {
            openmrsEncounter.addObs(obs);
        }
    }

    private void voidExistingObs(Encounter emrEncounter) {
        Set<Obs> existingObs = emrEncounter.getAllObs(false);
        for (Obs obs : existingObs) {
            obs.setVoided(true);
        }
    }
}
