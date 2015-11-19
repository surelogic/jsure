package edu.cmu.cs.fluid.parse;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;
import com.surelogic.javac.jobs.JSureConstants;
import com.surelogic.tree.SyntaxTreeSlotFactory;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.tree.PropagateUpTree;
import edu.cmu.cs.fluid.tree.SyntaxTree;
import edu.cmu.cs.fluid.tree.SyntaxTreeInterface;
import edu.cmu.cs.fluid.tree.Tree;
import edu.cmu.cs.fluid.util.FileLocator;
import edu.cmu.cs.fluid.util.IntegerTable;
import edu.cmu.cs.fluid.util.QuickProperties;
import edu.cmu.cs.fluid.util.UniqueID;
import edu.cmu.cs.fluid.version.VersionedSlotFactory;

/**
 * An IRNode being created by the parser. We have extra methods to interface
 * with the parser.
 * 
 * @see JJOperator
 * @see SyntaxTree
 */
@SuppressWarnings("serial")
public class JJNode extends PlainIRNode implements Node {
  /**
	 * Whether or not versioning is turned on. Versioning is turned off sometimes
	 * to permit large files to be read into the IR. We intend to improve the
	 * space efficiency of versioned IR so as to make this unnecessary. 7/8/2003
	 */
  public static final boolean versioningIsOn = JJNodeBoot.versioningIsOn;

  public static final boolean specializeForSyntaxTree = JJNodeBoot.specializeForSyntaxTree;
  
  /**
	 * Slot Factory used to define tree
	 */
  public static final SlotFactory treeSlotFactory = JJNodeBoot.slotFactory;

  /**
	 * We define a specific syntax tree to be used for all parseable languages.
	 * The corresponding operators can all inherit from JJOperator.
	 * 
	 * @see JJOperator
	 */
  public static final SyntaxTreeInterface tree = JJNodeBoot.tree;

  /**
	 * An attribute to store tokens for each node.
	 */
  private static final SlotInfo<String> infoSlotInfo = JJNodeBoot.infoSlotInfo;

  /**
	 * Things interested in knowing whether a subtree has changed, can use the
	 * methods in this object.
	 */
  public static final AbstractChangeRecord treeChanged = JJNodeBoot.treeChanged;

  /**
	 * This flag is true if the children are variable. and so to add a child, one
	 * uses appendChild, rather than setChild.
	 */
  //protected final boolean appendChild;

  /**
	 * The index of the next child to be set. It is used only when appendChild is
	 * false. It is incremented after every child is added.
	 * 
	 * Set to -1 when appendChild would have been false
	 */
  //protected int initNextChild(0;  
  //
  // Replaces the field below with this map that can be used on demand
  // TODO not thread-safe
  private static final IRNodeHashedMap<Integer> nextChildMap = new IRNodeHashedMap<Integer>();
  private static boolean usingJJTree = false;
  
  public static void setUsingJJTree() {
	  usingJJTree = true;
  }
  
  private void setNextChild(int next) {
	  if (usingJJTree) {
		  nextChildMap.put(this, IntegerTable.newInteger(next));
	  }
  }
  
  private int getNextChild() {
	  return nextChildMap.get(this);
  }
  
  public JJNode(Operator operator) {
    this(tree, operator);
  }

  /**
	 * Create a new node with a minimum number of children (when varying).
	 */
  public JJNode(Operator operator, int min) {
    this(tree, operator, min);
  }

  /** Constructor for bottom-up tree creation. */
  public JJNode(Operator operator, IRNode[] children) {
    this(tree, operator, children);
  }

  public JJNode(IRRegion region, Operator operator) {
    this(region, tree, operator);
  }

  /**
	 * Create a new node with a minimum number of children (when varying).
	 */
  public JJNode(IRRegion region, Operator operator, int min) {
    this(region, tree, operator, min);
  }

  /** Constructor for bottom-up tree creation. */
  public JJNode(IRRegion region, Operator operator, IRNode[] children) {
    this(region, tree, operator, children);
  }

