This side effect extraction system extracts physician-asserted drug side effects from clinical notes.
Alternatively, the system can extract sentences that possibly contain both side effects and causative drugs,
which cover higher number of side effect occurrences but needs human validation to extract individual causative drugs and side effects.
The detailed method used here can be found in:  
S Sohn, JP A Kocher, CG Chute, GK Savova, "Drug side effect extraction from clinical narratives of psychiatry and psychology patients"
JAMIA doi:10.1136/amiajnl-2011-000351  
The performance reported in the paper is based on a variation of cTAKES. 
So the actual performance of this release is slightly different from it. 
Also, note that this release uses LibSVM for side effect sentence classification instead of C4.5, which was used in the paper,
due to a license issue.  
The default cTAKES is shipped with a sample dictionary. 
To achieve the full function of it, it is necessary to create a more complete dictionary (refer to Creating your own dictionary in cTAKES User Guide).
This side effect extraction also requires Drug NER. 
