This module is dedicated to processing electronic medical records for meaningful events, temporal expressions, and their relations on a timeline.  

Temporal relations are of prime importance in biomedicine as they are intrinsically linked to diseases,
signs and symptoms, and treatments. 
The identification of temporal relations in medical text has drawn growing attention because of its
potential to dramatically increase the understanding of many medical phenomena such as disease progression,
longitudinal effects of medications, and a patient's clinical course, and its many clinical applications 
such as question answering[1, 2], clinical outcomes prediction[3], and the recognition of temporal patterns 
and timelines[4, 5].

<details>
<summary>Key components</summary>

**Event annotator**  
A Begin-Inside-Outside (BIO) style sequence annotator for clinically meaningful events, 
i.e. anything that would show up on a detailed timeline of the patient’s care or life.

**Temporal expression annotators**  
A series of BIO style sequence annotators that employed forward and backward search algorithms 
and multiple learning methods (Support Vector Machine (SVM), Conditional Random Field (CRV) )
for annotating temporal expressions which would provide concrete temporal references throughout 
the document or section, e.g. “today”, “24 hours ago”, “postoperative”.
More details can be found in [6].   

**DocTimeRel annotator**  
For every event, there is an SVM-based annotator that can automatically reason the temporal relation
between the target event and the document creation time (DCT). 
This module provided a basic and stable temporal solution that can position all events into coarse
temporal bins, e.g. “before the DCT”, “after the DCT”, or “overlap the DCT”. 
This annotator has proved helpful in solving real clinical temporal-sensitive tasks for multiple 
institutions [5].

**Temporal relation (TLINK) annotators**  
SVM-based annotators for detecting within-sentence Event-Time relations and Event-Event relations.
For i2b2 datasets there are also cross sentence Event-Time and Event-Event relation annotators.
Multiple techniques have been implemented, including narrative container-based annotation
concept [7], tree kernels [8] for syntactic similarity measurement,
multi-layered temporal modeling [9], event expansion [10], and deep neural network methods [11, 12].
</details>

<details>
<summary>Evaluation</summary>
The SVM-based temporal relation annotators achieve an F-score of 0.589 which outperform the best
system of Clinical TempEval 2016 [13], whose F-score was 0.573. State-of-the-art results for
event-time relations have been achieved with our neural network approaches.
</details>

All the above annotators were trained and tested on colon cancer notes from the THYME data set [14].
<details>
<summary>References</summary>
1.	Das, A.K. and M.A. Musen. A comparison of the temporal expressiveness of three database query methods. in Annual Symposium on Computer Applications in Medical Care. 1995. IEEE COMPUTER SOCIETY PRESS.
2.	Kahn, M.G., L.M. Fagan, and S. Tu, Extensions to the time-oriented database model to support temporal reasoning in medical expert systems. Methods of information in medicine, 1990. 30(1): p. 4-14.
3.	Schmidt, R., S. Ropele, C. Enzinger, et al., White matter lesion progression, brain atrophy, and cognitive decline: the Austrian stroke prevention study. Annals of neurology, 2005. 58(4): p. 610-616.
4.	Zhou, L. and G. Hripcsak, Temporal reasoning with medical data—a review with emphasis on medical natural language processing. Journal of biomedical informatics, 2007. 40(2): p. 183-202.
5.	Lin, C., E.W. Karlson, D. Dligach, et al., Automatic identification of methotrexate-induced liver toxicity in patients with rheumatoid arthritis from the electronic medical record. Journal of the American Medical Informatics Association, 2014: p. amiajnl-2014-002642.
6.	Miller, T.A., S. Bethard, D. Dligach, et al., Extracting Time Expressions from Clinical Text, in Proceedings of BioNLP 15. 2015.
7.	Miller, T.A., S. Bethard, D. Dligach, et al., Discovering narrative containers in clinical text, in ACL 2013. 2013: Sofia, Bulgaria. p. 18.
8.	Lin, C., T. Miller, A. Kho, et al., Descending-Path Convolution Kernel for Syntactic Structures, in Proceedings of the 52nd Annual Meeting of the Association for Computational Linguistics (ACL). 2014: Baltimore, Maryland, USA. p. 81-86.
9.	Lin, C., D. Dligach, T.A. Miller, et al., Multilayered temporal modeling for the clinical domain. J Am Med Inform Assoc, 2016. 23(2): p. 387-95.
10.	Lin, C., T. Miller, D. Dligach, et al., Improving Temporal Relation Extraction with Training Instance Augmentation. BioNLP 2016, 2016: p. 108.
11.	Dligach, D., T. Miller, C. Lin, et al., Neural temporal relation extraction., in European Chapter of the Association for Computational Linguistics (EACL 2017). 2017: Valencia, Spain.
12.	Hartzell, E. and C. Lin. Enhancing Clinical Temporal Relation Discovery with Syntactic Embeddings from GloVe. in International Conference on Intelligent Biology and Medicine (ICIBM 2016). 2016. Houston, Texas, USA.
13.	Bethard, S., G. Savova, W.-T. Chen, et al., Semeval-2016 task 12: Clinical tempeval. Proceedings of SemEval, 2016: p. 1052-1062.
14.	Styler IV, W.F., S. Bethard, S. Finan, et al., Temporal annotation in the clinical domain. Transactions of the Association for Computational Linguistics, 2014. 2: p. 143-154.  
</details>