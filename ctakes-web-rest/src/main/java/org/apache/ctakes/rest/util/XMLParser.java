package org.apache.ctakes.rest.util;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


public class XMLParser {

    Map<String, String> polarityMap = new HashMap<>();

    public Map<String, Map<String, List<String>>> parse(InputStream in) throws Exception {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLStreamReader streamReader = inputFactory.createXMLStreamReader(in);
        String analysisText = null;
        Map<String, String> ontologyArrayMap = new HashMap<>();
        Map<String, List> umlsConceptMap = new HashMap<>();
        List<String> disorderList = new ArrayList<>();
        List<String> findingsList = new ArrayList<>();
        List<String> procedureList = new ArrayList<>();
        List<String> timeList = new ArrayList<>();
        List<String> fractionStrengthList = new ArrayList<>();
        List<String> drugChangeStatusList = new ArrayList<>();
        List<String> strengthUnitList = new ArrayList<>();
        List<String> strengthList = new ArrayList<>();
        List<String> routeList = new ArrayList<>();
        List<String> frequencyUnitList = new ArrayList<>();
        List<String> measurementList = new ArrayList<>();
        List<String> dateList = new ArrayList<>();
        List<String> anatomicalList = new ArrayList<>();
        List<String> medicalList = new ArrayList<>();

        Map<String, String> disorderPosMap = new HashMap<>();
        Map<String, String> findingsPosMap = new HashMap<>();
        Map<String, String> procedurePosMap = new HashMap<>();
        Map<String, String> timePosMap = new HashMap<>();
        Map<String, String> fractionStrengthPosMap = new HashMap<>();
        Map<String, String> drugChangeStatusPosMap = new HashMap<>();
        Map<String, String> strengthUnitPosMap = new HashMap<>();
        Map<String, String> strengthPosMap = new HashMap<>();
        Map<String, String> routePosMap = new HashMap<>();
        Map<String, String> datePosMap = new HashMap<>();
        Map<String, String> frequencyPosMap = new HashMap<>();
        Map<String, String> measurementPosMap = new HashMap<>();
        Map<String, String> anatomicalPosMap = new HashMap<>();
        Map<String, String> medicalPosMap = new HashMap<>();


        Map<String, List<String>> disorderDetailMap = new HashMap<>();
        Map<String, List<String>> findingsDetailMap = new HashMap<>();
        Map<String, List<String>> procedureDetailMap = new HashMap<>();
        Map<String, List<String>> timeDetailMap = new HashMap<>();
        Map<String, List<String>> fractionStrengthDetailMap = new HashMap<>();
        Map<String, List<String>> drugChangeStatusDetailMap = new HashMap<>();
        Map<String, List<String>> strengthUnitDetaillMap = new HashMap<>();
        Map<String, List<String>> strengthDetailMap = new HashMap<>();
        Map<String, List<String>> routeDetailMap = new HashMap<>();
        Map<String, List<String>> frequencyUnitDetailMap = new HashMap<>();
        Map<String, List<String>> measurementDetailMap = new HashMap<>();
        Map<String, List<String>> dateDetailMap = new HashMap<>();
        Map<String, List<String>> anatomicalDetailMap = new HashMap<>();
        Map<String, List<String>> medicalDetailMap = new HashMap<>();

        while (streamReader.hasNext()) {
            if (streamReader.isStartElement()) {
                if (analysisText == null) {
                    if (streamReader.getLocalName().equalsIgnoreCase("Sofa")) {
                        analysisText = streamReader.getAttributeValue(null,"sofaString");
                    }
                }
                try {
                    disorderList = extractData(streamReader, analysisText, SemanticNames.DiseaseDisorderMention.name(), disorderPosMap, disorderList, ontologyArrayMap);
                    findingsList = extractData(streamReader, analysisText, SemanticNames.SignSymptomMention.name(), findingsPosMap, findingsList, ontologyArrayMap);
                    procedureList = extractData(streamReader, analysisText, SemanticNames.ProcedureMention.name(), procedurePosMap, procedureList, ontologyArrayMap);
                    timeList = extractData(streamReader, analysisText, SemanticNames.TimeMention.name(), timePosMap, timeList, ontologyArrayMap);
                    fractionStrengthList = extractData(streamReader, analysisText, SemanticNames.FractionStrengthAnnotation.name(), fractionStrengthPosMap, fractionStrengthList, ontologyArrayMap);
                    drugChangeStatusList = extractData(streamReader, analysisText, SemanticNames.DrugChangeStatusAnnotation.name(), drugChangeStatusPosMap, drugChangeStatusList, ontologyArrayMap);
                    strengthUnitList = extractData(streamReader, analysisText, SemanticNames.StrengthUnitAnnotation.name(), strengthUnitPosMap, strengthUnitList, ontologyArrayMap);
                    strengthList = extractData(streamReader, analysisText, SemanticNames.StrengthAnnotation.name(), strengthPosMap, strengthList, ontologyArrayMap);
                    routeList = extractData(streamReader, analysisText, SemanticNames.RouteAnnotation.name(), routePosMap, routeList, ontologyArrayMap);
                    frequencyUnitList = extractData(streamReader, analysisText, SemanticNames.FrequencyUnitAnnotation.name(), frequencyPosMap, frequencyUnitList, ontologyArrayMap);
                    dateList = extractData(streamReader, analysisText, SemanticNames.DateAnnotation.name(), datePosMap, dateList, ontologyArrayMap);
                    measurementList = extractData(streamReader, analysisText, SemanticNames.MeasurementAnnotation.name(), measurementPosMap, measurementList, ontologyArrayMap);
                    anatomicalList = extractData(streamReader, analysisText, SemanticNames.AnatomicalSiteMention.name(), anatomicalPosMap, anatomicalList, ontologyArrayMap);
                    medicalList = extractData(streamReader, analysisText, SemanticNames.MedicationMention.name(), medicalPosMap, medicalList, ontologyArrayMap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (streamReader.getLocalName().equalsIgnoreCase("UmlsConcept")) {
                    String id = streamReader.getAttributeValue(null, "id");
                    List<String> umlsDetailList = new ArrayList<>();
                    umlsDetailList.add("codingScheme: " + streamReader.getAttributeValue(null, "codingScheme"));
                    umlsDetailList.add("code: " + streamReader.getAttributeValue(null, "code"));
                    umlsDetailList.add("cui: " + streamReader.getAttributeValue(null, "cui"));
                    umlsDetailList.add("tui: " + streamReader.getAttributeValue(null, "tui"));
                    umlsConceptMap.put(id, umlsDetailList);
                }
            }
            streamReader.next();
        }

        disorderDetailMap = processUMLSDetail(ontologyArrayMap, umlsConceptMap, disorderDetailMap, disorderList, disorderPosMap);
        findingsDetailMap = processUMLSDetail(ontologyArrayMap, umlsConceptMap, findingsDetailMap, findingsList, findingsPosMap);
        procedureDetailMap = processUMLSDetail(ontologyArrayMap, umlsConceptMap, procedureDetailMap, procedureList, procedurePosMap);
        timeDetailMap = processUMLSDetail(ontologyArrayMap, umlsConceptMap, timeDetailMap, timeList, timePosMap);
        fractionStrengthDetailMap = processUMLSDetail(ontologyArrayMap, umlsConceptMap, fractionStrengthDetailMap, fractionStrengthList, fractionStrengthPosMap);
        drugChangeStatusDetailMap = processUMLSDetail(ontologyArrayMap, umlsConceptMap, drugChangeStatusDetailMap, drugChangeStatusList, drugChangeStatusPosMap);
        strengthUnitDetaillMap = processUMLSDetail(ontologyArrayMap, umlsConceptMap, strengthUnitDetaillMap, strengthUnitList, strengthUnitPosMap);
        strengthDetailMap = processUMLSDetail(ontologyArrayMap, umlsConceptMap, strengthDetailMap, strengthList, strengthPosMap);
        routeDetailMap = processUMLSDetail(ontologyArrayMap, umlsConceptMap, routeDetailMap, routeList, routePosMap);
        frequencyUnitDetailMap = processUMLSDetail(ontologyArrayMap, umlsConceptMap, frequencyUnitDetailMap, frequencyUnitList, frequencyPosMap);
        measurementDetailMap = processUMLSDetail(ontologyArrayMap, umlsConceptMap, measurementDetailMap, measurementList, measurementPosMap);
        dateDetailMap = processUMLSDetail(ontologyArrayMap, umlsConceptMap, dateDetailMap, dateList, datePosMap);
        anatomicalDetailMap = processUMLSDetail(ontologyArrayMap, umlsConceptMap, anatomicalDetailMap, anatomicalList, anatomicalPosMap);
        medicalDetailMap = processUMLSDetail(ontologyArrayMap, umlsConceptMap, medicalDetailMap, medicalList, medicalPosMap);

        Map<String, Map<String, List<String>>> responseMap = new HashMap<>();
        responseMap.put(SemanticNames.DiseaseDisorderMention.name(), disorderDetailMap);
        responseMap.put(SemanticNames.SignSymptomMention.name(),findingsDetailMap);
        responseMap.put(SemanticNames.ProcedureMention.name(),procedureDetailMap);
        responseMap.put(SemanticNames.AnatomicalSiteMention.name(),anatomicalDetailMap);
        responseMap.put(SemanticNames.MedicationMention.name(),medicalDetailMap);
        responseMap.put(SemanticNames.TimeMention.name(),timeDetailMap);
        responseMap.put(SemanticNames.FractionStrengthAnnotation.name(),fractionStrengthDetailMap);
        responseMap.put(SemanticNames.DrugChangeStatusAnnotation.name(),drugChangeStatusDetailMap);
        responseMap.put(SemanticNames.StrengthUnitAnnotation.name(),strengthUnitDetaillMap);
        responseMap.put(SemanticNames.StrengthAnnotation.name(),strengthDetailMap);
        responseMap.put(SemanticNames.RouteAnnotation.name(),routeDetailMap);
        responseMap.put(SemanticNames.FrequencyUnitAnnotation.name(),frequencyUnitDetailMap);
        responseMap.put(SemanticNames.DateAnnotation.name(),dateDetailMap);
        responseMap.put(SemanticNames.MeasurementAnnotation.name(),measurementDetailMap);
        return responseMap;
    }

    private Map<String, List<String>> processUMLSDetail(Map<String, String> ontologyArrayMap, Map<String, List> umlsConceptMap, Map<String, List<String>> semanticDetailsMap,
                                                List<String> sematicList, Map<String, String> semanticPosMap) {
        for (String semanticName : sematicList) {
            List<String> semanticDetailsList = semanticDetailsMap.get(semanticName);
            String[] posDetail = semanticPosMap.get(semanticName).split(",");
            if(semanticDetailsList == null) {
                semanticDetailsList = new ArrayList<>();
            }
            semanticDetailsList.add("start: " + posDetail[0]);
            semanticDetailsList.add("end: " + posDetail[1]);
            if(polarityMap.get(semanticName) != null) {
                semanticDetailsList.add("polarity: " + polarityMap.get(semanticName));
            }
            String ontologyArrayRawString = ontologyArrayMap.get(semanticName);
            if(ontologyArrayRawString != null) {
                String[] ontologyStringArr = ontologyArrayRawString.split("\\s");
                for (String ontologyString : ontologyStringArr) {
                    List umlsDetList = umlsConceptMap.get(ontologyString);
                    if (umlsDetList != null && umlsDetList.size() > 0) {
                        if (!umlsDetList.contains(semanticDetailsList)) {
                            semanticDetailsList.add(umlsDetList.toString());
                        }
                    }
                    semanticDetailsMap.put(semanticName, semanticDetailsList);
                }
            }
            else {
                semanticDetailsMap.put(semanticName, semanticDetailsList);
            }
        }
        return semanticDetailsMap;
    }

    private List<String> extractData(XMLStreamReader streamReader, String analysisText, String mentionName, Map<String, String> semanticPosMap,
                                     List<String> semanticList, Map<String, String> ontologyArrayMap) {
        if (streamReader.getLocalName().equalsIgnoreCase(mentionName)) {
            Integer start = Integer.parseInt(streamReader.getAttributeValue(null, "begin"));
            Integer end = Integer.parseInt(streamReader.getAttributeValue(null, "end"));
            String ontologyConceptArr = streamReader.getAttributeValue(null, "ontologyConceptArr");
            String polarity = streamReader.getAttributeValue(null, "polarity");
            String chunk = analysisText.substring(start, end);
            String chunkUpper = chunk.toUpperCase();
            boolean isFound = false;
            if (!semanticList.contains(chunkUpper)) {
                String chunkArray[] = chunk.split("\\s");
                for (String chunkedString : chunkArray) {
                    final String trimmedChunkString = chunkedString.trim();
                    if (!trimmedChunkString.equals("")) {
                        Object[] matchedSemanticArray = semanticList.stream().filter(str -> str.trim().contains(trimmedChunkString.toUpperCase())).toArray();
                        if (matchedSemanticArray.length > 0) {
                            for (Object semanticObj : matchedSemanticArray) {
                                String semanticTerm = semanticObj.toString();
                                String pos = semanticPosMap.get(semanticTerm);
                                if (pos != null) {
                                    String posArr[] = pos.split(",");
                                    int startPos = Integer.parseInt(posArr[0]);
                                    int endPos = Integer.parseInt(posArr[1]);
                                    if (start >= startPos && end <= endPos) {
                                        isFound = true;
                                        break;
                                    }
                                    if (start <= startPos && end == endPos) {
                                        semanticList.remove(semanticTerm);
                                        semanticList.add(chunkUpper);
                                        ontologyArrayMap.put(chunkUpper, ontologyConceptArr);
                                        semanticPosMap.remove(semanticTerm);
                                        semanticPosMap.put(chunkUpper, start + "," + end);
                                        isFound = true;
                                        if(polarity!=null) {
                                            polarityMap.put(chunkUpper,polarity);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                if (!isFound) {
                    semanticList.add(chunkUpper);
                    if(polarity!=null) {
                        polarityMap.put(chunkUpper,polarity);
                    }
                    ontologyArrayMap.put(chunkUpper, ontologyConceptArr);
                    //System.out.println("mentionName -> " + mentionName + " part -> " + chunkUpper + " -- " + start + "," + end);
                    semanticPosMap.put(chunkUpper, start + "," + end);
                }

            }
        }
        return semanticList;
    }

    public enum SemanticNames {
        DiseaseDisorderMention, SignSymptomMention, ProcedureMention, TimeMention,
        AnatomicalSiteMention, FractionStrengthAnnotation, DrugChangeStatusAnnotation,
        StrengthUnitAnnotation, StrengthAnnotation, RouteAnnotation, FrequencyUnitAnnotation, DateAnnotation,
        MeasurementAnnotation, MedicationMention
    }
}
