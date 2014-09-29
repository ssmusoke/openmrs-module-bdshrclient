package org.openmrs.module.shrclient.mapper;

import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.module.addresshierarchy.AddressHierarchyLevel;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.fhir.utils.Constants;
import org.openmrs.module.shrclient.mci.api.model.Address;
import org.openmrs.module.shrclient.mci.api.model.Patient;
import org.openmrs.module.shrclient.service.BbsCodeService;

import java.text.SimpleDateFormat;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.substring;

public class PatientMapper {

    private AddressHierarchyService addressHierarchyService;
    private BbsCodeService bbsCodeService;

    public PatientMapper(AddressHierarchyService addressHierarchyService, BbsCodeService bbsCodeService) {
        this.addressHierarchyService = addressHierarchyService;
        this.bbsCodeService = bbsCodeService;
    }

    public Patient map(org.openmrs.Patient openMrsPatient) {
        Patient patient = new Patient();

        String nationalId = getAttributeValue(openMrsPatient, Constants.NATIONAL_ID_ATTRIBUTE);
        if (nationalId != null) {
            patient.setNationalId(nationalId);
        }

        String healthId = getAttributeValue(openMrsPatient, Constants.HEALTH_ID_ATTRIBUTE);
        if (healthId != null) {
            patient.setHealthId(healthId);
        }

        patient.setGivenName(openMrsPatient.getGivenName());
        patient.setSurName(openMrsPatient.getFamilyName());
        patient.setGender(openMrsPatient.getGender());
        patient.setDateOfBirth(new SimpleDateFormat(Constants.ISO_DATE_FORMAT).format(openMrsPatient.getBirthdate()));

        PersonAttribute occupation = getAttribute(openMrsPatient, Constants.OCCUPATION_ATTRIBUTE);
        if (occupation != null) {
            patient.setOccupation(bbsCodeService.getOccupationCode(occupation.toString()));
        }

        PersonAttribute education = getAttribute(openMrsPatient, Constants.EDUCATION_ATTRIBUTE);
        if (education != null) {
            patient.setEducationLevel(bbsCodeService.getEducationCode(education.toString()));
        }

        String primaryContact = getAttributeValue(openMrsPatient, Constants.PRIMARY_CONTACT_ATTRIBUTE);
        if (primaryContact != null) {
            patient.setPrimaryContact(primaryContact);
        }

        patient.setAddress(getAddress(openMrsPatient));
        return patient;
    }

    private String getAttributeValue(org.openmrs.Patient openMrsPatient, String attributeName) {
        PersonAttribute attribute = getAttribute(openMrsPatient, attributeName);
        return attribute != null ? attribute.getValue() : null;
    }

    private PersonAttribute getAttribute(org.openmrs.Patient openMrsPatient, String attributeName) {
        return openMrsPatient.getAttribute(attributeName);
    }

    private Address getAddress(org.openmrs.Patient openMrsPatient) {
        PersonAddress openMrsPersonAddress = openMrsPatient.getPersonAddress();
        String addressLine = openMrsPersonAddress.getAddress1();
        String division = openMrsPersonAddress.getStateProvince();
        String district = openMrsPersonAddress.getCountyDistrict();
        String upazilla = openMrsPersonAddress.getAddress3();
        String cityCorporation = openMrsPersonAddress.getAddress2();
        String ward = openMrsPersonAddress.getCityVillage();

        List<AddressHierarchyLevel> levels = addressHierarchyService.getOrderedAddressHierarchyLevels();
        String divisionId = addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(levels.get(0), division).get(0).getUserGeneratedId();
        String districtId = addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(levels.get(1), district).get(0).getUserGeneratedId();
        String upazillaId = addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(levels.get(2), upazilla).get(0).getUserGeneratedId();
        String cityCorporationId = addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(levels.get(3), cityCorporation).get(0).getUserGeneratedId();
        String wardId = addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(levels.get(4), ward).get(0).getUserGeneratedId();

        Address presentAddress = new Address(addressLine,
                Address.getAddressCodeForLevel(divisionId,1),
                Address.getAddressCodeForLevel(districtId,2),
                Address.getAddressCodeForLevel(upazillaId,3),
                Address.getAddressCodeForLevel(cityCorporationId,4),
                Address.getAddressCodeForLevel(wardId,5),
                null, null);
        return presentAddress;
    }
}
