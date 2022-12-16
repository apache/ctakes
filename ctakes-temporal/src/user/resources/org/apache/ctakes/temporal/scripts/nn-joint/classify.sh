#!/bin/bash
>&2 echo "Usign BERT backend"
#source $(dirname $0)/../keras/env/bin/activate
#python $(dirname $0)/bi_lstm_classify_hybrid.py $*
#python $(dirname $0)/bi_lstm_classify.py $*
#ret=$?
#deactivate
#exit $ret

ssh clin@nlp-gpu "/home/clin/Projects/deepLearning/nn/classify.sh"
ret=$?
exit $ret
