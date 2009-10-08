package edu.cmu.cs.fluid.analysis.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.CompilationUnit;

import com.surelogic.annotation.parse.AnnotationVisitor;
import com.surelogic.annotation.parse.ParseHelper;
import com.surelogic.annotation.rules.AnnotationRules;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.xml.TestXMLParser;

import edu.cmu.cs.fluid.eclipse.Eclipse;
import edu.cmu.cs.fluid.eclipse.EclipseCodeFile;
import edu.cmu.cs.fluid.eclipse.QueuingSrcNotifyListener;
import edu.cmu.cs.fluid.eclipse.adapter.Binding;
import edu.cmu.cs.fluid.eclipse.promise.EclipsePromiseParser;
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.CodeInfo;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.IJavaFileLocator;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.bind.ModulePromises;
import edu.cmu.cs.fluid.java.bind.ScopedPromises;
import edu.cmu.cs.fluid.java.operator.NamedPackageDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeDeclarations;
import edu.cmu.cs.fluid.java.util.PromiseUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.java.xml.XML;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.Category;
import edu.cmu.cs.fluid.sea.IRReferenceDrop;
import edu.cmu.cs.fluid.sea.InfoDrop;
import edu.cmu.cs.fluid.sea.PromiseWarningDrop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.PackageDrop;
import edu.cmu.cs.fluid.sea.drops.SourceCUDrop;
import edu.cmu.cs.fluid.util.AbstractRunner;
import edu.cmu.cs.fluid.util.QuickProperties;

public final class PromiseParser extends AbstractFluidAnalysisModule
{
	/**
	 * Logger for this class
	 */
	private static final Logger LOG = SLLogger.getLogger("PromiseParser");

	private static final boolean useNewParser = AnnotationRules.useNewParser;

	public static final QuickProperties.Flag xmlgenFlag =
		new QuickProperties.Flag(LOG, "xml.useGenerator", "Generate");

	private static boolean useXMLGen() {
		return QuickProperties.checkFlag(xmlgenFlag);
	}

	public static final boolean useXMLGen = useXMLGen();

	public static final QuickProperties.Flag newXMLParserFlag =
	  new QuickProperties.Flag(LOG, "xml.useNewParser", "Parser", true);

	private static boolean useNewXMLParser() {
	  return QuickProperties.checkFlag(newXMLParserFlag);
	}

	public static final boolean useNewXMLParser = useNewXMLParser();

	private static PromiseParser INSTANCE;

	public static PromiseParser getInstance() {
		return INSTANCE;
	}

	private QueuingSrcNotifyListener listener = new QueuingSrcNotifyListener("PromiseParser");

	public PromiseParser() {
		INSTANCE = this;
		ConvertToIR.register(listener);
	}


	void processCompUnit(ITypeEnvironment te, IRNode cu, String name, String src) {
		if (IDE.getInstance().isCancelled()) {
			return;
		}
		postProcessFAST(te, cu);
		handlePackageLevelPromises(cu);

		if (useNewParser) {
			AnnotationVisitor v = new AnnotationVisitor(te, name);
			v.doAccept(cu);

			try {
				if (useNewXMLParser) {
					int num = TestXMLParser.process(cu, name + ".promises.xml");
					//System.out.println("Parsing XML for "+name+": "+num+" added");
				}
			} catch (Exception e) {
				if (!(e instanceof FileNotFoundException)) {
					e.printStackTrace();
				} else if (LOG.isLoggable(Level.FINER)) {
					LOG.finer("Couldn't find file " + name + ".promises.xml");
				}
			}
		}
		parsePromises(te, cu, name, src);

		doneProcessing(cu);
	}

	void postProcessFAST(ITypeEnvironment te, IRNode cu) {
		IDE.getInstance().getJavaFileLocator().getStatusForAST(cu).canonicalize();
		PromiseUtil.activateRequiredCuPromises(te.getBinder(), te.getBindHelper(), cu);
		if (LOG.isLoggable(Level.FINER)) {
			LOG.finer("Finished activating promises on " + DebugUnparser.toString(cu));
		}
	}

