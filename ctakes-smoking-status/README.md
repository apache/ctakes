This is version 1.2 of the cTAKES smoking status annotator.
The pipeline has been tested on flat text files and CDA documents.
The sample provided uses a bar (|) delimited file with multiple records (and patients) per file. 
The dictionary lookup annotator is limited to smoking status dictionaries provided in the '/resources/ss/data/*.dictionary' files. 

The smoking status pipeline processes patient records into five pre-determined categories -
past smoker (P), current smoker (C), smoker (S), non-smoker (N), and unknown (U).
The definition of smoking status was adapted from I2B2 Natural Language Processing Challenges for Clinical Records. 

- PAST SMOKER (P): A patient whose record asserts either that they are a past smoker or that they were a smoker a year or more ago but who have not smoked for at least one year. 
- CURRENT SMOKER (C): A patient whose record asserts that they are a current smoker (or that they smoked without indicating that they stopped more than a year ago) or that they were a smoker within the past year.
- SMOKER (S): A patient who is either a CURRENT or a PAST smoker but, whose medical record does not provide enough information to classify the patient as either a CURRENT or a PAST smoker.
- NON-SMOKER (N): A patient whose record indicates that they have never smoked.
- UNKNOWN (U): The patient's record does not mention anything about smoking.

