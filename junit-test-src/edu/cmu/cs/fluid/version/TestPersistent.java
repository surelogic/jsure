package edu.cmu.cs.fluid.version;

import java.io.*;
import java.util.*;

import edu.cmu.cs.fluid.FluidRuntimeException;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.tree.PropagateUpTree;
import edu.cmu.cs.fluid.tree.SymmetricEdgeDigraph;
import edu.cmu.cs.fluid.tree.Tree;
import edu.cmu.cs.fluid.util.FileLocator;
import edu.cmu.cs.fluid.util.UniqueID;
import edu.cmu.cs.fluid.util.ZipFileLocator;

/**
 * A class to test persistence of versioned IR structure.
 * See GNUmakefile for example calls.
 * @author boyland
 */
public class TestPersistent {
  
private static final String TESTPERS_FAR_NAME = "testpers.far";
  private static FileLocator floc;
  private static Tree t;
  private static SymmetricEdgeDigraph sed;
  private static SlotInfo<Integer> si;
  private static VersionedChangeRecord ci;
  private static SlotInfo<String> vname;

  static {
    try {
      t = new Tree("TestPersistent.syntax", VersionedSlotFactory.prototype);
      sed =
        new SymmetricEdgeDigraph(
          "TestPersistent.cfg",
          VersionedSlotFactory.prototype);
      si =
        VersionedSlotFactory.prototype.newAttribute(
          "TestPersistent.val",
          IRIntegerType.prototype,
          new Integer(0));
      ci = new VersionedChangeRecord("TestPersistent.treeChanged");
      t.addObserver(ci);
      PropagateUpTree.attach(ci, t);
      vname =
        ConstantSlotFactory.prototype.newAttribute(
          "TestPersistent.vname",
          IRStringType.prototype);
    } catch (SlotAlreadyRegisteredException ex) {
    }
  }

  private static Bundle[] bundle;
  private static Era[] era;
  private static VersionedRegion[] region;

  private static List<IRRegion> svrs = new ArrayList<IRRegion>();

  private static boolean debug = false;

