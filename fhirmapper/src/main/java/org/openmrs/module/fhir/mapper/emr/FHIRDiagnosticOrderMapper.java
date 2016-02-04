package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder;
import ca.uhn.fhir.model.dstu2.valueset.DiagnosticOrderStatusEnum;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.Concept;
import org.openmrs.Order;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir.MRSProperties;
import org.openmrs.module.fhir.mapper.model.EmrEncounter;
import org.openmrs.module.fhir.mapper.model.ShrEncounterBundle;
import org.openmrs.module.fhir.utils.DateUtil;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.openmrs.module.fhir.utils.OrderCareSettingLookupService;
import org.openmrs.module.fhir.utils.ProviderLookupService;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class FHIRDiagnosticOrderMapper implements FHIRResourceMapper {
    private static final String ORDER_NAME = "Lab Order";

    @Autowired
    private OMRSConceptLookup omrsConceptLookup;

    @Autowired
    private ProviderLookupService providerLookupService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderCareSettingLookupService orderCareSettingLookupService;

    @Override
    public boolean canHandle(IResource resource) {
        return resource instanceof DiagnosticOrder;
    }

    @Override
    public void map(IResource resource, EmrEncounter emrEncounter, ShrEncounterBundle shrEncounterBundle, SystemProperties systemProperties) {
        DiagnosticOrder diagnosticOrder = (DiagnosticOrder) resource;
        createTestOrders(shrEncounterBundle.getBundle(), diagnosticOrder, emrEncounter);
    }

    private void createTestOrders(Bundle bundle, DiagnosticOrder diagnosticOrder, EmrEncounter emrEncounter) {
        List<DiagnosticOrder.Item> cancelledItems = getCancelledDiagnosticOrderItems(diagnosticOrder);
        for (DiagnosticOrder.Item diagnosticOrderItemComponent : cancelledItems) {
            cancelTestOrderForItem(diagnosticOrder, diagnosticOrderItemComponent, bundle, emrEncounter);
        }
        List<DiagnosticOrder.Item> requestedItems = getRequestedDiagnosticOrderItems(diagnosticOrder);
        for (DiagnosticOrder.Item diagnosticOrderItemComponent : requestedItems) {
            createTestOrderForItem(diagnosticOrder, diagnosticOrderItemComponent, bundle, emrEncounter);
        }
    }

    private List<DiagnosticOrder.Item> getRequestedDiagnosticOrderItems(DiagnosticOrder diagnosticOrder) {
        ArrayList<DiagnosticOrder.Item> items = new ArrayList<>();
        for (DiagnosticOrder.Item item : diagnosticOrder.getItem()) {
            if (isRequestedOrder(item, diagnosticOrder))
                items.add(item);
        }
        return items;
    }

    private List<DiagnosticOrder.Item> getCancelledDiagnosticOrderItems(DiagnosticOrder diagnosticOrder) {
        ArrayList<DiagnosticOrder.Item> items = new ArrayList<>();
        for (DiagnosticOrder.Item item : diagnosticOrder.getItem()) {
            if (isCancelledOrder(item, diagnosticOrder))
                items.add(item);
        }
        return items;
    }

    private void createTestOrderForItem(DiagnosticOrder diagnosticOrder, DiagnosticOrder.Item diagnosticOrderItemComponent, Bundle bundle, EmrEncounter emrEncounter) {
        Concept testOrderConcept = omrsConceptLookup.findConceptByCode(diagnosticOrderItemComponent.getCode().getCoding());
        if (testOrderConcept != null) {
            Order existingRunningOrder = getExistingRunningOrder(emrEncounter, testOrderConcept);
            if (isRequestedOrderAlreadyPresent(diagnosticOrderItemComponent, diagnosticOrder, existingRunningOrder))
                return;
            Order testOrder = createRequestedTestOrder(diagnosticOrderItemComponent, bundle, diagnosticOrder, emrEncounter, testOrderConcept);
            emrEncounter.addOrder(testOrder);
        }
    }

    private void cancelTestOrderForItem(DiagnosticOrder diagnosticOrder, DiagnosticOrder.Item diagnosticOrderItemComponent, Bundle bundle, EmrEncounter emrEncounter) {
        Concept testOrderConcept = omrsConceptLookup.findConceptByCode(diagnosticOrderItemComponent.getCode().getCoding());
        if (testOrderConcept != null) {
            Order existingRunningOrder = getExistingRunningOrder(emrEncounter, testOrderConcept);
            if (isCancelledOrderNotCreated(diagnosticOrderItemComponent, diagnosticOrder, existingRunningOrder)) return;
            Order testOrder = createCancelledTestOrder(diagnosticOrderItemComponent, bundle, diagnosticOrder, emrEncounter, testOrderConcept);
            handleCancelledOrder(diagnosticOrderItemComponent, diagnosticOrder, existingRunningOrder, testOrder);
            emrEncounter.addOrder(testOrder);
        }
    }

    private boolean isRequestedOrderAlreadyPresent(DiagnosticOrder.Item diagnosticOrderItemComponent, DiagnosticOrder diagnosticOrder, Order existingRunningOrder) {
        return isRequestedOrder(diagnosticOrderItemComponent, diagnosticOrder) && existingRunningOrder != null;
    }

    private boolean isExistingOrderDiscontinued(Order existingRunningOrder, EmrEncounter emrEncounter) {
        for (Order order : emrEncounter.getOrders()) {
            if (existingRunningOrder.equals(order.getPreviousOrder())) return true;
        }
        return false;
    }

    private boolean isCancelledOrderNotCreated(DiagnosticOrder.Item diagnosticOrderItemComponent, DiagnosticOrder diagnosticOrder, Order existingRunningOrder) {
        return isCancelledOrder(diagnosticOrderItemComponent, diagnosticOrder) && existingRunningOrder == null;
    }

    private Order createCancelledTestOrder(DiagnosticOrder.Item item, Bundle bundle, DiagnosticOrder diagnosticOrder, EmrEncounter emrEncounter, Concept testOrderConcept) {
        Date dateActivated = getDateActivatedFromEventWithStatus(item, diagnosticOrder, DiagnosticOrderStatusEnum.CANCELLED);
        if (dateActivated == null)
            dateActivated = DateUtil.aSecondAfter(emrEncounter.getEncounter().getEncounterDatetime());
        return createTestOrder(bundle, diagnosticOrder, testOrderConcept, dateActivated);
    }

    private Order createRequestedTestOrder(DiagnosticOrder.Item item, Bundle bundle, DiagnosticOrder diagnosticOrder, EmrEncounter emrEncounter, Concept testOrderConcept) {
        Date dateActivated = getDateActivatedFromEventWithStatus(item, diagnosticOrder, DiagnosticOrderStatusEnum.REQUESTED);
        if (dateActivated == null) dateActivated = emrEncounter.getEncounter().getEncounterDatetime();
        return createTestOrder(bundle, diagnosticOrder, testOrderConcept, dateActivated);
    }

    private Order createTestOrder(Bundle bundle, DiagnosticOrder diagnosticOrder, Concept testOrderConcept, Date dateActivated) {
        Order testOrder = new Order();
        testOrder.setOrderType(orderService.getOrderTypeByName(ORDER_NAME));
        testOrder.setConcept(testOrderConcept);
        setOrderer(testOrder, diagnosticOrder);
        testOrder.setCareSetting(orderCareSettingLookupService.getCareSetting(bundle));
        testOrder.setDateActivated(dateActivated);
        testOrder.setAutoExpireDate(getAutoExpireDate(dateActivated));
        return testOrder;
    }

    private Date getDateActivatedFromEventWithStatus(DiagnosticOrder.Item item, DiagnosticOrder diagnosticOrder, DiagnosticOrderStatusEnum status) {
        DiagnosticOrder.Event event = getEvent(item.getEvent(), status);
        if (event != null) return event.getDateTime();
        event = getEvent(diagnosticOrder.getEvent(), status);
        if (event != null) return event.getDateTime();
        return null;
    }

    private DiagnosticOrder.Event getEvent(List<DiagnosticOrder.Event> events, DiagnosticOrderStatusEnum requested) {
        for (DiagnosticOrder.Event event : events) {
            if (event.getStatus().equals(requested.getCode())) return event;
        }
        return null;
    }

    private Date getAutoExpireDate(Date encounterDatetime) {
        return DateUtil.addMinutes(encounterDatetime, MRSProperties.ORDER_AUTO_EXPIRE_DURATION_MINUTES);
    }

    private void handleCancelledOrder(DiagnosticOrder.Item diagnosticOrderItemComponent, DiagnosticOrder diagnosticOrder, Order existingRunningOrder, Order testOrder) {
        if (isCancelledOrder(diagnosticOrderItemComponent, diagnosticOrder)) {
            testOrder.setAction(Order.Action.DISCONTINUE);
            testOrder.setOrderReasonNonCoded(MRSProperties.ORDER_DISCONTINUE_REASON);
            testOrder.setPreviousOrder(existingRunningOrder);
        }
    }

    private boolean isCancelledOrder(DiagnosticOrder.Item diagnosticOrderItemComponent, DiagnosticOrder diagnosticOrder) {
        if (!isItemStatusEmpty(diagnosticOrderItemComponent.getStatus()))
            return DiagnosticOrderStatusEnum.CANCELLED.getCode().equals(diagnosticOrderItemComponent.getStatus());
        if (!isItemStatusEmpty(diagnosticOrder.getStatus()))
            return DiagnosticOrderStatusEnum.CANCELLED.getCode().equals(diagnosticOrder.getStatus());
        return false;
    }

    private boolean isRequestedOrder(DiagnosticOrder.Item diagnosticOrderItemComponent, DiagnosticOrder diagnosticOrder) {
        if (!isItemStatusEmpty(diagnosticOrderItemComponent.getStatus()))
            return DiagnosticOrderStatusEnum.REQUESTED.getCode().equals(diagnosticOrderItemComponent.getStatus());
        if (!isItemStatusEmpty(diagnosticOrder.getStatus()))
            return DiagnosticOrderStatusEnum.REQUESTED.getCode().equals(diagnosticOrder.getStatus());
        return true;
    }

    private boolean isItemStatusEmpty(String status) {
        return status == null || StringUtils.isEmpty(status);
    }

    private Order getExistingRunningOrder(EmrEncounter emrEncounter, Concept testOrderConcept) {
        for (Order order : emrEncounter.getEncounter().getOrders()) {
            if (order.getConcept().equals(testOrderConcept) && isRunningOrder(order, emrEncounter)) {
                return order;
            }
        }
        return null;
    }

    private boolean isRunningOrder(Order order, EmrEncounter emrEncounter) {
        return Order.Action.NEW.equals(order.getAction()) && order.getDateStopped() == null
                && !isExistingOrderDiscontinued(order, emrEncounter);
    }

    private void setOrderer(Order testOrder, DiagnosticOrder diagnosticOrder) {
        ResourceReferenceDt orderer = diagnosticOrder.getOrderer();
        String practitionerReferenceUrl = null;
        if (orderer != null && !orderer.isEmpty()) {
            practitionerReferenceUrl = orderer.getReference().getValue();
        }
        testOrder.setOrderer(providerLookupService.getProviderByReferenceUrlOrDefault(practitionerReferenceUrl));
    }
}
