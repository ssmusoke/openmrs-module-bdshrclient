package org.openmrs.module.fhir.mapper.bundler.condition;

import org.apache.commons.collections.CollectionUtils;
import org.hl7.fhir.instance.model.*;
import org.hl7.fhir.instance.model.Boolean;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static org.openmrs.module.fhir.utils.FHIRFeedHelper.addReferenceCodes;

@Component
public class ObservationValueMapper {

    private IdMappingsRepository idMappingsRepository;

    private enum ValueReader {

        Numeric {
            @Override
            public Type readValue(Obs obs, IdMappingsRepository idMappingsRepository) {
                if (obs.getConcept().getDatatype().isNumeric()) {
                    Decimal decimal = new Decimal();
                    decimal.setValue(new BigDecimal(obs.getValueNumeric()));
                    return decimal;
                }
                return null;
            }
        },

        Text {
            @Override
            public Type readValue(Obs obs, IdMappingsRepository idMappingsRepository) {
                if (obs.getConcept().getDatatype().isText()) {
                    String_ text = new String_();
                    text.setValue(obs.getValueText());
                    return text;
                }
                return null;
            }
        },

        Coded {
            @Override
            public Type readValue(Obs obs, IdMappingsRepository idMappingsRepository) {
                if (obs.getConcept().getDatatype().isCoded()) {
                    Concept valueCoded = obs.getValueCoded();
                    CodeableConcept concept = addReferenceCodes(valueCoded, idMappingsRepository);
                    if (CollectionUtils.isEmpty(concept.getCoding())) {
                        Coding coding = concept.addCoding();
                        coding.setDisplaySimple(valueCoded.getName().getName());
                    }
                    return concept;
                }
                return null;
            }
        };

        public abstract Type readValue(Obs obs, IdMappingsRepository idMappingsRepository);
    }

    @Autowired
    public ObservationValueMapper(IdMappingsRepository idMappingsRepository) {
        this.idMappingsRepository = idMappingsRepository;
    }

    public Type map(Obs observation) {
        for (ValueReader valueReader : ValueReader.values()) {
            Type readValue = valueReader.readValue(observation, idMappingsRepository);
            if (null != readValue) {
                return readValue;
            }
        }
        return null;
    }
}
