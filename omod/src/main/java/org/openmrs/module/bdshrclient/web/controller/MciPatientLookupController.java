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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping(value = "/mci")
public class MciPatientLookupController {

    @Autowired
    MciPatientService mciPatientService;
    private String mciPatientUrl = null;


    @RequestMapping(method = RequestMethod.GET, value = "/search")
    @ResponseBody
    public Object search(MciPatientSearchRequest request) {
        if (request.getNid() != null) {
            Patient mciPatient = searchPatientByNationalId(request.getNid());
            return mapToPatientUIModel(mciPatient);
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
        addressModel.put("division", getAddressEntryText(address.getDivisionId()));
        addressModel.put("district", getAddressEntryText(address.getDistrictId()));
        addressModel.put("upazilla", getAddressEntryText(address.getUpazillaId()));
        addressModel.put("union",    getAddressEntryText(address.getUnionId()));

        patientModel.put("address", addressModel);
        return patientModel;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/download")
    @ResponseBody
    public Object download(MciPatientSearchRequest request) {
        Patient mciPatient = searchPatientByHealthId(request.getHid());
        org.openmrs.Patient patient = mciPatientService.createOrUpdatePatient(mciPatient);
        Map<String, String> downloadResponse = new HashMap<String, String>();
        downloadResponse.put("uuid", patient.getUuid());
        return downloadResponse;
    }

//    @RequestMapping(method = RequestMethod.POST, value = "/downloadPatient")
//    @ResponseBody
//    public MciPatientSearchRequest downloadPatient(@RequestBody MciPatientSearchRequest request) {
//        return request;
//    }

    private Patient searchPatientByNationalId(String nid)  {
        String mciNationIdSearchUrl = null;
        try {
            mciNationIdSearchUrl = String.format("%s?nid=%s", getMciPatientBaseUrl(), nid);
            WebClient webClient = new WebClient();
            Patient patient = webClient.get(mciNationIdSearchUrl, Patient.class);
            return patient;
        } catch (IOException e) {
            throw new RuntimeException("Error occurred while trying to query MCI", e);
        }
    }

    private Patient searchPatientByHealthId(String hid)  {
        String mciPatientUrl = null;
        try {
            mciPatientUrl = String.format("%s/%s", getMciPatientBaseUrl(), hid);
            WebClient webClient = new WebClient();
            Patient patient = webClient.get(mciPatientUrl, Patient.class);
            return patient;
        } catch (IOException e) {
            throw new RuntimeException("Error occurred while trying to query MCI", e);
        }
    }


    private String getMciPatientBaseUrl() throws IOException {
        if (mciPatientUrl == null) {
            MciProperties mciProperties = new MciProperties();
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
