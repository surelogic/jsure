/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/TestSymmetricEdgeDigraph.java,v 1.14 2009/02/27 19:01:20 chance Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.test.IReporter;

public class TestSymmetricEdgeDigraph extends TestEdgeDigraph {
  public TestSymmetricEdgeDigraph() {
	// Nothing to change
  }
  
  public TestSymmetricEdgeDigraph(IReporter r) {
    super(r);
  }
  
  public static void main(String args[]) {
    new TestSymmetricEdgeDigraph().test(args);
  }
  @Override
  EdgeDigraph createStored(SlotFactory sf) {
    try {
      return new SymmetricEdgeDigraph(null,sf);
    } catch (SlotAlreadyRegisteredException ex) {
      System.out.println("panic: " + ex);
      ex.printStackTrace();
      System.exit(1);
      return null;
    }
  }
  @Override
  @SuppressWarnings("unchecked") EdgeDigraph createCopy(EdgeDigraph dig) {
    return new SymmetricEdgeDigraph(dig.getAttribute("childEdges"),
				    dig.getAttribute("parentEdges"),
				    dig.getAttribute("sinks"),
				    dig.getAttribute("sources"),
				    dig.getAttribute("isEdge"));
  }
  @SuppressWarnings("unchecked") EdgeDigraph createBackwardCopy(EdgeDigraph dig) {
    return new SymmetricEdgeDigraph(dig.getAttribute("parentEdges"),
				    dig.getAttribute("childEdges"),
				    dig.getAttribute("sources"),
				    dig.getAttribute("sinks"),
				    dig.getAttribute("isEdge"));
  }

  protected class AsDigraph extends TestEdgeDigraph.AsDigraph {
    @Override
    public void test(String[] args) {
      // immutable SymmetricDigraphs are basically useless.
      MutableDigraphInterface dig = this.createStored(SimpleSlotFactory.prototype);
      this.test("mutable",dig,this.createCopy(dig),true,getDagOK());
      MutableDigraphInterface backward = createBackwardCopy(ed).getAsDigraph();
      this.test("backward",backward,backward,true,getDagOK());
    }
  }

  @Override
  public void test(String[] args) {
    new AsDigraph().setReporter(reporter).test(args);
    verbose = args.length != 0;
    // immutable SymmetricDigraphs are basically useless.
    test(SimpleSlotFactory.prototype);    
  }
  
  /**
   * @param slotFactory
   */
  public void test(SlotFactory slotFactory) {
    SymmetricEdgeDigraph dig =
      (SymmetricEdgeDigraph)createStored(slotFactory);
    SymmetricEdgeDigraph copy =
      (SymmetricEdgeDigraph)createCopy(dig);
    test("mutable",dig,copy,true,getDagOK());
    SymmetricEdgeDigraph backward =
      (SymmetricEdgeDigraph) createBackwardCopy(dig);
    test("backward",backward,backward,true,getDagOK());

    // real simple connect and disconnect test:
    
    IRNode x = new SelfDocumentingIRNode("x");
    IRNode y = new SelfDocumentingIRNode("y");
    IRNode e = new SelfDocumentingIRNode("e");

    test_init_node("mutable",dig,x,0,1,true);
    test_init_node("mutable_copy",copy,y,1,0,true);
    test_init_edge("mutable",dig,e,true);
    test_init_node("mutable_copy",copy,e,1,1,true);
    test_init_edge("mutable",dig,e,true);
    test_init_edge("mutable",dig,y,true);
    test_init_node("mutable_copy",copy,y,1,0,true);

    test_connect("backward",backward,e,y,x,true,true,true);
    test_get_child_edge("mutable.getChildEdge(x,0)",dig,x,0,e,
			true,true,true);
    test_get_child_edge("backward.getChildEdge(y,0) #1",backward,y,0,e,
			true,true,true);
    test_disconnect("mutable_copy",copy,e,true,true);
    test_get_child_edge("mutable.getChildEdge(x,0) #2",dig,x,0,null,
			true,true,true);
    test_get_child_edge("backward.getChildEdge(y,0) #2",backward,y,0,null,
			true,true,true);
    test_disconnect("mutable",dig,x,true,false);
    test_get_child_edge("mutable.getChildEdge(x,0) #3",dig,x,0,null,
			true,true,true);
    test_get_child_edge("backward.getChildEdge(y,0) #3",backward,y,0,null,
			true,true,true);
    test_connect("mutable_copy",copy,e,x,y,true,true,true);
    test_get_child_edge("mutable.getChildEdge(x,0)",dig,x,0,e,
			true,true,true);
    test_get_child_edge("backward.getChildEdge(y,0) #4",backward,y,0,e,
			true,true,true);
  }

