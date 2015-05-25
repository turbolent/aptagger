package com.turbolent.aptagger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TrainableAveragedPerceptron extends AveragedPerceptron {
    private int i = 0;
    private final Map<String, Map<String, Float>> totals = new HashMap<>();
    private final Map<String, Map<String, Integer>> timestamps = new HashMap<>();

    public TrainableAveragedPerceptron(Map<String, Map<String, Float>> weights,
                                       Set<String> labels)
    {
        super(weights, labels);
    }

    private static <T> Map<String, T> getOrInitialize
        (Map<String, Map<String, T>> featureMap, String feature)
    {
        return featureMap.computeIfAbsent(feature, missingFeature -> new HashMap<>());
    }

    public void update(String truth, String guess, Features features) {
        this.i += 1;

        if (guess.equals(truth))
            return;

        for (String feature : features.keySet()) {
            Map<String, Float> weights = getOrInitialize(this.weights, feature);
            updateFeature(truth, feature, weights.getOrDefault(truth, 0.f), 1.f);
            updateFeature(guess, feature, weights.getOrDefault(guess, 0.f), -1.f);
        }
    }

    private void updateFeature(String label, String feature, float weight, float v) {
        int i = this.i;
        getOrInitialize(this.totals, feature)
            .merge(label, 0.f,
                   (current, initial) -> {
                       int timestamp = this.timestamps.get(feature).get(label);
                       return current + (i - timestamp) * weight;
                   });
        getOrInitialize(this.timestamps, feature).put(label, i);
        this.weights.get(feature).put(label, weight + v);
    }

    public void averageWeights() {
        int i = this.i;
        for (Map.Entry<String, Map<String, Float>> featureEntry : this.weights.entrySet()) {
            String feature = featureEntry.getKey();
            Map<String, Float> weights = featureEntry.getValue();

            Map<String, Float> newWeights = new HashMap<>();

            Map<String, Integer> timestamps = this.timestamps.get(feature);
            Map<String, Float> totals = this.totals.get(feature);

            for (Map.Entry<String, Float> weightEntry : weights.entrySet()) {
                String label = weightEntry.getKey();
                float weight = weightEntry.getValue();

                int timestamp = timestamps.get(label);
                float total = totals.get(label) + (i - timestamp) * weight;
                float averaged = Math.round((total / (float) i) * 1000) / 1000.f;
                if (averaged > 0.f)
                    newWeights.put(label, averaged);
            }

            this.weights.put(feature, newWeights);
        }
    }
}
