package com.surelogic.dropsea.ir.drops;

import java.util.logging.Level;

import com.surelogic.aast.promise.PromiseDeclarationNode;
import com.surelogic.analysis.IIRProject;
import com.surelogic.common.Pair;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.javac.Projects;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.util.VisitUtil;

/**
 * getNode() should return the associated promise declaration
 */
public abstract class ModelDrop<D extends PromiseDeclarationNode> extends PromiseDrop<D> {
  public ModelDrop(D d) {
    super(d);
  }

  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
    // invalidate();
    if (invalidDeponent instanceof CUDrop) {
      // System.out.println("Invalidating "+getMessage());
      CUDrop cud = (CUDrop) invalidDeponent;
      IRNode cu = VisitUtil.getEnclosingCompilationUnit(getNode());
      if (cu != null && !cud.getCompilationUnitIRNode().equals(cu)) {
        throw new Error("unexpected dependence on CUDrop: " + DebugUnparser.toString(cud.getCompilationUnitIRNode()));
      }
      //clearNode();
      //clearAAST();
      // System.out.println("Clearing "+this.getMessage()+" due to invalidated "+invalidDeponent.getMessage());
    } else {
      /*
       * for(Drop d : getDeponents()) {
       * System.out.println(d.getClass().getSimpleName()+": "+d.getMessage()); }
       */
      if (SLLogger.getLogger().isLoggable(Level.FINE)) {
        SLLogger.getLogger().fine("Unexpected invalidate on " + getMessage() + " from " + invalidDeponent.getMessage());
      }
    }
  }
  
  protected static Pair<String,String> getPair(String name, IRNode context) {
	  if (name == null)
	      throw new IllegalArgumentException(I18N.err(44, "Name"));
	    if (context == null)
	      throw new IllegalArgumentException(I18N.err(44, "projectName"));
	    
	  IIRProject p = Projects.getEnclosingProject(context);
      final String project = p == null ? "" : p.getName();
      return new Pair<String,String>(name, project);
  }
}
