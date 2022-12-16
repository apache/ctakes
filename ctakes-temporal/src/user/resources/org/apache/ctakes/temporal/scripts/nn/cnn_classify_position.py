#!python

from keras.models import Sequential, model_from_json
import numpy as np
import et_cleartk_io as ctk_io
import sys
import os.path
import pickle
from keras.preprocessing.sequence import pad_sequences

def main(args):
    if len(args) < 1:
        sys.stderr.write("Error - one required argument: <model directory>\n")
        sys.exit(-1)
    working_dir = args[0]

    target_dir = 'ctakes-temporal/target/eval/thyme/train_and_test/event-event/'
    model_dir = os.path.join(os.environ['CTAKES_ROOT'], target_dir)
    maxlen   = pickle.load(open(os.path.join(model_dir, "maxlen.p"), "rb"))
    word2int = pickle.load(open(os.path.join(model_dir, "word2int.p"), "rb"))
    label2int = pickle.load(open(os.path.join(model_dir, "label2int.p"), "rb"))
    tdist2int = pickle.load(open(os.path.join(model_dir, "tdist2int.p"), "rb"))
    edist2int = pickle.load(open(os.path.join(model_dir, "edist2int.p"), "rb"))

    model = model_from_json(open(os.path.join(model_dir, "model_0.json")).read())
    model.load_weights(os.path.join(model_dir, "model_0.h5"))

    int2label = {}
    for label, integer in label2int.items():
      int2label[integer] = label

    while True:
        try:
            line = sys.stdin.readline().rstrip()
            if not line:
                break

            text, tdist, edist = line.strip().split('|')

            tokens = []
            for token in text.rstrip().split():
                if token in word2int:
                    tokens.append(word2int[token])
                else:
                    tokens.append(word2int['oov_word'])

            tdists = []
            for dist in tdist.rstrip().split():
                if dist in tdist2int:
                    tdists.append(tdist2int[dist])
                else:
                    tdists.append(tdist2int['oov_word'])

            edists = []
            for dist in edist.rstrip().split():
                if dist in edist2int:
                    edists.append(edist2int[dist])
                else:
                    edists.append(edist2int['oov_word'])

            if len(tokens) > maxlen:
                tokens = tokens[0:maxlen]
            if len(tdists) > maxlen:
                tdists = tdists[0:maxlen]
            if len(edists) > maxlen:
                edists = edists[0:maxlen]

            test_x1 = pad_sequences([tokens], maxlen=maxlen)
            test_x2 = pad_sequences([tdists], maxlen=maxlen)
            test_x3 = pad_sequences([edists], maxlen=maxlen)

            test_xs = []
            test_xs.append(test_x1)
            test_xs.append(test_x2)
            test_xs.append(test_x3)
            test_xs.append(test_x1)
            test_xs.append(test_x2)
            test_xs.append(test_x3)
            test_xs.append(test_x1)
            test_xs.append(test_x2)
            test_xs.append(test_x3)
            test_xs.append(test_x1)
            test_xs.append(test_x2)
            test_xs.append(test_x3)

            out = model.predict(test_xs, batch_size=50)[0]

        except KeyboardInterrupt:
            sys.stderr.write("Caught keyboard interrupt\n")
            break

        if line == '':
            sys.stderr.write("Encountered empty string so exiting\n")
            break

        out_str = int2label[out.argmax()]
        print out_str
        sys.stdout.flush()

    sys.exit(0)

if __name__ == "__main__":
    main(sys.argv[1:])
