package org.openmrs.module.shrclient.service;

public interface BbsCodeService {

    public String getGenderCode(String concept);

    public String getGenderConcept(String code);

    public String getEducationCode(String concept);

    public String getEducationConceptName(String code);

    public String getOccupationCode(String concept);

    public String getOccupationConceptName(String code);
}
