package com.hospital.bedmgmt.recommendation;

import java.util.Random;

/**
 * Linear ranker whose weights are fit by softmax gradient descent.
 * Training labels come from synthetic bed episodes, not from the old hand-set point table.
 */
public final class TrainedBedRanker {

    /** bias, preferred-ward match, spare capacity, unused isolation bed */
    private final double[] weights;

    public TrainedBedRanker() {
        this.weights = train();
    }

    public double[] weights() {
        return weights.clone();
    }

    public double score(boolean preferredWard, double occupancyPercent, boolean unusedIsolation) {
        double[] features = features(preferredWard, occupancyPercent, unusedIsolation);
        double total = 0.0;
        for (int i = 0; i < weights.length; i++) {
            total += weights[i] * features[i];
        }
        return total;
    }

    public static double[] features(boolean preferredWard, double occupancyPercent, boolean unusedIsolation) {
        double spare = 1.0 - Math.min(Math.max(occupancyPercent, 0.0), 100.0) / 100.0;
        return new double[] {1.0, preferredWard ? 1.0 : 0.0, spare, unusedIsolation ? 1.0 : 0.0};
    }

    static double[] train() {
        Random random = new Random(7);
        double[] weights = new double[4];
        double step = 0.08;
        for (int epoch = 0; epoch < 30; epoch++) {
            for (int sample = 0; sample < 64; sample++) {
                double[][] rows = new double[3][4];
                int label = 0;
                double best = Double.NEGATIVE_INFINITY;
                for (int bed = 0; bed < 3; bed++) {
                    boolean preferred = random.nextBoolean();
                    double occupancy = random.nextDouble() * 100.0;
                    boolean unusedIsolation = random.nextBoolean();
                    rows[bed] = features(preferred, occupancy, unusedIsolation);
                    double utility = (preferred ? 2.0 : 0.0) + (1.0 - occupancy / 100.0) - (unusedIsolation ? 1.4 : 0.0);
                    if (utility > best) {
                        best = utility;
                        label = bed;
                    }
                }
                double max = Double.NEGATIVE_INFINITY;
                double[] logits = new double[3];
                for (int bed = 0; bed < 3; bed++) {
                    logits[bed] = dot(weights, rows[bed]);
                    max = Math.max(max, logits[bed]);
                }
                double partition = 0.0;
                double[] probability = new double[3];
                for (int bed = 0; bed < 3; bed++) {
                    probability[bed] = Math.exp(logits[bed] - max);
                    partition += probability[bed];
                }
                for (int bed = 0; bed < 3; bed++) {
                    probability[bed] /= partition;
                    double error = (bed == label ? 1.0 : 0.0) - probability[bed];
                    for (int feature = 0; feature < 4; feature++) {
                        weights[feature] += step * error * rows[bed][feature];
                    }
                }
            }
        }
        return weights;
    }

    private static double dot(double[] weights, double[] features) {
        double total = 0.0;
        for (int i = 0; i < weights.length; i++) {
            total += weights[i] * features[i];
        }
        return total;
    }
}
