package org.openmrs.module.bdshrclient.web.controller;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.module.bdshrclient.model.Address;
import org.openmrs.module.bdshrclient.model.Patient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping(value = "/mci")
public class MciPatientLookupController {



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
            patient.setFullName("Papon", "Das", "Baul");
            patient.setAddress(new Address("01", "0101", "010101", "01010101"));
            patient.setGender("Male");
            return patient;
        }
        return null;
    }


    public static void main(String[] args) {
        Patient patient = new Patient();
        patient.setFullName("Papon", "Das", "Baul");
        patient.setAddress(new Address("01", "0101", "010101", "01010101"));
        patient.setGender("Male");

        ObjectMapper om = new ObjectMapper();
        try {
            String testAString = om.writeValueAsString(patient);
            System.out.println(testAString);
            Patient newTestA = om.readValue(testAString, Patient.class);
        } catch (JsonGenerationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JsonMappingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
