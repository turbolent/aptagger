package com.turbolent.aptagger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CommandLineInterface {

    public static final Pattern SENTENCE_SEPARATOR = Pattern.compile(" ");
    public static final Pattern TOKEN_SEPARATOR = Pattern.compile("_");
    public static final int TRAINING_STATUS_GAP = 500;
    public static final int TRAINING_ITERATIONS = 5;
    public static final int FREQUENCY_THRESHOLD = 20;
    public static final float AMBIGUITY_THRESHOLD = 0.97f;

    private static void printUsage() {
        String usage =
            "Usage:\n"
            + "  tag <model-file> <word>...\n"
            + "  train <corpus-file> <model-file>\n"
            + "  test <model-file> <corpus-file>\n"
            + "\n"
            + "Commands:\n"
            + "  tag    Returns a tagged sentence of the given words using the given model.\n"
            + "  train  Creates a model from the given corpus, which should contain\n"
            + "         one tagged sentence per line.\n"
            +"   test   Tests how the given model for the given corpus. Prints the ratio\n"
            + "         of correct and total number of tags.\n"
            + "\n"
            + "Format:\n"
            + "  A tagged sentence consists of tokens separated by spaces, where each token\n"
            + "  is a combination of a word and a tag, separated by an underscore.\n"
            + "  For example: \"Simple_NN is_VBZ better_JJR than_IN complex_JJ ._.\"\n";
        System.err.println(usage);
    }

    public static TaggedSentence asSentence(String sentence) {
        List<String> words = new ArrayList<>();
        List<String> tags = new ArrayList<>();
        SENTENCE_SEPARATOR.splitAsStream(sentence)
            .forEach(token -> {
                String[] parts = TOKEN_SEPARATOR.split(token);
                if (parts.length == 2) {
                    words.add(parts[0]);
                    tags.add(parts[1]);
                } else {
                    String message = String.format("Invalid token \"%s\" in sentence \"%s\"",
                                                   token, sentence);
                    throw new RuntimeException(message);
                }
            });

        return new TaggedSentence(words, tags);
    }

    public static void printTaggedWords(Path modelPath, List<String> words) {
        try {
            Tagger tagger = Tagger.loadFrom(modelPath);
            List<String> tags = tagger.tag(words);
            for (int i = 0; i < words.size(); i++) {
                String word = words.get(i);
                String tag = tags.get(i);
                System.out.format("%s_%s ", word, tag);
            }
            System.out.println();
        } catch (IOException e) {
            System.err.println("Failed: " + e);
            System.exit(1);
        }
    }

    public static List<TaggedSentence> readSentences(Path corpusPath) throws IOException {
        return Files.lines(corpusPath)
                    .map(CommandLineInterface::asSentence)
                    .collect(Collectors.toList());
    }

    public static void train(Path corpusPath, Path modelPath, int iterations,
                             int frequencyThreshold, float ambiguityThreshold)
    {
        try {
            System.err.println("Reading sentences ...");
            List<TaggedSentence> sentences = readSentences(corpusPath);

            System.err.println("Training ...");
            TrainableTagger.TrainingListener listener = new TrainableTagger.TrainingListener() {
                @Override
                public void onIterationStart(int iterationIndex, int iterationCount) {
                    System.err.format("Iteration %d/%d:\n", iterationIndex + 1, iterationCount);
                }

                @Override
                public void onTrainedSentence(int sentenceIndex, int sentenceCount,
                                              int correct, int total)
                {
                    int sentenceNumber = sentenceIndex + 1;
                    if (sentenceNumber != sentenceCount
                        && sentenceNumber % TRAINING_STATUS_GAP != 0)
                    {
                        return;
                    }

                    System.err.format("... %d/%d: %s\n",
                                      sentenceNumber, sentenceCount,
                                      formatCorrectAndTotal(correct, total));


                }

                @Override
                public void onIterationEnd(int iterationIndex, int iterationCount) {
                }

                @Override
                public void onAveraging() {
                    System.err.println("Averaging ...");
                }
            };

            Tagger tagger =
                TrainableTagger.getTrained(sentences, iterations, frequencyThreshold,
                                           ambiguityThreshold, listener);

            System.err.println("Saving ...");
            tagger.saveTo(modelPath);
        } catch (IOException e) {
            System.err.println("Failed: " + e);
            System.exit(1);
        }
    }

    private static String formatCorrectAndTotal(int correct, int total) {
        return String.format("%d/%d = %.3f%%",
                             correct, total,
                             (((float) correct) / total) * 100);
    }

    public static void test(Path modelPath, Path corpusPath) {
        try {
            System.err.println("Reading sentences ...");
            List<TaggedSentence> sentences = readSentences(corpusPath);

            System.err.println("Loading tagger ...");
            Tagger tagger = Tagger.loadFrom(modelPath);

            int total = 0;
            int correct = 0;

            for (TaggedSentence sentence : sentences) {
                List<String> tags = tagger.tag(sentence.words);
                for (int i = 0; i < sentence.words.size(); i++) {
                    String tag = sentence.tags.get(i);
                    String guess = tags.get(i);
                    if (guess.equals(tag))
                        correct += 1;
                    total += 1;
                }
            }

            System.err.println(formatCorrectAndTotal(correct, total));
        } catch (IOException e) {
            System.err.println("Failed: " + e);
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            return;
        }

        String command = args[0];
        switch (command) {
            case "tag": {
                Path modelPath = Paths.get(args[1]);
                List<String> words = Arrays.asList(args).subList(2, args.length);
                printTaggedWords(modelPath, words);
                break;
            }
            case "train": {
                if (args.length < 3) {
                    printUsage();
                    break;
                }

                Path corpusPath = Paths.get(args[1]);
                Path modelPath = Paths.get(args[2]);
                train(corpusPath, modelPath, TRAINING_ITERATIONS,
                      FREQUENCY_THRESHOLD, AMBIGUITY_THRESHOLD);
                break;
            }
            case "test": {
                if (args.length < 3) {
                    printUsage();
                    break;
                }

                Path modelPath = Paths.get(args[1]);
                Path corpusPath = Paths.get(args[2]);
                test(modelPath, corpusPath);
                break;
            }
            default: {
                printUsage();
                break;
            }
        }
    }
}
