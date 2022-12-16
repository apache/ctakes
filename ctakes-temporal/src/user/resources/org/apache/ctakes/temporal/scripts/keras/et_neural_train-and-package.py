#!/usr/bin/env python

from keras.models import Sequential
from keras.layers import Dense, Dropout, Activation
from keras.optimizers import SGD
from keras.utils import np_utils
#from sklearn.datasets import load_svmlight_file
import sklearn as sk
import sklearn.cross_validation
import numpy as np
import et_cleartk_io as ctk_io
import nn_models
import sys
import os.path

batch_size = 64
nb_epoch = 5
layers = (256, 256, 256)

def main(args):
    if len(args) < 1:
        sys.stderr.write("Error - one required argument: <data directory>\n")
        sys.exit(-1)

    working_dir = args[0]
    
#    print("Reading data...")
    Y, X = ctk_io.read_liblinear(working_dir) # ('data_testing/multitask_assertion/train_and_test')

    num_outputs = Y.shape[-1]
    num_examples, dimension = X.shape
    num_y_examples, num_labels = Y.shape
    assert num_examples == num_y_examples
    
    #print("Data has %d examples and dimension %d" % (num_examples, dimension) )
    #print("Output has %d dimensions" % (num_labels) )

    X = np.reshape(X, (num_examples, 6, dimension / 6))
    
    #Y_adj, indices = ctk_io.flatten_outputs(Y)

    #print("After reshaping the data has shape %s" % (str(X.shape)))
    
    '''for label_ind in range(0, Y.shape[1]):
        
        num_outputs = indices[label_ind+1] - indices[label_ind]
        model = nn_models.get_cnn_model(X.shape, num_outputs)

        #print("For label ind %d, grabbing indices from %d to %d" % (label_ind, int(indices[label_ind]), int(indices[label_ind+1])))
        
        train_y = Y_adj[:, int(indices[label_ind]):int(indices[label_ind+1])]

        #if(train_y.shape[-1] == 1):
        #    print("Number of values=1 is %d" % (train_y.sum()))

        #print("Shape of y is %s, shape of X is %s, max value in y is %f and min is %f" % (str(train_y.shape), str(X.shape), train_y.max(), train_y.min()) )
        
        model.fit(X, train_y,
                  nb_epoch=nb_epoch,
                  batch_size=batch_size,
                  verbose=1)
        
        model.summary()
        
        json_string = model.to_json()
        open(os.path.join(working_dir, 'model_%d.json' % label_ind), 'w').write(json_string)
        model.save_weights(os.path.join(working_dir, 'model_%d.h5' % label_ind), overwrite=True)
        
        #print("This model has %d layers and layer 3 has %d weights" % (len(model.layers), len(model.layers[3].get_weights()) ) )
        #print("The weight of the first layer at index 50 is %f" % model.layers[3].get_weights()[50])
    '''

    X_dup = []
    X_dup.append(X)
    X_dup.append(X)
    X_dup.append(X)
    model, branches = nn_models.get_dima_cnn_model(dimension, num_outputs)
    model.fit(X_dup, Y,
                  nb_epoch=nb_epoch,
                  batch_size=batch_size,
                  verbose=1)
    for b in branches:
        b.trainable  = False

    model.fit(X_dup, Y,
                  nb_epoch=nb_epoch,
                  batch_size=batch_size,
                  verbose=1)

    model.summary()

    json_string = model.to_json()
    open(os.path.join(working_dir, 'model_0.json'), 'w').write(json_string)
    model.save_weights(os.path.join(working_dir, 'model_0.h5'), overwrite=True)
    sys.exit(0)

if __name__ == "__main__":
    main(sys.argv[1:])