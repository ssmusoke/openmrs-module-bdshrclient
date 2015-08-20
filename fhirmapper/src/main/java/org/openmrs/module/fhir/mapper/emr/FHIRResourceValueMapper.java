package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.primitive.BooleanDt;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.DecimalDt;
import ca.uhn.fhir.model.primitive.StringDt;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
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

    public Obs map(IDatatype value, Obs obs) {
        if (null != value) {
            try {
                if (value instanceof StringDt) {
                    obs.setValueAsString(((StringDt) value).getValue());
                } else if (value instanceof DecimalDt) {
                    obs.setValueNumeric(((DecimalDt) value).getValue().doubleValue());
                } else if (value instanceof DateDt) {
                    obs.setValueDate(((DateDt) value).getValue());
                } else if (value instanceof DateTimeDt) {
                    obs.setValueDate(((DateTimeDt) value).getValue());
                } else if (value instanceof BooleanDt){
                    obs.setValueBoolean(((BooleanDt) value).getValue());
                } else if (value instanceof CodeableConceptDt) {
                    List<CodingDt> codings = ((CodeableConceptDt) value).getCoding();
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

    private Concept findConcept(List<CodingDt> codings) {
        Concept concept = omrsConceptLookup.findConcept(codings);
        if (concept != null) return concept;
        return conceptService.getConceptByName(codings.get(0).getDisplay());
    }

    public Obs mapObservationForConcept(IDatatype value, String conceptName) {
        Obs obs = new Obs();
        obs.setConcept(conceptService.getConceptByName(conceptName));
        return map(value, obs);
    }
}
