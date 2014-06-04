package org.openmrs.module.bdshrclient.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public enum GenderEnum {
    M("1"), F("2"), O("3");

    private static final Log log = LogFactory.getLog(GenderEnum.class);

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
            }
        }
        return null;
    }
}
