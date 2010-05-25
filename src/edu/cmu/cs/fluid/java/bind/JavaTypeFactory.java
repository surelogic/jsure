/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/JavaTypeFactory.java,v 1.84 2008/12/12 19:01:02 chance Exp $
 *
 * Created May 26, 2004 
 */
package edu.cmu.cs.fluid.java.bind;

import java.util.*;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.PrintStream;

import com.surelogic.ast.IType;
import com.surelogic.ast.java.operator.IDeclarationNode;
import com.surelogic.ast.java.operator.ITypeDeclarationNode;
import com.surelogic.ast.java.operator.ITypeFormalNode;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.NotImplemented;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.CustomizableHashCodeMap;
import edu.cmu.cs.fluid.util.EmptyIterator;
import edu.cmu.cs.fluid.util.ImmutableList;
import edu.cmu.cs.fluid.util.Iteratable;
import edu.cmu.cs.fluid.util.SingletonMap;
import edu.cmu.cs.fluid.debug.DebugUtil;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;

/**
 * Class that handles instances of classes that implement
 * {@link IJavaType}.  It ensures that two types are 'eq'
 * if they are 'equal'.  It also manages the persistence of
 * the instances.
 * @author boyland
 */
public class JavaTypeFactory implements IRType, Cleanable {
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
  
