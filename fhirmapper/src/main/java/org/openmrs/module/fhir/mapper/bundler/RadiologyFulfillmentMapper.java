package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticReport;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import ca.uhn.fhir.model.dstu2.valueset.ObservationStatusEnum;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.StringDt;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.module.fhir.FHIRProperties;
import org.openmrs.module.fhir.MRSProperties;
import org.openmrs.module.fhir.mapper.model.CompoundObservation;
import org.openmrs.module.fhir.mapper.model.FHIREncounter;
import org.openmrs.module.fhir.mapper.model.FHIRResource;
import org.openmrs.module.fhir.mapper.model.ObservationType;
import org.openmrs.module.fhir.utils.CodeableConceptService;
import org.openmrs.module.shrclient.dao.IdMappingRepository;
import org.openmrs.module.shrclient.model.IdMapping;
import org.openmrs.module.shrclient.model.IdMappingType;
import org.openmrs.module.shrclient.util.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.openmrs.module.fhir.FHIRProperties.*;

@Component("radiologyFulfillmentMapper")
public class RadiologyFulfillmentMapper implements EmrObsResourceHandler {

    @Autowired
    private IdMappingRepository idMappingRepository;

    @Autowired
    private DiagnosticReportBuilder diagnosticReportBuilder;

    @Autowired
    private CodeableConceptService codeableConceptService;

    @Autowired
    private ObservationValueMapper observationValueMapper;

    @Autowired
    private ObservationBuilder observationBuilder;

    @Override
    public boolean canHandle(Obs observation) {
        CompoundObservation procedureFulfillmentObs = new CompoundObservation(observation);
        return procedureFulfillmentObs.isOfType(ObservationType.RADIOLOGY_FULFILLMENT);
    }

    @Override
    public List<FHIRResource> map(Obs topLevelObs, FHIREncounter fhirEncounter, SystemProperties systemProperties) {
        List<FHIRResource> fhirResources = new ArrayList<>();
        buildResult(topLevelObs, fhirEncounter, fhirResources, systemProperties);
        return fhirResources;
    }

    private void buildResult(Obs topLevelTestObs, FHIREncounter fhirEncounter, List<FHIRResource> fhirResourceList, SystemProperties systemProperties) {
        DiagnosticReport diagnosticReport = buildDiagnosticReport(topLevelTestObs, fhirEncounter, systemProperties);
        if (diagnosticReport != null) {
            for (Obs resultObsGroup : topLevelTestObs.getGroupMembers()) {
                FHIRResource resultResource = getResultResource(resultObsGroup, fhirEncounter, systemProperties);
                if (resultResource == null) return;
                ResourceReferenceDt resourceReference = diagnosticReport.addResult();
                resourceReference.setReference(resultResource.getIdentifier().getValue());
                fhirResourceList.add(resultResource);

            }
            FHIRResource fhirResource = new FHIRResource("Diagnostic Report", diagnosticReport.getIdentifier(), diagnosticReport);
            fhirResourceList.add(fhirResource);
        }
    }

    private FHIRResource getResultResource(Obs resultObsGroup, FHIREncounter fhirEncounter, SystemProperties systemProperties) {

        CompoundObservation resultGroupObservation = new CompoundObservation(resultObsGroup);
        FHIRResource observationResource = getResultObservation(resultObsGroup, fhirEncounter, systemProperties, resultGroupObservation);

        Observation fhirObservation = (Observation) observationResource.getResource();
        setObservationStatusForResult(fhirObservation);

        return observationResource;
    }

    private void setObservationStatusForResult(Observation resultObservation) {
        resultObservation.setStatus(ObservationStatusEnum.FINAL);
    }

    private FHIRResource getResultObservation(Obs resultObsGroup, FHIREncounter fhirEncounter, SystemProperties systemProperties, CompoundObservation resultGroupObservation) {
        Obs resultObs = resultObsGroup;
// resultGroupObservation.getMemberObsForConcept(resultObsGroup.getConcept());
        FHIRResource fhirObservationResource = observationBuilder.buildObservationResource(fhirEncounter,
                UUID.randomUUID().toString(), resultObsGroup.getConcept().getName().getName(), systemProperties);
        Observation fhirObservation = (Observation) fhirObservationResource.getResource();
        fhirObservation.setCode(codeableConceptService.addTRCodingOrDisplay(resultObsGroup.getConcept()));
        if (resultObs != null) {
            mapResultValue(resultObs, fhirObservation);
        }
        return fhirObservationResource;
    }

    private void mapResultValue(Obs resultObs, Observation fhirObservation) {
        IDatatype value = observationValueMapper.map(resultObs);
        if (null != value) {
            fhirObservation.setValue(value);
        }
    }

    private DiagnosticReport buildDiagnosticReport(Obs obs, FHIREncounter fhirEncounter, SystemProperties systemProperties) {
        DiagnosticReport report = diagnosticReportBuilder.build(obs, fhirEncounter, systemProperties);
        report.setCode(getCodeForReport(obs));
        org.openmrs.Order obsOrder = obs.getOrder();
        report.setEffective(getOrderTime(obsOrder));

        String uri = getRequestUrl(obsOrder);
        report.addRequest().setReference(uri);
        CodeableConceptDt category = new CodeableConceptDt();
        category.addCoding()
                .setSystem(FHIRProperties.FHIR_V2_VALUESET_DIAGNOSTIC_REPORT_CATEGORY_URL)
                .setCode(FHIRProperties.FHIR_DIAGNOSTIC_REPORT_CATEGORY_RADIOLOGY_CODE)
                .setDisplay(FHIRProperties.FHIR_DIAGNOSTIC_REPORT_CATEGORY_RADIOLOGY_DISPLAY);
        report.setCategory(category);
        return report;
    }

    private CodeableConceptDt getCodeForReport(Obs obs) {
        Set<Obs> groupMembers = obs.getGroupMembers();
        CodeableConceptDt name = null;
        for (Obs groupMember : groupMembers) {
            if (MRSProperties.MRS_CONCEPT_TYPE_OF_RADIOLOGY_ORDER.equals(groupMember.getConcept().getName().getName()))
                name = codeableConceptService.addTRCodingOrDisplay(groupMember.getValueCoded());
        }
        return name;
    }

    private String getRequestUrl(Order order) {
        IdMapping orderIdMapping = idMappingRepository.findByInternalId(order.getUuid(), IdMappingType.DIAGNOSTIC_ORDER);
        if (orderIdMapping != null) {
            return orderIdMapping.getUri();
        }
        IdMapping encounterIdMapping = idMappingRepository.findByInternalId(order.getEncounter().getUuid(), IdMappingType.ENCOUNTER);
        if (encounterIdMapping != null)
            return encounterIdMapping.getUri();
        throw new RuntimeException("Encounter id [" + order.getEncounter().getUuid() + "] is not synced to SHR yet.");
    }

    private DateTimeDt getOrderTime(org.openmrs.Order obsOrder) {
        DateTimeDt diagnostic = new DateTimeDt();
        diagnostic.setValue(obsOrder.getDateActivated(), TemporalPrecisionEnum.MILLI);
        return diagnostic;
    }
}
