/*
 * Created on Mar 23, 2005
 *
 */
package edu.cmu.cs.fluid.java.bind;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.ThreadSafe;
import com.surelogic.common.SLUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.ClassInitDeclaration;
import edu.cmu.cs.fluid.java.promise.IFromInitializer;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.util.OpSearch;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.*;

/**
 * @author Edwin
 *
 */
@ThreadSafe
public abstract class AbstractBinder implements IBinder {
  private static final Logger LOG = SLLogger.getLogger("FLUID.bind");
  
  private final ClassMemberSearch search = new ClassMemberSearch(this);  
  final JavaTypeVisitor typeVisitor;
  
  private volatile boolean allowWarnings;
  final boolean processJava8;
  
  public AbstractBinder(boolean processJ8) {
	  processJava8 = true;
	  typeVisitor = JavaTypeVisitor.getTypeVisitor(this);
  }
  
  public AbstractBinder(IBinder b) {
	  this(b instanceof AbstractBinder && ((AbstractBinder)b).processJava8);
  }
  
  @Override
  public void enableWarnings() {
	  allowWarnings = true;
  }
  
  @Override
  public void disableWarnings() {
	  allowWarnings = false;
  }
  
  protected boolean allowWarnings() {
	  return allowWarnings;
  }
  
  @Override
  public final IRNode getBinding(IRNode node) {
    IBinding binding = getIBinding(node);
    return binding == null ? null : binding.getNode();
  }
  
  @Override
  public IBinding getIBinding(IRNode node) {
	  return getIBinding_impl(node);
  }
  
  @Override
  public IBinding getIBinding(IRNode node, IRNode contextFlowUnit) {
	  IBinding rv = getIBinding_impl(node);
	  
	  // Check if we need to remap anything
	  if (contextFlowUnit != null && rv.getNode() != null) {
		  // See if it's from an instance initializer and for a constructor
		  Operator op = JJNode.tree.getOperator(rv.getNode()); 
		  if (op instanceof IFromInitializer && ConstructorDeclaration.prototype.includes(contextFlowUnit)) {
			  final IRNode newDecl;
			  if (op == ReceiverDeclaration.prototype) {
				  newDecl = JavaPromise.getReceiverNode(contextFlowUnit);
			  } else {
				  throw new IllegalStateException("Unexpected decl to bind: "+op.name());
			  }
			  return IBinding.Util.makeBinding(newDecl);
		  }
		  // TODO handle Foo.this in constructor call
	  }
	  return rv;
  }
  
  protected abstract IBinding getIBinding_impl(IRNode node);
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.bind.IBinder#getJavaType(edu.cmu.cs.fluid.ir.IRNode)
   */
  @Override
  public IJavaType getJavaType(IRNode n) {
	/*
	String unparse = DebugUnparser.toString(n);
	if (unparse.contains("<>")) {
		System.out.println("Found diamond: "+unparse);
	}
	*/
	if (!processJava8) {
	  final Operator op = JJNode.tree.getOperator(n);
	  IJavaType rv = null;
	  if (ParameterizedType.prototype.includes(op)) {
		rv = inferForDiamond(n);
	  }
	  else if (NewExpression.prototype.includes(op)) {
		IRNode type = NewExpression.getType(n);
		rv = getJavaType(type);
	  }
	  if (rv != null) {
		return rv;
	  }
	}
	return typeVisitor.getJavaType(n);
  }
  
  private IJavaDeclaredType inferForDiamond(IRNode paramdType) {
	  final IJavaDeclaredType type = (IJavaDeclaredType) typeVisitor.getJavaType(paramdType);
	  IRNode actuals = ParameterizedType.getArgs(paramdType);
	  if (JJNode.tree.numChildren(actuals) == 0) {
		  // Found diamond operator
		  IJavaDeclaredType targetType = null;
		  try {
			  targetType = findTargetTypeForInstantiation(paramdType);			  
			  return matchTarget(targetType, type);
		  } catch(BindingException e) {
			  throw e;
		  } catch(RuntimeException e) {	
			  String msg = targetType+" => "+type;
			  if (e.getMessage() != null) {
				  msg = msg+" : "+e.getMessage();
			  }
			  throw new BindingException(paramdType, true, msg, e);
		  }
	  }
	  // Otherwise, fallback on the usual
	  return type;
  }

