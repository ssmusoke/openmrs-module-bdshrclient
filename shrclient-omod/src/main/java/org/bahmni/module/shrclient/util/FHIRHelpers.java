package org.bahmni.module.shrclient.util;

import org.hl7.fhir.instance.model.CodeableConcept;
import org.hl7.fhir.instance.model.Coding;

public class FHIRHelpers {

    public CodeableConcept getFHIRCodeableConcept(String code, String system, String display) {
        CodeableConcept codeableConcept = new CodeableConcept();
        Coding coding = codeableConcept.addCoding();
        coding.setCodeSimple(code);
        coding.setSystemSimple(system);
        coding.setDisplaySimple(display);
        return codeableConcept;
    }
}
