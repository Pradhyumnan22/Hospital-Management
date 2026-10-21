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
 * Explainable ranking. Hard rules remove a bed before any score is applied.
 * Starting score is 100. Preferred ward match adds 20. An isolation-capable bed
 * that the patient does not need subtracts 15. Ward occupancy at or below 70%
 * adds 15, and occupancy above 70% through 85% adds 8.
 */
public final class BedRecommendationEngine {

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
        int score = 100;
        List<String> notes = new ArrayList<>();
        notes.add("Base score 100 for passing mandatory rules");
        if (criteria.preferredWardType() == null) {
            notes.add("No ward preference supplied +0");
        } else if (criteria.preferredWardType() == bed.wardType()) {
            score += 20;
            notes.add("Preferred ward match +20");
        } else {
            notes.add("Preferred ward does not match +0");
        }
        if (bed.isolationCapable() && !criteria.isolationRequired()) {
            score -= 15;
            notes.add("Isolation bed kept in reserve -15");
        } else if (criteria.isolationRequired()) {
            notes.add("Isolation requirement matched +0");
        }
        int occupancyBonus;
        if (bed.wardOccupancyPercent() <= 70.0) {
            occupancyBonus = 15;
        } else if (bed.wardOccupancyPercent() <= 85.0) {
            occupancyBonus = 8;
        } else {
            occupancyBonus = 0;
        }
        score += occupancyBonus;
        notes.add("Ward occupancy " + bed.wardOccupancyPercent() + "% adds +" + occupancyBonus);
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
