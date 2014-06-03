package org.openmrs.module.bdshrclient.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum GenderEnum {
    MALE("1"), FEMALE("2"), OTHER("3");

    private static final Logger logger = LoggerFactory.getLogger(GenderEnum.class);

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
                logger.error("Invalid gender value. ", e);
            }
        }
        return null;
    }
}
