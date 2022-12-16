#!/usr/bin/env python

import sklearn as sk

import numpy as np
np.random.seed(1337)

import et_cleartk_io as ctk_io
import nn_models

import sys
import os.path

import dataset

import keras as k
from keras.utils.np_utils import to_categorical
from keras.optimizers import RMSprop
from keras.preprocessing.sequence import pad_sequences
from keras.models import Sequential,Model
from keras.layers import Input,merge
from keras.layers.core import Dense, Dropout, Activation, Flatten
from keras.layers.convolutional import Convolution1D, MaxPooling1D, AveragePooling1D
from keras.layers.embeddings import Embedding
from keras.layers.normalization import BatchNormalization

import pickle

def _conv_bn_relu(nb_filter, subsample=1):
    def f(input):
        conv = Convolution1D(nb_filter=nb_filter, subsample_length=subsample,
                             filter_length=int(3),border_mode="same")(input)
        norm = BatchNormalization(mode=2)(conv)
        return Activation("relu")(norm)

    return f

def _bn_relu_conv(nb_filter, subsample=1):
    def f(input):
        norm = BatchNormalization(mode=2)(input)
        activation = Activation("relu")(norm)
        return Convolution1D(nb_filter=nb_filter, subsample_length=subsample,
                             filter_length=int(3),border_mode="same")(activation)

    return f

def _residual_block(block_function, nb_filters, repetations):
    def f(input):
        for i in range(repetations):
            input = block_function(nb_filters=nb_filters)(input)
        return input

    return f

def _bottleneck(nb_filters, subsample=1):
    def f(input):
        conv_1   = _bn_relu_conv(nb_filters, subsample=subsample)(input)
        residual = _bn_relu_conv(nb_filters)(conv_1)
        return merge([input, residual], mode='sum')

    return f

def resnet(maxlen, alphabet, classes):
    input = Input(shape=(maxlen,),dtype='int32')
    embeds = Embedding(len(alphabet),
                         200,
                         input_length=maxlen,
                         weights=None)(input)

    conv1 = _conv_bn_relu(nb_filter=100, subsample=1)(embeds)
    pool1 = MaxPooling1D(pool_length=2)(conv1)

    #build residual block 16 layers:
    block_fn = _bottleneck
    block1   = _residual_block(block_fn, nb_filters=100,repetations=2)(pool1)
    block2   = _residual_block(block_fn, nb_filters=100,repetations=2)(block1)
    block3   = _residual_block(block_fn, nb_filters=100,repetations=2)(block2)
    block4   = _residual_block(block_fn, nb_filters=100,repetations=2)(block3)

    pool2 = AveragePooling1D(pool_length=4)(block4)
    flat  = Flatten()(pool2)
    dense = Dense(classes, init="he_normal", activation='softmax')(flat)

    model = Model(input=input, output=dense)
    return model

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

    #branches = [] # models to be merged
    #train_xs = [] # train x for each branch
    #test_xs = []  # test x for each branch

    model   = resnet(maxlen, dataset1.alphabet, classes)

    optimizer = RMSprop(lr=0.0001,#cfg.getfloat('cnn', 'learnrt'),
                      rho=0.9, epsilon=1e-08)
    model.compile(loss='categorical_crossentropy',
                optimizer=optimizer,
                metrics=['accuracy'])
    model.fit(train_x,
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