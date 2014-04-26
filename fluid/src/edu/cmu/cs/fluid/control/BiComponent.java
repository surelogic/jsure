package edu.cmu.cs.fluid.control;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidRuntimeException;
import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;

/**
 * A component that also serves as a subcomponent in its parent.
 * This structure means the CFG is directly connected -- cannot be implicitly
 * versioned -- which makes it take less space and be more easily optimized for space.
 * @see BiComponentFactory
 */
public class BiComponent extends Component implements ISubcomponent {
	/**
	 * Logger for this class
	 */
	private static final Logger LOG = SLLogger.getLogger("FLUID.control");

	BiComponent(BiComponentFactory f, IRNode node) {
		super(node);
		registerFactory(f);
		registerEntryPort(createPort(WhichPort.ENTRY,0,0));
		registerNormalExitPort(createPort(WhichPort.NORMAL_EXIT,0,0));
		registerAbruptExitPort(createPort(WhichPort.ABRUPT_EXIT,0,0));
	}
	
	@Override
	public Component getComponent() {
		return getComponent(tree.getParent(syntax));
	}

	@Override
	public IRLocation getLocation() {
		return tree.getLocation(syntax);
	}

	@Override
	public Component getComponentInChild() {
		return this;
	}

	Collection<SubcomponentNode> nodes;

	@Override
	public void registerSubcomponentNode(SubcomponentNode n) {
		if (nodes == null) nodes = new ArrayList<SubcomponentNode>();
		nodes.add(n);
	}

	@Override
	public VariableSubcomponentControlEdge getVariableEdge(int index,
			boolean isEntry) {
		return null;
	}

	/**
	 * Take over all the connections for this component.
	 * @param c
	 */
	void assumeIdentity(Component c, boolean quiet) {
		entryPort = augmentPort((BiPort)entryPort,c.getEntryPort());
		normalExitPort = augmentPort((BiPort)normalExitPort,c.getNormalExitPort());
		abruptExitPort = augmentPort((BiPort)abruptExitPort,c.getAbruptExitPort());
		BiComponentFactory fact = (BiComponentFactory)factory;
		// make sure all variable subcomponents are created
		if (c instanceof VariableComponent) {
			replaceVariableSubcomponentEdges(c.getVariableSubcomponent());
			for (IRLocation loc = tree.firstChildLocation(syntax); loc != null; loc = tree.nextChildLocation(syntax, loc)) {
				c.getSubcomponent(loc);
			}
		}
		// now grab all subcomponents
		for (Map.Entry<IRLocation,ISubcomponent> e : c.subcomponents.entrySet()) {
			if (e.getKey() == null) continue; // no BiComponent should be created for it
			BiComponent sub = fact.getBiComponent(e.getValue().getSyntax(), quiet);
			sub.assumeIdentity(e.getValue());
			registerSubcomponent(e.getKey(),sub);
		}
		if (c.componentNodes != null) {
			for (ComponentNode n : c.componentNodes) {
				if (n instanceof MutableComponentNode) {
					((MutableComponentNode)n).setComponent(this);
					registerComponentNode(n);
				} else {
					LOG.warning("Not a mutable CN? " + n);
				}
			}
			c.componentNodes = null;
		}
	}
	
	/**
	 * Take over all the connections for this subcomponent.
	 * @param s
	 */
	void assumeIdentity(ISubcomponent s) {
		entryPort = augmentPort((BiPort)entryPort,s.getEntryPort());
		normalExitPort = augmentPort((BiPort)normalExitPort,s.getNormalExitPort());
		abruptExitPort = augmentPort((BiPort)abruptExitPort,s.getAbruptExitPort());
		
		if (s instanceof Subcomponent) {
			Subcomponent oldSub = (Subcomponent)s;
			if (oldSub.nodes != null) {
				for (SubcomponentNode n : oldSub.nodes) {
					if (n instanceof MutableSubcomponentNode) {
						((MutableSubcomponentNode)n).setSubcomponent(this);
						registerSubcomponentNode(n);
					} else {
						LOG.warning("Not a mutable CN? " + n);
					}
				}
				oldSub.nodes = null;
			}
			if (oldSub instanceof VariableSubcomponent) {
				replaceVariableSubcomponentEdges((VariableSubcomponent)oldSub);
			}
		} else {
			LOG.warning("What sort of subcomponent is this? " + s);
		}
	}
	
