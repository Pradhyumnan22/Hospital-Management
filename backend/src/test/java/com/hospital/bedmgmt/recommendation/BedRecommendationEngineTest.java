package com.hospital.bedmgmt.recommendation;

import com.hospital.bedmgmt.domain.BedState;
import com.hospital.bedmgmt.domain.WardType;
import com.hospital.bedmgmt.recommendation.RecommendationModels.BedCandidate;
import com.hospital.bedmgmt.recommendation.RecommendationModels.RecommendationCriteria;
import com.hospital.bedmgmt.recommendation.RecommendationModels.RecommendationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedRecommendationEngineTest {

    private final BedRecommendationEngine engine = new BedRecommendationEngine();

    @Test
    void excludesMissingEquipmentAndIsolationFailures() {
        RecommendationCriteria criteria = new RecommendationCriteria(WardType.ICU, WardType.ICU, true, true, true, true);
        RecommendationResult result = engine.recommend(criteria, List.of(
                bed(1, "U-02", WardType.ICU, BedState.AVAILABLE, false, true, true, true, 40),
                bed(2, "U-03", WardType.ICU, BedState.AVAILABLE, true, false, true, true, 40),
                bed(3, "I-02", WardType.ISOLATION, BedState.AVAILABLE, true, true, true, true, 20)
        ));
        assertTrue(result.candidates().isEmpty());
        assertEquals("Isolation is required and this bed is not isolation-capable", result.exclusions().get(0).reason());
        assertEquals("Missing required equipment: ventilator", result.exclusions().get(1).reason());
        assertEquals("Ward type does not match the required ward", result.exclusions().get(2).reason());
        assertTrue(result.summary().startsWith("No suitable bed"));
    }

    @Test
    void ignoresOccupiedBedsEvenWhenEquipmentMatches() {
        RecommendationCriteria criteria = new RecommendationCriteria(null, WardType.ICU, false, true, false, false);
        RecommendationResult result = engine.recommend(criteria, List.of(
                bed(1, "U-01", WardType.ICU, BedState.OCCUPIED, false, true, true, true, 10)
        ));
        assertTrue(result.candidates().isEmpty());
        assertEquals("Bed is not available", result.exclusions().get(0).reason());
    }

    @Test
    void ranksEligibleBedsAndPreservesIsolationRooms() {
        RecommendationCriteria criteria = new RecommendationCriteria(null, WardType.GENERAL, false, false, false, true);
        RecommendationResult result = engine.recommend(criteria, List.of(
                bed(1, "I-02", WardType.ISOLATION, BedState.AVAILABLE, true, false, true, true, 10),
                bed(2, "G-03", WardType.GENERAL, BedState.AVAILABLE, false, false, false, true, 80),
                bed(3, "G-04", WardType.GENERAL, BedState.AVAILABLE, false, false, true, true, 40),
                bed(4, "P-03", WardType.PEDIATRIC, BedState.AVAILABLE, false, false, false, false, 10)
        ));
        assertEquals(3, result.candidates().size());
        assertEquals("G-04", result.candidates().get(0).bedCode());
        assertTrue(result.candidates().get(0).score() > result.candidates().get(1).score());
        assertTrue(result.candidates().get(0).explanations().stream().anyMatch(line -> line.contains("trained bed model")));
        assertTrue(result.exclusions().stream().anyMatch(exclusion -> exclusion.reason().contains("oxygen")));
        assertEquals("I-02", result.candidates().get(2).bedCode());
        assertTrue(result.candidates().get(2).explanations().stream().anyMatch(line -> line.contains("Unused isolation")));
    }

    @Test
    void hardRuleRemovesBedThatWouldOtherwiseScoreHighest() {
        RecommendationCriteria criteria = new RecommendationCriteria(WardType.ICU, WardType.GENERAL, false, false, false, false);
        BedCandidate attractiveWrongWard = bed(1, "G-01", WardType.GENERAL, BedState.AVAILABLE, false, true, true, true, 0);
        BedCandidate plainIcu = bed(2, "U-09", WardType.ICU, BedState.AVAILABLE, false, false, false, false, 95);
        RecommendationResult result = engine.recommend(criteria, List.of(attractiveWrongWard, plainIcu));
        assertEquals(1, result.candidates().size());
        assertEquals("U-09", result.candidates().get(0).bedCode());
    }

    private BedCandidate bed(long id, String code, WardType type, BedState state, boolean isolation, boolean ventilator, boolean monitor, boolean oxygen, double occupancy) {
        return new BedCandidate(id, code, type.name(), type.name(), type, state, isolation, ventilator, monitor, oxygen, occupancy);
    }
}
