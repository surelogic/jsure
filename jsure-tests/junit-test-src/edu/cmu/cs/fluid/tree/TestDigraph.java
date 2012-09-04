package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.test.IReporter;
import edu.cmu.cs.fluid.util.SimpleApp;

public class TestDigraph extends SimpleApp {
  protected boolean verbose = false;
  protected IReporter reporter = new IReporter() {
	@Override
    public void reportError(String msg) {
      System.out.println(msg);
    }
  };
  
  public TestDigraph() { 
	  // Nothing to do
  }
  
  public TestDigraph setReporter(IReporter r) {
    if (r != null) {
      reporter = r;
    }
    return this;
  }
  
  public TestDigraph(IReporter r) {
    setReporter(r);
  }
   
  public void report_error(String test, String message) {
    reporter.reportError("!!! " + test + message);
  }
  
  public static void main(String args[]) {
    new TestDigraph().test(args);
  }
  
  public boolean getVerbose() {
    return verbose; 
  }
  
  protected MutableDigraphInterface createStored(SlotFactory sf) {
    try {
      return new Digraph(null,sf);
    } catch (SlotAlreadyRegisteredException ex) {
      System.out.println("panic: " + ex);
      ex.printStackTrace();
      System.exit(1);
      return null;
    }
  }
  
  @SuppressWarnings("unchecked")
  protected MutableDigraphInterface createCopy(MutableDigraphInterface dig) {
    return new Digraph(dig.getAttribute(Digraph.CHILDREN));
  }
  
  public void test(String[] args) {
    configure("fluid-tree-TestDigraph");

    verbose = args.length != 0;
    test("mutable", SimpleSlotFactory.prototype, true);
    test("immutable", ConstantSlotFactory.prototype, false);
  }

  public void test(String name, SlotFactory factory) {
    test(name,factory,true);
  }
  
  public void test(String name, SlotFactory factory, boolean mutable) {
    MutableDigraphInterface dig = createStored(factory);
    MutableDigraphInterface copy = createCopy(dig);
    test(name,dig,copy,mutable,getDagOK());
  }

  protected boolean getDagOK() { return true; }

  public static class SelfDocumentingIRNode extends PlainIRNode {
    String name;
    public SelfDocumentingIRNode(String n) {
      super();
      name = n;
    }
    @Override
    public String toString() { return name + ":IRNode"; }
  }

  protected void test(String name, MutableDigraphInterface dig, MutableDigraphInterface copy,
		      boolean mutable, boolean dagOK) {
    IRNode root = new SelfDocumentingIRNode("root");
    IRNode c0 = new SelfDocumentingIRNode("c0");
    IRNode c1 = new SelfDocumentingIRNode("c1");
    IRNode c2 = new SelfDocumentingIRNode("c2");
    IRNode x = new SelfDocumentingIRNode("x");
    IRNode y = new SelfDocumentingIRNode("y");

    test_init_node(name+".initNode(root,3)",dig,root,3,true);
    test_init_node(name+".initNode(c0,0)",dig,c0,0,true);
    test_init_node(name+".initNode(c1,~0)",dig,c1,~0,true);
    test_init_node(name+"_copy.initNode(c2,~1)",copy,c2,~1,true);
    test_init_node(name+"_copy.initNode(x,0)",copy,x,0,true);

    /* Reinitializing children is OK now. */
    test_init_node(name+"_copy.initNode(root,3)",copy,root,3,mutable);

    test_num_children(name+"_copy.numChildren(root)",copy,root,3,true);
    test_num_children(name+".numChildren(c0)",dig,c0,0,true);
    test_num_children(name+"_copy.numChildren(c1)",copy,c1,0,true);
    test_num_children(name+".numChildren(c2)",dig,c2,1,true);
    test_num_children(name+"_copy.numChildren(x)",copy,x,0,true);
    test_num_children(name+".numChildren(y)",dig,y,0,false);

    test_set_child(name+".setChild(root,0,c0)",dig,root,0,c0,true,true,true);
    test_set_child(name+".setChild(root,2,c2)",dig,root,2,c2,true,true,true);
    test_get_child(name+".getChild(root,0)",dig,root,0,c0,true,true);
    test_get_child(name+"_copy.getChild(root,1)",copy,root,1,c1,false,true);
    test_get_child(name+".getChild(root,2)",dig,root,2,c2,true,true);
    test_get_child(name+".getChild(root,3)",dig,root,3,null,true,false);
    test_set_child(name+"_copy.setChild(root,1,c1)",
		   copy,root,1,c1,true,true,true);    
    test_set_child(name+"_copy.setChild(c2,0,x)",
		   copy,c2,0,x,true,true,true);

    // now try mutations:
    test_replace_child(name+"_copy.replaceChild(root,c2,null)",
		       copy,root,c2,null,
		       mutable,true);
    test_replace_child(name+".replaceChild(root,c1,c0)",
		       dig,root,c1,c0,
		       mutable,dagOK);
    test_replace_child(name+".replaceChild(root,x,null)",
		       dig,root,x,null,
		       mutable,false);

    test_set_child(name+"_copy.setChild(root,1,null)",copy,root,1,null,
		   mutable,true,true);
    
    test_init_node(name+".initNode(y,0)",dig,y,0,true);

    test_insert_child(name+"_copy.insertChild(c2,y,InsertionPoint.first)",
		      copy,c2,y,InsertionPoint.first,mutable,true);
    test_insert_child(name+".insertChild(c2,root,InsertionPoint.last)",
		      dig,c2,root,InsertionPoint.last,mutable,true);
    test_insert_child(name+".insertChild(c2,c1,InsertionPoint.first)",
		      dig,c2,c1,InsertionPoint.first,mutable,mutable|dagOK);
    test_insert_child(name+"_copy.insertChild(c2,c0,InsertionPoint.last)",
		      copy,c2,c0,InsertionPoint.last,mutable,dagOK);
    
  }

