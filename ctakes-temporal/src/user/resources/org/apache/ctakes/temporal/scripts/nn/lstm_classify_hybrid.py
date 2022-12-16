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

    target_dir = 'ctakes-temporal/target/eval/thyme/train_and_test/event-time/'
    model_dir = os.path.join(os.environ['CTAKES_ROOT'], target_dir)
    maxlen   = pickle.load(open(os.path.join(model_dir, "maxlen.p"), "rb"))
    word2int = pickle.load(open(os.path.join(model_dir, "word2int.p"), "rb"))
    tag2int = pickle.load(open(os.path.join(model_dir, "tag2int.p"), "rb"))
    label2int = pickle.load(open(os.path.join(model_dir, "label2int.p"), "rb"))
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

            text, pos = line.strip().split('|')

            tokens = []
            for token in text.rstrip().split():
                if token in word2int:
                    tokens.append(word2int[token])
                else:
                    tokens.append(word2int['none'])

            tags = []
            for tag in pos.rstrip().split():
                if tag in tag2int:
                    tags.append(tag2int[tag])
                else:
                    tags.append(tag2int['oov_tag'])

            if len(tokens) > maxlen:
                tokens = tokens[0:maxlen]
            if len(tags) > maxlen:
                tags = tags[0:maxlen]

            test_x1 = pad_sequences([tokens], maxlen=maxlen)
            test_x2 = pad_sequences([tags], maxlen=maxlen)

            test_xs = []
            test_xs.append(test_x1)
            test_xs.append(test_x2)

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
