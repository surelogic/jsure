package edu.cmu.cs.fluid.analysis.lock;

import java.util.*;

import org.eclipse.jdt.core.JavaModelException;

import com.surelogic.analysis.IAnalysisMonitor;

import edu.cmu.cs.fluid.analysis.util.*;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.*;

@Deprecated
public final class StaticStateDetector extends AbstractIRAnalysisModule {
  private static final StaticStateDetector INSTANCE = new StaticStateDetector();

  
  
  public static StaticStateDetector getInstance() {
    return INSTANCE;
  }
  
  public StaticStateDetector() {
    ConvertToIR.prefetch("java.lang.Object");
  }

  /*
   * (non-Javadoc)
   * 
   * @see edu.cmu.cs.fluid.analysis.util.AbstractIRAnalysisModule#doAnalysisOnAFile(edu.cmu.cs.fluid.ir.IRNode)
   */
  @Override
  protected boolean doAnalysisOnAFile(IRNode cu, IAnalysisMonitor monitor) throws JavaModelException {
    FastVisitor v = new FastVisitor(); // TODO cache
    Iterator<IRNode> e = JJNode.tree.bottomUp(cu);
    while (e.hasNext()) {
      v.doAccept(e.next());
    }
    return true;
  }

  private void reportInference(
      final IRNode loc, final Category c, final int msg, final Object... args) {
    InfoDrop id = new InfoDrop();
    // rd.addCheckedPromise(pd);
    id.setNodeAndCompilationUnitDependency(loc);
    id.setCategory(c);
    id.setResultMessage(msg, args);
  }

  private class FastVisitor extends Visitor<Object> {
    // TODO other forms?
    IRNode objectClass = analysisContext.binder.getTypeEnvironment().findNamedType("java.lang.Object");
    IJavaType objectType = JavaTypeFactory.convertNodeTypeToIJavaType(objectClass, analysisContext.binder);
    
    @Override
    public Object visitFieldDeclaration(IRNode fd) {
      int mods = JavaNode.getModifiers(fd);
      boolean isPublic  = JavaNode.isSet(mods, JavaNode.PUBLIC);
      boolean isStatic  = JavaNode.isSet(mods, JavaNode.STATIC);
      boolean isMutable = !JavaNode.isSet(mods, JavaNode.FINAL);
      if (!isStatic) { 
        return null;
      }
      IJavaType t = analysisContext.binder.getJavaType(FieldDeclaration.getType(fd));   
      if (isMutable) {
        reportInference(fd,
            isPublic ? Messages.DSC_PUBLIC_STATIC_FIELD : Messages.DSC_STATIC_FIELD,
                Messages.FIELD, t.getName());
        return null;
      }   
      if (isImmutable(t)) {
        return null;
      }
      if (t instanceof IJavaArrayType || isArrayTyped(fd)) {
        reportInference(fd,
            isPublic ? Messages.DSC_PUBLIC_STATIC_ARRAY : Messages.DSC_STATIC_ARRAY,
                Messages.ARRAY, t.getName());
      }
      else if (isObjectTyped(t) && !intendedAsConstant(fd) && hasMutableFields(t)) {
        reportInference(fd,
            isPublic ? Messages.DSC_PUBLIC_STATIC_OBJECT_FIELD : Messages.DSC_STATIC_OBJECT_FIELD,
                Messages.OBJECT_FIELD, t.getName());
      } 
      return Boolean.TRUE;
    }

    private boolean isImmutable(IJavaType t) {
      if (t instanceof IJavaSourceRefType) {
        IRNode d = ((IJavaSourceRefType) t).getDeclaration();
        return MutabilityAnnotation.isImmutable(d);         
      }
      return false;
    }
    
    private boolean hasMutableFields(IJavaType type) {
      if (type instanceof IJavaSourceRefType) {
        IRNode t = ((IJavaSourceRefType) type).getDeclaration();      
        Iterator<IRNode> fields = VisitUtil.getClassFieldDecls(t);
        while (fields.hasNext()) {
          IRNode fd = fields.next();
          int mods = JavaNode.getModifiers(fd);
          boolean isInstance = !JavaNode.isSet(mods, JavaNode.STATIC);
          boolean isMutable = !JavaNode.isSet(mods, JavaNode.FINAL);
          if (isInstance && isMutable) {
            return true;
          }
        }
      }
      return false;
    }

    /**
     * Check for capitalization
     * @param fd
     */
    private boolean intendedAsConstant(IRNode fd) {
      IRNode vars   = FieldDeclaration.getVars(fd);
      Iterator<IRNode> e = VariableDeclarators.getVarIterator(vars);
      while (e.hasNext()) {
        IRNode vd = e.next();
        String name = VariableDeclarator.getId(vd);
        if (!name.equals(name.toUpperCase())) {
          return false;
        }
      }
      return true;
    }

    private boolean isArrayTyped(IRNode fd) {
      IRNode vars   = FieldDeclaration.getVars(fd);
      Iterator<IRNode> e = VariableDeclarators.getVarIterator(vars);
      while (e.hasNext()) {
        IRNode vd = e.next();
        if (VariableDeclarator.getDims(vd) > 0) {
          return true;
        }
      }
      return false;
    }

    private boolean isObjectTyped(IJavaType type) {
      return analysisContext.binder.getTypeEnvironment().isSubType(type, objectType);
    }
  }
}