package edu.cmu.cs.fluid.pattern;

import java.util.*;
import edu.cmu.cs.fluid.util.IntegerTable;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.tree.*;

@SuppressWarnings("all")
class PrePros
{
  private TreeSet ops;
  private TreeSet aps;
  private TreeMap a2c;
  private TreeMap c2a;
  private USet us;
  private TreeMap oppm;
  private TreeMap opcpm;
  private TreeMap opsm;
  private TreeSet rs;

  public PrePros(TreeSet ps, SyntaxTree st)
  {
    PFSet pfset = new PFSet(ps, st);
    TreeSet pfs = pfset.getpfset();
    ops = pfset.getopset();
    TreeSet lfs = pfset.getlfset();

    APFSet apfset = new APFSet(ps, pfs, st);
    aps = apfset.getapset();
    c2a = apfset.getc2amap();
    a2c = apfset.geta2cmap();
    us = apfset.getuset();

    R0Set r0set = new R0Set(lfs, c2a, us);
    TreeSet r0s = r0set.getr0set();

    OpPMap oppmap = new OpPMap(pfs, ops, c2a, st);
    oppm = oppmap.getoppmap();
    opcpm = oppmap.getopcpmap();

    OpSMap opsmap = new OpSMap(oppm, r0s);
    TreeMap opsme = opsmap.getopsmap();
    TreeMap opsmo = null;

    OpSCMap opscmap = null;
    TreeMap opscm = null;
    
    OpMSPMap opmspmap = null;
    TreeMap opmspm = null;

    RiSet riset = null;
    TreeSet rise = r0s;
    TreeSet riso = null;

    int it = 0;
    Comparator mvsstc = Comps.mvsst;
    do
    {
      if (it == 0)
        opscmap = new OpSCMap(opsme);
      else
        opscmap = new OpSCMap(opsmo);
      opscm = opscmap.getopscmap();
    
      opmspmap = new OpMSPMap(opscm);
      opmspm = opmspmap.getopmspmap();

      if (it == 0)
      {
        riset = new RiSet(opmspm, opcpm, us, rise);
        riso = riset.getriset();
      }
      else
      {
        riset = new RiSet(opmspm, opcpm, us, riso);
        rise = riset.getriset();
      }

      it = (it + 1) % 2;
      if (it == 0)
      {
        opsmap = new OpSMap(oppm, rise);
        opsme = opsmap.getopsmap();
      }
      else
      {
        opsmap = new OpSMap(oppm, riso);
        opsmo = opsmap.getopsmap();
      }
    } while (mvsstc.compare(opsme, opsmo) != 0);
    if (it == 0)
    {
      rs = rise;
      opsm = opsme;
    }
    else
    {
      rs = riso;
      opsm = opsmo;
    }
  }

  public TreeSet getopset()
  {
    return ops;
  }

  public TreeSet getapset()
  {
    return aps;
  }

  public TreeMap getc2amap()
  {
    return c2a;
  }

  public TreeMap geta2cmap()
  {
    return a2c;
  }

  public USet getuset()
  {
    return us;
  }

  public TreeMap getoppmap()
  {
    return oppm;
  }

  public TreeMap getopcpmap()
  {
    return opcpm;
  }

  public TreeMap getopsmap()
  {
    return opsm;
  }

  public TreeSet getrset()
  {
    return rs;
  }
}

@SuppressWarnings("all")
class PFSet
{
  private TreeSet pfs;
  private TreeSet ops;
  private TreeSet lfs;

  public PFSet(TreeSet ps, SyntaxTree st)
  {
    CTComp ctc = new CTComp(st);
    pfs = new TreeSet(ctc);
    ops = new TreeSet(Comps.op);
    lfs = new TreeSet(ctc);
    ArrayList tmp = new ArrayList();

    int pat = -1;
    Iterator it = ps.iterator();
    while (it.hasNext())
    {
      tmp.add(++pat, new ArrayList());
      ((ArrayList)tmp.get(pat)).add(0, it.next());
    }

    ArrayList al;
    IRNode ir;
    Operator op;
    for (pat=0; pat<tmp.size(); pat++)
    {
      int loc = 0;
      int feloc = 1;
      al = (ArrayList)tmp.get(pat);
      while (loc < feloc)
      {
        ir = (IRNode)al.get(loc++);
        op = st.getOperator(ir);
        ops.add(op);
        if (op.numChildren() > 0)
          for (int c=0; c<op.numChildren(); c++)
            al.add(feloc++, st.getChild(ir , c));
        else
          lfs.add(ir);
      }
      pfs.addAll(al);
    }
  }

