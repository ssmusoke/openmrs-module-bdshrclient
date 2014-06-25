package org.openmrs.module.bdshrclient.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public enum GenderEnum {
    M("1"), F("2"), O("3");

    private static final Logger log = Logger.getLogger(GenderEnum.class);

    private String id;

    private GenderEnum(String s) {
        id = s;
    }

    public String getId() {
        return id;
    }

    public static String getCode(String value) {
        if (StringUtils.isNotEmpty(value)) {
            try {
                return GenderEnum.valueOf(value.toUpperCase()).getId();

            } catch (IllegalArgumentException e) {
                log.error("Invalid gender value. ", e);
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public static GenderEnum forCode(String code) {
        GenderEnum[] genders = GenderEnum.values();
        for (GenderEnum gender : genders) {
            if (gender.getId().equals(code)) {
                return gender;
            }
        }
        return GenderEnum.O;
    }
}
