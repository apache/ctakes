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
package org.apache.ctakes.core.cc;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.JCasUtil;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;


/**
 * Outputs an HTML table that visualizes the specified Annotation objects over
 * the document text.
 * 
 * @author Mayo Clinic
 * 
 */
@PipeBitInfo(
      name = "HTML Table Writer",
      description = "Writes HTML files with a Table representation of extracted information.",
      role = PipeBitInfo.Role.WRITER,
      dependencies = { PipeBitInfo.TypeProduct.BASE_TOKEN }
)
public class HtmlTableCasConsumer extends CasConsumer_ImplBase
{
    private File iv_outputDir;

    private int iv_tableSpanType;

    private int[] iv_nestedAnnTypeArr;

    // key = annotation type (java.lang.Integer)
    // val = getter method (java.lang.reflect.Method)
    private Map<Integer, Method> iv_getterMethMap = new HashMap<Integer, Method>();

    private int iv_count;

    private String[] iv_tdStyleArr = { "I", "B" };

   @Override
    public void initialize() throws ResourceInitializationException
    {
        try
        {
            iv_outputDir = new File(
                    (String) getConfigParameterValue("OutputDir"));

            String classname;
            classname = (String) getConfigParameterValue("TableSpanAnnotation");
            iv_tableSpanType = JCasUtil.getType(classname);

            String[] strArr = (String[]) getConfigParameterValue("NestedAnnotations");
            iv_nestedAnnTypeArr = new int[strArr.length];
            for (int i = 0; i < strArr.length; i++)
            {
                StringTokenizer st = new StringTokenizer(strArr[i], "|");
                classname = st.nextToken().trim();
                iv_nestedAnnTypeArr[i] = JCasUtil.getType(classname);

                // if there's an extra token, it must be a getter methodname
                if (st.countTokens() == 1)
                {
                    String methName = st.nextToken().trim();
                    Class<?> c = Class.forName(classname);
                    Method meth = c.getMethod(methName, (Class[]) null);
                    iv_getterMethMap.put(new Integer(iv_nestedAnnTypeArr[i]),
                            meth);
                }
            }

        } catch (Exception e)
        {
            throw new ResourceInitializationException(e);
        }
    }

   @Override
   public void processCas( CAS cas ) throws ResourceProcessException
    {
        try
        {
            JCas jcas = cas.getJCas();
            StringBuffer htmlSB = new StringBuffer();
            htmlSB.append("<HTML>");
            htmlSB.append("<TITLE>?</TITLE>");
            htmlSB.append("<BODY>");

            Iterator<Annotation> tSpanItr = jcas.getJFSIndexRepository()
                    .getAnnotationIndex(iv_tableSpanType).iterator();
            while (tSpanItr.hasNext())
            {
                Annotation tSpanAnn = tSpanItr.next();
                String tSpanText = tSpanAnn.getCoveredText();

                htmlSB.append("<TABLE border=1>");
                htmlSB.append("<TR bordercolor=\"white\">");
                for (int i = 0; i < tSpanText.length(); i++)
                {
                    htmlSB.append("<TD width=10>");
                    htmlSB.append(tSpanText.charAt(i));
                    htmlSB.append("</TD>");
                }
                htmlSB.append("</TR>");

                int tdStyleIdx = 0;
                for (int nestIdx = 0; nestIdx < iv_nestedAnnTypeArr.length; nestIdx++)
                {
                    List<Annotation> nestedAnnList = getAnnotations(jcas,
                            iv_nestedAnnTypeArr[nestIdx], tSpanAnn.getBegin(),
                            tSpanAnn.getEnd());

                    // sort nested annotation list
                    Collections.sort(nestedAnnList,
                            new AnnotationLengthComparator());

                    List<List<Annotation>> annotsAtRowList = arrangeIntoRows(tSpanAnn,
                            nestedAnnList);

                    Iterator<List<Annotation>> trAnnItr = annotsAtRowList.iterator();
                    while (trAnnItr.hasNext())
                    {
                        htmlSB.append("<TR>");
                        int cursor = tSpanAnn.getBegin();
                        List<Annotation> annList = trAnnItr.next();

                        // sort annotations in this row by offset position
                        Collections.sort(annList,
                                new AnnotationPositionComparator());

                        Iterator<Annotation> annItr = annList.iterator();
                        while (annItr.hasNext())
                        {
                           Annotation ann = annItr.next();
                            // account for preceeding whitespace
                            int delta = ann.getBegin() - cursor;
                            if (delta > 0)
                            {
                                htmlSB.append("<TD width=10 colspan=" + delta
                                        + ">");
                                String whitespaceStr = "";
                                for (int i = 0; i < delta; i++)
                                {
                                    whitespaceStr += ' ';
                                }
                                htmlSB.append(whitespaceStr);
                                htmlSB.append("</TD>");
                            }
                            cursor = ann.getEnd();

                            htmlSB
                                    .append("<TD width=10 align=\"center\" colspan="
                                            + ann.getCoveredText().length()
                                            + ">");
                            htmlSB.append("<");
                            htmlSB.append(iv_tdStyleArr[tdStyleIdx]);
                            htmlSB.append(">");
                            htmlSB.append(getDisplayValue(
                                    iv_nestedAnnTypeArr[nestIdx], ann));
                            htmlSB.append("</");
                            htmlSB.append(iv_tdStyleArr[tdStyleIdx]);
                            htmlSB.append(">");
                            htmlSB.append("</TD>");
                        }
                        htmlSB.append("</TR>");
                    }

                    tdStyleIdx++;
                    if (tdStyleIdx == iv_tdStyleArr.length)
                    {
                        tdStyleIdx = 0;
                    }
                }
                htmlSB.append("</BR>");
                htmlSB.append("</BR>");

                htmlSB.append("</TABLE>");
            }

            htmlSB.append("</BODY>");
            htmlSB.append("</HTML>");

            File f = new File(iv_outputDir.getAbsolutePath() + File.separator
                    + "doc" + iv_count + ".html");
            f.createNewFile();
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            bw.write(htmlSB.toString());
            bw.close();

        } catch (Exception e)
        {
            throw new ResourceProcessException(e);
        }
        iv_count++;
    }