  public TreeSet getpfset()
  {
    return pfs;
  }

  public TreeSet getopset()
  {
    return ops;
  }

  public TreeSet getlfset()
  {
    return lfs;
  }
}

@SuppressWarnings("all")
class APFSet
{
  private TreeSet aps;
  private TreeMap c2a;
  private TreeMap a2c;
  private USet us;

  public APFSet(TreeSet ps, TreeSet pfs, SyntaxTree st)
  {
    aps = new TreeSet();
    a2c = new TreeMap();
    c2a = new TreeMap(new CTComp(st));
    us = null;

    int cnt = -1;
    Integer id;
    IRNode ir;
    Iterator it = pfs.iterator();
    while (it.hasNext())
    {
      id = IntegerTable.newInteger(++cnt);
      ir = (IRNode)it.next();
      if (!(st.getOperator(ir)).isProduction())
        us = new USet(true, id);
      c2a.put(ir, id);
      a2c.put(id, ir);
      if (ps.contains(ir))
        aps.add(c2a.get(ir)); 
    }
    if (us == null)
      us = new USet(false, null);
  }

  public TreeSet getapset()
  {
    return aps;
  }

  public TreeMap getc2amap()
  {
    return c2a;
  }

  public TreeMap geta2cmap()
  {
    return a2c;
  }

  public USet getuset()
  {
    return us;
  }
}

@SuppressWarnings("all")
class R0Set
{
  private TreeSet r0s;

  public R0Set(TreeSet lfs, TreeMap c2a, USet u)
  {
    r0s = new TreeSet(Comps.st);

    TreeSet ts;
    IRNode ir;
    Iterator it = lfs.iterator();
    while (it.hasNext())
    {
      ir = (IRNode)it.next();
      ts = new TreeSet();
      ts.add(c2a.get(ir));
      if (u.containswc())
        ts.add(u.getwc());
      r0s.add(ts);
    } 
  }

  public TreeSet getr0set()
  {
    return r0s;
  } 
}

@SuppressWarnings("all")
class RiSet
{
  private TreeSet ris;

  public RiSet(TreeMap opmspm, TreeMap opcpm, USet us, TreeSet rm1)
  {
    ris = new TreeSet(Comps.st);
    
    Set ms = opmspm.entrySet();
    Iterator mit = ms.iterator();
    Map.Entry me;
    TreeMap cpm;
    TreeSet sts;
    Iterator sit;
    TreeSet ts;
    Iterator it;
    TreeSet tts;
    Integer id;
    ArrayList cv;
    while (mit.hasNext())
    {
      me = (Map.Entry)mit.next();
      cpm = (TreeMap)opcpm.get(me.getKey());
      sts = (TreeSet)me.getValue();
      sit = sts.iterator();
      while (sit.hasNext())
      {
        tts = new TreeSet();
        ts = (TreeSet)sit.next();
        it = ts.iterator();
        while (it.hasNext())
        {
          cv = (ArrayList)it.next();
          if (cpm.containsKey(cv))
            tts.add(cpm.get(cv));
        }
        if (us.containswc())
          tts.add(us.getwc());
        ris.add(tts);
      }
    }
    ris.addAll(rm1);
  }

  public TreeSet getriset()
  {
    return ris;
  }
}

@SuppressWarnings("all")
class OpPMap
{
  private TreeMap oppm;
  private TreeMap opcpm;
  
