package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.BaseResource;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticOrder;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticReport;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.*;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir.MRSProperties;
import org.openmrs.module.fhir.mapper.model.EmrEncounter;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.mapper.model.ShrEncounterBundle;
import org.openmrs.module.fhir.utils.FHIRBundleHelper;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.openmrs.module.shrclient.dao.IdMappingRepository;
import org.openmrs.module.shrclient.model.EncounterIdMapping;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.model.IdMappingType;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.openmrs.module.fhir.MRSProperties.MRS_CONCEPT_CLASS_LAB_SET;
import static org.openmrs.module.fhir.MRSProperties.MRS_CONCEPT_NAME_LAB_NOTES;
import static org.openmrs.module.fhir.MRSProperties.RESOURCE_MAPPING_EXTERNAL_ID_FORMAT;
import static org.openmrs.module.fhir.utils.FHIRBundleHelper.findResourcesByReference;


@Component
public class FHIRDiagnosticReportMapper implements FHIRResourceMapper {
    private OMRSConceptLookup omrsConceptLookup;
    private FHIRObservationsMapper observationsMapper;
    private ConceptService conceptService;
    private OrderService orderService;
    private EncounterService encounterService;
    private IdMappingRepository idMappingRepository;

    @Autowired
    public FHIRDiagnosticReportMapper(OMRSConceptLookup omrsConceptLookup, FHIRObservationsMapper observationsMapper,
                                      ConceptService conceptService, OrderService orderService,
                                      EncounterService encounterService, IdMappingRepository idMappingRepository) {
        this.omrsConceptLookup = omrsConceptLookup;
        this.observationsMapper = observationsMapper;
        this.conceptService = conceptService;
        this.orderService = orderService;
        this.encounterService = encounterService;
        this.idMappingRepository = idMappingRepository;
    }

    @Override
    public boolean canHandle(IResource resource) {
        return resource instanceof DiagnosticReport;
    }

    @Override
    public void map(IResource resource, EmrEncounter emrEncounter, ShrEncounterBundle shrEncounterBundle, SystemProperties systemProperties) {
        DiagnosticReport diagnosticReport = (DiagnosticReport) resource;
        Order order = null;
        Concept concept = omrsConceptLookup.findConceptByCode(diagnosticReport.getCode().getCoding());
        if (concept != null) {
            order = getOrder(diagnosticReport, concept);
        } else {
            final ca.uhn.fhir.model.dstu2.resource.Encounter shrEncounter = FHIRBundleHelper.getEncounter(shrEncounterBundle.getBundle());
            String facilityId = new EntityReference().parse(Location.class, shrEncounter.getServiceProvider().getReference().getValue());
            ConceptDatatype textDatatype = conceptService.getConceptDatatypeByUuid(ConceptDatatype.TEXT_UUID);
            ConceptClass labSetClass = conceptService.getConceptClassByName(MRSProperties.MRS_CONCEPT_CLASS_LAB_SET);
            concept = omrsConceptLookup.createLocalConceptFromCodings(diagnosticReport.getCode().getCoding(), facilityId, labSetClass, textDatatype);
        }

        Obs topLevelResultObsGroup = buildObs(concept, order);

        Set<Obs> resultObsGroups = buildResultObsGroup(shrEncounterBundle, emrEncounter, diagnosticReport, order, concept);

        for (Obs resultObs : resultObsGroups) {
            topLevelResultObsGroup.addGroupMember(resultObs);
        }

        if (order != null && order.getConcept().getConceptClass().getName().equals(MRS_CONCEPT_CLASS_LAB_SET)) {
            Obs panelObs = findObsByOrder(emrEncounter, order);
            if (panelObs == null) {
                panelObs = buildObs(order.getConcept(), order);
            }
            panelObs.addGroupMember(topLevelResultObsGroup);
            emrEncounter.addObs(panelObs);
        }
        emrEncounter.addObs(topLevelResultObsGroup);
    }

    private Order getOrder(DiagnosticReport diagnosticReport, Concept concept) {
        List<ResourceReferenceDt> requestDetail = diagnosticReport.getRequest();
        Order order = findOrderFromOrderRequestDetail(concept, requestDetail);
        if (order != null) return order;
        order = findOrderFromEncounterRequestDetail(concept, requestDetail);
        return order;
    }

