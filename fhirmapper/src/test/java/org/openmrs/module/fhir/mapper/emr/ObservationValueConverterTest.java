package org.openmrs.module.fhir.mapper.emr;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.model.dstu2.composite.*;
import ca.uhn.fhir.model.dstu2.valueset.EventTimingEnum;
import ca.uhn.fhir.model.dstu2.valueset.UnitsOfTimeEnum;
import ca.uhn.fhir.model.primitive.DecimalDt;
import ca.uhn.fhir.model.primitive.StringDt;
import org.junit.Test;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.*;
import static org.openmrs.module.fhir.mapper.emr.ObservationValueConverter.*;
import static org.openmrs.module.fhir.mapper.emr.ObservationValueConverter.convertToText;


public class ObservationValueConverterTest {
    @Test
    public void shouldConvertANullTypeToEmptyString() throws Exception {
        assertTrue(convertToText(null).isEmpty());
    }

    @Test
    public void shouldConvertADateType() throws Exception {
        Date date = new SimpleDateFormat("dd-MM-yyyy").parse("17-06-2015T00:00:00+05:30");
        String text = convertToText(date);
        assertEquals("17 Jun 2015", text);
    }

    @Test
    public void shouldConvertAQuantityDtType() throws Exception {
        QuantityDt quantity = new QuantityDt();
        assertEquals("", convertToText(quantity));

        DecimalDt decimalValue = new DecimalDt();
        decimalValue.setValue(new BigDecimal(12));
        quantity.setValue(decimalValue);

        assertEquals("12", convertToText(quantity));

        StringDt units = new StringDt();
        units.setValue("mg");
        quantity.setUnit(units);

        assertEquals("12 mg", convertToText(quantity));
    }

    @Test
    public void shouldConvertACodeableConceptDtWhenItHasCoding() throws Exception {
        CodeableConceptDt codeableConcept = new CodeableConceptDt();
        assertTrue(convertToText(codeableConcept).isEmpty());

        CodingDt coding = codeableConcept.addCoding();
        String fever = "Fever";
        coding.setDisplay(fever);

        assertEquals(fever, convertToText(codeableConcept));
    }

    @Test
    public void shouldGiveTextValueForACodeableConceptDtWithoutCoding() throws Exception {
        CodeableConceptDt codeableConcept = new CodeableConceptDt();
        assertTrue(convertToText(codeableConcept).isEmpty());

        String fever = "Fever";
        codeableConcept.setText(fever);

        assertEquals(fever, convertToText(codeableConcept));
    }

    @Test
    public void shouldConvertARatioDt() throws Exception {
        RatioDt ratio = new RatioDt();
        assertEquals("0", convertToText(ratio));

        ratio.setNumerator(new QuantityDt(12));
        ratio.setDenominator(new QuantityDt(24));

        assertEquals("12/24", convertToText(ratio));
    }

    @Test
    public void shouldConvertAPeriod() throws Exception {
        PeriodDt period1 = new PeriodDt();
        assertEquals(INVALID_PERIOD, convertToText(period1));

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
        Date startDate = simpleDateFormat.parse("17-06-2015T00:00:00+05:30");
        period1.setStart(startDate, TemporalPrecisionEnum.DAY);
        assertEquals("17 Jun 2015", convertToText(period1));

        PeriodDt period2 = new PeriodDt();
        Date endDate = simpleDateFormat.parse("17-06-2016T00:00:00+05:30");
        period2.setEnd(endDate, TemporalPrecisionEnum.DAY);
        assertEquals(PERIOD_START_UNKNOWN + " - 17 Jun 2016", convertToText(period2));

        period2.setStart(startDate, TemporalPrecisionEnum.DAY);
        assertEquals("17 Jun 2015 - 17 Jun 2016", convertToText(period2));
    }

    @Test
    public void shouldConvertASampledData() throws Exception {
        SampledDataDt sampledData = new SampledDataDt();
        String hello = "Hello";
        sampledData.setData(hello);

        assertEquals(hello, convertToText(sampledData));
    }

    @Test
    public void shouldConvertARange() throws Exception {
        RangeDt range1 = new RangeDt();
        SimpleQuantityDt upperLimit = new SimpleQuantityDt(12);
        range1.setHigh(upperLimit);
        assertEquals(" - 12", convertToText(range1));

        SimpleQuantityDt lowerLimit = new SimpleQuantityDt(6);
        range1.setLow(lowerLimit);

        assertEquals("6 - 12", convertToText(range1));

    }

    @Test
    public void shouldConvertATimingDtRepeatHavingFrequency() throws Exception {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
        Date startDate = simpleDateFormat.parse("17-06-2015T00:00:00+05:30");
        Date endDate = simpleDateFormat.parse("17-06-2016T00:00:00+05:30");

        PeriodDt period = new PeriodDt();
        period.setStart(startDate, TemporalPrecisionEnum.DAY);
        period.setEnd(endDate, TemporalPrecisionEnum.DAY);


        TimingDt.Repeat repeat = new TimingDt.Repeat();
        repeat.setBounds(period);
        repeat.setFrequency(2);
        repeat.setPeriod(1);
        repeat.setPeriodUnits(UnitsOfTimeEnum.D);

        TimingDt timing = new TimingDt();
        timing.setRepeat(repeat);

        assertEquals("2 time(s) in 1 Day. Duration:- 17 Jun 2015 - 17 Jun 2016", convertToText(timing));
    }

    @Test
    public void shouldConvertATimingDtRepeatHavingWhen() throws Exception {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
        Date startDate = simpleDateFormat.parse("17-06-2015T00:00:00+05:30");
        Date endDate = simpleDateFormat.parse("17-06-2016T00:00:00+05:30");

        PeriodDt period = new PeriodDt();
        period.setStart(startDate, TemporalPrecisionEnum.DAY);
        period.setEnd(endDate, TemporalPrecisionEnum.DAY);


        TimingDt.Repeat repeat = new TimingDt.Repeat();
        repeat.setBounds(period);
        repeat.setWhen(EventTimingEnum.ACM);
        repeat.setPeriod(1);
        repeat.setPeriodUnits(UnitsOfTimeEnum.H);

        TimingDt timing = new TimingDt();
        timing.setRepeat(repeat);

        assertEquals("1 Hour before breakfast. Duration:- 17 Jun 2015 - 17 Jun 2016", convertToText(timing));
    }
}