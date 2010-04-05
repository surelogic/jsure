/*
 * Created on Feb 3, 2004
 * Modified on Nov 1, 2004
 */
package edu.cmu.cs.fluid.analysis.cfg;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;

import com.surelogic.analysis.IAnalysisMonitor;
import com.surelogic.analysis.cfg.CFGDiagrammer;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.analysis.util.AbstractFluidAnalysisModule;
import edu.cmu.cs.fluid.eclipse.EclipseCodeFile;
import edu.cmu.cs.fluid.ir.IRNode;
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
public final class MakeCFGDiagram extends AbstractFluidAnalysisModule<Void> {
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
	public boolean analyzeCompilationUnit(ICompilationUnit file,
			org.eclipse.jdt.core.dom.CompilationUnit ast, 
            IAnalysisMonitor monitor) {
		LOG.info("Draw CFG diagram on " + file.getElementName());
		return doAnalysisOnAFile(file);		
	}

	@Override
	public IResource[] analyzeEnd(IProject project, IAnalysisMonitor monitor) {
		if (doingFullProjectPass) {
			doingFullProjectPass = fullBuildInProgress = false;
			return NONE_FURTHER;
		}
		doingFullProjectPass = true;
		return null; // the entire project needs to be analyzed again
	}

	private boolean doAnalysisOnAFile(final ICompilationUnit file) {

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
		return true;
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

						
						long temptime = System.currentTimeMillis();
						CFGDiagrammer.writeDotFile(currentNode, NO_PORTS, dotFile);
						cfgacctime += (System.currentTimeMillis() - temptime);
						LOG.info("created CFG digraph in " + dotFile);
//						String cmd = DOT + " -T" + OUTPUT_FORMAT + " -o"
//								+ cfgName + " " + dotName;
//						LOG.info(cmd);
//						Process proc = null;
//						try {
//							proc = Runtime.getRuntime().exec(cmd);
//							if (proc.waitFor() != 0) {
//								// System.out.println("Can not create the cfg
//								// files.");
//							}
//						} catch (SecurityException e) {
//							LOG
//									.info("The system does not allow creation of the dot subprocess.");
//						} catch (IOException e) {
//							LOG.info("Can not find the dot command.");
//						} catch (InterruptedException e) {
//							LOG.info("Unknown exception from cfg-maker.");
//						}
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
}