  public OpPMap(TreeSet pfs, TreeSet ops, TreeMap c2a, SyntaxTree st)
  {
    oppm = new TreeMap();
    opcpm = new TreeMap();
    
    Operator op;
    ArrayList al;
    Iterator it = ops.iterator();
    while (it.hasNext())
    {
      op = (Operator)it.next();
      if (op.numChildren() > 0)
      {
        al = new ArrayList();
        for (int c=0; c<op.numChildren(); c++)
          al.add(c, new TreeSet());
        oppm.put(op.name(), al);
        opcpm.put(op.name(), new TreeMap(Comps.vt)); 
      }
    }

    IRNode ir;
    ArrayList tal;
    it = pfs.iterator();
    while (it.hasNext())
    {
      ir = (IRNode)it.next();
      op = st.getOperator(ir);
      if (op.numChildren() > 0)
      {
        al = (ArrayList)oppm.get(op.name());
        tal = new ArrayList();
        Integer cid;
        for (int c=0; c<al.size(); c++)
        {
          cid = (Integer)c2a.get(st.getChild(ir, c));
          ((TreeSet)al.get(c)).add(cid);
          tal.add(c, cid);
        }
        ((TreeMap)opcpm.get(op.name())).put(tal, c2a.get(ir));
      }
    }
  }

  public TreeMap getoppmap()
  {
    return oppm;
  }

  public TreeMap getopcpmap()
  {
    return opcpm;
  }
}

@SuppressWarnings("all")
class OpSMap
{
  private TreeMap opsm;

  public OpSMap(TreeMap opfm, TreeSet r)
  {
    opsm = new TreeMap();

    Set ms = opfm.entrySet();
    Iterator mit = ms.iterator();
    Iterator rit;
    Map.Entry me;
    ArrayList sal;
    ArrayList dal;
    TreeSet ts;
    TreeSet tss;
    while (mit.hasNext())
    {
      me = (Map.Entry)mit.next();
      sal = (ArrayList)me.getValue();
      dal = new ArrayList();
      for (int c=0; c<sal.size(); c++)
      {
        tss = new TreeSet(Comps.st);
        rit = r.iterator();
        while (rit.hasNext())
        {
          ts = new TreeSet();
          ts.addAll((TreeSet)rit.next());
          ts.retainAll((TreeSet)sal.get(c));
          tss.add(ts);
        }
        dal.add(c, tss);
      }
      opsm.put(me.getKey(), dal);
    }
  }

  public TreeMap getopsmap()
  {
    return opsm;
  }
}

@SuppressWarnings("all")
class OpSCMap
{
  private TreeMap opscm;

  public OpSCMap(TreeMap tm)
  {
    opscm = new TreeMap();

    Set ms = tm.entrySet();
    Iterator it = ms.iterator();
    Map.Entry me;
    OpSCSet opscs;
    while (it.hasNext())
    {
      me = (Map.Entry)it.next();
      opscs = new OpSCSet((ArrayList)me.getValue());
      opscm.put(me.getKey(), opscs.getopscset());
    }
  }

  public TreeMap getopscmap()
  {
    return opscm;
  }

  class OpSCSet
  {
    private TreeSet opscs; 

    public OpSCSet(ArrayList al)
    {
      if (al.size() > 0)
      {
        SCSet scs = new SCSet((TreeSet)al.get(0), Comps.vst);
        for (int c=1; c<al.size(); c++)
          scs.incnext((TreeSet)al.get(c));
        opscs = scs.getscset();
      }
    }

    public TreeSet getopscset()
    {
      return opscs;
    }
  }
}

@SuppressWarnings("all")
class OpMSPMap
{
  private TreeMap opmspm;

  public OpMSPMap(TreeMap tm)
  {
    opmspm = new TreeMap();

    Set ms = tm.entrySet();
    Iterator it = ms.iterator();
    Map.Entry me;
    OpMSPSet opmsps;
    while (it.hasNext())
    {
      me = (Map.Entry)it.next();
      opmsps = new OpMSPSet((TreeSet)me.getValue());
      opmspm.put(me.getKey(), opmsps.getopmspset());
    }
  }

  public TreeMap getopmspmap()
  {
    return opmspm;
  }

  class OpMSPSet
  {
    private TreeSet opmsps;

    public OpMSPSet(TreeSet ts)
    {
      opmsps = new TreeSet(Comps.svt);

      ArrayList al;
      SCSet scs;
      Iterator it = ts.iterator();
      while (it.hasNext())
      {
        al = (ArrayList)it.next();
        if (al.size() > 0)
        {
          scs = new SCSet((TreeSet)al.get(0), Comps.vt);
          for (int c=1; c<al.size(); c++)
            scs.incnext((TreeSet)al.get(c));
          opmsps.add(scs.getscset());
        }
      }
    }

    public TreeSet getopmspset()
    {
      return opmsps;
    }
  }
}
