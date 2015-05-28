package org.openmrs.module.shrclient.mapper;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.openmrs.module.shrclient.model.PhoneNumber;

import static org.junit.Assert.assertEquals;

public class PhoneNumberMapperTest {

    @Test
    public void shouldMapMciPhoneNoOpenMRSPhoneNo() throws Exception {
        PhoneNumber mciPhoneNumber = new PhoneNumber();
        mciPhoneNumber.setNumber("4335335");
        assertEquals("4335335", PhoneNumberMapper.map(mciPhoneNumber));
    }

    @Test
    public void shouldMapAllTheFieldsInOrder() throws Exception {
        PhoneNumber mciPhoneNumber = new PhoneNumber();
        mciPhoneNumber.setNumber("4335335");
        mciPhoneNumber.setCountryCode("880");
        mciPhoneNumber.setAreaCode("10");
        mciPhoneNumber.setExtension("67");
        assertEquals("88010433533567", PhoneNumberMapper.map(mciPhoneNumber));
    }

    @Test
    public void shouldReturnEmptyStringForNullMciPhoneNo() throws Exception {
        PhoneNumber mciPhoneNumber = null;
        assertEquals(StringUtils.EMPTY, PhoneNumberMapper.map(mciPhoneNumber));
    }

    @Test
    public void shouldReturnEmptyStringForEmptyMciPhoneNo() throws Exception {
        PhoneNumber mciPhoneNumber = new PhoneNumber();
        assertEquals(StringUtils.EMPTY, PhoneNumberMapper.map(mciPhoneNumber));
    }

    @Test
    public void shouldReturnEmptyMciPhoneNoForBlankNo() throws Exception {
        String openmrsPhoneNo = " ";
        PhoneNumber mciPhoneNumber = new PhoneNumber();
        assertEquals(mciPhoneNumber, PhoneNumberMapper.map(openmrsPhoneNo));
    }

    @Test
    public void shouldReturnMciPhoneNumber() throws Exception {
        String openmrsPhoneNo = " 80870970909";
        PhoneNumber mciPhoneNumber = new PhoneNumber();
        mciPhoneNumber.setNumber("80870970909");
        assertEquals(mciPhoneNumber, PhoneNumberMapper.map(openmrsPhoneNo));
    }
}


