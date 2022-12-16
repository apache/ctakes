#!/usr/bin/env python

import sklearn as sk
import numpy as np
np.random.seed(1337)
import et_cleartk_io as ctk_io
import nn_models
import sys
import os.path
import dataset_position
import keras as k
from keras.utils.np_utils import to_categorical
from keras.optimizers import RMSprop
from keras.preprocessing.sequence import pad_sequences
from keras.models import Sequential
from keras.layers import Merge
from keras.layers.core import Dense, Dropout, Activation, Flatten
from keras.layers.convolutional import Convolution1D, MaxPooling1D
from keras.layers.embeddings import Embedding
import pickle

def main(args):
  if len(args) < 1:
    sys.stderr.write("Error - one required argument: <data directory>\n")
    sys.exit(-1)
  working_dir = args[0]
  data_file = os.path.join(working_dir, 'training-data.liblinear')

  # learn alphabets from training examples
  provider = dataset_position.DatasetProvider(data_file)
  # now load training examples and labels
  train_x1, train_x2, train_x3, train_y = provider.load(data_file)
  maxlen = max([len(seq) for seq in train_x1])

  # turn x and y into numpy array among other things
  classes = len(set(train_y))
  train_x1 = pad_sequences(train_x1, maxlen=maxlen)
  train_x2 = pad_sequences(train_x2, maxlen=maxlen)
  train_x3 = pad_sequences(train_x3, maxlen=maxlen)
  train_y = to_categorical(np.array(train_y), classes)

  pickle.dump(maxlen, open(os.path.join(working_dir, 'maxlen.p'),"wb"))
  pickle.dump(provider.word2int, open(os.path.join(working_dir, 'word2int.p'),"wb"))
  pickle.dump(provider.label2int, open(os.path.join(working_dir, 'label2int.p'),"wb"))
  pickle.dump(provider.tdist2int, open(os.path.join(working_dir, 'tdist2int.p'),"wb"))
  pickle.dump(provider.edist2int, open(os.path.join(working_dir, 'edist2int.p'),"wb"))

  branches = [] # models to be merged
  train_xs = [] # train x for each branch
  test_xs = []  # test x for each branch

  for filter_len in '2,3,4,5'.split(','):

    # token embeddings
    embed1 = Sequential()
    embed1.add(Embedding(input_dim=len(provider.word2int),
                         output_dim=300,
                         input_length=maxlen))
    # distance to timex position embeddings
    embed2 = Sequential()
    embed2.add(Embedding(input_dim=len(provider.tdist2int),
                         output_dim=50,
                         input_length=maxlen))
    # distance to event position embeddings
    embed3 = Sequential()
    embed3.add(Embedding(input_dim=len(provider.edist2int),
                         output_dim=50,
                         input_length=maxlen))

    branch = Sequential()
    branch.add(Merge([embed1, embed2, embed3], mode='concat'))
    branch.add(Convolution1D(nb_filter=200,
                             filter_length=int(filter_len),
                             border_mode='valid',
                             activation='relu',
                             subsample_length=1))
    branch.add(MaxPooling1D(pool_length=2))
    branch.add(Flatten())

    branches.append(branch)

    train_xs.append(train_x1)
    train_xs.append(train_x2)
    train_xs.append(train_x3)

  model = Sequential()
  model.add(Merge(branches, mode='concat'))

  model.add(Dropout(0.25))
  model.add(Dense(300))
  model.add(Activation('relu'))

  model.add(Dropout(0.25))
  model.add(Dense(classes))
  model.add(Activation('softmax'))

  optimizer = RMSprop(lr=0.0001,
                      rho=0.9, epsilon=1e-08)
  model.compile(loss='categorical_crossentropy',
                optimizer=optimizer,
                metrics=['accuracy'])
  model.fit(train_xs,
            train_y,
            nb_epoch=40,
            batch_size=50,
            verbose=0,
            validation_split=0.1,
            class_weight=None)

  json_string = model.to_json()
  open(os.path.join(working_dir, 'model_0.json'), 'w').write(json_string)
  model.save_weights(os.path.join(working_dir, 'model_0.h5'), overwrite=True)
  sys.exit(0)

if __name__ == "__main__":
    main(sys.argv[1:])
