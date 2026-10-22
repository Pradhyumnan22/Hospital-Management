package com.hospital.bedmgmt.recommendation;

import com.hospital.bedmgmt.domain.BedState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.hospital.bedmgmt.recommendation.RecommendationModels.BedCandidate;
import static com.hospital.bedmgmt.recommendation.RecommendationModels.Exclusion;
import static com.hospital.bedmgmt.recommendation.RecommendationModels.RankedBed;
import static com.hospital.bedmgmt.recommendation.RecommendationModels.RecommendationCriteria;
import static com.hospital.bedmgmt.recommendation.RecommendationModels.RecommendationResult;

/**
 * Eligible beds are ordered by {@link TrainedBedRanker}. Hard rules still remove a bed
 * before the model can score it.
 */
public final class BedRecommendationEngine {

    private final TrainedBedRanker ranker = new TrainedBedRanker();

    public RecommendationResult recommend(RecommendationCriteria criteria, List<BedCandidate> beds) {
        List<Exclusion> exclusions = new ArrayList<>();
        List<ScoredBed> scored = new ArrayList<>();
        for (BedCandidate bed : beds) {
            String failure = hardFailure(criteria, bed);
            if (failure != null) {
                exclusions.add(new Exclusion(bed.id(), bed.bedCode(), bed.wardCode(), failure));
            } else {
                scored.add(score(criteria, bed));
            }
        }
        scored.sort(Comparator.comparingInt(ScoredBed::score).reversed()
                .thenComparingDouble(bed -> bed.candidate().wardOccupancyPercent())
                .thenComparing(bed -> bed.candidate().bedCode()));
        List<RankedBed> ranked = scored.stream().map(ScoredBed::ranked).toList();
        String summary = ranked.isEmpty()
                ? "No suitable bed is available. " + exclusions.size() + " beds were excluded by mandatory rules."
                : ranked.size() + " beds match the mandatory requirements. Ranking is advisory and does not allocate a bed.";
        return new RecommendationResult(List.copyOf(ranked), List.copyOf(exclusions), summary);
    }

    private String hardFailure(RecommendationCriteria criteria, BedCandidate bed) {
        if (bed.state() != BedState.AVAILABLE) {
            return "Bed is not available";
        }
        if (criteria.requiredWardType() != null && criteria.requiredWardType() != bed.wardType()) {
            return "Ward type does not match the required ward";
        }
        if (criteria.isolationRequired() && !bed.isolationCapable()) {
            return "Isolation is required and this bed is not isolation-capable";
        }
        List<String> missing = new ArrayList<>();
        if (criteria.ventilatorRequired() && !bed.ventilator()) {
            missing.add("ventilator");
        }
        if (criteria.monitorRequired() && !bed.monitor()) {
            missing.add("cardiac monitor");
        }
        if (criteria.oxygenRequired() && !bed.oxygen()) {
            missing.add("oxygen");
        }
        if (!missing.isEmpty()) {
            return "Missing required equipment: " + String.join(", ", missing);
        }
        return null;
    }

    private ScoredBed score(RecommendationCriteria criteria, BedCandidate bed) {
        boolean preferred = criteria.preferredWardType() != null && criteria.preferredWardType() == bed.wardType();
        boolean unusedIsolation = bed.isolationCapable() && !criteria.isolationRequired();
        double raw = ranker.score(preferred, bed.wardOccupancyPercent(), unusedIsolation);
        int score = (int) Math.round(raw * 10.0);
        List<String> notes = new ArrayList<>();
        notes.add("Ranked by the trained bed model");
        notes.add(preferred ? "Preferred ward matched" : "No preferred-ward match");
        notes.add("Ward occupancy " + bed.wardOccupancyPercent() + "%");
        if (unusedIsolation) {
            notes.add("Unused isolation bed");
        }
        return new ScoredBed(bed, score, List.copyOf(notes));
    }

    private record ScoredBed(BedCandidate candidate, int score, List<String> explanations) {
        RankedBed ranked() {
            return new RankedBed(
                    candidate.id(),
                    candidate.bedCode(),
                    candidate.wardCode(),
                    candidate.wardName(),
                    candidate.wardType(),
                    score,
                    explanations
            );
        }
    }
}
