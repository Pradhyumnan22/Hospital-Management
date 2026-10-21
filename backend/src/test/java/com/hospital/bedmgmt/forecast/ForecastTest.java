package com.hospital.bedmgmt.forecast;

import com.hospital.bedmgmt.domain.BedState;
import com.hospital.bedmgmt.forecast.OccupancyCalculator.BedSnapshot;
import com.hospital.bedmgmt.forecast.OccupancyCalculator.OccupancySnapshot;
import com.hospital.bedmgmt.forecast.OccupancyForecaster.DailyFlow;
import com.hospital.bedmgmt.forecast.OccupancyForecaster.Forecast;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForecastTest {

    @Test
    void calculatesOccupancyAndTreatsEmptyCapacityAsZero() {
        OccupancySnapshot snapshot = OccupancyCalculator.summarize(List.of(
                new BedSnapshot("GEN", "General", BedState.OCCUPIED),
                new BedSnapshot("GEN", "General", BedState.OCCUPIED),
                new BedSnapshot("GEN", "General", BedState.AVAILABLE),
                new BedSnapshot("GEN", "General", BedState.CLEANING),
                new BedSnapshot("GEN", "General", BedState.OUT_OF_SERVICE)
        ));
        assertEquals(4, snapshot.operationalBeds());
        assertEquals(2, snapshot.occupiedBeds());
        assertEquals(1, snapshot.availableBeds());
        assertEquals(50.0, snapshot.occupancyPercent());
        assertEquals(0.0, OccupancyCalculator.percent(3, 0));
    }

    @Test
    void emptyHistoryStaysFlatAndLowConfidence() {
        Forecast forecast = OccupancyForecaster.forecast(10, 4, List.of(), LocalDate.of(2026, 10, 21));
        assertTrue(forecast.lowConfidence());
        assertEquals(0.0, forecast.averageDailyNetChange());
        assertEquals(4, forecast.next24Hours().get(23).predictedOccupied());
        assertEquals(4, forecast.next7Days().get(6).predictedOccupied());
        assertTrue(forecast.next7Days().get(0).predicted());
    }

    @Test
    void admissionSpikeRisesAndStopsAtCapacity() {
        List<DailyFlow> history = new ArrayList<>();
        LocalDate start = LocalDate.of(2026, 10, 8);
        for (int i = 0; i < 14; i++) {
            history.add(new DailyFlow(start.plusDays(i), 4, 0));
        }
        Forecast forecast = OccupancyForecaster.forecast(20, 10, history, LocalDate.of(2026, 10, 21));
        assertFalse(forecast.lowConfidence());
        assertEquals(4.0, forecast.averageDailyNetChange());
        assertEquals(14, forecast.next7Days().get(0).predictedOccupied());
        assertEquals(20, forecast.next7Days().get(6).predictedOccupied());
        assertEquals(11, forecast.next24Hours().get(5).predictedOccupied());
    }

    @Test
    void dischargesFallAndClampAtZero() {
        List<DailyFlow> history = new ArrayList<>();
        LocalDate start = LocalDate.of(2026, 10, 8);
        for (int i = 0; i < 14; i++) {
            history.add(new DailyFlow(start.plusDays(i), 0, 3));
        }
        Forecast forecast = OccupancyForecaster.forecast(20, 10, history, LocalDate.of(2026, 10, 21));
        assertEquals(-3.0, forecast.averageDailyNetChange());
        assertEquals(7, forecast.next7Days().get(0).predictedOccupied());
        assertEquals(1, forecast.next7Days().get(2).predictedOccupied());
        assertEquals(0, forecast.next7Days().get(3).predictedOccupied());
    }
}
