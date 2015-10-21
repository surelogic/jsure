/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/JavaTypeFactory.java,v 1.84 2008/12/12 19:01:02 chance Exp $
 *
 * Created May 26, 2004 
 */
package edu.cmu.cs.fluid.java.bind;

import java.io.IOException;
import java.io.PrintStream;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.*;
import java.util.logging.Logger;

import com.surelogic.ast.IType;
import com.surelogic.ast.java.operator.IDeclarationNode;
import com.surelogic.ast.java.operator.ITypeDeclarationNode;
import com.surelogic.ast.java.operator.ITypeFormalNode;
import com.surelogic.common.Pair;
import com.surelogic.common.SLUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.util.AppendIterator;
import com.surelogic.common.util.EmptyIterator;
import com.surelogic.common.util.Iteratable;
import com.surelogic.common.util.SingletonIterator;
import com.surelogic.javac.Projects;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.NotImplemented;
import edu.cmu.cs.fluid.debug.DebugUtil;
import edu.cmu.cs.fluid.ir.AbstractIRNode;
import edu.cmu.cs.fluid.ir.Cleanable;
import edu.cmu.cs.fluid.ir.CleanableMap;
import edu.cmu.cs.fluid.ir.IRInput;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRNodeHashedMap;
import edu.cmu.cs.fluid.ir.IROutput;
import edu.cmu.cs.fluid.ir.IRPersistent;
import edu.cmu.cs.fluid.ir.IRType;
import edu.cmu.cs.fluid.ir.SlotUndefinedException;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaOperator;
import edu.cmu.cs.fluid.java.bind.IJavaType.BooleanVisitor;
import edu.cmu.cs.fluid.java.bind.TypeInference8.ReboundedTypeFormal;
import edu.cmu.cs.fluid.java.operator.AnnotationElement;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.ArrayDeclaration;
import edu.cmu.cs.fluid.java.operator.ArrayType;
import edu.cmu.cs.fluid.java.operator.BooleanType;
import edu.cmu.cs.fluid.java.operator.ByteType;
import edu.cmu.cs.fluid.java.operator.CharType;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.DoubleType;
import edu.cmu.cs.fluid.java.operator.FloatType;
import edu.cmu.cs.fluid.java.operator.IntType;
import edu.cmu.cs.fluid.java.operator.IntegralType;
import edu.cmu.cs.fluid.java.operator.InterfaceDeclaration;
import edu.cmu.cs.fluid.java.operator.LongType;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.MoreBounds;
import edu.cmu.cs.fluid.java.operator.NameType;
import edu.cmu.cs.fluid.java.operator.NamedType;
import edu.cmu.cs.fluid.java.operator.NewExpression;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterizedType;
import edu.cmu.cs.fluid.java.operator.PrimitiveType;
import edu.cmu.cs.fluid.java.operator.ShortType;
import edu.cmu.cs.fluid.java.operator.Type;
import edu.cmu.cs.fluid.java.operator.TypeDeclInterface;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeExtensionInterface;
import edu.cmu.cs.fluid.java.operator.TypeFormal;
import edu.cmu.cs.fluid.java.operator.TypeFormals;
import edu.cmu.cs.fluid.java.operator.TypeRef;
import edu.cmu.cs.fluid.java.operator.UnionType;
import edu.cmu.cs.fluid.java.operator.VarArgsType;
import edu.cmu.cs.fluid.java.operator.VoidType;
import edu.cmu.cs.fluid.java.operator.WildcardExtendsType;
import edu.cmu.cs.fluid.java.operator.WildcardSuperType;
import edu.cmu.cs.fluid.java.operator.WildcardType;
import edu.cmu.cs.fluid.java.util.OpSearch;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.CustomizableHashCodeMap;
import edu.cmu.cs.fluid.util.ImmutableList;
import edu.cmu.cs.fluid.util.SingletonMap;

/**
 * Class that handles instances of classes that implement
 * {@link IJavaType}.  It ensures that two types are 'eq'
 * if they are 'equal'.  It also manages the persistence of
 * the instances.
 * @author boyland
 */
public class JavaTypeFactory implements IRType<IJavaType>, Cleanable {
  /** This prototype is needed only for IRType */
  public static final JavaTypeFactory prototype = new JavaTypeFactory();

  private static final Logger LOG = SLLogger.getLogger("FLUID.java");
  
  private JavaTypeFactory() {}

  /// type creation control

  // primitive types (plus void and null)
  public static final IJavaVoidType voidType = new JavaVoidType();
  public static final IJavaNullType nullType = new JavaNullType();
  public static final IJavaPrimitiveType byteType = new JavaPrimitiveType(ByteType.prototype);
  public static final IJavaPrimitiveType booleanType = new JavaPrimitiveType(BooleanType.prototype);
  public static final IJavaPrimitiveType charType = new JavaPrimitiveType(CharType.prototype);
  public static final IJavaPrimitiveType doubleType = new JavaPrimitiveType(DoubleType.prototype);
  public static final IJavaPrimitiveType floatType = new JavaPrimitiveType(FloatType.prototype);
  public static final IJavaPrimitiveType intType = new JavaPrimitiveType(IntType.prototype);
  public static final IJavaPrimitiveType longType = new JavaPrimitiveType(LongType.prototype);
  public static final IJavaPrimitiveType shortType = new JavaPrimitiveType(ShortType.prototype);
  
  public static final IJavaPrimitiveType[] primTypes = {
    byteType, booleanType, charType, doubleType, floatType, intType, longType, shortType    
  };
  
  public static final IJavaType anyType = new JavaReferenceType() {
    @Override
    public String toString() {
      return "*";
    }

    @Override
    public boolean isSubtype(ITypeEnvironment env, IJavaType t2) {
      return false;
    }
    @Override
    public boolean isAssignmentCompatible(ITypeEnvironment env, IJavaType t2, IRNode e2) {
      return false;
    }
    @Override
    public IJavaType getSuperclass(ITypeEnvironment env) {
      return null;
    }
    @Override
    public Iteratable<IJavaType> getSupertypes(ITypeEnvironment env) {
      return new EmptyIterator<IJavaType>();
    }    

    @Override
    public void visit(Visitor v) {
    	v.accept(this);
    	v.finish(this);
    }
    
    /*******************************************************
     * Added to implement IType
     *******************************************************/

    @Override
    public boolean isAssignmentCompatibleTo(IType t2) {
      return false;
    }
    @Override
    public boolean isSubtypeOf(IType t2) {
      return false;
    }
    @Override
    public IDeclarationNode getNode() {
      return null;
    }

    @Override
    void writeValue(IROutput out) throws IOException {
      throw new NotImplemented();
    }
  };
  
  private static Map<PrimitiveType,IJavaPrimitiveType> primitiveTypes = new HashMap<PrimitiveType,IJavaPrimitiveType>();
  private static Map<String,IJavaPrimitiveType> correspondingPrimTypes = new HashMap<String,IJavaPrimitiveType>();
  private static Map<IJavaPrimitiveType,String> correspondingDeclTypes = new HashMap<IJavaPrimitiveType,String>();
  static {
    // primitiveTypes.put(VoidType.prototype,new JavaVoidType());
    for (IJavaPrimitiveType pt : primTypes) {
      primitiveTypes.put(pt.getOp(), pt);
      correspondingPrimTypes.put(pt.getCorrespondingTypeName(), pt);
      correspondingDeclTypes.put(pt, pt.getCorrespondingTypeName());
    }
  }
  public static IJavaPrimitiveType getPrimitiveType(PrimitiveType op) {
    return primitiveTypes.get(op);
  }
  public static boolean hasCorrespondingPrimType(IJavaDeclaredType dt) {
    return getCorrespondingPrimType(dt) != null;
  }
  public static IJavaPrimitiveType getCorrespondingPrimType(IJavaDeclaredType dt) {
    String name = dt.getName();
    return correspondingPrimTypes.get(name);
  }
  public static IJavaDeclaredType getCorrespondingDeclType(ITypeEnvironment tEnv, IJavaPrimitiveType pt) {
	String name = correspondingDeclTypes.get(pt);
	return (IJavaDeclaredType) tEnv.findJavaTypeByName(name);
  }
  public static IJavaVoidType getVoidType() {
    return voidType;
  }
  
  public static IJavaNullType getNullType() {
    return nullType;
  }

  
  //private static final IRNodeHashedMap<IJavaTypeFormal> typeFormals = 
    //new IRNodeHashedMap<IJavaTypeFormal>();
  private static final ConcurrentMap<IRNode,IJavaTypeFormal> typeFormals =
	new ConcurrentHashMap<IRNode, IJavaTypeFormal>();
  
  public static /*synchronized*/ IJavaTypeFormal getTypeFormal(IRNode tf) {
    IJavaTypeFormal res = typeFormals.get(tf);
    if (res == null) {
      res = new JavaTypeFormal(tf);
      IJavaTypeFormal other = typeFormals.putIfAbsent(tf,res);
      if (other != null) {
    	  res = other;
      }
    }
    return res;
  }
  
  private static IRNodeHashedMap<IJavaDeclaredType> anonTypes = 
	    new IRNodeHashedMap<IJavaDeclaredType>();
	  
  public static synchronized IJavaDeclaredType getAnonType(IRNode ace) {
	  IJavaDeclaredType res = anonTypes.get(ace);
	  if (res == null) {
		  res = new JavaAnonType(ace);
		  anonTypes.put(ace,res);
	  }
	  return res;
  }
  
  private static JavaTypeCache2<IJavaReferenceType,IJavaReferenceType,JavaIntersectionType> intersectionTypes =
       new JavaTypeCache2<IJavaReferenceType,IJavaReferenceType,JavaIntersectionType>();
  
  public static synchronized IJavaIntersectionType getIntersectionType(IJavaReferenceType b1, IJavaReferenceType b2) {
    JavaIntersectionType type = intersectionTypes.get(b1, b2);
    if (type == null) {
      type = new JavaIntersectionType((JavaReferenceType)b1,(JavaReferenceType)b2);
    }
    return type;
  }
  
  private static JavaTypeCache2<IJavaWildcardType, Pair<IJavaReferenceType,IJavaReferenceType>, JavaCaptureType> captureTypes = 
       new JavaTypeCache2<IJavaWildcardType, Pair<IJavaReferenceType,IJavaReferenceType>, JavaCaptureType>();
  
  public static synchronized IJavaReferenceType getCaptureType(IJavaWildcardType wt, IJavaReferenceType lower, IJavaReferenceType upper) { 
	Pair<IJavaReferenceType,IJavaReferenceType> bounds = new Pair<IJavaReferenceType,IJavaReferenceType>(lower, upper);
    JavaCaptureType ct = captureTypes.get(wt,bounds);
    if (ct == null) {
      ct = new JavaCaptureType((JavaWildcardType)wt, lower, upper);
      captureTypes.put(wt,bounds,ct);
    }
    return ct;    
  }
  
  /*
  private static boolean onlyObjectBounds(List<IJavaReferenceType> bounds) {
    for(IJavaReferenceType bound : bounds) {
      if (!SLUtility.JAVA_LANG_OBJECT.equals(bound.getName())) {
        return false;
      }
    }
    return true;
  }
  */
  public static final IJavaWildcardType wildcardType = new JavaWildcardType(null,null);
  private static CleanableMap<IJavaType,JavaWildcardType> upperBounded = new JavaTypeCache<IJavaType,JavaWildcardType>();
  private static CleanableMap<IJavaType,JavaWildcardType> lowerBounded = new JavaTypeCache<IJavaType,JavaWildcardType>();
  private static JavaTypeCache2<IJavaReferenceType,IJavaReferenceType,JavaWildcardType> dualBounded =
	      new JavaTypeCache2<IJavaReferenceType,IJavaReferenceType,JavaWildcardType>();
  
  public static synchronized IJavaWildcardType getWildcardType(IJavaReferenceType upper, IJavaReferenceType lower) {
    JavaWildcardType res;
    if (upper == null) {
      if (lower == null) return wildcardType;
      res = lowerBounded.get(lower);
      if (res == null) {
        res = new JavaWildcardType(null,(JavaReferenceType)lower);
        lowerBounded.put(lower,res);
      }
    } else if (lower == null) {
      if (upper instanceof IJavaDeclaredType && SLUtility.JAVA_LANG_OBJECT.equals(upper.getName())) {
        return wildcardType; // HACK?
      }
      res = upperBounded.get(upper);
      if (res == null) {
        res = new JavaWildcardType((JavaReferenceType)upper,null);
        upperBounded.put(upper,res);
      }
    } else {
      //throw new FluidError("cannot create wildcard with upper AND lower bounds");
      res = dualBounded.get(upper, lower);
      if (res == null) {
    	res = new JavaWildcardType((JavaReferenceType) upper, (JavaReferenceType)lower);
    	dualBounded.put(upper,lower,res);
      }
    }
    return res;
  }
  
