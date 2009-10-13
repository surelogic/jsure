/*
 * Created on Feb 3, 2004
 * Modified on Nov 1, 2004
 */
package edu.cmu.cs.fluid.analysis.cfg;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.analysis.util.AbstractFluidAnalysisModule;
import edu.cmu.cs.fluid.control.Abort;
import edu.cmu.cs.fluid.control.AddLabel;
import edu.cmu.cs.fluid.control.BlankInputPort;
import edu.cmu.cs.fluid.control.BlankOutputPort;
import edu.cmu.cs.fluid.control.Choice;
import edu.cmu.cs.fluid.control.Component;
import edu.cmu.cs.fluid.control.ComponentAbruptExitPort;
import edu.cmu.cs.fluid.control.ComponentBlankAbruptExitPort;
import edu.cmu.cs.fluid.control.ComponentBlankEntryPort;
import edu.cmu.cs.fluid.control.ComponentBlankNormalExitPort;
import edu.cmu.cs.fluid.control.ComponentBooleanExitPort;
import edu.cmu.cs.fluid.control.ComponentChoice;
import edu.cmu.cs.fluid.control.ComponentEntryPort;
import edu.cmu.cs.fluid.control.ComponentFlow;
import edu.cmu.cs.fluid.control.ComponentNormalExitPort;
import edu.cmu.cs.fluid.control.ComponentPort;
import edu.cmu.cs.fluid.control.ComponentSink;
import edu.cmu.cs.fluid.control.ComponentSource;
import edu.cmu.cs.fluid.control.ControlEdgeIterator;
import edu.cmu.cs.fluid.control.ControlNode;
import edu.cmu.cs.fluid.control.DoubleInputPort;
import edu.cmu.cs.fluid.control.DoubleOutputPort;
import edu.cmu.cs.fluid.control.DynamicSplit;
import edu.cmu.cs.fluid.control.EntryPort;
import edu.cmu.cs.fluid.control.ExitPort;
import edu.cmu.cs.fluid.control.Flow;
import edu.cmu.cs.fluid.control.Fork;
import edu.cmu.cs.fluid.control.InputPort;
import edu.cmu.cs.fluid.control.Join;
import edu.cmu.cs.fluid.control.Merge;
import edu.cmu.cs.fluid.control.Never;
import edu.cmu.cs.fluid.control.NoOperation;
import edu.cmu.cs.fluid.control.OutputPort;
import edu.cmu.cs.fluid.control.PendingLabelStrip;
import edu.cmu.cs.fluid.control.Port;
import edu.cmu.cs.fluid.control.SimpleInputPort;
import edu.cmu.cs.fluid.control.SimpleOutputPort;
import edu.cmu.cs.fluid.control.Sink;
import edu.cmu.cs.fluid.control.Source;
import edu.cmu.cs.fluid.control.Split;
import edu.cmu.cs.fluid.control.Subcomponent;
import edu.cmu.cs.fluid.control.SubcomponentAbruptExitPort;
import edu.cmu.cs.fluid.control.SubcomponentBooleanExitPort;
import edu.cmu.cs.fluid.control.SubcomponentChoice;
import edu.cmu.cs.fluid.control.SubcomponentEntryPort;
import edu.cmu.cs.fluid.control.SubcomponentFlow;
import edu.cmu.cs.fluid.control.SubcomponentNormalExitPort;
import edu.cmu.cs.fluid.control.SubcomponentPort;
import edu.cmu.cs.fluid.control.TrackedDemerge;
import edu.cmu.cs.fluid.control.TrackedMerge;
import edu.cmu.cs.fluid.control.TwoInput;
import edu.cmu.cs.fluid.control.TwoOutput;
import edu.cmu.cs.fluid.eclipse.EclipseCodeFile;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaComponentFactory;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.operator.ClassBody;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodBody;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.java.operator.SomeFunctionDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeDeclarations;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.SourceCUDrop;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.AbstractRunner;
import edu.cmu.cs.fluid.util.Iteratable;
import edu.cmu.cs.fluid.util.QuickProperties;

/**
 * @author yangzhao
 */
public final class MakeCFGDiagram extends AbstractFluidAnalysisModule {
	// private static final String REPORT_CATEGORY = "CFG Diagram Maker";

	/**
	 * Log4j logger for this class
	 */
	private static final Logger LOG = SLLogger.getLogger("CFGMaker");

	private boolean fullBuildInProgress = false;

	private boolean doingFullProjectPass = false;

	private static MakeCFGDiagram INSTANCE;

	private static String OUTPUT_FORMAT = "png";

	private static boolean SAVEDOTFILE = true;

	private static String OUTPUT_DIRECTORY = ".";

	private static String DOT = "dot";

	// By default, we create a cfg graph without ports and components.
	private static boolean NO_PORTS = true;

	public static MakeCFGDiagram getInstance() {
		return INSTANCE;
	}

	private static void setInstance(MakeCFGDiagram me) {
		INSTANCE = me;
	}

