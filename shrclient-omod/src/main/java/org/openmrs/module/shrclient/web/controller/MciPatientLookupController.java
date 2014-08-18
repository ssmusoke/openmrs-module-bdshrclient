package org.openmrs.module.shrclient.web.controller;


import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.shrclient.model.Address;
import org.openmrs.module.shrclient.model.Patient;
import org.openmrs.module.shrclient.service.BbsCodeService;
import org.openmrs.module.shrclient.service.MciPatientService;
import org.openmrs.module.fhir.utils.Constants;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.RestClient;
import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;
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
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Controller
@RequestMapping(value = "/mci")
public class MciPatientLookupController {

    @Autowired
    private MciPatientService mciPatientService;
    @Autowired
    private BbsCodeService bbsCodeService;
    @Autowired
    private PropertiesReader propertiesReader;
    @Resource(name = "shrProperties")
    private Properties shrProperties;

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
        final String healthId = request.getHid();
        Patient mciPatient = searchPatientByHealthId(healthId);
        if (mciPatient != null) {
            Map<String, String> downloadResponse = new HashMap<String, String>();
            org.openmrs.Patient emrPatient = mciPatientService.createOrUpdatePatient(mciPatient);
            if (emrPatient != null) {
                createOrUpdateEncounters(healthId, emrPatient);
            }
            downloadResponse.put("uuid", emrPatient.getUuid());
            return downloadResponse;
        }
        return null;
    }

    private void createOrUpdateEncounters(String healthId, org.openmrs.Patient emrPatient) {
        final String url = String.format("/patients/%s/encounters", healthId);
        List<EncounterBundle> bundles = propertiesReader.getShrWebClient().get(url, new TypeReference<List<EncounterBundle>>() {
        });
        mciPatientService.createOrUpdateEncounters(emrPatient, bundles);
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
        addressModel.put("district", getAddressEntryText(address.createUserGeneratedDistrictId()));
        addressModel.put("upazilla", getAddressEntryText(address.createUserGeneratedUpazillaId()));
        addressModel.put("cityCorporation", getAddressEntryText(address.createUserGeneratedCityCorporationId()));
        addressModel.put("union", getAddressEntryText(address.createUserGeneratedWardId()));

        patientModel.put("address", addressModel);
        return patientModel;
    }

    private Patient searchPatientByNationalId(String nid) {
        String url = String.format("%s?nid=%s", Constants.MCI_PATIENT_URL, nid);
        return getMciRestClient().get(url, Patient.class);
    }

    private Patient searchPatientByHealthId(String hid) {
        if (StringUtils.isBlank(hid)) {
            return null;
        }
        return getMciRestClient().get(Constants.MCI_PATIENT_URL + "/" + hid, Patient.class);
    }

    private RestClient getMciRestClient() {
        return propertiesReader.getMciWebClient();
    }

    private String getAddressEntryText(String code) {
        AddressHierarchyService addressHierarchyService = Context.getService(AddressHierarchyService.class);
        AddressHierarchyEntry entry = addressHierarchyService.getAddressHierarchyEntryByUserGenId(code);
        return entry.getName();
    }
}
