package org.openmrs.module.fhir.utils;

import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Encounter;
import ca.uhn.fhir.model.dstu2.valueset.EncounterClassEnum;
import org.openmrs.CareSetting;
import org.openmrs.api.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.openmrs.module.fhir.MRSProperties.MRS_CARE_SETTING_FOR_INPATIENT;
import static org.openmrs.module.fhir.MRSProperties.MRS_CARE_SETTING_FOR_OUTPATIENT;
import static org.openmrs.module.fhir.utils.FHIRFeedHelper.getEncounter;

@Component
public class OrderCareSettingLookupService {
    @Autowired
    private OrderService orderService;

    public CareSetting getCareSetting(Bundle bundle) {
        //TODO : change to encounter
        Encounter fhirEncounter = getEncounter(bundle);
        EncounterClassEnum fhirEncounterClass = fhirEncounter.getClassElementElement().getValueAsEnum();
        String careSetting = fhirEncounterClass.equals(EncounterClassEnum.INPATIENT) ? MRS_CARE_SETTING_FOR_INPATIENT : MRS_CARE_SETTING_FOR_OUTPATIENT;
        return orderService.getCareSettingByName(careSetting);
    }
}
