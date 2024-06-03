A Python Bridge to Java (PBJ). 
<details>
<summary>Problem Statement</summary>
Solutions start with identifying the problem. 
Our problem is the lack of a standardized path to move information from cTAKES to a python program (and back again).
Having that ability is very important as most modern Machine Learning is done in Python.
</details>

<details>
<summary>Solution</summary>
The information that we want to move is stored in an object called a CAS (Common Analysis System). 
All objects within the CAS are of a Type defined in an extensible Type System. 
For instance a discovered instance of "cancer" is stored in the CAS as an object of Type "DiseaseOrderMention".  

The next step was for us to choose a method of delivery for our path of information.
We were looking for something that could handle multiple sub-pipelines, allow for parallel sub-pipelines, 
and a method that is fast, reusable, and easy to use.  

[Apache ActiveMQ](https://activemq.apache.org/components/artemis/) Message Broker combined with
[dkpro-cassis](https://github.com/dkpro/dkpro-cassis) became apparent as the ideal solution to our problem, 
allowing what we hoped for above and more.
</details>

<details>
<summary>How it Works</summary>

![](images/step_1.png)

![](images/step_2.png)

![](images/step_3.png)

![](images/step_4.png)
</details>

<details>
<summary>Other Configurations</summary>

![](images/other_configs.png)
</details>