  private static void createStructures() {
    bundle = new Bundle[4];
    t.saveAttributes(bundle[0] = new Bundle());
    sed.saveAttributes(bundle[1] = new Bundle());
    bundle[2] = new Bundle();
    bundle[2].saveAttribute(si);
    // bundle[2].saveAttribute(tc);
    bundle[3] = new Bundle();
    bundle[3].saveAttribute(vname);

    region = new VersionedRegion[2];
    region[0] = new VersionedRegion();
    region[1] = new VersionedRegion();

    IRRegion r1 = region[0], r2 = region[1];

    era = new Era[3];

    IRNode root = new PlainIRNode(r1),
      c1 = new PlainIRNode(r2),
      c2 = new PlainIRNode(r1),
      c3 = new PlainIRNode(r2),
      l1 = new PlainIRNode(r1),
      l2 = new PlainIRNode(r2),
      l3 = new PlainIRNode(r1);

    t.initNode(root, 3);
    t.removeSubtree(root);
    {
      t.initNode(c1, 0);
      t.setChild(root, 0, c1);
      t.initNode(c2, ~2);
      t.setChild(root, 1, c2);
      {
        t.initNode(l1, 0);
        t.setChild(c2, 0, l1);
        t.initNode(l2, 0);
        t.setChild(c2, 1, l2);
        t.initNode(l3, 0);
      }
      t.initNode(c3, 1);
      t.setChild(root, 2, c3);
      {
        t.setChild(c3, 0, null);
      }
    }
    t.removeSubtree(l3);

    /* Tree is complete. l3 is not anywhere in tree */

    IRNode e1 = new PlainIRNode(r2),
      e3 = new PlainIRNode(r1),
      e12 = new PlainIRNode(r2),
      e23 = new PlainIRNode(r1),
      f21 = new PlainIRNode(r2),
      f12 = new PlainIRNode(r1),
      f22 = new PlainIRNode(r2);

    sed.initNode(root, 2, 2);
    sed.initNode(c1, 2, 2);
    sed.initNode(c2, 2, 2);
    sed.initNode(c3, 2, 2);
    sed.initNode(l1, 2, 2);
    sed.initNode(l2, 2, 2);
    sed.initNode(l3, 2, 2);

    sed.initEdge(e1);
    sed.initEdge(e3);
    sed.initEdge(e12);
    sed.initEdge(e23);
    sed.initEdge(f21);
    sed.initEdge(f12);
    sed.initEdge(f22);

    sed.setParentEdge(root, 0, null);
    sed.setChildEdge(root, 0, e1);
    sed.setSink(e1, c1);
    sed.setChildEdge(c1, 0, null);
    sed.setParentEdge(c1, 1, null);
    sed.setChildEdge(c1, 1, e12);
    sed.setSink(e12, c2);
    sed.setChildEdge(c2, 0, f21);
    sed.setSink(f21, l1);
    sed.setChildEdge(l1, 0, null);
    sed.setParentEdge(l1, 1, null);
    sed.setChildEdge(l1, 1, f12);
    sed.setSink(f12, l2);
    sed.setChildEdge(l2, 0, null);
    sed.setParentEdge(l2, 1, null);
    sed.setChildEdge(l2, 1, f22);
    sed.setSink(f22, c2);
    sed.setChildEdge(c2, 1, e23);
    sed.setSink(e23, c3);
    sed.setChildEdge(c3, 0, null);
    sed.setParentEdge(c3, 1, null);
    sed.setChildEdge(c3, 1, e3);
    sed.setSink(e3, root);
    sed.setChildEdge(root, 1, null);

    sed.setChildEdge(l3, 0, null);
    sed.setParentEdge(l3, 0, null);
    sed.setChildEdge(l3, 1, null);
    sed.setParentEdge(l3, 1, null);

    /* graph is now complete */

    e1.setSlotValue(si, new Integer(1));
    e3.setSlotValue(si, new Integer(-1));
    f21.setSlotValue(si, new Integer(1));
    f22.setSlotValue(si, new Integer(-1));

    Version complete = Version.getVersion();
    complete.getShadowNode().setSlotValue(vname, "complete");

    try {
      era[0] = new Era(Version.getInitialVersion(), new Version[] { complete });
    } catch (OverlappingEraException ex) {
      System.out.println("Overlapping eras!");
      ex.printStackTrace();
    }

    if (verbose > 1) System.out.println("Created initial era");
    if (verbose > 2) era[0].describe(System.out);
    if (debug) {
      System.out.println("Initial state:");
      describeState(complete, 0);
      describeState(complete, 1);
    }

    era[1] = new Era(complete);
    Version.setDefaultEra(era[1]);

    /* now we mutate in two possible ways */

    /* Mutation #1:
     * (a) move l3 to be c3's child and then
     * (b) swap c1 and c3
     */
    Version.bumpVersion();
    Version.clampCurrent();

    t.setChild(c3, 0, l3);

    /* the SED needs new edges */
    IRNode f3a = new PlainIRNode(r1), f3b = new PlainIRNode(r2);

    sed.initEdge(f3a);
    sed.initEdge(f3b);

    sed.setChildEdge(c3, 0, f3a);
    sed.setSink(f3a, l3);
    sed.setChildEdge(l3, 1, f3b);
    sed.setSink(f3b, c3);

    f3a.setSlotValue(si, new Integer(1));
    f3b.setSlotValue(si, new Integer(-1));

    Version.unclampCurrent();
    Version mutation1midway = Version.getVersion();
    mutation1midway.getShadowNode().setSlotValue(vname, "mutation1midway");

    if (mutation1midway.parent() != complete) {
      int i = 0;
      for (Version v = mutation1midway; v != complete; v = v.parent()) {
        ++i;
      }
      System.out.println(
        "!! Mutation #1 midway point is " + i + " generations after start");
    } else if (verbose > 1) {
      System.out.println(
        "Mutation #1 midway point is 1 generation after start");
    }
    if (debug) {
      System.out.println("Mutation #1: midpoint:");
      describeState(mutation1midway, 0);
      describeState(mutation1midway, 1);
    }

    t.exchangeSubtree(c1, c3);

    sed.disconnect(e1);
    sed.disconnect(e12);
    sed.disconnect(e23);
    sed.disconnect(e3);

    sed.connect(e3, root, c3);
    sed.connect(e23, c3, c2);
    sed.connect(e12, c2, c1);

    //! This connects wrong parts together.
    // sed.connect(e1,c1,root);
    //! so instead we do the following:
    sed.setChildEdge(c1, 1, e1);
    sed.setParentEdge(root, 1, e1);

    /* slot does not need to be changed */

    Version mutation1done = Version.getVersion();
    mutation1done.getShadowNode().setSlotValue(vname, "mutation1done");
    if (debug) {
      System.out.println("Mutation #1: done:");
      describeState(mutation1done, 0);
      describeState(mutation1done, 1);
    }

    Version.setVersion(complete);
    /* Mutation 2
     * (a) create a new node l4, add l3 to c2
     * (b) Give l4 to c3.
     *     Connect l4 to root to make cycle.
     */
    Version.bumpVersion();

    t.appendChild(c2, l3);

    IRNode f23 = new PlainIRNode(r1), f32 = new PlainIRNode(r2);

    sed.initEdge(f23);
    sed.initEdge(f32);

    sed.disconnect(f22);
    sed.connect(f23, l2, l3);
    sed.setChildEdge(l3, 1, f32);
    sed.setSink(f32, c2);

    f22.setSlotValue(si, new Integer(0));
    f32.setSlotValue(si, new Integer(-1));

    IRNode l4 = new PlainIRNode(r1);
    t.initNode(l4, ~0);

    Version mutation2midway = Version.getVersion();
    mutation2midway.getShadowNode().setSlotValue(vname, "mutation2midway");

    if (mutation2midway.parent() != complete) {
      int i = 0;
      for (Version v = mutation2midway; v != complete; v = v.parent()) {
        ++i;
      }
      System.out.println(
        "!! Mutation #2 midway point is " + i + " generations after start");
    } else if (verbose > 1) {
      System.out.println(
        "Mutation #2 midway point is 1 generation after start");
    }
    if (debug) {
      System.out.println("Mutation #2: midpoint:");
      describeState(mutation2midway, 0);
      describeState(mutation2midway, 1);
    }

    Version.bumpVersion();

    t.setChild(c3, 0, l4);

    IRNode f34 = new PlainIRNode(r2),
      f43 = new PlainIRNode(r1),
      f4r = new PlainIRNode(r2),
      fr4 = new PlainIRNode(r1);

    sed.initNode(l4, 2, 2);
    sed.initEdge(f34);
    sed.initEdge(f43);
    sed.initEdge(f4r);
    sed.initEdge(fr4);

    sed.connect(f34, c3, l4);
    sed.connect(f4r, l4, root);
    sed.connect(fr4, root, l4);
    sed.connect(f43, l4, c3);

    f34.setSlotValue(si, new Integer(1));
    f4r.setSlotValue(si, new Integer(1));
    fr4.setSlotValue(si, new Integer(-1));
    f43.setSlotValue(si, new Integer(-1));

    Version mutation2done = Version.getVersion();
    mutation2done.getShadowNode().setSlotValue(vname, "mutation2done");
    if (debug) {
      System.out.println("Mutation #2: done:");
      describeState(mutation2done, 0);
      describeState(mutation2done, 1);
    }

    era[1].complete();

    if (verbose > 1) System.out.println("Completed second era");
    if (verbose > 2) era[1].describe(System.out);

    // Now a third era with some changes driven by trying
    // to get certain bugs to appear.
    Version.setVersion(mutation2midway);
    era[2] = new Era(mutation2midway);
    Version.setDefaultEra(era[2]);

    Version.bumpVersion();

    t.removeSubtree(l3);
    t.appendChild(l4, l3);
    f32.setSlotValue(si, new Integer(2));

    era[2].complete();
    if (verbose > 1) System.out.println("Completed third era");
    if (verbose > 2) era[2].describe(System.out);
  }

