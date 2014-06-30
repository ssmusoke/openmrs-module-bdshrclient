package org.openmrs.module.bdshrclient.service.impl;

import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.User;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.bdshrclient.dao.PatientAttributeSearchHandler;
import org.openmrs.module.bdshrclient.model.Address;
import org.openmrs.module.bdshrclient.model.Patient;
import org.openmrs.module.bdshrclient.service.BbsCodeService;
import org.openmrs.module.bdshrclient.service.MciPatientService;
import static org.openmrs.module.bdshrclient.util.Constants.*;

import org.openmrs.module.idgen.IdentifierSource;
import org.openmrs.module.idgen.SequentialIdentifierGenerator;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


public class MciPatientServiceImpl extends BaseOpenmrsService implements MciPatientService {

    @Autowired
    private BbsCodeService bbsCodeService;

    @Override
    public org.openmrs.Patient createOrUpdatePatient(Patient mciPatient) {
        PatientService patientService = Context.getPatientService();
        PersonService personService = Context.getPersonService();

        Integer emrPatientId = new PatientAttributeSearchHandler(HEALTH_ID_ATTRIBUTE).getUniquePatientIdFor(mciPatient.getHealthId());
        org.openmrs.Patient emrPatient = emrPatientId != null ? patientService.getPatient(emrPatientId) : new org.openmrs.Patient();

        emrPatient.setGender(bbsCodeService.getGenderConcept(mciPatient.getGender()));
        setIdentifier(emrPatient);
        setPersonName(emrPatient, mciPatient);
        setPersonAddress(emrPatient, mciPatient.getAddress());

        addPersonAttribute(personService, emrPatient, NATIONAL_ID_ATTRIBUTE, mciPatient.getNationalId());
        addPersonAttribute(personService, emrPatient, HEALTH_ID_ATTRIBUTE, mciPatient.getHealthId());
        addPersonAttribute(personService, emrPatient, PRIMARY_CONTACT_ATTRIBUTE, mciPatient.getPrimaryContact());
        addPersonAttribute(personService, emrPatient, OCCUPATION_ATTRIBUTE, getConceptId(bbsCodeService.getOccupationConcept(mciPatient.getOccupation())));
        addPersonAttribute(personService, emrPatient, EDUCATION_ATTRIBUTE, getConceptId(bbsCodeService.getEducationConcept(mciPatient.getEducationLevel())));

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(ISO_DATE_FORMAT);
        try {
            Date dob = simpleDateFormat.parse(mciPatient.getDateOfBirth());
            emrPatient.setBirthdate(dob);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        setCreator(emrPatient);
        patientService.savePatient(emrPatient);
        return emrPatient;
    }

    private String getConceptId(String conceptName) {
        if (conceptName == null) {
            return null;
        }
        return String.valueOf(Context.getConceptService().getConceptByName(conceptName).getConceptId());
    }

    private void setCreator(org.openmrs.Patient emrPatient) {
        User systemUser = getShrClientSystemUser();
        if (emrPatient.getCreator() == null) {
            emrPatient.setCreator(systemUser);
        } else {
            emrPatient.setChangedBy(systemUser);
        }
    }

    private void setIdentifier(org.openmrs.Patient emrPatient) {
        PatientIdentifier patientIdentifier = emrPatient.getPatientIdentifier();
        if (patientIdentifier == null) {
            patientIdentifier = generateIdentifier();
            patientIdentifier.setPreferred(true);
            emrPatient.addIdentifier(patientIdentifier);
        }
    }

    private void setPersonName(org.openmrs.Patient emrPatient, Patient mciPatient) {
        PersonName emrPersonName = emrPatient.getPersonName();
        if (emrPersonName == null) {
            emrPersonName = new PersonName();
            emrPersonName.setPreferred(true);
            emrPatient.addName(emrPersonName);
        }
        emrPersonName.setGivenName(mciPatient.getFirstName());
        emrPersonName.setMiddleName(mciPatient.getMiddleName());
        emrPersonName.setFamilyName(mciPatient.getLastName());
    }

    private User getShrClientSystemUser() {
        UserService userService = Context.getUserService();
        return userService.getUserByUsername(SHR_CLIENT_SYSTEM_NAME);
    }

    private void setPersonAddress(org.openmrs.Patient emrPatient, Address address) {
        AddressHierarchyService addressHierarchyService = Context.getService(AddressHierarchyService.class);
        PersonAddress emrPatientAddress = emrPatient.getPersonAddress();
        if (emrPatientAddress == null) {
            emrPatientAddress = new PersonAddress();
        }

        AddressHierarchyEntry division = addressHierarchyService.getAddressHierarchyEntryByUserGenId(address.getDivisionId());
        if (division != null) {
            emrPatientAddress.setStateProvince(division.getName());
        }
        AddressHierarchyEntry district = addressHierarchyService.getAddressHierarchyEntryByUserGenId(address.getDistrictId());
        if (district != null) {
            emrPatientAddress.setCountyDistrict(district.getName());
        }
        AddressHierarchyEntry upazilla = addressHierarchyService.getAddressHierarchyEntryByUserGenId(address.getUpazillaId());
        if (upazilla != null) {
            emrPatientAddress.setAddress3(upazilla.getName());
        }
        AddressHierarchyEntry union = addressHierarchyService.getAddressHierarchyEntryByUserGenId(address.getUnionId());
        if (union != null) {
            emrPatientAddress.setCityVillage(union.getName());
        }

        if (!"".equals(address.getAddressLine())) {
            emrPatientAddress.setAddress1(address.getAddressLine());
        }

        emrPatientAddress.setPreferred(true);
        emrPatient.addAddress(emrPatientAddress);

    }

    private void addPersonAttribute(PersonService personService, org.openmrs.Patient emrPatient, String attributeName, String attributeValue) {
        PersonAttribute attribute = new PersonAttribute();
        PersonAttributeType attributeType = personService.getPersonAttributeTypeByName(attributeName);
        if (attributeType != null) {
            attribute.setAttributeType(attributeType);
            attribute.setValue(attributeValue);
            emrPatient.addAttribute(attribute);
        } else {
            System.out.println("Attribute not defined: " + attributeName);
        }
    }

    private PatientIdentifierType getPatientIdentifierType() {
        AdministrationService administrationService = Context.getAdministrationService();
        String globalProperty = administrationService.getGlobalProperty(EMR_PRIMARY_IDENTIFIER_TYPE);
        PatientIdentifierType patientIdentifierByUuid = Context.getPatientService().getPatientIdentifierTypeByUuid(globalProperty);
        return patientIdentifierByUuid;
    }


    public PatientIdentifier generateIdentifier() {
        IdentifierSourceService identifierSourceService = Context.getService(IdentifierSourceService.class);
        List<IdentifierSource> allIdentifierSources = identifierSourceService.getAllIdentifierSources(false);
        for (IdentifierSource identifierSource : allIdentifierSources) {
            if (((SequentialIdentifierGenerator)identifierSource).getPrefix().equals(IDENTIFIER_SOURCE_NAME)) {
                String identifier = identifierSourceService.generateIdentifier(identifierSource, "MCI Patient");
                PatientIdentifierType identifierType = getPatientIdentifierType();
                return new PatientIdentifier(identifier, identifierType, null);
            }
        }
        return null;
    }

}
