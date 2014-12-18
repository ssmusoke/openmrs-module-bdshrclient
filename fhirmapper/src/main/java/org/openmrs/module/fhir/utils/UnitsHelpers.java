package org.openmrs.module.fhir.utils;

import org.hl7.fhir.instance.model.Schedule.UnitsOfTime;

import java.util.HashMap;
import java.util.Map;

import static org.hl7.fhir.instance.model.Schedule.UnitsOfTime.*;

public class UnitsHelpers {
    public enum UnitToDaysConverter {
        Hour("Hour(s)", h, 0.041667),
        Day("Days(s)", d, 1),
        Week("Week(s)", wk, 7),
        Month("Months(s)", mo, 30);

        private final String units;
        private final UnitsOfTime unitsOfTime;
        private final double inDays;

        UnitToDaysConverter(String units, UnitsOfTime unitsOfTime, double inDays) {
            this.units = units;
            this.unitsOfTime = unitsOfTime;
            this.inDays = inDays;
        }

        public String getUnits() {
            return units;
        }

        public UnitsOfTime getUnitsOfTime() {
            return unitsOfTime;
        }

        public double getInDays() {
            return inDays;
        }
    }

    private Map<String, UnitsOfTime> conceptNameToUnitMapper;
    private Map<String, UnitToDaysConverter> durationUnitToUnitMapper;
    private Map<UnitsOfTime, UnitToDaysConverter> unitsOfTimeMapper;

    public UnitsHelpers() {
        buildConceptNameMapper();
        buildDurationUnitMapper();
        buildUnitsOfTimeToUnitsMapper();
    }

    private void buildUnitsOfTimeToUnitsMapper() {
        unitsOfTimeMapper = new HashMap<>();
        unitsOfTimeMapper.put(h, UnitToDaysConverter.Hour);
        unitsOfTimeMapper.put(d, UnitToDaysConverter.Day);
        unitsOfTimeMapper.put(wk, UnitToDaysConverter.Week);
        unitsOfTimeMapper.put(mo, UnitToDaysConverter.Month);
    }

    private void buildDurationUnitMapper() {
        durationUnitToUnitMapper = new HashMap<>();
        durationUnitToUnitMapper.put("Hour(s)", UnitToDaysConverter.Hour);
        durationUnitToUnitMapper.put("Day(s)", UnitToDaysConverter.Day);
        durationUnitToUnitMapper.put("Week(s)", UnitToDaysConverter.Week);
        durationUnitToUnitMapper.put("Months(s)", UnitToDaysConverter.Month);
    }

    private void buildConceptNameMapper() {
        conceptNameToUnitMapper = new HashMap<>();
        conceptNameToUnitMapper.put("Once a day", d);
        conceptNameToUnitMapper.put("Twice a day", d);
        conceptNameToUnitMapper.put("Thrice a day", d);
        conceptNameToUnitMapper.put("Four times a day", d);
        conceptNameToUnitMapper.put("Every Hour", d);
        conceptNameToUnitMapper.put("Every 2 hours", d);
        conceptNameToUnitMapper.put("Every 3 hours", d);
        conceptNameToUnitMapper.put("Every 4 hours", d);
        conceptNameToUnitMapper.put("Every 6 hours", d);
        conceptNameToUnitMapper.put("Every 8 hours", d);
        conceptNameToUnitMapper.put("Every 12 hours", d);
        conceptNameToUnitMapper.put("On alternate days", d);
        conceptNameToUnitMapper.put("Once a week", wk);
        conceptNameToUnitMapper.put("Twice a week", wk);
        conceptNameToUnitMapper.put("Thrice a week", wk);
        conceptNameToUnitMapper.put("Every 2 weeks", wk);
        conceptNameToUnitMapper.put("Every 3 weeks", wk);
        conceptNameToUnitMapper.put("Once a month", mo);
    }

    public Map<String, UnitToDaysConverter> getDurationUnitToUnitMapper() {
        return durationUnitToUnitMapper;
    }

    public UnitsOfTime getUnitsOfTime(String conceptName) {
        return conceptNameToUnitMapper.get(conceptName);
    }

    public Map<UnitsOfTime, UnitToDaysConverter> getUnitsOfTimeMapper() {
        return unitsOfTimeMapper;
    }
}