  private static Object undefined = new Object() {
    @Override
    public String toString() {
      return "undefined";
    }
  };
  private static Object unknown = new Object() {
    @Override
    public String toString() {
      return "unknown";
    }
  };
  private static Object exception = new Object() {
    @Override
    public String toString() {
      return "exception";
    }
  };
  
  static void error(StreamTokenizer st, String s) {
    throw new FluidRuntimeException(st + ": " + s);
  }

  static void expect(StreamTokenizer st, int tok) {
    try {
      if (st.nextToken() != tok)
        error(st, "expected " + (char) tok);
    } catch (IOException e) {
      error(st, "expected " + tok + " but got exception " + e);
    }
  }

  static void expect(StreamTokenizer st, String tok) {
    try {
      if (st.nextToken() != StreamTokenizer.TT_WORD || !st.sval.equals(tok))
        error(st, "expected " + tok);
    } catch (IOException e) {
      error(st, "expected " + tok + " but got exception " + e);
    }
  }

  static Era read_era(StreamTokenizer st) throws IOException {
    expect(st, "e");
    expect(st, '#');
    return read_era_number(st);
  }

  private static Era read_era_number(StreamTokenizer st) {
    expect(st, StreamTokenizer.TT_NUMBER);
    int i = (int) st.nval;
    if (i < 0 || i >= era.length)
      error(st, "era index out of bounds");
    return era[i];
  }

  static Version read_version(StreamTokenizer st) throws IOException {
    expect(st, "v");
    expect(st, '(');
    Era e = read_era(st);
    expect(st, ',');
    expect(st, StreamTokenizer.TT_NUMBER);
    int i = (int) st.nval;
    if (i < 0 || i > e.maxVersionOffset())
      error(st, "version offset out of bounds");
    expect(st, ')');
    return e.getVersion(i);
  }

  static IRNode read_node(StreamTokenizer st) throws IOException {
    if (st.nextToken() == '#') {
      // short version
      expect(st,StreamTokenizer.TT_NUMBER);
      int val = (int)st.nval; 
      st.nval = val / 100;
      st.pushBack();
      IRRegion reg = read_region(st);
      val %= 100;
      st.nval = val / 10;
      st.pushBack();
      Era e = read_era_number(st);
      reg = ((VersionedRegion)reg).getDelta(e);
      st.nval = val % 10;
      st.pushBack();
      return read_reg_node(st,reg);
    }
    st.pushBack();
    expect(st, "n");
    expect(st, '(');
    IRRegion reg;
    if (st.nextToken() == StreamTokenizer.TT_WORD) {
      st.pushBack();
      expect(st,"V");
      expect(st,StreamTokenizer.TT_NUMBER);
      int i = (int) st.nval;
      if (i < 0 || i >= svrs.size()) {
	error(st,"version independent region out of bounds");
      }
      reg = svrs.get(i);
    } else {
      st.pushBack();
      VersionedRegion versionedRegion = read_region(st);
      expect(st, ',');
      Era e = read_era(st);
      reg = versionedRegion.getDelta(e);
    }
    expect(st, ',');
    IRNode node = read_reg_node(st, reg);
    expect(st,')');
    return node;
  }

  private static IRNode read_reg_node(StreamTokenizer st, IRRegion reg) throws IOException {
    expect(st, StreamTokenizer.TT_NUMBER);
    int j = (int) st.nval;
    if (j < 1 || j > reg.getNumNodes())
      error(st, "node offset out of bounds");
    return reg.getNode(j);
  }

  /**
   * @param st
   * @return
   */
  private static VersionedRegion read_region(StreamTokenizer st) {
    expect(st, StreamTokenizer.TT_NUMBER);
    int i = (int) st.nval;
    if (i < 0 || i >= region.length)
      error(st, "region index out of bounds");
    return region[i];
  }
  
  private static Bundle read_bundle(StreamTokenizer st) {
    expect(st, StreamTokenizer.TT_NUMBER);
    int i = (int) st.nval;
    if (i < 0 || i >= bundle.length) {
      error(st,"bundle index out of bounds");
    }
    return bundle[i];
  }
  
  private static VersionedChunk read_chunk(StreamTokenizer st) {
    expect(st, StreamTokenizer.TT_WORD);
    if (st.sval.equals("Ch")) {
      expect(st,'(');
      VersionedRegion vr = read_region(st);
      expect(st,',');
      Bundle b = read_bundle(st);
      expect(st,')');
      return VersionedChunk.get(vr,b);
    } else {
      error(st,"expected 'Ch' to start chunk");
      return null;
    }
  }

