package edu.cmu.cs.fluid.pattern;

import java.util.*;
import edu.cmu.cs.fluid.util.IntegerTable;

@SuppressWarnings("all")
class SetMap
{
  private TreeMap setm;
  private TreeMap isetm;

  public SetMap(TreeSet ts)
  {
    setm = new TreeMap(Comps.st);
    isetm = new TreeMap();
   
    int c = -1;
    Object elem;
    Integer CInt;
    Iterator it = ts.iterator();
    while (it.hasNext())
    {
      elem = it.next();
      CInt = IntegerTable.newInteger(++c);
      setm.put(elem, CInt);
      isetm.put(CInt, elem);
    }
  }

  public TreeMap getsetmap()
  {
    return setm;
  }

  public TreeMap getisetmap()
  {
    return isetm;
  }
}