package com.surelogic.jsure.client.eclipse.views.results;

import java.util.*;

public class DiffResultsViewContentProvider extends ResultsViewContentProvider {
  protected Object[] getElementsInternal(final Object parent) {
    long start = System.currentTimeMillis();
    try {
      return getDiffFromLastElements();
    } 
    finally {
      long end = System.currentTimeMillis();
      System.out.println("Time to do diff: "+(end-start)+" ms");
    }  
  }
  
  private static List<Content> makeList(Object[] roots) {
    List<Content> l = new ArrayList<Content>();
    for (Object o : roots) {
      Content c = (Content) o;
      l.add(c.deepCopy());
    }
    return l;
  }
  
  public Object[] getDiffFromLastElements() {
    synchronized (ResultsViewContentProvider.class) {
      if (m_lastRoot == null || m_lastRoot.length == 0) {
        //return m_root;
        return noObjects;
      }
      List<Content> now  = makeList(m_root);
      List<Content> last = makeList(m_lastRoot);
      return AbstractContent.diffChildren(last, now).toArray();
    }
  }    
}
