#!/usr/bin/env python

from keras.models import Sequential
from keras.layers import Dense, Dropout, Activation, Convolution1D, MaxPooling1D, Lambda, Flatten, Merge
from keras.optimizers import SGD
from keras import backend as K
from keras.optimizers import RMSprop

def get_mlp_model(dimension, num_outputs, layers=(64, 256, 64) ):
    model = Sequential()
    sgd = get_mlp_optimizer()

    drop = 0.5

    # Dense(64) is a fully-connected layer with 64 hidden units.
    # in the first layer, you must specify the expected input data shape:
    # here, 20-dimensional vectors.
    model.add(Dense(layers[0], input_dim=dimension, init='uniform'))
    model.add(Activation('relu'))
    model.add(Dropout(drop))
    model.add(Dense(layers[1], init='uniform'))
    model.add(Activation('relu'))
    model.add(Dropout(drop))
    #model.add(Dense(layers[2], init='uniform'))
    #model.add(Activation('relu'))
    #model.add(Dropout(drop))

#            model.add(Dense(layers[2], init='uniform'))
#            model.add(Activation('relu'))
#            model.add(Dropout(0.5))

    if num_outputs == 1:
        model.add(Dense(1, init='uniform'))
        model.add(Activation('sigmoid'))
        model.compile(loss='binary_crossentropy',
                      optimizer=sgd,
                      metrics=['accuracy'])
    else:
        model.add(Dense(num_outputs, init='uniform'))
        model.add(Activation('softmax'))                
        model.compile(loss='categorical_crossentropy',
                      optimizer=sgd,
                      metrics=['accuracy'])

    return model

def get_mlp_optimizer():
    return SGD(lr=0.1, decay=1e-6, momentum=0.9, nesterov=True)

def get_cnn_model(dimension, num_outputs, nb_filter = 200, layers=(64, 64, 256) ):
    model = Sequential()
    sgd = get_mlp_optimizer()

    ## Convolutional layers:
    model.add(Convolution1D(nb_filter, 3, input_shape=(6,200)))
    def max_1d(X):
        return K.max(X, axis=1)

    model.add(Lambda(max_1d, output_shape=(nb_filter,)))

    
    #model.add(MaxPooling1D())

    model.add(Dense(layers[1], init='uniform'))
    model.add(Activation('relu'))
    model.add(Dropout(0.5))

#    model.add(Dense(layers[2], init='uniform'))
#    model.add(Activation('relu'))
#    model.add(Dropout(0.5))

    if num_outputs == 1:
        model.add(Dense(1, init='uniform'))
        model.add(Activation('sigmoid'))
        model.compile(loss='binary_crossentropy',
                      optimizer=sgd,
                      metrics=['accuracy'])
    else:
        model.add(Dense(num_outputs, init='uniform'))
        model.add(Activation('softmax'))                
        model.compile(loss='categorical_crossentropy',
                      optimizer=sgd,
                      metrics=['accuracy'])

    return model

def get_dima_cnn_model(dimension, num_outputs):
    filtlens = "3,4,5"
    branches = [] # models to be merged
    train_xs = []
    for filterLen in filtlens.split(','):
        branch = Sequential()
        branch.add(Convolution1D(nb_filter=200,
                             filter_length=int(filterLen),
                             border_mode='valid',
                             activation='relu',
                             subsample_length=1,
                             input_shape=(6,200)))
        branch.add(MaxPooling1D(pool_length=2))
        branch.add(Flatten())

        branches.append(branch)
    model = Sequential()
    model.add(Merge(branches, mode='concat'))

    dropout = 0.25
    model.add(Dense(250))
    model.add(Dropout(dropout))
    model.add(Activation('relu'))

    model.add(Dropout(dropout))
    model.add(Dense(num_outputs))
    model.add(Activation('softmax'))

    optimizer = RMSprop(lr=0.001,
                      rho=0.9, epsilon=1e-08)
    model.compile(loss='categorical_crossentropy',
                optimizer=optimizer,
                metrics=['accuracy'])

    return model, branches