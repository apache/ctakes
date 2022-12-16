/*
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
package org.apache.ctakes.core.cc;

/*
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

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.log4j.Logger;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.fit.component.CasConsumer_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.UriUtils;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLSerializer;
import org.cleartk.util.ViewUriUtil;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * A simple CAS consumer that writes the CAS to XMI format.
 * <p/>
 * This CAS Consumer takes one parameter:
 * <ul>
 * <li><code>OutputDirectory</code> - path to directory into which output files will be written</li>
 * </ul>
 * @deprecated Please use FileTreeXmiWriter.
 * FileTreeXmiWriter can use a specific output subdirectory, patient name (directories), and write to a full output tree.
 */
@PipeBitInfo(
      name = "XMI Writer",
      description = "Writes XMI files with full representation of input text and all extracted information.",
      role = PipeBitInfo.Role.WRITER,
      dependencies = { PipeBitInfo.TypeProduct.DOCUMENT_ID }
)
@Deprecated
public class XmiWriterCasConsumerCtakes extends CasConsumer_ImplBase {
	/**
	 * Name of configuration parameter that must be set to the path of a directory into which the
	 * output files will be written.
	 */
	public static final String PARAM_OUTPUTDIR = "OutputDirectory";
	@ConfigurationParameter(name = PARAM_OUTPUTDIR, description = "Output directory to write xmi files", mandatory = true)
	private File mOutputDir;

	private int mDocNum;

	@Override
	public void initialize( UimaContext context ) throws ResourceInitializationException {
		super.initialize( context );
		Logger.getLogger( getClass().getSimpleName() ).warn( "Deprecated.  Please use FileTreeXmiWriter instead." );
		mDocNum = 0;
		if ( !mOutputDir.exists() ) {
			mOutputDir.mkdirs();
		}
	}

	/**
	 * Processes the CAS which was populated by the TextAnalysisEngines. <br>
	 * In this case, the CAS is converted to XMI and written into the output file .
	 *
	 * @param aCAS a CAS which has been populated by the TAEs
	 * @throws AnalysisEngineProcessException
	 * @see org.apache.uima.collection.base_cpm.CasObjectProcessor#processCas(org.apache.uima.cas.CAS)
	 */
	@Override
	public void process( CAS aCAS ) throws AnalysisEngineProcessException {
		String modelFileName = null;

		JCas jcas;
		try {
			jcas = aCAS.getJCas();
		} catch ( CASException e1 ) {
			e1.printStackTrace();
			throw new AnalysisEngineProcessException( e1 );
		}

      String originalFileName = DocIdUtil.getDocumentID( jcas );
		File outFile = null;
		if ( originalFileName != null
           && !originalFileName.isEmpty()
           && !originalFileName.equals( DocIdUtil.NO_DOCUMENT_ID ) ) {
			File inFile;
			try {
				String outFileName = null;
				if ( originalFileName.contains( "/" ) ) {
					URI uri = UriUtils.quote( originalFileName );
					inFile = new File( uri );
					outFileName = inFile.getName();
				} else {
					outFileName = originalFileName;
				}
				outFileName += ".xmi";
				outFile = new File( mOutputDir, outFileName );

			} catch ( URISyntaxException e ) {
				// bad URI, use default processing below
			}

		}
		if ( outFile == null ) {
			originalFileName = ViewUriUtil.getURI(jcas).toString();
			File inFile;
			String outFileName = null;
			if ( originalFileName != null
              && !originalFileName.isEmpty()
              && !originalFileName.equals( DocIdUtil.NO_DOCUMENT_ID ) ) {
				try {
					if ( originalFileName.contains( "/" ) ) {
						URI uri = UriUtils.quote( originalFileName );
						inFile = new File( uri );
						outFileName = inFile.getName();
					} else {
						outFileName = originalFileName;
					}
					outFileName += ".xmi";
					outFile = new File( mOutputDir, outFileName );
				} catch ( URISyntaxException e ) {
					// bad URI, use default processing below
				}
			}
		}

		if ( outFile == null){
			outFile = new File( mOutputDir, "doc" + mDocNum++ + ".xmi" ); // Jira UIMA-629
		}
		// serialize XCAS and write to output file
		try {
			writeXmi( jcas.getCas(), outFile, modelFileName );
		} catch ( IOException e ) {
			throw new AnalysisEngineProcessException( e );
		} catch ( SAXException e ) {
			throw new AnalysisEngineProcessException( e );
		}
	}

	/**
	 * Serialize a CAS to a file in XMI format
	 *
	 * @param aCas CAS to serialize
	 * @param name output file
	 * @throws SAXException
	 * @throws Exception
	 * @throws ResourceProcessException
	 */
	private void writeXmi( CAS aCas, File name ) throws IOException, SAXException {
		FileOutputStream out = null;

		try {
			// write XMI
			out = new FileOutputStream( name );
			XmiCasSerializer ser = new XmiCasSerializer( aCas.getTypeSystem() );
			XMLSerializer xmlSer = new XMLSerializer( out, false );
			ser.serialize( aCas, xmlSer.getContentHandler() );
		} finally {
			if ( out != null ) {
				out.close();
			}
		}
	}

	/**
	 * Serialize a CAS to a file in XMI format
	 *
	 * @param aCas CAS to serialize
	 * @param name output file
	 * @throws SAXException
	 * @throws Exception
	 * @throws ResourceProcessException
	 */
	private void writeXmi( CAS aCas, File name, String modelFileName ) throws IOException, SAXException {
		FileOutputStream out = null;

		try {
			// write XMI
			out = new FileOutputStream( name );
			XmiCasSerializer ser = new XmiCasSerializer( aCas.getTypeSystem() );
			XMLSerializer xmlSer = new XMLSerializer( out, false );
			ser.serialize( aCas, xmlSer.getContentHandler() );
		} finally {
			if ( out != null ) {
				out.close();
			}
		}
	}

	/**
	 * Parses and returns the descriptor for this collection reader. The descriptor is stored in the
	 * uima.jar file and located using the ClassLoader.
	 *
	 * @return an object containing all of the information parsed from the descriptor.
	 * @throws InvalidXMLException if the descriptor is invalid or missing
	 */
	public static CasConsumerDescription getDescription() throws InvalidXMLException {
		InputStream descStream = XmiWriterCasConsumerCtakes.class
				.getResourceAsStream( "XmiWriterCasConsumerCtakes.xml" );
		return UIMAFramework.getXMLParser().parseCasConsumerDescription(
				new XMLInputSource( descStream, null ) );
	}

	public static URL getDescriptorURL() {
		return XmiWriterCasConsumerCtakes.class.getResource( "XmiWriterCasConsumerCtakes.xml" );
	}
}