  static SlotInfo<? extends Object> read_attribute(StreamTokenizer st) throws IOException {
    st.wordChars('.', '.');
    expect(st, StreamTokenizer.TT_WORD);
    try {
      return SlotInfo.findSlotInfo(st.sval);
    } catch (SlotNotRegisteredException ex) {
      error(st, "slot not registered");
    } finally {
      st.ordinaryChar('.');
    }
    return null; // unreached
  }

  static Object read_value(StreamTokenizer st) throws IOException {
    switch (st.nextToken()) {
      case '{':
        List<Object> result = new ArrayList<Object>();
        boolean first = true;
        while (st.nextToken() != '}') {
          st.pushBack();
          if (first) first = false; else expect(st,',');
          result.add(read_value(st));
        }
        return result;
      case '#':
        st.pushBack();
        return read_node(st);
      case StreamTokenizer.TT_WORD :
        String s = st.sval;
        if (s.equals("true"))
          return Boolean.TRUE;
        if (s.equals("false"))
          return Boolean.FALSE;
        if (s.equals("undefined"))
          return undefined;
        if (s.equals("unknown"))
          return unknown;
        if (s.equals("exception"))
          return exception;
        if (s.equals("null"))
          return null;
        if (s.equals("location")) {
          expect(st, StreamTokenizer.TT_NUMBER);
          int i = (int) st.nval;
          expect(st,')');
          return IRLocation.get(i);
        }
        if (s.equals("n")) {
          st.pushBack();
          return read_node(st);
        }
        if (s.equals("v")) {
          st.pushBack();
          return read_version(st);
        }
        error(st, "unknown value: " + s);
        break;
      case StreamTokenizer.TT_NUMBER :
        return new Integer((int) st.nval);
      default :
        error(st, "unknown value");
        break;
    }
    return null; // unreachable code
  }

  static int passed = 0;
  static int failed = 0;
  
  static boolean assert_equal(Object value, Object required, String name, Throwable ex) {
    boolean equal = false;
    if (required != null && ex == null) {
      try {
        equal = required.equals(value);
      } catch (RuntimeException ex1) {
        if (ex != null) ex=ex1;
      }
    } else {
      equal = value == required;
    }
    if (equal) {
      ++passed;
      return true;
    }
    System.err.println("!!! In " + Version.getVersion() + ", " + name + " is " + value + " but should be " + required);
    if (verbose > 0) {
      System.err.println(
          "  debugIsDefined = " + VersionedChunk.debugIsDefined);

      if (ex != null) {
        System.err.println("  exception information");
        ex.printStackTrace(System.err);
      }
    }
    ++failed;
    return false;
  }
  
  static void test_value(IRNode n, SlotInfo<? extends Object> si, Object required) {
    Object value;
    Throwable ex = null;
    try {
      value = n.getSlotValue(si);
    } catch (SlotUnknownException e) {
      ex = e;
      value = unknown;
    } catch (SlotUndefinedException e) {
      ex = e;
      value = undefined;
    } catch (RuntimeException e) {
      ex = e;
      value = exception;
    }
    assert_equal(value, required, n + "." + si, ex);
  }
  
  /**
   * An object that claims to the be the same as "any sequence".
   * (Of course the equals test only works in one direction: and thus this
   * breaks the required semantics for equals.  Sorry.)
   */
  private static Object any_sequence = new Object() {
    @Override
    public boolean equals(Object other) {
      return other == this || other instanceof IRSequence;
    }
    @Override
    public String toString() {
      return "any sequence";
    }
  };

  static void test_sequence(IRNode n, SlotInfo<? extends Object> si, int i, Object required) {
    Object seq_value;
    Throwable seq_exception = null;
    try {
      seq_value = n.getSlotValue(si);
    } catch (SlotUnknownException e) {
      seq_value = unknown;
      seq_exception = e;
    } catch (SlotUndefinedException e) {
      seq_value = undefined;
      seq_exception = e;
    } catch (RuntimeException e) {
      seq_value = exception;
      seq_exception = e;
    }
    if (!assert_equal(seq_value, any_sequence, n + "." + si, seq_exception)) {
      return;
    }
    IRSequence seq = (IRSequence) seq_value; // should not throw exception
    Object value;
    Throwable ex = null;
    try {
      value = seq.elementAt(i);
    } catch (SlotUnknownException e) {
      value = unknown;
      ex = e;
    } catch (SlotUndefinedException e) {
      value = undefined;
      ex = e;
    } catch (RuntimeException e) {
      value = exception;
      ex = e;
    }
    assert_equal(value, required, n + "." + si + "[" + i + "]", ex);
  }

  static void read_command(StreamTokenizer st) throws IOException {
    switch (st.nextToken()) {
      case StreamTokenizer.TT_WORD :
        if (st.sval.equals("version")) {
          Version.setVersion(read_version(st));
          expect(st, ';');
          //System.out.println("Set version to " + Version.getVersion());
        } else if (st.sval.equals("assert")) {
          IRNode n = read_node(st);
          expect(st, '.');
          SlotInfo<? extends Object> si = read_attribute(st);
          int i = -1;
          //Version v = null;
          switch (st.nextToken()) {
            case '[' :
              expect(st, StreamTokenizer.TT_NUMBER);
              i = (int) st.nval;
              expect(st, ']');
              break;
            default :
              st.pushBack();
          }
          expect(st, '=');
          Object value = read_value(st);
          expect(st, ';');
          if (i >= 0)
            test_sequence(n, si, i, value);
          else
            test_value(n, si, value);
        } else if (st.sval.equals("read")) {
          expect(st, '"');
          String f = st.sval;
          expect(st, ';');
          read_test_file(f);
        } else if (st.sval.equals("load")) {
          expect(st,StreamTokenizer.TT_WORD);
          IRPersistent p = null;
          if (st.sval.equals("delta")) {
            VersionedChunk vc = read_chunk(st);
            Era e = read_era(st);
            p = vc.getDelta(e);
          } else if (st.sval.equals("snapshot")) {
            VersionedChunk vc = read_chunk(st);
            Version v = read_version(st);
            p = vc.getSnapshot(v);
          } else if (st.sval.equals("vchunk")) {
            Bundle b = read_bundle(st);
            Era e = read_era(st);
            p = IRChunk.get(e.getShadowRegion(),b);
          } else {
            error(st,"expected delta/snapshot/delta");
          }
          if (p != null) {
            if (verbose > 0) System.out.println("Loading " + p);
            p.load(floc);
          }
          expect(st,';');
        } else {
          error(st, "unknown command: " + st.sval);
        }
        break;
      default :
        error(st, "unknown command");
    }
  }