	public MakeCFGDiagram() {
		setInstance(this);
		/*
		 * Read CFG diagram settings from Fluid system properties
		 */
		Properties fluidProperties = QuickProperties.getInstance()
				.getProperties();
		String value;
		value = fluidProperties.getProperty("cfgdiagram.output.format");
		if ((value != null)
				&& ("png".equals(value) || "jpg".equals(value)
						|| "svg".equals(value) || "ps".equals(value)
						|| "fig".equals(value) || "gif".equals(value) || "pic"
						.equals(value))) {
			OUTPUT_FORMAT = value;
		}
		value = fluidProperties.getProperty("cfgdiagram.savedotfile");
		if ((value != null) && ("yes".equals(value) || "YES".equals(value))) {
			SAVEDOTFILE = true;
		}
		value = fluidProperties.getProperty("cfgdiagram.output.directory");
		if (value != null) {
			OUTPUT_DIRECTORY = value;
		}
		value = fluidProperties.getProperty("cfgdiagram.ports");
		if (value != null && ("yes".equals(value) || "YES".equals(value))) {
			NO_PORTS = false;
		}
		value = fluidProperties.getProperty("cfgdiagram.dot");
		if (value != null) {
			DOT = value;
		}
	}

	@Override
	public void resetForAFullBuild(IProject project) {
		fullBuildInProgress = true;
	}

	@Override
	public void analyzeBegin(IProject project) {
		super.analyzeBegin(project);
		doingFullProjectPass = fullBuildInProgress;
	}

	@Override
	public boolean analyzeResource(IResource resource, int kind) {
		return !doingFullProjectPass;
	}

	@Override
	protected void removeResource(IResource resource) {
		// Nothing to do
	}

	@Override
	public void analyzeCompilationUnit(ICompilationUnit file,
			org.eclipse.jdt.core.dom.CompilationUnit ast) {
		LOG.info("Draw CFG diagram on " + file.getElementName());
		doAnalysisOnAFile(file);
	}

	@Override
	public IResource[] analyzeEnd(IProject project) {
		if (doingFullProjectPass) {
			doingFullProjectPass = fullBuildInProgress = false;
			return NONE_FURTHER;
		}
		doingFullProjectPass = true;
		return null; // the entire project needs to be analyzed again
	}

	private void doAnalysisOnAFile(final ICompilationUnit file) {

		javaFile = file;

		runInVersion(new AbstractRunner() {
			public void run() {
				SourceCUDrop drop = SourceCUDrop.queryCU(new EclipseCodeFile(
						file));

				if (drop != null) {
					javaFile = file;

					if (LOG.isLoggable(Level.FINE))
						LOG.fine("Drawing CFG diagram on file: "
								+ javaFile.getElementName());
					cfgDiagramMaker(drop.cu);

					javaFile = null;
				} else {
					LOG.warning("No IR drop found for "
							+ javaFile.getElementName());
				}
			}
		});

		javaFile = null;
	}

	private void cfgDiagramMaker(final IRNode compUnit) {
		try {
			final IRNode typeDecls = edu.cmu.cs.fluid.java.operator.CompilationUnit
					.getDecls(compUnit);
			final Iterator<IRNode> decls = TypeDeclarations
					.getTypesIterator(typeDecls);
			String className = "";
			String methodName = "";
			IRNode methodDecl = null;

			long cfgStartTime = System.currentTimeMillis();
			long cfgacctime = 0;

			while (decls.hasNext()) {
				final IRNode decl = decls.next();
				final Iterator<IRNode> nodes = JJNode.tree.topDown(decl);
				Operator decl_op = JJNode.tree.getOperator(decl);

				if (ClassDeclaration.prototype.equals(decl_op)) {
					className = ClassDeclaration.getId(decl);
					if (LOG.isLoggable(Level.FINE)) {
						LOG.fine("In Class: " + className);
					}
				} else {
					continue;
				}
				while (nodes.hasNext()) {
					IRNode currentNode = nodes.next();
					Operator op = JJNode.tree.getOperator(currentNode);
					if (ClassBody.prototype.equals(op)
							|| MethodBody.prototype.equals(op)) {
						String dotName = OUTPUT_DIRECTORY
								+ System.getProperty("file.separator")
								+ className;
						if (!MethodBody.prototype.equals(op)) {
							// Only create cfg for method or constructor, no cfg
							// for the class.
							continue;
						}
						dotName += "_"
								+ methodName
								+ "_"
								+ getFormalsTypeStr(SomeFunctionDeclaration
										.getParams(methodDecl)) + "_";
						dotName += ((methodName != className) ? (getReturnTypeStr(MethodDeclaration
								.getReturnType(methodDecl)))
								: methodName);
						dotName += ".dot";
						File dotFile = new File(dotName);
						String cfgName = dotName.replaceAll("\\.dot", "."
								+ OUTPUT_FORMAT);

						PrintWriter out = new PrintWriter(new BufferedWriter(
								new FileWriter(dotFile)));
						out.println("digraph G {");
						long temptime = System.currentTimeMillis();
						String cfgStr = makeCFG(currentNode);
						cfgacctime += (System.currentTimeMillis() - temptime);
						out.println(cfgStr);
						out.println("}");
						out.close();
						LOG.info("created CFG digraph in " + dotFile);
						String cmd = DOT + " -T" + OUTPUT_FORMAT + " -o"
								+ cfgName + " " + dotName;
						LOG.info(cmd);
						Process proc = null;
						try {
							proc = Runtime.getRuntime().exec(cmd);
							if (proc.waitFor() != 0) {
								// System.out.println("Can not create the cfg
								// files.");
							}
						} catch (SecurityException e) {
							LOG
									.info("The system does not allow creation of the dot subprocess.");
						} catch (IOException e) {
							LOG.info("Can not find the dot command.");
						} catch (InterruptedException e) {
							LOG.info("Unknown exception from cfg-maker.");
						}
						if (!SAVEDOTFILE) {
							dotFile.delete();
						}
					} else if (MethodDeclaration.prototype.equals(op)) {
						methodName = MethodDeclaration.getId(currentNode);
						methodDecl = currentNode;
					} else if (ConstructorDeclaration.prototype.equals(op)) {
						methodName = ConstructorDeclaration.getId(currentNode);
						methodDecl = currentNode;
					} else {
						continue;
					}

				}
			}
			System.err.println("cfg browser took " + cfgacctime + " ms");
			System.err.println("cfg maker took "
					+ (System.currentTimeMillis() - cfgStartTime) + " ms");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Exception in Cfg diagram maker", e);
		}
	}

