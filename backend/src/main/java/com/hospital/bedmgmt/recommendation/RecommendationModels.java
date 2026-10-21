package com.hospital.bedmgmt.recommendation;

import com.hospital.bedmgmt.domain.BedState;
import com.hospital.bedmgmt.domain.WardType;

import java.util.List;

public final class RecommendationModels {

    private RecommendationModels() {
    }

    public record BedCandidate(
            long id,
            String bedCode,
            String wardCode,
            String wardName,
            WardType wardType,
            BedState state,
            boolean isolationCapable,
            boolean ventilator,
            boolean monitor,
            boolean oxygen,
            double wardOccupancyPercent
    ) {
    }

    public record RecommendationCriteria(
            WardType requiredWardType,
            WardType preferredWardType,
            boolean isolationRequired,
            boolean ventilatorRequired,
            boolean monitorRequired,
            boolean oxygenRequired
    ) {
    }

    public record Exclusion(long bedId, String bedCode, String wardCode, String reason) {
    }

    public record RankedBed(
            long bedId,
            String bedCode,
            String wardCode,
            String wardName,
            WardType wardType,
            int score,
            List<String> explanations
    ) {
    }

    public record RecommendationResult(
            List<RankedBed> candidates,
            List<Exclusion> exclusions,
            String summary
    ) {
    }
}
