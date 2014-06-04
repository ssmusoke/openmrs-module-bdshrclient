package org.openmrs.module.bdshrclient.web.controller;

import org.openmrs.module.bdshrclient.model.Address;
import org.openmrs.module.bdshrclient.model.Patient;
import org.openmrs.module.bdshrclient.service.MciPatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/mci")
public class MciPatientLookupController {

    @Autowired
    MciPatientService mciPatientService;


    @RequestMapping(method = RequestMethod.GET, value = "/search")
    @ResponseBody
    public Object search(MciPatientSearchRequest request) {
        if (request.getNid() != null) {
            Patient patient = new Patient();
            patient.setFirstName("Papon");
            patient.setMiddleName("Das");
            patient.setLastName("Baul");
            patient.setAddress(new Address("01", "0101", "010101", "01010101"));
            patient.setGender("Male");
            return patient;
        }
        return null;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/download")
    @ResponseBody
    public String download(MciPatientSearchRequest request) {
        Patient mciPatient = new Patient();
        //mciPatientService.createOrUpdatePatient(mciPatient);
        mciPatientService.getIdentifier();
        return mciPatientService.getIdentifier();
    }
}
