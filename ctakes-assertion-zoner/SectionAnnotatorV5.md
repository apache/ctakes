## Modifications to the ctakes-assertion-zoner

This describes the improvements made to the ctakes-assertion-zoner in versions 5.0.1 and above for detecting sections in clinical documents.  It has always been very configurable and adaptable to many document types.  But perhaps also a bit daunting because of the dense use of regular expressions in its configuration.  But it is worth the effort.

A quick look at the configuration file shows the structure and logic of a configuration.

A number of re-usable fragments are defined. E.g.

    <fragment>
    	<name>sochistory</name>
    	<expansion>(social\s+history|social\s+hx|\bshx?|soc\s*hx|habits)			</expansion>
    </fragment>

Sections are defined by original regex and by reference to one or more fragments

    <section>
    	<regex>((past\s+(?:medical\s+)?history|medical\s+history|pmhx?|pmedhx)\/(<fragment-ref name="sochistory"/>)\s*\:)</regex>
    	<label>past medical history</label>
    </section>

Subsections are also defined by composition and by reference to one or more parent section definitions

    <subsection>
    	<regex>((<fragment-ref name="subsection-left"/>)?Head\s*\:)</regex>
    	<subsection_of>physical examination,review of systems</subsection_of>
    	<label>HEENT</label>
    </subsection>

The previous versions of this software had several defects.  It was very cpu intensive, it was painful to configure and there was an occasional error due to a negative offset being calculated.   The painfulness of configuration was partially due to the fact that there was no efficient way to capture the sections that had NOT been configured in notes, so they could be added to the existing configurations.  The modifications here were made in an environment where there were millions of notes from a multitude of clinics.  

At the end of the exercise we detected over 175 different document types each of which used or partially used  document sections which often had the same meaning.   A key feature of this annotator is that by giving each section definition a tag, it was possible to equate section formats which were incompatible in terms of Regex design under a single definition.  So multiple section definitions could have the same tag 

This version of the annotator has the following new features

 - Significant improvement of performance by providing a lookup mechanism to preview each chunk of text provided to the annotator, to see which of the regex definitions might apply - rather than blindly running each of the hundreds of complex expressions against that piece of text.
 - Conditionally adding and logging the behavior of two new "Generic" section definitions.   When this is turned on, it writes out all of the "section-looking" bits of text that were not found by any of the named section definitions.
 - The addition of two fragment definition which allow for the removal of a huge amount of boiler-plate regex.
	 - `<prolog>` which defines the ways a heading start can be detected
	 - `<eoh>` end-of-header, defining the ways the heading-end can be detected,

- Note that this annotator has used an external open source code artifact from Mitre - the Mastif library.  In order to create the new behavior it was necessary to import the code into the ctakes project and make substantial changes to it.  Hence the new project 'ctakes-mastif-zoner'

## How to use the new features
### Working with Generic Document Sections
A new boolean initialization parameter "IncludeGenerics" has been added.  This should only be turned on in the piper file while developing and configuring the section definitions file, as defined by the preexisting parameter "SectionRegex"

The example file section_regex_ctakes5.xml is just the default distributed example section_regex.xml with the addition of the prolog and eoh fragments and the generic section definitions.  It is not meant to represent a complete configuration.

At the end of parsing a document, an entry for each unique section-like passage that doesn't already exist in the definition file  will be written to the logger for the class ZonerCli,   The entry is designed to closely resemble the actual entry needed in the regex definition file for "SectionRegex"  For example the log file contains this:

    19 Sep 2022 16:33:29 INFO  [ZonerCli] 
    	<!-- IRRITANTS -->
    	<section>
    		<regex><fragment-ref name="prolog"/>(?:irritants)<fragment-ref name="eoh"/></regex>
    		<label>generic</label>
    	</section>

It means that in a note, the word IRRITANTS was formatted in such a way that it was likely to signal a document section that had not been defined.

Subsections that are generics are also indicated by the label of the parent under which they were found.  These parent sections could themselves be Generics or previously defined sections.  For instance:

    19 Sep 2022 16:33:35 INFO  [ZonerCli] 
    	<!-- physical examination::NECK -->
    	<subsection>
    		<regex><fragment-ref name="prolog"/>(?:neck)<fragment-ref name="eoh"/></regex>
    		<label>generic</label>
    		<subsection_of>physical examination</subsection_of>
    	</section>

The log file can become quite large, so it is strongly advised to be activated  only during configuration of the sections definition file.   It will print a single entry of each generic it finds in each note.  Your work can be significantly reduced by 

    grep '<label>' zoner_cli_logfile | sort | uniq  -c | sort

This will give you a unique list of the 'non found sections' ordered by increasing occurrence in your note corpus.   Like this:

     469 	<!-- physical examination::CONSTITUTIONAL -->
     501 	<!-- HENT -->
     526 	<!-- MUSCULOSKELETAL -->
     558 	<!-- RESPIRATORY -->
     567 	<!-- DATE -->
     581 	<!-- EYES -->
     621 	<!-- NEUROLOGICAL -->
     665 	<!-- CONSTITUTIONAL -->
     670 	<!-- TEMP -->
     679 	<!-- CARDIOVASCULAR -->
     710 	<!-- SKIN -->
    1196 	<!-- PATIENT NAME -->
    1611 	<!-- * TYPE -->
    1941 	<!-- EDITOR -->
    1989 	<!-- FILED -->

  Obviously some of these headings are eligible for being defined in your set.  Some of them already are, but requiring a change to the regex.  Other entries, perhaps 'FILED' can just be ignored. 

### Using the \<prolog> and \<eoh> fragments
The examples in the above section also illustrate how one can remove a lot of the repetitive zoner boiler plate and allow formatting specifics to be propagated efficiently by changing the **prolog** and **eoh** definitions themselves.  

    <section>
	    <regex>(((?:discharge\s+)?activity)\s*\:)</regex>
	    <label>discharge activity</label>
    </section>
becomes

    	<section>
    		<regex><fragment-ref name="prolog"/>((?:discharge\s+)?activity)<fragment-ref name="eoh"/></regex>
    		<label>discharge activity</label>
      </section>


> Written with [StackEdit](https://stackedit.io/).
