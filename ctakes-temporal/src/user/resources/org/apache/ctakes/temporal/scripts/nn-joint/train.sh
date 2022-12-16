#!/bin/bash

#source $(dirname $0)/../keras/env/bin/activate
#python $(dirname $0)/cnn_train.py $* 2> nn_error.log
ssh clin@nlp-gpu "/home/clin/Projects/deepLearning/nn/train.sh"
ret=$?
#deactivate
exit $ret
