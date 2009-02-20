package edu.cmu.cs.fluid.analysis.util;

import java.util.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.JavaModelException;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.eclipse.Eclipse;
import edu.cmu.cs.fluid.eclipse.bind.EclipseTypeEnvironment;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.TypeDeclInterface;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.*;
import edu.cmu.cs.fluid.tree.Operator;

public class TestMultipleProjects extends AbstractIRAnalysisModule {
  private IProject firstProject  = null;
  private boolean isFirstProject = false;
  
  /**
   * Map from qualified name to drop
   */
  private Map<String,CUDrop> primaryTypes = new HashMap<String,CUDrop>();
  private Map<String,IRNode> types        = null;
  private EclipseTypeEnvironment oldTEnv  = null;

  @Override
  public void analyzeBegin(IProject p) {
    super.analyzeBegin(p);
    
    if (firstProject == null) {
      firstProject = p;      
    }
    if (p == firstProject) {
      isFirstProject = true;
      primaryTypes.clear();
    }
  }
  
  @Override
  protected void doAnalysisOnAFile(IRNode cu) throws JavaModelException {
    String qname = JavaNames.genPrimaryTypeName(cu);
    CUDrop cud   = CUDrop.queryCU(cu);
    if (isFirstProject) {
      if (cud == null) {
        System.out.println("Couldn't find a drop for "+qname);
      } else {
        System.out.println("Putting in a drop for "+qname);
        primaryTypes.put(qname, cud);      
      }
    } else {
      CUDrop lastCud = primaryTypes.get(qname);
      if (lastCud != null) {
        boolean osNamesEqual = cud.javaOSFileName.equals(lastCud.javaOSFileName);
        boolean rootsEqual   = cud.cu.equals(lastCud.cu);
        if (osNamesEqual != rootsEqual) {
          String msg = cud.javaOSFileName+" ?= "+lastCud.javaOSFileName+", "+
                       cud.cu+" ?= "+lastCud.cu;
          reportProblem(msg, cu);
          throw new FluidError(msg);          
        }
      } else {
        System.out.println("Couldn't find a drop for "+qname);
      }
    }
  }
  
  @Override
  protected Iterable<IRNode> finishAnalysis(IProject project) {
    if (isFirstProject) {
      isFirstProject = false;
      
      oldTEnv = Eclipse.getDefault().getETypeEnv(project);
      types   = oldTEnv.snapshot();
    } else {
      EclipseTypeEnvironment tEnv = Eclipse.getDefault().getETypeEnv(firstProject);
      if (oldTEnv != tEnv) {
        String msg = oldTEnv+" != "+tEnv;
        reportProblem(msg, null);
        //new Throwable(msg).printStackTrace();
        throw new FluidError(msg);
      }
      EclipseTypeEnvironment thisTEnv = Eclipse.getDefault().getETypeEnv(project);
      if (thisTEnv == tEnv) {
        String msg = thisTEnv+" == "+tEnv;
        reportProblem(msg, null);
        //new Throwable(msg).printStackTrace();
        throw new FluidError(msg);
      }
      
      // Check that the type environment for the first project didn't change
      // because of the subsequent projects
      Map<String,IRNode> newTypes = tEnv.snapshot();
      for (Map.Entry<String, IRNode> entry : newTypes.entrySet()) {
        String qname = entry.getKey();
        IRNode oldT = types.get(qname);
        IRNode newT = entry.getValue();
        if (/* oldT != null && */ oldT != newT) {
          String msg = qname+": "+oldT+" != "+newT;
          reportProblem(msg, newT);
          //new Throwable(msg).printStackTrace();
          throw new FluidError(msg);
        }
      }
      
//      for(String qname : newTypes.keySet()) {
//        IRNode oldT = types.get(qname);
//        IRNode newT = newTypes.get(qname);
//        if (/* oldT != null && */ oldT != newT) {
//          String msg = qname+": "+oldT+" != "+newT;
//          reportProblem(msg, newT);
//          //new Throwable(msg).printStackTrace();
//          throw new FluidError(msg);
//        }
//      }
      
      // Check bindings in the CUs in the first project
      for(Map.Entry<String,CUDrop> e : primaryTypes.entrySet()) {
        for(IRNode n : JJNode.tree.bottomUp(e.getValue().cu)) {
          Operator op = JJNode.tree.getOperator(n);
          if (op instanceof IHasBinding) {
            // Check that each binding corresponds to a 
            IRNode b     = tEnv.getBinder().getBinding(n);
            IRNode newT  = VisitUtil.getEnclosingType(b);
            if (newT != null) {
              checkIfInSnapshot(newT);
            } else {
              Operator bop = JJNode.tree.getOperator(b);
              if (bop instanceof TypeDeclInterface) {
                checkIfInSnapshot(b);
              } else {
                System.out.println("No enclosing type for "+DebugUnparser.toString(b));
              }
            }
          }
          if (op instanceof IHasType) {
            IJavaType t = tEnv.getBinder().getJavaType(n);
            if (t instanceof IJavaDeclaredType) {
              IJavaDeclaredType dt = (IJavaDeclaredType) t;
              checkIfInSnapshot(dt.getDeclaration());
            }
          }
        }
      }
    }
    return super.finishAnalysis(project);
  }

  private void checkIfInSnapshot(IRNode newT) {
    String qname = JavaNames.getQualifiedTypeName(newT);
    IRNode oldT  = types.get(qname);
    if (!newT.equals(oldT)) {
      String msg = qname+": "+oldT+" != "+newT;
      reportProblem(msg, newT);
      //new Throwable(msg).printStackTrace();
      throw new FluidError(msg);
    }
  }
}
