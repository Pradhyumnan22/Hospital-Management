package com.hospital.bedmgmt.forecast;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Straight-line forecast from the mean net daily flow of the supplied window.
 * Predictions are clamped to operational capacity. This is not a trained model.
 */
public final class OccupancyForecaster {

    public static final String METHOD = "14-day rolling average of net admissions minus discharges";
    public static final String LIMITATIONS =
            "Assumes the recent average daily net change continues. Ignores hour of day, day of week, "
                    + "scheduled procedures, and length of stay. Figures are predictions, not measurements, "
                    + "and are clamped to current operational capacity.";

    private OccupancyForecaster() {
    }

    public record DailyFlow(LocalDate date, int admissions, int discharges) {
    }

    public record HourPoint(int hourOffset, int predictedOccupied, double predictedOccupancyPercent, boolean predicted) {
    }

    public record DayPoint(LocalDate date, int predictedOccupied, double predictedOccupancyPercent, boolean predicted) {
    }

    public record Forecast(
            String method,
            double averageDailyNetChange,
            boolean lowConfidence,
            String limitations,
            List<HourPoint> next24Hours,
            List<DayPoint> next7Days,
            List<DailyFlow> history
    ) {
    }

    public static Forecast forecast(int operationalBeds, int occupiedBeds, List<DailyFlow> history, LocalDate today) {
        List<DailyFlow> window = history == null ? List.of() : List.copyOf(history);
        boolean anyActivity = window.stream().anyMatch(day -> day.admissions() > 0 || day.discharges() > 0);
        double average = 0.0;
        if (!window.isEmpty()) {
            double total = 0.0;
            for (DailyFlow day : window) {
                total += day.admissions() - day.discharges();
            }
            average = total / window.size();
        }
        int capacity = Math.max(operationalBeds, 0);
        List<HourPoint> hours = new ArrayList<>();
        for (int hour = 1; hour <= 24; hour++) {
            int predicted = clamp(Math.round(occupiedBeds + average * hour / 24.0), capacity);
            hours.add(new HourPoint(hour, predicted, OccupancyCalculator.percent(predicted, capacity), true));
        }
        List<DayPoint> days = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            int predicted = clamp(Math.round(occupiedBeds + average * day), capacity);
            days.add(new DayPoint(today.plusDays(day), predicted, OccupancyCalculator.percent(predicted, capacity), true));
        }
        return new Forecast(METHOD, round2(average), !anyActivity, LIMITATIONS, List.copyOf(hours), List.copyOf(days), window);
    }

    private static int clamp(long value, int capacity) {
        if (value < 0) {
            return 0;
        }
        if (value > capacity) {
            return capacity;
        }
        return (int) value;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