  public static final IJavaType anyType = new JavaType() {
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
      return EmptyIterator.prototype();
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

  private static IRNodeHashedMap<IJavaTypeFormal> typeFormals = 
    new IRNodeHashedMap<IJavaTypeFormal>();
  
  public static synchronized IJavaTypeFormal getTypeFormal(IRNode tf) {
    IJavaTypeFormal res = typeFormals.get(tf);
    if (res == null) {
      res = new JavaTypeFormal(tf);
      typeFormals.put(tf,res);
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
  
  private static JavaTypeCache2<IJavaWildcardType, List<IJavaReferenceType>, JavaCaptureType> captureTypes = 
       new JavaTypeCache2<IJavaWildcardType, List<IJavaReferenceType>, JavaCaptureType>();
  
  public static synchronized IJavaReferenceType getCaptureType(IJavaWildcardType wt, List<IJavaReferenceType> bounds) {
    if (bounds.isEmpty() || onlyObjectBounds(bounds)) {
      return wt;
    }    
    JavaCaptureType ct = captureTypes.get(wt,bounds);
    if (ct == null) {
      ct = new JavaCaptureType((JavaWildcardType)wt, bounds);
      captureTypes.put(wt,bounds,ct);
    }
    return ct;    
  }
  
  private static boolean onlyObjectBounds(List<IJavaReferenceType> bounds) {
    for(IJavaReferenceType bound : bounds) {
      if (!"java.lang.Object".equals(bound.getName())) {
        return false;
      }
    }
    return true;
  }

  public static final IJavaWildcardType wildcardType = new JavaWildcardType(null,null);
  private static CleanableMap<IJavaType,JavaWildcardType> upperBounded = new JavaTypeCache<IJavaType,JavaWildcardType>();
  private static CleanableMap<IJavaType,JavaWildcardType> lowerBounded = new JavaTypeCache<IJavaType,JavaWildcardType>();

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
      if ("java.lang.Object".equals(upper.getName())) {
        return wildcardType; // HACK?
      }
      res = upperBounded.get(upper);
      if (res == null) {
        res = new JavaWildcardType((JavaReferenceType)upper,null);
        upperBounded.put(upper,res);
      }
    } else {
      throw new FluidError("cannot create wildcard with upper AND lower bounds");
    }
    return res;
  }
  
  // declared types  
  private static final int NUM_ROOTS = 1 << 2;
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
  
  private static class RootType extends JavaDeclaredType {
    @Override public String toString() { return ""; }
  };

  /** Return a declared type for the given declaration
   * and parameters nested in the given context (or null).
   * @param decl IRNode of type declaration (or an anonymous class expression,
   * apparently?)
   * @param params an immutable list of IJavaType for the type parameters.
   * nil is an acceptable parameter value and means no parameters.
   * @param outer the context type.
   */
  public static IJavaDeclaredType getDeclaredType(IRNode decl,
						  /* @immutable */ List<IJavaType> params,
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
    for(IJavaType p : params) {
    	if (p == null) {
    		throw new IllegalArgumentException();
    	}
    }
    Operator op = JJNode.tree.getOperator(decl);
    if (TypeFormal.prototype.includes(op) || !(op instanceof TypeDeclInterface)) {
      throw new IllegalArgumentException();
    }
    IJavaDeclaredType result = ((JavaDeclaredType)outer).getNestedType(decl,params);

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
	  initRootTypes();
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
      IRNode decl = binder.getBinding(nodeType);
      if (decl == null) return null; // program may have binding error
      if (TypeFormal.prototype.includes(decl)) {
        return getTypeFormal(decl);
      }
      // LOG.info("Binding " + DebugUnparser.toString(nodeType) + " to " + JavaNode.getInfo(decl));
      IRNode enclosingType = VisitUtil.getEnclosingType(decl);
      if (enclosingType != null) {
        IJavaDeclaredType dt = (IJavaDeclaredType) convertNodeTypeToIJavaType(enclosingType, binder);
        return getDeclaredType(decl, null, dt);
      }
      return getDeclaredType(decl,null,null);
    } else if (op instanceof TypeRef) {
      IJavaType outer = convertNodeTypeToIJavaType(TypeRef.getBase(nodeType),binder);
      IRNode decl = binder.getBinding(nodeType);
      if (decl == null) {
    	  return null;
      }
      return getDeclaredType(decl,null,(IJavaDeclaredType)outer);
    } else if (op instanceof TypeFormal) {
      return getTypeFormal(nodeType);      
    } else if (op instanceof TypeDeclInterface) {
      return getMyThisType(nodeType);
    } else if (op instanceof ArrayDeclaration) {
      IJavaType bt = convertNodeTypeToIJavaType(ArrayDeclaration.getBase(nodeType),binder);
      return getArrayType(bt, ArrayDeclaration.getDims(nodeType));
    } else if (op instanceof ParameterizedType) {
      IJavaType bt = convertNodeTypeToIJavaType(ParameterizedType.getBase(nodeType),binder);
      List<IJavaType> typeActuals = new ArrayList<IJavaType>();
      IRNode args = ParameterizedType.getArgs(nodeType);
      for (Iterator<IRNode> ch = JJNode.tree.children(args); ch.hasNext();) {
        IRNode arg = ch.next();
        typeActuals.add(convertNodeTypeToIJavaType(arg,binder));
      }
      if (!(bt instanceof JavaDeclaredType)) {
        LOG.severe("parameterizing what? " + bt);
        return bt;
      }
      JavaDeclaredType base = (JavaDeclaredType)bt;
      if (base.getTypeParameters().size() > 0) {
        LOG.severe("Already has parameters! " + bt);
        return bt;
      }
      IJavaDeclaredType outer = null;
      if (base instanceof JavaDeclaredType.Nested) {
        outer = ((JavaDeclaredType.Nested)base).getOuterType();
      }
      return getDeclaredType(base.getDeclaration(),typeActuals,outer);
    } else if (op instanceof WildcardSuperType) {
      IJavaReferenceType st = (IJavaReferenceType) convertNodeTypeToIJavaType(WildcardSuperType.getUpper(nodeType),binder);
      return getWildcardType(null,st);
    } else if (op instanceof WildcardExtendsType) {
      IJavaReferenceType st = (IJavaReferenceType) convertNodeTypeToIJavaType(WildcardExtendsType.getLower(nodeType),binder);
      return getWildcardType(st,null);
    } else if (op instanceof WildcardType) {   
      return getWildcardType(null,null);
    } else if (op instanceof CaptureType) {
      final IRNode bounds              = CaptureType.getBounds(nodeType);
      List<IJavaReferenceType> tbounds = new ArrayList<IJavaReferenceType>();
      for(IRNode b : MoreBounds.getBoundIterator(bounds)) {
        tbounds.add((IJavaReferenceType) binder.getJavaType(b));
      }      
      return getCaptureType(wildcardType, tbounds);
    } else if (op instanceof VarArgsType) {
      IJavaType bt = convertNodeTypeToIJavaType(VarArgsType.getBase(nodeType),binder);
      return getArrayType(bt, 1);
    } else if (op instanceof MoreBounds) {
      IJavaReferenceType result = null;
      for (IRLocation loc = JJNode.tree.lastChildLocation(nodeType); loc != null; loc = JJNode.tree.prevChildLocation(nodeType, loc)) {
        IJavaReferenceType bound = (IJavaReferenceType)convertNodeTypeToIJavaType(JJNode.tree.getChild(nodeType, loc),binder);
        if (result == null) result = bound;
        else result = getIntersectionType(bound,result);
      }
      if (result == null) {
        return binder.getTypeEnvironment().getObjectType();
      } else {
        return result;
      }
    } else {
      LOG.severe("Cannot convert type " + op);
      return null;
    }
  }

