package org.apache.ctakes.coreference.util;

import org.apache.ctakes.temporal.utils.PatientViewsUtil;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.tcas.Annotation;

public class ClusterUtils {
  public static Annotation getMostRecent(NonEmptyFSList list, Annotation focus){
    NonEmptyFSList cur = list;
    Annotation annot = (Annotation) cur.getHead();
    
    // check if the focus annotation is before any of the list elements
    if(PatientViewsUtil.isSameDocument(annot, focus) && annot.getEnd() > focus.getEnd()) return null;
    
    while(cur.getTail() instanceof NonEmptyFSList){
      cur = (NonEmptyFSList) cur.getTail();
      if(((Annotation)cur.getHead()).getEnd() < focus.getEnd()){
        annot = (Annotation) cur.getHead();
      }else{
        break;
      }
    }

    return annot;
  }
  
  public static int getSize(NonEmptyFSList list){
    int size=1;

    NonEmptyFSList cur = list;
    while(cur.getTail() instanceof NonEmptyFSList){
      cur = (NonEmptyFSList) cur.getTail();
      size++;
    }
    return size;
  }
}
