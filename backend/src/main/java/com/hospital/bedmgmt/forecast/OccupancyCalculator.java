package com.hospital.bedmgmt.forecast;

import com.hospital.bedmgmt.domain.BedState;

import java.util.List;

public final class OccupancyCalculator {

    private OccupancyCalculator() {
    }

    public record BedSnapshot(String wardCode, String wardName, BedState state) {
    }

    public record OccupancySnapshot(
            int operationalBeds,
            int occupiedBeds,
            int availableBeds,
            int cleaningBeds,
            int outOfServiceBeds,
            double occupancyPercent
    ) {
    }

    public record WardOccupancy(
            String wardCode,
            String wardName,
            int operationalBeds,
            int occupiedBeds,
            int availableBeds,
            double occupancyPercent
    ) {
    }

    public static OccupancySnapshot summarize(List<BedSnapshot> beds) {
        int occupied = 0;
        int available = 0;
        int cleaning = 0;
        int out = 0;
        for (BedSnapshot bed : beds) {
            switch (bed.state()) {
                case OCCUPIED -> occupied++;
                case AVAILABLE -> available++;
                case CLEANING -> cleaning++;
                case OUT_OF_SERVICE -> out++;
            }
        }
        int operational = occupied + available + cleaning;
        return new OccupancySnapshot(operational, occupied, available, cleaning, out, percent(occupied, operational));
    }

    public static double percent(int occupied, int operational) {
        if (operational <= 0) {
            return 0.0;
        }
        return Math.round(occupied * 1000.0 / operational) / 10.0;
    }
}
