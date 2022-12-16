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
package org.apache.ctakes.preprocessor;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;


/**
 * Given an HL7 document, the text contained inside a section is extracted.
 * 
 * @author Mayo
 */
public class ClinicalNotePreProcessor extends DefaultHandler
        implements PreProcessor
{
    // LOG4J logger based on class name
    private final Logger iv_logger = Logger.getLogger( getClass().getName() );

    // Jan 1, 1AM, 0001
    // private final long DEFAULT_DATE_MILLIS = -62135571600l;
 
    public static final String MD_KEY_PATIENT_STREET_ADDR = "PATIENT_ADDR"; 
    public static final String MD_KEY_PATIENT_CITY = "PATIENT_CITY";
    public static final String MD_KEY_PATIENT_STATE = "PATIENT_STATE";
    public static final String MD_KEY_PATIENT_ZIP = "PATIENT_ZIP";
    public static final String MD_KEY_PATIENT_CNT = "PATIENT_CNT";
    
    public static final String MD_KEY_DOC_ID = "DOC_ID";
    public static final String MD_KEY_DOC_LINK_ID = "DOC_LINK_ID";
    public static final String MD_KEY_DOC_REVISION_ID = "DOC_REVISION_ID";
    public static final String MD_KEY_NOTE_DATE = "NOTE_DATE";
    public static final String MD_KEY_REV_DATE = "REV_DATE";
    public static final String MD_KEY_ADMIT_DATE = "ADMIT_DATE";
    public static final String MD_KEY_DISCHARGE_DATE = "DISCHARGE_DATE";
    public static final String MD_KEY_SERVICE_CODE = "SERVICE_CODE";
    public static final String MD_KEY_SITE_CODE = "SITE_CODE";
    public static final String MD_KEY_FACILITY_CODE = "FACILITY_CODE";
    public static final String MD_KEY_PT_BIRTH_DATE = "PT_BIRTH_DATE";
    public static final String MD_KEY_PT_HEIGHT = "PT_HEIGHT";
    public static final String MD_KEY_PT_WEIGHT = "PT_WEIGHT";
    public static final String MD_KEY_PT_HEAD_CIRCUMFERENCE = "PT_HEAD_CIRCUMFERENCE";
    public static final String MD_KEY_PT_GENDER = "PT_GENDER";
    public static final String MD_KEY_PT_BMI = "PT_BMI";
    public static final String MD_KEY_PT_BSA = "PT_BSA";
    public static final String MD_KEY_PT_TEMPERATURE = "PT_TEMPERATURE";
    public static final String MD_KEY_PT_BILLING_CODE = "PT_BILLING_CODE";
    public static final String MD_KEY_PT_RESPIRATORY_RATE = "PT_RESPIRATORY_RATE";
    public static final String MD_KEY_EVENT_TYPE_CODE = "EVENT_TYPE_CODE";
    public static final String MD_KEY_DOC_CONFIDENTIAL_IND = "DOC_CONFIDENTIAL_IND";
    public static final String MD_KEY_DOC_STATUS_CODE = "DOC_STATUS_CODE";
    public static final String MD_KEY_HISTORY_SECTION = "HISTORY_SECTION";
    public static final String MD_KEY_LOCATION = "LOCATION";
    public static final String MD_KEY_MINUTES_COUNSELING = "MINUTES_COUNSELING";
    public static final String MD_KEY_TOTAL_TIME = "TOTAL_TIME";
    public static final String MD_KEY_PROVIDER_ID = "PROVIDER_ID";
    public static final String MD_KEY_PROVIDER2_ID = "PROVIDER2_ID";
    public static final String MD_KEY_SIGNATURE_ID = "SIGNATURE_ID";
    public static final String MD_KEY_SIGNATURE_DATE = "SIGNATURE_DATE";
    public static final String MD_KEY_TRANSCRIBER_ID = "TRANSCRIBER_ID";
    public static final String MD_KEY_TRANSCRIPTION_DATE = "TRANSCRIPTION_DATE";
    
    public static final String MD_KEY_CUSTOMER_ID = "CLINICAL_NUMBER"; 
    


    private DocumentMetaData iv_docMetaData;

    private boolean iv_insideHeader = false;
    private boolean iv_insideAdminData = false;
    private boolean iv_insideTranscriptionist = false;
    private boolean iv_insidePatient = false;
    private boolean iv_insideKnownBy = false;
    private boolean iv_insidePatientEncounter = false;
    private boolean iv_insideLegalAuth = false;
    private boolean iv_insideProvider = false;
    private boolean iv_foundProvider1 = false;
    private boolean iv_insideServiceLoc = false;
    private boolean iv_isHospitalSummary = false;

    private int iv_sectionStartOffset;
    private String iv_sectionIdentifier;
    // tracks level of nested section elements
    private int iv_sectionNestingLevel = 0;
    private boolean iv_insideSection = false;
    private boolean iv_insideCaption = false;

    public static final int UNKNOWN_TABLE_TYPE = 0;
    private boolean iv_insideTable = false;
    private boolean iv_insideTableRow = false;
    private boolean iv_insideTableHeader = false;
    private boolean iv_insideTableData = false;
    private String iv_tableHeaderKeyID = null;
    private int iv_tdStartOffset;
    private int iv_tdCounter = 0;

    private boolean iv_insideExamComponent = false;
    private int iv_examComponentTableDataCnt = 0;
    private StringBuffer iv_examComponentText = null;


    private List<String> iv_headerList = new ArrayList<>();

    private XMLReader iv_xmlParser;

    private StringBuffer iv_sectionText = new StringBuffer();
    private StringBuffer iv_text = new StringBuffer();

    private boolean iv_includeSectionMarkers;

    private String iv_previousElement = null;

    private StringBuffer iv_contiguousTextBuffer = new StringBuffer();

    /**
     * Constructor
     * 
     * @param dtdFile
     *            File object pointing to DTD.
     * 
     * @param includeSectionMarkers
     *            Flag that determines whether the section markers are included
     *            as part of the section.
     * @throws SAXException
     */
    public ClinicalNotePreProcessor(InputStream dtdFile, boolean includeSectionMarkers)
            throws SAXException, FileNotFoundException
    {
        iv_includeSectionMarkers = includeSectionMarkers;

        iv_xmlParser = XMLReaderFactory
                .createXMLReader("org.apache.xerces.parsers.SAXParser");
        iv_xmlParser.setContentHandler(this);

        EntityResolver eResolver = new DTDloader(dtdFile);
        iv_xmlParser.setEntityResolver(eResolver);
    }

    public DocumentMetaData process(String xml) throws Exception
    {
        iv_docMetaData = new DocumentMetaData();

        InputSource input = new InputSource(new StringReader(xml));
        iv_xmlParser.parse(input);

        return iv_docMetaData;
    }

    /**
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
     *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement(
            String uri,
            String localName,
            String qName,
            Attributes attributes) throws SAXException
    {
        // must be first statement of method to properly capture contiguous text
        // nodes
        if (iv_contiguousTextBuffer.length() > 0)
        {
            newTextNode();
        }

        if (!iv_insideHeader && localName.equals("clinical_document_header"))
        {
            iv_insideHeader = true;
        }
        else if (localName.equals("is_known_by"))
        {
    	    iv_insideKnownBy = true;
        }
        else if (iv_insideHeader && localName.equals("origination_dttm"))
        {
            String revDate = attributes.getValue("V");
            if (revDate.length() > 0)
            {
                try
                {
                    long timeInMillis = convertTime(revDate);
                    iv_docMetaData.addMetaData(MD_KEY_REV_DATE, String
                            .valueOf(timeInMillis));
                }
                catch (Exception e)
                {
                  iv_logger.warn(MD_KEY_REV_DATE+" invalid:"+revDate);
                }
            }
        }
        else if (localName.equals("section"))
        {
            iv_sectionNestingLevel++;

            if (!iv_insideSection)
            {
                iv_insideSection = true;
                iv_sectionText = new StringBuffer();
                iv_sectionStartOffset = iv_text.length();
            }
        }
        else if (iv_insideSection && localName.equals("caption"))
        {
            iv_insideCaption = true;
        }
        else if (iv_insideSection && localName.equals("caption_cd"))
        {
//            if (iv_insideTable && iv_previousElement.equals("caption")) {
                // processing a table, trace why type of table this is
            	// String tableType = attributes.getValue("V");
              
				//              if(tableType == null || tableType.length() == 0)
				//            	iv_tableType = UNKNOWN_TABLE_TYPE;
				//              else
				//                iv_tableType = Integer.parseInt(tableType);
//            }
            
            if (iv_sectionIdentifier == null)
            {
                iv_sectionIdentifier = attributes.getValue("V");
            }

            if (iv_insideTableHeader)
            {
                // There are cases where the cnote will embed structured
                // key-value
                // pairs inside a table row. The signal that is is the case is
                // finding
                // a caption_cd element embedded inside a table header. If we
                // run into
                // this, record the ID of the caption_cd
                iv_tableHeaderKeyID = attributes.getValue("V");
            }
        }
        else if (iv_insideSection && localName.equals("table"))
        {
            iv_insideTable = true;
        }
        else if (iv_insideTable && localName.equals("tr"))
        {
            iv_insideTableRow = true;
            iv_tdCounter = 0;
        }
        else if (iv_insideTableRow && localName.equals("th"))
        {
            iv_insideTableHeader = true;
        }
        else if (iv_insideTableRow && localName.equals("td"))
        {
            iv_insideTableData = true;
            compress(iv_sectionText);
            iv_tdStartOffset = iv_text.length() + iv_sectionText.length();
//        } else if (iv_insideTableHeader && localName.equals("activity_tmr")) {
//        } else if (iv_insideTableData && localName.equals("coded_entry.value")) {
        }
        else if (iv_insideTableData && localName.equals("value"))
        {
            // check to see if we're at a key-value pair
            if (iv_tableHeaderKeyID != null)
            {
                String value = attributes.getValue("V");

                if (iv_sectionIdentifier.equals("20114"))
                {
                    // processing for Administrative section
                    switch ( iv_tableHeaderKeyID ) {
                        case "30004":
                            // margin code
                            iv_docMetaData.addMetaData(
                                  MD_KEY_PT_BILLING_CODE,
                                  value );
                            break;
                        case "30005":
                            // total time
                            iv_docMetaData.addMetaData( MD_KEY_TOTAL_TIME, value );
                            break;
                        case "30006":
                            // counseling time
                            iv_docMetaData.addMetaData(
                                  MD_KEY_MINUTES_COUNSELING,
                                  value );
                            break;
                    }
                }
            }
        }
        else if (iv_insideHeader)
        {
            // extract data from header
            if (localName.equals("cn1_admin_data"))
            {
                iv_insideAdminData = true;
            }
            else if (localName.equals("transcriptionist"))
            {
                iv_insideTranscriptionist = true;
            }
            else if (localName.equals("patient"))
            {
              iv_insidePatient = true;
            }            
            else if (localName.equals("patient_encounter"))
            {
                iv_insidePatientEncounter = true;
            }
            else if (localName.equals("legal_authenticator"))
            {
                iv_insideLegalAuth = true;
            }
            else if (localName.equals("provider"))
            {
                iv_insideProvider = true;
            }
            else if (localName.equals("service_location"))
            {
                iv_insideServiceLoc = true;
            }
            else if (localName.equals("document_type_cd"))
            {
                if (attributes.getValue("V").equals("2025539"))
                {
                    iv_isHospitalSummary = true;
                }
            }
            else if ((iv_previousElement != null)
                    && (iv_previousElement.equals("clinical_document_header"))
                    && localName.equals("id"))
            {
                // Handle older version notes
                String docID = attributes.getValue("EX");
                if ((docID != null)
                        && (docID.length() > 0)
                        && (docID.indexOf('#') == -1))
                {
                    iv_docMetaData.addMetaData(MD_KEY_DOC_ID, docID);
                }
            }
            else if (localName.equals("set_id"))
            {
                // Handle older version notes
                String docLinkID = attributes.getValue("EX");
                if ((docLinkID != null)
                        && (docLinkID.length() > 0)
                        && (docLinkID.indexOf('#') == -1))
                {
                    iv_docMetaData.addMetaData(MD_KEY_DOC_LINK_ID, docLinkID);
                }
            }
            else if (localName.equals("version_nbr"))
            {
                // Handle older version notes
                String revisionID = attributes.getValue("V");
                if ((revisionID != null)
                        && (revisionID.length() > 0)
                        && (revisionID.indexOf('#') == -1))
                {
                    iv_docMetaData.addMetaData(
                            MD_KEY_DOC_REVISION_ID,
                            revisionID);
                }
            }
            else if (iv_insideAdminData && localName.equals("cn1_discharge_date"))
            {
                String dischargeDate = attributes.getValue("V");
                if (dischargeDate!= null && 
                    (dischargeDate.length() == 8 || 
                     dischargeDate.length() == 15))
                {
                  try
                  {
                    // only admission date entered
                    long admitTimeInMillis = convertTime(dischargeDate);
                    iv_docMetaData.addMetaData(MD_KEY_DISCHARGE_DATE, String.valueOf(admitTimeInMillis));
                  }
                  catch (Exception e)
                  {
                    iv_logger.warn(MD_KEY_DISCHARGE_DATE+" invalid:"+dischargeDate);
                  }
                }
            }
            else if (iv_insideAdminData && localName.equals("cn1_status_cd"))
            {
                String statusCode = attributes.getValue("V");
                iv_docMetaData.addMetaData(MD_KEY_DOC_STATUS_CODE, statusCode);
            }
            else if (iv_insideAdminData && localName.equals("cn1_service_cd"))
            {
                String serviceCode = attributes.getValue("V");
                iv_docMetaData.addMetaData(MD_KEY_SERVICE_CODE, serviceCode);
            }
            else if (iv_insideAdminData && localName.equals("cn1_event_cd"))
            {
                String eventCode = attributes.getValue("V");
                iv_docMetaData.addMetaData(MD_KEY_EVENT_TYPE_CODE, eventCode);
            }
            else if (iv_insideAdminData && localName.equals("cn1_document_id"))
            {
                String docID = attributes.getValue("EX");
                iv_docMetaData.addMetaData(MD_KEY_DOC_ID, docID);

                // DOC ID and DOC LINK ID are identical
                iv_docMetaData.addMetaData(MD_KEY_DOC_LINK_ID, docID);
            }
            else if (iv_insideAdminData && localName.equals("cn1_revision_nbr"))
            {
                String revisionID = attributes.getValue("V");
                iv_docMetaData.addMetaData(MD_KEY_DOC_REVISION_ID, revisionID);
            }
            else if (iv_insideAdminData && localName.equals("cn1_site_cd"))
            {
                String siteCode = attributes.getValue("V");
                iv_docMetaData.addMetaData(MD_KEY_SITE_CODE, siteCode);
            }
            else if (iv_insideTranscriptionist)
            {
                if (localName.equals("id"))
                {
                    String transcriberID = attributes.getValue("EX");
                    iv_docMetaData.addMetaData(
                            MD_KEY_TRANSCRIBER_ID,
                            transcriberID);
                }
                else if (localName.equals("participation_tmr"))
                {
                    String transcriptionDate = attributes.getValue("V");
                    if (transcriptionDate.length() > 0)
                    {
                        try
                        {
                            long timeInMillis = convertTime(transcriptionDate);
                            iv_docMetaData.addMetaData(
                                    MD_KEY_TRANSCRIPTION_DATE,
                                    String.valueOf(timeInMillis));
                        }
                        catch (Exception e)
                        {
                          iv_logger.warn(MD_KEY_TRANSCRIPTION_DATE+" invalid:"+transcriptionDate);                          
                        }
                    }
                }
            }
            else if (iv_insidePatient)
            {
              if(localName.equals("LIT")) //addr->lit, addr->cty, etc
              {
                String streetAddr = attributes.getValue("V");
                iv_docMetaData.addMetaData(MD_KEY_PATIENT_STREET_ADDR, streetAddr);
              }
              else if(localName.equals("CTY")) //addr->lit, addr->cty, etc
              {
                String city = attributes.getValue("V");
                iv_docMetaData.addMetaData(MD_KEY_PATIENT_CITY, city);
              }
              else if(localName.equals("STA")) //addr->lit, addr->cty, etc
              {
                String state = attributes.getValue("V");
                iv_docMetaData.addMetaData(MD_KEY_PATIENT_STATE, state);
              }
              else if(localName.equals("ZIP")) //addr->lit, addr->cty, etc
              {
                String zip = attributes.getValue("V");
                iv_docMetaData.addMetaData(MD_KEY_PATIENT_ZIP, zip);
              }
              else if(localName.equals("CNT")) //addr->lit, addr->cty, etc
              {
                String cnt = attributes.getValue("V");
                iv_docMetaData.addMetaData(MD_KEY_PATIENT_CNT, cnt);
              }
              else if (localName.equals("administrative_gender_cd"))
              {
                  String ptGender = attributes.getValue("V");
                  iv_docMetaData.addMetaData(MD_KEY_PT_GENDER, ptGender);
              }
              else if (localName.equals("birth_dttm"))
              {
                  String birthDttm = attributes.getValue("V");
                  try
                  {
                      long birthTimeInMillis = convertTime(birthDttm);
                      iv_docMetaData.addMetaData(MD_KEY_PT_BIRTH_DATE, String
                              .valueOf(birthTimeInMillis));
                  }
                  catch (Exception e)
                  {
                    iv_logger.warn(MD_KEY_PT_BIRTH_DATE+" invalid:"+birthDttm);
                  }
              }
              else if (iv_insideKnownBy)
              {
              	if (localName.equals("id") && (iv_previousElement.compareTo("is_known_by") == 0)){
                  // SPM add clinical id retrieval
              		String clinicalNumber = attributes.getValue("EX");
              		iv_docMetaData.addMetaData(MD_KEY_CUSTOMER_ID, clinicalNumber.replaceAll("-", ""));
              	}
              }              
            }
            else if (iv_insideLegalAuth)
            {
                if (localName.equals("id"))
                {
                    String signatureID = attributes.getValue("EX");
                    iv_docMetaData
                            .addMetaData(MD_KEY_SIGNATURE_ID, signatureID);
                }
                else if (localName.equals("participation_tmr"))
                {
                    String signatureDate = attributes.getValue("V");
                    if (signatureDate.length() > 0)
                    {
                        try
                        {
                            long timeInMillis = convertTime(signatureDate);
                            iv_docMetaData.addMetaData(
                                    MD_KEY_SIGNATURE_DATE,
                                    String.valueOf(timeInMillis));
                        }
                        catch (Exception e)
                        {
                          iv_logger.warn(MD_KEY_SIGNATURE_DATE+" invalid:"+signatureDate);
                        }
                    }
                }
            }
            else if (iv_insideProvider)
            {
                if (localName.equals("id"))
                {
                    if ( !iv_foundProvider1 )
                    {
                        String providerID = attributes.getValue("EX");
                        iv_docMetaData.addMetaData(
                                MD_KEY_PROVIDER_ID,
                                providerID);
                        iv_foundProvider1 = true;
                    }
                    else
                    {
                        String provider2ID = attributes.getValue("EX");
                        iv_docMetaData.addMetaData(
                                MD_KEY_PROVIDER2_ID,
                                provider2ID);
                    }
                }
            }
            else if (iv_insidePatientEncounter)
            {
                if (localName.equals("organization.nm"))
                {
                    String facilityCode = attributes.getValue("V");
                    iv_docMetaData.addMetaData(
                            MD_KEY_FACILITY_CODE,
                            facilityCode);
                }
                else if (localName.equals("encounter_tmr"))
                {
                    String noteDate = attributes.getValue("V");
                    if (noteDate.length() > 0)
                    {
                        try
                        {
                            if (iv_isHospitalSummary)
                            {
                              /*
                               * Note: 07/23/07
                               * Formats that we need to handel:YYYYMMDDTHHMMSS, YYYYMMDD or YYYYMMDD<separator>YYYYMMDD
                               */
                                if (noteDate.length() == 8 || noteDate.length() == 15)
                                {
                                    // only admission date entered
                                    long admitTimeInMillis = convertTime(noteDate);
                                    iv_docMetaData.addMetaData(
                                            MD_KEY_ADMIT_DATE,
                                            String.valueOf(admitTimeInMillis));
                                    iv_docMetaData.addMetaData(
                                            MD_KEY_NOTE_DATE,
                                            String.valueOf(admitTimeInMillis));
                                }
                                else if (noteDate.length() == 17)
                                {
                                    // admission date and dismissal date
                                    // entered separated by semi colon
                                    String admitDate = noteDate.substring(0, 8);
                                    String dischargeDate = noteDate.substring(
                                            9,
                                            17);
                                    long admitTimeInMillis = convertTime(admitDate);
                                    long dischargeTimeInMillis = convertTime(dischargeDate);
                                    iv_docMetaData.addMetaData(
                                            MD_KEY_ADMIT_DATE,
                                            String.valueOf(admitTimeInMillis));
                                    iv_docMetaData
                                            .addMetaData(
                                                    MD_KEY_DISCHARGE_DATE,
                                                    String
                                                            .valueOf(dischargeTimeInMillis));
                                    iv_docMetaData.addMetaData(
                                            MD_KEY_NOTE_DATE,
                                            String.valueOf(admitTimeInMillis));
                                }
                            }
                            else
                            {
                                long timeInMillis = convertTime(noteDate);
                                iv_docMetaData.addMetaData(
                                        MD_KEY_NOTE_DATE,
                                        String.valueOf(timeInMillis));
                            }
                        }
                        catch (Exception e)
                        {
                            throw new SAXException(e);
                        }
                    }
                }
            }
            else if (iv_insideServiceLoc && localName.equals("id"))
            {
                String location = attributes.getValue("EX");
                iv_docMetaData.addMetaData(MD_KEY_LOCATION, location);
            }
            else if (localName.equals("confidentiality_cd"))
            {
                String confidentialCode = attributes.getValue("V");
                iv_docMetaData.addMetaData(
                        MD_KEY_DOC_CONFIDENTIAL_IND,
                        confidentialCode);
            }
            else if (localName.equals("administrative_gender_cd"))
            {
                String ptGender = attributes.getValue("V");
                iv_docMetaData.addMetaData(MD_KEY_PT_GENDER, ptGender);
            }
            else if (localName.equals("birth_dttm"))
            {
                String birthDttm = attributes.getValue("V");
                try
                {
                    long birthTimeInMillis = convertTime(birthDttm);
                    iv_docMetaData.addMetaData(MD_KEY_PT_BIRTH_DATE, String
                            .valueOf(birthTimeInMillis));
                }
                catch (Exception e)
                {
                  iv_logger.warn(MD_KEY_PT_BIRTH_DATE+" invalid:"+birthDttm);
                }
            }
        }

        iv_previousElement = localName;
    } 
    
    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    public void characters(char[] ch, int start, int length)
            throws SAXException
    {
        if ((length > 0) && !iv_insideTableHeader)
        {
            iv_contiguousTextBuffer.append(ch, start, length);
        }
    }

    /**
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    public void endElement(String uri, String localName, String qName)
            throws SAXException
    { // must be first statement of method to properly capture contiguous text nodes
        if (iv_contiguousTextBuffer.length() > 0)
        {
            newTextNode();
        }

        if (iv_insideHeader)
        {
            if (localName.equals("clinical_document_header"))
            {
                iv_insideHeader = false;
            }
            else if (iv_insideAdminData && localName.equals("cn1_admin_data"))
            {
                iv_insideAdminData = false;
            }
            else if (iv_insideTranscriptionist
                    && localName.equals("transcriptionist"))
            {
                iv_insideTranscriptionist = false;
            }
            else if (iv_insidePatientEncounter
                    && localName.equals("patient_encounter"))
            {
                iv_insidePatientEncounter = false;
            }
            else if (iv_insideLegalAuth
                    && localName.equals("legal_authenticator"))
            {
                iv_insideLegalAuth = false;
            }
            else if (localName.equals("provider"))
            {
                iv_insideProvider = false;
            }
            else if (localName.equals("service_location"))
            {
                iv_insideServiceLoc = false;
            }
        }
        else if (localName.equals("paragraph"))
        {
            iv_sectionText.append('\n');
        }
        else if (iv_insideSection && localName.equals("section"))
        {
            iv_sectionNestingLevel--;

            if (iv_sectionNestingLevel == 0)               
            {
                if (iv_sectionText.toString().trim().length() > 0)
                {
                    String sectionStartMarker = getSectionStartMarker(iv_sectionIdentifier);
                    iv_text.append(sectionStartMarker);
                    iv_text.append('\n');
                    iv_text.append('\n');
                    if (!iv_includeSectionMarkers)
                    {
                        // skip past the section marker text
                        iv_sectionStartOffset = iv_text.length();
                    }

                    iv_text.append(compress(iv_sectionText));
                    SegmentMetaData smd = new SegmentMetaData();
                    IntegerRange span = new IntegerRange();
                    span.start = iv_sectionStartOffset;
                    span.end = iv_text.length();
                    smd.span = span;
                    smd.id = iv_sectionIdentifier;
                    iv_docMetaData.addSegment(smd);
                    String sectionEndMarker = getSectionEndMarker(iv_sectionIdentifier);
                    iv_text.append('\n');
                    iv_text.append(sectionEndMarker);
                    iv_text.append('\n');
                    iv_text.append('\n');                    
                }

                iv_insideSection = false;
                iv_sectionIdentifier = null;
                iv_sectionText = null;
            }
        }
        else if (iv_insideCaption && localName.equals("caption"))
        {
            iv_insideCaption = false;
        }
        else if (iv_insideTable && localName.equals("table"))
        {

            // iv_tableType = UNKNOWN_TABLE_TYPE;
            iv_tdCounter = 0;
            iv_headerList.clear();
            iv_insideTable = false;
        }
        else if (iv_insideTableRow && localName.equals("tr"))
        {
            iv_examComponentText = null;
            iv_insideExamComponent = false;
            iv_examComponentTableDataCnt = 0;
            iv_insideTableRow = false;
            iv_tableHeaderKeyID = null;
            iv_sectionText.append('\n');
        }
        else if (iv_insideTableRow && localName.equals("th"))
        {
            iv_insideTableHeader = false;
        }
        else if (iv_insideTableRow && localName.equals("td"))
        {
            if (iv_tdCounter < iv_headerList.size())
            {
                String thText = iv_headerList.get(iv_tdCounter);
                Annotation a = new Annotation();
                a.iv_type = thText;
                a.startOffset = iv_tdStartOffset;
                compress(iv_sectionText);
                a.endOffset = iv_text.length() + iv_sectionText.length();
                iv_docMetaData.addAnnotation(a);
            }
            iv_insideTableData = false;
            iv_sectionText.append(' ');
            iv_tdCounter++;
        }
        else if (iv_insideTableRow && localName.equals("br"))
        {
            // there are line breaks in the clinical note
            // inject a newline character for each <br/>
            iv_sectionText.append('\n');
        }
    }

    /**
     * @see org.xml.sax.ContentHandler#startDocument()
     */
    public void startDocument() throws SAXException
    {
        iv_insideTable = false;
        iv_insideTableRow = false;
        iv_insideTableHeader = false;
        iv_insideTableData = false;
        iv_tdCounter = 0;
        iv_headerList.clear();
        iv_insideHeader = false;
        iv_insideAdminData = false;
        iv_insideTranscriptionist = false;
        iv_insidePatientEncounter = false;
        iv_insideLegalAuth = false;
        iv_insideProvider = false;
        iv_insideServiceLoc = false;
        iv_tableHeaderKeyID = null;
        iv_examComponentText = null;
        iv_insideExamComponent = false;
        iv_isHospitalSummary = false;
        iv_previousElement = null;
    }

    /**
     * @see org.xml.sax.ContentHandler#endDocument()
     */
    public void endDocument() throws SAXException
    {
        // replace non ASCII chars to avoid NSE problem
        replaceNonAsciiChars(iv_text, ' ');
        iv_docMetaData.setText(iv_text.toString());

        if (iv_logger.isDebugEnabled())
        {
            String docID = iv_docMetaData.getMetaData().get(
                    MD_KEY_DOC_ID);
            iv_logger.debug("Finished processing document id=" + docID);
        }
    }

    /**
     * Called when a fully contiguous text node has been found.
     */
    private void newTextNode()
    {
        String text = iv_contiguousTextBuffer.toString().trim();
        if (iv_insideTableHeader)
        {
            iv_headerList.add(text);
            iv_sectionText.append(text);
        }
        else if (iv_insideExamComponent)
        {
          if (iv_examComponentTableDataCnt == 1)
            {
                iv_examComponentText.append(text);
            }
            iv_examComponentTableDataCnt++;
        }
        else if (iv_insideSection && !iv_insideCaption)
        {
            iv_sectionText.append(text);
        }

//        if (iv_insideTableData && (iv_tableHeaderKeyID != null))  {
            // Do nothing	
//        }
        // reset the buffer to zero to start accumulating afresh
        iv_contiguousTextBuffer.setLength(0);
    }

    private String getSectionStartMarker(String id)  {
        return "[start section id=\"" + id + "\"]";
    }

    private String getSectionEndMarker(String id) {
        return "[end section id=\"" + id + "\"]";
    }

    private String compress(StringBuffer sb)
    {
        StringBuilder compressedSB = new StringBuilder();
        if (sb == null)
        {
            return compressedSB.toString();
        }

        int indexOfLastNewline = 0;
        for (int i = 0; i < sb.length(); i++)
        {
            char currentChar = sb.charAt(i);
            if (currentChar == '\n')
            {
                if ((i - indexOfLastNewline) > 1)
                {
                    String lineText = sb.substring(indexOfLastNewline, i);
                    String compressedText = lineText.trim();
                    if (compressedText.length() > 0)
                    {
                        compressedSB.append(compressedText);
                        compressedSB.append('\n');
                    }
                }
                indexOfLastNewline = i;
            }
        }
        if (indexOfLastNewline < sb.length())
        {
            String lineText = sb.substring(indexOfLastNewline, sb.length());
            String compressedText = lineText.trim();
            compressedSB.append(compressedText);
        }

        return compressedSB.toString();
    }

    /**
     * Given a drm string that may or may not contain the time, this method will
     * return a string.
     * 
     * @param drmStr -
     * @return If something goes wrong, 0 is returned. Otherwise the time in
     *         milliseconds is returned.
     */
    private long convertTime(String drmStr) throws Exception
    {
        // DRM FORMAT: YYYYMMDDThhmmss
        // first 4 digits = year
        // next 2 digits = month
        // next 2 digits = day
        // followed by time with prefix T
        // next 2 digits = hour (military)
        // next 2 digits = minutes
        // next 2 digits = seconds
        int tIndex = drmStr.indexOf('T');
        String dateStr = null;
        String timeStr = null;
        if (tIndex != -1)
        {
            dateStr = drmStr.substring(0, tIndex);
            timeStr = drmStr.substring(tIndex + 1 );
        }
        else
        {
            dateStr = drmStr;
        }

        try
        {
            if (dateStr.length() == 8)
            {
                int year = Integer.parseInt(dateStr.substring(0, 4));
                int month = Integer.parseInt(dateStr.substring(4, 6));
                int day = Integer.parseInt(dateStr.substring(6, 8));
                Calendar c = new GregorianCalendar();
                c.clear();
                int hours = 0;
                int minutes = 0;
                int seconds = 0;
                if (timeStr != null)
                { // process time
                    if (timeStr.length() >= 4)
                    {
                        hours = Integer.parseInt(timeStr.substring(0, 2));
                        minutes = Integer.parseInt(timeStr.substring(2, 4));
                    }
                    if (timeStr.length() == 6)
                    {
                        // seconds may be optional
                        seconds = Integer.parseInt(timeStr.substring(4, 6));
                    }
                } // months are zero-based, so subtract one from the current
                // month value when creating the Calendar
                c.set(year, month - 1, day, hours, minutes, seconds);
                return c.getTime().getTime();
            }
            else
            {
                throw new Exception();
            }
        }
        catch (Exception e)
        {
            throw new Exception("Invalid DRM. date="
                    + dateStr
                    + " time="
                    + timeStr);
        }
    }

    /**
     * Replaces any non-ascii characters with the specified char.
     * 
     * @param sb -
     */
    private void replaceNonAsciiChars(StringBuffer sb, char replacementChar)
    {
        for (int i = 0; i < sb.length(); i++)
        {
            char c = sb.charAt(i);
            // Unicode range 0000-007f Basic Latin
            // equivalent to ASCII charset
            if (c > 0x007f)
            {
                // character is outside ASCII range of unicode char set
                sb.setCharAt(i, replacementChar);
            }
        }
    }
}