	String getFormalsTypeStr(IRNode paramsNode) {
		String formalsStr = "";
		Iteratable<IRNode> it = Parameters.getFormalIterator(paramsNode);
		boolean FIRST = true;
		while (it.hasNext()) {
			IRNode node = (it.next());
			if (!FIRST) {
				formalsStr += ".";
			}
			formalsStr += JavaNames.getTypeName(ParameterDeclaration
					.getType(node));
			FIRST = false;
		}
		return formalsStr;
	}

	String getReturnTypeStr(IRNode returnNode) {
		return JavaNames.getTypeName(returnNode);
	}

	/**
	 * Draw a CFG of a new method.
	 */
	public String makeCFG(IRNode irnode) {
		this.irnode = irnode;
		comp = JavaComponentFactory.getComponent(irnode, true);
		entry = comp.getEntryPort();
		normalExit = comp.getNormalExitPort();
		abruptExit = comp.getAbruptExitPort();
		visitedNodes = new ArrayList<ControlNode>();
		visitedEntrys = new Stack<ControlNode>();
		visitedNodesTable = new Hashtable<ControlNode, Integer>();
		visitedNodesInBackword = new HashSet<ControlNode>();
		visitedNodesEntryTable = new Hashtable<ControlNode, Stack<ControlNode>>();
		visitedDoubleInputDoubleOutputPort = new HashSet<ControlNode>();
		no = -1;
		return getCFGString();
	}

	/**
	 * The main method of this class.
	 * 
	 * @return All string of the cfg file.
	 */
	protected String getCFGString() {
		String cfg = "";
		cfg += "Entry [shape=ellipse,style=filled,color=red];\n" + "Entry -> ";
		int total_forward_node;
		if (NO_PORTS) {
			cfg += getNoPortCFGDiagramForward(entry, entry);
			total_forward_node = no;
			cfg += getNoPortCFGDiagramBackward(normalExit, null,
					total_forward_node);
			cfg += getNoPortCFGDiagramBackward(abruptExit, null,
					total_forward_node);
		} else {
			cfg += getCFGDiagramForward(entry);
			total_forward_node = no;
			cfg += getCFGDiagramBackward(normalExit, null, total_forward_node);
			cfg += getCFGDiagramBackward(abruptExit, null, total_forward_node);
		}
		cfg += "\"NORMAL EXIT\" -> \"ABRUPT EXIT\" [arrowhead = none style = \"dashed\"]\n";
		cfg += "\"NORMAL EXIT\" [shape=ellipse,style=filled,color=red];\n";
		cfg += "\"ABRUPT EXIT\" [shape=ellipse,style=filled,color=red];\n";
		cfg = decorate(visitedNodes) + cfg;
		return cfg;
	}

	/**
	 * Functions: 1. remove the number in Ports and make Ports ellipse shape; 2.
	 * make Join inverse-triangle shape and lightgrey color; 3. make Split
	 * diamond shape and lightgrey color;
	 * 
	 * @param nodeList
	 *            The all visited nodes.
	 * @return String to decroate above in the dot files.
	 */
	private String decorate(List<ControlNode> nodeList) {
		String labelport = "";
		String labeljoin = "";
		String labelsplit = "";
		for (int i = 0; i < nodeList.size(); i++) {
			if (nodeList.get(i) instanceof Join) {
				String str = getCFGDiagramFromJoin(nodeList.get(i));
				int index = labeljoin.indexOf(")" + str.substring(1));
				if (index < 0) {
					labeljoin += "node [shape=invtriangle,style=filled,color=lightgrey]; \"("
							+ i + ")" + str.substring(1) + ";\n";
				} else {
					String label1 = labeljoin.substring(0, index);
					String label2 = labeljoin.substring(index, labeljoin
							.length());
					labeljoin = label1 + ")" + str.substring(1) + "; \"(" + i
							+ label2;
				}
			} else if (nodeList.get(i) instanceof Split) {
				String str = getCFGDiagramFromSplit(nodeList.get(i));
				int index = labelsplit.indexOf(")" + str.substring(1));
				if (index < 0) {
					labelsplit += "node [shape=diamond,style=filled,color=lightgrey]; \"("
							+ i + ")" + str.substring(1) + ";\n";
				} else {
					String label1 = labelsplit.substring(0, index);
					String label2 = labelsplit.substring(index, labelsplit
							.length());
					labelsplit = label1 + ")" + str.substring(1) + "; \"(" + i
							+ label2;
				}
			} else if ((!NO_PORTS) && (nodeList.get(i) instanceof Port)) {
				String str = getCFGDiagramFromPort(nodeList.get(i));
				int index = labelport.indexOf(")" + str.substring(1));
				if (index < 0) {
					labelport += "node [shape=ellipse,style=unfilled]; {node [label="
							+ str
							+ "] \"("
							+ i
							+ ")"
							+ str.substring(1)
							+ ";};\n";
				} else {
					String label1 = labelport.substring(0, index);
					String label2 = labelport.substring(index, labelport
							.length());
					labelport = label1 + ")" + str.substring(1) + "; \"(" + i
							+ label2;
				}
			} else {
				// Do nothing.
			}

		}
		return labelport + labeljoin + labelsplit
				+ "node [shape=box,style=filled,color=lightgrey];\n";
	}

