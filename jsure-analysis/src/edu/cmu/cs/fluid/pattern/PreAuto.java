package edu.cmu.cs.fluid.pattern;

import java.util.*;
import edu.cmu.cs.fluid.util.IntegerTable;

@SuppressWarnings("all")
class PreAuto
{
  private TreeMap opmum;
  private TreeMap opomegam;

  public PreAuto(TreeMap opsm, TreeMap opfm, TreeMap opcpm, USet us, 
                 TreeMap rsm)
  {
    OpSMMap opsmmap = new OpSMMap(opsm);
    TreeMap opsmm = opsmmap.getopsmmap();
    TreeMap opismm = opsmmap.getopismmap();

    OpMuMap opmumap = new OpMuMap(opfm, opsmm, rsm);
    opmum = opmumap.getopmumap();

    OpOmegaMap opomegamap = new OpOmegaMap(opismm, opcpm, us, rsm);
    opomegam = opomegamap.getopomegamap();
  }

  public TreeMap getopmumap()
  {
    return opmum;
  }

  public TreeMap getopomegamap()
  {
    return opomegam;
  }
}

@SuppressWarnings("all")
class OpSMMap
{
  private TreeMap opsmm;
  private TreeMap opismm;

  public OpSMMap(TreeMap tm)
  {
    opsmm = new TreeMap();
    opismm = new TreeMap();

    Set ms = tm.entrySet();
    Iterator it = ms.iterator();
    Map.Entry me;
    ArrayList al;
    ArrayList tal;
    ArrayList ital;
    SetMap sm;
    while (it.hasNext())
    {
      me = (Map.Entry)it.next();
      al = (ArrayList)me.getValue();
      tal = new ArrayList();
      ital = new ArrayList();
      for (int c=0; c<al.size(); c++)
      {
        sm = new SetMap((TreeSet)al.get(c));
        tal.add(c, sm.getsetmap());
        ital.add(c, sm.getisetmap());
      }
      opsmm.put(me.getKey(), tal);
      opismm.put(me.getKey(), ital);
    }
  }

  public TreeMap getopsmmap()
  {
    return opsmm;
  }

  public TreeMap getopismmap()
  {
    return opismm;
  }
}

@SuppressWarnings("all")
class OpMuMap
{
  private TreeMap opmum;

  public OpMuMap(TreeMap opfm, TreeMap opsmm, TreeMap rsm)
  {
    opmum = new TreeMap();

    Set ms = opsmm.entrySet();
    Iterator it = ms.iterator();
    Map.Entry me;
    ArrayList al;
    ArrayList tal;
    MuList ml;
    while (it.hasNext())
    {
      me = (Map.Entry)it.next();
      al = (ArrayList)me.getValue();
      tal = new ArrayList();
      for (int c=0; c<al.size(); c++)
      {
        ml = new MuList((TreeSet)((ArrayList)opfm.get(me.getKey())).get(c), 
                        (TreeMap)al.get(c), rsm);
        tal.add(c, ml.getmulist());
      }
      opmum.put(me.getKey(), tal);
    }
  }

  public TreeMap getopmumap()
  {
    return opmum;
  }

  class MuList
  {
    private ArrayList mul;

    public MuList(TreeSet opfs, TreeMap osm, TreeMap rsm)
    {
      mul = new ArrayList();

      Set ms = rsm.entrySet();
      Iterator it = ms.iterator();
      Map.Entry me;
      TreeSet tts;
      while (it.hasNext())
      {
        me = (Map.Entry)it.next();
        tts = new TreeSet();
        tts.addAll((TreeSet)me.getKey());
        tts.retainAll(opfs);
        mul.add(((Integer)me.getValue()).intValue(),
                osm.get(tts));
      }
    }

    public ArrayList getmulist() 
    {
      return mul;
    }
  } 
}

@SuppressWarnings("all")
class OpOmegaMap
{
  private TreeMap opomegam;

  public OpOmegaMap(TreeMap opismm, TreeMap opcpm, USet us, TreeMap rsm)
  {
    opomegam = new TreeMap();

    Set ms = opismm.entrySet();
    Iterator mit = ms.iterator();
    Map.Entry me;
    TreeMap cpm;
    ANDArray anda;
    int[] cord;
    TreeSet ts;
    Iterator it;
    TreeSet tts = new TreeSet();
    ArrayList cv;
    while (mit.hasNext())
    {
      me = (Map.Entry)mit.next();
      cpm = (TreeMap)opcpm.get(me.getKey());
      anda = getArray((ArrayList)me.getValue());
      anda.resetIt();
      while (anda.hasNext())
      {
        cord = anda.next();
        ts = stv(cord, (ArrayList)me.getValue());
        it = ts.iterator();
        tts.clear();
        while (it.hasNext())
        {
          cv = (ArrayList)it.next();
          if (cpm.containsKey(cv))
            tts.add(cpm.get(cv));
        }
        if (us.containswc())
          tts.add(us.getwc());
        anda.set(cord, (Integer)rsm.get(tts));
      }
      opomegam.put(me.getKey(), anda);
    }
  }

  private ANDArray getArray(ArrayList al)
  {
    ArrayList dims = new ArrayList(al.size());
   
    for (int c=0; c<al.size(); c++)
      dims.add(c, IntegerTable.newInteger(((TreeMap)al.get(c)).size()));
    ANDArray anda = new ANDArray(dims);

   return anda;
  }

  private TreeSet stv(int[] cord, ArrayList ismv)
  {
    ArrayList al = new ArrayList();
    for (int c=0; c<cord.length; c++)
      al.add(c, ((TreeMap)ismv.get(c)).get(IntegerTable.newInteger(cord[c])));
    
    SCSet scs = new SCSet((TreeSet)al.get(0), Comps.vt);
    for (int c=1; c<al.size(); c++)
      scs.incnext((TreeSet)al.get(c));

    return scs.getscset();
  }

  public TreeMap getopomegamap()
  {
    return opomegam;
  }
}