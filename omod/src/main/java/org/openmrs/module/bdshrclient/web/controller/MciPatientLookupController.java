package org.openmrs.module.bdshrclient.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@Controller
@RequestMapping(value = "/mci")
public class MciPatientLookupController {

    @RequestMapping(method = RequestMethod.GET, value = "/search")
    @ResponseBody
    public Object search(@RequestParam(value = "nid", required = false) String nid,
                         @RequestParam(value = "hid", required = false) String hid) {

        HashMap<String, String> model = new HashMap<String, String>();
        model.put("nid", nid);
        model.put("hid", hid);
        model.put("name", "unknown");
        return model;
    }

}
