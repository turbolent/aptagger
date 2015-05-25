package com.turbolent.aptagger;

import java.util.*;

public class TrainableTagger extends Tagger {
    public interface TrainingListener {
        void onIterationStart(int iterationIndex, int iterationCount);

        void onTrainedSentence(int sentenceIndex, int sentenceCount, int correct, int total);

        void onIterationEnd(int iterationIndex, int iterationCount);

        void onAveraging();
    }

    protected final TrainableAveragedPerceptron trainablePerceptron;

    private TrainableTagger(Map<String, Map<String, Float>> weights,
                            Map<String, String> tags, Set<String> labels)
    {
        super(tags, new TrainableAveragedPerceptron(weights, labels));
        this.trainablePerceptron = (TrainableAveragedPerceptron)this.perceptron;
    }

    private static Map<String, String> getTags(Map<String, Map<String, Integer>> counts,
                                               int frequencyThreshold,
                                               float ambiguityThreshold)
    {
        Map<String, String> tags = new HashMap<>();

        for (Map.Entry<String, Map<String, Integer>> countEntry : counts.entrySet()) {
            String word = countEntry.getKey();
            Map<String, Integer> tagCounts = countEntry.getValue();

            Map.Entry<String, Integer> maxEntry =
                Collections.max(tagCounts.entrySet(),
                                (entry1, entry2) ->
                                    Integer.compare(entry1.getValue(),
                                                    entry2.getValue()));

            String tag = maxEntry.getKey();
            int mode = maxEntry.getValue();
            int n = tagCounts.values().stream().mapToInt(Integer::intValue).sum();

            if (n > frequencyThreshold
                && (((float)mode) / n) >= ambiguityThreshold)
            {
                tags.put(word, tag);
            }
        }
        return tags;
    }

    private static TrainableTagger getInitialTagger(List<TaggedSentence> sentences,
                                                    int frequencyThreshold,
                                                    float ambiguityThreshold)
    {
        HashSet<String> labels = new HashSet<>();
        Map<String, Map<String, Integer>> counts = new HashMap<>();

        for (TaggedSentence sentence : sentences) {
            for (int index = 0; index < sentence.words.size(); index++) {
                String word = sentence.words.get(index);
                String tag = sentence.tags.get(index);
                Map<String, Integer> wordCounts =
                    counts.computeIfAbsent(word, key -> new HashMap<>());
                wordCounts.merge(tag, 1, (current, initial) -> current + 1);
                labels.add(tag);
            }
        }

        HashMap<String, Map<String, Float>> weights = new HashMap<>();
        Map<String, String> tags = getTags(counts, frequencyThreshold, ambiguityThreshold);

        return new TrainableTagger(weights, tags, labels);
    }

    public static Tagger getTrained(List<TaggedSentence> sentences, int iterations,
                                    int frequencyThreshold, float ambiguityThreshold,
                                    TrainingListener listener)
    {
        TrainableTagger tagger =
            getInitialTagger(sentences, frequencyThreshold, ambiguityThreshold);
        Map<String, String> tags = tagger.tags;

        int offset = START.length;
        for (int iterationIndex = 0; iterationIndex < iterations; iterationIndex++) {
            if (listener != null)
                listener.onIterationStart(iterationIndex, iterations);

            int correct = 0;
            int total = 0;
            int sentenceCount = sentences.size();
            for (int sentenceIndex = 0; sentenceIndex < sentenceCount; sentenceIndex++) {
                TaggedSentence sentence = sentences.get(sentenceIndex);
                List<String> context = getContext(sentence.words);

                String prev = START[0];
                String prev2 = START[1];
                for (int index = 0; index < sentence.words.size(); index++) {
                    String word = sentence.words.get(index);
                    String tag = sentence.tags.get(index);

                    String guess = tags.get(word);
                    if (guess == null) {
                        Features features =
                            Features.getFeatures(offset + index, word, context, prev, prev2);
                        guess = tagger.trainablePerceptron.predict(features);
                        tagger.trainablePerceptron.update(tag, guess, features);
                    }
                    prev2 = prev;
                    prev = guess;

                    if (guess.equals(tag))
                        correct += 1;
                    total += 1;
                }

                if (listener != null) {
                    listener.onTrainedSentence(sentenceIndex, sentenceCount,
                                               correct, total);
                }
            }

            Collections.shuffle(sentences);

            if (listener != null)
                listener.onIterationEnd(iterationIndex, iterations);
        }

        if (listener != null)
            listener.onAveraging();
        tagger.trainablePerceptron.averageWeights();

        return tagger;
    }
}
