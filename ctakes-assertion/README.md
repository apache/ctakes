The assertion annotators provide a mechanism for examining and documenting the real-world implications for annotations in text.  
For example, one might assume that a mention of "diabetes" in text implies that patient has diabetes.
However, the assertion module will consider whether a named entity or event is negated, uncertain,
used in a generic way, or in the context of a person's history.
Additionally, the subject of the statement may be someone other than the patient
(e.g., the patient's father, for "father has hx of diabetes").
Each of these attributes illustrate how the "assertion" value of a named entity might be marked.  
The annotators set the value of their attribute using constants in
```org.apache.ctakes.typesystem.type.constants.CONST```
