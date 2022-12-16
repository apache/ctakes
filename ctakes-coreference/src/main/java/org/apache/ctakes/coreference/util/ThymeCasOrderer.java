package org.apache.ctakes.coreference.util;

import org.apache.ctakes.core.patient.PatientViewUtil;
import org.apache.ctakes.coreference.ae.ThymeAnaforaCrossDocCorefXmlReader;
import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.JCas;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by tmill on 3/22/18.
 */
public class ThymeCasOrderer implements Comparator<JCas> {
    private static Pattern fnPatt = Pattern.compile("ID(\\d+)_([^_]+)_(\\d+)");
    static ThymeCasOrderer sorter = new ThymeCasOrderer();

    // TODO: Maybe this should just be done when we read them in?
    public static List<JCas> getOrderedCases(JCas jCas) {
        List<JCas> cases = new ArrayList<>();

        Collection<JCas> allViews = PatientViewUtil.getAllViews(jCas);
        for(JCas jcas : allViews){
            // contains the default CAS name but isn't _exactly_ the default CAS name (that would be the main patient cas)
            if(jcas.getViewName().contains(CAS.NAME_DEFAULT_SOFA) &&
                    jcas.getViewName().length() > CAS.NAME_DEFAULT_SOFA.length()){
                cases.add(jcas);
            }
        }
        // TODO: Resort this based on last item of name (e.g. ID001_clinic_003 use 003 as its index)
        Collections.sort(cases, sorter);
        return cases;
    }

    @Override
    public int compare(JCas cas0, JCas cas1) {
        String v0 = cas0.getViewName();
        String v1 = cas1.getViewName();
        Matcher m = fnPatt.matcher(v0);
        int doc0Id = -1, doc1Id = -1;
        if(m.find()){
            doc0Id = Integer.parseInt(m.group(3));
        }
        m = fnPatt.matcher(v1);
        if(m.find()){
            doc1Id = Integer.parseInt(m.group(3));
        }

        return doc0Id - doc1Id;
    }
}
