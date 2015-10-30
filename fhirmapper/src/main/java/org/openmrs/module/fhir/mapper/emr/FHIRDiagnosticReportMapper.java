package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticReport;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.*;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


import static org.openmrs.module.fhir.MRSProperties.MRS_CONCEPT_CLASS_LAB_SET;
import static org.openmrs.module.fhir.MRSProperties.MRS_CONCEPT_NAME_LAB_NOTES;
import static org.openmrs.module.fhir.utils.FHIRFeedHelper.findResourceByReference;
import static org.openmrs.module.fhir.utils.FHIRFeedHelper.findResourcesByReference;


@Component
public class FHIRDiagnosticReportMapper implements FHIRResourceMapper {
    private OMRSConceptLookup omrsConceptLookup;
    private FHIRObservationsMapper observationsMapper;
    private ConceptService conceptService;
    private EncounterService encounterService;
    private IdMappingsRepository idMappingsRepository;

    @Autowired
    public FHIRDiagnosticReportMapper(OMRSConceptLookup omrsConceptLookup,
                                      FHIRObservationsMapper observationsMapper,
                                      ConceptService conceptService,
                                      EncounterService encounterService,
                                      IdMappingsRepository idMappingsRepository) {
        this.omrsConceptLookup = omrsConceptLookup;
        this.observationsMapper = observationsMapper;
        this.conceptService = conceptService;
        this.encounterService = encounterService;
        this.idMappingsRepository = idMappingsRepository;
    }

    @Override
    public boolean canHandle(IResource resource) {
        return resource instanceof DiagnosticReport;
    }

    @Override
    public void map(Bundle bundle, IResource resource, Patient emrPatient, Encounter newEmrEncounter) {
        DiagnosticReport diagnosticReport = (DiagnosticReport) resource;
        Concept concept = omrsConceptLookup.findConceptByCode(diagnosticReport.getCode().getCoding());
        if (concept == null) {
            return;
        }
        Order order = getOrder(diagnosticReport, concept);
        if (order == null) {
            return;
        }
        Obs topLevelResultObsGroup = buildObs(concept, order);

        Set<Obs> resultObsGroups = buildResultObsGroup(bundle, newEmrEncounter, diagnosticReport, order, concept);

        topLevelResultObsGroup.setGroupMembers(resultObsGroups);

        if (order.getConcept().getConceptClass().getName().equals(MRS_CONCEPT_CLASS_LAB_SET)) {
            Obs panelObs = findObsByOrder(newEmrEncounter, order);
            if (panelObs == null) {
                panelObs = buildObs(order.getConcept(), order);
            }
            panelObs.addGroupMember(topLevelResultObsGroup);
            newEmrEncounter.addObs(panelObs);
        }
        newEmrEncounter.addObs(topLevelResultObsGroup);
    }

    private Order getOrder(DiagnosticReport diagnosticReport, Concept concept) {
        List<ResourceReferenceDt> requestDetail = diagnosticReport.getRequest();
        for (ResourceReferenceDt reference : requestDetail) {
            String requestDetailReference = reference.getReference().getValue();
            if (requestDetailReference.startsWith("http://") || requestDetailReference.startsWith("https://")) {
                String shrEncounterId = new EntityReference().parse(Encounter.class, requestDetailReference);
                IdMapping orderEncounterIdMapping = idMappingsRepository.findByExternalId(shrEncounterId);
                Encounter orderEncounter = encounterService.getEncounterByUuid(orderEncounterIdMapping.getInternalId());
                return findOrderFromEncounter(orderEncounter.getOrders(), concept);
            }
        }
        return null;
    }

    private Order findOrderFromEncounter(Set<Order> orders, Concept concept) {
        for (Order order : orders) {
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
        return null;
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

    private Set<Obs> buildResultObsGroup(Bundle bundle, Encounter newEmrEncounter, DiagnosticReport diagnosticReport, Order order, Concept concept) {
        Set<Obs> resultObsGroups = new HashSet<>();
        List<IResource> resultObservationList = findResourcesByReference(bundle, diagnosticReport.getResult());

        for (IResource resultObservation : resultObservationList) {
            Obs resultObsGroup = buildObs(concept, order);
            populateResultsAndNotes(bundle, newEmrEncounter, order, (Observation) resultObservation, resultObsGroup);
            resultObsGroups.add(resultObsGroup);
        }
        return resultObsGroups;
    }

    private void populateResultsAndNotes(Bundle bundle, Encounter newEmrEncounter, Order order, Observation resultObservation, Obs resultObsGroup) {
        Observation observationResource = resultObservation;
        Obs resultObs = observationsMapper.mapObs(bundle, newEmrEncounter, observationResource);
        resultObs.setOrder(order);
        resultObsGroup.addGroupMember(resultObs);
        resultObsGroup.addGroupMember(getNotes(observationResource, order));
    }

    private Obs findObsByOrder(Encounter encounter, Order order) {
        for (Obs obs : encounter.getObsAtTopLevel(false)) {
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
