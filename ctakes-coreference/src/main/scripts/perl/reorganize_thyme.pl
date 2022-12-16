#!/usr/bin/perl

use File::Basename;
use strict;

if ($#ARGV < 1){
  print "Error: Two arguments required <Current location> <New location>\n";
  exit;
}

if(not -e $ARGV[1]){
  print "Output directory does not exist; creating for you.\n";
  mkdir $ARGV[1];
}

my $in_dir = $ARGV[0];
my $out_dir = $ARGV[1];

for my $split ("Train", "Test", "Dev"){
    my @split_dirs = glob($ARGV[0]."/$split/*");
    for my $dir (@split_dirs){
        my $dirname = dirname($dir);
        my $fname = basename($dir);
        my ($pt, $note_type, $enc_id) = split(/_/, $fname);
        
        print "Directory $dir is for patient $pt, with note type $note_type, and encounter id $enc_id\n";

        my @corefs = glob("$dir/*.Coreference.gold.completed.xml");
        if($#corefs < 0){
          next;
        }        

        if(not -e "$out_dir/$pt"){
            mkdir "$out_dir/$pt";
        }
        
        `cp -r $dir $out_dir/$pt`;
    }
}
