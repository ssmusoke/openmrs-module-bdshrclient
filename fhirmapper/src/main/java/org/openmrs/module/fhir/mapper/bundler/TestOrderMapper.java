package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder;
import ca.uhn.fhir.model.dstu2.valueset.DiagnosticOrderStatusEnum;
import org.apache.commons.collections.CollectionUtils;
import org.openmrs.Concept;
import org.openmrs.Order;
import org.openmrs.module.fhir.MRSProperties;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.mapper.model.FHIREncounter;
import org.openmrs.module.fhir.mapper.model.FHIRResource;
import org.openmrs.module.fhir.utils.CodeableConceptService;
import org.openmrs.module.fhir.utils.ProviderLookupService;
import org.openmrs.module.shrclient.dao.IdMappingRepository;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static java.util.Arrays.asList;
import static org.openmrs.module.fhir.MRSProperties.MRS_LAB_ORDER_TYPE;

@Component("fhirTestOrderMapper")
public class TestOrderMapper implements EmrOrderResourceHandler {

    private static final int TEST_TYPE_POSITION_IN_SPLITED_CONCEPT_REF_TERM = 3;

    @Autowired
    private IdMappingRepository idMappingsRepository;

    @Autowired
    private CodeableConceptService codeableConceptService;
    @Autowired
    private ProviderLookupService providerLookupService;

    @Override
    public boolean canHandle(Order order) {
        return order.getOrderType().getName().equalsIgnoreCase(MRS_LAB_ORDER_TYPE);
    }

    @Override
    public List<FHIRResource> map(Order order, FHIREncounter fhirEncounter, Bundle bundle, SystemProperties systemProperties) {
        if (order.getDateStopped() != null) return Collections.EMPTY_LIST;
        DiagnosticOrder diagnosticOrder;
        diagnosticOrder = createDiagnosticOrder(order, fhirEncounter, systemProperties);
        addItemsToDiagnosticOrder(order, diagnosticOrder);
        if (CollectionUtils.isEmpty(diagnosticOrder.getItem())) {
            return null;
        }
        return asList(new FHIRResource("Diagnostic Order", diagnosticOrder.getIdentifier(), diagnosticOrder));
    }

    private void addItemsToDiagnosticOrder(Order order, DiagnosticOrder diagnosticOrder) {
        if (order.getConcept().getConceptClass().getName().equals(MRSProperties.MRS_CONCEPT_CLASS_LAB_SET)) {
            CodeableConceptDt panelOrderCode = codeableConceptService.addTRCoding(order.getConcept());
            if (panelOrderCode != null && !panelOrderCode.isEmpty()) {
                createOrderItem(order, diagnosticOrder, panelOrderCode);
            } else {
                for (Concept testConcept : order.getConcept().getSetMembers()) {
                    createOrderItemForTest(order, diagnosticOrder, testConcept);
                }
            }
        } else {
            createOrderItemForTest(order, diagnosticOrder, order.getConcept());
        }
    }

    private void createOrderItemForTest(Order order, DiagnosticOrder diagnosticOrder, Concept concept) {
        CodeableConceptDt orderCode = codeableConceptService.addTRCodingOrDisplay(concept);
        createOrderItem(order, diagnosticOrder, orderCode);
    }

    private void createOrderItem(Order order, DiagnosticOrder diagnosticOrder, CodeableConceptDt orderCode) {
        DiagnosticOrder.Item orderItem = diagnosticOrder.addItem();
        orderItem.setCode(orderCode);
        if (isDiscontinuedOrder(order)) {
            orderItem.setStatus(DiagnosticOrderStatusEnum.CANCELLED);
            addEvent(orderItem, DiagnosticOrderStatusEnum.REQUESTED, order.getPreviousOrder().getDateActivated());
            addEvent(orderItem, DiagnosticOrderStatusEnum.CANCELLED, order.getDateActivated());
        }
        else {
            orderItem.setStatus(DiagnosticOrderStatusEnum.REQUESTED);
            addEvent(orderItem, DiagnosticOrderStatusEnum.REQUESTED, order.getDateActivated());
        }
    }

    private boolean isDiscontinuedOrder(Order order) {
        return order.getAction().equals(Order.Action.DISCONTINUE);
    }

    private void addEvent(DiagnosticOrder.Item orderItem, DiagnosticOrderStatusEnum status, Date dateActivated) {
        DiagnosticOrder.Event event = orderItem.addEvent();
        event.setStatus(status);
        event.setDateTime(dateActivated, TemporalPrecisionEnum.MILLI);
    }

    private DiagnosticOrder createDiagnosticOrder(Order order, FHIREncounter fhirEncounter, SystemProperties systemProperties) {
        DiagnosticOrder diagnosticOrder;
        diagnosticOrder = new DiagnosticOrder();
        diagnosticOrder.setSubject(fhirEncounter.getPatient());
        diagnosticOrder.setOrderer(getOrdererReference(order, fhirEncounter, systemProperties));
        String orderUuid = order.getUuid();
        if (isDiscontinuedOrder(order)) 
            orderUuid = order.getPreviousOrder().getUuid();
        setDiagnosticOrderId(systemProperties, diagnosticOrder, orderUuid);
        diagnosticOrder.setEncounter(new ResourceReferenceDt().setReference(fhirEncounter.getId()));
        return diagnosticOrder;
    }

    private void setDiagnosticOrderId(SystemProperties systemProperties, DiagnosticOrder diagnosticOrder, String orderUuid) {
        String id = new EntityReference().build(Order.class, systemProperties, orderUuid);
        diagnosticOrder.addIdentifier().setValue(id);
        diagnosticOrder.setId(id);
    }

    private ResourceReferenceDt getOrdererReference(Order order, FHIREncounter fhirEncounter, SystemProperties systemProperties) {
        if (order.getOrderer() != null) {
            String providerUrl = providerLookupService.getProviderRegistryUrl(systemProperties, order.getOrderer());
            if (providerUrl != null) {
                return new ResourceReferenceDt().setReference(providerUrl);
            }
        }
        return fhirEncounter.getFirstParticipantReference();
    }
}