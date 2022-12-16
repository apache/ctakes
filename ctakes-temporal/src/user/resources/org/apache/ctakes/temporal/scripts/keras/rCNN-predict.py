#!python

from keras.models import Sequential, model_from_json
import numpy as np
import et_cleartk_io as ctk_io
import sys
import os.path
import pickle
from keras.preprocessing.sequence import pad_sequences
from fnmatch import fnmatch

def main(args):
    if len(args) < 1:
        sys.stderr.write("Error - one required argument: <model directory>\n")
        sys.exit(-1)

    working_dir = args[0]

    int2label = {
        0:'none',
        1:'CONTAINS',
        2:'CONTAINS-1'
    }

    ## Load models and weights:
    #outcomes = ctk_io.get_outcome_array(working_dir)
    model_dir = "/Users/chenlin/Programming/ctakesWorkspace/ctakes/ctakes-temporal/target/eval/thyme/train_and_test/event-time"
    maxlen   = pickle.load(open(os.path.join(model_dir, "maxlen.p"), "rb"))
    alphabet = pickle.load(open(os.path.join(model_dir, "alphabet.p"), "rb"))
    #print("Outcomes array is %s" % (outcomes) )
    model = model_from_json(open(os.path.join(model_dir, "model_0.json")).read())
    model.load_weights(os.path.join(model_dir, "model_0.h5"))

    while True:
        try:
            line = sys.stdin.readline().rstrip()
            if not line:
                break

            ## Convert the line of Strings to lists of indices
            pre=[]
            arg1=[]
            cont=[]
            arg2=[]
            post=[]
            train_x = []
            tag = 0
            for unigram in line.rstrip().split():
                if(alphabet.has_key(unigram)):
                    idx = alphabet[unigram]
                else:
                    idx = alphabet["none"]

                train_x.append(idx)
                if( fnmatch(unigram, '<*>')):
                    tag = tag + 1
                    continue
                if(tag ==0 ):
                    pre.append(idx)
                elif(tag == 1):
                    arg1.append(idx)
                elif(tag == 2):
                    cont.append(idx)
                elif(tag == 3):
                    arg2.append(idx)
                elif(tag == 4):
                    post.append(idx)

            train_x = pad_sequences([train_x], maxlen=maxlen, truncating='pre')
            pres_x = pad_sequences([pre], maxlen=5, truncating='pre')
            arg1s_x = pad_sequences([arg1], maxlen = 5, truncating='pre')
            conts_x  = pad_sequences([cont], maxlen = 120, truncating='pre')
            arg2s_x = pad_sequences([arg2], maxlen = 5, truncating='pre')
            posts_x = pad_sequences([post], maxlen=5, truncating='post')
            #test_x = pad_sequences([feats], maxlen=maxlen)
            #feats = np.reshape(feats, (1, 6, input_dims / 6))
            #feats = np.reshape(feats, (1, input_dims))

            X_dup = []
            X_dup.append(train_x)
            X_dup.append(train_x)
            X_dup.append(train_x)
            X_dup.append(pres_x)
            X_dup.append(pres_x)
            X_dup.append(arg1s_x)
            X_dup.append(conts_x)
            X_dup.append(conts_x)
            X_dup.append(conts_x)
            X_dup.append(arg2s_x)
            X_dup.append(posts_x)
            X_dup.append(posts_x)

            out = model.predict(X_dup)[0]
            # print("Out is %s and decision is %d" % (out, out.argmax()))
        except KeyboardInterrupt:
            sys.stderr.write("Caught keyboard interrupt\n")
            break

        if line == '':
            sys.stderr.write("Encountered empty string so exiting\n")
            break

        out_str = int2label[out.argmax()]

        print(out_str)
        sys.stdout.flush()

    sys.exit(0)


if __name__ == "__main__":
    main(sys.argv[1:])
