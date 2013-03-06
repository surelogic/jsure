package edu.cmu.cs.fluid.pattern;

import java.util.*;
import edu.cmu.cs.fluid.tree.Operator;

class Comps
{
  public static final Comparator<Operator> op = new OPComp();
  public static final Comparator<TreeSet<Integer>> st = new STComp();
  public static final Comparator<List<Integer>> vt = new VTComp();
  public static final Comparator<TreeSet<TreeSet<Integer>>> sst = new SSTComp();
  public static final Comparator<TreeSet<List<Integer>>> svt = new SVTComp();
  public static final Comparator<List<TreeSet<Integer>>> vst = new VSTComp();
  public static final Comparator<List<TreeSet<TreeSet<Integer>>>> vsst = new VSSTComp();
  public static final Comparator mvsst = new MVSSTComp();
}

class OPComp implements Comparator<Operator>
{
  @Override
  public int compare(Operator x, Operator y)
  {
    return (x).name().compareTo((y).name());
  }
}

class STComp implements Comparator<TreeSet<Integer>>
{
  @Override
  public int compare(TreeSet<Integer> xs, TreeSet<Integer> ys)
  {
    if (xs.size() < ys.size())
      return -1;
    else if (xs.size() > ys.size())
      return 1;
    else 
    {
      int cmp;
      Iterator<Integer> it1 = xs.iterator();
      Iterator<Integer> it2 = ys.iterator();
      while ((it1.hasNext()) && (it2.hasNext()))
      {
        cmp = (it1.next()).compareTo(it2.next());
        if (cmp != 0)
          return cmp;
      }
    }
    return 0;
  }
}

class VTComp implements Comparator<List<Integer>>
{
  @Override
  public int compare(List<Integer> xv, List<Integer> yv)
  {
    if (xv.size() < yv.size())
      return -1;
    else if (xv.size() > yv.size())
      return 1;
    else 
    {
      int cmp;
      for (int c=0; c<xv.size(); c++)
      {
        cmp = (xv.get(c)).compareTo(yv.get(c));
        if (cmp != 0)
          return cmp;
      }
    }
    return 0;
  }
}

class SSTComp implements Comparator<TreeSet<TreeSet<Integer>>>
{
  @Override
  public int compare(TreeSet<TreeSet<Integer>> xs, TreeSet<TreeSet<Integer>> ys)
  {
    if (xs.size() < ys.size())
      return -1;
    else if (xs.size() > ys.size())
      return 1;
    else
    {
      Comparator<TreeSet<Integer>> stc = Comps.st;
      Iterator<TreeSet<Integer>> xit = xs.iterator();
      Iterator<TreeSet<Integer>> yit = ys.iterator();
      int cmp;
      while (xit.hasNext() && yit.hasNext())
      {
        cmp = stc.compare(xit.next(), yit.next());
        if (cmp != 0)
          return cmp;
      }
    }
    return 0;
  }
}

class SVTComp implements Comparator<TreeSet<List<Integer>>>
{
  @Override
  public int compare(TreeSet<List<Integer>> xs, TreeSet<List<Integer>> ys)
  {
    Comparator<List<Integer>> vtc = Comps.vt;
    if (xs.size() < ys.size())
      return -1;
    else if (xs.size() > ys.size())
      return 1;
    else 
    {
      int cmp;
      Iterator<List<Integer>> xsit = xs.iterator();
      Iterator<List<Integer>> ysit = ys.iterator();
      while ((xsit.hasNext()) && (ysit.hasNext()))
      {
        cmp = vtc.compare(xsit.next(), ysit.next());
        if (cmp != 0)
          return cmp;
      }
    }
    return 0;
  }
}

class VSTComp implements Comparator<List<TreeSet<Integer>>>
{
  @Override
  public int compare(List<TreeSet<Integer>> xv, List<TreeSet<Integer>> yv)
  {
    Comparator<TreeSet<Integer>> stc = Comps.st;
    if (xv.size() < yv.size())
      return -1;
    else if (xv.size() > yv.size())
      return 1;
    else 
    {
      int cmp;
      for (int c=0; c<xv.size(); c++)
      {
        cmp = stc.compare(xv.get(c), yv.get(c));
        if (cmp != 0)
          return cmp;
      }
    }
    return 0;
  }
}

class VSSTComp implements Comparator<List<TreeSet<TreeSet<Integer>>>>
{
  @Override
  public int compare(List<TreeSet<TreeSet<Integer>>> xv, List<TreeSet<TreeSet<Integer>>> yv)
  {
    if (xv.size() < yv.size())
      return -1;
    else if (xv.size() > yv.size())
      return 1;
    else
    {
      Comparator<TreeSet<TreeSet<Integer>>> sstc = Comps.sst;
      int cmp;
      for (int c=0; c<xv.size(); c++)
      {
        cmp = sstc.compare(xv.get(c), yv.get(c));
        if (cmp != 0)
          return cmp;
      }
    }
    return 0;
  }
}

class MVSSTComp implements Comparator<Map<String,List<TreeSet<TreeSet<Integer>>>>>
{
  @Override
  public int compare(Map<String,List<TreeSet<TreeSet<Integer>>>> xm, 
                     Map<String,List<TreeSet<TreeSet<Integer>>>> ym)
  {
    if (xm.size() < ym.size())
      return -1;
    else if (xm.size() > ym.size())
      return 1;
    else
    {
      Comparator<List<TreeSet<TreeSet<Integer>>>> vsstc = Comps.vsst;
      Set<Map.Entry<String,List<TreeSet<TreeSet<Integer>>>>> xms = xm.entrySet();
      Set<Map.Entry<String,List<TreeSet<TreeSet<Integer>>>>> yms = ym.entrySet();
      Iterator<Map.Entry<String,List<TreeSet<TreeSet<Integer>>>>> xit = xms.iterator();
      Iterator<Map.Entry<String,List<TreeSet<TreeSet<Integer>>>>> yit = yms.iterator();
      Map.Entry<String,List<TreeSet<TreeSet<Integer>>>> xme;
      Map.Entry<String,List<TreeSet<TreeSet<Integer>>>> yme;
      int cmp;
      while (xit.hasNext() && yit.hasNext())
      {
        xme = xit.next();
        yme = yit.next();
        cmp = (xme.getKey()).compareTo(yme.getKey());
        if (cmp != 0)
          return cmp;
        cmp = vsstc.compare(xme.getValue(), yme.getValue());
        if (cmp != 0)
          return cmp;
      }
    }
    return 0;
  }
}