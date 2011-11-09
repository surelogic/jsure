package edu.cmu.cs.fluid.pattern;

import java.util.*;
import edu.cmu.cs.fluid.util.IntegerTable;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.tree.*;
import edu.cmu.cs.fluid.version.*;

/** A simple bottom-up tree automaton.
 */
@SuppressWarnings("all")
public class Automaton
{
  private SyntaxTree st;
  private TreeChanged tc;
  private SlotInfo ssi;
  private TreeSet ops;
  private TreeMap c2a;
  private USet us;
  private TreeMap rsm;
  private TreeMap opmum;
  private TreeMap opomegam;
  private TreeSet mss;

  /** Create an automaton that matches the given patterns in 
   * tree set ps.
   * @param synt syntax tree used to access children and operators.
   *             (should be versioned)
   * @param tch used for versioning.
   */
  public Automaton(TreeSet ps, SyntaxTree synt, TreeChanged tch)
  {
    st = synt;
    tc = tch;
    ssi = SimpleSlotFactory.prototype.newAttribute();

    PrePros pp = new PrePros(ps, st);
    ops = pp.getopset();
    TreeSet aps = pp.getapset();
    c2a = pp.getc2amap();
    us = pp.getuset();
    TreeMap oppm = pp.getoppmap();
    TreeMap opcpm = pp.getopcpmap();
    TreeMap opsm = pp.getopsmap();
    TreeSet rs = pp.getrset();

    SetMap sm = new SetMap(rs);
    rsm = sm.getsetmap();
    TreeMap rism = sm.getisetmap();
    
    MSSet msset = new MSSet(aps, rism);
    mss = msset.getmsset();
   
    PreAuto pa = new PreAuto(opsm, oppm, opcpm, us, rsm);
    opmum = pa.getopmumap();
    opomegam = pa.getopomegamap();
  }


  private Integer getState(IRNode root)
  {
    int[] cord;
    StateCache sc = null; 
    Version version = Version.getVersion();

    if (!ops.contains(st.getOperator(root)))
      return IntegerTable.newInteger(-1); // very cheap

    if (root.valueExists(ssi))
    {
      sc = (StateCache)root.getSlotValue(ssi);
      Version oldVersion = sc.getVersion();
      if (oldVersion == version) 
        return sc.getState();
      else if (!tc.changed(root, oldVersion, version)) 
      {
	  sc.setVersion(version); // make next access faster
        return sc.getState();
      }
    }

    Integer state;
    if (st.numChildren(root) == 0)
    {
      TreeSet ts = new TreeSet();
      if (c2a.containsKey(root))
        ts.add(c2a.get(root));
      if (us.containswc())
        ts.add(us.getwc());
      state = (Integer)rsm.get(ts);
    }
    else
    {
      String op = st.getOperator(root).name();
      ArrayList mumv = (ArrayList)opmum.get(op);
      cord = new int[st.numChildren(root)];
      for (int c=0; c<st.numChildren(root); c++)
	{
	  ArrayList childmum = (ArrayList)(mumv.get(c));
	  IRNode child = st.getChild(root, c);
	  int childstate = getState(child).intValue();
          if (childstate == -1)
          {
            state = IntegerTable.newInteger(-1);
            break;
          }
	  cord[c] = ((Integer)childmum.get(childstate)).intValue();
	}
	//! fix this assignment not to apply if state = -1
      state = ((ANDArray)opomegam.get(op)).get(cord);
    }

    if (sc == null)
    {
      sc = new StateCache(state, version);
      root.setSlotValue(ssi, sc);
    }
    else
    {
      sc.setState(state);
      sc.setVersion(version);
    }

    return state;
  }

  /** Return true if given node matches a pattern in the tree set
   * used to construct the automaton.
   */
  public boolean isMatch(IRNode node)
  {
    if (node == null)
    {
      return false;
    }
    else
      return mss.contains(getState(node));
  }

  /** Compute the list of nodes which match a pattern in the set.
   * @param root subtree to look in for matches.
   */
  public ArrayList matches(IRNode root)
  {
    ArrayList ml = new ArrayList();

    rmatches(root, ml);  

    return ml;
  }

  private void rmatches(IRNode root, ArrayList ml)
  {
    if (isMatch(root)) 
      ml.add(root);
    for (int c=0; c<st.numChildren(root); c++)
      rmatches(st.getChild(root, c), ml); 
  }

/*
  public Enumerate matches(final IRNode root)
  {
    return new SimpleIterator()
               {
                 Enumeration nodes = st.topDown(root);

                 Object computeNextElement();
                 {
                   while (nodes.hasNext())
	             {
	               IRNode node = (IRNode)nodes.getElement();
	               if (isMatch(node)) 
             
                     return node;
	             }

                  return noElement;
                }
              };
  }
*/

  /* Construct a highlighter that highlights matching nodes. 
  public Highlighter getHighlighter(Role role) 
  {
    return new TreeHighlighter(role, st) 
               {
                 public boolean contains(IRNode node) 
                 { 
                   return isMatch(node);
                 }
               };
  }
  */

  static class MSSet
  {
    TreeSet mss;

    public MSSet(TreeSet aps, TreeMap rism)
    {
      mss = new TreeSet();

      Set ms = rism.entrySet();
      Iterator it = ms.iterator();
      Map.Entry me;
      TreeSet tts = new TreeSet();
      while (it.hasNext())
      {
        me = (Map.Entry)it.next();
        tts.addAll((TreeSet)me.getValue());
        tts.retainAll(aps);
        if (tts.size() > 0)
          mss.add(me.getKey());
        tts.clear();
      }
    }

    public TreeSet getmsset()
    {
      return mss;
    }
  }

  static class StateCache 
  {
    private Integer state;
    private Version version;

    public StateCache(Integer s, Version v)
    {
      setState(s);
      setVersion(v);
    }

    public Integer getState()
    {
      return state;
    }

    public Version getVersion()
    {
      return version;
    }

    public void setState(Integer s)
    {
      state = s;
    }

    public void setVersion(Version v)
    {
      version = v;
    }
  }
}
