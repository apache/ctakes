#!/usr/bin/env python

import numpy as np
np.random.seed(1337)

import sys
import os.path

import dataset

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

    #read in data file
#    print("Reading data...")
    #Y, X = ctk_io.read_liblinear(working_dir) # ('data_testing/multitask_assertion/train_and_test')
    data_file = os.path.join(working_dir, 'training-data.liblinear')

    # learn alphabet from training and test data
    dataset1 = dataset.DatasetProvider([data_file])
    # now load training examples and labels
    train_x, train_y = dataset1.load(data_file)

    init_vectors = None #used for pre-trained embeddings
    
    # turn x and y into numpy array among other things
    maxlen = max([len(seq) for seq in train_x])
    outcomes = set(train_y)
    classes = len(outcomes)

    train_x = pad_sequences(train_x, maxlen=maxlen)
    train_y = to_categorical(np.array(train_y), classes)

    pickle.dump(maxlen, open(os.path.join(working_dir, 'maxlen.p'),"wb"))
    pickle.dump(dataset1.alphabet, open(os.path.join(working_dir, 'alphabet.p'),"wb"))
    #test_x = pad_sequences(test_x, maxlen=maxlen)
    #test_y = to_categorical(np.array(test_y), classes)

    print 'train_x shape:', train_x.shape
    print 'train_y shape:', train_y.shape

    branches = [] # models to be merged
    train_xs = [] # train x for each branch
    #test_xs = []  # test x for each branch

    filtlens = "3,4,5"
    for filter_len in filtlens.split(','):
        branch = Sequential()
        branch.add(Embedding(len(dataset1.alphabet),
                         300,
                         input_length=maxlen,
                         weights=init_vectors))
        branch.add(Convolution1D(nb_filter=200,
                             filter_length=int(filter_len),
                             border_mode='valid',
                             activation='relu',
                             subsample_length=1))
        branch.add(MaxPooling1D(pool_length=2))
        branch.add(Flatten())

        branches.append(branch)
        train_xs.append(train_x)
        #test_xs.append(test_x)
    model = Sequential()
    model.add(Merge(branches, mode='concat'))

    model.add(Dense(250))#cfg.getint('cnn', 'hidden')))
    model.add(Dropout(0.25))#cfg.getfloat('cnn', 'dropout')))
    model.add(Activation('relu'))

    model.add(Dropout(0.25))#cfg.getfloat('cnn', 'dropout')))
    model.add(Dense(classes))
    model.add(Activation('softmax'))

    optimizer = RMSprop(lr=0.0001,#cfg.getfloat('cnn', 'learnrt'),
                      rho=0.9, epsilon=1e-08)
    model.compile(loss='categorical_crossentropy',
                optimizer=optimizer,
                metrics=['accuracy'])
    model.fit(train_xs,
            train_y,
            nb_epoch=3,#cfg.getint('cnn', 'epochs'),
            batch_size=50,#cfg.getint('cnn', 'batches'),
            verbose=1,
            validation_split=0.1,
            class_weight=None)

    model.summary()

    json_string = model.to_json()
    open(os.path.join(working_dir, 'model_0.json'), 'w').write(json_string)
    model.save_weights(os.path.join(working_dir, 'model_0.h5'), overwrite=True)
    sys.exit(0)

if __name__ == "__main__":
    main(sys.argv[1:])