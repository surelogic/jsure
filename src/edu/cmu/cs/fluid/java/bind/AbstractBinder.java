/*
 * Created on Mar 23, 2005
 *
 */
package edu.cmu.cs.fluid.java.bind;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.BindUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.*;

/**
 * @author Edwin
 *
 */
public abstract class AbstractBinder implements IBinder {
  private static final Logger LOG = SLLogger.getLogger("FLUID.bind");
  
  private final ClassMemberSearch search = new ClassMemberSearch(this);  
  private final JavaTypeVisitor typeVisitor = JavaTypeVisitor.getTypeVisitor(this);
  
  public IRNode getBinding(IRNode node) {
    IBinding binding = getIBinding(node);
    return binding == null ? null : binding.getNode();
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.bind.IBinder#getJavaType(edu.cmu.cs.fluid.ir.IRNode)
   */
  public IJavaType getJavaType(IRNode n) {
    return typeVisitor.getJavaType(n);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.bind.IBinder#getTypeEnvironment()
   */
  public ITypeEnvironment getTypeEnvironment() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Not in abstract binder");
  }
    
  public final IJavaDeclaredType getSuperclass(IJavaDeclaredType type) {
    if (type == null) {
      return null;
    }
    // TODO delete this deprecated method.
    IRNode decl = type.getDeclaration();
    Operator op = JJNode.tree.getOperator(decl);
    ITypeEnvironment tEnv = getTypeEnvironment();
    
    // BAD: causes a cycle if loading java.lang.Object as source
    // if (type == tEnv.getObjectType())
    //
    // FIX how else to identify it?
    String name = type.getName();
    if (name.equals("java.lang.Object")) {
      return null;
    }
    if (ClassDeclaration.prototype.includes(op)) {
      IRNode extension = ClassDeclaration.getExtension(decl);
      IJavaType t = tEnv.convertNodeTypeToIJavaType(extension);
      // TODO: What if we extend a nested class from our superclass?
      // A: The type factory should correctly insert type actuals
      // for the nesting (if any).  Actually maybe the canonicalizer should.
      t = t.subst(JavaTypeSubstitution.create(getTypeEnvironment(), type));
      if (!(t instanceof IJavaDeclaredType)) {
        LOG.severe("Classes can only extend other classes");
        return null;
      }
      return (IJavaDeclaredType) t;
    } else if (InterfaceDeclaration.prototype.includes(op)) {
      return tEnv.getObjectType();
    } else if (EnumDeclaration.prototype.includes(op)) {
      IRNode ed              = tEnv.findNamedType("java.lang.Enum");
      List<IJavaType> params = new ArrayList<IJavaType>(1);
      params.add(type);
      return JavaTypeFactory.getDeclaredType(ed, params, null);
    } else if (AnonClassExpression.prototype.includes(op)) {
      IRNode nodeType = AnonClassExpression.getType(decl);
      IJavaType t = tEnv.convertNodeTypeToIJavaType(nodeType);
      if (!(t instanceof IJavaDeclaredType)) {
        LOG.severe("Classes can only extend other classes");
        return null;
      }
      IJavaDeclaredType dt = ((IJavaDeclaredType) t);
      if (JJNode.tree.getOperator(dt.getDeclaration()) instanceof InterfaceDeclaration) {
        return tEnv.getObjectType();
      }
      return dt;
    } else {
      LOG.severe("Don't know what sort of declared type this is: " + type);
      return null;
    }
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.bind.IBinder#findRegionInType(edu.cmu.cs.fluid.ir.IRNode, java.lang.String)
   */
  public IRNode findRegionInType(IRNode type, String region) {
    return BindUtil.findRegionInType(type, region);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.bind.IBinder#getRegionParent(edu.cmu.cs.fluid.ir.IRNode)
   */
  public IRNode getRegionParent(IRNode n) {
    return BindUtil.getRegionParent(getTypeEnvironment(), n);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.bind.IBinder#findClassBodyMembers(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.java.bind.ITypeSearchStrategy, boolean)
   */
  public <T> T findClassBodyMembers(IRNode type, ISuperTypeSearchStrategy<T> tvs, boolean throwIfNotFound) {
    return search.findClassBodyMembers(type, tvs, throwIfNotFound);
  }
  
  protected boolean isOverridingMethod(IRNode overridingM, IJavaType type, 
		                            IRNode parentM, IJavaType stype) {
	  IRNode overridingParams = SomeFunctionDeclaration.getParams(overridingM);
	  IRNode parentParams     = SomeFunctionDeclaration.getParams(parentM);
	  if (JJNode.tree.numChildren(overridingParams) == JJNode.tree.numChildren(parentParams)) {
		  final ITypeEnvironment tEnv = getTypeEnvironment();
		  final Iterator<IRNode> oIt = JJNode.tree.children(overridingParams);
		  final Iterator<IRNode> pIt = JJNode.tree.children(parentParams);
		  while (oIt.hasNext()) {
			  IRNode oParam = oIt.next();
			  IRNode pParam = pIt.next();			  
			  IJavaType oType = getJavaType(oParam); // FIX What about type substitution?
			  IJavaType pType = getJavaType(pParam); // FIX What about type substitution?
			  if (!oType.equals(pType)) {
				  return false;
			  }
		  }
		  return true;
	  }
	  return false;
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.bind.IBinder#findOverriddenParentMethods(edu.cmu.cs.fluid.ir.IRNode)
   */
  public Iteratable<IRNode>  findOverriddenParentMethods(IRNode mth) {
	  final ITypeEnvironment tEnv = getTypeEnvironment();
	  final IRNode typeDecl = VisitUtil.getEnclosingType(mth);
	  final IJavaType type = tEnv.convertNodeTypeToIJavaType(typeDecl);
	  final IteratableHashSet<IRNode> overridden = new IteratableHashSet<IRNode>();
	  for(IJavaType stype : tEnv.getSuperTypes(type)) {
		  IJavaDeclaredType st = (IJavaDeclaredType) stype;
		  for(IRNode method : VisitUtil.getClassMethods(st.getDeclaration())) {
			  if (isOverridingMethod(mth, type, method, st)) {
				  overridden.add(method);
				  break; // At most one such method
			  }
		  }
	  }
	  return overridden;
  }

/* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.bind.IBinder#findOverriddenMethods(edu.cmu.cs.fluid.ir.IRNode)
   */
  public Iteratable<IRNode> findOverriddenMethods(final IRNode methodDeclaration) {
    final IteratableHashSet<IRNode> overridden = new IteratableHashSet<IRNode>();
    for (Iterator<IRNode> it = findOverriddenParentMethods(methodDeclaration); it.hasNext();) {
      IRNode pm = it.next();      
      if (!overridden.contains(pm)) {
    	// Only add/process the method if it hasn't been done before
        overridden.add(pm);
        for (Iterator<IRNode> it2 = findOverriddenMethods(pm); it2.hasNext();) {
          overridden.add(it2.next());
        }
      }
    }
    return overridden;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.bind.IBinder#findOverridingMethodsFromType(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.ir.IRNode)
   */
  public Iteratable<IRNode>  findOverridingMethodsFromType(IRNode callee, IRNode receiver) {
    // Optional method
    return new EmptyIterator<IRNode>();
  }
}
