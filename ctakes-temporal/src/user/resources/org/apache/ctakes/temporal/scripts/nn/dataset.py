#!/usr/bin/env python

import numpy as np
import sys
sys.dont_write_bytecode = True
import ConfigParser
import glob, string, collections, operator

class DatasetProvider:
  """THYME relation data"""
  
  def __init__(self, path):
    """Index words by frequency in a file"""

    self.word2int = {} # words indexed by frequency
    self.label2int = {}   # class to int mapping
    
    unigrams = [] # corpus as list
    labels = []   # classes as list
    for line in open(path):
      label, text = line.strip().split('|')
      unigrams.extend(text.split())
      labels.append(label)
        
    index = 1 # zero used to encode unknown words
    self.word2int['oov_word'] = 0
    unigram_counts = collections.Counter(unigrams)
    for unigram, count in unigram_counts.most_common():
      self.word2int[unigram] = index
      index = index + 1

    index = 0 # index classes
    for label in set(labels):
      self.label2int[label] = index
      index = index + 1

  def load(self, path, maxlen=float('inf')):
    """Convert sentences (examples) into lists of indices"""

    examples = []
    labels = []

    for line in open(path):
      label, text = line.strip().split('|')
      example = []
      for unigram in text.split():
        if unigram in self.word2int:
          example.append(self.word2int[unigram])
        else:
          example.append(self.word2int['oov_word'])

      # truncate example if it's too long
      if len(example) > maxlen:
        example = example[0:maxlen]

      examples.append(example)
      labels.append(self.label2int[label])

    return examples, labels

if __name__ == "__main__":

  cfg = ConfigParser.ConfigParser()
  cfg.read(sys.argv[1])

  dataset = DatasetProvider(cfg.get('data', 'train'))
  print 'alphabet size:', len(dataset.word2int)

  x,y = dataset.load(cfg.get('data', 'train'))
  print 'train max seq len:', max([len(s) for s in x])
  
  x,y = dataset.load(cfg.get('data', 'test'), maxlen=10)
  print 'number of examples:', len(x)
  print 'test max seq len:', max([len(s) for s in x])
  print 'labels:', dataset.label2int
  print 'label counts:', collections.Counter(y)
  print 'first 10 examples:', x[:10]