  public void test_init_node(String test,
			     MutableDigraphInterface dig, IRNode node, int numChildren,
			     boolean mutationOK)
  {
    try {
      dig.initNode(node,numChildren);
      if (!mutationOK)
	report_error(test, " should not have succeeded");
    } catch (SlotImmutableException ex) {
      if (mutationOK) {
	report_error(test, " should have succeeded");
	if (verbose) ex.printStackTrace();
      }
    } catch (Throwable ex) {
      report_error(test, " should not have failed: " + ex);
      ex.printStackTrace();
      System.exit(1);
    }
  }

  public void test_num_children(String test,
				MutableDigraphInterface dig, IRNode node, int num, 
				boolean defined)
  {
    try {
      int count = dig.numChildren(node);
      if (!defined)
	report_error(test, " should not have returned.");
      if (num != count)
	report_error(test, " returned wrong result");
    } catch (SlotUndefinedException ex) {
      if (defined)
	report_error(test, " should have returned");
    } catch (Throwable ex) {
      report_error(test, " should not have failed: " + ex);
      ex.printStackTrace();
      System.exit(1);
    }
  }

  public void test_set_child(String test,
			     MutableDigraphInterface dig, IRNode node, int i, IRNode newChild,
			     boolean mutationOK,
			     boolean locationOK,
			     boolean childOK)
  {
    try {
      dig.setChild(node,i,newChild);
      if (!mutationOK)
	report_error(test, " should have rejected mutation.");
      if (!locationOK)
	report_error(test, " should have rejected location");
      if (!childOK)
	report_error(test, " should have rejected child");
    } catch (SlotImmutableException ex) {
      if (mutationOK) {
	report_error(test, " should not have rejected mutation");
	if (verbose) ex.printStackTrace();
      }
    } catch (IRSequenceException ex) {
      if (locationOK) {
	report_error(test, " should not have rejected location");
	if (verbose) ex.printStackTrace();
      }
    } catch (StructureException ex) {
      if (childOK) {
	report_error(test, " should not have rejected child");
	if (verbose) ex.printStackTrace();
      }
    } catch (Throwable ex) {
      report_error(test, " should not have failed: " + ex);
      ex.printStackTrace();
    }
  }

  public void test_get_child(String test,
			     MutableDigraphInterface dig, IRNode node, int i, IRNode result,
			     boolean defined,
			     boolean locationOK)
  {
    try {
      IRNode value = dig.getChild(node,i);
      if (!defined)
	report_error(test, " should not have returned.");
      if (!locationOK)
	report_error(test, " should have rejected location");
      if (!value.equals(result))
	if (value == null || !value.equals(result))
	  report_error(test, " returned wrong result");
    } catch (SlotUndefinedException ex) {
      if (defined) {
	report_error(test, " should have returned");
	if (verbose) ex.printStackTrace();
      }
    } catch (IRSequenceException ex) {
      if (locationOK) {
	report_error(test, " should not have rejected location");
	if (verbose) ex.printStackTrace();
      }
    } catch (Throwable ex) {
      report_error(test, " should not have failed: " + ex);
      ex.printStackTrace();
    }
  }

  public void test_replace_child(String test,
				 MutableDigraphInterface dig,
				 IRNode node, IRNode oldChild, IRNode newChild,
				 boolean mutationOK,
				 boolean childOK)
  {
    try {
      dig.replaceChild(node,oldChild,newChild);
      if (!mutationOK)
	report_error(test, " should have rejected mutation.");
      if (!childOK)
	report_error(test, " should have rejected child");
    } catch (SlotImmutableException ex) {
      if (mutationOK) {
	report_error(test, " should not have rejected mutation");
	if (verbose) ex.printStackTrace();
      }
    } catch (StructureException ex) {
      if (childOK) {
	report_error(test, " should not have rejected child");
	if (verbose) ex.printStackTrace();
      }
    } catch (Throwable ex) {
      report_error(test, " should not have failed: " + ex);
      ex.printStackTrace();
      System.exit(1);
    }
  }
  public void test_insert_child(String test, MutableDigraphInterface dig,
				IRNode node, IRNode newChild,
				InsertionPoint ip,
				boolean mutationOK,
				boolean childOK)
  {
    try {
      IRLocation loc = dig.insertChild(node,newChild,ip);
      if (loc == null) {
        report_error(test," should not have returned null location");
      }
      if (!mutationOK)
	report_error(test, " should have rejected mutation.");
      if (!childOK)
	report_error(test, " should have rejected child");
      test_get_child("same.getChild("+test+")",dig,node,
		     dig.childLocationIndex(node,loc),newChild,
		     true,true);
    } catch (SlotImmutableException ex) {
      if (mutationOK)
	report_error(test, " should not have rejected mutation");
    } catch (StructureException ex) {
      if (childOK)
	report_error(test, " should not have rejected child");
    } catch (Throwable ex) {
      report_error(test, " should not have failed: " + ex);
      ex.printStackTrace();
      System.exit(1);
    }
  }
}
