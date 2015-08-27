package org.openmrs.module.fhir.mapper.bundler.condition;

import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.QuantityDt;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.StringDt;
import org.apache.commons.collections.CollectionUtils;
import org.openmrs.Concept;
import org.openmrs.ConceptNumeric;
import org.openmrs.Obs;
import org.openmrs.module.fhir.utils.CodableConceptService;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.openmrs.module.fhir.mapper.FHIRProperties.*;

@Component
public class ObservationValueMapper {

    private final CodableConceptService codableConceptService;
    private IdMappingsRepository idMappingsRepository;

    private enum ValueReader {

        Numeric {
            @Override
            public IDatatype readValue(Obs obs, IdMappingsRepository idMappingsRepository, CodableConceptService codableConceptService) {
                if (obs.getConcept().getDatatype().isNumeric() && obs.getValueNumeric() != null) {
                    QuantityDt quantity = new QuantityDt();
                    quantity.setValue(obs.getValueNumeric());
                    if (obs.getConcept() instanceof ConceptNumeric) {
                        String units = ((ConceptNumeric) obs.getConcept()).getUnits();
                        if (units != null) quantity.setUnits(units);
                    }
                    return quantity;
                }
                return null;
            }
        },

        Text {
            @Override
            public IDatatype readValue(Obs obs, IdMappingsRepository idMappingsRepository, CodableConceptService codableConceptService) {
                if (obs.getConcept().getDatatype().isText() && obs.getValueText() != null) {
                    return new StringDt(obs.getValueText());
                }
                return null;
            }
        },

        Boolean {
            @Override
            public IDatatype readValue(Obs obs, IdMappingsRepository idMappingsRepository, CodableConceptService codableConceptService) {
                if (obs.getConcept().getDatatype().isBoolean() && obs.getValueAsBoolean() != null) {
                    CodeableConceptDt codeableConcept = new CodeableConceptDt();
                    CodingDt coding = codeableConcept.addCoding();
                    coding.setSystem(FHIR_YES_NO_INDICATOR_URL);
                    coding.setCode(obs.getValueBoolean() ? FHIR_YES_INDICATOR_CODE : FHIR_NO_INDICATOR_CODE);
                    coding.setDisplay(obs.getValueBoolean() ? FHIR_YES_INDICATOR_DISPLAY : FHIR_NO_INDICATOR_DISPLAY);
                    return codeableConcept;
                }
                return null;
            }
        },

        Date {
            @Override
            public IDatatype readValue(Obs obs, IdMappingsRepository idMappingsRepository, CodableConceptService codableConceptService) {
                if (obs.getConcept().getDatatype().isDate() && obs.getValueDate() != null) {
                    return new DateDt(obs.getValueDate(), TemporalPrecisionEnum.DAY);
                }
                return null;
            }
        },

        Coded {
            @Override
            public IDatatype readValue(Obs obs, IdMappingsRepository idMappingsRepository, CodableConceptService codableConceptService) {
                if (obs.getConcept().getDatatype().isCoded() && obs.getValueCoded() != null) {
                    Concept valueCoded = obs.getValueCoded();
                    CodeableConceptDt concept = codableConceptService.addTRCoding(valueCoded, idMappingsRepository);
                    if (CollectionUtils.isEmpty(concept.getCoding())) {
                        CodingDt coding = concept.addCoding();
                        coding.setDisplay(valueCoded.getName().getName());
                    }
                    return concept;
                }
                return null;
            }
        };

        public abstract IDatatype readValue(Obs obs, IdMappingsRepository idMappingsRepository, CodableConceptService codableConceptService);
    }

    @Autowired
    public ObservationValueMapper(IdMappingsRepository idMappingsRepository, CodableConceptService codableConceptService) {
        this.codableConceptService = codableConceptService;
        this.idMappingsRepository = idMappingsRepository;
    }

    public IDatatype map(Obs observation) {
        for (ValueReader valueReader : ValueReader.values()) {
            IDatatype readValue = valueReader.readValue(observation, idMappingsRepository, codableConceptService);
            if (null != readValue) {
                return readValue;
            }
        }
        return null;
    }
}
