package org.openmrs.module.bdshrclient.web.controller;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.bdshrclient.model.Address;
import org.openmrs.module.bdshrclient.model.Patient;
import org.openmrs.module.bdshrclient.service.MciPatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping(value = "/mci")
public class MciPatientLookupController {

    @Autowired
    MciPatientService patientService;


    @RequestMapping(method = RequestMethod.GET, value = "/search")
    @ResponseBody
    public Object search(@RequestParam(value = "nid", required = false) String nid,
                         @RequestParam(value = "hid", required = false) String hid) {

        Map<String, Object> criteria = new HashMap<String, Object>();
        if (!StringUtils.isBlank(nid)) {
            criteria.put("nid", nid);
        }

        if (!StringUtils.isBlank(nid)) {
            criteria.put("nid", nid);
        }

        if (criteria.get("nid") != null) {
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
}