  static String testDir = null;
  
  static void read_test_file(String filename) throws FileNotFoundException, IOException {
    if (verbose > 0) {
      System.out.println("Reading tests from " + filename);
    }
    InputStream is = new FileInputStream(new File(testDir,filename));
    Reader r = new BufferedReader(new InputStreamReader(is));
    StreamTokenizer st = new StreamTokenizer(r);
    st.slashSlashComments(true);
    st.eolIsSignificant(false);
    st.lowerCaseMode(false);
    st.parseNumbers();
    st.ordinaryChar('.');
    while (st.nextToken() != StreamTokenizer.TT_EOF) {
      st.pushBack();
      read_command(st);
    }
  }

  static void do_test(String filename) {
    try {
      read_test_file(filename);
    } catch (Exception e) {
      System.err.println("got exception " + e);
      e.printStackTrace();
    }
    System.out.println("Passed " + passed + " tests.  Failed " + failed +
        (failed == 1 ? " test" : " tests"));
  }

  static String pad2(int i) {
    String s = Integer.toString(i);
    if (s.length() >= 2)
      return s;
    return " " + s;
  }

  static void describeChunks() {
    for (int i = 0; i < region.length; ++i) {
      for (int j = 0; j < bundle.length - 1; ++j) {
        IRChunk irc = VersionedChunk.get(region[i], bundle[j]);
        irc.describe(System.out);
        if (true) { // (irc instanceof IRState) {
          IRState st = irc;
          System.out.println("isDefined:");
          for (int k = 0; k < era.length; ++k) {
            System.out.println("  " + era[k] + " => " + era[k].isLoaded(st));
            for (Iterator e = era[k].elements(); e.hasNext();) {
              Version v = (Version) e.next();
              System.out.println("  " + v + " => " + v.isLoaded(st));
            }
          }
        }
        System.out.println("");
      }
    }
  }

  static void describeState(Version v, int i) {
    Version.saveVersion();
    try {
      System.out.print("Version: " + v);
      if (v.getShadowNode().valueExists(vname)) {
        System.out.print(" aka " + v.getShadowNode().getSlotValue(vname));
      }
      System.out.println();
      Version.setVersion(v);
      System.out.println(
        "Node reg era off par children  input  output <- ->  dir  Tree");
      System.out.println(
        " #   ion     set ent  0  1  2   0  1   0  1             Changed");
      System.out.println("");
      Vector<IRNode> nodes = new Vector<IRNode>();
      nodes.addElement(null);
      for (int r = 0; r < region.length; ++r) {
        Iterator<IRNode> enm = region[r].allNodes(v);
        while (enm.hasNext()) {
          nodes.addElement(enm.next());
        }
      }
      for (Iterator enm = region[i].allNodes(v); enm.hasNext();) {
        IRNode node = (IRNode) enm.next();
        System.out.print(" " + pad2(nodes.indexOf(node)) + "  " + pad2(i));
        try {
          IRRegion owner = IRRegion.getOwner(node);
          for (int e = 0; e < era.length; ++e) {
            if (region[i].getDelta(era[e]).equals(owner)) {
              System.out.print("  " + pad2(e));
              break;
            }
          }
        } catch (SlotUndefinedException ex) {
          /* kludge for debugging! */
          System.out.print("   1");
        }
        try {
          System.out.print("  " + pad2(IRRegion.getOwnerIndex(node)));
        } catch (SlotUndefinedException ex) {
          /* kludge for debugging */
          System.out.print("    ");
        }
        for (int j = 1; j < 11; ++j) {
          IRNode result = null;
          boolean resultDefined = false;
          boolean resultUnknown = false;
          System.out.print(" ");
          try {
            switch (j) {
              case 1 :
                System.out.print(" ");
                result = t.getParent(node);
                break;
              case 2 :
                System.out.print(" ");
                result = t.getChild(node, 0);
                break;
              case 3 :
                result = t.getChild(node, 1);
                break;
              case 4 :
                result = t.getChild(node, 2);
                break;
              case 5 :
                System.out.print(" ");
                result = sed.getParentEdge(node, 0);
                break;
              case 6 :
                result = sed.getParentEdge(node, 1);
                break;
              case 7 :
                System.out.print(" ");
                result = sed.getChildEdge(node, 0);
                break;
              case 8 :
                result = sed.getChildEdge(node, 1);
                break;
              case 9 :
                System.out.print(" ");
                result = sed.getSource(node);
                break;
              case 10 :
                result = sed.getSink(node);
                break;
            }
            resultDefined = true;
          } catch (Exception ex) {
            if (ex instanceof SlotUnknownException)
              resultUnknown = true;
            // else ex.printStackTrace();
            resultDefined = false;
          }
          if (resultDefined) {
            if (result == null) {
              System.out.print("--");
            } else {
              System.out.print(pad2(nodes.indexOf(result)));
            }
          } else {
            if (resultUnknown) {
              System.out.print(" ?");
            } else {
              System.out.print("  ");
            }
          }
        }
        System.out.print("  ");
        try {
          System.out.print(pad2(node.getIntSlotValue(si)));
        } catch (Exception ex) {
          if (ex instanceof SlotUnknownException)
            System.out.print(" ?");
          else
            System.out.print("  ");
        }
        System.out.print("   ");
        if (ci.changed(node,v.parent(), v)) {
          System.out.print("* ");
        } else {
          System.out.print("  ");
        }
        for (int e = 0; e < era.length; ++e) {
          if (era[e] == null)
            continue;
          Iterator vs = era[e].elements();
          while (vs.hasNext()) {
            Version other = (Version) vs.next();
            try {
              System.out.print(ci.changed(node, v, other) ? "*" : "-");
            } catch (SlotUnknownException ex) {
              System.out.print("?");
            }
          }
        }
        System.out.println();
      }
    } finally {
      Version.restoreVersion();
    }
  }

