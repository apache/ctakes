/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package data.chunk

/**
 * Output to stdout the list of part of speech (pos) tags and chunk tags contained 
 * in a file, where the file format is assumed to be an OpenNLP chunk file.
 */
class TagReport {

  static void main(args) {
		println "usage: groovy TagReport.groovy data/chunk/genia/genia.opennlp.chunks"
		def input = new File(args[0])

		def tags = new HashSet()
		def chunkTags = new HashSet()

        input.eachLine{ line ->
        	line = line.trim()
			if(line == "")
				return
			def columns = line.split("\\s+")
	  		tags.add(columns[1])
	  		def chunkTag = columns[2]
        	if(chunkTag.startsWith("B-") || 
			   chunkTag.startsWith("I-"))
        		chunkTag = chunkTag[2..-1]
        	chunkTags.add(chunkTag)
		  }
		  
		  println "list of pos tags: "
		  for(tag in tags)
			  println tag

	      println "\n\n\nlist of chunk tags: "
	      for(tag in chunkTags)
	    	  println tag
    
  }

}

