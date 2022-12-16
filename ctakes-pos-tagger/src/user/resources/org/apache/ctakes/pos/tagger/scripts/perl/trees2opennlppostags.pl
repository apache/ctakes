#!/usr/bin/perl
#
#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
# 
#       http://www.apache.org/licenses/LICENSE-2.0
# 
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
#===================================================================

use strict;
my $num_parens=0;
my $tree = "";

while(<STDIN>){
  chomp;
  ## Get rid of leading spaces
  s/^\s*//;

  if($_ eq ""){
    next;
  }

  # Accumulate tree lines until parens match
  $tree .= "$_ ";
  my @left_parens = m/(\()/g;
  my @right_parens = m/(\))/g;
  $num_parens += ($#left_parens + 1);
  $num_parens -= ($#right_parens + 1);
  if($num_parens == 0){
    ## Get rid of extra parentheses around the tree
      my @tokens = ( $tree =~ m/(?<=\()[^() ]+ [^() ]+(?=\))/g );
      foreach my $token (@tokens) {
	  $token =~ s/ /_/g;
	  print $token." ";
      }
      print "\n";
#    $tree =~ s/^\s*\(\s*\((.*)\s*\)\s*\)\s*$/(\1)/;
#    print "$tree\n";
#      print join(" ",@tokens);
    $tree = "";
  }

}