  // Specialized for finding the target type for "new ArrayList<>()"
  private IJavaDeclaredType findTargetTypeForInstantiation(IRNode pType) {
	  final IRNode newE = newExpr.findEnclosing(pType);
	  IRNode polyE = JJNode.tree.getParent(newE);
	  if (!OuterObjectSpecifier.prototype.includes(polyE)) {
		  polyE = newE;
	  }
	  /*
	  Operator contextOp = JJNode.tree.getOperator(context);
	  if (Initialization.prototype.includes(contextOp)) {
		  IRNode varDecl = JJNode.tree.getParent(context);
		  return (IJavaDeclaredType) getJavaType(varDecl);
	  }
	  else if (AssignExpression.prototype.includes(contextOp)) {
		  IRNode leftSide = AssignExpression.getOp1(context);
		  return (IJavaDeclaredType) getJavaType(leftSide);
	  }	  
	  else if (ArrayInitializer.prototype.includes(contextOp)) {
		  // Only appearing in Initialization or ArrayCreationExpression
		  IRNode parent = JJNode.tree.getParent(context);
		  Operator parentOp = JJNode.tree.getOperator(parent);
		  IJavaArrayType at;
		  if (Initialization.prototype.includes(parentOp)) {
			  IRNode varDecl = JJNode.tree.getParent(context);
			  at = (IJavaArrayType) getJavaType(varDecl);
		  }
		  else if (ArrayCreationExpression.prototype.includes(parentOp)) {
			  at = (IJavaArrayType) getJavaType(parent);
		  } else {
			  return null;
		  }
		  return (IJavaDeclaredType) at.getElementType();
	  }
	  */
	  final IJavaType type = new TypeUtils(getTypeEnvironment()).getPolyExpressionTargetType(polyE);
	  if (type != null) {
		  return (IJavaDeclaredType) type;
	  }
	  new TypeUtils(getTypeEnvironment()).getPolyExpressionTargetType(polyE);
	  throw new BindingException(newE, true);
  }

  private final OpSearch newExpr = new OpSearch() {
	  @Override
	  protected boolean found(Operator op) { 
		  return op instanceof NewExpression; 
	  }
  };
   
  private IJavaDeclaredType matchTarget(IJavaDeclaredType targetType, IJavaDeclaredType newType) {
	//System.out.println("Target: "+targetType);
	if (targetType.isRawType(getTypeEnvironment())) {
		return (IJavaDeclaredType) getTypeEnvironment().computeErasure(newType);
	}
	if (targetType == getTypeEnvironment().getObjectType()) {
		return (IJavaDeclaredType) getTypeEnvironment().computeErasure(newType);
	}
	/*
	  final TypeUtils utils = new TypeUtils(getTypeEnvironment());
	  final Constraints constraints = utils.new Constraints(null, null, InvocationKind.STRICT);
	  constraints.addConstraints(targetType, type);
	  constraints.computeTypeMapping();
	 */	  
	IJavaDeclaredType genType = makeGeneric(newType);
	IJavaDeclaredType targetedST = matchDeclToSuperType(targetType.getDeclaration(), genType);
	IJavaDeclaredType rv = matchupParameters(targetType, genType, targetedST);
	if (rv == null) {
		return newType; // Unable to infer right now
	}
	return rv;
  }
  
  private IJavaDeclaredType makeGeneric(IJavaDeclaredType t) {
	IRNode types = TypeUtils.getParametersForType(t.getDeclaration());
	List<IJavaType> params = new ArrayList<IJavaType>();
	for(IRNode tf : TypeFormals.getTypeIterator(types)) {
		params.add(JavaTypeFactory.getTypeFormal(tf));
	}
	// TODO is the outer type right?
	return JavaTypeFactory.getDeclaredType(t.getDeclaration(), params, t.getOuterType());
  }
  
