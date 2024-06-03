Performs coreference resolution for several types of coreference, excluding person mentions and some rare pronouns.

Most basically, the output of this module will be several data types added to 
the CAS representing the output of the system.  These types are as follows:

Markable - Subtyped into NEMarkable (Named entities), PronounMarkable (pronouns),
and DemMarkable (certain demonstrative and relative pronouns), these are automatically
discovered and taken as input to the coreference resolution algorithm.  These are types
required above the SHARP types for entities due to some special considerations with
span differences and differing type inheritances.

CoreferenceRelation - A type containing two Markables that are believed to
co-refer.  A CoreferenceRelation has two arguments of type RelationArgument,
with a role field containing a value of either "anaphor" or "antecedent."  There
is also an "argument" field which contains the Markable fulfilling the role.

CollectionTextRelation - A linked list containing chains of Annotations that the classifier
says refer to the same entity.  This is derived from the set of CoreferenceRelation
elements described above.  It contains a list of UIMA type NonEmptyFSList, as well
as a size field.  For singletons there are lists of length 1.  For actual chains
the size will be different, and each node in the list is of type NonEmptyFSList.
That type has a head and tail field.  The head points to the data for the node,
which is a Markable, and the tail points to the next element in the list, or
to a node of type EmptyFSList when the chain is complete.