	/**
	 * Find all VariableSubcomponentControlEdge instances for this VSC
	 * and replace with normal edges.
	 * @param vsc must not be null
	 */
	protected void replaceVariableSubcomponentEdges(VariableSubcomponent vsc) {
		int n = vsc.getNumEdges();
		for (boolean b=false; ;b = true) {
			for (int i=0; i < n; ++i) {
				ControlEdge vce = vsc.getVariableEdge(i, b);
				ControlNode source = vce.getSource();
				ControlNode sink = vce.getSink();
				vce.detach();
				ControlEdge.connect(source, sink);
			}
			if (b) break;
		}
	}
	
	private static class ArrayControlEdgeIterator extends ControlEdgeIterator {
		private final ControlEdge[] edges;
		private final int bound;
		private int next;
		
		public ArrayControlEdgeIterator(ControlEdge[] es, int start, int stop) {
			edges = es;
			bound = stop;
			next = start;
		}
		@Override
		public boolean hasNext() {
			return next < bound;
		}
		@Override
		public ControlEdge nextControlEdge() throws NoSuchElementException {
			if (!hasNext()) throw new NoSuchElementException("done");
			return edges[next++];
		}
	}
	
	abstract class BiPort extends Entity implements IInputPort, IOutputPort, ComponentPort, SubcomponentPort, MutableControlNode {
		final WhichPort kind;
		final ControlEdge[] edges;
		final int numInput, numOutput;
		public BiPort(WhichPort k, int in, int out) {
			kind = k;
			numInput = in;
			numOutput = out;
			edges = new ControlEdge[in+out];
		}
		@Override
		public edu.cmu.cs.fluid.control.Port getDual() {
			return this;
		}
		@Override
		public IRNode getSyntax() {
			return BiComponent.this.getSyntax();
		}
		@Override
		public ISubcomponent getSubcomponent() {
			return BiComponent.this;
		}
		@Override
		public ControlEdgeIterator getInputs() {
			return new ArrayControlEdgeIterator(edges,0,numInput);
		}
		@Override
		public ControlEdgeIterator getOutputs() {
			return new ArrayControlEdgeIterator(edges,numInput,edges.length);
		}
		@Override
		public Component getComponent() {
			return BiComponent.this;
		}
			
		@Override
		public WhichPort which() {
			return kind;
		}

		
		/// Input methods
		
		public ControlEdge getInput() {
			assert numInput == 1;
			return edges[0];
		}
		
		public void setInput(ControlEdge input) {
			assert numInput == 1;
			edges[0] = input;
		}

		public ControlEdge getInput1() {
			assert numInput == 2;
			return edges[0];
		}
		public ControlEdge getInput2() {
			assert numInput == 2;
			return edges[1];
		}
		
		public ControlEdge getInput(boolean secondary) {
			assert numInput == 2;
			return edges[secondary ? 1 : 0];
		}
		
		public void setInput1(ControlEdge input1) {
			assert numInput == 2;
			if (edges[0] != null) {
				throw new EdgeLinkageError("Input1 already set");
			}
			edges[0] = input1;
		}
		
		public void setInput2(ControlEdge input2) {
			assert numInput == 2;
			if (edges[1] != null) {
				throw new EdgeLinkageError("Input2 already set");
			}
			edges[1] = input2;
		}

		
		/// Input methods
		
		public ControlEdge getOutput() {
			assert numOutput == 1;
			return edges[numInput+0];
		}
		
		public void setOutput(ControlEdge output) {
			assert numOutput == 1;
			edges[numInput+0] = output;
		}

		public ControlEdge getOutput1() {
			assert numOutput == 2;
			return edges[numInput+0];
		}
		public ControlEdge getOutput2() {
			assert numOutput == 2;
			return edges[numInput+1];
		}
		
		public ControlEdge getOutput(boolean secondary) {
			assert numOutput == 2;
			return edges[numInput+(secondary ? 1 : 0)];
		}
		
		public void setOutput1(ControlEdge output1) {
			assert numOutput == 2;
			if (edges[numInput+0] != null) {
				throw new EdgeLinkageError("Output1 already set");
			}
			edges[numInput+0] = output1;
		}
		
		public void setOutput2(ControlEdge output2) {
			assert numOutput == 2;
			if (edges[numInput+1] != null) {
				throw new EdgeLinkageError("Output2 already set");
			}
			edges[numInput+1] = output2;
		}
		
		
		/// Mutation methods
		
		
		@Override
		public void resetInput(ControlEdge e) {
			for (int i=0; i < numInput; ++i) {
				if (edges[i] == e) {
					edges[i] = null;
					return;
				}
			}
			throw new EdgeLinkageError("Not an incoming edge: " + e);
		}
		
