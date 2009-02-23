package edu.cmu.cs.fluid.analysis.threads;

import org.eclipse.jdt.core.JavaModelException;

import com.surelogic.ast.*;
import com.surelogic.ast.java.operator.*;

import edu.cmu.cs.fluid.analysis.util.*;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.CUDrop;

public abstract class AbstractConcurrencyDetector extends AbstractIRAnalysisModule {
  /**
   * Log4j logger for this class
   */
  //protected static final Logger LOG = Logger.getLogger("analysis.threads");

  //protected static final String CONCURRENCY_DETECTOR = "IR Concurrency detector";

  protected static final Category threadCreationCategory = Category
      .getInstance("java.lang.Thread subtype instance creation(s)");

  protected static final Category runnableCreationCategory = Category
      .getInstance("java.lang.Runnable subtype instance creation(s) - not Thread");

  protected static final Category threadStartsCategory = Category
      .getInstance("thread start(s)");
  
  AbstractConcurrencyDetector() {
    ConvertToIR.prefetch("java.lang.Runnable");
    ConvertToIR.prefetch("java.lang.Thread");
  }

  @Override
  protected abstract void doAnalysisOnAFile(IRNode cu) throws JavaModelException;

  protected final void reportInference(Category c, String msg, IRNode loc) {
    InfoDrop id = new InfoDrop();
    // rd.addCheckedPromise(pd);
    id.setNodeAndCompilationUnitDependency(loc);
    id.setMessage(msg);
    id.setCategory(c);
  }
  
  protected final IJavaDeclaredType findNamedType(String qname) {
    IRNode t = analysisContext.binder.getTypeEnvironment().findNamedType(qname);
    return (IJavaDeclaredType) JavaTypeFactory.convertNodeTypeToIJavaType(t, analysisContext.binder);
  }
  
  protected final IRNode toNode(IJavaOperatorNode n) {
    return (IRNode) n;
  }
  
  protected final String getDeclName(IDeclarationNode decl) {
    return JavaNames.getTypeName(toNode(decl));
  }
  
  protected final ISourceRefType findNamedTypeBinding(String qname) {
    IRNode t = analysisContext.binder.getTypeEnvironment().findNamedType(qname);
    CUDrop.queryCU(VisitUtil.getEnclosingCompilationUnit(t));
    return null;    
  }
}