  private static JavaTypeCache2<IJavaReferenceType,IJavaReferenceType,JavaUnionType> unionTypes =
      new JavaTypeCache2<IJavaReferenceType,IJavaReferenceType,JavaUnionType>();
  
  public static IJavaType getUnionType(List<IJavaType> orig) {
	  if (orig.isEmpty()) {
		  throw new IllegalArgumentException();
	  }
	  List<IJavaType> types = new LinkedList<IJavaType>(orig);
	  Collections.sort(types, new Comparator<IJavaType>() {
		public int compare(IJavaType o1, IJavaType o2) {
			return o1.toString().compareTo(o2.toString());
		}
	  });
	  int size;
	  while ((size = types.size()) > 1) {
		  JavaReferenceType b2 = (JavaReferenceType) types.remove(size-1);
		  JavaReferenceType b1 = (JavaReferenceType) types.remove(size-2);
		  IJavaType u = getUnionType(b1, b2);
		  types.add(0, u);
	  }
	  return types.get(0);
  }
  
  static IJavaUnionType getUnionType(JavaReferenceType b1, JavaReferenceType b2) {
	  JavaUnionType t = unionTypes.get(b1, b2);
	  if (t == null) {
		  t = new JavaUnionType(b1, b2);
		  unionTypes.put(b1, b2, t);
	  }
	  return t;
  }
  
  // declared types  
  private static final int NUM_ROOTS = 1 << 4;
  private static final int ROOT_MASK = NUM_ROOTS - 1;
  private static final JavaDeclaredType[] rootTypes = new JavaDeclaredType[NUM_ROOTS];
  static {
	  initRootTypes();
  }  
  
  private static void initRootTypes() {
	  for(int i=0; i<NUM_ROOTS; i++) {
		  rootTypes[i] = new RootType();
	  }
  }
  
  private static int cleanupRootTypes() {
	  int sum = 0;
	  for(int i=0; i<NUM_ROOTS; i++) {
		  sum += rootTypes[i].cleanup();
	  }
	  return sum;
  }
  
  static class RootType extends JavaDeclaredType {
    @Override public String toString() { return ""; }
  };

  /** Return a declared type for the given declaration
   * and parameters nested in the given context (or null).
   * @param decl IRNode of type declaration (or an anonymous class expression,
   * apparently?)
   * @param params an immutable list of IJavaType for the type parameters.
   * nil is an acceptable parameter value and means no parameters (RAW).
   * @param outer the context type.
   */
  public static IJavaDeclaredType getDeclaredType(IRNode decl,
						  /* @immutable */ List<? extends IJavaType> params,
						  IJavaDeclaredType outer) 
  {
    if (decl == null) {
      throw new NullPointerException("type declaration is null!");
    }
    if (decl.identity() == IRNode.destroyedNode) {
    	LOG.warning("Trying to get IJavaType for a destroyed node");
    	return null;
    }
    if (outer == null) {
    	int i = decl.hashCode() & ROOT_MASK;
    	outer = rootTypes[i];
    }
    if (params == null || params.isEmpty()) {
      params = Collections.emptyList();
    }
    List<IJavaType> updatedParams = new ArrayList<IJavaType>(params);
    boolean allNull = true;
    int i=0;
    for(IJavaType p : params) {
    	if (p == null/* || p instanceof IJavaWildcardType*/) {
    		final IRNode tparams = TypeUtils.getParametersForType(decl);
    		final IRNode tparam = TypeFormals.getType(tparams, i);
    		final IJavaTypeFormal tf = getTypeFormal(tparam);    
    		// Hack to get type env
    		final ITypeEnvironment te = Projects.getEnclosingProject(decl).getTypeEnv();
    		final IJavaType bound = tf.getExtendsBound(te);
    		if (p == null) {
    			/*
    		    if (!bound.equals(te.getObjectType())) {
    			    System.err.println("Got null type parameter, replacing with "+bound);
    		    }
    			*/
    			updatedParams.set(i, bound);    			
    		}
    		/*
    		else if (!bound.equals(te.getObjectType())) {
    			allNull = false;
    			
    			// Non-trivial bound, so check if we can combine wildcard bounds w/ type parameter bounds
    			final IJavaWildcardType wildcard = (IJavaWildcardType) p;
    			final IJavaReferenceType wUpper = wildcard.getUpperBound();    			
    			final IJavaReferenceType fBound = (IJavaReferenceType) bound;
    			final IJavaReferenceType updatedUpper;
    			if (wUpper == null) {
    				updatedUpper = fBound;
    			}
    			else if (wUpper.isSubtype(te, bound)) {
    				continue; // Already contained
    			}
    			else if (bound.isSubtype(te, wUpper)) {
    				updatedUpper = fBound;
    			} 
    			else {
    				updatedUpper = JavaTypeFactory.getIntersectionType(wUpper, fBound);
    			}
    			updatedParams.set(i, JavaTypeFactory.getWildcardType(updatedUpper, wildcard.getLowerBound()));
    		} else { 
    			// Param is ? extends Object, so not null
    			allNull = false;
    		}
    		*/
    	} else {
    		allNull = false;
    	}
    	i++;
    }
    Operator op = JJNode.tree.getOperator(decl);
    if (TypeFormal.prototype.includes(op) || !(op instanceof TypeDeclInterface)) {
      throw new IllegalArgumentException();
    }
    IJavaDeclaredType result = ((JavaDeclaredType)outer).getNestedType(decl, allNull? Collections.<IJavaType>emptyList() : updatedParams);

    return result;
  }

  // array types
  private static CleanableMap<IJavaType,JavaArrayType> arrayTypes = new JavaTypeCache<IJavaType,JavaArrayType>();

  /** Return an array type for the given base type
   * and dimensions
   * @throws IllegalArgumentException if dims < 1
   */
  public static synchronized IJavaArrayType getArrayType(IJavaType t, int dims) {
    if (dims < 1) {
      throw new IllegalArgumentException("too few dimensions");
    }
    JavaArrayType a;
    do {
      if (t == null) {
        LOG.severe("Type of arrays is null?");
        return null;
      }
      a = arrayTypes.get(t);
      if (a == null) {
        a = new JavaArrayType(t);
        arrayTypes.put(t,a);
      }
      t = a;
      --dims;
    } while (dims > 0);
    return a;
  }
  
  // each function type is mapped to itself, a special kind of set:
  private static Map<JavaFunctionType,JavaFunctionType> functionTypes =
	      new HashMap<JavaFunctionType,JavaFunctionType>();
  private static final IJavaTypeFormal[] emptyTypeFormals = new IJavaTypeFormal[0];
  static final IJavaType[] emptyTypes = JavaGlobals.noTypes;
	

  public static IJavaFunctionType getFunctionType(
		  List<IJavaTypeFormal> typeFormals,
		  IJavaType returnType,
		  List<IJavaType> paramTypes,
		  boolean isVariable,
		  Set<IJavaType> throwTypes) {
	  if (typeFormals == null) typeFormals = Collections.emptyList();
	  if (paramTypes == null) paramTypes = Collections.emptyList();
	  if (throwTypes == null) throwTypes = Collections.emptySet();
	  JavaFunctionType ft = new JavaFunctionType(
			  typeFormals.toArray(emptyTypeFormals),
			  returnType,
			  paramTypes.toArray(emptyTypes),
			  isVariable,
			  throwTypes.toArray(emptyTypes));
	  JavaFunctionType result = functionTypes.get(ft);
	  if (result == null) {
		  result = ft;
		  functionTypes.put(ft, ft);
	  }
	  return ft;
  }
  
  static void clearCaches() {
	  // Nothing to do here
  }
  
  public static void clearAll() {
	  typeFormals.clear();
	  intersectionTypes.clear();
	  captureTypes.clear();
	  arrayTypes.clear();
	  lowerBounded.clear();
	  upperBounded.clear();
	  functionTypes.clear();
	  initRootTypes();
  }
  
  /**
   * Get the function type corresponding to a method or constructor declaration.
   * For a method declaration, the receiver can be 
   * @param memDecl node for method or constructor
   * @param receiverType type to use for receiver, or null if it should be omitted
   * For a constructor, the receiver type must be set.
   * @param binder binder (needed to perform task)
   * @return function type for this member
   */
  public static IJavaFunctionType getMemberFunctionType(
		  IRNode memDecl, 
		  IJavaType receiverType, 
		  IBinder binder) {
	  IRNode tformals;
	  IRNode formals;
	  IRNode throwsNode;
	  List<IJavaType> paramTypes = new ArrayList<IJavaType>();
	  Operator op = JJNode.tree.getOperator(memDecl);
	  IJavaType returnType;
	  if (MethodDeclaration.prototype.includes(op)) {
		  tformals = MethodDeclaration.getTypes(memDecl);
		  formals = MethodDeclaration.getParams(memDecl);
		  IRNode rtype = MethodDeclaration.getReturnType(memDecl);
		  returnType = binder.getJavaType(rtype);
		  if (receiverType != null) {
			  paramTypes.add(receiverType);
		  }
		  throwsNode = MethodDeclaration.getExceptions(memDecl);
	  } else if (ConstructorDeclaration.prototype.includes(op)) {
		  tformals = ConstructorDeclaration.getTypes(memDecl);
		  formals = ConstructorDeclaration.getParams(memDecl);
		  if (receiverType == null) {
			  throw new IllegalArgumentException("constructors need non-null receiver types");
		  }
		  returnType = receiverType;
		  throwsNode = ConstructorDeclaration.getExceptions(memDecl);
	  } else if (AnnotationElement.prototype.includes(op)) {
		  tformals = AnnotationElement.getTypes(memDecl);
		  formals = AnnotationElement.getParams(memDecl);
		  IRNode rtype = AnnotationElement.getType(memDecl);
		  returnType = binder.getJavaType(rtype);
		  if (receiverType != null) {
			  paramTypes.add(receiverType);
		  }
		  throwsNode = AnnotationElement.getExceptions(memDecl);
	  } else {
		  throw new IllegalArgumentException("passed a node of wrong type: " + op);
	  }
	  List<IJavaTypeFormal> typeFormals = null;
	  for (IRNode tf : JJNode.tree.children(tformals)) {
		  if (typeFormals == null) typeFormals = new ArrayList<IJavaTypeFormal>();
		  typeFormals.add(getTypeFormal(tf));
	  }
	  boolean isVariable = true;
	  for (IRNode f : JJNode.tree.children(formals)) {
		  IRNode ptype = ParameterDeclaration.getType(f);
		  paramTypes.add(binder.getJavaType(ptype));
		  if (VarArgsType.prototype.includes(JJNode.tree.getOperator(ptype))) {
			  isVariable = true;
		  }
	  }
	  Set<IJavaType> throwTypes = null;
	  for (IRNode et : JJNode.tree.children(throwsNode)) {
		  if (throwTypes == null) throwTypes = new HashSet<IJavaType>();
		  throwTypes.add(binder.getJavaType(et));
	  }
	  return getFunctionType(typeFormals,returnType,paramTypes,isVariable,throwTypes);
  }
  
