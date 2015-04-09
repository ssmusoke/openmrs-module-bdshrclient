package org.openmrs.module.shrclient.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.fhir.utils.Constants;
import org.openmrs.module.shrclient.handlers.ClientRegistry;
import org.openmrs.module.shrclient.identity.IdentityStore;
import org.openmrs.module.shrclient.identity.IdentityUnauthorizedException;
import org.openmrs.module.shrclient.model.Address;
import org.openmrs.module.shrclient.model.Patient;
import org.openmrs.module.shrclient.model.mci.api.MciPatientSearchResponse;
import org.openmrs.module.shrclient.service.MCIPatientLookupService;
import org.openmrs.module.shrclient.service.MciPatientService;
import org.openmrs.module.shrclient.util.PropertiesReader;
import org.openmrs.module.shrclient.util.RestClient;
import org.openmrs.module.shrclient.web.controller.MciPatientSearchRequest;
import org.openmrs.module.shrclient.web.controller.dto.EncounterBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MCIPatientLookupServiceImpl extends BaseOpenmrsService implements MCIPatientLookupService {
    private static final Logger log = Logger.getLogger(MCIPatientLookupServiceImpl.class);

    private static final String NID_PARAM_KEY = "nid";
    private static final String UID_PARAM_KEY = "uid";
    private static final String BRN_PARAM_KEY = "bin_brn";
    private static final String HOUSE_HOLD_CODE_PARAM_KEY = "household_code";

    private MciPatientService mciPatientService;
    private PropertiesReader propertiesReader;
    private IdentityStore identityStore;
    
    @Autowired
    public MCIPatientLookupServiceImpl(MciPatientService mciPatientService, PropertiesReader propertiesReader, IdentityStore identityStore) {
        this.mciPatientService = mciPatientService;
        this.propertiesReader = propertiesReader;
        this.identityStore = identityStore;
    }

    @Override
    public Object searchPatientInRegistry(MciPatientSearchRequest request) {
        Patient mciPatient = null;
        List<Patient> patientList = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getHid())) {
            mciPatient = searchPatientByHealthId(request.getHid());
            if (mciPatient != null) {
                patientList.add(mciPatient);
            }

        } else {
            String searchParamKey = null;
            String searchParamValue = null;
            if (StringUtils.isNotBlank(request.getNid())) {
                searchParamKey = NID_PARAM_KEY;
                searchParamValue = request.getNid();
            } else if (StringUtils.isNotBlank(request.getUid())) {
                searchParamKey = UID_PARAM_KEY;
                searchParamValue = request.getUid();
            } else if (StringUtils.isNotBlank(request.getBrn())) {
                searchParamKey = BRN_PARAM_KEY;
                searchParamValue = request.getBrn();
            } else if (StringUtils.isNotBlank(request.getHouseHoldCode())) {
                searchParamKey = HOUSE_HOLD_CODE_PARAM_KEY;
                searchParamValue = request.getHouseHoldCode();
            }
            if (StringUtils.isNotBlank(searchParamKey) && StringUtils.isNotBlank(searchParamValue)) {
                Patient[] patients = searchPatients(searchParamKey, searchParamValue);
                if ((patients != null) && (patients.length > 0)) {
                    patientList.addAll(Arrays.asList(patients));
                }
            }
        }
        if (!patientList.isEmpty()) {
            return mapSearchResults(patientList);
        }
        return null;
    }

    @Override
    public Object downloadPatient(MciPatientSearchRequest request) {
        final String healthId = request.getHid();
        Patient mciPatient = searchPatientByHealthId(healthId);
        if (mciPatient != null) {
            Map<String, String> downloadResponse = new HashMap<>();
            org.openmrs.Patient emrPatient = mciPatientService.createOrUpdatePatient(mciPatient);
            if (emrPatient != null) {
                createOrUpdateEncounters(healthId, emrPatient);
            }
            downloadResponse.put("uuid", emrPatient.getUuid());
            return downloadResponse;
        }
        return null;
    }

    private Object[] mapSearchResults(List<Patient> patientList) {
        Object[] results = new Object[patientList.size()];
        int idx = 0;
        for (Patient patient : patientList) {
            results[idx] = mapToPatientUIModel(patient);
            idx++;
        }
        return results;
    }

    private Map<String, Object> mapToPatientUIModel(Patient mciPatient) {
        Map<String, Object> patientModel = new HashMap<>();
        patientModel.put("firstName", mciPatient.getGivenName());
        patientModel.put("lastName", mciPatient.getSurName());
        patientModel.put("gender", mciPatient.getGender());
        patientModel.put("nationalId", mciPatient.getNationalId());
        patientModel.put("healthId", mciPatient.getHealthId());
        patientModel.put("primaryContact", mciPatient.getPrimaryContact());

        Map<String, String> addressModel = new HashMap<>();
        Address address = mciPatient.getAddress();
        addressModel.put("addressLine", address.getAddressLine());
        addressModel.put("division", getAddressEntryText(address.getDivisionId()));
        addressModel.put("district", getAddressEntryText(address.createUserGeneratedDistrictId()));
        addressModel.put("upazilla", getAddressEntryText(address.createUserGeneratedUpazillaId()));
        
        if (address.getCityCorporationId() != null) {
            addressModel.put("cityCorporation", getAddressEntryText(address.createUserGeneratedCityCorporationId()));
            if (address.getUnionOrUrbanWardId() != null)
                addressModel.put("unionOrUrbanWard", getAddressEntryText(address
                        .createUserGeneratedUnionOrUrbanWardId()));
        }
        patientModel.put("address", addressModel);
        return patientModel;
    }

    private String getAddressEntryText(String code) {
        AddressHierarchyService addressHierarchyService = Context.getService(AddressHierarchyService.class);
        AddressHierarchyEntry entry = addressHierarchyService.getAddressHierarchyEntryByUserGenId(code);
        return entry.getName();
    }

    private void createOrUpdateEncounters(String healthId, org.openmrs.Patient emrPatient) {
        final String url = String.format("/patients/%s/encounters", healthId);
        List<EncounterBundle> bundles = null;
        try {
            bundles = new ClientRegistry(propertiesReader, identityStore).getSHRClient().getEncounters(url);
        } catch (IdentityUnauthorizedException e) {
            log.info("Clearing unauthorized identity token.");
            identityStore.clearToken();
        }
        mciPatientService.createOrUpdateEncounters(emrPatient, bundles, healthId);
    }

    private Patient[] searchPatients(String searchParamKey, String searchParamValue) {
        String url = String.format("%s?%s=%s", Constants.MCI_PATIENT_URL, searchParamKey, searchParamValue);
        MciPatientSearchResponse mciPatientSearchResponse = null;
        try {
            mciPatientSearchResponse = getMciRestClient().get(url, MciPatientSearchResponse.class);
        } catch (IdentityUnauthorizedException e) {
            log.info("Clearing unauthorized identity token.");
            identityStore.clearToken();
            throw new RuntimeException(e);
        }
        return mciPatientSearchResponse.getResults();
    }

    private Patient searchPatientByHealthId(String hid) {
        if (StringUtils.isBlank(hid)) {
            return null;
        }
        try {
            return getMciRestClient().get(Constants.MCI_PATIENT_URL + "/" + hid, Patient.class);
        } catch (IdentityUnauthorizedException e) {
            log.info("Clearing unauthorized identity token.");
            identityStore.clearToken();
            throw new RuntimeException(e);
        }
    }

    private RestClient getMciRestClient() throws IdentityUnauthorizedException {
        return new ClientRegistry(propertiesReader, identityStore).getMCIClient();
    }

}