	protected int getIndex(ControlNode node,
			Hashtable<ControlNode, Integer> table) {
		Integer in = table.get(node);
		return (in == null ? -1 : in.intValue());
	}

	/**
	 * Get the Non-ports CFG string from a single node and its previous nodes,
	 * this method will be called recursively.
	 * 
	 * @param node
	 *            current node
	 * @param next
	 *            current node's direct offspring
	 * @param num
	 *            the number of node before calling this method
	 * @return
	 */
	protected String getNoPortCFGDiagramBackward(ControlNode node,
			ControlNode next, int num) {
		String cfg = "";
		ControlNode currentNode = node;
		ControlNode nextNode = next;
		String currentNodeStr = "";
		String nextNodeStr = "";

		if (nextNode == null) {
			// The currentNode should be normalExit or abruptExit.
			if (getIndex(currentNode, visitedNodesTable) < 0) {
				// This normalExit(or abruptExit) has not been visited.
				currentNodeStr = getCFGDiagramStr(currentNode, ++no);
				visitedNodes.add(currentNode);
				visitedNodesTable.put(currentNode, new Integer(no));
				cfg += currentNodeStr + " -> \""
						+ (currentNode == normalExit ? "NORMAL" : "ABRUPT")
						+ " EXIT\"\n";
			}
			cfg += getNoPortCFGDiagramBackward(currentNode.getInputs()
					.nextControlEdge().getSource(), currentNode, num);
			return cfg;
		}

		if (currentNode == entry) {
			// Reach the root of the component, so stop here.
			return cfg;
		}

		if (!(currentNode instanceof Port)) {
			int index = getIndex(currentNode, visitedNodesTable);
			if (visitedNodesInBackword.contains(currentNode)) {
				if (index <= num) {
					// This currentNode has been visited in the forward and
					// backward process.
					return cfg;
				}
			} else {
				visitedNodesInBackword.add(currentNode);
			}

			int nextNodeIndex = getIndex(nextNode, visitedNodesTable);
			if (index >= 0 && index <= num) {
				// This node has been visited during forward visit.
				ControlEdgeIterator cee = currentNode.getInputs();
				while (cee.hasNext()) {
					cfg += getNoPortCFGDiagramBackward(cee.nextControlEdge()
							.getSource(), currentNode, num);
				}
			} else {
				// Possibility:
				// 1. This node has been visited in neither forward nor
				// backward: index < 0
				// 2. This node has not been visited in forward visit, but has
				// been visited in the backward: index >num
				/*
				 * NOTICE: The visitedNodes.get( nextNodeIndex ) and nextNode
				 * may be different objects which are dual of each other
				 */
				nextNodeStr = getCFGDiagramStr(nextNode, nextNodeIndex);
				if (index < 0) {
					currentNodeStr = getCFGDiagramStr(currentNode, ++no);
					visitedNodes.add(currentNode);
					visitedNodesTable.put(currentNode, new Integer(no));

					cfg += currentNodeStr + " -> " + nextNodeStr + "\n";
					if (!(currentNode instanceof Source)) {
						ControlEdgeIterator cee = currentNode.getInputs();
						while (cee.hasNext()) {
							cfg += getNoPortCFGDiagramBackward(cee
									.nextControlEdge().getSource(),
									currentNode, num);
						}
					}
				} else {
					currentNodeStr = getCFGDiagramStr(currentNode, index);
					cfg += currentNodeStr + " -> " + nextNodeStr + "\n";
				}
			}
		} else {
			// The currentNode is a port.
			ControlEdgeIterator cee = currentNode.getInputs();
			while (cee.hasNext()) {
				cfg += getNoPortCFGDiagramBackward(cee.nextControlEdge()
						.getSource(), nextNode, num);
			}
		}
		return cfg;
	}

