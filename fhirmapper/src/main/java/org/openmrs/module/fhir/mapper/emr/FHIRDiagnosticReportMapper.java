package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticReport;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.module.fhir.mapper.model.EntityReference;
import org.openmrs.module.fhir.utils.OMRSConceptLookup;
import org.openmrs.module.shrclient.dao.IdMappingsRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_CONCEPT_CLASS_LAB_SET;
import static org.openmrs.module.fhir.mapper.MRSProperties.MRS_CONCEPT_NAME_LAB_NOTES;
import static org.openmrs.module.fhir.utils.FHIRFeedHelper.findResourceByReference;

@Component
public class FHIRDiagnosticReportMapper implements FHIRResourceMapper {
    @Autowired
    private OMRSConceptLookup omrsConceptLookup;
    @Autowired
    private FHIRObservationsMapper observationsMapper;
    @Autowired
    private ConceptService conceptService;
    @Autowired
    private EncounterService encounterService;
    @Autowired
    private IdMappingsRepository idMappingsRepository;

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
        Obs topLevelObs = buildObs(concept, order);

        Obs secondLevelObs = buildObs(concept, order);
        topLevelObs.addGroupMember(secondLevelObs);

        Obs resultObs = buildResultObs(bundle, newEmrEncounter, diagnosticReport);
        resultObs.setOrder(order);
        secondLevelObs.addGroupMember(resultObs);

        Obs notesObs = addNotes(diagnosticReport, order);
        secondLevelObs.addGroupMember(notesObs);

        if (order.getConcept().getConceptClass().getName().equals(MRS_CONCEPT_CLASS_LAB_SET)) {
            Obs panelObs = findObsByOrder(newEmrEncounter, order);
            if (panelObs == null) {
                panelObs = buildObs(order.getConcept(), order);
            }
            panelObs.addGroupMember(topLevelObs);
            newEmrEncounter.addObs(panelObs);
        }
        newEmrEncounter.addObs(topLevelObs);
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

    private Obs addNotes(DiagnosticReport diagnosticReport, Order order) {
        String conclusion = diagnosticReport.getConclusion();
        if (StringUtils.isNotBlank(conclusion)) {
            Concept labNotesConcept = conceptService.getConceptByName(MRS_CONCEPT_NAME_LAB_NOTES);
            Obs notesObs = buildObs(labNotesConcept, order);
            notesObs.setValueText(conclusion);
            return notesObs;
        }
        return null;
    }

    private Obs buildResultObs(Bundle bundle, Encounter newEmrEncounter, DiagnosticReport diagnosticReport) {
        Observation observationResource = (Observation) findResourceByReference(bundle, diagnosticReport.getResult());
        return observationsMapper.mapObs(bundle, newEmrEncounter, observationResource);
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
