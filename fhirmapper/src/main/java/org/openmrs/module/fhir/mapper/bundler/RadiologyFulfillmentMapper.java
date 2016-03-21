package org.openmrs.module.fhir.mapper.bundler;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.DiagnosticReport;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.module.fhir.FHIRProperties;
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

@Component("radiologyFulfillmentMapper")
public class RadiologyFulfillmentMapper implements EmrObsResourceHandler {

    @Autowired
    private IdMappingRepository idMappingRepository;

    @Autowired
    private DiagnosticReportBuilder diagnosticReportBuilder;

    @Autowired
    private CodeableConceptService codeableConceptService;

    @Autowired
    private ObservationMapper observationMapper;

    @Autowired
    private ObservationBuilder observationBuilder;

    @Override
    public boolean canHandle(Obs observation) {
        CompoundObservation obs = new CompoundObservation(observation);
        return obs.isOfType(ObservationType.RADIOLOGY_FULFILLMENT);
    }

    @Override
    public List<FHIRResource> map(Obs orderFullfillmentObs, FHIREncounter fhirEncounter, SystemProperties systemProperties) {
        List<FHIRResource> fhirResources = new ArrayList<>();
        buildResult(orderFullfillmentObs, fhirEncounter, fhirResources, systemProperties);
        return fhirResources;
    }

    private void buildResult(Obs orderFullfillmentObs, FHIREncounter fhirEncounter, List<FHIRResource> fhirResourceList, SystemProperties systemProperties) {
        DiagnosticReport diagnosticReport = buildDiagnosticReport(orderFullfillmentObs, fhirEncounter, systemProperties);
        if (diagnosticReport != null) {
            for (Obs resultObs : orderFullfillmentObs.getGroupMembers()) {
                FHIRResource resultResource = getResultResource(resultObs, fhirEncounter, fhirResourceList, systemProperties);
                if (resultResource == null) continue;
                ResourceReferenceDt resourceReference = diagnosticReport.addResult();
                resourceReference.setReference(resultResource.getIdentifier().getValue());
            }
            FHIRResource fhirResource = new FHIRResource("Diagnostic Report", diagnosticReport.getIdentifier(), diagnosticReport);
            fhirResourceList.add(fhirResource);
        }
    }

    private FHIRResource getResultResource(Obs resultObs, FHIREncounter fhirEncounter, List<FHIRResource> fhirResourceList, SystemProperties systemProperties) {
        return observationMapper.mapObs(resultObs, fhirEncounter, null, fhirResourceList, systemProperties);
    }

    private DiagnosticReport buildDiagnosticReport(Obs obs, FHIREncounter fhirEncounter, SystemProperties systemProperties) {
        DiagnosticReport report = diagnosticReportBuilder.build(obs, fhirEncounter, systemProperties);
        report.setCode(codeableConceptService.addTRCodingOrDisplay(obs.getOrder().getConcept()));
        org.openmrs.Order obsOrder = obs.getOrder();
        report.setEffective(getOrderTime(obsOrder));

        report.addRequest().setReference(getRequestUrl(obsOrder));
        CodeableConceptDt category = new CodeableConceptDt();
        category.addCoding()
                .setSystem(FHIRProperties.FHIR_V2_VALUESET_DIAGNOSTIC_REPORT_CATEGORY_URL)
                .setCode(FHIRProperties.FHIR_DIAGNOSTIC_REPORT_CATEGORY_RADIOLOGY_CODE)
                .setDisplay(FHIRProperties.FHIR_DIAGNOSTIC_REPORT_CATEGORY_RADIOLOGY_DISPLAY);
        report.setCategory(category);
        return report;
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
