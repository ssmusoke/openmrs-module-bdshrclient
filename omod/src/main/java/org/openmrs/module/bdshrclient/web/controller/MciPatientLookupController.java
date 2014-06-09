package org.openmrs.module.bdshrclient.web.controller;


import org.openmrs.api.context.Context;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.bdshrclient.model.Address;
import org.openmrs.module.bdshrclient.model.Patient;
import org.openmrs.module.bdshrclient.service.MciPatientService;
import org.openmrs.module.bdshrclient.util.GenderEnum;
import org.openmrs.module.bdshrclient.util.MciProperties;
import org.openmrs.module.bdshrclient.util.WebClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Controller
@RequestMapping(value = "/mci")
public class MciPatientLookupController {

    @Autowired
    MciPatientService mciPatientService;
    private String mciPatientUrl = null;


    @RequestMapping(method = RequestMethod.GET, value = "/search")
    @ResponseBody
    public Object search(MciPatientSearchRequest request) {
        Patient mciPatient = null;
        if (!isBlankString(request.getNid())) {
            mciPatient = searchPatientByNationalId(request.getNid());

        } else if (!isBlankString(request.getHid())) {
            mciPatient = searchPatientByHealthId(request.getHid());
        }

        if (mciPatient != null) {
            return mapToPatientUIModel(mciPatient);
        }
        return null;
    }

    //@RequestMapping(method = RequestMethod.POST, value = "/download")
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
        patientModel.put("gender", GenderEnum.forCode(mciPatient.getGender()).name());
        patientModel.put("nationalId", mciPatient.getNationalId());
        patientModel.put("healthId", mciPatient.getHealthId());
        patientModel.put("primaryContact", mciPatient.getPrimaryContact());

        Map<String, String> addressModel = new HashMap<String, String>();
        Address address = mciPatient.getAddress();
        addressModel.put("address_line", address.getAddressLine());
        addressModel.put("division", getAddressEntryText(address.getDivisionId()));
        addressModel.put("district", getAddressEntryText(address.getDistrictId()));
        addressModel.put("upazilla", getAddressEntryText(address.getUpazillaId()));
        addressModel.put("union",    getAddressEntryText(address.getUnionId()));

        patientModel.put("address", addressModel);
        return patientModel;
    }

    private boolean isBlankString(String value) {
        if ((value != null) && !"".equals(value)) {
            return false;
        }
        return true;
    }

    private Patient searchPatientByNationalId(String nid)  {
        String mciNationIdSearchUrl = null;
        try {
            mciNationIdSearchUrl = String.format("%s?nid=%s", getMciPatientBaseUrl(), nid);
            WebClient webClient = new WebClient();
            Patient mciPatient = webClient.get(mciNationIdSearchUrl, Patient.class);
            return mciPatient;
        } catch (IOException e) {
            throw new RuntimeException("Error occurred while trying to query MCI", e);
        }
    }

    private Patient searchPatientByHealthId(String hid)  {
        if ((hid == null) || "".equals(hid)) return null;
        try {
            String mciPatientUrl = String.format("%s/%s", getMciPatientBaseUrl(), hid);
            WebClient webClient = new WebClient();
            Patient mciPatient = webClient.get(mciPatientUrl, Patient.class);
            return mciPatient;
        } catch (IOException e) {
            throw new RuntimeException("Error occurred while trying to query MCI", e);
        }
    }


    private String getMciPatientBaseUrl() throws IOException {
        if (mciPatientUrl == null) {
            MciProperties mciProperties = new MciProperties(new Properties());
            mciProperties.loadProperties();
            mciPatientUrl = mciProperties.getMciPatientBaseURL();
        }
        return mciPatientUrl;
    }

    private String getAddressEntryText(String code) {
        AddressHierarchyService addressHierarchyService = Context.getService(AddressHierarchyService.class);
        AddressHierarchyEntry entry = addressHierarchyService.getAddressHierarchyEntryByUserGenId(code);
        return entry.getName();
    }

    public static void main(String[] args) throws IOException {
        MciPatientLookupController mciPatientLookupController = new MciPatientLookupController();
        Patient nid107 = mciPatientLookupController.searchPatientByNationalId("nid107");
        System.out.println(nid107);

    }
}
