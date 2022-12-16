package org.apache.ctakes.core.util;

import java.util.Iterator;

import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.NonEmptyFSList;

public class ListIterable<T> implements Iterable<T> {

  FSListIterator<T> iter = null;
  
  public ListIterable(FSList list){
    iter = new FSListIterator<>(list);
  }
  
  public Iterator<T> iterator() {
    return iter;
  }

  
}

class FSListIterator<T> implements Iterator<T> {
  FSList list;
  public FSListIterator(FSList list){
    this.list = list;
  }
  
  public boolean hasNext() {
    return (list instanceof NonEmptyFSList);
  }
  
  public T next() {
    T element = (T) ((NonEmptyFSList)list).getHead();
    list = ((NonEmptyFSList)list).getTail();
    return element;
  }
  
  public void remove() {
    // don't do anything
  }
}
