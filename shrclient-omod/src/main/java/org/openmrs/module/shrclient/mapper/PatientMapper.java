package org.openmrs.module.shrclient.mapper;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.module.shrclient.model.Patient;
import org.openmrs.module.shrclient.model.PhoneNumber;
import org.openmrs.module.shrclient.model.Status;
import org.openmrs.module.shrclient.service.BbsCodeService;
import org.openmrs.module.shrclient.util.AddressHelper;
import org.openmrs.module.shrclient.util.StringUtil;
import org.openmrs.module.shrclient.util.SystemProperties;

import java.util.Collection;

import static org.openmrs.module.fhir.utils.Constants.*;

public class PatientMapper {
    private BbsCodeService bbsCodeService;
    private final AddressHelper addressHelper;

    public PatientMapper(BbsCodeService bbsCodeService) {
        this.bbsCodeService = bbsCodeService;
        this.addressHelper = new AddressHelper();
    }

    public PatientMapper(BbsCodeService bbsCodeService, AddressHelper addressHelper) {
        this.bbsCodeService = bbsCodeService;
        this.addressHelper = addressHelper;
    }

    public Patient map(org.openmrs.Patient openMrsPatient, SystemProperties systemProperties) {
        Patient patient = new Patient();
        patient.setActive(true);
        String nationalId = getAttributeValue(openMrsPatient, NATIONAL_ID_ATTRIBUTE);
        if (nationalId != null) {
            patient.setNationalId(nationalId);
        }

        String healthId = getAttributeValue(openMrsPatient, HEALTH_ID_ATTRIBUTE);
        if (healthId != null) {
            patient.setHealthId(healthId);
        }

        String birthRegNo = getAttributeValue(openMrsPatient, BIRTH_REG_NO_ATTRIBUTE);
        if (birthRegNo != null) {
            patient.setBirthRegNumber(birthRegNo);
        }

        String houseHoldCode = getAttributeValue(openMrsPatient, HOUSE_HOLD_CODE_ATTRIBUTE);
        if (houseHoldCode != null) {
            patient.setHouseHoldCode(houseHoldCode);
        }

        String givenNameLocal = getAttributeValue(openMrsPatient, GIVEN_NAME_LOCAL);
        String familyNameLocal = getAttributeValue(openMrsPatient, FAMILY_NAME_LOCAL);
        String banglaName = (StringUtils.isNotBlank(givenNameLocal) ? givenNameLocal : "")
                .concat(" ")
                .concat((StringUtils.isNotBlank(familyNameLocal) ? familyNameLocal : "")).trim();

        if (StringUtils.isNotBlank(banglaName)) {
            patient.setBanglaName(banglaName);
        }

        String openmrsPhoneNumber = getAttributeValue(openMrsPatient, PHONE_NUMBER);
        if (StringUtils.isNotBlank(openmrsPhoneNumber)) {
            PhoneNumber mciPhoneNumber = PhoneNumberMapper.map(openmrsPhoneNumber);
            patient.setPhoneNumber(mciPhoneNumber);
        }

        patient.setGivenName(openMrsPatient.getGivenName());
        patient.setSurName(openMrsPatient.getFamilyName());
        patient.setGender(openMrsPatient.getGender());
        patient.setDateOfBirth(openMrsPatient.getBirthdate());

        PersonAttribute occupation = getAttribute(openMrsPatient, OCCUPATION_ATTRIBUTE);
        if (occupation != null) {
            patient.setOccupation(bbsCodeService.getOccupationCode(occupation.toString()));
        }

        PersonAttribute education = getAttribute(openMrsPatient, EDUCATION_ATTRIBUTE);
        if (education != null) {
            patient.setEducationLevel(bbsCodeService.getEducationCode(education.toString()));
        }

        patient.setAddress(addressHelper.getMciAddress(openMrsPatient));
        patient.setStatus(getMciPatientStatus(openMrsPatient));

        setProvider(patient, openMrsPatient, systemProperties);

        return patient;
    }

    private void setProvider(Patient patient, org.openmrs.Patient openMrsPatient, SystemProperties systemProperties) {
        Person person = null;
        person = openMrsPatient.getChangedBy() != null ? openMrsPatient.getChangedBy().getPerson() : openMrsPatient.getCreator().getPerson();
        if (null == person) return;
        Collection<Provider> providers = Context.getProviderService().getProvidersByPerson(person);
        if (CollectionUtils.isEmpty(providers)) return;

        for (Provider provider : providers) {
            String identifier = provider.getIdentifier();
            if (!StringUtils.isBlank(identifier)) {
                String providerUrl = String.format("%s/%s.json",
                        StringUtil.removeSuffix(systemProperties.getProviderResourcePath(), "/"), provider.getIdentifier());
                patient.setProviderReference(providerUrl);
                return;
            }
        }
    }

    private String getAttributeValue(org.openmrs.Patient openMrsPatient, String attributeName) {
        PersonAttribute attribute = getAttribute(openMrsPatient, attributeName);
        return attribute != null ? attribute.getValue() : null;
    }

    private PersonAttribute getAttribute(org.openmrs.Patient openMrsPatient, String attributeName) {
        return openMrsPatient.getAttribute(attributeName);
    }

    private Status getMciPatientStatus(org.openmrs.Patient openMrsPatient) {
        Status status = new Status();
        Character type = '1';
        String dateOfDeath = null;
        Boolean isDead = openMrsPatient.isDead();
        if (isDead) {
            type = '2';
        }
        status.setType(type);
        status.setDateOfDeath(openMrsPatient.getDeathDate());
        return status;
    }
}