  public void test_init_node(String name, SymmetricEdgeDigraph dig,
			     IRNode node, int numParents, int numChildren,
			     boolean mutationOK)
  {
    String test = name + ".initNode(" + node + "," + numParents + "," +
	numChildren + ")";
    try {
      dig.initNode(node,numParents,numChildren);
      if (!mutationOK)
        reportError(test, " should have rejected mutation.");
    } catch (SlotImmutableException ex) {
      if (mutationOK) {
        reportError(test, " should not have rejected mutation");
        if (verbose) ex.printStackTrace();
      }
    } catch (Throwable ex) {
      reportError(test, " should not have failed: " + ex);
      ex.printStackTrace();
    }
  }

  public void test_init_edge(String name,
                             SymmetricEdgeDigraph dig, IRNode edge,
                             boolean mutationOK)
  {
    String test = name + ".initEdge(" + edge + ")";
    super.test_init_edge(test,dig,edge,mutationOK);
  }

  public void test_connect(String name, EdgeDigraph dig,
			   IRNode edge, IRNode source, IRNode sink,
			   boolean mutationOK,
			   boolean childOK,
			   boolean isEdgeOK)
  {
    String test = name + ".connect(" + edge + "," +
      source + "," + sink + ")";
    try {
      dig.connect(edge,source,sink);
      if (!mutationOK)
	reportError(test, " should have rejected mutation.");
      if (!childOK)
	reportError(test, " should have rejected child");
      if (!isEdgeOK)
	reportError(test, " should have caused EdgeDigraph exception");
    } catch (SlotImmutableException ex) {
      if (mutationOK) {
	reportError(test, " should not have rejected mutation");
	if (verbose) ex.printStackTrace();
      }
    } catch (EdgeDigraphException ex) {
      if (isEdgeOK) {
	reportError(test, " should not have had node/edge problems");
	if (verbose) ex.printStackTrace();
      }
    } catch (StructureException ex) {
      if (childOK) {
	reportError(test, " should not have rejected child");
	if (verbose) ex.printStackTrace();
      }
    } catch (Throwable ex) {
      reportError(test, " should not have failed: " + ex);
      ex.printStackTrace();
    }
  }

  public void test_disconnect(String name, SymmetricEdgeDigraph dig,
			      IRNode edge,
			      boolean mutationOK,
			      boolean isEdgeOK)
  {
    String test = name + ".disconnect(" + edge + ")";
    try {
      dig.disconnect(edge);
      if (!mutationOK)
	reportError(test, " should have rejected mutation.");
      if (!isEdgeOK)
	reportError(test, " should have caused EdgeDigraph exception");
    } catch (SlotImmutableException ex) {
      if (mutationOK) {
	reportError(test, " should not have rejected mutation");
	if (verbose) ex.printStackTrace();
      }
    } catch (EdgeDigraphException ex) {
      if (isEdgeOK) {
	reportError(test, " should not have had node/edge problems");
	if (verbose) ex.printStackTrace();
      }
    } catch (Throwable ex) {
      reportError(test, " should not have failed: " + ex);
      ex.printStackTrace();
    }    
  }
}
