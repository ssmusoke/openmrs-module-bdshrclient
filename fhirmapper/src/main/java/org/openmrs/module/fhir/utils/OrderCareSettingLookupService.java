package org.openmrs.module.fhir.utils;

import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.Encounter;
import org.openmrs.api.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_CARE_SETTING_FOR_INPATIENT;
import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_CARE_SETTING_FOR_OUTPATIENT;
import static org.openmrs.module.fhir.utils.FHIRFeedHelper.getEncounter;

@Component
public class OrderCareSettingLookupService {
    @Autowired
    private OrderService orderService;

    public org.openmrs.CareSetting getCareSetting(AtomFeed feed) {
        //TODO : change to encounter
        org.hl7.fhir.instance.model.Encounter fhirEncounter = getEncounter(feed);
        org.hl7.fhir.instance.model.Enumeration<org.hl7.fhir.instance.model.Encounter.EncounterClass> fhirEncounterClass = fhirEncounter.getClass_();
        String careSetting = fhirEncounterClass.getValue().equals(Encounter.EncounterClass.inpatient) ? MRS_CARE_SETTING_FOR_INPATIENT : MRS_CARE_SETTING_FOR_OUTPATIENT;
        return orderService.getCareSettingByName(careSetting);
    }
}
