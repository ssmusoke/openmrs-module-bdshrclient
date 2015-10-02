package org.openmrs.module.shrclient.mapper;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.module.shrclient.model.Patient;
import org.openmrs.module.shrclient.model.PhoneNumber;
import org.openmrs.module.shrclient.model.Relation;
import org.openmrs.module.shrclient.model.Status;
import org.openmrs.module.shrclient.service.BbsCodeService;
import org.openmrs.module.shrclient.util.AddressHelper;
import org.openmrs.module.shrclient.util.StringUtil;
import org.openmrs.module.shrclient.util.SystemProperties;

import java.util.Collection;
import java.util.List;

import static org.openmrs.module.fhir.utils.Constants.*;
import static org.openmrs.module.shrclient.mapper.PersonAttributeMapper.getAttribute;
import static org.openmrs.module.shrclient.mapper.PersonAttributeMapper.getAttributeValue;

public class PatientMapper {
    private static final String DOB_TYPE_DECLARED = "1";
    private static final String DOB_TYPE_ESTIMATED = "3";
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
        List<Relation> relations = RelationshipMapper.map(openMrsPatient);
        if (CollectionUtils.isNotEmpty(relations)) {
            patient.setRelations(relations.toArray(new Relation[relations.size()]));
        }

        setProvider(patient, openMrsPatient, systemProperties);
        patient.setDobType(getDobType(openMrsPatient));

        return patient;
    }

    public String getDobType(org.openmrs.Patient openMrsPatient) {
        return openMrsPatient.getBirthdateEstimated() ? DOB_TYPE_ESTIMATED : DOB_TYPE_DECLARED;
    }

    private void setProvider(Patient patient, org.openmrs.Patient openMrsPatient, SystemProperties systemProperties) {
        Person person = null;
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    person = openMrsPatient.getChangedBy() != null ? openMrsPatient.getChangedBy().getPerson() : openMrsPatient.getCreator().getPerson();
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
