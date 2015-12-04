package org.openmrs.module.shrclient.service;

import org.openmrs.module.shrclient.model.FacilityCatchment;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface FacilityCatchmentService {
    List<FacilityCatchment> getCatchmentsForFacility(int locationId);

    List<FacilityCatchment> getFacilitiesForCatchment(String catchment);

}
