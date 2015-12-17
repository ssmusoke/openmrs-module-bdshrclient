package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder;
import ca.uhn.fhir.model.dstu2.valueset.DiagnosticOrderStatusEnum;
import org.joda.time.DateTime;
import org.openmrs.Concept;
import org.openmrs.Order;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir.mapper.model.EmrEncounter;
import org.openmrs.module.fhir.mapper.model.ShrEncounter;
import org.openmrs.module.fhir.utils.GlobalPropertyLookUpService;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.openmrs.module.fhir.utils.OrderCareSettingLookupService;
import org.openmrs.module.fhir.utils.ProviderLookupService;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class FHIRDiagnosticOrderMapper implements FHIRResourceMapper {
    private static final String ORDER_NAME = "Lab Order";
    private static final String BAHMNI_ENCOUNTER_SESSION_PROPERTY_NAME = "bahmni.encountersession.duration";
    private static final String ORDER_DISCONTINUE_REASON = "Cancelled in SHR";
    private static final int BAHMNI_DEFAULT_ENCOUNTER_SESSION_TIME = 60;

    @Autowired
    private OMRSConceptLookup omrsConceptLookup;

    @Autowired
    private GlobalPropertyLookUpService globalPropertyLookUpService;

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
    public void map(IResource resource, EmrEncounter emrEncounter, ShrEncounter encounterComposition, SystemProperties systemProperties) {
        DiagnosticOrder diagnosticOrder = (DiagnosticOrder) resource;
        createTestOrders(encounterComposition.getBundle(), diagnosticOrder, emrEncounter);
    }

    private void createTestOrders(Bundle bundle, DiagnosticOrder diagnosticOrder, EmrEncounter emrEncounter) {
        List<DiagnosticOrder.Item> item = diagnosticOrder.getItem();
        for (DiagnosticOrder.Item diagnosticOrderItemComponent : item) {
            createTestOrderForItem(diagnosticOrder, diagnosticOrderItemComponent, bundle, emrEncounter);
        }
    }

    private void createTestOrderForItem(DiagnosticOrder diagnosticOrder, DiagnosticOrder.Item diagnosticOrderItemComponent, Bundle bundle, EmrEncounter emrEncounter) {
        Concept testOrderConcept = omrsConceptLookup.findConceptByCode(diagnosticOrderItemComponent.getCode().getCoding());
        if (testOrderConcept != null) {
            Order existingRunningOrder = getExistingRunningOrder(emrEncounter, testOrderConcept);
            if (isRequestedOrderAlreadyPresent(diagnosticOrderItemComponent, existingRunningOrder)) return;
            if (isCancelledOrderNotCreated(diagnosticOrderItemComponent, existingRunningOrder)) return;
            Order testOrder = createTestOrder(bundle, diagnosticOrder, emrEncounter, testOrderConcept);
            handleCancelledOrder(diagnosticOrderItemComponent, existingRunningOrder, testOrder);
            emrEncounter.addOrder(testOrder);
        }
    }

    private boolean isRequestedOrderAlreadyPresent(DiagnosticOrder.Item diagnosticOrderItemComponent, Order existingRunningOrder) {
        return isRequestedOrder(diagnosticOrderItemComponent) && existingRunningOrder != null;
    }

    private boolean isCancelledOrderNotCreated(DiagnosticOrder.Item diagnosticOrderItemComponent, Order existingRunningOrder) {
        return isCancelledOrder(diagnosticOrderItemComponent) && existingRunningOrder == null;
    }

    private Order createTestOrder(Bundle bundle, DiagnosticOrder diagnosticOrder, EmrEncounter emrEncounter, Concept testOrderConcept) {
        Order testOrder = new Order();
        testOrder.setOrderType(orderService.getOrderTypeByName(ORDER_NAME));
        testOrder.setConcept(testOrderConcept);
        setOrderer(testOrder, diagnosticOrder);
        Date encounterDatetime = emrEncounter.getEncounter().getEncounterDatetime();
        testOrder.setDateActivated(encounterDatetime);
        testOrder.setAutoExpireDate(getAutoExpireDate(encounterDatetime));
        testOrder.setCareSetting(orderCareSettingLookupService.getCareSetting(bundle));
        return testOrder;
    }

    private Date getAutoExpireDate(Date encounterDatetime) {
        String configuredSessionDuration = globalPropertyLookUpService.getGlobalPropertyValue(BAHMNI_ENCOUNTER_SESSION_PROPERTY_NAME);
        int encounterSessionDuration = BAHMNI_DEFAULT_ENCOUNTER_SESSION_TIME;
        if (configuredSessionDuration != null) {
            encounterSessionDuration = Integer.parseInt(configuredSessionDuration);
        }
        return new DateTime(encounterDatetime.getTime()).plusMinutes(encounterSessionDuration).toDate();
    }

    private void handleCancelledOrder(DiagnosticOrder.Item diagnosticOrderItemComponent, Order existingRunningOrder, Order testOrder) {
        if (isCancelledOrder(diagnosticOrderItemComponent)) {
            testOrder.setAction(Order.Action.DISCONTINUE);
            testOrder.setOrderReasonNonCoded(ORDER_DISCONTINUE_REASON);
            testOrder.setPreviousOrder(existingRunningOrder);
        }
    }

    private boolean isCancelledOrder(DiagnosticOrder.Item diagnosticOrderItemComponent) {
        return DiagnosticOrderStatusEnum.CANCELLED.getCode().equals(diagnosticOrderItemComponent.getStatus());
    }

    private boolean isRequestedOrder(DiagnosticOrder.Item diagnosticOrderItemComponent) {
        return DiagnosticOrderStatusEnum.REQUESTED.getCode().equals(diagnosticOrderItemComponent.getStatus());
    }

    private Order getExistingRunningOrder(EmrEncounter emrEncounter, Concept testOrderConcept) {
        for (Order order : emrEncounter.getEncounter().getOrders()) {
            if (order.getConcept().equals(testOrderConcept) && isRunningOrder(order)) {
                return order;
            }
        }
        return null;
    }

    private boolean isRunningOrder(Order order) {
        return Order.Action.NEW.equals(order.getAction()) && order.getDateStopped() == null;
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
