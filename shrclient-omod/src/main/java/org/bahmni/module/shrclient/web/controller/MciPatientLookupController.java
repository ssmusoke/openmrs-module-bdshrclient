package org.bahmni.module.shrclient.web.controller;


import org.apache.commons.lang3.StringUtils;
import org.bahmni.module.shrclient.model.Address;
import org.bahmni.module.shrclient.model.Patient;
import org.bahmni.module.shrclient.service.BbsCodeService;
import org.bahmni.module.shrclient.service.MciPatientService;
import org.bahmni.module.shrclient.util.RestClient;
import org.openmrs.api.context.Context;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Controller
@RequestMapping(value = "/mci")
public class MciPatientLookupController {

    @Autowired
    private MciPatientService mciPatientService;
    @Autowired
    private BbsCodeService bbsCodeService;
    @Resource(name = "mciProperties")
    private Properties properties;


    @RequestMapping(method = RequestMethod.GET, value = "/search")
    @ResponseBody
    public Object search(MciPatientSearchRequest request) {
        Patient mciPatient = null;
        if (StringUtils.isNotBlank(request.getNid())) {
            mciPatient = searchPatientByNationalId(request.getNid());

        } else if (StringUtils.isNotBlank(request.getHid())) {
            mciPatient = searchPatientByHealthId(request.getHid());
        }

        if (mciPatient != null) {
            return mapToPatientUIModel(mciPatient);
        }
        return null;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/download")
    @ResponseBody
    public Object download(MciPatientSearchRequest request) {
        Patient mciPatient = searchPatientByHealthId(request.getHid());
        if (mciPatient != null) {
            Map<String, String> downloadResponse = new HashMap<String, String>();
            org.openmrs.Patient emrPatient = mciPatientService.createOrUpdatePatient(mciPatient);
            downloadResponse.put("uuid", emrPatient.getUuid());
            return downloadResponse;
        }
        return null;
    }

    private Map<String, Object> mapToPatientUIModel(Patient mciPatient) {
        Map<String, Object> patientModel = new HashMap<String, Object>();
        patientModel.put("firstName", mciPatient.getFirstName());
        patientModel.put("middleName", mciPatient.getMiddleName());
        patientModel.put("lastName", mciPatient.getLastName());
        patientModel.put("gender", bbsCodeService.getGenderConcept(mciPatient.getGender()));
        patientModel.put("nationalId", mciPatient.getNationalId());
        patientModel.put("healthId", mciPatient.getHealthId());
        patientModel.put("primaryContact", mciPatient.getPrimaryContact());

        Map<String, String> addressModel = new HashMap<String, String>();
        Address address = mciPatient.getAddress();
        addressModel.put("address_line", address.getAddressLine());
        addressModel.put("division", getAddressEntryText(address.getDivisionId()));
        addressModel.put("district", getAddressEntryText(address.getDistrictId()));
        addressModel.put("upazilla", getAddressEntryText(address.getUpazillaId()));
        addressModel.put("union", getAddressEntryText(address.getUnionId()));

        patientModel.put("address", addressModel);
        return patientModel;
    }

    private Patient searchPatientByNationalId(String nid) {
        String url = String.format("/patient?nid=%s", nid);
        return getMciWebClient().get(url, Patient.class);
    }

    private Patient searchPatientByHealthId(String hid) {
        if (StringUtils.isBlank(hid)) {
            return null;
        }
        return getMciWebClient().get("/patient", Patient.class);
    }

    private RestClient getMciWebClient() {
        return new RestClient(properties.getProperty("mci.user"),
                properties.getProperty("mci.password"),
                properties.getProperty("mci.host"),
                properties.getProperty("mci.port"));
    }

    private String getAddressEntryText(String code) {
        AddressHierarchyService addressHierarchyService = Context.getService(AddressHierarchyService.class);
        AddressHierarchyEntry entry = addressHierarchyService.getAddressHierarchyEntryByUserGenId(code);
        return entry.getName();
    }
}
