package com.turbolent.aptagger;

import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.Unpacker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.msgpack.template.Templates.*;

public class Tagger {
    protected static final String[] START = {"-START-", "-START2-"};
    protected static final String[] END = {"-END-", "-END2-"};
    protected static final Pattern NUMBER = Pattern.compile("[0-9][0-9,.]*");

    private static boolean isNumber(String string) {
        return NUMBER.matcher(string).matches();
    }

    private static String normalize(String word) {
        if (isNumber(word))
            return "!NUMBER";
        return word.toLowerCase();
    }

    protected final AveragedPerceptron perceptron;
    protected final Map<String, String> tags;

    protected Tagger(Map<String, String> tags, AveragedPerceptron perceptron) {
        this.tags = tags;
        this.perceptron = perceptron;
    }

    protected Tagger(Map<String, Map<String, Float>> weights,
                     Map<String, String> tags, Set<String> labels)
    {
        this(tags, new AveragedPerceptron(weights, labels));
    }

    public static Tagger loadFrom(Path inputPath) throws IOException {
        File inputFile = inputPath.toFile();

        try (FileInputStream fileStream = new FileInputStream(inputFile);
             Unpacker unpacker = new MessagePack().createUnpacker(fileStream))
        {
            Map<String, String> tags =
                unpacker.read(tMap(TString, TString));
            Map<String, Map<String, Float>> weights =
                unpacker.read(tMap(TString, tMap(TString, TFloat)));
            Set<String> labels = new HashSet<>();
            labels.addAll(unpacker.read(tList(TString)));

            return new Tagger(weights, tags, labels);
        }
    }

    public void saveTo(Path outputPath) throws IOException {
        File outputFile = outputPath.toFile();

        try (FileOutputStream fileStream = new FileOutputStream(outputFile);
             Packer packer = new MessagePack().createPacker(fileStream))
        {
            packer.write(this.tags);
            packer.write(this.perceptron.weights);
            packer.write(this.perceptron.labels);
        }
    }

    protected static List<String> getContext(List<String> words) {
        return Stream.concat(Stream.concat(Arrays.stream(START),
                                           words.stream().map(Tagger::normalize)),
                             Arrays.stream(END))
                     .collect(Collectors.toList());
    }

    public List<String> tag(List<String> words) {
        List<String> context = getContext(words);
        String prev = START[0];
        String prev2 = START[1];
        int offset = START.length;
        List<String> tags = new ArrayList<>();
        for (int index = 0; index < words.size(); index++) {
            String word = words.get(index);

            String tag = this.tags.get(word);
            if (tag == null) {
                Features features =
                    Features.getFeatures(offset + index, word, context, prev, prev2);
                tag = this.perceptron.predict(features);
            }
            tags.add(tag);
            prev2 = prev;
            prev = tag;
        }
        return tags;
    }
}
