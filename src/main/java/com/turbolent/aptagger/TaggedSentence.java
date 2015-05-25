package com.turbolent.aptagger;

import java.util.List;

public class TaggedSentence {
    public final List<String> words;
    public final List<String> tags;

    public TaggedSentence(List<String> words, List<String> tags) {
        this.words = words;
        this.tags = tags;
    }
}