  /**
   * Convert an IRNode based type to the new format.
   * @param nodeType
   * @return
   */
  public static IJavaType convertNodeTypeToIJavaType(IRNode nodeType, IBinder binder) {
	if (nodeType == null) {
		return null;
	}
    // TODO Change to use a Visitor, more efficient
    Operator op = JJNode.tree.getOperator(nodeType);
    if (op instanceof PrimitiveType) {
      return getPrimitiveType((PrimitiveType)op);
    } else if (op == TypeDeclaration.prototype) { // formerly a hacked nullType
      return getNullType();
    } else if (op instanceof VoidType) {
      return getVoidType();      
    } else if (op instanceof ArrayType) {
      IJavaType bt = convertNodeTypeToIJavaType(ArrayType.getBase(nodeType),binder);
      return getArrayType(bt, ArrayType.getDims(nodeType));
    } else if (op instanceof NamedType || op instanceof NameType) {
      IBinding b = binder.getIBinding(nodeType);
      if (b == null) {
    	  binder.getIBinding(nodeType);
    	  return null; // program may have binding error
      } 
      IRNode decl = b.getNode();
      // We now let the IBinding do the work for us: 
      {
    	  /*
    	  String name = DebugUnparser.toString(nodeType);
    	  if (name.startsWith("CachedValue")) {
    		  System.out.println("Converting CachedValue");
    	  }
    	  */
    	  //return b.convertType(convertNodeTypeToIJavaType(decl, binder));
    	  return b.convertType(binder, getMyThisType(decl, true, !isRelatedTo(binder.getTypeEnvironment(), nodeType, decl)));
      }  
    } else if (op instanceof TypeRef) {
      IJavaType outer = convertNodeTypeToIJavaType(TypeRef.getBase(nodeType),binder);
      IBinding b = binder.getIBinding(nodeType);
      if (b == null) {
    	  return null;
      }
      return b.convertType(binder, getDeclaredType(b.getNode(),null,(IJavaDeclaredType)outer));
    } else if (op instanceof TypeFormal) {
      return getTypeFormal(nodeType);      
    } else if (op instanceof TypeDeclInterface) {
      return getMyThisType(nodeType);
    } else if (op instanceof ArrayDeclaration) {
      IJavaType bt = convertNodeTypeToIJavaType(ArrayDeclaration.getBase(nodeType),binder);
      return getArrayType(bt, ArrayDeclaration.getDims(nodeType));
    } else if (op instanceof ParameterizedType) {
      IRNode baseNode = ParameterizedType.getBase(nodeType);
      IJavaType bt = convertNodeTypeToIJavaType(baseNode, binder);
      
      if (!(bt instanceof JavaDeclaredType)) {
          LOG.severe("parameterizing what? " + bt);
          return bt;
      }
      JavaDeclaredType base = (JavaDeclaredType)bt;
      List<IJavaType> typeActuals = new ArrayList<IJavaType>();
      IRNode args = ParameterizedType.getArgs(nodeType);
      IRNode tformals;
      if (ClassDeclaration.prototype.includes(base.getDeclaration())) {
    	  tformals = ClassDeclaration.getTypes(base.getDeclaration());
      } else {
    	  tformals = InterfaceDeclaration.getTypes(base.getDeclaration());
      }
      Iterator<IRNode> it = JJNode.tree.children(tformals);
      for (Iterator<IRNode> ch = JJNode.tree.children(args); ch.hasNext();) {
        IRNode arg = ch.next();
        IRNode tf = it.next();
        IJavaType ta = convertNodeTypeToIJavaType(arg,binder);
        /*
        if (ta instanceof IJavaWildcardType) {
        	IJavaTypeFormal f = JavaTypeFactory.getTypeFormal(tf);
        	if (f.toString().contains("Enum")) {
        		System.out.println("Got Enum formal");
        	}
        	IJavaReferenceType bound = f.getExtendsBound(binder.getTypeEnvironment());
        	if (!bound.equals(binder.getTypeEnvironment().getObjectType())) {
            	IJavaWildcardType wt = (IJavaWildcardType) ta;
            	IJavaReferenceType upper;
            	if (wt.getUpperBound() == null) {
            		upper = bound;
            	} else {
            		TypeUtils helper = new TypeUtils(binder.getTypeEnvironment());
            		upper = helper.getLowestUpperBound(wt.getUpperBound(), bound);
            	}
            	ta = JavaTypeFactory.getWildcardType(upper, wt.getLowerBound());
        	}
        }
        */
        typeActuals.add(ta);
      }
      if (AbstractJavaBinder.isBinary(nodeType) && bt == null) {    	
    	System.err.println("Null base type for "+DebugUnparser.toString(nodeType));
    	return null;  
      }      
      /* Check unneeded due to changes for NamedType
      if (base.getTypeParameters().size() > 0) {
        LOG.severe("Already has parameters! " + bt);
        return bt;
      }
      */
      IJavaDeclaredType outer = null;
      if (base instanceof JavaDeclaredType.Nested) {
        outer = ((JavaDeclaredType.Nested)base).getOuterType();
      }
      IJavaType rv = getDeclaredType(base.getDeclaration(),typeActuals,outer);
      if (rv == null) {
    	  if (AbstractJavaBinder.isBinary(nodeType)) {
    		  System.err.println("Couldn't create IJavaType for "+DebugUnparser.toString(nodeType)+" in binary");
    	  } else {
    		  throw new IllegalStateException("Couldn't create IJavaType for "+DebugUnparser.toString(nodeType));
    	  }
      }
      IBinding baseB = binder.getIBinding(baseNode);
      if (baseB != null && !TypeUtil.isStatic(baseB.getNode())) {
    	  // Adjust for outer class parameters only if non-static
    	  rv = baseB.convertType(binder, rv);
      }
      // WILDCARD
      //rv = JavaTypeVisitor.captureWildcards(binder, rv);
      return rv;
    } else if (op instanceof WildcardSuperType) {
      IJavaReferenceType st = (IJavaReferenceType) convertNodeTypeToIJavaType(WildcardSuperType.getLower(nodeType),binder);
      return getWildcardType(null,st);
    } else if (op instanceof WildcardExtendsType) {
      IJavaReferenceType st = (IJavaReferenceType) convertNodeTypeToIJavaType(WildcardExtendsType.getUpper(nodeType),binder);
      return getWildcardType(st,null);
    } else if (op instanceof WildcardType) {   
      return getWildcardType(null,null);
    } else if (op instanceof VarArgsType) {
      IJavaType bt = convertNodeTypeToIJavaType(VarArgsType.getBase(nodeType),binder);
      return getArrayType(bt, 1);
    } else if (op instanceof MoreBounds) {
      return computeGreatestLowerBound(binder, null, nodeType, IJavaTypeSubstitution.NULL); // TODO is this right?
    } else if (op instanceof UnionType) {
        final List<IJavaType> types = new ArrayList<IJavaType>();
        for(IRNode t : UnionType.getTypeIterator(nodeType)) {
      	  types.add(convertNodeTypeToIJavaType(t, binder));
        }
        return JavaTypeFactory.getUnionType(types);      
    } else if (op instanceof Type) {
      return binder.getJavaType(nodeType);
      //return JavaTypeVisitor.getJavaType(nodeType, binder);
    } else {
      LOG.severe("Cannot convert type " + op);
      return null;
    }
  }

  /**
   * Check if other is related to n
   * e.g. n is not in a subclass/inner class of other
   */
  public static boolean isRelatedTo(ITypeEnvironment tEnv, IRNode n, IRNode other) {	  
	  // Need to check if n is part of the extends/implements -- otherwise, infinite loop
	  final IRNode parent = OpSearch.nonTypeSearch.findEnclosing(n);
	  final Operator pop = JJNode.tree.getOperator(parent);
	  if (pop instanceof TypeExtensionInterface) {
		  return true;
	  }
	  // Check if n specifies the superclass
	  if (ClassDeclaration.prototype.includes(parent)) {
		  return true;
	  }
	  if (NewExpression.prototype.includes(parent)) {
		  final IRNode gparent = JJNode.tree.getParentOrNull(parent);
		  if (AnonClassExpression.prototype.includes(gparent)) {
			  return true;
		  }
	  }
	  final IJavaType tt = getThisType(n);
	  final IJavaSourceRefType thisType = (IJavaSourceRefType) tt;
	  if (thisType == null) {
		  return false;
	  }
	  final IJavaSourceRefType otherType = (IJavaSourceRefType) getMyThisType(other); // Just to get supertype
	  /*
	  final String msg = "Checking if "+thisType+" is related to "+otherType;
	  if ("Checking if vuze.ACETest.ACE #1 is related to vuze.ACETest.Inner".equals(msg) ||
          "Checking if E extends java.lang.Enum <E> in java.lang.Enum is related to java.lang.Enum<E extends java.lang.Enum <E> in java.lang.Enum>".equals(msg)) {
		  System.err.println("Got infinite loop");
	  } else {
		  System.err.println(msg);
	  }
	  */
	  return isRelatedTo(tEnv, thisType, otherType);
  }

  private static boolean isRelatedTo(final ITypeEnvironment tEnv, final IJavaSourceRefType thisT, final IJavaSourceRefType otherT) {  
	  if (thisT.getDeclaration().equals(otherT.getDeclaration())) {
		  return true;
	  }
	  if (thisT instanceof IJavaDeclaredType) {
		  IJavaDeclaredType tt = (IJavaDeclaredType) thisT;
		  if (tt.getOuterType() != null && isRelatedTo(tEnv, tt.getOuterType(), otherT)) {
			  return true;
		  }
	  }
	  if (otherT instanceof IJavaDeclaredType) {
		  IJavaDeclaredType ot = (IJavaDeclaredType) otherT;
		  if (ot.getOuterType() != null && isRelatedTo(tEnv, thisT, ot.getOuterType())) {
			  return true;
		  }		  
	  }
	  for(IJavaType parent : thisT.getSupertypes(tEnv)) {
		  if (isRelatedTo(tEnv, (IJavaSourceRefType) parent, otherT)) {
			  return true;
		  }
	  }
	  return false;
  }
  
  // Meant to compute glb(Bi, Ui[A1 := S1, ..., An := Sn]) 
  public static IJavaReferenceType computeGreatestLowerBound(IBinder binder, IJavaReferenceType wildcardBound, IRNode moreBounds, IJavaTypeSubstitution subst) {	  
      IJavaReferenceType result = null;
	  /*
      for (IRLocation loc = JJNode.tree.lastChildLocation(moreBounds); loc != null; loc = JJNode.tree.prevChildLocation(moreBounds, loc)) {
        IJavaReferenceType bound = (IJavaReferenceType)convertNodeTypeToIJavaType(JJNode.tree.getChild(moreBounds, loc),binder);
        if (result == null) result = bound;
        else result = getIntersectionType(bound,result);
      }
      */
      final int num = JJNode.tree.numChildren(moreBounds);
      if (num <= 0) {
    	result = wildcardBound;  
      } else {
    	  final List<IJavaReferenceType> bounds = new ArrayList<IJavaReferenceType>(wildcardBound == null ? num : num+1);
    	  if (wildcardBound != null) {
    		  bounds.add(wildcardBound);
    	  }
    	  for(IRNode b : MoreBounds.getBoundIterator(moreBounds)) {
    		  final IJavaReferenceType bt = (IJavaReferenceType) binder.getJavaType(b);
    		  final IJavaReferenceType btSubst = bt;// TODO (IJavaReferenceType) bt.subst(subst);
    		  bounds.add(btSubst);
    	  }
    	  return new TypeUtils(binder.getTypeEnvironment()).getGreatestLowerBound(bounds.toArray(new IJavaReferenceType[bounds.size()]));
      }
      if (result == null) {
        return binder.getTypeEnvironment().getObjectType();
      } else {
        return result;
      }

  }
    
  public static IJavaSourceRefType getMyThisType(IRNode tdecl) {
	  return getMyThisType(tdecl, false, false);
  }
  
  private static IJavaDeclaredType computeRawType(IJavaDeclaredType dt) {
	  IJavaDeclaredType outer = dt.getOuterType();
	  if (outer != null && !outer.getTypeParameters().isEmpty() && TypeUtil.isStatic(dt.getDeclaration())) {
		  outer = computeRawType(outer);
	  }
	  return getDeclaredType(dt.getDeclaration(), null, outer);
  }
  
  /**
   * Return the "this" type for this declaration, what "this"
   * means inside this class.  The correct type actuals and
   * outer type are inferred from the structure.  This method doesn't care
   * about "static"; it's only interested in proper nesting and polymorphism. 
   * @param tdecl type declaration node
   * @return type of "this" within this class/interface.
   */
  public static IJavaSourceRefType getMyThisType(IRNode tdecl, boolean raw, boolean isUnrelated) {
    TypeDeclInterface op = (TypeDeclInterface)JJNode.tree.getOperator(tdecl);    
    if (op instanceof TypeFormal) {
      return JavaTypeFactory.getTypeFormal(tdecl); 
    }
    if (op instanceof AnonClassExpression) {
      return JavaTypeFactory.getAnonType(tdecl);
    }
    IJavaDeclaredType outer = (IJavaDeclaredType) getThisType(tdecl);
    if (outer != null && !outer.getTypeParameters().isEmpty() && (isUnrelated || TypeUtil.isStatic(tdecl))) {
    	outer = computeRawType(outer);
    }
    IRNode typeFormals = null;
    if (op instanceof ClassDeclaration) {
      typeFormals = ClassDeclaration.getTypes(tdecl);
    } else if (op instanceof InterfaceDeclaration) {
      typeFormals = InterfaceDeclaration.getTypes(tdecl);
    }
    List<IJavaType> tactuals = null;
    if (!raw && typeFormals != null) {
      int num = JJNode.tree.numChildren(typeFormals);
      tactuals = new ArrayList<IJavaType>(num);
      for (Iterator<IRNode> tfs = JJNode.tree.children(typeFormals); tfs.hasNext();) {
        tactuals.add(getTypeFormal(tfs.next()));
      }
    }
    return getDeclaredType(tdecl,tactuals,outer);
  }
  
