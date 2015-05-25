aptagger
========

[![Build Status](https://travis-ci.org/turbolent/aptagger.svg?branch=master)](https://travis-ci.org/turbolent/aptagger)

An implementation of [Matthew Honnibal's](https://github.com/syllog1sm/) 
fast and accurate part-of-speech tagger based on the Averaged Perceptron, 
as described in ["A good POS tagger in about 200 lines of Python"]
(https://honnibal.wordpress.com/2013/09/11/a-good-part-of-speechpos-tagger-in-about-200-lines-of-python/)


## Usage

### API

```java
Path modelPath = Paths.get("<path-to-model>");
Tagger tagger = Tagger.loadFrom(modelPath);
List<String> words = Arrays.asList("Simple", "is", "better", "than", "complex");
List<String> tags = tagger.tag(words);
```

### Command Line

    $ mvn compile assembly:single
    $ java -jar target/aptagger.jar
	Usage:
	  tag <model-file> <word>...
	  train <corpus-file> <model-file>
	  test <model-file> <corpus-file>

	Commands:
	  tag    Returns a tagged sentence of the given words using the given model.
	  train  Creates a model from the given corpus, which should contain
	         one tagged sentence per line.
	  test   Tests how the given model performs for the given corpus.
	         Prints the ratio of correct and total number of tags.

	Format:
	  A tagged sentence consists of tokens separated by spaces, where each token
	  is a combination of a word and a tag, separated by an underscore.
	  For example: "Simple_NN is_VBZ better_JJR than_IN complex_JJ ._."