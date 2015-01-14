package org.openmrs.module.fhir.mapper.emr;

import org.hl7.fhir.instance.model.Boolean;
import org.hl7.fhir.instance.model.*;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir.utils.DateUtil;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.List;

@Component
public class FHIRResourceValueMapper {

    @Autowired
    private OMRSConceptLookup omrsConceptLookup;
    @Autowired
    private ConceptService conceptService;

    public Obs map(Type value, Obs obs) {
        if (null != value) {
            try {
                if (value instanceof String_) {
                    obs.setValueAsString(((String_) value).getValue());
                } else if (value instanceof Decimal) {
                    obs.setValueNumeric(((Decimal) value).getValue().doubleValue());
                } else if (value instanceof Date) {
                    DateAndTime date = ((Date) value).getValue();
                    java.util.Date parsedDate = DateUtil.parseDate(date.toString());
                    obs.setValueDate(parsedDate);
                } else if (value instanceof DateTime) {
                    DateAndTime date = ((DateTime) value).getValue();
                    java.util.Date parsedDate = DateUtil.parseDate(date.toString());
                    obs.setValueDate(parsedDate);
                } else if (value instanceof Boolean){
                    java.lang.Boolean bool = ((Boolean) value).getValue();
                    obs.setValueBoolean(bool);
                } else if (value instanceof CodeableConcept) {
                    List<Coding> codings = ((CodeableConcept) value).getCoding();
                /* TODO: The last element of codings is the concept. Make this more explicit*/
                    Drug drug = omrsConceptLookup.findDrug(codings);
                    if(drug != null){
                        obs.setValueCoded(drug.getConcept());
                    }else{
                        obs.setValueCoded(findConcept(codings));
                    }
                }
                return obs;
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private Concept findConcept(List<Coding> codings) {
        Concept concept = omrsConceptLookup.findConcept(codings);
        if (concept != null) return concept;
        return conceptService.getConceptByName(codings.get(codings.size() - 1).getDisplaySimple());
    }

    public Obs mapObservationForConcept(Type value, String conceptName) {
        Obs obs = new Obs();
        obs.setConcept(conceptService.getConceptByName(conceptName));
        return map(value, obs);
    }
}
