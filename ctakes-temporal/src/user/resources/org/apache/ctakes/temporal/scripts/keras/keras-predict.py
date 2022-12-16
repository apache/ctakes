#!python

from keras.models import Sequential, model_from_json
import numpy as np
import et_cleartk_io as ctk_io
import sys
import os.path


def main(args):
    if len(args) < 1:
        sys.stderr.write("Error - one required argument: <model directory>\n")
        sys.exit(-1)

    working_dir = args[0]

    ## Load models and weights:
    model_list = []
    model_ind = 0
    input_dims = 0
    outcomes = ctk_io.get_outcome_array(working_dir)
    #print("Outcomes array is %s" % (outcomes) )
    model = model_from_json(open(os.path.join(working_dir, "model_0.json")).read())
    model.load_weights(os.path.join(working_dir, "model_0.h5"))

    input_dims = 1200 #model.layers[0].input_shape[1]

    while True:
        try:
            line = sys.stdin.readline().rstrip()
            if not line:
                break

            ## Convert the line into a feature vector and pass to model.
            feat_list = ctk_io.feature_string_to_list(line.rstrip(), input_dims)
            feats = np.array(feat_list)
            feats = np.reshape(feats, (1, 6, input_dims / 6))
            #feats = np.reshape(feats, (1, input_dims))

            X_dup = []
            X_dup.append(feats)
            X_dup.append(feats)
            X_dup.append(feats)

            out = model.predict(X_dup, batch_size=1, verbose=0)[0]
            # print("Out is %s and decision is %d" % (out, out.argmax()))
        except KeyboardInterrupt:
            sys.stderr.write("Caught keyboard interrupt\n")
            break

        if line == '':
            sys.stderr.write("Encountered empty string so exiting\n")
            break

        out_str = outcomes[out.argmax()]

        print(out_str + "\n")
        sys.stdout.flush()

    sys.exit(0)


if __name__ == "__main__":
    main(sys.argv[1:])
