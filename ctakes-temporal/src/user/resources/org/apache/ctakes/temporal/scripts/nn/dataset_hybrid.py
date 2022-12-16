#!/usr/bin/env python

import numpy as np
import sys, ConfigParser, collections
sys.dont_write_bytecode = True

class DatasetProvider:
  """THYME relation data"""
  
  def __init__(self, path):
    """Index words by frequency in a file"""

    self.word2int = {}  # words indexed by frequency
    self.tag2int = {}   # pos tags indexed by frequency
    self.label2int = {} # class to int mapping
    
    unigrams = [] # corpus as list
    tags = []     # pos tags as list
    labels = []   # classes as list
    for line in open(path):
      label, text, pos = line.strip().split('|')
      unigrams.extend(text.split())
      tags.extend(pos.split())
      labels.append(label)
        
    index = 1 # zero used to encode unknown words
    self.word2int['oov_word'] = 0
    unigram_counts = collections.Counter(unigrams)
    for unigram, count in unigram_counts.most_common():
      self.word2int[unigram] = index
      index = index + 1

    index = 1 # zero used to encode unknown pos tags
    self.tag2int['oov_tag'] = 0
    tag_counts = collections.Counter(tags)
    for tag, count in tag_counts.most_common():
      self.tag2int[tag] = index
      index = index + 1

    index = 0 # index classes
    for label in set(labels):
      self.label2int[label] = index
      index = index + 1

  def load(self, path, maxlen=float('inf')):
    """Convert sentences (examples) into lists of indices"""

    examples = [] # sequences of words as ints
    tagseqs = []  # sequences of pos tags as ints
    labels = []   # labels

    for line in open(path):
      label, text, pos = line.strip().split('|')

      example = []
      for unigram in text.split():
        if unigram in self.word2int:
          example.append(self.word2int[unigram])
        else:
          example.append(self.word2int['oov_word'])

      tagseq = []
      for tag in pos.split():
        if tag in self.tag2int:
          tagseq.append(self.tag2int[tag])
        else:
          tagseq.append(self.tag2int['oov_tag'])

      # truncate example if it's too long
      if len(example) > maxlen:
        example = example[0:maxlen]
      if len(tagseq) > maxlen:
        tagseq = tagseq[0:maxlen]

      examples.append(example)
      tagseqs.append(tagseq)
      labels.append(self.label2int[label])

    return examples, tagseqs, labels

if __name__ == "__main__":

  cfg = ConfigParser.ConfigParser()
  cfg.read(sys.argv[1])

  dataset = DatasetProvider(cfg.get('data', 'train'))
  print 'alphabet size:', len(dataset.tag2int)
  x1, x2, y = dataset.load(cfg.get('data', 'train'))
  print 'train max seq len:', max([len(s) for s in x1])
  
  x1, x2, y = dataset.load(cfg.get('data', 'test'), maxlen=10)
  print 'test max seq len:', max([len(s) for s in x2])
  print 'labels:', dataset.label2int
  print 'label counts:', collections.Counter(y)
  print 'first 10 examples:', x2[:10]
