package org.openmrs.module.bdshrclient.service.impl;

import org.openmrs.*;
import org.openmrs.Patient;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.openmrs.module.bdshrclient.model.*;
import org.openmrs.module.bdshrclient.model.Address;
import org.openmrs.module.bdshrclient.service.MciPatientService;
import org.openmrs.module.bdshrclient.util.GenderEnum;
import org.openmrs.module.idgen.IdentifierSource;
import org.openmrs.module.idgen.SequentialIdentifierGenerator;
import org.openmrs.module.idgen.service.IdentifierSourceService;

import java.util.List;


public class MciPatientServiceImpl extends BaseOpenmrsService implements MciPatientService {

    private static final String SHR_CLIENT_SYSTEM_NAME = "shrclientsystem";
    private final String IDENTIFIER_SOURCE_NAME = "BAM";
    public static final String EMR_PRIMARY_IDENTIFIER_TYPE = "emr.primaryIdentifierType";


    @Override
    public Patient createOrUpdatePatient(org.openmrs.module.bdshrclient.model.Patient mciPatient) {
        PatientService patientService = Context.getPatientService();
        PersonService personService = Context.getPersonService();
        Patient newPatient = new Patient();
        newPatient.setGender(GenderEnum.forCode(mciPatient.getGender()).name());
        PersonName personName = new PersonName(mciPatient.getFirstName(), mciPatient.getMiddleName(), mciPatient.getLastName());
        personName.setPreferred(true);
        newPatient.addName(personName);

        addPersonAddress(newPatient, mciPatient.getAddress());

        addPersonAttribute(personService, newPatient, "National ID", mciPatient.getNationalId());
        addPersonAttribute(personService, newPatient, "Health ID", mciPatient.getHealthId());

        PatientIdentifier identifier = generateIdentifier();
        identifier.setPreferred(true);
        newPatient.addIdentifier(identifier);

        User systemUser = getShrClientSystemUser();
        newPatient.setCreator(systemUser);
        patientService.savePatient(newPatient);
        return newPatient;
    }

    private User getShrClientSystemUser() {
        UserService userService = Context.getUserService();
        return userService.getUserByUsername(SHR_CLIENT_SYSTEM_NAME);
    }

    private void addPersonAddress(Patient newPatient, Address address) {
        AddressHierarchyService addressHierarchyService = Context.getService(AddressHierarchyService.class);
        PersonAddress personAddress = new PersonAddress();

        AddressHierarchyEntry division = addressHierarchyService.getAddressHierarchyEntryByUserGenId(address.getDivisionId());
        if (division != null) {
            personAddress.setStateProvince(division.getName());
        }
        AddressHierarchyEntry district = addressHierarchyService.getAddressHierarchyEntryByUserGenId(address.getDistrictId());
        if (district != null) {
            personAddress.setCountyDistrict(district.getName());
        }
        AddressHierarchyEntry upazilla = addressHierarchyService.getAddressHierarchyEntryByUserGenId(address.getUpazillaId());
        if (upazilla != null) {
            personAddress.setAddress3(upazilla.getName());
        }
        AddressHierarchyEntry union = addressHierarchyService.getAddressHierarchyEntryByUserGenId(address.getUnionId());
        if (union != null) {
            personAddress.setCityVillage(union.getName());
        }

        personAddress.setPreferred(true);
        newPatient.addAddress(personAddress);
    }

    private void addPersonAttribute(PersonService personService, Patient newPatient, String attributeName, String attributeValue) {
        PersonAttribute attribute = new PersonAttribute();
        PersonAttributeType attributeType = personService.getPersonAttributeTypeByName(attributeName);
        if (attributeType != null) {
            attribute.setAttributeType(attributeType);
            attribute.setValue(attributeValue);
            newPatient.addAttribute(attribute);
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
