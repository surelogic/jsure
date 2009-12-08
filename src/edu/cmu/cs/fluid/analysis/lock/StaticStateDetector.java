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

public final class StaticStateDetector extends AbstractIRAnalysisModule {
  /**
   * Log4j logger for this class
   */
  //private static final Logger LOG = Logger.getLogger("analysis.static.state");

  //private static final String STATIC_STATE_DETECTOR = "Static state detector";

  private static final StaticStateDetector INSTANCE = new StaticStateDetector();

  private static final Category publicStaticFieldsCategory = Category
  .getInstance("public static mutable field(s)");
  
  private static final Category publicStaticObjectFieldsCategory = Category
  .getInstance("public static Object-typed field(s)");
  
  private static final Category publicStaticArraysCategory = Category
  .getInstance("public static mutable array(s)");
  
  private static final Category staticFieldsCategory = Category
  .getInstance("static mutable field(s)");
  
  private static final Category staticObjectFieldsCategory = Category
  .getInstance("static Object-typed field(s)");
  
  private static final Category staticArraysCategory = Category
  .getInstance("static mutable array(s)");
  
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

  private void reportInference(Category c, String msg, IRNode loc) {
    InfoDrop id = new InfoDrop();
    // rd.addCheckedPromise(pd);
    id.setNodeAndCompilationUnitDependency(loc);
    id.setMessage(msg);
    id.setCategory(c);
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
        reportInference(isPublic? publicStaticFieldsCategory : staticFieldsCategory, 
                        "static mutable field of type " + t.getName(), fd);
        return null;
      }   
      if (isImmutable(t)) {
        return null;
      }
      if (t instanceof IJavaArrayType || isArrayTyped(fd)) {
        reportInference(isPublic? publicStaticArraysCategory : staticArraysCategory, 
                        "static mutable array of type " + t.getName(), fd);
      }
      else if (isObjectTyped(t) && !intendedAsConstant(fd) && hasMutableFields(t)) {        
        reportInference(isPublic? publicStaticObjectFieldsCategory : staticObjectFieldsCategory, 
                        "static Object-typed field of type " + t.getName(), fd);
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