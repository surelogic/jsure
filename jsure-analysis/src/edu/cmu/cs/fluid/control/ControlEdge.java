/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/ControlEdge.java,v 1.14 2005/05/23 18:28:50 chance Exp $ */
package edu.cmu.cs.fluid.control;


/** Edges into the Java control-flow graph.
 * @author John Tang Boyland
 * @see ControlNode
 * @see SimpleControlEdge
 * @invariant isSane
 */

public abstract class ControlEdge extends Entity {
  protected static final String SOURCE_TYPE_NOT_SUPPORTED = "the provided source node is of an unsupported type";
  protected static final String SOURCE_FULL = "source full";
  protected static final String SOURCE_ALREADY_TAKEN = "source already taken";
 
  /** Private invariant, that is something true after
   * any constructor or method completes.
   * @fluid invariant
   */
  /*
  private boolean isSane() {
    return (okSourceSink(getSource(),getSink())
	    && (!sourceIsSecondary() || (getSource() != null))
	    && (!sinkIsSecondary() || (getSink() != null)));
  }
  */
  /** Return true if the given values are legal source and sinks
   * @fluid property
   */
  /*
  private static boolean okSourceSink(ControlNode source, ControlNode sink) {
    return (!(source == null ^ sink == null)
	    && (source == null ||
		source instanceof OneOutput ||
		source instanceof TwoOutput ||
		source instanceof VariableOutput)
	    && (sink == null ||
		sink instanceof OneInput ||
		sink instanceof TwoInput ||
		source instanceof VariableInput ));
  }
  */
  
  /** Return the control node this edge starts from.
   * @pure
   */
  public abstract ControlNode getSource();

  /** Return the control node this edge goes to.
   * @pure
   */
  public abstract ControlNode getSink();

  /** Return whether this edge is the *second* edge coming out of
   * the source.  That is ((TwoOutput)getSource()).getOutput2() == this
   * @pure
   */
  public abstract boolean sourceIsSecondary();

  /** Return whether this edge is the *second* edge going into
   * the sink.  That is ((TwoInput)getSource()).getInput2() == this
   * @pure
   */
  public abstract boolean sinkIsSecondary();

  /** Set the source of an edge.
   * (Used only in initialization by attach and friends.)
   */
  protected abstract void setSource(ControlNode source, boolean secondary)
       throws EdgeLinkageError;

  /** Set the sink of an edge.
   * (Used only in initialization by attach and friends.)
   */
  protected abstract void setSink(ControlNode sink, boolean secondary) 
       throws EdgeLinkageError;

  /** Attach the edge to the input/output slots of the nodes.
   * If this edge is the second output of a node or a second
   * input to a node, record that it is such.
   * @exception EdgeLinkageError
   *            Edge already has sources and sinks
   *            Nodes already taken.
   * @precondition NonNull(source)
   * @precondition NonNull(sink)
   * @precondition okSourceSink(source,sink)
   */
  protected void attach(ControlNode source, ControlNode sink)
       throws EdgeLinkageError
  {
    attachSource(source);
    attachSink(sink);
  }

  /** Attach the edge to a source.
   * @exception EdgeLinkageError
   *    if source does not have outputs;
   *    if source outputs are full
   */
  protected void attachSource(ControlNode source)
       throws EdgeLinkageError
  {
    if (source instanceof OneOutput) {
      OneOutput ns = (OneOutput) source;
      if (ns.getOutput() != null) {
        throw new EdgeLinkageError(SOURCE_ALREADY_TAKEN);
      } else {
	setSource(source,false);
	ns.setOutput(this);
      }
    } else if (source instanceof TwoOutput) {
      TwoOutput ns = (TwoOutput) source;
      if (ns.getOutput1() != null) {
	if (ns.getOutput2() != null) {
	  throw new EdgeLinkageError(SOURCE_FULL);
	} else {
	  setSource(source,true);
	  ns.setOutput2(this);
	}
      } else {
	setSource(source,false);
	ns.setOutput1(this);
      }
    } else {
      throw new EdgeLinkageError(SOURCE_TYPE_NOT_SUPPORTED);
    }
  }


  /** Attach the edge to a sink.
   * @exception EdgeLinkageError
   *    if sink does not have inputs;
   *    if sink inputs are full
   */
  protected void attachSink(ControlNode sink)
       throws EdgeLinkageError
  {
    if (sink instanceof OneInput) {
      OneInput ns = (OneInput) sink;
      if (ns.getInput() != null) {
        throw new EdgeLinkageError("sink already taken");
      } else {
	setSink(sink,false);
	ns.setInput(this);
      }
    } else if (sink instanceof TwoInput) {
      TwoInput ns = (TwoInput) sink;
      if (ns.getInput1() != null) {
	if (ns.getInput2() != null) {
	  throw new EdgeLinkageError("sink full");
	} else {
	  setSink(sink,true);
	  ns.setInput2(this);
	}
      } else {
	setSink(sink,false);
	ns.setInput1(this);
      }
    } else {
      throw new EdgeLinkageError("sink does not have inputs");
    }
  }

  protected void detachSource() {
	  MutableControlNode n = (MutableControlNode)getSource();
	  n.resetOutput(this);
	  setSource(null,false);
  }
  
  protected void detachSink() {
	  MutableControlNode n = (MutableControlNode)getSink();
	  n.resetInput(this);
	  setSink(null,false);
  }
  
  protected void detach() {
	  detachSource();
	  detachSink();
  }
  
  /** A useful shortcut: connect two nodes
   * with a SimpleControlEdge (q.v.)
   * @see SimpleControlEdge
   */
  public static void connect(ControlNode source, ControlNode sink) {
    new SimpleControlEdge(source,sink);
  }
  
}