	/**
	 * @param top
	 * @param javaOSFileName
	 * @param src
	 */
	void parsePromises(ITypeEnvironment te, IRNode top, String javaOSFileName, String src) {
		// adapter.run(top, javaOSFileName, src);

		// check source for promises to parse
		if (src != null && src.length() > 0) {
			if (!useNewParser) {
				EclipsePromiseParser.process(top, src);
				LOG.info("Using the old promise parser");
			}
		} else if (LOG.isLoggable(Level.FINER)) {
			LOG.finer("No source to parse for promises: " + javaOSFileName);
		}
		if (!useNewParser || !useNewXMLParser) {
			XML.getDefault().setProcessingXMLInSrc(src != null);
			Eclipse.getDefault().getContext(getProject()).getXmlProcessor().process(top);
			XML.getDefault().setProcessingXMLInSrc(false);
		}
		// make sure all promises are part of fm
		//
//		SyntaxForestModel fm = Eclipse.getDefault().getForestModel();
//		Iterator<IRNode> it = JavaPromise.promisesBottomUp(top);
//		while (it.hasNext()) {
//		fm.addRoot(it.next());
//		}
	}

	@Override
	public void preBuild(IProject p) {
		super.preBuild(p);
	}

	@Override
	public void analyzeBegin(IProject p) {
		super.analyzeBegin(p);

		/*
		 * Some code needs java.lang.Object as promises are parsed, so
		 * we need to parse its promises early
		 */
		final ITypeEnvironment te = Eclipse.getDefault().getTypeEnv(p);
		final CodeInfo info       = listener.find("java.lang.Object");
		final IRNode cu;
    ParseHelper.getInstance().initialize(te.getClassTable());

		if (info != null) {
			info.setProperty(CodeInfo.DONE, Boolean.TRUE);

			cu = info.getNode();
		} else if (ConvertToIR.getInstance().didJavaLangObjectChange()) {
			IRNode jlo = te.findNamedType("java.lang.Object");
			cu = VisitUtil.getEnclosingCompilationUnit(jlo);
		} else {
			cu = null;
		}
		if (cu != null) {
			//System.out.println("Parsing promises for "+info.getFileName());
			runInVersion(new AbstractRunner() {
				public void run() {
					processCompUnit(te, cu, "java.lang.Object", null);
				}
			});
		}
	}

	@Override
	public boolean analyzeResource(final IResource resource, int kind) {
		if (AbstractFluidAnalysisModule.isPackageInfo(resource)) {
			//Moved to PackageLevelPreprocessing
			//parsePackageInfo(resource);
			return true;
		}
		else if (AbstractFluidAnalysisModule.isPackageJava(resource)) {
			reportWarning("Using package.java, instead of package-info.java", null);
			return true;
		}
		return super.analyzeResource(resource, kind);
	}

	/**
	 * @see edu.cmu.cs.fluid.dc.IAnalysis#analyzeCompilationUnit(org.eclipse.jdt.core.ICompilationUnit,
	 *      org.eclipse.jdt.core.dom.CompilationUnit)
	 */
	@Override
	public void analyzeCompilationUnit(
			ICompilationUnit file,
			CompilationUnit ast) {
		javaFile = file;

		/*
    if (ConvertToIR.jlo != null) {
      String unparse = DebugUnparser.toString(ConvertToIR.jlo);
      if (!unparse.equals(ConvertToIR.jloUnparsed)) {
        System.out.println("Unparses not the same");
      }
    }
		 */
		final CUDrop drop = SourceCUDrop.queryCU(new EclipseCodeFile(file));

		if (drop != null) {
			runVersioned(new AbstractRunner() {
				public void run() {
					try {
						final String src = javaFile.getSource();
						final ITypeEnvironment te = Eclipse.getDefault().getTypeEnv(getProject());
						processCompUnit(te, drop.cu, javaFile.getPath().toString(), src);
					} catch (JavaModelException e) {
						LOG.log(
								Level.SEVERE,
								"Promise scrubbing skipped due to problem finding IR for "
								+ javaFile.getElementName(),
								e);
					}
				}
			});

		}

		javaFile = null;
	}

