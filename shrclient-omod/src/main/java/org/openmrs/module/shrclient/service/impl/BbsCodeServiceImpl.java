package org.openmrs.module.shrclient.service.impl;

import org.openmrs.module.shrclient.dao.BbsCodeDao;
import org.openmrs.module.shrclient.service.BbsCodeService;

public class BbsCodeServiceImpl implements BbsCodeService {

    private final BbsCodeDao dao;

    public BbsCodeServiceImpl() {
        this.dao = BbsCodeDao.getInstance();
    }

    public String getGenderCode(String concept) {
        return dao.getGenderCode(concept);
    }

    public String getGenderConcept(String code) {
        return dao.getGenderConcept(code);
    }

    public String getEducationCode(String concept) {
        return dao.getEducationCode(concept);
    }

    public String getEducationConceptName(String code) {
        return dao.getEducationConcept(code);
    }

    public String getOccupationCode(String concept) {
        return dao.getOccupationCode(concept);
    }

    public String getOccupationConceptName(String code) {
        return dao.getOccupationConcept(code);
    }
}