  static int verbose = 1;
  
  /**
   * @return The number of failures
   */
  public static int test(String flags) {
    String[] args = parseFlags(flags);
    main(args);
    return failed;
  }
  
  private static String[] parseFlags(String flags) {
    List<String> args  = new ArrayList<String>();
    StringTokenizer st = new StringTokenizer(flags);
    while (st.hasMoreTokens()) {
      args.add(st.nextToken());
    }
    return args.toArray(new String[args.size()]);
  }

  public static void main(String args[]) {
    floc = IRPersistent.fluidFileLocator;
    VersionedSlot.debugOn();
    if (args.length < 1 || args[0].equals("--debug")) {
      debug = true;
      verbose += 3;
      createStructures();
      return;
    } else if (args[0].equals("--create")) {
      ++verbose;
      createStructures();
    } else if (args[0].equals("--store")) {
      boolean writeVic = false;
      String testFile = null;
      for (int i = 1; i < args.length; ++i) {
        if (args[i].equals("--debug"))
          IRPersistent.setDebugIO(true);
        else if (args[i].equals("--trace"))
          IRPersistent.setTraceIO(true);
        else if (args[i].equals("--vic"))
          writeVic = true;
        else if (args[i].equals("--testdir"))
          testDir = args[++i];
        else if (args[i].equals("--test"))
          testFile = args[++i];
        else if (args[i].equals("--quiet"))
          --verbose;
        else if (args[i].equals("--verbose"))
          ++verbose;
        else if (args[i].equals("--zip")) {
          try {
            floc = new ZipFileLocator(floc.locateFile(TESTPERS_FAR_NAME,false),ZipFileLocator.WRITE);
          } catch (IOException e) {
            e.printStackTrace();
          }
        } else
          System.out.println("Unknown option: " + args[i]);
      }
      try {
        createStructures();
        if (testFile != null) {
          do_test(testFile);
        }
        for (int i = 0; i < region.length; ++i) {
          region[i].setName(Integer.toString(i));
          region[i].store(floc);
          if (!region[i].finishNodes())
            System.out.println("!!! not all nodes assigned!");
        }
        for (int i = 0; i < era.length; ++i) {
          era[i].setName(Integer.toString(i));
          era[i].store(floc);
        }
        for (int i = 0; i < bundle.length; ++i) {
          bundle[i].setName(Integer.toString(i));
          bundle[i].store(floc);
          if (i == 3) {
            for (int k = 0; k < era.length; ++k) {
              IRPersistent c = IRChunk.get(era[k].getShadowRegion(), bundle[i]);
              c.store(floc);
            }
          } else {
            for (int j = 0; j < region.length; ++j) {
              VersionedChunk ch = VersionedChunk.get(region[j], bundle[i]);
              if (verbose > 0) System.out.println("Writing for " + ch);
              if (verbose > 1) ch.describe(System.out);
              for (int k = 0; k < era.length; ++k) {
                IRPersistent vcd = ch.getDelta(era[k]);
                if (verbose > 0) System.out.println("Writing --delta " + j + " " + i + " " + k);
                vcd.store(floc);
                Iterator vs = era[k].elements();
                while (vs.hasNext()) {
                  Version v = (Version) vs.next();
                  IRPersistent vcs = ch.getSnapshot(v);
                  if (verbose > 0) System.out.println(
                      "Writing --snapshot "
                      + j
                      + " "
                      + i
                      + " "
                      + k
                      + " "
                      + v.getEraOffset());
                  vcs.store(floc);
                  if (writeVic) {
                    SharedVersionedChunk svc = SharedVersionedChunk.get(ch,v);
                    if (verbose > 0) System.out.println("Writing --svc " + svc);
                    svc.store(floc);
                    if (i == 0) {
                      SharedVersionedRegion svr = SharedVersionedRegion.get(region[j],v);
                      svr.store(floc);
                      svrs.add(svr);
                    }
                  }
                }
              }
            }
          }
        }
        PrintWriter p = new PrintWriter(floc.openFileWrite("index.arg"));
        p.print(" --bundles " + bundle.length);
        for (int i = 0; i < bundle.length; ++i) {
          p.print(" " + bundle[i].getID());
        }
        p.println();
        p.print(" --eras " + era.length);
        for (int i = 0; i < era.length; ++i) {
          p.print(" " + era[i].getID());
        }
        p.println();
        p.print(" --regions " + region.length);
        for (int i = 0; i < region.length; ++i) {
          p.print(" " + region[i].getID());
        }
        p.println();
        p.print(" --svrs " + svrs.size());
        for (Iterator it = svrs.iterator(); it.hasNext();) {
          p.print(" " + ((SharedVersionedRegion) it.next()).getExportedID());
        }
        p.println();
        p.close();
      } catch (IOException ex) {
        System.err.println("Stopped: " + ex);
        ex.printStackTrace();
        return;
      } catch (Exception ex) {
        System.err.println("Stopped: " + ex);
        ex.printStackTrace();
        if (verbose > 1) System.err.println("debugIsDefined = " + VersionedChunk.debugIsDefined);
        return;
      } catch (Error e) {
        e.printStackTrace();
        throw e;
      } finally {
        try {
          floc.commit();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      // return;
    } else if (args[0].equals("--load")) {
      VersionedRegion.ensureLoaded();
      VersionedChunk.ensureLoaded();
      Era.ensureLoaded();
      Bundle.ensureLoaded();
      try {
        processLoadArgs(args);
      } catch (IOException ex) {
        System.err.println("Exception occurred: " + ex);
        ex.printStackTrace();
      }
      if (verbose > 1) describeChunks();
    } else if (args[0].equals("--write")) {
      try {
        createStructures();
        ObjectOutput out =
          new ObjectOutputStream(floc.openFileWrite("test.ser"));
        for (int i = 0; i < era.length; ++i) {
          era[i].setName(Integer.toString(i));
          out.writeObject(era[i]);
        }
        for (int i = 0; i < region.length; ++i) {
          region[i].setName(Integer.toString(i));
          if (!region[i].finishNodes()) {
            System.out.println("!!! not finished: " + region[i]);
          }
          out.writeObject(region[i]);
        }
        for (int i = 0; i < bundle.length; ++i) {
          bundle[i].setName(Integer.toString(i));
          out.writeObject(bundle[i]);
          for (int j = 0; j < region.length; ++j) {
            VersionedChunk ch = VersionedChunk.get(region[j], bundle[i]);
            for (int k = 0; k < era.length; ++k) {
              IRPersistent vcd = ch.getDelta(era[k]);
              if (verbose > 0) System.out.println("Writing --delta " + j + " " + i + " " + k);
              out.writeObject(vcd);
            }
          }
        }
        out.close();
      } catch (IOException ex) {
        System.err.println("Stopped: " + ex);
        ex.printStackTrace();
        return;
      }
      System.out.print("gmake RUNARGS=\"--read");
      System.out.print(" --eras " + era.length);
      System.out.print(" --regions " + region.length);
      System.out.print(" --bundles " + bundle.length);
      System.out.println("\" TestPersistent.run");
      // return;
    } else if (args[0].equals("--read")) {
      // force loading to happen:
      VersionedRegion.ensureLoaded();
      VersionedChunk.ensureLoaded();
      Era.ensureLoaded();
      Bundle.ensureLoaded();
      try {
        ObjectInput in = new ObjectInputStream(floc.openFileRead("test.ser"));
        for (int i = 1; i < args.length; ++i) { // NB: i modified in loop
          if (args[i].equals("--debug")) {
            IRPersistent.setDebugIO(true);
          } else if (args[i].equals("--nodebug")) {
            IRPersistent.setDebugIO(false);
          } else if (args[i].equals("--trace")) {
            IRPersistent.setTraceIO(true);
          } else if (args[i].equals("--notrace")) {
            IRPersistent.setTraceIO(false);
          } else if (args[i].equals("--regions")) {
            region = new VersionedRegion[Integer.parseInt(args[++i])];
            for (int j = 0; j < region.length; ++j) {
              region[j] = (VersionedRegion) in.readObject();
              region[j].setName(Integer.toString(j));
              region[j].describe(System.out);
            }
          } else if (args[i].equals("--bundles")) {
            bundle = new Bundle[Integer.parseInt(args[++i])];
            for (int j = 0; j < bundle.length; ++j) {
              bundle[j] = (Bundle) in.readObject();
              bundle[j].setName(Integer.toString(j));
              bundle[j].describe(System.out);
              // read in the chunks:
              for (int k = 0; k < region.length; ++k) {
                for (int l = 0; l < era.length; ++l) {
                  ((IRPersistent) in.readObject()).describe(System.out);
                }
              }
            }
          } else if (args[i].equals("--eras")) {
            era = new Era[Integer.parseInt(args[++i])];
            for (int j = 0; j < era.length; ++j) {
              era[j] = (Era) in.readObject();
              era[j].setName(Integer.toString(j));
              era[j].describe(System.out);
            }
          } else {
            System.err.println("unknown option: " + args[i]);
            return;
          }
        }
        in.close();
      } catch (IOException ex) {
        System.err.println("Exception occurred: " + ex);
        ex.printStackTrace();
      } catch (ClassNotFoundException ex) {
        System.err.println("Exception occurred: " + ex);
        ex.printStackTrace();
      }
    } else {
      System.out.println(
        "usage: gmake RUNARGS=\"--create\" TestPersistent.run");
      return;
    }
    if (verbose >= 2) {    
      for (int i = 0; i < era.length; ++i) {
        System.out.println("Structure during era " + era[i].getID());
        for (Iterator enm = era[i].elements(); enm.hasNext();) {
          Version v = (Version) enm.next();
          System.out.println("For version #" + v.getEraOffset());
          describeState(v, 0);
          System.out.println();
          describeState(v, 1);
          System.out.println();
          System.out.println();
        }
      }
    }
    if (verbose >= 3) VersionedSlot.listing(100);
  }

  /**
   * Process command-line arguments related to loads.
   * @param args
   * @throws IOException
   */
  private static void processLoadArgs(String[] args) throws IOException {
    for (int i = 1; i < args.length; ++i) { // NB: i modified in loop
      //System.out.println("(Processing load arg " + args[i] + ")");
      if (args[i].equals("--debug")) {
        IRPersistent.setDebugIO(true);
      } else if (args[i].equals("--nodebug")) {
        IRPersistent.setDebugIO(false);
      } else if (args[i].equals("--trace")) {
        IRPersistent.setTraceIO(true);
      } else if (args[i].equals("--notrace")) {
        IRPersistent.setTraceIO(false);
      } else if (args[i].equals("--zip")) {
          try {
            floc = new ZipFileLocator(floc.locateFile(TESTPERS_FAR_NAME,true),ZipFileLocator.READ);
          } catch (IOException e) {
            e.printStackTrace();
          }
      } else if (args[i].equals("--indirect")) {
        try {
          Reader r = new InputStreamReader(floc.openFileRead(args[++i]));
          List<String> indirect_args = new ArrayList<String>();
          indirect_args.add("--ignored");
          /*
           * Stream Tokenizer is being untrustworthy.
           * I cannot find out a way to force '-' to be a word char.
           */
          /*
          StreamTokenizer st = new StreamTokenizer(r);
          st.wordChars('-','-');
          st.wordChars('0','9');
          st.commentChar('#');
          int tk;
          while (st.nextToken() == StreamTokenizer.TT_WORD) {
            indirect_args.add(st.sval);
          }
          System.out.println("Token is " + st.ttype);
          */
          StringBuilder buf = new StringBuilder();
          int ch;
          while ((ch = r.read()) >= 0) {
            if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
              if (buf.length() > 0) {
                indirect_args.add(buf.toString());
                buf.setLength(0);
              }
            } else {
              buf.append((char)ch);
            }
          }
          processLoadArgs(indirect_args.toArray(new String[0]));
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      } else if (args[i].equals("--regions")) {
        region = new VersionedRegion[Integer.parseInt(args[++i])];
        for (int j = 0; j < region.length; ++j) {
          UniqueID id = UniqueID.parseUniqueID(args[++i]);
          region[j] = VersionedRegion.loadVersionedRegion(id, floc);
          region[j].setName(Integer.toString(j));
          if (verbose > 1) region[j].describe(System.out);
        }
      } else if (args[i].equals("--bundles")) {
        bundle = new Bundle[Integer.parseInt(args[++i])];
        for (int j = 0; j < bundle.length; ++j) {
          UniqueID id = UniqueID.parseUniqueID(args[++i]);
          bundle[j] = Bundle.loadBundle(id, floc);
          bundle[j].setName(Integer.toString(j));
          if (verbose > 1) bundle[j].describe(System.out);
        }
      } else if (args[i].equals("--eras")) {
        era = new Era[Integer.parseInt(args[++i])];
        for (int j = 0; j < era.length; ++j) {
          UniqueID id = UniqueID.parseUniqueID(args[++i]);
          era[j] = Era.loadEra(id, floc);
          era[j].setName(Integer.toString(j));
          if (verbose > 1) era[j].describe(System.out);
        }
      } else if (args[i].equals("--svrs")) {
        int nsvrs = Integer.parseInt(args[++i]);
        for (int j = 0; j < nsvrs; ++j) {
          UniqueID id = UniqueID.parseUniqueID(args[++i]);
          VersionedRegion reg = VersionedRegion.loadVersionedRegion(id, floc);
          reg.setName("SVR " + Integer.toString(j));
          svrs.add(IRRegion.getRegion(id));
        }
      } else if (args[i].equals("--delta")) {
        VersionedRegion vr = region[Integer.parseInt(args[++i])];
        Bundle b = bundle[Integer.parseInt(args[++i])];
        Era e = era[Integer.parseInt(args[++i])];
        VersionedChunk vc = VersionedChunk.get(vr, b);
        if (verbose > 1) vc.getDelta(e).describe(System.out);
        vc.getDelta(e).load(floc);
      } else if (args[i].equals("--snapshot")) {
        VersionedRegion vr = region[Integer.parseInt(args[++i])];
        Bundle b = bundle[Integer.parseInt(args[++i])];
        Era e = era[Integer.parseInt(args[++i])];
        Version v = e.getVersion(Integer.parseInt(args[++i]));
        VersionedChunk vc = VersionedChunk.get(vr, b);
        vc.getSnapshot(v).load(floc);
        // System.out.println("Loaded --snaphot " + vc.getSnapshot(v));
      } else if (args[i].equals("--vchunk")) {
        Bundle b = bundle[Integer.parseInt(args[++i])];
        Era e = era[Integer.parseInt(args[++i])];
        IRChunk.load(e.getShadowRegion(), b, floc);
      } else if (args[i].equals("--sreg")) {
        int rn = Integer.parseInt(args[++i]);
        SharedVersionedRegion reg = (SharedVersionedRegion) svrs.get(Integer
            .parseInt(args[++i]));
        region[rn] = reg;
      } else if (args[i].equals("--testdir")) {
        testDir = args[++i];
      } else if (args[i].equals("--test")) {
        do_test(args[++i]);
      } else if (args[i].equals("--quiet")) {
        --verbose;
      } else if (args[i].equals("--verbose")) {
        ++verbose;
      } else {
        System.err.println("unknown option: " + args[i]);
        // return;
      }
    }
  }
  
}