		@Override
		public void resetOutput(ControlEdge e) {
			for (int i=0; i < numOutput; ++i) {
				if (edges[numInput+i] == e) {
					edges[numInput+i] = null;
					return;
				}
			}
			throw new EdgeLinkageError("Not an Outgoing edge: " + e);
		}
		
		
		/** Take the edges from an existing port.
		 * The existing port can be a BiPort or not.
		 * But this BiPort must not have corresponding edges.  
		 * @param p port to take from, must not be null.
		 */
		public void stealEdges(Port p) {
			if (p instanceof OneInput) {
				ControlEdge e = ((OneInput)p).getInput();
				e.detachSink();
				e.attachSink(this);
			} else if (p instanceof TwoInput) {
				ControlEdge e1 = ((TwoInput)p).getInput1();
				ControlEdge e2 = ((TwoInput)p).getInput2();
				e1.detachSink();
				e2.detachSink();
				e1.attachSink(this);
				e2.attachSink(this);				
			}
			if (p instanceof OneOutput) {
				ControlEdge e = ((OneOutput)p).getOutput();
				e.detachSource();
				e.attachSource(this);				
			} else if (p instanceof TwoOutput) {
				ControlEdge e1 = ((TwoOutput)p).getOutput1();
				ControlEdge e2 = ((TwoOutput)p).getOutput2();
				e1.detachSource();
				e2.detachSource();
				e1.attachSource(this);
				e2.attachSource(this);			
			}
		}
    }
	
	// unfortunately, we have to create a lot of classes:
	
	private class Port00 extends BiPort implements NoInput, NoOutput { Port00(WhichPort k) { super(k,0,0); } }
	private class Port01 extends BiPort implements NoInput, OneOutput { Port01(WhichPort k) { super(k,0,1); } }
	private class Port02 extends BiPort implements NoInput, TwoOutput { Port02(WhichPort k) { super(k,0,2); } }
	private class Port10 extends BiPort implements OneInput, NoOutput { Port10(WhichPort k) { super(k,1,0); } }
	private class Port11 extends BiPort implements OneInput, OneOutput { Port11(WhichPort k) { super(k,1,1); } }
	private class Port12 extends BiPort implements OneInput, TwoOutput { Port12(WhichPort k) { super(k,1,2); } }
	private class Port20 extends BiPort implements TwoInput, NoOutput { Port20(WhichPort k) { super(k,2,0); } }
	private class Port21 extends BiPort implements TwoInput, OneOutput { Port21(WhichPort k) { super(k,2,1); } }
	private class Port22 extends BiPort implements TwoInput, TwoOutput { Port22(WhichPort k) { super(k,2,2); } }

	private BiPort createPort(WhichPort k, int in, int out) {
		switch (in*10+out) {
		case 00: return new Port00(k);
		case 01: return new Port01(k);
		case 02: return new Port02(k);
		case 10: return new Port10(k);
		case 11: return new Port11(k);
		case 12: return new Port12(k);
		case 20: return new Port20(k);
		case 21: return new Port21(k);
		case 22: return new Port22(k);
		default:
		}
		LOG.severe("CreatePort(" + k + "," + in + "," + out + ")");
		return null;
	}

	/**
	 * Create a new BiPort that takes all the edges from an existing BiPort
	 * and from another Port.
	 * @param init
	 * @param source
	 * @return
	 */
	protected BiPort augmentPort(BiPort init, Port source) {
		assert init.which() == source.which();
		final int newIn, newOut;
		if (source instanceof IInputPort) {
			newIn = init.numInput;
			if (init.numOutput != 0) {
				throw new FluidRuntimeException("BiPort already has outputs: " + init + " for " + source);
			}
			if (source instanceof NoOutput) return init;
			if (source instanceof OneOutput) {
				newOut = 1;
			} else if (source instanceof TwoOutput) {
				newOut = 2;
			} else {
				throw new FluidRuntimeException("unknown InputPort: " + source);
			}
		} else if (source instanceof IOutputPort) {
			newOut = init.numOutput;
			if (init.numInput != 0) {
				throw new FluidRuntimeException("BiPort already has inputs: " + init + " for " + source);
			}
			if (source instanceof NoInput) return init;
			if (source instanceof OneInput) {
				newIn = 1;
			} else if (source instanceof TwoInput) {
				newIn = 2;
			} else {
				throw new FluidRuntimeException("unknown OutputPort: " + source);
			}
		} else {
			throw new FluidRuntimeException("unknown Port: " + source);
		}
		final BiPort result = createPort(init.kind,newIn,newOut);
		result.stealEdges(init);
		result.stealEdges(source);
		return result;
	}
	

}