  /**
   * Like convertNodeTypeToIJavaType, but primarily handles type declarations.
   * This function does not require an IBinder.  This method has confused
   * purpose and will be removed since we have replaced its functionality.
   * @deprecated use convertNodeTypeToIJavaType
   */
  @Deprecated
  public static IJavaType convertIRTypeDeclToIJavaType(IRNode nodeType) {
    Operator op = JJNode.tree.getOperator(nodeType);
    
    if (op == TypeDeclaration.prototype) { // formerly a hacked nullType
      return getNullType();
    } else if (op instanceof TypeFormal) {
      return getTypeFormal(nodeType);      
    } else if (op instanceof TypeDeclInterface) {
      return getMyThisType(nodeType);
    } else if (op instanceof ArrayDeclaration) {
      IJavaType bt = convertIRTypeDeclToIJavaType(ArrayDeclaration.getBase(nodeType));
      return getArrayType(bt, ArrayDeclaration.getDims(nodeType));
    } else if (op instanceof PrimitiveType) {
      return getPrimitiveType((PrimitiveType)op);
    } else if (op instanceof VoidType) {
      return getVoidType();
    } else if (op == Type.prototype) {
      return JavaTypeFactory.anyType;
    } else {
      LOG.severe("Cannot convert type " + op);
      return null;
    }
  }
  
