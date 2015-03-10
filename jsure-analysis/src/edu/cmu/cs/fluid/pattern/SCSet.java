package edu.cmu.cs.fluid.pattern;

import java.util.*;

@SuppressWarnings("all")
class SCSet
{
  private TreeSet scs;
  private Comparator scsc;

  public SCSet(TreeSet ts, Comparator ec)
  {
    scsc = ec;
    scs = new TreeSet(scsc);

    Iterator it = ts.iterator();
    ArrayList al;
    while (it.hasNext())
    {
      al = new ArrayList();
      al.add(0, it.next());
      scs.add(al);
    }
  }

  public void incnext(TreeSet ts)
  {
    TreeSet tscs = new TreeSet(scsc);
    ArrayList al;
    ArrayList tal;
    Iterator scsit = scs.iterator();
    while (scsit.hasNext())
    {
      al = (ArrayList)scsit.next();
      Iterator tsit = ts.iterator();
      while (tsit.hasNext())
      {
        tal = new ArrayList(al);
        tal.add(tal.size(), tsit.next());
        tscs.add(tal);
      }
      al.clear();
    }
    scs.clear();
    scs.addAll(tscs);
    tscs.clear();
  }
  
  public TreeSet getscset()
  {
    return scs;
  }
}