  /**
   * Return the type of "this" in this context (whether or not the
   * context is actually static).  The type will have outer types and
   * polymorphism correctly in place.  If the node is a declaration, it gives the type
   * of "this" in the <i>surrounding</i> context.
   * @param n a node in an AST.
   * @return type of this at this point.
   */
  public static IJavaSourceRefType getThisType(IRNode n) {
    IRNode p = VisitUtil.getEnclosingTypeForPromise(n);
    if (p == null) {
    	return null;
    }
    return getMyThisType(p);
  }
  
  
  /// IRType methods:

  /**
   * Return the IRType
   */
  public static IRType<IJavaType> getIJavaTypeType() { return prototype; }

  public boolean isValid(Object x) {
    return x instanceof IJavaType;
  }

  /**
   * Return a null comparator.
   * Do not use!
   * @return null comparator
   */
  public Comparator<IJavaType> getComparator() { return null; }

  public void writeValue(IJavaType value, IROutput out) throws IOException {
    JavaType t = (JavaType)value;
    t.writeValue(out);
  }

  public IJavaType readValue(IRInput in) throws IOException {
    int c = in.readByte();
    // we use abbreviations reminiscent of the class file format for types:
    switch (c) {
    default:
      throw new IOException("Cannot start an IJavaType value: '"+(char)c+"'");
    case 'Z': return booleanType;
    case 'B': return byteType;
    case 'C': return charType;
    case 'D': return doubleType;
    case 'F': return floatType;
    case 'I': return intType;
    case 'J': return longType;
    case 'S': return shortType;
    case 'V': return getVoidType();
    case 'L': return getDeclaredType(in.readNode(),readTypeList(in),null);
    case '[': return getArrayType((IJavaType)readValue(in),1);
      // nested types done separately
    case 'N': return getDeclaredType(in.readNode(),readTypeList(in),(IJavaDeclaredType)readValue(in));
    case '0': return getNullType();
      // new things for Java 5
    case 'T': return getTypeFormal(in.readNode());
    case '&': return getIntersectionType((IJavaReferenceType)readValue(in),(IJavaReferenceType)readValue(in));
    case '?': return getWildcardType(null,null);
    case '-': return getWildcardType(null,(IJavaReferenceType)readValue(in));
    case '+': return getWildcardType((IJavaReferenceType)readValue(in),null);
   }
  }
  
  private List<IJavaType> readTypeList(IRInput in) throws IOException {
    int n = in.readUnsignedByte();
    ImmutableList<IJavaType> res = ImmutableList.nil();
    if (n == 0) return res;
    List<IJavaType> l = new ArrayList<IJavaType>();
    for (int i=0; i < n; ++i) {
      l.add((IJavaType)readValue(in));
    }
    for (ListIterator<IJavaType> it = l.listIterator(l.size()); it.hasPrevious();) {
      res = ImmutableList.cons(it.previous(),res);
    }
    return res;
  }

  public void writeType(IROutput out) throws IOException {
    out.writeByte(TYPE_BYTE);
  }

  public IRType<IJavaType> readType(IRInput in) throws IOException {
    return this;
  }

  public IJavaType fromString(String s) {
    return null; // TODO
  }

  public String toString(IJavaType o) {
    return o.toString();
  }

  private static final int TYPE_BYTE = 'J';

  static {
    IRPersistent.registerIRType(prototype,TYPE_BYTE);
  }
  
  public static int doCleanup() {
    return
       //convertedTypeCache.cleanup() +
       //typeFormals.cleanup() +
       arrayTypes.cleanup() +
       captureTypes.cleanup() +
       upperBounded.cleanup() +
       lowerBounded.cleanup() + 
       cleanupRootTypes();
  }
  
  public int cleanup() {
    return doCleanup();
  }
}

abstract class JavaTypeCleanable {
	  /**
	   * Whether this type is currently valid, doesn't use deleted nodes.
	   * This implementation returns true by default.
	   * @return true if not using deleted nodes
	   */
	  public boolean isValid() {
	    return true;
	  }
	  
	  /**
	   * Perform any cleanup of any nested caches.
	   * By default, do nothing
	   * @return some indication of how much cleanup was done.
	   */
	  int cleanup() {
	    return 0;
	  }	
}

abstract class JavaType extends JavaTypeCleanable implements IJavaType {
  
  public IJavaType subst(IJavaTypeSubstitution s) {
    return this; // assume no change;
  }
  
  abstract void writeValue(IROutput out) throws IOException;

  public final String getName() { return toString(); }
  
  public String toSourceText() { return toString(); }
  
  public String toFullyQualifiedText() {
	  return toSourceText();
  }
  
  public boolean isSubtype(ITypeEnvironment env, IJavaType t2) {
    return env.isSubType(this, t2);
  }
  
  public boolean isAssignmentCompatible(ITypeEnvironment env, IJavaType t2, IRNode e2) {
    return env.isAssignmentCompatible(this, t2, e2);
  }

  public IJavaType getSuperclass(ITypeEnvironment env) {
    return null;
  }

  public Iteratable<IJavaType> getSupertypes(ITypeEnvironment env) {
    return env.getSuperTypes(this);
  }
  
  
  public void printStructure(PrintStream out, int indent) {
    DebugUtil.println(out, indent, this.getClass().getSimpleName()+": "+this.toString());
  }
  
  public boolean isEqualTo(ITypeEnvironment env, IJavaType t2) {
	  if (this == t2) {
		  return true;
	  }
	  if (this instanceof IJavaDeclaredType && t2 instanceof IJavaDeclaredType) {
		  IJavaDeclaredType dt = (IJavaDeclaredType) this;
		  IJavaDeclaredType dt2 = (IJavaDeclaredType) t2;
		  return AbstractTypeEnvironment.areEquivalent(env, dt, dt2);
	  }
	  if (t2 instanceof IJavaTypeFormal && this instanceof IJavaTypeFormal) {
		  IJavaTypeFormal tf = (IJavaTypeFormal) t2;
		  return tf.isEqualTo(env, this);
	  }
	  return false;
  }

  public void visit(Visitor v) {
	  if (v.accept(this)) {
		  v.finish(this);
	  }
  }
  
  /*******************************************************
   * Added to implement IType
   *******************************************************/

  public boolean isAssignmentCompatibleTo(IType t2) {
    throw new NotImplemented();
//    ITypeEnvironment env = null;
//    IRNode e2 = null;
//    return env.isAssignmentCompatible(this, (IJavaType) t2, e2);
  }

  public boolean isSubtypeOf(IType t2) {
    throw new NotImplemented();
//    ITypeEnvironment env = null;
//    return env.isSubType(this, (IJavaType) t2);
  }

  public IDeclarationNode getNode() {
    throw new NotImplemented("Not defined yet");
  }
}

class JavaPrimitiveType extends JavaType implements IJavaPrimitiveType {
  final PrimitiveType op;
  final String name;
  final Kind k;

  JavaPrimitiveType(PrimitiveType op) {
    this.op = op;
    if (op instanceof IntegralType) {
      if (op == IntType.prototype) {
        name = "java.lang.Integer";
        k = Kind.INT;
      }
      else if (op == LongType.prototype) {
        name = "java.lang.Long";
        k = Kind.LONG;
      }
      else if (op == CharType.prototype) {
        name = "java.lang.Character";
        k = Kind.CHAR;
      }
      else if (op == ByteType.prototype) {
        name = "java.lang.Byte";
        k = Kind.BYTE;
      }
      else if (op == ShortType.prototype) {
        name = "java.lang.Short";
        k = Kind.SHORT;
      }
      else throw new IllegalArgumentException("unknown operator");
    } else {
      if (op == BooleanType.prototype) {
        name = "java.lang.Boolean";
        k = Kind.BOOLEAN;
      }
      else if (op == DoubleType.prototype) {
        name = "java.lang.Double";
        k = Kind.DOUBLE;
      }
      else if (op == FloatType.prototype) {
        name = "java.lang.Float";
        k = Kind.FLOAT;
      }
      else throw new IllegalArgumentException("unknown operator");
    }
  }

  public PrimitiveType getOp() { return op; }

  @Override
  void writeValue(IROutput out) throws IOException {
    byte b;
    if (op instanceof IntegralType) {
      if (op == IntType.prototype) b = 'I';
      else if (op == LongType.prototype) b = 'J';
      else if (op == CharType.prototype) b = 'C';
      else if (op == ByteType.prototype) b = 'B';
      else if (op == ShortType.prototype) b = 'S';
      else throw new IllegalArgumentException("unknown operator to write");
    } else {
      if (op == BooleanType.prototype) b = 'Z';
      else if (op == DoubleType.prototype) b = 'D';
      else if (op == FloatType.prototype) b = 'F';
      else throw new IllegalArgumentException("unknown operator to write");
    }
    out.writeByte(b);
  }
  
  @Override
  public String toString() {
    //return ((Operator)op).name();
    return ((JavaOperator)op).asToken().toString();
  }
  
  @Override
  public IJavaType getSuperclass(ITypeEnvironment env) {
    return null;
  }

  @Override
  public Iteratable<IJavaType> getSupertypes(ITypeEnvironment env) {
    return new EmptyIterator<IJavaType>();
  }
  
  /*******************************************************
   * Added to implement IPrimitiveType
   *******************************************************/
  
  public Kind getKind() {
    return k;
  }

  public String getCorrespondingTypeName() {
    return name;
  }
}

class JavaVoidType extends JavaType implements IJavaVoidType {
  @Override
  void writeValue(IROutput out) throws IOException {
    out.writeByte('V');
  }
  
  @Override
  public String toString() {
    return "void";
  }
  
  @Override
  public IJavaType getSuperclass(ITypeEnvironment env) {
    return null;
  }

  @Override
  public Iteratable<IJavaType> getSupertypes(ITypeEnvironment env) {
    return new EmptyIterator<IJavaType>();
  }
}

abstract class JavaReferenceType extends JavaType implements IJavaReferenceType {	  
	public boolean equalInTEnv(IJavaReferenceType other, ITypeEnvironment t) {
		return this == other;
	}
}

class JavaNullType extends JavaReferenceType implements IJavaNullType {
  @Override
  void writeValue(IROutput out) throws IOException {
    out.writeByte('0');
  }  
  @Override
  public String toString() {
    return "null-type";
  }
  
  @Override
  public IJavaType getSuperclass(ITypeEnvironment env) {
    throw new NotImplemented("what IJavaType for Object");
  }
}

class JavaTypeFormal extends JavaReferenceType implements IJavaTypeFormal {
  final IRNode declaration;
  JavaTypeFormal(IRNode d) {
    declaration = d;
  }
  
  public IRNode getDeclaration() {
    return declaration;
  }
  
  @Override
  public IJavaType subst(final IJavaTypeSubstitution s) {
    if (s == null) return this;
    if (s == IJavaTypeSubstitution.NULL) {
    	return this;
    }
    /*
	String unparse = toString();
	if (unparse.contains("in java.util.List.toArray")) {
		System.out.println("Subst for "+unparse);
	}
	*/
    IJavaType rv = s.get(this);
    if (rv == null) {
    	return getExtendsBound(s.getTypeEnv());
    }
    if (rv != this) {
    	return rv;
    }
    if (!couldNeedSubst(s,getExtendsBound(s.getTypeEnv()))) {
    	return this;
    }
    return new BoundedTypeFormal(this, s);
  }
  
  @Override
  void writeValue(IROutput out) throws IOException {
    out.writeByte('T');
    out.writeNode(declaration);
  }
  
  @Override
  public String toString() {
    IRNode decl = VisitUtil.getEnclosingDecl(declaration);
    return JavaNames.getTypeName(declaration)+" in "+JavaNames.getFullName(decl);
  }
  