	/**
	 * Get the CFG string from a single node and its previous nodes, this method
	 * will be called recursively.
	 * 
	 * @param node
	 *            current node
	 * @param next
	 *            current node's direct offspring
	 * @param num
	 *            the number of node before calling this method
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected String getCFGDiagramBackward(ControlNode node, ControlNode next,
			int num) {
		String cfg = "";
		ControlNode currentNode = node;
		ControlNode nextNode = next;
		Stack<ControlNode> nextStack;
		Stack<ControlNode> currentStack;
		String currentNodeStr = "";
		String nextNodeStr = "";

		if (currentNode == entry) {// Reach the root of the component, so stop.
			return cfg;
		}
		int index = getIndex(currentNode, visitedNodesTable);

		if (visitedNodesInBackword.contains(currentNode)) {
			if (index <= num) {
				// This currentNode has been visited in the forward and backward
				// process.
				return cfg;
			}
		} else {
			visitedNodesInBackword.add(currentNode);
		}

		if (nextNode == null) {
			// The currentNode should be normalExit or abruptExit
			if (index < 0) {
				// The normailExit(or abruptExit) has not been visited.
				// cfg += getCFGDiagramStr( currentNode, ++no )+ " -> \"" +
				// (currentNode==normalExit?"NORMAL":"ABRUPT") + " EXIT\"\n";
				currentNodeStr = getCFGDiagramStr(currentNode, ++no);
				cfg += "subgraph " + "cluster" + "_" + 0 + "{\n";// The entry
				// node is
				// always given 0.
				cfg += currentNodeStr + "\n}\n";
				cfg += currentNodeStr + " -> \""
						+ (currentNode == normalExit ? "NORMAL" : "ABRUPT")
						+ " EXIT\"\n";
				currentStack = new Stack<ControlNode>();
				currentStack.push(currentNode == normalExit ? abruptExit
						: normalExit);
				visitedNodes.add(currentNode);
				visitedNodesTable.put(currentNode, new Integer(no));
				visitedNodesEntryTable.put(currentNode, visitedNodesEntryTable
						.get(entry));
			}
			cfg += getCFGDiagramBackward(currentNode.getInputs()
					.nextControlEdge().getSource(), currentNode, num);
			return cfg;
		}

		int nextNodeIndex = getIndex(nextNode, visitedNodesTable);
		if (index >= 0 && index <= num) {// This node has been visited during
			// forward visit.
			ControlEdgeIterator cee = currentNode.getInputs();
			while (cee.hasNext()) {
				cfg += getCFGDiagramBackward(cee.nextControlEdge().getSource(),
						currentNode, num);
			}
		} else {
			// Possibility: 1. This node has been visited in neither forward nor
			// backward. index < 0
			// 2. This node has not been visited in forward visit, but has been
			// visited in the backward, index >num
			/*
			 * NOTICE: The visitedNodes.get( nextNodeIndex ) and nextNode may be
			 * different objects which are dual of each other
			 */
			nextNodeStr = getCFGDiagramStr(nextNode, nextNodeIndex);
			if (index < 0) {
				currentNodeStr = getCFGDiagramStr(currentNode, ++no);
				visitedNodes.add(currentNode);
				visitedNodesTable.put(currentNode, new Integer(no));
			} else {
				currentNodeStr = getCFGDiagramStr(currentNode, index);
			}
			nextStack = visitedNodesEntryTable.get(nextNode);
			currentStack = (Stack<ControlNode>) nextStack.clone();

			if (currentNode instanceof EntryPort) {
				throw new FluidError("Find EntryPort in backward process:"
						+ node);
			} else if (currentNode instanceof ExitPort) {
				currentStack.push(currentNode);
				if (currentNode instanceof SubcomponentPort) {
					ControlNode entrynode = ((SubcomponentPort) currentNode)
							.getSubcomponent().getEntryPort();
					if (entrynode != null) {
						currentStack = visitedNodesEntryTable.get(entrynode);
					}
				} else if (currentNode instanceof ComponentPort) {
					ControlNode entrynode = ((ComponentPort) currentNode)
							.getComponent().getEntryPort();
					if (entrynode != null) {
						currentStack = visitedNodesEntryTable.get(entrynode);
					}
				} else {
					// do nothing.
				}
				cfg += subgraphHead(currentStack);
				cfg += currentNodeStr + "\n}\n";
				cfg += currentNodeStr + " -> " + nextNodeStr + "\n";
				cfg += subgraphTail(currentStack.size() - 1);

			} else {
				cfg += subgraphHead(currentStack);
				cfg += currentNodeStr + " -> " + nextNodeStr + "\n";
				cfg += subgraphTail(currentStack.size());
			}
			visitedNodesEntryTable.put(currentNode, currentStack);