  public JJNode(SyntaxTreeInterface tree, Operator operator) {
    super();
    tree.initNode(this, operator);
    //appendChild = operator.numChildren() < 0;
    setNextChild(operator.numChildren() < 0 ? -1 : 0);
  }

  protected JJNode(SyntaxTreeInterface tree) {
	super();
	setNextChild(-1);
  }
  
  /**
	 * Create a new node with a minimum number of children (when varying).
	 */
  public JJNode(SyntaxTreeInterface tree, Operator operator, int min) {
    super();
    tree.initNode(this, operator, min);
    //appendChild = false;
    setNextChild(-1);
  }

  /** Constructor for bottom-up tree creation. */
  public JJNode(
    SyntaxTreeInterface tree,
    Operator operator,
    IRNode[] children) {
    super();
    tree.initNode(this, operator, children);
    //appendChild = false;
    setNextChild(-1);
  }

  public JJNode(IRRegion region, SyntaxTreeInterface tree, Operator operator) {
    super(region);
    tree.initNode(this, operator);
    //appendChild = operator.numChildren() < 0;
    setNextChild(operator.numChildren() < 0 ? -1 : 0);
  }

  /**
	 * Create a new node with a minimum number of children (when varying).
	 */
  public JJNode(
    IRRegion region,
    SyntaxTreeInterface tree,
    Operator operator,
    int min) {
    super(region);
    tree.initNode(this, operator, min);
    //appendChild = false;
    setNextChild(-1);
  }

  /** Constructor for bottom-up tree creation. */
  public JJNode(
    IRRegion region,
    SyntaxTreeInterface tree,
    Operator operator,
    IRNode[] children) {
    super(region);
    tree.initNode(this, operator, children);
    //appendChild = false;
    setNextChild(-1);
  }

  // unused methods
  @Override
  public void jjtOpen() { // unused
  }
  @Override
  public void jjtClose() { // unused
  }
  @Override
  public void jjtSetParent(Node n) { // unused
  }

  @Override
  public void jjtAddChild(Node n) {
	//if (appendChild)
	final int nextChild = getNextChild();
	if (nextChild < 0)
      tree.appendSubtree(this, (JJNode) n);
    else {
      tree.setChild(this, nextChild, (JJNode) n);
      setNextChild(nextChild+1);
    }
  }
  @Override
  public void jjtAddChild(Node n, int i) {
    //if (appendChild)
	final int nextChild = getNextChild();
	if (nextChild < 0)
      // assume reverse order
      // FIX insert nulls if necessary
      tree.insertSubtree(this, (JJNode) n);
    else
      tree.setChild(this, i, (JJNode) n);
  }

  public void setInfo(String s) {
    setSlotValue(infoSlotInfo, s);
  }

  public static void setInfo(IRNode node, String s) {
	  /*
	  if ("E".equals(s) && s != CommonStrings.pool(s)) {
		  System.out.println();
	  }
	  */
    node.setSlotValue(infoSlotInfo, s);
  }

  public static String getInfo(IRNode node) {
    return node.getSlotValue(infoSlotInfo);
  }

  public static String getInfoOrNull(IRNode node) {
    if (!node.valueExists(infoSlotInfo)) {
      return null;
    }
    return getInfo(node);
  }

  public static void doindent(PrintStream s, int i) {
    for (; i > 0; --i) {
      s.print("  ");
    }
  }

  public static String toString(IRNode node) {
    if (node == null) {
      return "null";
    } else {
      String opName = tree.getOperator(node).name();
      try {
        String info = getInfo(node);
        if (info == null)
          return opName + " null";
        else
          return opName + " " + info;
      } catch (SlotUndefinedException e) {
        return opName;
      }
    }
  }

  public static void dumpTree(PrintStream s, IRNode root, int indent) {
    doindent(s, indent);
    if (root == null) {
      s.println("null");
    } else {
      s.println(toString(root));
      Iterator<IRNode> children = tree.children(root);
      while (children.hasNext()) {
        IRNode child = children.next();
        dumpTree(s, child, indent + 1);
      }
    }
  }

