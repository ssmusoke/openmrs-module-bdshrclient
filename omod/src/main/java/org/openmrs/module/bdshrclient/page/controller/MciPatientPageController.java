package org.openmrs.module.bdshrclient.page.controller;

import org.springframework.web.bind.annotation.RequestParam;
import org.openmrs.ui.framework.page.PageModel;

public class MciPatientPageController {
    public void get(PageModel model,
                    @RequestParam(value = "nid", required = false) String nid,
                    @RequestParam(value = "hid", required = false) String hid) {
        System.out.printf("********************************************* ");
        System.out.printf("National ID: " + nid);
        System.out.printf("National ID: " + hid);
        System.out.printf("********************************************* ");
    }
}
