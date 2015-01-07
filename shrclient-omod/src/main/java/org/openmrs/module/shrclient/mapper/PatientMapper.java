package org.openmrs.module.shrclient.mapper;

import org.openmrs.PersonAttribute;
import org.openmrs.module.fhir.utils.Constants;
import org.openmrs.module.shrclient.mci.api.model.Patient;
import org.openmrs.module.shrclient.service.BbsCodeService;
import org.openmrs.module.shrclient.util.AddressHelper;

import java.text.SimpleDateFormat;

public class PatientMapper {
    private final AddressHelper addressHelper;
    private BbsCodeService bbsCodeService;

    public PatientMapper(BbsCodeService bbsCodeService) {
        this.bbsCodeService = bbsCodeService;
        this.addressHelper = new AddressHelper();
    }

    public PatientMapper(BbsCodeService bbsCodeService, AddressHelper addressHelper) {
        this.bbsCodeService = bbsCodeService;
        this.addressHelper = addressHelper;
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
        patient.setAddress(addressHelper.getMciAddress(openMrsPatient));
        Boolean isDead = openMrsPatient.isDead();
        if (isDead){
            patient.setStatus('2');
        } else {
            patient.setStatus('1');
        }
        if (openMrsPatient.getDeathDate() != null) {
            patient.setDateOfDeath(new SimpleDateFormat(Constants.ISO_DATE_FORMAT).format(openMrsPatient.getDeathDate()));
        }
        return patient;
    }

    private String getAttributeValue(org.openmrs.Patient openMrsPatient, String attributeName) {
        PersonAttribute attribute = getAttribute(openMrsPatient, attributeName);
        return attribute != null ? attribute.getValue() : null;
    }

    private PersonAttribute getAttribute(org.openmrs.Patient openMrsPatient, String attributeName) {
        return openMrsPatient.getAttribute(attributeName);
    }
}