  /**
   * Return the "this" type for this declaration, what "this"
   * means inside this class.  The correct type actuals and
   * outer type are inferred from the structure.  This method doesn't care
   * about "static"; it's only interested in proper nesting and polymorphism. 
   * @param tdecl type declaration node
   * @return type of "this" within this class/interface.
   */
  public static IJavaDeclaredType getMyThisType(IRNode tdecl) {
    TypeDeclInterface op = (TypeDeclInterface)JJNode.tree.getOperator(tdecl);
    IJavaDeclaredType outer = getThisType(tdecl);
    IRNode typeFormals = null;
    if (op instanceof ClassDeclaration) {
      typeFormals = ClassDeclaration.getTypes(tdecl);
    } else if (op instanceof InterfaceDeclaration) {
      typeFormals = InterfaceDeclaration.getTypes(tdecl);
    }
    List<IJavaType> tactuals = null;
    if (typeFormals != null) {
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
  public static IJavaDeclaredType getThisType(IRNode n) {
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
  public static IRType getIJavaTypeType() { return prototype; }

  public boolean isValid(Object x) {
    return x instanceof IJavaType;
  }

  public Comparator getComparator() { return null; }

  public void writeValue(Object value, IROutput out) throws IOException {
    JavaType t = (JavaType)value;
    t.writeValue(out);
  }

  public Object readValue(IRInput in) throws IOException {
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
    case '-': return getWildcardType((IJavaReferenceType)readValue(in),null);
    case '+': return getWildcardType(null,(IJavaReferenceType)readValue(in));
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

  public IRType readType(IRInput in) throws IOException {
    return this;
  }

  public Object fromString(String s) {
    return null; // TODO
  }

  public String toString(Object o) {
    return o.toString();
  }

  private static final int TYPE_BYTE = 'J';

  static {
    IRPersistent.registerIRType(prototype,TYPE_BYTE);
  }
  
  public static int doCleanup() {
    return
       //convertedTypeCache.cleanup() +
       typeFormals.cleanup() +
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

abstract class JavaType implements IJavaType {
  
  public IJavaType subst(IJavaTypeSubstitution s) {
    return this; // assume no change;
  }
  
  abstract void writeValue(IROutput out) throws IOException;

  public final String getName() { return toString(); }
  
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
  
  public void printStructure(PrintStream out, int indent) {
    DebugUtil.println(out, indent, this.getClass().getSimpleName()+": "+this.toString());
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
    return EmptyIterator.prototype();
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
    return EmptyIterator.prototype();
  }
}

abstract class JavaReferenceType extends JavaType implements IJavaReferenceType {
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
  public IJavaType subst(IJavaTypeSubstitution s) {
    if (s == null || s.isNull()) return this;
    return s.get(this);
  }
  
  @Override
  void writeValue(IROutput out) throws IOException {
    out.writeByte('T');
    out.writeNode(declaration);
  }
  
  @Override
  public String toString() {
    IRNode tdecl = VisitUtil.getEnclosingType(declaration);
    return JavaNames.getTypeName(declaration)+" in "+JavaNames.getTypeName(tdecl);
  }
  
  @Override
  public IJavaType getSuperclass(ITypeEnvironment env) {
    return getExtendsBound(env);
  }
  
  @Override
  public boolean isValid() {
    return !AbstractIRNode.isDestroyed(declaration);
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
    IRNode bounds = TypeFormal.getBounds(declaration);
    if (JJNode.tree.numChildren(bounds) <= 0) {
      return tEnv.getObjectType();
    }
    IRNode first = MoreBounds.getBound(bounds, 0);
    return (IJavaReferenceType)tEnv.convertNodeTypeToIJavaType(first);
  }

  @Override
  public void printStructure(PrintStream out, int indent) {
    DebugUtil.println(out, indent, "TypeFormal: "); 
    JavaNode.dumpTree(out, declaration, indent+2);
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
  public IJavaReferenceType getUpperBound() {
    return upperBound;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.bind.IJavaWildcardType#getLowerBound()
   */
  public IJavaReferenceType getLowerBound() {
    return lowerBound;
  }
  
  @Override
  public IJavaType subst(IJavaTypeSubstitution s) {
	if (s == null || s.isNull()) {
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
}

class JavaCaptureType extends JavaReferenceType implements IJavaCaptureType {
  final JavaWildcardType wildcard;
  final List<IJavaReferenceType> bounds;
  
  JavaCaptureType(JavaWildcardType wt, List<IJavaReferenceType> b) {
    wildcard = wt;
    bounds = b;
  }
  
  public IJavaWildcardType getWildcard() {
    return wildcard;
  }

  public List<IJavaReferenceType> getTypeBounds() {
    return Collections.<IJavaReferenceType>unmodifiableList(bounds);
  }
  
  @Override public IJavaType subst(IJavaTypeSubstitution s) {
	if (s == null || s.isNull()) {
	  return this;
	}	
    boolean changedBounds = false;
    List<IJavaReferenceType> newBounds = new ArrayList<IJavaReferenceType>();
    for(IJavaReferenceType bound : bounds) {
      IJavaType t = bound.subst(s);
      newBounds.add((IJavaReferenceType) t);
      
      if (t != bound) {
        changedBounds = true;
      }
    }
    
    IJavaType newBase = wildcard.subst(s); // Either wildcard or capture
    if (newBase instanceof IJavaCaptureType) {
      IJavaCaptureType ct = (IJavaCaptureType) newBase;
      newBounds.addAll(ct.getTypeBounds());
      return JavaTypeFactory.getCaptureType(ct.getWildcard(), newBounds);
    } else {
      IJavaWildcardType wt = (IJavaWildcardType) newBase;
      if (wildcard == wt && !changedBounds) {
        return this;
      }
      return JavaTypeFactory.getCaptureType(wt, newBounds);
    }
  }
  
  @Override
  public IJavaType getSuperclass(ITypeEnvironment env) {
    throw new NotImplemented("what IJavaType for Object");
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.bind.JavaType#writeValue(edu.cmu.cs.fluid.ir.IROutput)
   */
  @Override void writeValue(IROutput out) throws IOException {
    throw new NotImplemented("Wait for John to come back");
  }
  
  @Override public String toString() {
    String base = wildcard.toString();

    if (bounds.isEmpty()) return base;
    StringBuilder sb = new StringBuilder(base);
    sb.append("<");
    boolean first = true;
    for (Iterator<IJavaReferenceType> it = bounds.iterator(); it.hasNext();) {
      if (!first) sb.append(',');
      sb.append(it.next().toString());
    }
    sb.append(">");
    return sb.toString();
  }

  @Override
  public boolean isValid() {
    if (!wildcard.isValid()) return false;
    for (IJavaReferenceType t : bounds) {
      if (!((JavaReferenceType)t).isValid()) return false;
    }
    return true;
  }

  @Override
  public void printStructure(PrintStream out, int indent) {
    super.printStructure(out, indent);
    for(IJavaReferenceType b : bounds) {
      b.printStructure(out, indent+2);
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
    if (s == null || s.isNull()) return this;
    return JavaTypeFactory.getArrayType(elementType.subst(s),1);
  }

  @Override
  void writeValue(IROutput out) throws IOException {
    out.writeByte('[');
    elementType.writeValue(out);
  }
  
  @Override
  public String toString() {
    return elementType.toString() + "[]";
  }
  
  @Override
  public IJavaType getSuperclass(ITypeEnvironment env) {
    IRNode decl = env.findNamedType("java.lang.Object");
    return JavaTypeFactory.convertIRTypeDeclToIJavaType(decl);
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
}

class JavaDeclaredType extends JavaReferenceType implements IJavaDeclaredType {
  private final static Logger LOG = SLLogger.getLogger("FLUID.java.bind");
  final IRNode declaration;
  final List<IJavaType> parameters;

  JavaDeclaredType() { this(null); }
  JavaDeclaredType(IRNode n) { this(n, Collections.<IJavaType>emptyList()); }
  JavaDeclaredType(IRNode n, /* @immutable */ List<IJavaType> l) { declaration = n; parameters = l; }
  
  public IRNode getDeclaration() { return declaration; }

  public List<IJavaType> getTypeParameters() { return parameters; }

  public IJavaDeclaredType getOuterType() { return null; }
  
  @Override
  public IJavaDeclaredType subst(IJavaTypeSubstitution s) {
    if (s == null || s.isNull()) return this;
    List<IJavaType> newParams = s.substTypes(parameters);
    if (newParams == parameters) return this;
    return JavaTypeFactory.getDeclaredType(declaration,newParams,null);
  }
  
  @Override
  public IJavaDeclaredType getSuperclass(ITypeEnvironment tEnv) {
    Operator op = JJNode.tree.getOperator(declaration);
    if (this == tEnv.getObjectType())
      return null;
    if (ClassDeclaration.prototype.includes(op)) {
      IRNode extension = ClassDeclaration.getExtension(declaration);
      IJavaType t = tEnv.convertNodeTypeToIJavaType(extension);
      // TODO: What if we extend a nested class from our superclass?
      // A: The type factory should correctly insert type actuals
      // for the nesting (if any).  Actually maybe the canonicalizer should.
      t = t.subst(JavaTypeSubstitution.create(tEnv, this));
      /*if (!(t instanceof IJavaDeclaredType)) {
        LOG.severe("Classes can only extend other classes");
        return null;
      }*/
      return (IJavaDeclaredType) t;
    } else if (InterfaceDeclaration.prototype.includes(op)) {
      return tEnv.getObjectType();
    } else if (EnumDeclaration.prototype.includes(op)) {
      IRNode ed              = tEnv.findNamedType("java.lang.Enum");
      List<IJavaType> params = new ArrayList<IJavaType>(1);
      params.add(this);
      return JavaTypeFactory.getDeclaredType(ed, params, null);
    } else if (AnonClassExpression.prototype.includes(op)) {
      IRNode nodeType = AnonClassExpression.getType(declaration);
      IJavaType t = tEnv.convertNodeTypeToIJavaType(nodeType);
      /*if (!(t instanceof IJavaDeclaredType)) {
        LOG.severe("Classes can only extend other classes");
        return null;
      }*/
      IJavaDeclaredType dt = ((IJavaDeclaredType) t);
      if (JJNode.tree.getOperator(dt.getDeclaration()) instanceof InterfaceDeclaration) {
        return tEnv.getObjectType();
      }
      return dt;
    } else if (EnumConstantClassDeclaration.prototype.includes(op)) {
      IRNode enumD = VisitUtil.getEnclosingType(declaration);
      return (IJavaDeclaredType) tEnv.convertNodeTypeToIJavaType(enumD);
    } else {
      LOG.severe("Don't know what sort of declation node this is: " + DebugUnparser.toString(declaration));
      return null;
    }
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
      if (s == null || s.isNull()) {
        //System.out.println("null subst");
        return this;
      }
      List<IJavaType> newParams = s.substTypes(parameters);
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
      return JavaDeclaredType.this.toString() + "." + super.toString(false);
    }
  }
  
  @Override public String toString() {
    return toString(true);
  }
  
  protected final String toString(boolean fullyQualify) {
    if (declaration == null) return "?NULL?";
    String base;
    try {
      if (fullyQualify) {
        base = JavaNames.getQualifiedTypeName(declaration);
      } else {
        base = JJNode.getInfoOrNull(declaration);
        if (base == null) {
        	base = DebugUnparser.toString(declaration);
        }
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
      sb.append(it.next().toString());
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

/*
 * The following three classes are used
 * to implement cleanable caches of types.
 */

class JavaTypeCache2<K1,K2,T extends JavaType> extends CustomizableHashCodeMap<K1,CleanableMap<K2,T>> 
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
class JavaTypeCache<K,T extends JavaType> extends CustomizableHashCodeMap<K,T>  
    implements CleanableMap<K,T> {
  JavaTypeCache() {
    super(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_THRESHOLD);
  }

  @Override
  protected boolean isValidEntry(HashEntry<K, T> e) {
    JavaType jt = e.getValue();
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

class SingletonJavaTypeCache<K,T extends JavaType> extends SingletonMap<K,T> 
    implements CleanableMap<K,T> {

  public SingletonJavaTypeCache(K k, T v) {
    super(k, v);
  }

  public int cleanup() {
    JavaType jt = getValue();
    if (jt == null || jt.isValid()) return 0;
    clear();
    return 1;
  }
  
}

