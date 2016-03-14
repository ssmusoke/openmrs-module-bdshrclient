package org.openmrs.module.fhir.utils;

import ca.uhn.fhir.model.dstu2.valueset.UnitsOfTimeEnum;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static ca.uhn.fhir.model.dstu2.valueset.UnitsOfTimeEnum.D;
import static ca.uhn.fhir.model.dstu2.valueset.UnitsOfTimeEnum.H;
import static ca.uhn.fhir.model.dstu2.valueset.UnitsOfTimeEnum.MIN;
import static ca.uhn.fhir.model.dstu2.valueset.UnitsOfTimeEnum.MO;
import static ca.uhn.fhir.model.dstu2.valueset.UnitsOfTimeEnum.WK;

@Component
public class DurationMapperUtil {

    private Map<String, UnitsOfTimeEnum> unitsOfTimeMap;
    private Map<UnitsOfTimeEnum, String> conceptNameToUnitsOfTimeMap;

    public DurationMapperUtil() {
        buildUnitOfTimeMap();
        buildConceptNameToUnitOfTimeMap();
    }

    private void buildUnitOfTimeMap() {
        unitsOfTimeMap = new HashMap<>();
        unitsOfTimeMap.put("Minute(s)", MIN);
        unitsOfTimeMap.put("Hour(s)", H);
        unitsOfTimeMap.put("Day(s)", D);
        unitsOfTimeMap.put("Week(s)", WK);
        unitsOfTimeMap.put("Month(s)", MO);
    }

    private void buildConceptNameToUnitOfTimeMap() {
        conceptNameToUnitsOfTimeMap = new HashMap<>();
        conceptNameToUnitsOfTimeMap.put(MIN, "Minute(s)");
        conceptNameToUnitsOfTimeMap.put(H, "Hour(s)");
        conceptNameToUnitsOfTimeMap.put(D, "Day(s)");
        conceptNameToUnitsOfTimeMap.put(WK, "Week(s)");
        conceptNameToUnitsOfTimeMap.put(MO, "Month(s)");
    }

    public UnitsOfTimeEnum getUnitOfTime(String conceptName) {
        return unitsOfTimeMap.get(conceptName);
    }

    public String getConceptNameFromUnitOfTime(UnitsOfTimeEnum unitsOfTime) {
        return conceptNameToUnitsOfTimeMap.get(unitsOfTime);
    }
}
