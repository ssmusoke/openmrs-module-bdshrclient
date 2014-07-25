package org.openmrs.module.fhir.mapper.emr;

import org.hl7.fhir.instance.model.Condition;
import org.hl7.fhir.instance.model.Resource;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FHIRChiefComplaintConditionMapper implements FHIRResource{

    @Autowired
    ConceptService conceptService;
    public void map(Patient emrPatient, Encounter newEmrEncounter, Condition composition) {


    }

    @Override
    public boolean handles(Resource resource) {
        return false;
    }

    @Override
    public void map(Resource resource, Patient emrPatient, Encounter newEmrEncounter) {

    }
}