  private IJavaDeclaredType matchDeclToSuperType(final IRNode decl, IJavaDeclaredType t) {
	  if (decl.equals(t.getDeclaration())) {
		  return t;
	  }
	  for(IJavaType st : t.getSupertypes(getTypeEnvironment())) {
		  IJavaDeclaredType rv = matchDeclToSuperType(decl, (IJavaDeclaredType) st);
		  if (rv != null) {
			  return rv;
		  }
	  }
	  return null;
  }
  
  private IJavaDeclaredType matchupParameters(IJavaDeclaredType targetType, IJavaDeclaredType generic, IJavaDeclaredType match) {	  
	  // Compute substitution
	  Map<IJavaType,IJavaType> map = new HashMap<IJavaType, IJavaType>();
	  int i=0;
	  for(IJavaType t : match.getTypeParameters()) {
		  IJavaType tt = targetType.getTypeParameters().get(i);
		  if (!createMapping(map, t, tt)) {
			  return null;
		  }
		  i++;
	  }
	  // Substitute
	  List<IJavaType> params = new ArrayList<IJavaType>();
	  for(IJavaType t : generic.getTypeParameters()) {
		  IJavaType subst = map.get(t);
		  if (subst == null) {
			  return null;
		  }
		  params.add(subst);
	  }
	  // TODO is the outer type right?
	  return JavaTypeFactory.getDeclaredType(generic.getDeclaration(), params, generic.getOuterType());
  }
  
  private boolean createMapping(Map<IJavaType, IJavaType> map, IJavaType t, IJavaType tt) {	  
	  if (t instanceof IJavaTypeFormal) {
		  if (tt instanceof IJavaWildcardType) {
			  IJavaTypeFormal tf = (IJavaTypeFormal) t;
			  IJavaReferenceType tBound = tf.getExtendsBound(getTypeEnvironment());
			  IJavaWildcardType wtt = (IJavaWildcardType) tt;
			  
			  if (wtt.getUpperBound() != null) {
				  map.put(t, new TypeUtils(getTypeEnvironment()).getLowestUpperBound(wtt.getUpperBound(), tBound));
			  }
			  else if (wtt.getLowerBound() != null) {
				  // TODO is this right?
				  map.put(t, new TypeUtils(getTypeEnvironment()).getGreatestLowerBound(wtt.getLowerBound(), tBound));
			  }
			  else {
				  map.put(t, tBound);
			  }
		  } else {
			  map.put(t, tt); // Assume that it matches tBound
		  }
	  } 
	  else if (tt instanceof IJavaDeclaredType) {
		  IJavaDeclaredType dtt = (IJavaDeclaredType) tt;
		  if (t instanceof IJavaDeclaredType) {
			  // Check if the same
			  IJavaDeclaredType dt = (IJavaDeclaredType) t;
			  if (dt.getDeclaration().equals(dtt.getDeclaration())) {
				  int i=0;
				  for(IJavaType t2 : dt.getTypeParameters()) {
					  IJavaType tt2 = dtt.getTypeParameters().get(i);
					  if (!createMapping(map, t2, tt2)) {
						  return false;
					  }
					  i++;
				  }
			  } else {
				  return false;
			  }
		  } else {
			  return false;
		  }
	  } else if (tt instanceof IJavaWildcardType) {
		  final IJavaWildcardType wtt = (IJavaWildcardType) tt;
		  if (!(t instanceof IJavaDeclaredType)) {
			  return false; // TODO What is this?
		  }
		  IJavaDeclaredType dt = (IJavaDeclaredType) t;
		  if (wtt.getUpperBound() != null) {
			  if (dt.isSubtype(getTypeEnvironment(), wtt.getUpperBound())) {				  
				  for(IJavaType t2 : dt.getTypeParameters()) {
					  createMapping(map, t2, JavaTypeFactory.wildcardType);
				  }
			  } else {
				  return false; // TODO
			  }
		  }
		  else if (wtt.getLowerBound() != null) {
			  if (wtt.getLowerBound().isSubtype(getTypeEnvironment(), dt)) {
				  for(IJavaType t2 : dt.getTypeParameters()) {
					  createMapping(map, t2, JavaTypeFactory.wildcardType);
				  }
			  } else {
				  return false; // TODO
			  }
		  }  
		  else { // Unbounded			 
			  for(IJavaType t2 : dt.getTypeParameters()) {
				  createMapping(map, t2, tt);
			  }
		  }		  
 	  } else {
		  return false;
	  }
	  return true;
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.bind.IBinder#getTypeEnvironment()
   */
  @Override
  public ITypeEnvironment getTypeEnvironment() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Not in abstract binder");
  }
    