	/**
	 * @see edu.cmu.cs.fluid.dc.IAnalysis#analyzeEnd(org.eclipse.core.resources.IProject)
	 */
	@Override
	public IResource[] analyzeEnd(IProject p) {
		final ITypeEnvironment te = Eclipse.getDefault().getTypeEnv(p);
		runVersioned(new AbstractRunner() {
			public void run() {
				final Iterator<CodeInfo> it     = listener.infos();
				while (it.hasNext()) {
					CodeInfo info = it.next();
					if (info.getProperty(CodeInfo.DONE) == Boolean.TRUE) {
						continue;
					}
					//System.out.println("Parsing promises for "+info.getFileName());
					String name = info.getFileName();
					IRNode cu   = info.getNode();
					processCompUnit(te, cu, name, info.getSource());
				}
			}
		});
		listener.clear();

		runVersioned(new AbstractRunner() {
      public void run() {
        handleWaitQueue(new IQueueHandler() {
          public void handle(String qname) {
            IRNode n = te.findNamedType(qname);
            if (n == null) {
              PackageDrop p = PackageDrop.findPackage(qname);
              if (p != null) {
                n = p.node;
              } else {
                String msg = "Couldn't parse promises: "+qname+" don't exists as a type or a package";
                LOG.warning(msg);
                reportWarning(msg, null);
              }
              // FIX ignore packages?
              return;
            }
            IRNode cu  = VisitUtil.getEnclosingCompilationUnit(n);
            CUDrop d   = CUDrop.queryCU(cu);
            if (d == null) {
              System.out.println("Couldn't find a drop for "+qname);
            }
            String src = (d instanceof SourceCUDrop) ? ((SourceCUDrop) d).source : null;
            processCompUnit(te, cu, d.javaOSFileName, src);
          }
        });
      }
		});
		if (IJavaFileLocator.useIRPaging) {
			//*
			try {
				IDE.getInstance().getJavaFileLocator().persistNew();
				// EclipseFileLocator loc = (EclipseFileLocator) Eclipse.getInstance().getJavaFileLocator();
				// loc.testUnload(false);
				// loc.testReload();
			} catch (IOException e) {
				LOG.log(Level.SEVERE, "Error while calling persistNew()", e);
			}
			//*/
		}
		return NONE_FURTHER;
	}

	/**
	 * (Re-)apply promises from the package to a class in the package
	 * @param cu
	 */
	private void handlePackageLevelPromises(IRNode cu) {
		final boolean fineIsLoggable = LOG.isLoggable(Level.FINE);

		// Check for package-level @promises
		IRNode pkg                = edu.cmu.cs.fluid.java.operator.CompilationUnit.getPkg(cu);

		if (NamedPackageDeclaration.prototype.includes(JJNode.tree.getOperator(pkg))) {
			String pkgName    = NamedPackageDeclaration.getId(pkg);
			PackageDrop p = Binding.findPackage(pkgName);
			if (p == null) {
				LOG.severe("No package made for decl of "+pkgName);
				return;
			}
			if (!p.isFromSrc()) {
				XML.getDefault().setProcessingXML(true);
			}
			ModulePromises.applyScopedModules(p.node, cu);

			IRNode types = edu.cmu.cs.fluid.java.operator.CompilationUnit.getDecls(cu);
			for (IRNode type : TypeDeclarations.getTypesIterator(types)) {
				Iterator<IRNode> enm  = ScopedPromises.assuredPromises(p.node);
				while (enm.hasNext()) {
					IRNode promise = enm.next();
					if (ScopedPromises.getInstance().processAssurance(promise, type, JavaNode.getSrcRef(promise))) {
						if (fineIsLoggable) {
							LOG.fine("Successfully processed assurance "+DebugUnparser.toString(promise)+
									" on "+JJNode.getInfoOrNull(type));
						}
					} else {
						LOG.severe("Failed to process assurance "+DebugUnparser.toString(promise)+
								" on "+JJNode.getInfoOrNull(type));
					}
				}
			}
			XML.getDefault().setProcessingXML(false);
		}
	}

	@Override
	protected IRReferenceDrop makeWarningDrop() {
		return new InfoDrop("PromiseParser");
	}
	@Override
	protected IRReferenceDrop makeProblemDrop() {
		return new PromiseWarningDrop();
	}
	@Override
	protected Category warningCategory() {
		return JavaGlobals.PROMISE_PARSER_WARNING;
	}
	@Override
	protected Category problemCategory() {
		return JavaGlobals.PROMISE_PARSER_PROBLEM;
	}
}
