package com.turbolent.aptagger;

import java.util.*;

public class AveragedPerceptron {
    public final Map<String, Map<String, Float>> weights;
    public final Set<String> labels;

    public AveragedPerceptron(Map<String, Map<String, Float>> weights, Set<String> labels) {
        this.weights = weights;
        this.labels = labels;
    }

    public String predict(Map<String, Integer> features) {
        Map<String, Float> scores = new HashMap<>();

        for (Map.Entry<String, Integer> featureEntry : features.entrySet()) {
            String feature = featureEntry.getKey();
            int value = featureEntry.getValue();
            Map<String, Float> weights = this.weights.get(feature);
            if (weights == null || value == 0)
                continue;

            for (Map.Entry<String, Float> weightEntry : weights.entrySet()) {
                String label = weightEntry.getKey();
                float weight = weightEntry.getValue();
                scores.merge(label, 0.f, (current, initial) ->
                    current + value * weight);
            }
        }

        return Collections.max(this.labels, (String label1, String label2) ->
            Float.compare(scores.getOrDefault(label1, Float.MIN_VALUE),
                          scores.getOrDefault(label2, Float.MIN_VALUE)));
    }
}
