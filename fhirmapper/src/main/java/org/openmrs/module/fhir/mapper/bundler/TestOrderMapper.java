package org.openmrs.module.fhir.mapper.bundler;

import org.apache.commons.collections.CollectionUtils;
import org.hl7.fhir.instance.model.AtomFeed;
import org.hl7.fhir.instance.model.CodeableConcept;
import org.hl7.fhir.instance.model.Coding;
import org.hl7.fhir.instance.model.DiagnosticOrder;
import org.hl7.fhir.instance.model.Encounter;
import org.hl7.fhir.instance.model.Enumeration;
import org.hl7.fhir.instance.model.Identifier;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.ResourceReference;
import org.hl7.fhir.instance.model.ResourceType;
import org.openmrs.Order;
import org.openmrs.TestOrder;
import org.openmrs.module.fhir.utils.FHIRFeedHelper;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("fhirTestOrderMapper")
public class TestOrderMapper implements EmrOrderResourceHandler {

    @Autowired
    private IdMappingsRepository idMappingsRepository;

    @Override
    public boolean handles(Order order) {
        return (order instanceof TestOrder) && order.getOrderType().getName().equals("Lab Order");
    }

    @Override
    public EmrResource map(Order order, Encounter fhirEncounter, AtomFeed feed) {
        DiagnosticOrder diagnosticOrder;
        Resource resource = FHIRFeedHelper.identifyResource(feed.getEntryList(), ResourceType.DiagnosticOrder);
        if (resource != null) {
            diagnosticOrder = (DiagnosticOrder) resource;
        } else {
            diagnosticOrder = createDiagnosticOrder(order, fhirEncounter);
        }
        addItemsToDiagnosticOrder(order, diagnosticOrder);
        if(CollectionUtils.isEmpty(diagnosticOrder.getItem())) {
            return null;
        }
        return new EmrResource("Diagnostic Order", diagnosticOrder.getIdentifier(), diagnosticOrder);
    }

    private void addItemsToDiagnosticOrder(Order order, DiagnosticOrder diagnosticOrder) {
        DiagnosticOrder.DiagnosticOrderItemComponent orderItem = diagnosticOrder.addItem();
        findOrderName(order);
        CodeableConcept orderCode = findOrderName(order);
        if (orderCode == null) return;
        orderItem.setCode(orderCode);
        orderItem.setStatusSimple(DiagnosticOrder.DiagnosticOrderStatus.requested);
        ResourceReference specimen = orderItem.addSpecimen();

    }

    private CodeableConcept findOrderName(Order order) {
        if (null == order.getConcept()) {
            return null;
        }
        IdMapping mapping = idMappingsRepository.findByInternalId(order.getConcept().getUuid());
        if (null == mapping) {
            CodeableConcept result = new CodeableConcept();
            Coding coding = result.addCoding();
            coding.setDisplaySimple(order.getConcept().getName().getName());
            return result;
        } else {
            CodeableConcept result = new CodeableConcept();
            Coding coding = result.addCoding();
            coding.setSystemSimple(mapping.getUri());
            coding.setDisplaySimple(order.getConcept().getName().getName());
            coding.setCodeSimple(mapping.getExternalId());
            return result;
        }
    }

    private DiagnosticOrder createDiagnosticOrder(Order order, Encounter fhirEncounter) {
        DiagnosticOrder diagnosticOrder;
        diagnosticOrder = new DiagnosticOrder();
        diagnosticOrder.setSubject(fhirEncounter.getSubject());
        ResourceReference orderer = new ResourceReference();
        orderer.setReferenceSimple(order.getOrderer().getUuid());
        diagnosticOrder.setOrderer(orderer);
        Identifier identifier = diagnosticOrder.addIdentifier();
        identifier.setValueSimple(UUID.randomUUID().toString());
        diagnosticOrder.setEncounter(fhirEncounter.getIndication());
        diagnosticOrder.setStatusSimple(DiagnosticOrder.DiagnosticOrderStatus.requested);
        return diagnosticOrder;
    }

}