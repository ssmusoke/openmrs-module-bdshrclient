package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder;
import ca.uhn.fhir.model.primitive.StringDt;
import org.apache.commons.collections.CollectionUtils;
import org.openmrs.Concept;
import org.openmrs.Order;
import org.openmrs.module.fhir.MRSProperties;
import org.openmrs.module.fhir.mapper.model.FHIREncounter;
import org.openmrs.module.fhir.mapper.model.FHIRResource;
import org.openmrs.module.fhir.utils.CodeableConceptService;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.openmrs.module.fhir.FHIRProperties.DIAGNOSTIC_ORDER_CATEGORY_EXTENSION_NAME;
import static org.openmrs.module.fhir.FHIRProperties.FHIR_DIAGNOSTIC_REPORT_CATEGORY_RADIOLOGY_CODE;
import static org.openmrs.module.fhir.FHIRProperties.getFhirExtensionUrl;

@Component("fhirRadiologyOrderMapper")
public class RadiologyOrderMapper implements EmrOrderResourceHandler {

    @Autowired
    private CodeableConceptService codeableConceptService;
    @Autowired
    private DiagnosticOrderBuilder orderBuilder;

    @Override
    public boolean canHandle(Order order) {
        return order.getOrderType().getName().equals(MRSProperties.MRS_RADIOLOGY_ORDER_TYPE);
    }

    @Override
    public List<FHIRResource> map(Order order, FHIREncounter fhirEncounter, Bundle bundle, SystemProperties systemProperties) {
        if (order.getDateStopped() != null) return Collections.EMPTY_LIST;
        DiagnosticOrder diagnosticOrder = orderBuilder.createDiagnosticOrder(order, fhirEncounter, systemProperties);
        String fhirExtensionUrl = getFhirExtensionUrl(DIAGNOSTIC_ORDER_CATEGORY_EXTENSION_NAME);
        diagnosticOrder.addUndeclaredExtension(false, fhirExtensionUrl, new StringDt(FHIR_DIAGNOSTIC_REPORT_CATEGORY_RADIOLOGY_CODE));
        createOrderItemForTest(order, diagnosticOrder, order.getConcept());
        if (CollectionUtils.isEmpty(diagnosticOrder.getItem())) {
            return null;
        }
        return asList(new FHIRResource("Diagnostic Order", diagnosticOrder.getIdentifier(), diagnosticOrder));
    }

    private void createOrderItemForTest(Order order, DiagnosticOrder diagnosticOrder, Concept concept) {
        CodeableConceptDt orderCode = codeableConceptService.addTRCodingOrDisplay(concept);
        diagnosticOrder.addItem(orderBuilder.createOrderItem(order, orderCode));
    }
}