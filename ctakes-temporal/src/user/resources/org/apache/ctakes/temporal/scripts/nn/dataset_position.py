#!/usr/bin/env python

import numpy as np
import sys, ConfigParser, collections, os
sys.dont_write_bytecode = True

class DatasetProvider:
  """THYME relation data"""
  
  def __init__(self, path):
    """Index words by frequency in a file"""

    self.word2int = {}  # words indexed by frequency
    self.tdist2int = {} # distance to time to int map
    self.edist2int = {} # distance to event to int map
    self.label2int = {} # class to int map
    
    unigrams = []   # corpus as list
    labels = []     # classes as list
    tdistances = [] # distance to time
    edistances = [] # distance to event
    for line in open(path):
      label, text, tdist, edist = line.strip().split('|')
      labels.append(label)
      unigrams.extend(text.split())
      tdistances.extend(tdist.split())
      edistances.extend(edist.split())
        
    index = 1 # zero used to encode unknown words
    self.word2int['oov_word'] = 0
    unigram_counts = collections.Counter(unigrams)
    for unigram, count in unigram_counts.most_common():
      self.word2int[unigram] = index
      index = index + 1

    index = 1 # zero used to encode unknown words
    self.tdist2int['oov_word'] = 0
    tdist_counts = collections.Counter(tdistances)
    for tdist, count in tdist_counts.most_common():
      self.tdist2int[tdist] = index
      index = index + 1

    index = 1 # zero used to encode unknown words
    self.edist2int['oov_word'] = 0
    edist_counts = collections.Counter(edistances)
    for edist, count in edist_counts.most_common():
      self.edist2int[edist] = index
      index = index + 1

    index = 0 # index classes
    for label in set(labels):
      self.label2int[label] = index
      index = index + 1

  def load(self, path, maxlen=float('inf'), maxdist=1000):
    """Convert sentences (examples) into lists of indices"""

    examples = []   # sequences of words as ints
    tdistances = [] # distances to timex as ints
    edistances = [] # distances to event as ints
    labels = []     # labels

    for line in open(path):
      label, text, tdist, edist = line.strip().split('|')

      example = []
      for unigram in text.split():
        if unigram in self.word2int:
          example.append(self.word2int[unigram])
        else:
          example.append(self.word2int['oov_word'])

      tdistance = []
      for dist in tdist.split():
        if dist in self.tdist2int:
          tdistance.append(self.tdist2int[dist])
        else:
          tdistance.append(self.edist2int['oov_word'])

      edistance = []
      for dist in edist.split():
        if dist in self.edist2int:
          edistance.append(self.edist2int[dist])
        else:
          edistance.append(self.edist2int['oov_word'])
        
      # truncate example if it's too long
      # assume distances and examples have same length
      if len(example) > maxlen:
        example = example[0:maxlen]
        tdistance = tdistance[0:maxlen]
        edistance = edistance[0:maxlen]

      examples.append(example)
      tdistances.append(tdistance)
      edistances.append(edistance)
      labels.append(self.label2int[label])

    return examples, tdistances, edistances, labels

if __name__ == "__main__":

  cfg = ConfigParser.ConfigParser()
  cfg.read(sys.argv[1])
  base = os.environ['DATA_ROOT']
  train_file = os.path.join(base, cfg.get('data', 'train'))
  test_file = os.path.join(base, cfg.get('data', 'test'))

  dataset = DatasetProvider(test_file)
  x1, x2, x3, y = dataset.load(test_file)
  print 'first 10 examples:', x2[:10]
  print 'time dist alphabet len:', len(dataset.tdist2int)
