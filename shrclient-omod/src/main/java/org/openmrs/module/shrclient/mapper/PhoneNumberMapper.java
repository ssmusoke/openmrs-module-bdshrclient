package org.openmrs.module.shrclient.mapper;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.shrclient.model.PhoneNumber;

public class PhoneNumberMapper {

    public static final int MINIMUM_LENGTH_OF_PHONE_NO = 3;
    public static final int MAXIMUM_LENGTH_OF_PHONE_NO = 11;

    public static PhoneNumber map(String phoneNumber) {
        if (null == phoneNumber) {
            return null;
        }
        if (phoneNumber.length() < MINIMUM_LENGTH_OF_PHONE_NO || phoneNumber.length() > MAXIMUM_LENGTH_OF_PHONE_NO) {
            throw new RuntimeException(String.format("Phone Number should be between %s to %s digits", MINIMUM_LENGTH_OF_PHONE_NO, MAXIMUM_LENGTH_OF_PHONE_NO));
        }
        PhoneNumber mciPhoneNumber = new PhoneNumber();
        mciPhoneNumber.setNumber(phoneNumber);
        return mciPhoneNumber;
    }

    public static String map(PhoneNumber phoneNumber) {
        String openmrsPhoneNumber = "";
        if (StringUtils.isNotBlank(phoneNumber.getCountryCode())) {
            openmrsPhoneNumber = openmrsPhoneNumber + phoneNumber.getCountryCode();
        }
        if (StringUtils.isNotBlank(phoneNumber.getAreaCode())) {
            openmrsPhoneNumber = openmrsPhoneNumber + phoneNumber.getAreaCode();
        }
        if (StringUtils.isNotBlank(phoneNumber.getNumber())) {
            openmrsPhoneNumber = openmrsPhoneNumber + phoneNumber.getNumber();
        }
        if (StringUtils.isNotBlank(phoneNumber.getExtension())) {
            openmrsPhoneNumber = openmrsPhoneNumber + phoneNumber.getExtension();
        }
        return StringUtils.isNotBlank(openmrsPhoneNumber.trim()) ? openmrsPhoneNumber.trim() : null;
    }
}
