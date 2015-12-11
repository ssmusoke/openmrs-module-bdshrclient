package org.openmrs.module.shrclient.service;

import org.apache.log4j.Logger;
import org.openmrs.module.shrclient.dao.FacilityCatchmentRepository;
import org.openmrs.module.shrclient.model.FacilityCatchment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FacilityCatchmentService {
    private static final Logger logger = Logger.getLogger(FacilityCatchmentService.class);

    private FacilityCatchmentRepository facilityCatchmentRepository;

    @Autowired
    public FacilityCatchmentService(FacilityCatchmentRepository facilityCatchmentRepository) {
        this.facilityCatchmentRepository = facilityCatchmentRepository;
    }

    public List<FacilityCatchment> getCatchmentsForFacility(int locationId) {
        logger.debug("Finding catchments for facility with location_id: " + locationId);
        return facilityCatchmentRepository.findByFacilityLocationId(locationId);
    }

    public List<FacilityCatchment> getFacilitiesForCatchment(String catchment) {
        logger.debug("Finding facilities for catchment :  " + catchment);
        return facilityCatchmentRepository.findByCatchment(catchment);
    }

}