			if (index < 0) {
				if (!(currentNode instanceof Source)) {
					ControlEdgeIterator cee = currentNode.getInputs();
					while (cee.hasNext()) {
						cfg += getCFGDiagramBackward(cee.nextControlEdge()
								.getSource(), currentNode, num);
					}
				}
			}
		}
		return cfg;
	}

	private String subgraphHead(Stack<ControlNode> curStack) {
		String head = "";
		for (int i = 0; i < curStack.size(); i++) {
			head += "subgraph " + "cluster" + "_"
					+ visitedNodesTable.get(curStack.elementAt(i)) + "{\n";
		}
		return head;
	}

	private String subgraphTail(int len) {
		String tail = "";
		for (int i = 0; i < len; i++)
			tail += "}";
		return (tail + "\n");
	}

	/**
	 * Get cfg String starting from this ControlNode.
	 */
	protected String getNoPortCFGDiagramForward(ControlNode curr,
			ControlNode pre) {
		String cfg = "";

		ControlNode currentNode = curr;
		String currentNodeStr = "Error of Curr";

		ControlNode preNode = pre;
		String preNodeStr = "Error of Pre";

		if (currentNode == entry || currentNode == normalExit
				|| currentNode == abruptExit) {
			currentNodeStr = getCFGDiagramStr(currentNode, ++no);
			visitedNodes.add(currentNode);
			visitedNodesTable.put(currentNode, new Integer(no));
			if (currentNode == normalExit || currentNode == abruptExit) {
				return (currentNode == normalExit ? "\"NORMAL" : "\"ABRUPT")
						+ " EXIT\"\n";
			}
		}

		if (!(currentNode instanceof Port)) {
			int index = getIndex(currentNode, visitedNodesTable);
			if (index >= 0) {
				// currentNode has been visited before, so it should be a kind
				// of "Merge".
				cfg += getCFGDiagramStr(currentNode, index) + "; \n";
				return cfg;
			} else {
				// currentNode has not been visited before.
				currentNodeStr = getCFGDiagramStr(currentNode, ++no);
				visitedNodes.add(currentNode);
				visitedNodesTable.put(currentNode, new Integer(no));

				if (currentNode instanceof Sink) {
					cfg += currentNodeStr + "\n\n";
					return cfg;
				} else {
					cfg += currentNodeStr + "\n" + currentNodeStr + " -> ";
				}
			}
		}

		ControlEdgeIterator cee = currentNode.getOutputs();
		if (currentNode instanceof Port) {

			// In the case of TwoInput Port with TwoOutput of its dual, we must
			// treat it carefully
			if (currentNode instanceof TwoInput
					&& ((Port) currentNode).getDual() instanceof TwoOutput) {
				if (!visitedDoubleInputDoubleOutputPort.contains(currentNode)) {
					visitedDoubleInputDoubleOutputPort.add(currentNode);
					// The first time, we trace the first edge.
					cfg += getNoPortCFGDiagramForward(cee.nextControlEdge()
							.getSink(), preNode);
				} else {
					cee.nextControlEdge().getSink();
					// The second time, we trace the second edge.
					cfg += getNoPortCFGDiagramForward(cee.nextControlEdge()
							.getSink(), preNode);
				}
			} else {
				if (cee.hasNext()) {
					cfg += getNoPortCFGDiagramForward(cee.nextControlEdge()
							.getSink(), preNode);
					if (cee.hasNext()) {
						preNodeStr = getCFGDiagramStr(preNode,
								visitedNodesTable.get(preNode));
						cfg += preNodeStr
								+ " -> "
								+ getNoPortCFGDiagramForward(cee
										.nextControlEdge().getSink(), preNode);
					}
				}
			}
		} else {
			if (cee.hasNext()) {
				cfg += getNoPortCFGDiagramForward(cee.nextControlEdge()
						.getSink(), currentNode);
				if (cee.hasNext()) {
					cfg += currentNodeStr
							+ " -> "
							+ getNoPortCFGDiagramForward(cee.nextControlEdge()
									.getSink(), currentNode);
				}
			}
		}
		return cfg;
	}

	protected String getCFGDiagramForward(ControlNode node) {
		String cfg = "";
		ControlNode currentNode = node;

		if (currentNode == normalExit || currentNode == abruptExit) {
			String currentNodeStr = getCFGDiagramStr(currentNode, ++no);
			visitedNodes.add(currentNode);
			visitedNodesTable.put(currentNode, new Integer(no));
			visitedNodesEntryTable.put(currentNode, visitedNodesEntryTable
					.get(entry));

			// Both normalExit and abruptExit are a kind of ExitPort, so we must
			// deal
			// with it according to the method in below, which is pop() and add
			// "}" to
			// the cfg.
			visitedEntrys.pop();
			return currentNodeStr + "\n}\n" + currentNodeStr + " -> \""
					+ (currentNode == normalExit ? "NORMAL" : "ABRUPT")
					+ " EXIT\"\n";
		}

		Integer in = visitedNodesTable.get(currentNode);
		int index = (in == null ? -1 : in.intValue());

		if (index >= 0) { // currentNode has been visited before, so it should
			// be a
			// kind of "Merge".
			cfg += getCFGDiagramStr(currentNode, index) + "; \n";
			// We need not go to visit the node again(because we have visited it
			// before).
			cfg += subgraphTail(visitedEntrys.size());
			visitedEntrys.clear();
		} else {

			String currentNodeStr = getCFGDiagramStr(currentNode, ++no);

			// Save the current visitedEntrys for the use of Split. If
			// currentNode is
			// not a Split, this will not be used.
			@SuppressWarnings("unchecked")
			Stack<ControlNode> currentEntrys = (Stack<ControlNode>) visitedEntrys
					.clone();
			visitedNodes.add(currentNode);
			visitedNodesTable.put(currentNode, new Integer(no));

			if (currentNode instanceof EntryPort) {// If it is a EntryPort,
				// push it
				// to the visitedEntrys and add
				// "subgraph cluster_xxx {" to the
				// cfg string
				visitedEntrys.push(currentNode);
				currentEntrys.push(currentNode);
				cfg += currentNodeStr + "\n" + "subgraph " + "cluster" + "_"
						+ no + "{\n" + currentNodeStr + " -> ";
			} else if (currentNode instanceof ExitPort) {// If it is a
				// ExitPort, pop
				// a stuff from the
				// visitedEntrys and add "}"
				// to the cfg string.
				visitedEntrys.pop();
				cfg += currentNodeStr + "\n}\n" + currentNodeStr + " -> ";
			} else if (currentNode instanceof Sink) {
				visitedEntrys.pop();
				cfg += currentNodeStr + "\n}\n";
			} else {// If it is neither EntryPort nor ExitPort, just do it
				// normally.
				cfg += currentNodeStr + "\n" + currentNodeStr + " -> ";
			}

			visitedNodesEntryTable.put(currentNode, currentEntrys);

			if (!(currentNode instanceof Sink)) {
				ControlEdgeIterator cee = currentNode.getOutputs();
				if (cee.hasNext()) {// cee is a EmptyControlEdgeIterator.
					cfg += getCFGDiagramForward(cee.nextControlEdge().getSink());

					if (cee.hasNext()) {// The currentNode is a Split
						visitedEntrys = currentEntrys;
						for (int i = 0; i < currentEntrys.size(); i++) {
							cfg += "subgraph "
									+ "cluster"
									+ "_"
									+ visitedNodesTable.get(currentEntrys
											.elementAt(i)) + "{\n";
						}
						if (currentNode instanceof ExitPort) {// the
							// ComponentBooleanExitPort is
							// an exit node but have two
							// exit paths.
							visitedEntrys.pop();
							cfg += currentNodeStr
									+ "\n}\n"
									+ currentNodeStr
									+ " -> "
									+ getCFGDiagramForward(cee
											.nextControlEdge().getSink());
						} else {
							cfg += currentNodeStr
									+ " -> "
									+ getCFGDiagramForward(cee
											.nextControlEdge().getSink());
						}

					}
				}
			}
		}
		return cfg;
	}

	/**
	 * Get the cfg string from a ControlNode.
	 * 
	 * @param nodeNo
	 *            The visited order number of this ControlNode in the cfg maker.
	 */
	protected String getCFGDiagramStr(ControlNode node, int nodeNo) {
		String info = "";
		if (node instanceof Source) {
			info = getCFGDiagramFromSource(node);
		} else if (node instanceof Flow) {
			info = getCFGDiagramFromFlow(node);
		} else if (node instanceof Port) {
			info = getCFGDiagramFromPort(node);
		} else if (node instanceof Split) {
			info = getCFGDiagramFromSplit(node);
		} else if (node instanceof Join) {
			info = getCFGDiagramFromJoin(node);
		} else if (node instanceof Sink) {
			info = getCFGDiagramFromSink(node);
		} else {
			throw new FluidError("unknown Node " + node);
		}
		return "\"" + "(" + nodeNo + ")" + info.substring(1);
	}

	/**
	 * Get the cfg string from a Source which has no inputs
	 */
	protected String getCFGDiagramFromSource(ControlNode node) {
		String info = "";
		if (node instanceof Never) {
			info += "\"" + "Never\"";
		} else if (node instanceof ComponentSource) {
			info += "\"" + "ComponentSource\"";
		} else {
			throw new FluidError("unknown Flow " + node);
		}
		return info;
	}

	/**
	 * Get the cfg string from a flow which is one input and one output.
	 */
	protected String getCFGDiagramFromFlow(ControlNode node) {
		String info = "";
		if (node instanceof NoOperation) {
			info += "\"" + "NoOperation\"";
		} else if (node instanceof AddLabel) {
			info += "\"" + "AddLabel\"";
		} else if (node instanceof ComponentFlow) {
			Component comp = ((ComponentFlow) node).getComponent();
			IRNode irn = comp.getSyntax();
			info += "\"" + "ComponentFlow:" + DebugUnparser.toString(irn)
					+ "\"";
		} else if (node instanceof SubcomponentFlow) {
			Subcomponent subcomp = ((SubcomponentFlow) node).getSubcomponent();
			IRNode irn = subcomp.getSyntax();
			info += "\"" + "SubcomponentFlow:" + DebugUnparser.toString(irn)
					+ "\"";
		} else if (node instanceof PendingLabelStrip) {
			info += "\"" + "PendingLabelStrip\"";
		} else {
			throw new FluidError("unknown Flow " + node);
		}
		return info;
	}

	/**
	 * Get the cfg string from a port which may have zero,one or two input and
	 * zero,one or two output.
	 */
	protected String getCFGDiagramFromPort(ControlNode node) {
		String info = "";
		if (node instanceof InputPort) {
			if (node instanceof BlankInputPort) {
				if (node instanceof ComponentBlankEntryPort) {
					info += "\"" + "ComponentBlankEntryPort\"";
				} else {
					throw new FluidError("unknown Port " + node);
				}
			} else if (node instanceof SimpleInputPort) {
				if (node instanceof ComponentEntryPort) {
					info += "\"" + "ComponentEntryPort\"";
				} else if (node instanceof SubcomponentAbruptExitPort) {
					info += "\"" + "SubcomponentAbruptExitPort\"";
				} else if (node instanceof SubcomponentNormalExitPort) {
					info += "\"" + "SubcomponentNormalExitPort\" ";
				} else {
					throw new FluidError("unknown Port " + node);
				}
			} else if (node instanceof DoubleInputPort) {
				if (node instanceof SubcomponentBooleanExitPort) {
					info += "\"" + "SubcomponentBooleanExitPort\"";
				} else {
					throw new FluidError("unknown Port " + node);
				}
			} else {
				throw new FluidError("unknown Port " + node);
			}
		} else if (node instanceof OutputPort) {
			if (node instanceof BlankOutputPort) {
				if (node instanceof ComponentBlankAbruptExitPort) {
					info += "\"" + "ComponentBlankAbruptExitPort\"";
				} else if (node instanceof ComponentBlankNormalExitPort) {
					info += "\"" + "ComponentBlankNormalExitPort\"";
				} else {
					throw new FluidError("unknown Port " + node);
				}
			} else if (node instanceof SimpleOutputPort) {
				if (node instanceof ComponentAbruptExitPort) {
					info += "\"" + "ComponentAbruptExitPort\" ";
				} else if (node instanceof ComponentNormalExitPort) {
					info += "\"" + "ComponentNormalExitPort\"";
				} else if (node instanceof SubcomponentEntryPort) {
					info += "\"" + "SubcomponentEntryPort\"";
				} else {
					throw new FluidError("unknown Port " + node);
				}
			} else if (node instanceof DoubleOutputPort) {
				if (node instanceof ComponentBooleanExitPort) {
					info += "\"" + "ComponentBooleanExitPort\"";
				} else {
					throw new FluidError("unknown Port " + node);
				}
			} else {
				throw new FluidError("unknown Port " + node);
			}
		} else {
			throw new FluidError("unknown Port " + node);
		}
		return info;
	}

	/**
	 * Get the cfg string from a Split which is one input and two output.
	 */
	protected String getCFGDiagramFromSplit(ControlNode node) {
		String info = "";
		if (node instanceof Fork) {
			info += "\"" + "Fork\" ";
		} else if (node instanceof Choice) {
			if (node instanceof ComponentChoice) {
				Component comp = ((ComponentChoice) node).getComponent();
				IRNode irn = comp.getSyntax();
				info += "\"" + "ComponentChoice:" + DebugUnparser.toString(irn)
						+ "\"";
			} else if (node instanceof SubcomponentChoice) {
				Subcomponent subcomp = ((SubcomponentChoice) node)
						.getSubcomponent();
				IRNode irn = subcomp.getSyntax();
				info += "\"" + "SubcomponentChoice:"
						+ DebugUnparser.toString(irn) + "\"";
			} else {
				throw new FluidError("unknown Split " + node);
			}
		} else if (node instanceof DynamicSplit) {
			info += "\"" + "DynamicSplit\"";
		} else if (node instanceof TrackedDemerge) {
			info += "\"" + "TrackedDemerge\"";
		} else {
			throw new FluidError("unknown Split " + node);
		}
		return info;
	}

	/**
	 * Get the cfg string from a Join which is two input and one output.
	 */
	protected String getCFGDiagramFromJoin(ControlNode node) {
		String info = "";
		if (node instanceof Merge) {
			info += "\"" + "Merge\"";
		} else if (node instanceof TrackedMerge) {
			info += "\"" + "TrackedMerge\"";
		} else {
			throw new FluidError("unknown Join " + node);
		}
		return info;
	}

	/**
	 * Get the cfg string from a Sink which is one input and no output.
	 */
	protected String getCFGDiagramFromSink(ControlNode node) {
		String info = "";
		if (node instanceof Abort) {
			info += "\"" + "Abort\" ";
		} else if (node instanceof ComponentSink) {
			Component comp = ((ComponentSink) node).getComponent();
			IRNode irn = comp.getSyntax();
			info += "\"" + "ComponentSink:" + DebugUnparser.toString(irn)
					+ "\"";
		} else {
			throw new FluidError("unknown Sink " + node);
		}
		return info;
	}

	@SuppressWarnings("unused")
	private IRNode irnode;

	private ControlNode entry;

	private ControlNode normalExit;

	private ControlNode abruptExit;

	private Component comp;

	// Save the visited nodes in the forward process.
	private List<ControlNode> visitedNodes;

	// A table map ControlNode to its visited order in the cfg creation.
	private Hashtable<ControlNode, Integer> visitedNodesTable;

	// A set that keeps all the ControlNodes visited in the backward
	private HashSet<ControlNode> visitedNodesInBackword;

	// A table map ControlNode to its entry information.
	private Hashtable<ControlNode, Stack<ControlNode>> visitedNodesEntryTable;

	// Save the visited Entry, the top of the stack is the current component's
	// entry.
	private Stack<ControlNode> visitedEntrys;

	// Save the Node which is both DoubleInputPort and DoubleOutputPort.
	private HashSet<ControlNode> visitedDoubleInputDoubleOutputPort;

	private int no;

}
