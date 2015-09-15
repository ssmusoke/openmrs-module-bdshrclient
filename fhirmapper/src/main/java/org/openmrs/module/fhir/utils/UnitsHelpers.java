package org.openmrs.module.fhir.utils;

import ca.uhn.fhir.model.dstu2.valueset.UnitsOfTimeEnum;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static ca.uhn.fhir.model.dstu2.valueset.UnitsOfTimeEnum.*;


@Component
public class UnitsHelpers {
    private Map<String, FrequencyUnits> conceptNameToFrequencyUnitMapper;

    public UnitsHelpers() {
        buildConceptNameMapper();
    }

    public enum FrequencyUnits {
        ONCE_A_DAY("Once a day", 1, D, 1),
        TWICE_A_DAY("Twice a day", 2, D, 1),
        THRICE_A_DAY("Thrice a day", 3, D, 1),
        FOUR_TIMES_A_DAY("Four times a day", 4, D, 1),
        EVERY_HOUR("Every Hour", 1, H, 1),
        EVERY_TWO_HOURS("Every 2 hours", 1, H, 2),
        EVERY_THREE_HOURS("Every 3 hours", 1, H, 3),
        EVERY_FOUR_HOURS("Every 4 hours", 1, H, 4),
        EVERY_SIX_HOURS("Every 6 hours", 1, H, 6),
        EVERY_EIGHT_HOURS("Every 8 hours", 1, H, 8),
        EVERY_TWELVE_HOURS("Every 12 hours", 1, H, 12),
        ON_ALTERNATE_DAYS("On alternate days", 1, D, 2),
        ONCE_A_WEEK("Once a week", 1, WK, 1),
        TWICE_A_WEEK("Twice a week", 2, WK, 1),
        THRICE_A_WEEK("Thrice a week", 3, WK, 1),
        EVERY_TWO_WEEKS("Every 2 weeks", 1, WK, 2),
        EVERY_THREE_WEEKS("Every 3 weeks", 1, WK, 3),
        ONCE_A_MONTH("Once a month", 1, MO, 1);

        private final String conceptName;
        private int frequency;
        private final UnitsOfTimeEnum unitOfTime;
        private int frequencyPeriod;

        FrequencyUnits(String conceptName, int frequency, UnitsOfTimeEnum unitOfTime, int frequencyPeriod) {
            this.conceptName = conceptName;
            this.frequency = frequency;
            this.unitOfTime = unitOfTime;
            this.frequencyPeriod = frequencyPeriod;
        }

        public String getConceptName() {
            return conceptName;
        }

        public int getFrequency() {
            return frequency;
        }

        public UnitsOfTimeEnum getUnitOfTime() {
            return unitOfTime;
        }

        public int getFrequencyPeriod() {
            return frequencyPeriod;
        }
    }

    private void buildConceptNameMapper() {
        conceptNameToFrequencyUnitMapper = new HashMap<>();
        conceptNameToFrequencyUnitMapper.put("Once a day", FrequencyUnits.ONCE_A_DAY);
        conceptNameToFrequencyUnitMapper.put("Twice a day", FrequencyUnits.TWICE_A_DAY);
        conceptNameToFrequencyUnitMapper.put("Thrice a day", FrequencyUnits.THRICE_A_DAY);
        conceptNameToFrequencyUnitMapper.put("Four times a day", FrequencyUnits.FOUR_TIMES_A_DAY);
        conceptNameToFrequencyUnitMapper.put("Every Hour", FrequencyUnits.EVERY_HOUR);
        conceptNameToFrequencyUnitMapper.put("Every 2 hours", FrequencyUnits.EVERY_TWO_HOURS);
        conceptNameToFrequencyUnitMapper.put("Every 3 hours", FrequencyUnits.EVERY_THREE_HOURS);
        conceptNameToFrequencyUnitMapper.put("Every 4 hours", FrequencyUnits.EVERY_FOUR_HOURS);
        conceptNameToFrequencyUnitMapper.put("Every 6 hours", FrequencyUnits.EVERY_SIX_HOURS);
        conceptNameToFrequencyUnitMapper.put("Every 8 hours", FrequencyUnits.EVERY_EIGHT_HOURS);
        conceptNameToFrequencyUnitMapper.put("Every 12 hours", FrequencyUnits.EVERY_TWELVE_HOURS);
        conceptNameToFrequencyUnitMapper.put("On alternate days", FrequencyUnits.ON_ALTERNATE_DAYS);
        conceptNameToFrequencyUnitMapper.put("Once a week", FrequencyUnits.ONCE_A_WEEK);
        conceptNameToFrequencyUnitMapper.put("Twice a week", FrequencyUnits.TWICE_A_WEEK);
        conceptNameToFrequencyUnitMapper.put("Thrice a week", FrequencyUnits.THRICE_A_WEEK);
        conceptNameToFrequencyUnitMapper.put("Every 2 weeks", FrequencyUnits.EVERY_TWO_WEEKS);
        conceptNameToFrequencyUnitMapper.put("Every 3 weeks", FrequencyUnits.EVERY_THREE_WEEKS);
        conceptNameToFrequencyUnitMapper.put("Once a month", FrequencyUnits.ONCE_A_MONTH);
    }

    public FrequencyUnits getFrequencyUnits(String conceptName) {
        return conceptNameToFrequencyUnitMapper.get(conceptName);
    }

}