  public static IRNode copyTree(IRNode root) {
    if (root == null)
      return root;
    return ((JJOperator) tree.getOperator(root)).copyTree(root);
  }

  private static Bundle jjbundle = null;
  static {
	final UniqueID id = UniqueID.parseUniqueID("parse");  
    try {
      jjbundle =
        Bundle.loadBundle(id,          
          IRPersistent.fluidFileLocator);
    } catch (IOException ex) {
      JavaGlobals.PARSE.fine(ex.toString());

      // temp
      jjbundle = Bundle.findBundle(id);
      JJNodeBoot.saveAttributes(jjbundle);
    }
  }

  public static Bundle getBundle() {
    return jjbundle;
  }

  public static void main(String args[]) {
    Bundle b = getBundle();
    if (b != null)
      b.describe(System.out);
  }
  
  public static void ensureLoaded() { /* do nothing */ }
}

/**
 * This class holds the definitions of the attributes associated with JJNode.
 * it is in a separate class so that we can have code that generates the
 * Bundle. In JJNode, the bundle is assumed to be already stored, and is
 * loaded.
 */
class JJNodeBoot {
  /**
	 * Logger for this class
	 */
  private static final Logger LOG = SLLogger.getLogger("FLUID.parse");
  
  public static final QuickProperties.Flag specializeFlag = 
    new QuickProperties.Flag(LOG, "fluid.specializeForSyntaxTree", "Tree",
                             true, true);

  /**
	 * Slot Factory used to define tree
	 */
  public static final SlotFactory slotFactory;

  /**
	 * We define a specific syntax tree to be used for all parseable languages.
	 * The corresponding operators can all inherit from JJOperator.
	 * 
	 * @see JJOperator
	 */
  public static final SyntaxTreeInterface tree;

  /**
	 * An attribute to store tokens for each node.
	 */
  static final SlotInfo<String> infoSlotInfo;

  /**
	 * Things interested in knowing whether a subtree has changed, can use the
	 * methods in this object.
	 */
  public static final AbstractChangeRecord treeChanged;

  public static boolean versioningIsOn = JSureConstants.versioningIsOn;
  
  static final boolean specializeForSyntaxTree = QuickProperties.checkFlag(specializeFlag);

  static {
    try {
      //System.out.println("versioningIsOn = "+versioningIsOn);
      if (versioningIsOn) {    	
        slotFactory = VersionedSlotFactory.prototype;
      } 
      else if (specializeForSyntaxTree) {
        slotFactory = SyntaxTreeSlotFactory.prototype;
      }        
      else {
        slotFactory = SimpleSlotFactory.prototype;
      }

      //tree = new SyntaxTreeSubclass("Parse", slotFactory);
      tree = new SyntaxTree("Parse", slotFactory);     
      treeChanged = slotFactory.newChangeRecord("Parse.changed");
      tree.addObserver(treeChanged);
      PropagateUpTree.attach(treeChanged,(Tree) tree);
      infoSlotInfo =
        slotFactory.newAttribute("JJNode.info", IRStringType.prototype, "");
      infoSlotInfo.addObserver(treeChanged);
    } catch (SlotAlreadyRegisteredException e) {
      throw new FluidError("Parse slots already registered " + e);
    }
  }

  static void saveAttributes(Bundle b) {
    ((SyntaxTree)tree).saveAttributes(b);
    b.saveAttribute(infoSlotInfo);
  }

  public static void main(String args[]) {
    Bundle b = new Bundle();
    FileLocator floc = IRPersistent.fluidFileLocator;
    saveAttributes(b);
    try {
      b.store(floc);
    } catch (IOException ex) {
      System.out.println(ex.toString());
      System.out.println("Please press return to try again");
      try {
        System.in.read();
        b.store(floc);
      } catch (IOException ex2) {
        System.out.println(ex2.toString());
        return;
      }
    }
    new File(b.getID().toString() + ".ab").renameTo(new File("parse.ab"));
  }
}
