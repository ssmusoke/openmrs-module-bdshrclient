package org.openmrs.module.shrclient.service;

import org.openmrs.api.OpenmrsService;
import org.openmrs.module.shrclient.model.FacilityCatchment;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface FacilityCatchmentService extends OpenmrsService{
   List<FacilityCatchment> getCatchmentsForFacility(int locationId);
   List<FacilityCatchment> getFacilitiesForCatchment(String catchment);

}