  @Override
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
    if (name.equals(SLUtility.JAVA_LANG_OBJECT)) {
      return null;
    }
    if (ClassDeclaration.prototype.includes(op)) {
      IRNode extension = ClassDeclaration.getExtension(decl);
      IJavaType t = tEnv.convertNodeTypeToIJavaType(extension);
      // TODO: What if we extend a nested class from our superclass?
      // A: The type factory should correctly insert type actuals
      // for the nesting (if any).  Actually maybe the canonicalizer should.
      if (t != null) {
    	  t = t.subst(JavaTypeSubstitution.create(getTypeEnvironment(), type));
      }
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
   * @see edu.cmu.cs.fluid.java.bind.IBinder#findClassBodyMembers(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.java.bind.ITypeSearchStrategy, boolean)
   */
  @Override
  public <T> T findClassBodyMembers(IRNode type, ISuperTypeSearchStrategy<T> tvs, boolean throwIfNotFound) {
    return search.findClassBodyMembers(type, tvs, throwIfNotFound);
  }
  
  protected boolean isOverridingMethod(IRNode overridingM, IJavaType type, 
		                            IRNode parentM, IJavaType stype) {
	  if (!SomeFunctionDeclaration.getId(overridingM).equals(SomeFunctionDeclaration.getId(parentM))) {
		  return false;
	  }
	  IRNode overridingParams = SomeFunctionDeclaration.getParams(overridingM);
	  IRNode parentParams     = SomeFunctionDeclaration.getParams(parentM);
	  if (JJNode.tree.numChildren(overridingParams) == JJNode.tree.numChildren(parentParams)) {
		  //final ITypeEnvironment tEnv = getTypeEnvironment();
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
  @Override
  public Iteratable<IBinding>  findOverriddenParentMethods(IRNode mth) {
	  Operator op = JJNode.tree.getOperator(mth);		 
	  if (ClassInitDeclaration.prototype.includes(op)) {
		  return new EmptyIterator<IBinding>();
	  }
	  final ITypeEnvironment tEnv = getTypeEnvironment();
	  final IRNode typeDecl = VisitUtil.getEnclosingType(mth);
	  final IJavaType type = tEnv.convertNodeTypeToIJavaType(typeDecl);
	  
	  IteratableHashSet<IBinding> overridden =
		  findOverridenParentsFromType(mth, tEnv, type);
	  if (overridden.isEmpty()) {
		  IteratableHashSet<IJavaType> nextTsupers = new IteratableHashSet<IJavaType>();
		  for (IJavaType t : tEnv.getSuperTypes(type)) {
			  nextTsupers.add(t);
		  }
		  while (!nextTsupers.isEmpty()) {
			Iteratable<IJavaType> tsupers = nextTsupers;
			nextTsupers = new IteratableHashSet<IJavaType>();
			while (tsupers.hasNext()) {
				IJavaType tt = tsupers.next();
				overridden.addAll(findOverridenParentsFromType(mth, tEnv, tt));
				for (IJavaType t : tEnv.getSuperTypes(tt)) {
					  nextTsupers.add(t);
				}
			}
			if (!overridden.isEmpty()) {
				return overridden;
			}
		}
	  }
	  return overridden;
  }

/**
 * @param mth The method whose parent methods we wish to fine
 * @param tEnv A valid type environment.
 * @param type The type whose super(s) may contain the method we are looking for.
 * @return
 */
private IteratableHashSet<IBinding> findOverridenParentsFromType(IRNode mth,
		final ITypeEnvironment tEnv, final IJavaType type) {
	final IteratableHashSet<IBinding> overridden = new IteratableHashSet<IBinding>();
	  
	  for(IJavaType stype : tEnv.getSuperTypes(type)) {
		  IJavaDeclaredType st = (IJavaDeclaredType) stype;
		  for(IRNode method : VisitUtil.getClassMethods(st.getDeclaration())) {
			  if (isOverridingMethod(mth, type, method, st)) {
				  overridden.add(IBinding.Util.makeBinding(method, st, tEnv));
				  break; // At most one such method
			  }
		  }
	  }
	  return overridden;
}

/* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.bind.IBinder#findOverriddenMethods(edu.cmu.cs.fluid.ir.IRNode)
   */
  @Override
  public Iteratable<IBinding> findOverriddenMethods(final IRNode methodDeclaration) {
    final IteratableHashSet<IBinding> overridden = new IteratableHashSet<IBinding>();
    for (Iterator<IBinding> it = findOverriddenParentMethods(methodDeclaration); it.hasNext();) {
    	IBinding pm = it.next();      
      //System.out.println("findOverriddenMethods at "+JavaNames.getFullName(pm.getNode()));
      if (!overridden.contains(pm)) {
    	// Only add/process the method if it hasn't been done before
        overridden.add(pm);
        for (Iterator<IBinding> it2 = findOverriddenMethods(pm.getNode()); it2.hasNext();) {
          overridden.add(it2.next());
        }
      }
    }
    return overridden;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.bind.IBinder#findOverridingMethodsFromType(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.ir.IRNode)
   */
  @Override
  public Iteratable<IRNode>  findOverridingMethodsFromType(IRNode callee, IRNode receiverType) {
	  final ITypeEnvironment tEnv = getTypeEnvironment();
	  final IJavaType dt = tEnv.convertNodeTypeToIJavaType(receiverType);
	  return findOverriddenMethodsFromType(new FindOverriddenMethodsStrategy(
			  this, callee, true), callee, dt);
  }
  
  @SuppressWarnings("unchecked")
  private Iteratable<IRNode> findOverriddenMethodsFromType(
		  ISubTypeSearchStrategy s, IRNode method, IJavaType startType) {
	  final Operator op = JJNode.tree.getOperator(method);
	  if (op instanceof ConstructorDeclaration) {
		  return new EmptyIterator<IRNode>();
	  } else if (op instanceof AnnotationElement) {		  
		  // TODO just continue?
	  } else if (!(op instanceof MethodDeclaration)) {
		  //if (!warnedSet.contains(method)) {
			  LOG.warning("findOverriddenMethod: trying to match against "
					  + op.name() + ": " + DebugUnparser.toString(method));
		  //  warnedSet.add(method);
		  //}
		  return new EmptyIterator<IRNode>();
	  }

	  final IRNode methodsTypeD = VisitUtil.getEnclosingType(method);
	  if (startType == null || methodsTypeD == null) {
		  return new EmptyIterator<IRNode>();
	  }	  	 
	  // Need to make sure that startType is a subtype of methodsType
	  final ITypeEnvironment tEnv = getTypeEnvironment();
	  final IJavaType methodsType = tEnv.convertNodeTypeToIJavaType(methodsTypeD);
	  if (!startType.equals(methodsType)
			  && !tEnv.isSubType(startType, methodsType)) {
		  return new EmptyIterator<IRNode>();
	  }

	  if (LOG.isLoggable(Level.FINE))
		  LOG.fine("Starting from " + startType.getName());

	  findXinSubclasses(s, startType);	  

	  Iteratable<IRNode> it;
	  Object o = s.getResult();
	  if (o instanceof Iteratable) {
		  it = (Iteratable<IRNode>) o;
	  } else {
		  it = new SimpleIteratable<IRNode>((Iterator<IRNode>) o);
	  }

	  if (it == null) {
		  final String context =  startType.getName();
		  LOG.severe("Couldn't find " + s.getLabel() + " in " + context);
	  }

	  if (LOG.isLoggable(Level.INFO)) {
		  String msg;
		  if (it.hasNext()) {
			  msg = "Matched 1+ methods against ";
		  } else {
			  msg = "No matches against ";
		  }
		  if (LOG.isLoggable(Level.FINE))
			  LOG.fine(msg + DebugUnparser.toString(method));
	  }
	  return it;
  }

  /*
   * Searches for X, starting at the given type and its subclasses
   */
  @SuppressWarnings("unchecked")
  public void findXinSubclasses(ISubTypeSearchStrategy s, IJavaType type) {
	  if (type == null) {
		  return;
	  }
	  if (type instanceof IJavaDeclaredType) {
		  IJavaDeclaredType dt = (IJavaDeclaredType) type;
		  IRNode typeD = dt.getDeclaration();	  
		  Operator op = JJNode.tree.getOperator(typeD);
		  if (ClassDeclaration.prototype.includes(op)) {
			  findXinClass_down(s, dt);
		  } else if (InterfaceDeclaration.prototype.includes(op)) {
			  findXinInterface_down(s, dt);
		  } else if (AnonClassExpression.prototype.includes(op)) {
			  findXinClass_down(s, dt);
		  } else if (AnnotationDeclaration.prototype.includes(op)) {
			  findXinAnnotation_down(s, dt);
		  } else if (EnumDeclaration.prototype.includes(op)) {
			  findXinEnum_down(s, dt);
		  } else if (EnumConstantClassDeclaration.prototype.includes(op)) {
			  findXinEnumConstantClassDecl_down(s, dt);
		  } else {
			  LOG.severe("Calling findXinSubclasses on unhandled op "
					  + op.name());
			  return;
		  }
	  } else if (type instanceof IJavaTypeFormal) {
		  findXinTypeFormal_down(s, (IJavaTypeFormal) type);
	  } else {
		  LOG.severe("Calling findXinSubclasses on unhandled type "+type);
		  return;
	  }
  }
  
	/*
	 * Helper for findXinType Only to be called by findXinType and its helpers
	 */
	@SuppressWarnings("unchecked")
	private void findXinClass_down(ISubTypeSearchStrategy s, IJavaDeclaredType type) {
		if (type == null) {
			return;
		}
		s.visitClass(type.getDeclaration());

		if (s.visitSubclasses()) {
			for(IJavaType sub : getSubclasses(type)) {
				findXinClass_down(s, (IJavaDeclaredType) sub);
			}
		}
	}
	@SuppressWarnings("unchecked")
	private void findXinAnnotation_down(ISubTypeSearchStrategy s, IJavaDeclaredType type) {
		if (type == null) {
			return;
		}
		s.visitClass(type.getDeclaration());
	}
	@SuppressWarnings("unchecked")
	private void findXinInterface_down(ISubTypeSearchStrategy s, IJavaDeclaredType type) {
		if (type == null) {
			return;
		}
		// LOG.info("findXinInterface_down @"+InterfaceDeclaration.getId(type));
		s.visitInterface(type.getDeclaration());

		if (s.visitSubclasses()) {
			for(IJavaType sub : getSubclasses(type)) {
				findXinSubclasses(s, sub);
			}
		}
	}
	@SuppressWarnings("unchecked")
	private void findXinTypeFormal_down(ISubTypeSearchStrategy s, IJavaTypeFormal formal) {
		/*
		for (IRNode bound : getFormalBounds(formal)) {
			findXinSubclasses(s, bound);
		}
		*/
		// No subclasses
		return;
	}
	@SuppressWarnings("unchecked")
	private void findXinEnum_down(ISubTypeSearchStrategy s, IJavaDeclaredType type) {
		findXinClass_down(s, type);
	}
	
	@SuppressWarnings("unchecked")
	private void findXinEnumConstantClassDecl_down(ISubTypeSearchStrategy s,
			IJavaDeclaredType type) {
		findXinClass_down(s, type);
	}
	
	private Iterable<IJavaType> getSubclasses(IJavaDeclaredType root) {
		final ITypeEnvironment tEnv = getTypeEnvironment();
		final Iterator<IRNode> it   = tEnv.getRawSubclasses(root.getDeclaration()).iterator();
		return new FilterIterator<IRNode, IJavaType>(it) {
			@Override
			protected Object select(IRNode o) {
				// TODO what about root's type arguments?
				return tEnv.convertNodeTypeToIJavaType(o);
			}
		};
	}
}