    /**
     * Gets a value to be displayed in table cell for the given annotation
     * object.
     * 
     * @param annType
     * @param ann
     * @return
     */
    private String getDisplayValue(int annType, Annotation ann)
            throws IllegalAccessException, InvocationTargetException
    {
        Integer key = new Integer(annType);
        if (iv_getterMethMap.containsKey(key))
        {
           Method meth = iv_getterMethMap.get( key );
            Object val = meth.invoke(ann, (Object[]) null);
            if (val != null)
            {
                return String.valueOf(val);
            } else
            {
                // otherwise return empty string
                return "";
            }
        } else
        {
            String typeName = ann.getType().getShortName();
            return typeName.substring(0, typeName.indexOf("Annotation"));
        }
    }

    /**
     * Arranges the list of annotations into one or more rows. Each element of
     * the return List represents a row. Each row is represented as a row of
     * Annotation objects that below to that row.
     * 
     * @param tSpanAnn
     * @param nestedAnnList
     * @return
     */
    private List<List<Annotation>> arrangeIntoRows(Annotation tSpanAnn, List<Annotation> nestedAnnList)
    {
        int tSpanSize = tSpanAnn.getCoveredText().length();
        List<BitSet> maskAtRowList = new ArrayList<BitSet>();
        maskAtRowList.add(new BitSet(tSpanSize));

        List<List<Annotation>> annotsAtRowList = new ArrayList<List<Annotation>>();

        // divide parse annotations into rows
        while (nestedAnnList.size() != 0)
        {
            // pop annotation off
           Annotation ann = nestedAnnList.remove( 0 );

            BitSet annBitSet = new BitSet(tSpanSize);
            annBitSet.set(ann.getBegin() - tSpanAnn.getBegin(), ann.getEnd()
                    - tSpanAnn.getBegin());

            // figure out which TR to place it in
            int idx = 0;
            boolean rowFound = false;
            while (!rowFound)
            {
               BitSet trBitSet = maskAtRowList.get( idx );

                // interset BitSets to determine if annotation will fit
                // in this row
                while (trBitSet.intersects(annBitSet))
                {
                    idx++;
                    if ((idx + 1) > maskAtRowList.size())
                    {
                        trBitSet = new BitSet(tSpanSize);
                        maskAtRowList.add(trBitSet);
                    } else
                    {
                       trBitSet = maskAtRowList.get( idx );
                    }
                }
                trBitSet.or(annBitSet);
                rowFound = true;
            }

            List<Annotation> annList = null;
            if ((idx + 1) > annotsAtRowList.size())
            {
                annList = new ArrayList<Annotation>();
                annList.add(ann);
                annotsAtRowList.add(annList);
            } else
            {
                annList = annotsAtRowList.get(idx);
                annList.add(ann);
            }
        }
        return annotsAtRowList;
    }

    /**
     * Comparator for comparing two Annotation objects based on span length.
     * 
     * @author Mayo Clinic
     * 
     */
    class AnnotationLengthComparator implements Comparator<Annotation>
    {
        /*
         * (non-Javadoc)
         * 
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        @Override
        public int compare( Annotation a1, Annotation a2 )
        {
            Integer len1 = new Integer(a1.getCoveredText().length());
            Integer len2 = new Integer(a2.getCoveredText().length());

            if (len1.equals(len2))
            {
                if (a1.getBegin() < a2.getBegin())
                    return -1;
                else if (a1.getBegin() > a2.getBegin())
                    return 1;
                else
                {
                    if (a1.getEnd() < a2.getEnd())
                        return 1;
                    else if (a1.getEnd() > a2.getEnd())
                        return -1;
                    else
                        return 0;
                }
            } else
            {
                return len1.compareTo(len2);
            }
        }
    }

    /**
     * Comparator for comparing two Annotation objects based on offset position.
     * 
     * @author Mayo Clinic
     * 
     */
    class AnnotationPositionComparator implements Comparator<Annotation>
    {
        /*
         * (non-Javadoc)
         * 
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        @Override
        public int compare( Annotation a1, Annotation a2 )
        {
            if (a1.getBegin() < a2.getBegin())
                return -1;
            else if (a1.getBegin() > a2.getBegin())
                return 1;
            else
            {
                if (a1.getEnd() < a2.getEnd())
                    return 1;
                else if (a1.getEnd() > a2.getEnd())
                    return -1;
                else
                    return 0;
            }
        }
    }

    private List<Annotation> getAnnotations(JCas jcas, int annType, int begin, int end)
    {
        List<Annotation> list = new ArrayList<Annotation>();
        Iterator<Annotation> itr = jcas.getJFSIndexRepository().getAnnotationIndex(annType)
                .iterator();
        while (itr.hasNext())
        {
           Annotation ann = itr.next();
            if ((ann.getBegin() >= begin) && (ann.getEnd() <= end))
            {
                list.add(ann);
            }
        }
        return list;
    }
}