  @Override
  public String toSourceText() {
    return TypeFormal.getId(declaration);
  }
  
  @Override
  public IJavaType getSuperclass(ITypeEnvironment env) {
    return getExtendsBound(env);
  }
  
  @Override
  public boolean isValid() {
    return !AbstractIRNode.isDestroyed(declaration);
  }
  
  @Override
  public boolean isEqualTo(ITypeEnvironment env, IJavaType t2) {
	  if (this == t2) {
		  return true;
	  }
	  if (t2 instanceof IJavaTypeFormal) {
		  IJavaTypeFormal tf = (IJavaTypeFormal) t2;
		  return declaration.equals(tf.getDeclaration()) &&
				  getSuperclass(env).isEqualTo(env, tf.getSuperclass(env));
	  }
	  return false;
  }
  
  /*******************************************************
   * Added to implement ITypeFormal
   *******************************************************/

  @Override
  public ITypeFormalNode getNode() {
    return (ITypeFormalNode) super.getNode();
  }
 
  public IJavaReferenceType getExtendsBound() {
    throw new FluidError("getExtendsBound() requires a type environment!");
  }
  
  public IJavaReferenceType getExtendsBound(ITypeEnvironment tEnv) {
    final IRNode bounds = TypeFormal.getBounds(declaration);
    final int num = JJNode.tree.numChildren(bounds);
    if (num <= 0) {
      return tEnv.getObjectType();
    }
    else if (num == 1) {        
    	IRNode first = MoreBounds.getBound(bounds, 0);
    	return (IJavaReferenceType) tEnv.convertNodeTypeToIJavaType(first);
    } 
    else {
    	return (IJavaReferenceType) 
    	JavaTypeFactory.convertNodeTypeToIJavaType(bounds, tEnv.getBinder());
    }
  }

  @Override
  public void printStructure(PrintStream out, int indent) {
    DebugUtil.println(out, indent, "TypeFormal: "); 
    JavaNode.dumpTree(out, declaration, indent+2);
  }

  public IJavaReferenceType getUpperBound(ITypeEnvironment te) {
	  return getExtendsBound(te);
  }
  
  public IJavaReferenceType getLowerBound() {
	  return null;
  }
  
  boolean couldNeedSubst(final IJavaTypeSubstitution s, IJavaType t) {
	  TypeFormalChecker c = new TypeFormalChecker();
	  t.visit(c);
	  return c.result && s.involves(c.formals);
  }
}

class TypeFormalChecker extends BooleanVisitor {
	final Set<IJavaTypeFormal> formals = new HashSet<>();
	
	@Override
	public boolean accept(IJavaType t) {
		if (t instanceof IJavaTypeFormal) {
			result = true;
			formals.add((IJavaTypeFormal) t);
		}
		return true;
	}
	
}

class BoundedTypeFormal extends JavaTypeFormal {
	final JavaTypeFormal source;
	final IJavaTypeSubstitution subst;
	
	BoundedTypeFormal(JavaTypeFormal src, IJavaTypeSubstitution s) {
		super(src.declaration);		
		/*
		String unparse = src.toString();
		if (unparse.contains("in java.util.List.toArray")) {
			System.out.println("Creating bound for "+unparse);
		}
		*/
		subst = s;
		source = src;
	}
	
	@Override
	public IJavaReferenceType getExtendsBound(ITypeEnvironment tEnv) {
		IJavaReferenceType bound = super.getExtendsBound(tEnv);
		return (IJavaReferenceType) bound.subst(subst);
	}
	
	@Override
	public String toString() {
	    IRNode decl = VisitUtil.getEnclosingDecl(declaration);
		return JavaNames.getTypeName(declaration)+" ..."+subst+"... in "+JavaNames.getFullName(decl);
	}
	
	/**
	 * FIX Is this right?
	 * Set to compare properly in maps for capture
	 */
	@Override
	public int hashCode() {
		return source.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		return source.equals(o);
	}
	
	@Override
	public boolean isEqualTo(ITypeEnvironment env, IJavaType t2) {
		if (t2 instanceof BoundedTypeFormal) {
			BoundedTypeFormal o = (BoundedTypeFormal) t2;
			if (source.isEqualTo(env, t2)) {
				if (subst.equals(o.subst)) {
					return true;
				}
				IJavaType b1 = getExtendsBound(env).subst(subst);
				IJavaType b2 = o.getExtendsBound(env).subst(o.subst);
				return b1.isEqualTo(env, b2);
			}
		}
		else if (t2 instanceof JavaTypeFormal) {
			JavaTypeFormal f = (JavaTypeFormal) t2;
			if (source.isEqualTo(env, t2)) {
				IJavaType b1 = getExtendsBound(env).subst(subst);
				IJavaType b2 = f.getExtendsBound(env);
				return b1.isEqualTo(env, b2);
			}
		}
		return false;
	}
	
	@Override
	public IJavaType subst(final IJavaTypeSubstitution s) {
		// Try to apply the first substitution
		IJavaType oldBound = getExtendsBound(subst.getTypeEnv());
		IJavaType newBound = oldBound.subst(subst);
		if (oldBound.equals(newBound)) {
			// Ignore subst, and try s
			IJavaType rv = source.subst(s);
			if (rv == null) {
				// Raw, so return bound
				return oldBound;
			}
			return rv;
		}
		IJavaType newType = source.subst(s);
		if (!newType.equals(source)) {
			return newType; // TODO is this right?
		}
		newBound = newBound.subst(s);
		return new ReboundedTypeFormal(subst.getTypeEnv(), this, newBound);
	}
}

class JavaIntersectionType extends JavaReferenceType implements IJavaIntersectionType {
  final JavaReferenceType primaryBound;
  final JavaReferenceType secondaryBound;
  
  JavaIntersectionType(JavaReferenceType b1, JavaReferenceType b2) {
    primaryBound = b1;
    secondaryBound = b2;
  }
  
  @Override
  public IJavaReferenceType getSuperclass(ITypeEnvironment env) {
    return getPrimarySupertype();
  }

  @Override
  void writeValue(IROutput out) throws IOException {
    out.writeByte('&');
    primaryBound.writeValue(out);
    secondaryBound.writeValue(out);
  }

  public IJavaReferenceType getPrimarySupertype() {
    return primaryBound;
  }
  
  public IJavaReferenceType getSecondarySupertype() {
    return secondaryBound;
  }

  @Override
  public void printStructure(PrintStream out, int indent) {
    DebugUtil.println(out, indent, "IntersectionType:primaryBound"); 
    primaryBound.printStructure(out, indent+2);
    DebugUtil.println(out, indent, "IntersectionType:secondaryBound"); 
    secondaryBound.printStructure(out, indent+2);
  }
  
  @Override
  public String toString() {
	return primaryBound+" & "+secondaryBound;
  }
  
  @Override
  public String toSourceText() {
	  return primaryBound.toSourceText()+" & "+secondaryBound.toSourceText();
  }
  
  @Override
  public String toFullyQualifiedText() {
	  return primaryBound.toFullyQualifiedText()+" & "+secondaryBound.toFullyQualifiedText();
  }
  
  @Override
  public Iterator<IJavaType> iterator() {
	  final Iterator<IJavaType> it1;
	  if (primaryBound instanceof IJavaIntersectionType) {
		  it1 = ((IJavaIntersectionType)primaryBound).iterator();
	  } else {
		  it1 = new SingletonIterator<IJavaType>(primaryBound);
	  }
	  final Iterator<IJavaType> it2;
	  if (secondaryBound instanceof IJavaIntersectionType) {
		  it2 = ((IJavaIntersectionType)secondaryBound).iterator();
	  } else {
		  it2 = new SingletonIterator<IJavaType>(secondaryBound);
	  }
	  return new AppendIterator<IJavaType>(it1,it2);
  }
  
  @Override
  public boolean isEqualTo(ITypeEnvironment env, IJavaType t2) {
	  if (super.isEqualTo(env, t2)) {
		  return true;
	  }
	  if (t2 instanceof IJavaIntersectionType) {
		  IJavaIntersectionType other = (IJavaIntersectionType) t2;
		  if (primaryBound != null) {
			  if (!primaryBound.isEqualTo(env, other.getPrimarySupertype())) {
				  return false;
			  }
		  } else if (other.getPrimarySupertype() != null) {
			  return false;
		  }
		  if (secondaryBound != null) {
			  if (!secondaryBound.isEqualTo(env, other.getSecondarySupertype())) {
				  return false;
			  } 		  
		  } else if (other.getSecondarySupertype() != null) {
			  return false;
		  }
		  return true;
	  }
	  return false;
  }
  
  @Override
  public final void visit(Visitor v) {
	boolean go = v.accept(this);
	if (go) {
		if (primaryBound != null) {
			primaryBound.visit(v);
		}
		if (secondaryBound != null) {
			secondaryBound.visit(v);
		}
		v.finish(this);
	}
  }	    
  
  @Override
  public IJavaIntersectionType subst(IJavaTypeSubstitution s) {
	JavaReferenceType newB1 = (JavaReferenceType) primaryBound.subst(s);
	JavaReferenceType newB2 = (JavaReferenceType) secondaryBound.subst(s);
	if (newB1 != primaryBound || newB2 != secondaryBound) {
		return JavaTypeFactory.getIntersectionType(newB1, newB2);
	}
	return this;
  }
}

class JavaUnionType extends JavaReferenceType implements IJavaUnionType {
	  final JavaReferenceType primaryBound;
	  final JavaReferenceType secondaryBound;
	  
	  JavaUnionType(JavaReferenceType b1, JavaReferenceType b2) {
	    primaryBound = b1;
	    secondaryBound = b2;
	  }
	  
	  @Override
	  public IJavaReferenceType getSuperclass(ITypeEnvironment env) {
		  // Could be very slow if there are a lot of bounds
		  TypeUtils helper = new TypeUtils(env);
		  IJavaReferenceType lub2 = helper.getLowestUpperBound((IJavaReferenceType) primaryBound, (IJavaReferenceType) secondaryBound);
		  return lub2;
	  }

	  @Override
	  void writeValue(IROutput out) throws IOException {
	    out.writeByte('|');
	    primaryBound.writeValue(out);
	    secondaryBound.writeValue(out);
	  }

	  public IJavaReferenceType getFirstType() {
	    return primaryBound;
	  }
	  
	  public IJavaReferenceType getAlternateType() {
	    return secondaryBound;
	  }

	  @Override
	  public void printStructure(PrintStream out, int indent) {
	    DebugUtil.println(out, indent, "UnionType:primaryBound"); 
	    primaryBound.printStructure(out, indent+2);
	    DebugUtil.println(out, indent, "UnionType:secondaryBound"); 
	    secondaryBound.printStructure(out, indent+2);
	  }
	  
	  @Override
	  public String toString() {
		return primaryBound+" | "+secondaryBound;
	  }
	  
	  @Override
	  public String toSourceText() {
		return primaryBound.toSourceText()+" | "+secondaryBound.toSourceText();
	  }
	  
	  @Override
	  public String toFullyQualifiedText() {
		  return primaryBound.toFullyQualifiedText()+" | "+secondaryBound.toFullyQualifiedText();
	  }
	  
	  @Override
	  public boolean isEqualTo(ITypeEnvironment env, IJavaType t2) {
		  if (super.isEqualTo(env, t2)) {
			  return true;
		  }
		  if (t2 instanceof IJavaUnionType) {
			  IJavaUnionType other = (IJavaUnionType) t2;
			  if (primaryBound != null) {
				  if (!primaryBound.isEqualTo(env, other.getFirstType())) {
					  return false;
				  }
			  } else if (other.getFirstType() != null) {
				  return false;
			  }
			  if (secondaryBound != null) {
				  if (!secondaryBound.isEqualTo(env, other.getAlternateType())) {
					  return false;
				  } 		  
			  } else if (other.getAlternateType() != null) {
				  return false;
			  }
			  return true;
		  }
		  return false;
	  }
	  
	  @Override
	  public final void visit(Visitor v) {
		boolean go = v.accept(this);
		if (go) { 
			if (primaryBound != null) {
				primaryBound.visit(v);
			}
			if (secondaryBound != null) {
				secondaryBound.visit(v);
			}
			v.finish(this);
		}
	  }	  
	  
	  @Override
	  public IJavaUnionType subst(IJavaTypeSubstitution s) {
		JavaReferenceType newB1 = (JavaReferenceType) primaryBound.subst(s);
		JavaReferenceType newB2 = (JavaReferenceType) secondaryBound.subst(s);
		if (newB1 != primaryBound || newB2 != secondaryBound) {
			return JavaTypeFactory.getUnionType(newB1, newB2);
		}
		return this;
	  }	  
	}

