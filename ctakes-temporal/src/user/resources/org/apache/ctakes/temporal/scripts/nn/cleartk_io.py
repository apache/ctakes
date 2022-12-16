#!/usr/bin/env python3

import numpy as np
import os, os.path
import subprocess


def string_label_to_label_vector(label_string, outcome_maps):    
    label_vec = []
    
    for label_val in label_string.split('#'):
        (label, val) = label_val.split('=')
        cur_map = outcome_maps[label]
        label_ind = cur_map[val]
        label_vec.append(label_ind)
        
    return label_vec
    
def get_data_dimensions(data_file):
    wc_out = subprocess.check_output(['wc',  data_file])
    wc_fields = wc_out.decode().strip().split(' ')
    file_len = int(wc_fields[0])

    num_feats = 0
    for line in open(data_file):
        max_dim = int( line.rstrip().split(' ')[-1].split(':')[0] )
        if max_dim > num_feats:
            num_feats = max_dim

    return (file_len, num_feats)

def flatten_outputs(Y):
    maxes = Y.max(0)
    #print("Maxes = %s" % (maxes) )
    reqd_dims = 0
    indices = [0]
    
    ## Create an indices array that maps from "true" label indices to neural network 
    ## output layer indices -- binary labels map to single output nodes (2->1) while n-ary
    ## labels map to n nodes.
    for val in maxes:
        if val == 1:
            reqd_dims += 1
        elif val > 1:
            reqd_dims += (int(val) + 1)
        else:
            raise Exception("There is a column with all zeros!")
            
        indices.append(reqd_dims)

    Y_adj = np.zeros( (Y.shape[0], reqd_dims) )
    for row_ind in range(0, Y.shape[0]):
        for col_ind in range(0, Y.shape[1]):
            if maxes[col_ind] == 1:
                ## For binary variables just need the offset and copy the value
                Y_adj[row_ind][ int(indices[col_ind]) ] = Y[row_ind][col_ind]
            else:
                ## for n-ary variables we use the value to find the offset that will 
                ## be set to 1.
                Y_adj[row_ind][ int(indices[col_ind]) + int(Y[row_ind][col_ind]) ] = 1
    
    return Y_adj, indices

def read_outcome_maps(dirname):
    raw_outcomes = []
    raw_outcomes.append(None)
    
    derived_maps = {}
    lookup_map = {}
    ## First read outcome file
    for line in open(os.path.join(dirname, 'outcome-lookup.txt') ):
        (index, label) = line.rstrip().split(' ')
        raw_outcomes.append(label)
        
        for task_label in label.split('#'):
            #print(task_label)
            (task, val) = task_label.rstrip().split("=")
            if not task in derived_maps:
                derived_maps[task] = {}
                lookup_map[task] = []
                
            cur_map = derived_maps[task]
            lookup = lookup_map[task]
            if not val in cur_map:
                cur_map[val] = len(cur_map)
                lookup.append(val)
    
    return raw_outcomes, derived_maps, lookup_map

def outcome_list(raw_outcomes):
    outcomes = []
    for outcome_val in raw_outcomes[1].split("#"):
        outcomes.append(outcome_val.split("=")[0])
    
    return outcomes
    
def read_multitask_liblinear(dirname):
    
    raw_outcomes, derived_maps, outcome_lookups = read_outcome_maps(dirname)
        
    data_file = os.path.join(dirname, 'training-data.liblinear')
    
    (data_points, feat_dims) = get_data_dimensions(data_file)
    
    ## Remove bias feature -- will be part of any neural network
    label_dims = len(derived_maps)
    
    label_matrix = np.zeros( (data_points, label_dims) )
    feat_matrix = np.zeros( (data_points, feat_dims) )
    
    line_ind = 0
    for line in open( data_file ):
        label_and_feats = line.rstrip().split(' ')
        label = label_and_feats[0]
        string_label = raw_outcomes[int(label)]
        label_vec = string_label_to_label_vector(string_label, derived_maps)
        
        for ind, val in enumerate(label_vec):
            label_matrix[line_ind, ind] = val
    
        ## Go from 2 on -- skip both the label and the first feature since it will be
        ## the bias term from the liblinear data writer.
#        feat_list = feature_array_to_list( label_and_feats[1:], feat_dims )
#        feat_matrix[line_ind,:] = feat_list[1:]
        feat_matrix[line_ind, :] = feature_array_to_list( label_and_feats[1:], feat_dims )
#        for feat in label_and_feats[1:]:
#            (ind, val) = feat.split(':')
#            feat_ind = int(ind) - 1    ## since feats are indexed at 1
#            feat_matrix[line_ind, feat_ind] = float(val)
            
                
        line_ind += 1

    return label_matrix, feat_matrix

def convert_multi_output_to_string(outcomes, outcome_list, lookup_map, raw_outcomes):
    """Return the int value corresponding to the class implied by the
    set of outputs in the outcomes array."""
    str = ''
    for ind, label in enumerate(outcome_list):
        str += label
        str += "="
        str += lookup_map[label][outcomes[ind]]
        str += "#"
        
    str = str[:-1]
    return str
    
def feature_string_to_list( feat_string, length=-1 ):
    return feature_array_to_list( feat_string.split(' '), length )

def feature_array_to_list( feats, length=-1 ):
    if length == -1:
        length = len(feats)
        
    #f = np.zeros(length)
    f = [0] * length
    
    for feat in feats:
        (ind, val) = feat.split(':')
        ind = int(ind) - 1
        if int(ind) >= len(f):
            raise Exception("Feature index %d is larger than feature vector length %d -- you may need to specify the expected length of the vector." % (int(ind), len(f) ) )
        f[int(ind)] = val
    
    return f
    
if __name__ == "__main__":
    (labels, feats) = read_multitask_liblinear('data_testing/multitask_assertion/train_and_test/')
    print("train[0][100] = %f" % feats[0][100])