    private Order findOrderFromOrderRequestDetail(Concept concept, List<ResourceReferenceDt> requestDetail) {
        for (ResourceReferenceDt reference : requestDetail) {
            String requestDetailReference = reference.getReference().getValue();
            if (requestDetailReference.contains("#" + new DiagnosticOrder().getResourceName())) {
                String orderId = new EntityReference().parse(BaseResource.class, requestDetailReference);
                String encounterId = new EntityReference().parse(Encounter.class, requestDetailReference);
                String externalId = String.format(RESOURCE_MAPPING_EXTERNAL_ID_FORMAT, encounterId, orderId);
                List<IdMapping> idMappingList = idMappingRepository.findMappingsByExternalId(externalId, IdMappingType.DIAGNOSTIC_ORDER);
                if (CollectionUtils.isNotEmpty(idMappingList)) {
                    for (IdMapping idMapping : idMappingList) {
                        Order order = orderService.getOrderByUuid(idMapping.getInternalId());
                        if (order.getConcept().equals(concept))
                            return order;
                    }
                }
            }
        }
        return null;
    }

    private Order findOrderFromEncounterRequestDetail(Concept concept, List<ResourceReferenceDt> requestDetail) {
        for (ResourceReferenceDt reference : requestDetail) {
            String requestDetailReference = reference.getReference().getValue();
            String encounterId = new EntityReference().parse(Encounter.class, requestDetailReference);
            if (encounterId != null) {
                EncounterIdMapping encounterIdMapping = (EncounterIdMapping) idMappingRepository.findByExternalId(encounterId, IdMappingType.ENCOUNTER);
                if (encounterIdMapping == null) {
                    throw new RuntimeException(String.format("Encounter with id [%s] is not yet synced.", encounterId));
                }
                Encounter orderEncounter = encounterService.getEncounterByUuid(encounterIdMapping.getInternalId());
                return findOrderFromEncounter(orderEncounter.getOrders(), concept);
            }
        }
        return null;
    }

    private Order findOrderFromEncounter(Set<Order> orders, Concept concept) {
        for (Order order : orders) {
            if (isRunningOrder(order)) {
                Concept orderConcept = order.getConcept();
                if (orderConcept.equals(concept)) {
                    return order;
                } else if (orderConcept.getConceptClass().getName().equals(MRS_CONCEPT_CLASS_LAB_SET)) {
                    for (Concept setMember : orderConcept.getSetMembers()) {
                        if (setMember.equals(concept)) {
                            return order;
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isRunningOrder(Order order) {
        return Order.Action.NEW.equals(order.getAction()) && order.getDateStopped() == null;
    }

    private Obs getNotes(Observation observation, Order order) {
        String comments = observation.getComments();
        if (StringUtils.isNotBlank(comments)) {
            Concept labNotesConcept = conceptService.getConceptByName(MRS_CONCEPT_NAME_LAB_NOTES);
            Obs notesObs = buildObs(labNotesConcept, order);
            notesObs.setValueText(comments);
            return notesObs;
        }
        return null;
    }

    private Set<Obs> buildResultObsGroup(ShrEncounterBundle encounterComposition, EmrEncounter emrEncounter, DiagnosticReport diagnosticReport, Order order, Concept concept) {
        Set<Obs> resultObsGroups = new HashSet<>();
        List<IResource> resultObservationList = findResourcesByReference(encounterComposition.getBundle(), diagnosticReport.getResult());

        for (IResource resultObservation : resultObservationList) {
            Obs resultObsGroup = buildObs(concept, order);
            populateResultsAndNotes(encounterComposition, emrEncounter, order, (Observation) resultObservation, resultObsGroup);
            resultObsGroups.add(resultObsGroup);
        }
        return resultObsGroups;
    }

    private void populateResultsAndNotes(ShrEncounterBundle encounterComposition, EmrEncounter emrEncounter, Order order, Observation resultObservation, Obs resultObsGroup) {
        Obs resultObs = observationsMapper.mapObs(encounterComposition, emrEncounter, resultObservation);
        resultObs.setOrder(order);
        resultObsGroup.addGroupMember(resultObs);
        resultObsGroup.addGroupMember(getNotes(resultObservation, order));
    }

    private Obs findObsByOrder(EmrEncounter emrEncounter, Order order) {
        for (Obs obs : emrEncounter.getTopLevelObs()) {
            if (obs.getOrder().equals(order) && obs.getConcept().equals(order.getConcept())) {
                return obs;
            }
        }
        return null;
    }

    private Obs buildObs(Concept concept, Order order) {
        Obs obs = new Obs();
        obs.setConcept(concept);
        obs.setOrder(order);
        return obs;
    }
}