class JavaWildcardType extends JavaReferenceType implements IJavaWildcardType {
  private final JavaReferenceType upperBound;
  private final JavaReferenceType lowerBound;
  
  JavaWildcardType(JavaReferenceType ub, JavaReferenceType lb) {
    upperBound = ub;
    lowerBound = lb;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.bind.IJavaWildcardType#getUpperBound()
   */
  public IJavaReferenceType getLowerBound() {
    return lowerBound;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.bind.IJavaWildcardType#getLowerBound()
   */
  public IJavaReferenceType getUpperBound() {
    return upperBound;
  }
  
  @Override
  public IJavaType subst(IJavaTypeSubstitution s) {
	if (s == null) {
		return this;
	}	
    IJavaReferenceType newUpperBound = upperBound == null ? null : (IJavaReferenceType) upperBound.subst(s);
    IJavaReferenceType newLowerBound = lowerBound == null ? null : (IJavaReferenceType) lowerBound.subst(s);
    if (newUpperBound == upperBound && newLowerBound == lowerBound) return this;
    return JavaTypeFactory.getWildcardType(newUpperBound,newLowerBound);
  }
  
  @Override
  public IJavaType getSuperclass(ITypeEnvironment env) {
    throw new NotImplemented("what IJavaType for Object");
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.bind.JavaType#writeValue(edu.cmu.cs.fluid.ir.IROutput)
   */
  @Override
  void writeValue(IROutput out) throws IOException {
    if (upperBound != null) {
      assert(lowerBound == null);
      out.writeByte('-');
      upperBound.writeValue(out);
    } else if (lowerBound != null) {
      out.writeByte('+');
      lowerBound.writeValue(out);
    } else {
      out.writeByte('?');
    }
  }
  
  @Override
  public String toString() {
	  if (upperBound != null) {
	      return "? extends "+upperBound;
	  } else if (lowerBound != null) {
		  return "? super "+lowerBound;
	  } else {
		  return "?";
	  }
  }
  
  @Override
  public String toSourceText() {
	  if (upperBound != null) {
	      return "? extends "+upperBound.toSourceText();
	  } else if (lowerBound != null) {
		  return "? super "+lowerBound.toSourceText();
	  } else {
		  return "?";
	  }
  }
 
  @Override
  public String toFullyQualifiedText() {
	  if (upperBound != null) {
	      return "? extends "+upperBound.toFullyQualifiedText();
	  } else if (lowerBound != null) {
		  return "? super "+lowerBound.toFullyQualifiedText();
	  } else {
		  return "?";
	  }
  }
  
  @Override
  public boolean isValid() {
    return (lowerBound == null || lowerBound.isValid()) &&
           (upperBound == null || upperBound.isValid());
  }

  @Override
  public void printStructure(PrintStream out, int indent) {
    super.printStructure(out, indent);
    if (upperBound != null) {
      upperBound.printStructure(out, indent+2);
    }
    if (lowerBound != null) {
      lowerBound.printStructure(out, indent+2);
    }
  }
  
  @Override
  public boolean isEqualTo(ITypeEnvironment env, IJavaType t2) {
	  if (super.isEqualTo(env, t2)) {
		  return true;
	  }
	  if (t2 instanceof IJavaWildcardType) {
		  IJavaWildcardType other = (IJavaWildcardType) t2;
		  if (lowerBound != null) {
			  if (!lowerBound.isEqualTo(env, other.getLowerBound())) {
				  return false;
			  }
		  } else if (other.getLowerBound() != null) {
			  return false;
		  }
		  if (upperBound != null) {
			  if (!upperBound.isEqualTo(env, other.getUpperBound())) {
				  return false;
			  } 		  
		  } else if (other.getUpperBound() != null) {
			  return false;
		  }
		  return true;
	  }
	  return false;
  }
  
  @Override
  public final void visit(Visitor v) {
	boolean go = v.accept(this);
	if (go) {
		if (upperBound != null) {
			upperBound.visit(v);
		}
		if (lowerBound != null) {
			lowerBound.visit(v);
		}
		v.finish(this);
	}
  }	 
}

class JavaCaptureType extends JavaReferenceType implements IJavaCaptureType {
  final JavaWildcardType wildcard;
  final IJavaReferenceType lowerBound;
  final IJavaReferenceType upperBound;
  
  JavaCaptureType(JavaWildcardType wt, IJavaReferenceType lower, IJavaReferenceType upper) {
    wildcard = wt;
    lowerBound = lower;
    upperBound = upper;
  }
  
  public IJavaWildcardType getWildcard() {
    return wildcard;
  }

  public IJavaReferenceType getLowerBound() {
    return lowerBound;
  }
  
  public IJavaReferenceType getUpperBound() {
    return upperBound;
  }
  
  @Override public IJavaType subst(IJavaTypeSubstitution s) {
	if (s == null) {
	  return this;
	}	
	IJavaType newLower = lowerBound == null ? null : lowerBound.subst(s);
	IJavaType newUpper = upperBound == null ? null : upperBound.subst(s);
	/*
	if (newUpper instanceof IJavaCaptureType) {
		System.out.println("Original before subst: "+this);
	}
	*/
    IJavaType newBase = wildcard.subst(s); // Either wildcard or capture
    if (newBase == wildcard && newLower == lowerBound && newUpper == upperBound) {
    	return this;
    }
    if (newBase instanceof IJavaCaptureType) {
      IJavaCaptureType ct = (IJavaCaptureType) newBase;
      return JavaTypeFactory.getCaptureType(ct.getWildcard(), (IJavaReferenceType) newLower, (IJavaReferenceType) newUpper);
    } else {
      IJavaWildcardType wt = (IJavaWildcardType) newBase;
      return JavaTypeFactory.getCaptureType(wt, (IJavaReferenceType) newLower, (IJavaReferenceType) newUpper);
    }
  }
  
  @Override
  public IJavaType getSuperclass(ITypeEnvironment env) {
    if (upperBound != null) {
    	// TODO is this right?
    	return upperBound;
    }
    return wildcard.getSuperclass(env);
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.bind.JavaType#writeValue(edu.cmu.cs.fluid.ir.IROutput)
   */
  @Override void writeValue(IROutput out) throws IOException {
    throw new NotImplemented("Wait for John to come back");
  }
  
  @Override public String toString() {
    String base = wildcard.toString();

    StringBuilder sb = new StringBuilder(base);
    sb.append("<");    
    sb.append(lowerBound);
    sb.append(',');
    sb.append(upperBound);
    sb.append(">");
    return sb.toString();
  }

  @Override
  public String toSourceText() {
	  return wildcard.toSourceText(); // TODO is this right?
  }
  
  @Override
  public String toFullyQualifiedText() {
	  return wildcard.toFullyQualifiedText(); // TODO
  }

  @Override
  public void printStructure(PrintStream out, int indent) {
    super.printStructure(out, indent);
    if (lowerBound != null) {
    	lowerBound.printStructure(out, indent+2);
    }
    if (upperBound != null) {
    	upperBound.printStructure(out, indent+2);
    }
  }
  
  @Override
  public boolean isEqualTo(ITypeEnvironment env, IJavaType t2) {
	  if (super.isEqualTo(env, t2)) {
		  return true;
	  }
	  if (t2 instanceof IJavaCaptureType) {
		  IJavaCaptureType other = (IJavaCaptureType) t2;
		  if (!wildcard.isEqualTo(env, other.getWildcard())) {
			  return false;
		  }
		  if (lowerBound != null) {
			  if (!lowerBound.isEqualTo(env, other.getLowerBound())) {
				  return false;
			  }
		  } else if (other.getLowerBound() != null) {
			  return false;
		  }
		  if (upperBound != null) {
			  if (!upperBound.isEqualTo(env, other.getUpperBound())) {
				  return false;
			  } 		  
		  } else if (other.getUpperBound() != null) {
			  return false;
		  }
		  return true;
	  }	  
	  //return false;
	  if (t2 == null) {
		  return false;
	  }
	  return t2.isSubtype(env, upperBound); // TODO is this right?
  }

  public IJavaReferenceType getUpperBound(ITypeEnvironment te) {
	  return getUpperBound();
  }
  
  @Override
  public final void visit(Visitor v) {
	boolean go = v.accept(this);
	if (go) {  
		if (upperBound != null) {
			upperBound.visit(v);
		}
		if (lowerBound != null) {
			lowerBound.visit(v);
		}
		v.finish(this);
	}
  }	 
}

class JavaArrayType extends JavaReferenceType implements IJavaArrayType {
  final JavaType elementType;
  JavaArrayType(IJavaType et) {
    elementType = (JavaType)et;
  }

  public IJavaType getElementType() {
    return elementType;
  }

  public int getDimensions() {
    int d = 1;
    for (IJavaType t = elementType; t instanceof IJavaArrayType; ++d) {
      t = ((IJavaArrayType)t).getElementType();
    }
    return d;
  }

  public IJavaType getBaseType() {
    IJavaType t;
    for (t = elementType; t instanceof IJavaArrayType; ) {
      t = ((IJavaArrayType)t).getElementType();
    }
    return t;
  }
  
  @Override
  public IJavaType subst(IJavaTypeSubstitution s) {
    if (s == null) return this;
    IJavaType newElementType = IBinding.Util.subst(elementType, s);
    return JavaTypeFactory.getArrayType(newElementType,1);
  }

  @Override
  void writeValue(IROutput out) throws IOException {
    out.writeByte('[');
    elementType.writeValue(out);
  }
  
  @Override
  public final String toSourceText() {
	  return elementType.toSourceText() + "[]";
  }
  
  @Override
  public String toFullyQualifiedText() {
	  return elementType.toFullyQualifiedText() + "[]";
  }
  
  @Override
  public String toString() {
    return elementType.toString() + "[]";
  }
  
  @Override
  public IJavaType getSuperclass(ITypeEnvironment env) {
	IRNode decl = env.getArrayClassDeclaration();
    //IRNode decl = env.findNamedType(SLUtility.JAVA_LANG_OBJECT);
    return JavaTypeFactory.getMyThisType(decl);
  }
  
  @Override
  public boolean isValid() {
    return elementType.isValid();
  }

  @Override
  public void printStructure(PrintStream out, int indent) {    
    super.printStructure(out, indent);
    elementType.printStructure(out, indent+2);
  }
  
  @Override
  public boolean isEqualTo(ITypeEnvironment env, IJavaType t2) {
	  if (super.isEqualTo(env, t2)) {
		  return true;
	  }
	  if (t2 instanceof IJavaArrayType) {
		  IJavaArrayType other = (IJavaArrayType) t2;
		  return getDimensions() == other.getDimensions() &&
				  getBaseType().isEqualTo(env, other.getBaseType());
	  }
	  return false;
  }
  
  @Override
  public final void visit(Visitor v) {
	boolean go = v.accept(this);
	if (go) {
		elementType.visit(v);
		v.finish(this);
	}
  }
}

class JavaDeclaredType extends JavaReferenceType implements IJavaDeclaredType {
  // private final static Logger LOG = SLLogger.getLogger("FLUID.java.bind");
  final IRNode declaration;
  final List<IJavaType> parameters;
  final boolean isRaw;

  JavaDeclaredType() { this(null); }
  JavaDeclaredType(IRNode n) { this(n, Collections.<IJavaType>emptyList()); }
  JavaDeclaredType(IRNode n, /* @immutable */ List<IJavaType> l) { 
	declaration = n; 
	parameters = l; 
	isRaw = isRawType_private();
  }
  
  public IRNode getDeclaration() { return declaration; }

  public List<IJavaType> getTypeParameters() { return parameters; }

  public IJavaDeclaredType getOuterType() { return null; }
  
  @Override
  public IJavaDeclaredType subst(IJavaTypeSubstitution s) {
    if (s == null) return this;
    List<IJavaType> newParams = s.substTypes(this, parameters);
    if (newParams == parameters) return this;
    return JavaTypeFactory.getDeclaredType(declaration,newParams,null);
  }
  
  public boolean isSameDecl(IRNode other) {
	return AbstractTypeEnvironment.areEquivalent(declaration, other);
  }
  
  @Override
  public IJavaDeclaredType getSuperclass(ITypeEnvironment tEnv) {
	  return tEnv.getSuperclass(this);
//    Operator op = JJNode.tree.getOperator(declaration);
//    if (this == tEnv.getObjectType())
//      return null;
//    if (ClassDeclaration.prototype.includes(op)) {
//      if (this.getName().equals(SLUtility.JAVA_LANG_OBJECT)) {
//    	  return null;
//      }
//      IRNode extension = ClassDeclaration.getExtension(declaration);
//      IJavaType t = tEnv.convertNodeTypeToIJavaType(extension);
//      // TODO: What if we extend a nested class from our superclass?
//      // A: The type factory should correctly insert type actuals
//      // for the nesting (if any).  Actually maybe the canonicalizer should.
//      if (t != null) {
//    	  t = t.subst(JavaTypeSubstitution.create(tEnv, this));
//      }
//      /*if (!(t instanceof IJavaDeclaredType)) {
//        LOG.severe("Classes can only extend other classes");
//        return null;
//      }*/
//      return (IJavaDeclaredType) t;
//    } else if (InterfaceDeclaration.prototype.includes(op)) {
//      return tEnv.getObjectType();
//    } else if (EnumDeclaration.prototype.includes(op)) {
//      IRNode ed              = tEnv.findNamedType("java.lang.Enum");
//      List<IJavaType> params = new ArrayList<IJavaType>(1);
//      params.add(this);
//      return JavaTypeFactory.getDeclaredType(ed, params, null);
//    } else if (AnonClassExpression.prototype.includes(op)) {
//      IRNode nodeType = AnonClassExpression.getType(declaration);
//      IJavaType t = tEnv.convertNodeTypeToIJavaType(nodeType);
//      /*if (!(t instanceof IJavaDeclaredType)) {
//        LOG.severe("Classes can only extend other classes");
//        return null;
//      }*/
//      IJavaDeclaredType dt = ((IJavaDeclaredType) t);
//      if (JJNode.tree.getOperator(dt.getDeclaration()) instanceof InterfaceDeclaration) {
//        return tEnv.getObjectType();
//      }
//      return dt;
//    } else if (EnumConstantClassDeclaration.prototype.includes(op)) {
//      IRNode enumD = VisitUtil.getEnclosingType(declaration);
//      return (IJavaDeclaredType) tEnv.convertNodeTypeToIJavaType(enumD);
//    } else {
//      LOG.severe("Don't know what sort of declation node this is: " + DebugUnparser.toString(declaration));
//      return null;
//    }
  }
  
  @Override
  void writeValue(IROutput out) throws IOException {
    out.writeByte('L');
    writeValueContents(out);
  }
  /**
   * @param out
   * @throws IOException
   */
  protected void writeValueContents(IROutput out) throws IOException {
    out.writeNode(declaration);
    if (parameters.size() > 255) {
      throw new IOException("too many type parameters: " + parameters.size());
    }
    out.writeByte(parameters.size());
    for (Iterator<IJavaType> it = parameters.iterator(); it.hasNext();) {
      JavaType ty = (JavaType)it.next();
      ty.writeValue(out);
    }
  }

  JavaTypeCache2<IRNode,List<IJavaType>,JavaDeclaredType> nestedCache; 

  synchronized IJavaDeclaredType getNestedType(IRNode d, List<IJavaType> params) {
    if (nestedCache == null) {
      nestedCache = new JavaTypeCache2<IRNode,List<IJavaType>,JavaDeclaredType>();// create lazily
    }
    JavaDeclaredType nested = nestedCache.get(d,params);
    if (nested == null) {
      if (declaration == null) {
        // nested in the root type => not really nested
        nested = new JavaDeclaredType(d,params);
      } else {
        nested = new Nested(d,params);
      }
      nestedCache.put(d, params, nested);
    }   
    return nested;
  }

  @Override
  public boolean isValid() {
    if (declaration == null) return true;
    if (AbstractIRNode.isDestroyed(declaration)) return false;
    for (IJavaType jt : parameters) {
      if (!((JavaType)jt).isValid()) return false;
    }
    return true;
  }
  
  @Override
  public synchronized int cleanup() {
    if (nestedCache != null) return nestedCache.cleanup();
    return 0;
  }

  /*
   * We assume that types are cached so we can rely on object identity.
   * If we cannot rely on object identity, then we will have to be very careful,
   * so that nested type comparison is done correctly.
  public boolean equals(Object other) {
    if (!(other instanceof JavaDeclaredType)) return false;
    JavaDeclaredType type = ((JavaDeclaredType) other);
    return (declaration == null ? type.declaration == null : this.declaration
        .equals(type.declaration))
        && parameters.equals(type.parameters);
  }
  
  public int hashCode() {
    return (declaration == null ? 0 : declaration.hashCode()) + parameters.hashCode();
  }
  */

  public int getNesting() {
    return 0;
  }
  
  public boolean isRawType(ITypeEnvironment tEnv) {
	return isRaw;
  }
  
  private boolean isRawType_private() {
	  if (declaration == null) {
		  return false;
	  }
	  if (parameters.isEmpty()) {
		  final IRNode typeParams = TypeUtils.getParametersForType(declaration);
		  if (typeParams != null) {
			  return JJNode.tree.numChildren(typeParams) > 0;
		  }
	  }
	  return false;
  }
  
  @Override
  public void visit(Visitor v) {
	  if (visit_private(v)) {
		  v.finish(this);
	  }
  }
  
  final boolean visit_private(Visitor v) {
	boolean go = v.accept(this);
	if (go) {
		for(IJavaType p : parameters) {
			p.visit(v);
		}
	}
	return go;
  }
  
  /*******************************************************
   * Added to implement IDeclaredType
   *******************************************************/

  @Override
  public ITypeDeclarationNode getNode() {
    return (ITypeDeclarationNode) super.getNode();
  }
  
  /**
   * A nested type is a declared type inside another declared type.
   * Thus it inherits from JavaDeclaredType as well as being nested in 
   * JavaDeclaredType.
   * @author boyland
   */
  class Nested extends JavaDeclaredType implements IJavaNestedType {
    Nested(IRNode d, List<IJavaType> ps) { super(d,ps); }

    public IJavaDeclaredType getOuterType() {
      return JavaDeclaredType.this;
    }
    
    @Override public int getNesting() {
      return 1 + JavaDeclaredType.this.getNesting();
    }

    @Override public boolean isValid() {
      return super.isValid() && JavaDeclaredType.this.isValid();
    }
    
    @Override public IJavaDeclaredType subst(IJavaTypeSubstitution s) {
      if (s == null) {
        //System.out.println("null subst");
        return this;
      }
      List<IJavaType> newParams = s.substTypes(this, parameters);
      JavaDeclaredType newOuter = (JavaDeclaredType) getOuterType().subst(s);
      if (newParams == parameters && newOuter == getOuterType()) return this;
      return JavaTypeFactory.getDeclaredType(declaration,newParams,newOuter);
    }
    
    @Override void writeValue(IROutput out) throws IOException {
      out.writeByte('N');
      super.writeValueContents(out);
      JavaDeclaredType.this.writeValue(out);
    }
    
    @Override public String toString() {
      return JavaDeclaredType.this.toString() + "." + 
      	super.toString(TextKind.UNQUALIFIED, ParamTextKind.TO_STRING);
    }
    
    @Override
    public String toSourceText() {
    	return JavaDeclaredType.this.toSourceText() + "." + 
    		super.toString(TextKind.UNQUALIFIED, ParamTextKind.SOURCE_TEXT);
    }
    
    @Override
    public String toFullyQualifiedText() {
    	return JavaDeclaredType.this.toFullyQualifiedText() + "." + 
    		super.toString(TextKind.UNQUALIFIED, ParamTextKind.QUALIFIED_TEXT);
    }
    
    /**
     * JLS 4.8
     * 
     * More precisely, a raw type is defined to be one of:
     * • The reference type that is formed by taking the name of a generic type declaration
     *   without an accompanying type argument list.
     * • An array type whose element type is a raw type.
     * • A non- static member type of a raw type R that is not inherited from a superclass
     *   or superinterface of R .
     */
    @Override
    public boolean isRawType(ITypeEnvironment tEnv) {
    	if (super.isRawType(tEnv)) {
    		return true;
    	}
    	if (TypeUtil.isStatic(declaration)) {
    		return false;
    	}
    	return getOuterType().isRawType(tEnv);
    }

    @Override
    public final void visit(Visitor v) {
    	boolean go = super.visit_private(v);
    	if (go) {
    		getOuterType().visit(v);
    		v.finish(this);
    	}
    }
  }
  
  @Override public String toString() {
    return toString(TextKind.FULLY_QUALIFIED, ParamTextKind.TO_STRING);
  }
  
  @Override
  public String toSourceText() {
	  return toString(TextKind.RELATIVE, ParamTextKind.SOURCE_TEXT);
  }
  
  @Override
  public String toFullyQualifiedText() {
	  return toString(TextKind.FULLY_QUALIFIED, ParamTextKind.QUALIFIED_TEXT);
  }
  enum TextKind {
	  UNQUALIFIED, RELATIVE, FULLY_QUALIFIED
  }
  
  enum ParamTextKind {
	  TO_STRING, SOURCE_TEXT, QUALIFIED_TEXT
  }
  
  protected final String toString(TextKind kindOfUnparse, ParamTextKind kindOfParams) {
    if (declaration == null) return "?NULL?";
    if (declaration.identity() == IRNode.destroyedNode) {
    	return "?Destroyed?";
    }
    String base;
    try {
    	switch (kindOfUnparse) {
    	case FULLY_QUALIFIED:
    		base = JavaNames.getQualifiedTypeName(declaration);
    		break;
    	case RELATIVE:
    		base = JavaNames.getRelativeTypeNameDotSep(declaration);    		
    		break;
    	case UNQUALIFIED:
    	default:
    		base = JJNode.getInfoOrNull(declaration);
    		break;
    	}
        if (base == null) {
        	base = DebugUnparser.toString(declaration);
        }      
    } catch (SlotUndefinedException ex) {
      base = DebugUnparser.toString(declaration);
    }
    if (parameters.isEmpty()) return base;
    StringBuilder sb = new StringBuilder(base);
    sb.append("<");
    boolean first = true;
    for (Iterator<IJavaType> it = parameters.iterator(); it.hasNext();) {
      if (first) {
        first = false;
      } else {
        sb.append(',');
      }
      switch (kindOfParams) {
      case SOURCE_TEXT:
    	  sb.append(it.next().toSourceText());
    	  break;
      case QUALIFIED_TEXT:
     	  sb.append(it.next().toFullyQualifiedText());
    	  break;
      case TO_STRING:
    	  sb.append(it.next().toString());
    	  break;
      }
    }
    sb.append(">");
    return sb.toString();
  }
  @Override
  public void printStructure(PrintStream out, int indent) {
    super.printStructure(out, indent);
    for(IJavaType param : parameters) {
      param.printStructure(out, indent+2);
    }
  }
}

class JavaAnonType extends JavaDeclaredType implements IJavaDeclaredType {
	  JavaAnonType(IRNode ace) {
		  super(ace);
	  }
	  
	  @Override
	  public boolean isRawType(ITypeEnvironment tEnv) {
		  IRNode type = AnonClassExpression.getType(declaration);
		  IJavaDeclaredType jt = (IJavaDeclaredType) tEnv.convertNodeTypeToIJavaType(type);
		  return jt.isRawType(tEnv);
	  }
}

class JavaFunctionType extends JavaTypeCleanable implements IJavaFunctionType {
	private final IJavaTypeFormal[] typeFormals;
	private final IJavaType[] pieces; // 0 = return type, rest are parameter types
	private final IJavaType[] exceptions; // throw types
	private final boolean isVariable;
	private final TypeFormals typeFormalList = new TypeFormals();
	private final ParameterTypes paramTypes = new ParameterTypes();
	private final ThrowTypes throwTypes = new ThrowTypes();
	private final int hashCode;
	
	public JavaFunctionType(
			IJavaTypeFormal[] tfs, 
			IJavaType rt, 
			IJavaType[] pts,
			boolean isVar,
			IJavaType[] ths) {
		typeFormals = tfs;
		pieces = new IJavaType[pts.length+1];
		pieces[0] = rt;
		for (int i=0; i < pts.length; ++i) {
			pieces[i+1] = pts[i];
		}
		isVariable = isVar;
		exceptions = ths;
		hashCode = computeHashCode();
	}

	private int computeHashCode() {
		// Warning: this method is called from the constructor,
		// before the hash code is assigned
		int h = 0;
		for (IJavaType p : pieces) {
			h += p.hashCode();
			h *= 3;
		}
		h += typeFormalList.hashCode();
		h *= 5;
		h += throwTypes.hashCode();
		if (isVariable) h ^= 1066;
		return h;
	}
	
	@Override
	public boolean equals(Object x) {
		if (x == null || x.getClass() != this.getClass()) {
			return false;
		}
		if (x == this) return true;
		JavaFunctionType ft = (JavaFunctionType)x;
		if (ft.hashCode != hashCode) return false;
		return ft.typeFormalList.equals(typeFormalList) &&
				ft.throwTypes.equals(throwTypes) &&
				Arrays.equals(ft.pieces, pieces) &&
				ft.isVariable == isVariable;
	}
	
	@Override 
	public int hashCode() {
		return hashCode;
	}
	
	@Override
	public List<IJavaTypeFormal> getTypeFormals() {
		return typeFormalList;
	}
	
	@Override
	public List<IJavaType> getParameterTypes() {
		return paramTypes;
	}

	@Override
	public IJavaType getReturnType() {
		return pieces[0];
	}

	@Override
	public Set<IJavaType> getExceptions() {
		return throwTypes;
	}
	
	@Override
	public boolean isVariable() {
		return isVariable;
	}
	
	private class TypeFormals extends AbstractList<IJavaTypeFormal> {
		@Override
		public int size() {
			return typeFormals.length;
		}
		
		@Override
		public IJavaTypeFormal get(int i) {
			return typeFormals[i];
		}
	}
	
	private class ParameterTypes extends AbstractList<IJavaType> {
		@Override
		public IJavaType get(int arg0) {
			if (arg0 < 0) throw new IndexOutOfBoundsException("negative index not allowed: " + arg0);
			if (arg0+1 > pieces.length) {
				 throw new IndexOutOfBoundsException();
			}
			try {
				return pieces[arg0+1];
			} catch(Exception e) {
				System.out.println("pieces len = "+pieces.length);
				System.out.println("Looking at this function: "+JavaFunctionType.this);
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public int size() {
			return pieces.length-1;
		}
	}

	private class ThrowTypes extends AbstractSet<IJavaType> {
		@Override
		public int size() {
			return exceptions.length;
		}
		
		@Override
		public Iterator<IJavaType> iterator() {
			return new Iterator<IJavaType>() {
				private int index = -1;
				@Override
				public boolean hasNext() {
					return index+1 < exceptions.length;
				}
				@Override
				public IJavaType next() {
					if (!hasNext()) throw new NoSuchElementException("no more"); 
					++index;
					return exceptions[index];
				}
				@Override
				public void remove() {
					throw new UnsupportedOperationException("immutable");
				}
			};
		}
	}

	@Override
	public String toString() {
		return "JavaFunctionType(" + typeFormalList + "," + getReturnType() +
				"," + paramTypes + "," + isVariable + "," + throwTypes + ")";
	}
	
	@Override
	public String toSourceText() {
		StringBuilder sb = new StringBuilder();
		if (typeFormals.length > 0) {
			boolean first = true;
			for (IJavaTypeFormal tf : typeFormals) {
				if (first) {
					sb.append("<");
					first = false;
				}
				else sb.append(",");
				sb.append(tf.toSourceText());
			}
			sb.append("> ");
		}
		sb.append(pieces[0].toSourceText());
		sb.append(" ? ");
		if (pieces.length > 1) {
			boolean first = true;
			for (int i=1; i < pieces.length; ++i) {
				if (first) {
					sb.append("(");
					first = false;
				} else sb.append(",");
				if (i+1 == pieces.length && isVariable) {
					if (pieces[i] instanceof IJavaArrayType) {
						sb.append(((IJavaArrayType)pieces[i]).getElementType().toSourceText());
						sb.append("...");
					} else {
						sb.append(pieces[i].toSourceText());
					}
				} else {
					sb.append(pieces[i].toSourceText());
				}
				sb.append(" arg" + i);
			}
			sb.append(")");
		} else sb.append("()");
		if (exceptions.length > 0) {
			boolean first = true;
			for (IJavaType t : exceptions) {
				if (first) {
					sb.append(" throws ");
					first = false;
				} else {
					sb.append(",");
				}
				sb.append(t.toSourceText());
			}
		}
		return sb.toString();
	}

	/**
	 * Perform a substitution on the function type
	 * for type variables <em>outside</em> the scope.
	 * @see instantiate
	 * @param s type substitution on outer variables
	 * @return substituted function type
	 */
	@Override
	public IJavaFunctionType subst(IJavaTypeSubstitution s) {
		for (IJavaTypeFormal f : typeFormals) {
			if (s.get(f) != f) throw new IllegalArgumentException("substitution applies to formal, use instantiate instead");
		}
		return JavaTypeFactory.getFunctionType(
				Arrays.asList(subst(typeFormals,s)), 
				getReturnType().subst(s), 
				Arrays.asList(subst(paramTypes.toArray(JavaTypeFactory.emptyTypes),s)), 
				isVariable, 
				new HashSet<IJavaType>(Arrays.asList(subst(exceptions,s))));
	}

	@Override
	public IJavaFunctionType instantiate(List<IJavaTypeFormal> newFormals, IJavaTypeSubstitution s) {
		for (IJavaTypeFormal f : typeFormals) {
			if (s.get(f) == f && !newFormals.contains(f)) {
				throw new IllegalArgumentException("instantiation didn't handle " + f);
			}
		}
		IJavaType rt = getReturnType();
		IJavaType rt_subst = rt.subst(s);
		if (rt_subst == null && rt instanceof IJavaTypeFormal) {
			// Handle raw types
			IJavaTypeFormal f = (IJavaTypeFormal) rt;
			rt_subst = f.getExtendsBound(s.getTypeEnv());
		}
		return JavaTypeFactory.getFunctionType(
				newFormals,
				rt_subst, 
				Arrays.asList(subst(paramTypes.toArray(JavaTypeFactory.emptyTypes),s)), 
				isVariable, 
				new HashSet<IJavaType>(Arrays.asList(subst(exceptions,s))));		
	}
	
	private <T extends IJavaType> T[] subst(T[] ts, IJavaTypeSubstitution s) {
		T[] result = ts;
		for (int i=0; i < ts.length; ++i) {
			@SuppressWarnings("unchecked")
			T piece = (T)ts[i].subst(s);
			if (piece == null) {
				// Deal with raw type
				if (ts[i] instanceof IJavaTypeFormal) {
					IJavaTypeFormal f = (IJavaTypeFormal) ts[i];
					piece = (T) f.getExtendsBound(s.getTypeEnv());
				}
				if (piece == null) {
					throw new NullPointerException();
				}
			}
			if (piece != ts[i]) {
				if (ts == result) result = ts.clone();
				result[i] = piece;
				
				if (ts.length > 1) {
					for(T t : result) {
						if (t == null) {
							throw new NullPointerException();
						}
					}
				}
			}
		}
		return result;
	}

	@Override
	int cleanup() {
		int total = 0;
		for (IJavaType t : pieces) {
			total += ((JavaType)t).cleanup();
		}
		return total;
	}
}

/*
 * The following classes are used
 * to implement cleanable caches of types.
 */

class JavaTypeCache4<K1,K2,K3,K4,T extends JavaTypeCleanable> extends CustomizableHashCodeMap<K1,JavaTypeCache3<K2,K3,K4,T>>
	implements CleanableMap<K1,JavaTypeCache3<K2,K3,K4,T>>
{
	public JavaTypeCache4() {
		super(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_THRESHOLD);
	}

	public T get(K1 k1, K2 k2, K3 k3, K4 k4) {
		JavaTypeCache3<K2,K3,K4,T> subCache = super.get(k1);
		if (subCache == null) {
			return null;
		}
		return subCache.get(k2,k3,k4);
	}
	
	public void put(K1 k1, K2 k2, K3 k3, K4 k4, T v) {
		JavaTypeCache3<K2,K3,K4,T> subCache = super.get(k1);
		if (subCache == null) {
			subCache = new JavaTypeCache3<K2,K3,K4,T>();
			super.put(k1,subCache);
		}
		subCache.put(k2, k3, k4, v);
	}
	  
	@Override
	protected boolean isValidEntry(HashEntry<K1, JavaTypeCache3<K2, K3, K4, T>> e) {
		JavaTypeCache3<K2, K3, K4, T> subCache = e.getValue();
		subCache.cleanup();
		return !subCache.isEmpty(); // if no entries, then ditch it.
	}

	@Override
	public int cleanup() {
	  return super.cleanup();
	}
}

class JavaTypeCache3<K1,K2,K3,T extends JavaTypeCleanable> extends CustomizableHashCodeMap<K1,JavaTypeCache2<K2,K3,T>>
implements CleanableMap<K1,JavaTypeCache2<K2,K3,T>>
{
	public JavaTypeCache3() {
		super(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_THRESHOLD);
	}

	public T get(K1 k1, K2 k2, K3 k3) {
		JavaTypeCache2<K2,K3,T> subCache = super.get(k1);
		if (subCache == null) {
			return null;
		}
		return subCache.get(k2,k3);
	}

	public void put(K1 k1, K2 k2, K3 k3, T v) {
		JavaTypeCache2<K2,K3,T> subCache = super.get(k1);
		if (subCache == null) {
			subCache = new JavaTypeCache2<K2,K3,T>();
			super.put(k1,subCache);
		}
		subCache.put(k2, k3, v);
	}

	@Override
	protected boolean isValidEntry(HashEntry<K1, JavaTypeCache2<K2, K3, T>> e) {
		JavaTypeCache2<K2, K3, T> subCache = e.getValue();
		subCache.cleanup();
		return !subCache.isEmpty(); // if no entries, then ditch it.
	}

	@Override
	public int cleanup() {
		return super.cleanup();
	}
}

class JavaTypeCache2<K1,K2,T extends JavaTypeCleanable> extends CustomizableHashCodeMap<K1,CleanableMap<K2,T>> 
  implements CleanableMap<K1,CleanableMap<K2,T>> {
  
  public JavaTypeCache2() {
    super(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_THRESHOLD);
  }

  public T get(K1 n, K2 k) {
    CleanableMap<K2,T> subCache = super.get(n);
    if (subCache == null) {
      return null;
    }
    T val = subCache.get(k); 
    return val;
  }
  
  public void put(K1 n, K2 k, T v) {
    CleanableMap<K2,T> subCache = super.get(n);
    if (subCache == null) {
      subCache = new SingletonJavaTypeCache<K2,T>(k,v);
      super.put(n,subCache);
    } else if (subCache instanceof SingletonJavaTypeCache) {
      Map.Entry<K2, T> e = (SingletonJavaTypeCache<K2,T>)subCache;
      if (!e.getKey().equals(k)) {
        subCache = new JavaTypeCache<K2,T>();
        super.put(n, subCache);
        subCache.put(e.getKey(), e.getValue());
      }
      subCache.put(k, v);
    } else {
      subCache.put(k, v);
    }
  }

  @Override
  protected boolean isValidEntry(HashEntry<K1, CleanableMap<K2, T>> e) {
    CleanableMap<K2, T> subCache = e.getValue();
    subCache.cleanup();
    return !subCache.isEmpty(); // if no entries, then ditch it.
  }

  @Override
  public int cleanup() {
    return super.cleanup();
  }
}

// FIX this doesn't cache the hashCode() for Lists!!!
class JavaTypeCache<K,T extends JavaTypeCleanable> extends CustomizableHashCodeMap<K,T>  
    implements CleanableMap<K,T> {
  JavaTypeCache() {
    super(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_THRESHOLD);
  }

  @Override
  protected boolean isValidEntry(HashEntry<K, T> e) {
    JavaTypeCleanable jt = e.getValue();
    if (jt == null) {
      return true;
    }
    jt.cleanup(); // this is where we cascade the cleanups.
    boolean valid = jt.isValid();
    return valid;
  }

  @Override
  public int cleanup() {
    return super.cleanup();
  }
}

class SingletonJavaTypeCache<K,T extends JavaTypeCleanable> extends SingletonMap<K,T> 
    implements CleanableMap<K,T> {

  public SingletonJavaTypeCache(K k, T v) {
    super(k, v);
  }

  public int cleanup() {
    JavaTypeCleanable jt = getValue();
    if (jt == null || jt.isValid()) return 0;
    clear();
    return 1;
  }
  
}

