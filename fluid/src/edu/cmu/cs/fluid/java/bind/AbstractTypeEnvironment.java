package edu.cmu.cs.fluid.java.bind;

import static com.surelogic.common.util.IteratorUtil.noElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.Borrowed;
import com.surelogic.Immutable;
import com.surelogic.RegionEffects;
import com.surelogic.ThreadSafe;
import com.surelogic.ast.IPrimitiveType;
import com.surelogic.common.Pair;
import com.surelogic.common.SLUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.util.EmptyIterator;
import com.surelogic.common.util.FilterIterator;
import com.surelogic.common.util.Iteratable;
import com.surelogic.common.util.PairIterator;
import com.surelogic.common.util.SimpleIterator;
import com.surelogic.common.util.SingletonIterator;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.operator.AnnotationDeclaration;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.ArrayDeclaration;
import edu.cmu.cs.fluid.java.operator.BooleanType;
import edu.cmu.cs.fluid.java.operator.ByteType;
import edu.cmu.cs.fluid.java.operator.CharType;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.EnumConstantClassDeclaration;
import edu.cmu.cs.fluid.java.operator.EnumDeclaration;
import edu.cmu.cs.fluid.java.operator.FloatType;
import edu.cmu.cs.fluid.java.operator.IntType;
import edu.cmu.cs.fluid.java.operator.IntegralType;
import edu.cmu.cs.fluid.java.operator.InterfaceDeclaration;
import edu.cmu.cs.fluid.java.operator.LambdaExpression;
import edu.cmu.cs.fluid.java.operator.LongType;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.MoreBounds;
import edu.cmu.cs.fluid.java.operator.NumericType;
import edu.cmu.cs.fluid.java.operator.PrimitiveType;
import edu.cmu.cs.fluid.java.operator.ReferenceType;
import edu.cmu.cs.fluid.java.operator.ReturnTypeInterface;
import edu.cmu.cs.fluid.java.operator.ShortType;
import edu.cmu.cs.fluid.java.operator.TypeDeclInterface;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeFormal;
import edu.cmu.cs.fluid.java.operator.TypeInterface;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.TripleIterator;

/**
 * A class that implements some of the basic type operations.
 * (in terms of getClassTable())
 */
@ThreadSafe
public abstract class AbstractTypeEnvironment implements ITypeEnvironment {
  protected static final Logger LOG = SLLogger.getLogger("FLUID.java");
  private static final boolean debug = LOG.isLoggable(Level.FINE);
  
  private final AtomicReference<IJavaDeclaredType> objectType =
      new AtomicReference<IJavaDeclaredType>(null);
  private final Map<IRNode,IJavaType> convertedTypeCache = 
	  new ConcurrentHashMap<IRNode,IJavaType>();
  private IBindHelper helper;
  
  protected final Map<String, IJavaType> javaTypeMap = new HashMap<String, IJavaType>();
  {
	  initTypeMap();
  }

  void initTypeMap() {
	  javaTypeMap.put("int", JavaTypeFactory.intType);
	  javaTypeMap.put("long", JavaTypeFactory.longType);
	  javaTypeMap.put("byte", JavaTypeFactory.byteType);
	  javaTypeMap.put("char", JavaTypeFactory.charType);
	  javaTypeMap.put("short", JavaTypeFactory.shortType);
	  javaTypeMap.put("float", JavaTypeFactory.floatType);
	  javaTypeMap.put("double", JavaTypeFactory.doubleType);
	  javaTypeMap.put("boolean", JavaTypeFactory.booleanType);
	  javaTypeMap.put("void", JavaTypeFactory.voidType);
  }
  
  /*
  static int cached, total;
  */
  private final Map<Pair<IJavaType, IJavaType>, Boolean> subTypeCache = 
	  new ConcurrentHashMap<Pair<IJavaType,IJavaType>, Boolean>();
  
  @Override
  public void clearCaches(boolean clearAll) {
	  objectType.set(null);
	  stringType = null;
	  convertedTypeCache.clear();
	  subTypeCache.clear();
  }
  
  public static void printStats() {
	  //System.out.println("CachedT = "+cached);
	  //System.out.println("Total T = "+total);
  }
  
  @Override
  public int getMajorJavaVersion() {
	  return 8; // sort of
  }
  
  @Override
  public void addTypesInCU(IRNode root) {
	  throw new UnsupportedOperationException();
  }
  
  @Override
  public IJavaType convertNodeTypeToIJavaType(IRNode nodeType) {
	  if (nodeType == null) {
		  return null;
	  }
	  IJavaType result = convertedTypeCache.get(nodeType);
	  if (result == null) {
		  result = JavaTypeFactory.convertNodeTypeToIJavaType(nodeType, getBinder());
		  if (result != null) {
			  convertedTypeCache.put(nodeType, result);		 		  
		  } else if (!AbstractJavaBinder.isBinary(nodeType)) {
			  LOG.severe("Null type for "+DebugUnparser.toString(nodeType)+
			             " in "+DebugUnparser.toString(VisitUtil.getClosestClassBodyDecl(nodeType)));
			  JavaTypeFactory.convertNodeTypeToIJavaType(nodeType, getBinder());
		  }
	  } else {
		  //cached++;
	  }
	  //total++;
	  return result;
  }
  
  @Override
  public IJavaSourceRefType getMyThisType(IRNode typeDecl) {
	  if (typeDecl == null) {
		  return null;
	  }
	  IJavaSourceRefType result = (IJavaSourceRefType) convertedTypeCache.get(typeDecl);
	  if (result == null) {
		  result = JavaTypeFactory.getMyThisType(typeDecl);
		  if (result != null) {
			  convertedTypeCache.put(typeDecl, result);		 
		  } else if (!AbstractJavaBinder.isBinary(typeDecl)) {
			  LOG.severe("Null type for "+DebugUnparser.toString(typeDecl));
			  JavaTypeFactory.getMyThisType(typeDecl);
		  }
	  } else {
		  //cached++;
	  }
	  //total++;
	  return result;
  }
  
  @Override
  public IJavaDeclaredType getObjectType() {
	IJavaDeclaredType oType = objectType.get();
    while (oType == null) {
      // this method has to be synchronized because otherwise, another thread
      // may see an uninitialized IJavaDeclaredType:
      IRNode jlo = findNamedType(SLUtility.JAVA_LANG_OBJECT);
      assert(jlo != null);
      /*
      if (jlo == null) {
    	  findNamedType(SLUtility.JAVA_LANG_OBJECT);
      }
      */
      oType = JavaTypeFactory.getDeclaredType(jlo, null, null);
      
      final boolean set = objectType.compareAndSet(null, oType);
      if (!set) {
    	  // Get new value
    	  oType = objectType.get();
      }
    }
    if (debug) {
    	IRNode jlo = findNamedType(SLUtility.JAVA_LANG_OBJECT);
    	IJavaDeclaredType jloType = JavaTypeFactory.getDeclaredType(jlo, null, null);
    	if (objectType != jloType) {
    		JavaTypeFactory.getDeclaredType(jlo, null, null);
    		throw new Error("j.l.O not equal");
    	}
    }
    return oType;
  }

  private IRNode arrayClassDeclaration = null;
  @Override
  public synchronized IRNode getArrayClassDeclaration() {
    // no need to be synchronized because nodes havce identity
    // and nothing else.
    if (arrayClassDeclaration == null) {
      arrayClassDeclaration = findNamedType(PromiseConstants.ARRAY_CLASS_QNAME);
      if (arrayClassDeclaration == null) {
        LOG.severe("default implementation for getArrayClassDeclaration doesn't work");
      }
    }
    return arrayClassDeclaration;
  }
  
  private IJavaDeclaredType stringType = null;
  @Override
  public  synchronized IJavaDeclaredType getStringType() {
    if (stringType == null) {
      // this method has to be synchronized because otherwise, another thread
      // may see an uninitialized IJavaDeclaredType:
      stringType = JavaTypeFactory.getDeclaredType(findNamedType("java.lang.String"), null, null);
    }
    return stringType;
  }

  @Override
  public final IBindHelper getBindHelper() {
	if (helper == null) {
      helper = new BindHelper(getBinder());
	}
	return helper;
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.bind.ITypeEnvironment#isAssignmentCompatible(edu.cmu.cs.fluid.java.bind.IJavaType, edu.cmu.cs.fluid.java.bind.IJavaType, edu.cmu.cs.fluid.ir.IRNode)
   */
  @Override
  public boolean isAssignmentCompatible(IJavaType t1, IJavaType t2, IRNode n2) {
    if (t1.isEqualTo(this, t2)) return true;
    if (t2 == JavaTypeFactory.anyType) {
      return true;
    }
    if (t1 instanceof IJavaPrimitiveType) {
      if (!(t2 instanceof IJavaPrimitiveType)) return false;
      return arePrimTypesCompatible((IJavaPrimitiveType) t1, (IJavaPrimitiveType) t2, n2);
    } else {
      return isSubType(t2,t1);
    }
  }
  
  @Override
  public boolean isAssignmentCompatible(IJavaType[] ts1, IJavaType[] ts2) {
    if (ts1.length != ts2.length) return false;
    for (int i=0; i < ts1.length; ++i) {
      if (!isAssignmentCompatible(ts1[i],ts2[i],null)) return false;
    }
    return true;
  }
  
  /** JLS 3
   * Method invocation conversion is applied to each argument value in
   * a method or constructor invocation (�8.8.7.1, �15.9, �15.12): 
   * the type of the argument expression must be converted to the type 
   * of the corresponding parameter. Method invocation contexts allow 
   * the use of one of the following:
   * 
   * an identity conversion (�5.1.1)
   * a widening primitive conversion (�5.1.2)
   * a widening reference conversion (�5.1.5)
   * a boxing conversion (�5.1.7) optionally followed by widening reference conversion
   * an unboxing conversion (�5.1.8) optionally followed by a widening primitive conversion. 
   * 
   * If, after the conversions listed above have been applied, the resulting type is a raw type
   * (�4.8), an unchecked conversion (�5.1.9) may then be applied. It is a compile time error 
   * if the chain of conversions contains two parameterized types that are not not in the subtype
   * relation.
   * 
   * If the type of an argument expression is either float or double, then value set conversion 
   * (�5.1.13) is applied after the type conversion:
   * 
   * If an argument value of type float is an element of the float-extended-exponent value set, 
   * then the implementation must map the value to the nearest element of the float value set. 
   * This conversion may result in overflow or underflow.
   * 
   * If an argument value of type double is an element of the double-extended-exponent value set,
   * then the implementation must map the value to the nearest element of the double value set. 
   * This conversion may result in overflow or underflow. 
   * 
   * If, after the type conversions above have been applied, the resulting value is an object 
   * which is not an instance of a subclass or subinterface of the erasure of the corresponding 
   * formal parameter type, then a ClassCastException is thrown.
   * 
   * @see edu.cmu.cs.fluid.java.bind.ITypeEnvironment#isCallCompatible(edu.cmu.cs.fluid.java.bind.IJavaType, edu.cmu.cs.fluid.java.bind.IJavaType)
   */  
  @Override
  public boolean isCallCompatible(IJavaType param, IJavaType arg) {
    if (param.isEqualTo(this, arg)) return true; 
    if (arg == JavaTypeFactory.anyType) {
      return true;
    }
    if (param instanceof IJavaPrimitiveType) {
      if (!(arg instanceof IJavaPrimitiveType)) return false;
      return arePrimTypesCompatible((IJavaPrimitiveType) param, (IJavaPrimitiveType) arg, null);
    } else if (isSubType(arg, param)) {
      return true;
    } else if (isCallCompatibleIfRaw(param, arg)) {
      // I can apply an unchecked conversion
      return true;
    } else {
      /*
      // Check whether it would throw an exception
      IJavaType erasure = computeErasure(param);
      boolean oldRv = isSubType(arg, erasure);
      // TODO unchecked conversion?
      if (oldRv) {
    	  if (true) { // TODO
    		  return true; // Old result for "compatibility"
    	  } else {
    		  System.err.println("Previously trying to convert "+arg+" to "+param);   
    		  isSubType(arg, param);
    		  isCallCompatibleIfRaw(param, arg);
    	  }
      }
      */
      return false;
    }
  }
  
  private boolean isCallCompatibleIfRaw(IJavaType param, IJavaType arg) {
	  if (param instanceof IJavaArrayType && arg instanceof IJavaArrayType) {
		  IJavaArrayType paramA = (IJavaArrayType) param;
		  IJavaArrayType argA = (IJavaArrayType) arg;
		  return paramA.getDimensions() == argA.getDimensions() &&
				 isCallCompatibleIfRaw(paramA.getBaseType(), argA.getBaseType());
	  }
	  /*
      if (arg instanceof IJavaDeclaredType) {
    	  IJavaDeclaredType argD = (IJavaDeclaredType) arg;
    	  if (argD.isRawType(this) && isSubType(arg, param, true)) {
    		  return true;
    	  }
      }
      */
      if (param instanceof IJavaDeclaredType) {
    	  IJavaDeclaredType paramD = (IJavaDeclaredType) param;
       	  if (paramD.isRawType(this) && isSubType(arg, param, true)) {
    		  return true;
    	  }
       	  if (arg instanceof IJavaDeclaredType && paramD.getTypeParameters().size() > 0) {
       		  IJavaDeclaredType argD = (IJavaDeclaredType) arg;
       		  IJavaDeclaredType matchingSuper = matchSuper(paramD.getDeclaration(), argD);
       		  if (matchingSuper != null) {
       			  if (matchingSuper.isRawType(this)) {
       				  return true;
       			  }       			  
       			  else if (matchingSuper.getTypeParameters().size() == paramD.getTypeParameters().size()) {
       				// Look at type args
      				List<IJavaType> sl = matchingSuper.getTypeParameters();
    				List<IJavaType> tl = paramD.getTypeParameters();
    				if (tl.isEmpty()) return true; // raw types (from JLS 4.10.2)
    				if (sl.isEmpty()) return false; // raw is NOT a subtype
    				Iterator<IJavaType> sli = sl.iterator();
    				Iterator<IJavaType> tli = tl.iterator();
    				// if we find any non-matches, we fail
    				while (sli.hasNext() && tli.hasNext()) {
    					IJavaType ss = sli.next();
    					IJavaType tt = tli.next();
    					if (!typeArgumentContained(ss,tt,true)) return false;
    				}
    				return true;
       			  }
       		  }
       	  }
       	  if (arg instanceof IJavaTypeFormal) {
       		IJavaTypeFormal jtf = (IJavaTypeFormal) arg;
       		if (isCallCompatibleIfRaw(paramD, jtf.getExtendsBound(this))) {
       			return true;
       		}
       	  }
      }
      return false;
  }
  
  /**
   * Find the supertype that matches the given declaration
   */
  private IJavaDeclaredType matchSuper(IRNode decl, IJavaDeclaredType t) {
	if (t.getDeclaration().equals(decl)) {
		return t;
	}
	for(IJavaType st : getSuperTypes(t)) {
		if (st instanceof IJavaDeclaredType) {
			IJavaDeclaredType result = matchSuper(decl, (IJavaDeclaredType) st);
			if (result != null) {
				return result;
			}
		} 
	}
	return null;
  }

  private boolean arePrimTypesCompatible(IJavaPrimitiveType t1, IJavaPrimitiveType t2, IRNode n2) {
    PrimitiveType op1 = t1.getOp();
    PrimitiveType op2 = t2.getOp();
    assert (op1 != op2); // assuming JavaTypeFactory caches correctly.
    if (op1 instanceof BooleanType || op2 instanceof BooleanType) {
      return false;
    }    
    if (n2 != null) {
      final ConstantExpressionVisitor v = new ConstantExpressionVisitor(getBinder());
      Object value = v.doAccept(n2);
      if (value instanceof Number && !(value instanceof Double || value instanceof Float)) {
    	  final long i = ((Number) value).longValue();
    	  if (i >= -128 && i < 128 && op1 instanceof ByteType) return true;
          if (i >= -32768 && i < 32768 && op1 instanceof ShortType) return true;
          if (i >= 0 && i < 65536 && op1 instanceof CharType) return true;
          // otherwise, fall through to normal case.
      }
      /*
      Operator op = JJNode.tree.getOperator(n2);
      if (op instanceof ArithUnopExpression) {
    	  return arePrimTypesCompatible(t1, t2, UnopExpression.getOp(n2));    	  
      }
      else if (op instanceof IntLiteral) {    
        String token = IntLiteral.getToken(n2);
        final long i = parseIntLiteral(token);
        if (i >= -128 && i < 128 && op1 instanceof ByteType) return true;
        if (i >= -32768 && i < 32768 && op1 instanceof ShortType) return true;
        if (i >= 0 && i < 65536 && op1 instanceof CharType) return true;
        // otherwise, fall through to normal case.
      }
      */
    }
    // According to JLS: No primitive widening to a char type
    if (op1 instanceof CharType) return false;
    
    if (op2 instanceof ByteType) return true;
    if (op1 instanceof ByteType) return false;
    if (op2 instanceof ShortType) return true;
    if (op1 instanceof ShortType) return false; // no widening char -> short
    if (op2 instanceof CharType) return true; 
    if (op2 instanceof IntType) return true;
    if (op1 instanceof IntType) return false;
    if (op2 instanceof LongType) return true;
    if (op1 instanceof LongType) return false;
    if (op2 instanceof FloatType) return true;
    if (op1 instanceof FloatType) return false;
    
    assert (false); // should not be possible
    return false;
  }

  private long parseIntLiteral(String token) {
	final long i;
	if (token.endsWith("L") || token.endsWith("l")) {
        token = token.substring(0, token.length()-1);
        if (token.startsWith("0")) { // hex or octal?
          if (token.length() == 1) {
            i = 0;
          }
          else if (token.startsWith("0x")) { // hex
          i = Long.parseLong(token.substring(2), 16);
          }
          else {
            i = Long.parseLong(token.substring(1), 8);
          }
        } else {
          i = Long.parseLong(token);
        } 
      } else {
        if (token.startsWith("0")) { // hex or octal?
          if (token.length() == 1) {
            i = 0;
          }
          else if (token.startsWith("0x")) { // hex
          i = Integer.parseInt(token.substring(2), 16);
          }
          else {
            i = Integer.parseInt(token.substring(1), 8);
          }
        } else {
          i = Integer.parseInt(token);
        } 
      }
	return i;
}

  @Override
  public boolean isCallCompatible(IJavaType[] ts1, IJavaType[] ts2) {
    if (ts1.length != ts2.length) return false;
    for (int i=0; i < ts1.length; ++i) {
      if (!isCallCompatible(ts1[i],ts2[i])) return false;
    }
    return true;
  }
  
  
  /// Java 8: FunctionType methods
  
  @Override
  public IJavaFunctionType computeErasure(IJavaFunctionType ft) {
	  // not pretty, but at least simple:
	  IJavaType[] ptypes = ft.getParameterTypes().toArray(JavaTypeFactory.emptyTypes);
	  IJavaType[] etypes = ft.getExceptions().toArray(JavaTypeFactory.emptyTypes);
	  for (int i=0; i < ptypes.length; ++i) {
		  ptypes[i] = computeErasure(ptypes[i]);
	  }
	  for (int i=0; i < etypes.length; ++i) {
		  etypes[i] = computeErasure(etypes[i]);
	  }
	  return JavaTypeFactory.getFunctionType(
			  null, computeErasure(ft.getReturnType()), 
			  Arrays.asList(ptypes), ft.isVariable(), 
			  new HashSet<IJavaType>(Arrays.asList(etypes)));
  }
  
  /**
   * Check whether this type is a functional type,
   * and if so return its descriptor.  This code
   * meets the Java specification, modified to simplify
   * the definition for an intersection type.
   * @param t type to check
   * @return descriptor if this is a function type and null otherwise
   */
  @Override
  public IJavaFunctionType isFunctionalType(IJavaType t) {
	  /*
	   *  A function type is one of the following:
       *  - A non-generic functional interface
       *  - A parameterization (4.5) of a functional interface
       *  - A raw functional interface type (4.8)
       *  - An intersection (4.9) of interface types that meets the following criteria:
       *    (i) Exactly one element of the intersection is a functional interface type. 
       *        Let F be this interface.
       *    (ii) A notional interface, I, that extends all the interfaces in the 
       *         intersection would be a functional interface. 
       *         If any of the intersection elements is a parameterized type, 
       *         then I is generic: for each element of the intersection 
       *         that is a parameterized type J<A1...An>, let P1...Pn be the type parameters of J; 
       *         then P1...Pn are also type parameters of I, and 
       *         I extends the type J<P1...Pn>.
       *    (iii) The function descriptor of I is the same as the function descriptor of F 
       *          (after adapting for any type parameters (8.4.4)). 
       * JTB: The last definition is changed (in this code) to be have simpler criteria
       *    (i) Exactly one of the elements of the intersection is a functional interface type
       *    (ii) All other interfaces have no members other than ones with
       *         the same signature as any public instance method of the class Object
       *         
       * The function descriptor of a parameterized functional interface, F<A1...An>, 
       * where A1...An are type arguments (4.5.1), is derived as follows. 
       * Let P1...Pn be the type parameters of F; 
       * types T1...Tn are derived from the type arguments 
       * according to the following rules (for 1 ≤ i ≤ n):
       *  - If Ai is a type, then Ti = Ai.
       *  - If Ai is a upper-bounded wildcard ? extends Ui, then Ti = Ui.
       *  - If Ai is a lower-bounded wildcard ? super Li, then Ti = Li.
       *  - If Ai is an unbound wildcard ?, 
       *    then if Pi has upper bound Bi that mentions none of P1...Pn, 
       *    then Ti = Bi; otherwise, Ti = Object. 
       * If F<T1...Tn> is a well-formed type, 
       * then the descriptor of F<A1...An> is the result of applying substitution 
       *   [P1:=T1, ..., Pn:=Tn] to the descriptor of interface F. 
       * Otherwise, the descriptor of F<A1...An> is undefined.
       * JTB: This definition is a mess
       *      Something could be a function type but not have a descriptor.
       *      I will modify it to simply be the capture-handling substitution
       *      
       * The function descriptor of a raw functional interface type 
       * is the erasure of the functional interface's descriptor.
       *
       * The function descriptor of an intersection that is a function type 
       * is the same as the function descriptor of the functional interface or 
       * parameterization of a functional interface that is an element of the intersection. 
	   */
	  if (t instanceof IJavaIntersectionType) {
		  IJavaIntersectionType intt = (IJavaIntersectionType)t;
		  boolean found = false;
		  for (IJavaType piece : intt) {
			  if (!(piece instanceof IJavaDeclaredType)) return null;
			  IJavaDeclaredType p = (IJavaDeclaredType)piece;
			  SingleMethodGroupSignatures sigs = this.getInterfaceSingleMethodSignatures(p.getDeclaration());
			  if (sigs == null) return null;
			  if (sigs.size() == 0) continue; // ignore this one
			  if (found) return null; // more than one, can't be right
			  found = true;
			  t = p;
		  }
		  // fall through
	  }
	  if (!(t instanceof IJavaDeclaredType)) return null;
	  IJavaDeclaredType p = (IJavaDeclaredType)t;
	  IRNode decl = p.getDeclaration();
	  IJavaFunctionType descriptor = isFunctionalInterface(decl);
	  if (descriptor == null) return null;
	  IRNode typeFormalsNode = InterfaceDeclaration.getTypes(decl);
	  if (JavaNode.tree.numChildren(typeFormalsNode) > 0) {
		  List<IJavaType> actuals = p.getTypeParameters();
		  if (actuals.size() == 0) return computeErasure(descriptor);
		  List<IJavaTypeFormal> formals = new ArrayList<IJavaTypeFormal>();
		  for (IRNode tf : JavaNode.tree.children(typeFormalsNode)) {
			  formals.add(JavaTypeFactory.getTypeFormal(tf));
		  }
		  return descriptor.subst(new SimpleTypeSubstitution(getBinder(),formals,actuals));
	  }
	  return descriptor;
  }

  /**
   * If the interface is a functional interface, return its descriptor.
   * Otherwise return null.  Meets the Java specification.
   * @param idecl node of the interface declaration
   * @return descriptor (if a functional interface) or null (if not)
   */
  public IJavaFunctionType isFunctionalInterface(IRNode idecl) {
	  /* For interface I, 
	   * let M be the set of abstract methods that are members of I 
	   * but that do not have the same signature as any 
	   * public instance method of the class Object. 
	   * Then I is a functional interface 
	   *    if there exists a method m in M for which the following conditions hold:
       *    * The signature of m is a subsignature (8.4.2) 
       *      of every method's signature in M.
       *    * m is return-type-substitutable (8.4.5) for every method in M.
       *    
       * The descriptor of I consists of the following:
       *   Type parameters, formal parameters, and return types: 
       *     Let m be a method in M with 
       *     i) a signature that is a subsignature of every method's signature in M and 
       *     ii) a return type that is a subtype of every method's return type in M 
       *         (after adapting for any type parameters (8.4.4)); 
       *     if no such method exists, then let m be a method in M that 
       *     i) has a signature that is a subsignature of every method's signature in M and
       *     ii) is return-type-substitutable for every method in M. 
       *     Then the descriptor's type parameters, formal parameter types, and return type
       *     are as given by m. 
       *   Thrown types: 
       *     The descriptor's thrown types are derived from the throws clauses 
       *     of the methods in M. If the descriptor is generic, these clauses 
       *     are first adapted to the type parameters of m (see above); 
       *     if the descriptor is not generic but at least one method in M is, 
       *     these clauses are first erased. 
       *     Then the descriptor's thrown types include every type, E, satisfying the following constraints:
       *     * E is mentioned in one of the throws clauses.
       *     * For each throws clause, E is a subtype of some type named in that clause.
       */
	  SingleMethodGroupSignatures sigs = getInterfaceSingleMethodSignatures(idecl);
	  if (sigs == null) return null;
	  if (sigs.size() == 0) return null;
	  // We need to see if there is a single signature
	  // that is a subsignature of all others and is return compatible.
	  // If so return it, otherwise return null
	  // for now, do the very simplest literal thing:
	  IJavaFunctionType descriptorStrict = null, descriptorLoose = null;
	  for (IJavaFunctionType ft1 : sigs) {
		  // LOG.warning("Considering sig: " + ft1);
		  Boolean strict = Boolean.TRUE;
		  for (IJavaFunctionType ft2 : sigs) {
			  if (ft1 == ft2) continue;
			  if (!isSubsignature(ft1, ft2) ||
				  !isReturnTypeSubstitutable(ft1,ft2)) {
				  /*if (!isSubsignature(ft1, ft2)) {
					  LOG.warning("  not a subsig of " + ft2);
				  } else if (!isReturnTypeSubstitutable(ft1, ft2)) {
					  LOG.warning("  not return type substitutable of " + ft2);
				  }*/
				  strict = null;
				  break;
			  }
			  SimpleTypeSubstitution s = new SimpleTypeSubstitution(getBinder(),ft2.getTypeFormals(),ft1.getTypeFormals());
			  if (!isSubType(ft1.getReturnType(),ft2.getReturnType().subst(s))) {
				  // LOG.warning("  not a strict subtype of " + ft2);
				  strict = Boolean.FALSE;
			  }
		  }
		  if (strict == null) continue;
		  if (strict) descriptorStrict = ft1;
		  else descriptorLoose = ft1;
	  }
	  IJavaFunctionType result = descriptorStrict;
	  if (result == null) result = descriptorLoose;
	  if (result == null) return result;
	  if (sigs.size() == 1) return result;
	  
	  // figure out the intersection of the throws
	  Collection<Set<IJavaType>> throwSets = new ArrayList<Set<IJavaType>>();
	  Set<IJavaType> resultThrows = new HashSet<IJavaType>();
	  for (IJavaFunctionType ft : sigs) {
		  Set<IJavaType> ts = ft.getExceptions();
		  List<IJavaTypeFormal> tfs1 = result.getTypeFormals();
		  List<IJavaTypeFormal> tfs2 = ft.getTypeFormals();
		  if (tfs1.size() == 0 && tfs2.size() > 0) {
			  Set<IJavaType> erased = new HashSet<IJavaType>();
			  for (IJavaType t : ts) {
				  erased.add(computeErasure(t));
			  }
			  ts = erased;
		  } else if (tfs1.size() > 0 && tfs2.size() > 0) {
			  Set<IJavaType> adapted = new HashSet<IJavaType>();
			  IJavaTypeSubstitution s = new SimpleTypeSubstitution(getBinder(), tfs2, tfs1);
			  for (IJavaType t : ts) {
				  adapted.add(t.subst(s));
			  }
			  ts = adapted;
		  }
		  throwSets.add(ts);
	  }
	  for (Set<IJavaType> setOfCandidates : throwSets) {
		  testCandidate: for (IJavaType candidate : setOfCandidates) {
			  inSet: for (Set<IJavaType> throwSet : throwSets) {
				  for (IJavaType th : throwSet) {
					  if (isSubType(candidate,th)) continue inSet; // OK here
				  }
				  continue testCandidate; // no good, try next candidate
			  }
			  resultThrows.add(candidate);
 		  }
	  }
	  // modify the function type accordingly
	  return JavaTypeFactory.getFunctionType(result.getTypeFormals(), result.getReturnType(), result.getParameterTypes(), result.isVariable(), resultThrows);
  }
  
  // efficiency, avoid complex case for methods without these names.
  private final Set<String> javaPublicMethodNames = 
		  new HashSet<String>(Arrays.asList("equals","getClass","hashCode",
				  "notify","notifyAll","toString","wait"));
  
  // a helper method for isFunctionalInterface
  /**
   * Return a collection of signature all with the same name and arity that
   * this interface exclusively defines, ignoring all public methods of Object
   * and those override equivalent to them.  If there are no such methods,
   * the collection is empty.  If there are more than one name, or more than
   * one arity, then return null.
   * @param idecl IRNode for an interface declaration
   * @return collection of signatures if all such signatures have the same
   * name and arity.  Otherwise, return an empty signature (if no such methods) or null 
   * (if there are conflicting names and/or arities). 
   */
  public SingleMethodGroupSignatures getInterfaceSingleMethodSignatures(IRNode idecl) {
	  if (idecl == null) return null;
	  if (!InterfaceDeclaration.prototype.includes(JJNode.tree.getOperator(idecl))) {
		  return null;
	  }
	  
	  // get all inherited methods
	  SingleMethodGroupSignatures result = SingleMethodGroupSignatures.EmptyMethodGroupSignatures.instance;
	  for (IRNode extn : JJNode.tree.children(InterfaceDeclaration.getExtensions(idecl))) {
		  IJavaType extt = this.convertNodeTypeToIJavaType(extn);
		  if (extt instanceof IJavaDeclaredType) {
			  IJavaDeclaredType dt = (IJavaDeclaredType)extt;
			  IRNode edecl = dt.getDeclaration();
			  SingleMethodGroupSignatures igroup = getInterfaceSingleMethodSignatures(edecl);
			  if (igroup == null) return null;
			  if (igroup.size() == 0) continue;
			  IJavaTypeSubstitution sub = IJavaTypeSubstitution.NULL;
			  if (JJNode.tree.numChildren(InterfaceDeclaration.getTypes(edecl)) > 0) {
				  List<IJavaTypeFormal> tformals = new ArrayList<IJavaTypeFormal>();
				  for (IRNode tfn : JJNode.tree.children(InterfaceDeclaration.getTypes(edecl))) {
					  tformals.add(JavaTypeFactory.getTypeFormal(tfn));
				  }
				  sub = new SimpleTypeSubstitution(getBinder(),tformals,dt.getTypeParameters());
			  }
			  result = result.add(igroup.subst(sub));
			  if (result == null) return null; // give up immediately
		  }
	  }
	  
	  // look at all methods declared in the interface
	  considerMethod:
	  for (IRNode memNode : JJNode.tree.children(InterfaceDeclaration.getBody(idecl))) {
		  // must be a method declaration
		  if (!MethodDeclaration.prototype.includes(JJNode.tree.getOperator(memNode))) continue;
		  // ignore if default or static
		  int mods = JavaNode.getModifiers(memNode);
		  if ((mods & (JavaNode.DEFAULT|JavaNode.STATIC)) != 0) continue;
		  
		  String name = JJNode.getInfo(memNode);
		  IJavaFunctionType sig = JavaTypeFactory.getMemberFunctionType(memNode, null, getBinder());
		  // ignore if a public method in Object
		  if (javaPublicMethodNames.contains(name)) {
			  IRNode odecl = this.findNamedType("java.lang.Object");
			  for (IRNode omem : JJNode.tree.children(ClassDeclaration.getBody(odecl))) {
				  if (JJNode.getInfo(omem).equals(name)) {
					  IJavaFunctionType osig = JavaTypeFactory.getMemberFunctionType(omem, null, getBinder());
					  if (isSameSignature(sig,osig)) continue considerMethod; // ignore this method
				  }
			  }
		  }
		  
		  result = result.add(new SingleMethodGroupSignatures.SingletonMethodGroupSignature(name,sig));
		  if (result == null) return null;
	  }
	  return result;
  }
  
  /* Two methods or constructors, M and N, have the same signature if 
   * they have the same name, 
   * the same type parameters (if any) (8.4.4), and, 
   * after adapting the formal parameter types of N 
   * to the the type parameters of M, the same formal parameter types.
   * 
   * Two methods or constructors M and N have the same type parameters if 
   * both of the following are true:
   * 1. M and N have same number of type parameters (possibly zero).
   * 2. Where A1, ... , An are the type parameters of M and 
   *    B1, ..., Bn are the type parameters of N, 
   *    let theta=[B1:=A1, ..., Bn:=An]. 
   *    Then, for all i, 1�i�n, 
   *        the bound of Ai is the same type as theta applied to the bound of Bi. 
   * Where two methods or constructors M and N have the same type parameters, 
   * a type mentioned in N can be adapted to the type parameters of M 
   * by applying theta, as defined above, to the type.
   * 
   * JTB: theta is a SimpleTypeSubstitution from the type formals of ft1 to
   * the type formals of ft2.
   */
  @Override
  public boolean isSameSignature(IJavaFunctionType ft1, IJavaFunctionType ft2) {
	  int ntf = ft1.getTypeFormals().size();
	  if (ntf != ft2.getTypeFormals().size()) return false;
	  
	  int nf = ft1.getParameterTypes().size();
	  if (nf != ft2.getParameterTypes().size()) return false;
	  
	  IJavaTypeSubstitution theta = new SimpleTypeSubstitution(
			  getBinder(),ft1.getTypeFormals(),ft2.getTypeFormals());
	  
	  for (int i=0; i < ntf; ++i) {
		  if (ft1.getTypeFormals().get(i).getExtendsBound(this).subst(theta) !=
				  ft2.getTypeFormals().get(i).getExtendsBound(this)) {
			  return false;
		  }
	  }
	  
	  for (int i=0; i < nf; ++i) {
		if (ft1.getParameterTypes().get(i).subst(theta) !=
				ft2.getParameterTypes().get(i)) {
			return false;
		}
	  }
	  
	  return true;
  }
  
  /* The signature of a method m1 is a subsignature of the signature of a method m2 
   * if either:
   * m2 has the same signature as m1, or
   * the signature of m1 is the same as the erasure (�4.6) of the signature of m2.
   */
  @Override
  public boolean isSubsignature(IJavaFunctionType ft1, IJavaFunctionType ft2) {
	  return isSameSignature(ft1,ft2) ||
			  isSameSignature(ft1,computeErasure(ft2));
  }

  @Override
  public boolean isOverrideEquivalent(IJavaFunctionType ft1, IJavaFunctionType ft2) {
	  return isSubsignature(ft1,ft2) || isSubsignature(ft2,ft1);
  }

  /*
   * A method declaration d1 with return type R1 is return-type-substitutable for another method d2 with return type R2, if and only if the following conditions hold:
   *
   * If R1 is void then R2 is void.
   * If R1 is a primitive type, then R2 is identical to R1.
   * If R1 is a reference type then one of the following is true:
   *     R1, adapted to the type parameters of d2 (8.4.4), is a subtype of R2. or
   *     R1 can be converted to a subtype of R2 by unchecked conversion (5.1.9), or
   *     d1 does not have the same signature as d2 (8.4.2), and R1 = |R2| 
   * 
   * JTB: Presumably all uses of R1 in the second and third choices should refer
   * to R1, adapted to the type parameters of d2.
   * Also presumably, we don't call this method if the function types are wildly different
   * (say with different numbers of type parameters).
   */
  @Override
  public boolean isReturnTypeSubstitutable(IJavaFunctionType ft1,
		  IJavaFunctionType ft2) {
	  IJavaType r1 = ft1.getReturnType();
	  IJavaType r2 = ft2.getReturnType();
	  if (r1 == JavaTypeFactory.voidType || r1 instanceof IJavaPrimitiveType) {
		  return r2 == r1;
	  }
	  if (r2 == JavaTypeFactory.voidType || r2 instanceof IJavaPrimitiveType) {
		  return false;
	  }
	  if (ft1.getTypeFormals().size() != ft2.getTypeFormals().size()) return false;
	  IJavaTypeSubstitution subst = new SimpleTypeSubstitution(getBinder(),ft1.getTypeFormals(),ft2.getTypeFormals());
	  r1 = r1.subst(subst);
	  if (isSubType(r1,r2)) return true;
	  //XXX: Not sure how to do (ii)
	  //(iii) without the extra check seems to handle most things.
	  if (r1 == computeErasure(r2)) return true;
	  return false;
  }
  
  @Override
  public boolean isCallCompatible(IJavaFunctionType ft1, IJavaFunctionType ft2, InvocationKind kind) {
	  // TODO: write this using the new constraint stuff.
	  if (!kind.isVariable() && ft1.getParameterTypes().size() != ft2.getParameterTypes().size()) {
		  return false;
	  }
	  return false;  
  }


class SupertypesIterator extends SimpleIterator<IJavaType> {
	final Iterator<IRNode> nodes;
    final IJavaTypeSubstitution subst;
    final boolean needsErasure;

    SupertypesIterator(IJavaType first, Iterator<IRNode> nodes, IJavaTypeSubstitution s, boolean needsErasure) {
      super(first);
      this.nodes = nodes;
      this.subst = s;
      this.needsErasure = needsErasure;
    }
    
    @Override
    protected Object computeNext() {
      if (!nodes.hasNext()) {
        return noElement;
      }
      IJavaType type = convertNodeTypeToIJavaType(nodes.next());
      if (type == null) return computeNext(); // skip this one
      type = type.subst(subst);
  	
      if (needsErasure) {
    	  // JLS 4.8
    	  // The superclasses (respectively, superinterfaces) of a raw type are the erasures 
    	  // of the superclasses (superinterfaces) of any of the parameterizations of the generic type.
    	  type = JavaTypeVisitor.computeErasure((IJavaDeclaredType) type);
      }
      return type;
    }
    
    @Override
    @Borrowed("this")
	@RegionEffects("writes Instance")
	public void remove() {
      throw new UnsupportedOperationException("Cannot remove");
    }
  }
  
  static class BoundsIterator extends FilterIterator<IRNode,IJavaType> {
    final IBinder binder;
    
    BoundsIterator(IRNode bounds, IBinder binder) {
      super(MoreBounds.getBoundIterator(bounds));
      this.binder = binder;
    }

    @Override
    protected IJavaType select(IRNode n) {      
      IJavaType bound = binder.getJavaType(n);
      IJavaType rv = JavaTypeVisitor.captureWildcards(binder, bound);
      return rv;
    }
  }
  
  /**
   * http://java.sun.com/docs/books/jls/third_edition/html/typesValues.html#4.10.2
   * 
   * Return an enumeration of the direct supertypes of the given type.
   * If the type is a class type, return its superclass and each of its interfaces.
   * If the type is an interface type, return "Object" and its super interfaces.
   * If it is an array type, return an iteration of the single type "Object".
   * If it is an anon class expression, return the named supertype
   * (preceded by Object, if this is an interface type).
   * Otherwise (even for a null type) return an empty iterator.
   * @param ty
   * @return
   */
  @Override
  public Iteratable<IJavaType> getSuperTypes(IJavaType ty) {
	ty = JavaTypeVisitor.captureWildcards(getBinder(), ty);
	
    //IBinder binder               = getBinder();
    final IJavaType javalangobjectType = getObjectType();
    if (ty == javalangobjectType || !(ty instanceof IJavaReferenceType) || ty instanceof IJavaNullType) {
      return new EmptyIterator<IJavaType>();
    }
    if (ty instanceof IJavaIntersectionType) {
      IJavaIntersectionType it = (IJavaIntersectionType)ty;
      return new PairIterator<IJavaType>(it.getPrimarySupertype(),it.getSecondarySupertype());
    }
    if (ty instanceof IJavaTypeFormal) {
      IJavaTypeFormal tf = (IJavaTypeFormal) ty; 
      IRNode bounds      = TypeFormal.getBounds(tf.getDeclaration());
      if (JJNode.tree.numChildren(bounds) == 0) {
    	  return new SingletonIterator<IJavaType>(javalangobjectType);
      }
      return new BoundsIterator(bounds, getBinder());
    } 
    if (ty instanceof IJavaCaptureType) {
      IJavaCaptureType ct = (IJavaCaptureType) ty;
      if (ct.getUpperBound() instanceof IJavaNullType) {
    	  // JLS 4.10.2: 
    	  // The direct supertypes of the null type are all reference 
    	  // types other than the null type itself.
    	  LOG.info("WARNING: Using upper bound as supertype for capture type "+ct);
          return new SingletonIterator<IJavaType>(ct.getLowerBound());
      }
      return new SingletonIterator<IJavaType>(ct.getUpperBound());
    }
    if (ty instanceof IJavaWildcardType) {
      IJavaWildcardType wct = (IJavaWildcardType)ty;
      IJavaType bt = wct.getUpperBound();
      if (bt != null) {
        return new SingletonIterator<IJavaType>(bt);
      }      
      return new SingletonIterator<IJavaType>(javalangobjectType);
    }
    else if (ty instanceof IJavaArrayType) {
      IJavaArrayType at = (IJavaArrayType) ty;
      IRNode decl = getArrayClassDeclaration();
      IJavaType arrayType = JavaTypeFactory.getDeclaredType(decl, Collections.singletonList(at.getElementType()), null);      
      return new TripleIterator<IJavaType>(arrayType,//javalangobjectType, 
                                           findJavaTypeByName("java.lang.Cloneable"),
                                           findJavaTypeByName("java.io.Serializable"));      
    }
    if (!(ty instanceof IJavaDeclaredType)) {
      // TODO not right for null type
      return new EmptyIterator<IJavaType>();
    }
    IJavaDeclaredType dt = ((IJavaDeclaredType)ty);
    IRNode tdecl = dt.getDeclaration(); 
    if (tdecl == null) {
      LOG.severe("declared type is null ? " + ty);
      return new EmptyIterator<IJavaType>();
    }
    IJavaType superclass;
    IRNode superinterfaces;
    Operator op = JJNode.tree.getOperator(tdecl);
    final IJavaTypeSubstitution subst = JavaTypeSubstitution.create(this, dt);
    final boolean needsErasure = dt.isRawType(this);
    if (op instanceof ClassDeclaration) {
      superclass = convertNodeTypeToIJavaType(ClassDeclaration.getExtension(tdecl));
      if (superclass == null) {
    	if (AbstractJavaBinder.isBinary(tdecl)) {
    		System.err.println("null extension for "+DebugUnparser.toString(tdecl));
    	} else {
    		LOG.severe("extension for " + DebugUnparser.toString(tdecl) + " is empty!");
    	}
 		superclass = javalangobjectType;
      } else {
    	superclass = superclass.subst(subst);
      }
      superinterfaces = ClassDeclaration.getImpls(tdecl);
    } else if (op instanceof InterfaceDeclaration) {
      superinterfaces = InterfaceDeclaration.getExtensions(tdecl);
      
      if (JJNode.tree.hasChildren(superinterfaces)) {
    	  superclass = null; 
      } else {
    	  return new SingletonIterator<IJavaType>(javalangobjectType);
      }
    } else if (op instanceof EnumDeclaration) {
      IRNode ed = findNamedType("java.lang.Enum");      
      List<IJavaType> params = Collections.singletonList(ty);
      superclass = JavaTypeFactory.getDeclaredType(ed, params, null);
      superinterfaces = EnumDeclaration.getImpls(tdecl);
    } else if (op instanceof AnonClassExpression) {
      IRNode supertypenode = AnonClassExpression.getType(tdecl);
      IJavaType supertype = convertNodeTypeToIJavaType(supertypenode);
      supertype = supertype.subst(subst);
      if (supertype instanceof IJavaDeclaredType) {
    	/*
        IRNode supertdecl = ((IJavaDeclaredType)supertype).getDeclaration();        
        if (JJNode.tree.getOperator(supertdecl) instanceof InterfaceDeclaration) {
          //complicated case!
          return new PairIterator<IJavaType>(javalangobjectType, supertype);
        } else {
          return new SingletonIterator<IJavaType>(supertype);
        }
        */
        return new SingletonIterator<IJavaType>(supertype);
      }    
      LOG.severe("Unexpected anon class superclass " + supertype);
      return new EmptyIterator<IJavaType>();
    } else if (op instanceof AnnotationDeclaration) {
      final int version = getMajorJavaVersion();
      IRNode anno     = findNamedType("java.lang.annotation.Annotation");      
      if (version < 5 || anno == null) {
    	  // Avoid dying if the JRE is lower than Java 5
    	  return new SingletonIterator<IJavaType>(getObjectType());
      }
      IJavaType annoT = convertNodeTypeToIJavaType(anno);
      return new SingletonIterator<IJavaType>(annoT);
    } else if (op instanceof EnumConstantClassDeclaration) {
      IRNode enumD    = VisitUtil.getEnclosingType(tdecl);
      IJavaType enumT = convertNodeTypeToIJavaType(enumD);
      return new SingletonIterator<IJavaType>(enumT);
    } else if (op instanceof LambdaExpression) {
      return new SingletonIterator<IJavaType>(javalangobjectType);
    } else {
      LOG.severe("Unknown type declaration node " + op);
      return new EmptyIterator<IJavaType>();
    }
    Iterator<IRNode> ch = JJNode.tree.children(superinterfaces);
    return new SupertypesIterator(superclass,ch,subst,needsErasure);
  }
  
  @Override
  public IJavaDeclaredType getSuperclass(IJavaDeclaredType dt) {
	  dt = (IJavaDeclaredType) JavaTypeVisitor.captureWildcards(getBinder(), dt);
	  
	  final IRNode declaration = dt.getDeclaration();
	  final Operator op = JJNode.tree.getOperator(declaration);
	  if (this == getObjectType())
		  return null;
	  if (ClassDeclaration.prototype.includes(op)) {
		  if ("Object".equals(JJNode.getInfo(declaration)) && dt.getName().equals(SLUtility.JAVA_LANG_OBJECT)) {
			  return null;
		  }
		  IRNode extension = ClassDeclaration.getExtension(declaration);
		  IJavaType t = convertNodeTypeToIJavaType(extension);
		  // TODO: What if we extend a nested class from our superclass?
		  // A: The type factory should correctly insert type actuals
		  // for the nesting (if any).  Actually maybe the canonicalizer should.
		  if (t != null) {
			  if (dt.isRawType(this)) {
				  // JLS 4.8
				  // The superclasses (respectively, superinterfaces) of a raw type are the erasures 
				  // of the superclasses (superinterfaces) of any of the parameterizations of the generic type.
				  return JavaTypeVisitor.computeErasure((IJavaDeclaredType) t);
			  }
			  t = t.subst(JavaTypeSubstitution.create(this, dt));
		  } else {
			  // Default to j.l.O
			  return getObjectType();
		  }
		  /*if (!(t instanceof IJavaDeclaredType)) {
	        LOG.severe("Classes can only extend other classes");
	        return null;
	      }*/
		  return (IJavaDeclaredType) t;
	  } else if (InterfaceDeclaration.prototype.includes(op)) {
		  return getObjectType();
	  } else if (EnumDeclaration.prototype.includes(op)) {
		  IRNode ed              = findNamedType("java.lang.Enum");
		  List<IJavaType> params = new ArrayList<IJavaType>(1);
		  params.add(dt);
		  return JavaTypeFactory.getDeclaredType(ed, params, null);
	  } else if (AnonClassExpression.prototype.includes(op)) {
		  IRNode nodeType = AnonClassExpression.getType(declaration);
		  IJavaType t = convertNodeTypeToIJavaType(nodeType);
		  /*if (!(t instanceof IJavaDeclaredType)) {
	        LOG.severe("Classes can only extend other classes");
	        return null;
	      }*/
		  IJavaDeclaredType jdt = ((IJavaDeclaredType) t);
		  if (JJNode.tree.getOperator(jdt.getDeclaration()) instanceof InterfaceDeclaration) {
			  return getObjectType();
		  }
		  return jdt;
	  } else if (EnumConstantClassDeclaration.prototype.includes(op)) {
		  IRNode enumD = VisitUtil.getEnclosingType(declaration);
		  return (IJavaDeclaredType) convertNodeTypeToIJavaType(enumD);
	  } else if (AnnotationDeclaration.prototype.includes(op)) {
	      IRNode ed              = findNamedType("java.lang.annotation.Annotation");
	      List<IJavaType> params = new ArrayList<IJavaType>(1);
	      params.add(dt);
	      return JavaTypeFactory.getDeclaredType(ed, params, null);
	  } else {
		  LOG.severe("Don't know what sort of declation node this is: " + DebugUnparser.toString(declaration));
		  return null;
	  }
  }
  
  /**
	 * @see edu.cmu.cs.fluid.java.bind.ITypeEnvironment#isSubType(IJavaType, IJavaType)
	 */
  @Override
  public final boolean isSubType(IJavaType s, IJavaType t) {
	  return isSubType(s, t, false);
  }
  
  @Override
  public final boolean isRawSubType(IJavaType s, IJavaType t) {
	  return isSubType(s, t, true);
  }
  
  protected boolean isSubType(final IJavaType s, final IJavaType t, final boolean ignoreGenerics) {
	if (!ignoreGenerics) {
		//total++;
		Boolean result = subTypeCache.get(Pair.getInstance(s, t));

		if (result != null) {
			//cached++;
			return result.booleanValue();
		}		
	}
	if (s == null || t == null) {
		// LOG.severe("isSubType() s = "+s+", t = "+t);
		return false; // nonsense
	}
	if (s.isEqualTo(this, t) || s == JavaTypeFactory.anyType) {
		return true;
	}
	if (s instanceof IJavaPrimitiveType) {
		if (t instanceof IJavaPrimitiveType) {
			return isPrimSubType((IJavaPrimitiveType) s, (IJavaPrimitiveType) t);
		}
		return false;
	}
	else if (t instanceof IJavaPrimitiveType) {
		return false; // Only one is a prim
	}
	/*
	// subtyping is only true for reference types.
	if (!(s instanceof IJavaReferenceType) || !(t instanceof IJavaReferenceType)) {
		return false;
	}
	*/
	// From JLS 4.10.2
	if (s instanceof IJavaNullType) {
		return !(t instanceof IJavaNullType);
	}
	if (t == getObjectType()) { 
		return true;
	}
	
	boolean result = false;
	try {
		if (s instanceof IJavaArrayType) {
			if (t instanceof IJavaDeclaredType) {
				for (IJavaType supertype : getSuperTypes(s)) {
					if (isSubType(supertype,t, ignoreGenerics)) return result = true;
				}
				return result = false;
			}
			else if (!(t instanceof IJavaArrayType)) {
				return result = false;
			}
			// use standard Java broken array subtyping.
			result = isSubType(((IJavaArrayType)s).getElementType(),
					 ((IJavaArrayType)t).getElementType(), ignoreGenerics);
			return result;
			// NB: we cannot require the number of dimensions to
			// be the same because int[][] is a subtype of Object[] etc.
		}
		if (t instanceof IJavaArrayType || t instanceof IJavaNullType) {
			return result = false;
		}

		// subtype of an intersection type requires that we are subtype of each one
		// (handling an intersection type on the left is handle by the general case)
		if (t instanceof IJavaIntersectionType) {
			IJavaIntersectionType it = (IJavaIntersectionType)t;
			result = isSubType(s,it.getPrimarySupertype(), ignoreGenerics) && 
			         isSubType(s,it.getSecondarySupertype(), ignoreGenerics);
			return result;
		}

		if (s instanceof IJavaUnionType) {
			IJavaUnionType us = (IJavaUnionType) s;
			result = isSubType(us.getFirstType(), t, ignoreGenerics) &&
			         isSubType(us.getAlternateType(), t, ignoreGenerics);
			return result;
		}
		
		if (t instanceof IJavaWildcardType) {
			// JLS 4.10.2 -- A type variable is a direct supertype of its lower bound
			IJavaWildcardType wt = (IJavaWildcardType) t;
			if (wt.getLowerBound() != null) {
				result = isSubType(s, wt.getLowerBound());
				if (result) {
					return result;
				}
			}
		}
		
		if (t instanceof IJavaTypeVariable) {
			// JLS 4.10.2 -- A type variable is a direct supertype of its lower bound
			IJavaTypeVariable ct = (IJavaTypeVariable) t;
			result = /*isSubType(s, ct.getUpperBound()) &&*/ isSubType(s, ct.getLowerBound());
			if (result) {
				return result;
			}
		}
		
		// need to handle wildcard type parameters:
		// List<Integer> is NOT a subtype of List<Object>, but 
		// List<Integer> is a subtype of List<? extends Object>
		if (s instanceof IJavaDeclaredType && t instanceof IJavaDeclaredType) {
			IJavaDeclaredType sd = ((IJavaDeclaredType)s);
			IJavaDeclaredType td = ((IJavaDeclaredType)t);
			if (sd.getDeclaration() == td.getDeclaration() || areEquivalent(sd, td)) {
				if (ignoreGenerics) {
					return result = true;
				}
				// we will return true or false.
				List<IJavaType> sl = sd.getTypeParameters();
				List<IJavaType> tl = td.getTypeParameters();
				if (tl.isEmpty()) return result = true; // raw types (from JLS 4.10.2)
				if (sl.isEmpty()) return result = false; // raw is NOT a subtype
				Iterator<IJavaType> sli = sl.iterator();
				Iterator<IJavaType> tli = tl.iterator();
				// if we find any non-matches, we fail
				while (sli.hasNext() && tli.hasNext()) {
					IJavaType ss = sli.next();
					IJavaType tt = tli.next();
					if (!typeArgumentContained(ss,tt,ignoreGenerics)) return result = false;
				}
				return result = true;
			}
		}

		if (t instanceof JavaRefTypeProxy) {
			JavaRefTypeProxy p = (JavaRefTypeProxy) t;
			return isSubType(s, p.first()) || isSubType(s, p.second());
		}
		
		// now we do the easy, and slow, thing:
		for (Iterator<IJavaType> it = s.getSupertypes(this); it.hasNext(); ) {
			IJavaType supertype = it.next();
			if (isSubType(supertype,t,ignoreGenerics)) return result = true;
		}
		return result = false;
	} finally {
		if (!ignoreGenerics) {
			//subTypeCache.put(Pair.getInstance(s, t), result);			
		}
	}
  }

  /**
   * � double >1 float
   * � float >1 long
   * � long >1 int
   * � int >1 char
   * � int >1 short
   * � short >1 byte
   */
  private boolean isPrimSubType(IJavaPrimitiveType s, IJavaPrimitiveType t) {
	final IPrimitiveType.Kind sKind = s.getKind();
	final IPrimitiveType.Kind tKind = t.getKind();
	if (sKind == tKind) {
		return true;
	}
	if (sKind.rank() < 0 || tKind.rank() < 0) {
		return false; 
	}
	if (tKind == IPrimitiveType.Kind.CHAR) {
		return false;		
	}
	return sKind.rank() < tKind.rank();
  }

  /**
   * Added to deal with the fact that Eclipse seems to allow classes from different JREs
   * to be considered the same
   */
  private boolean areEquivalent(IJavaDeclaredType sd, IJavaDeclaredType td) {
	  final String sId = JJNode.getInfoOrNull(sd.getDeclaration());
	  final String tId = JJNode.getInfoOrNull(td.getDeclaration());
	  if (sId.equals(tId)) {
		  final String sName = JavaNames.getFullTypeName(sd.getDeclaration()); 
		  //if (sName.startsWith("java")) {
			  final String tName = JavaNames.getFullTypeName(td.getDeclaration()); 
			  return sName.equals(tName);
		  //}
	  }
	  return false;
  }

/**
   * Return whether type first type argument is "contained" in the second type argument.
   * This is a stricter relation than subtyping!
   * @param ss a type argument
   * @param tt another type argument
   * @return whether ss <= tt
   * @param ignoreGenerics an argument to isSubType() if needed
   * TODO really should be "allow unchecked conversion"
   */
  protected boolean typeArgumentContained(IJavaType ss, IJavaType tt, boolean ignoreGenerics) {
    /*
     * See JLS 4.5.1.1: Type argument containment
     * (1) ? extends S <= ? extends T if S <: T
     * (1a) ? extends S <= ? 
     * (2) ? super S <= ? super T if T <: S 
     * (3) S <= S
     * (4) S <= ? extends T if S <: T
     * (5) S <= ? super T if T <: S    
     * (This is modified from JLS3 to put transitivity in. I may
     * have made a mistake here.)
     * 
     * Note from Edwin: added 1a and 2a to match apparently legal code
     * 
     * (from JLS 8)
     * (1) ? extends T <= ? extends S if T <: S
     * (1a) ? extends T <= ?
     * (2) ? super T <= ? super S if S <: T
     * (2a) ? super T <= ?
     * (2b) ? super T <= ? extends Object
     * (3) T <= T
     * T <= ? extends T
     * T <= ? super T
     */
    if (ss.isEqualTo(this, tt)) return true; // (3)
    
    // Hack to deal with capture types as if they're type variables
    if (ss instanceof IJavaCaptureType) {
    	IJavaCaptureType cs = (IJavaCaptureType) ss;
	    if (typeArgumentContained(cs.getUpperBound(), tt, ignoreGenerics)) {
	    	return true; // TODO what about lower bound?    	
	    }
    }
    if (tt instanceof IJavaCaptureType) {
    	IJavaCaptureType ct = (IJavaCaptureType) tt;
    	if (isSubType(ss, ct.getLowerBound(), ignoreGenerics) && typeArgumentContained(ss, ct.getUpperBound(), ignoreGenerics)) {
    		return true;
    	}
    }        
    
    if (ss instanceof IJavaWildcardType) {
      IJavaWildcardType sw = (IJavaWildcardType) ss;
      if (tt instanceof IJavaWildcardType) {
        IJavaWildcardType tw = (IJavaWildcardType) tt;
        if (sw.getUpperBound() != null) {
          if (tw.getUpperBound() == null) {
        	  return tw.getLowerBound() == null; // (1a)
          }
          return isSubType(sw.getUpperBound(), tw.getUpperBound(), ignoreGenerics); // (1)
        } 
        else if (sw.getLowerBound() != null) {
          if (tw.getLowerBound() == null) return true; //(2a)
          if (tw.getLowerBound() == getObjectType()) return true; //(2b)          
          return isSubType(tw.getLowerBound(), sw.getLowerBound(), ignoreGenerics); // (2)
        } else { // ? <= nothing
        	return false;
        }
      } else {
    	if (true) {
            // TODO the below doesn't seem right
    		return false;
    	}
    	// Added to handle the case of "? extends T <: T?
        return isSubType(sw.getUpperBound() != null ? sw.getUpperBound() : getObjectType(), tt, ignoreGenerics);
      }
    } else {
      if (!(tt instanceof IJavaWildcardType)) return false;
      IJavaWildcardType tw = (IJavaWildcardType)tt;
      if (tw.getUpperBound() != null) {
        return isSubType(ss,tw.getUpperBound(), ignoreGenerics); // (4)
      } else if (tw.getLowerBound() != null) {        
        return isSubType(tw.getLowerBound(),ss, ignoreGenerics); // (5)
      } else {
        // return (ss instanceof IJavaReferenceType); 
        // Anything matches ?
        return true; // Not just ref types, due to Class<?>
      }
    }
    //return false;
  }
  
  @Override
  public IJavaClassTable getClassTable() {
    throw new UnsupportedOperationException("getClassTable() is not yet implemented!");
  }


  public Set<String> getTypes() {
    return getClassTable().allNames();
  }


  @Override
  public IRNode findNamedType(String qname) {
    return getClassTable().getOuterClass(qname,null);
  }

  @Override
  public IRNode findNamedType(String qname, IRNode context) {
	  return getClassTable().getOuterClass(qname, context);
  }
  
  @Override
  public Iterable<IRNode> getRawSubclasses(IRNode type) {
	  // Optional method
	  return new EmptyIterator<IRNode>();
  }  
  
  @Override
  public IRNode findPackage(String name, IRNode context) {
    return getClassTable().getOuterClass(name,context);
  }

  @Override
  public Iterable<Pair<String,IRNode>> getPackages() {
	return getClassTable().allPackages(); 
  }
  
  @Override
  public IJavaType findJavaTypeByName(final String name) {	  
	// Check for array dimensions
    int dims = 0;    
    int possibleArray = name.length() - 2;
    while (possibleArray > 0 && name.charAt(possibleArray) == '[' && 
    		                    name.charAt(possibleArray+1) == ']') {
      dims++;
      possibleArray -= 2;
    }
    possibleArray += 2;
    // FIX what about generics
    
    if (dims > 0) {
    	IJavaType base = findJavaTypeByName_internal(name.substring(0, possibleArray));
    	return JavaTypeFactory.getArrayType(base, dims);
    }
    return findJavaTypeByName_internal(name);
  }
  
  /**
   * Arrays should already be processed
   */
  protected IJavaType findJavaTypeByName_internal(final String name) {
	IRNode t = findNamedType(name);
	if (t != null) {
	  if (TypeDeclaration.prototype.includes(t)) {
		  return convertNodeTypeToIJavaType(t);
	  } else {
		  return null; // Probably a package
	  }
	} else {	  
	  // Try to look up primitive types
	  return javaTypeMap.get(name);
	}
  }

  public boolean isSubType(IJavaType[] a1, IJavaType[] a2) {
    if (a1 == null || a2 == null || a1.length != a2.length) return false;
    for (int i=0; i < a1.length; ++i) {
      if (!isSubType(a1[i],a2[i])) return false;
    }
    return true;
  }


  /// checkIfX
  static void checkIfX(Check isX, IRNode t) {
    Operator op = JJNode.tree.getOperator(t);
    if (!isX.check(op)) {
      throw new FluidError("Unexpected "+op);
    }
  }
  public static void checkIfReturnType(IRNode t)    { checkIfX(isReturnType, t); }
  public static void checkIfType(IRNode t)          { checkIfX(isType, t); }

  public static void checkIfRefType(IRNode t)       { checkIfX(isRefType, t); }
  public static void checkIfArrayType(IRNode t)     { checkIfX(isArrayType, t); }
  public static void checkIfNamedType(IRNode t)     { checkIfX(isNamedType, t); }
  public static void checkIfNullType(IRNode t)      { checkIfX(isNullType, t); }

  public static void checkIfBooleanType(IRNode t)   { checkIfX(isBooleanType, t); }
  public static void checkIfNumericType(IRNode t)   { checkIfX(isNumericType, t); }
  public static void checkIfIntegralType(IRNode t)  { checkIfX(isIntegralType, t); }

  public static final Check isReturnType = new Check() {
    @Override public boolean check(Operator op) { return (op instanceof ReturnTypeInterface); }
  };
  public static final Check isType = new Check() {
    @Override public boolean check(Operator op) { return (op instanceof TypeInterface); }
  };
  public static final Check isRefType = new Check() {
    @Override public boolean check(Operator op) { return (op instanceof ReferenceType); }
  };
  public static final Check isArrayType = new Check() {
    @Override public boolean check(Operator op) { return (op instanceof ArrayDeclaration); }
  };
  public static final Check isIfaceType = new Check() {
    @Override public boolean check(Operator op) { return InterfaceDeclaration.prototype.includes(op); }
  };
  public static final Check isBooleanType = new Check() {
    @Override public boolean check(Operator op) { return (op instanceof BooleanType); }
  };
  public static final Check isNumericType = new Check() {
    @Override public boolean check(Operator op) { return (op instanceof NumericType); }
  };
  public static final Check isIntegralType = new Check() {
    @Override public boolean check(Operator op) { return (op instanceof IntegralType); }
  };

  public static final Check isPrimType = new Check() {
    @Override public boolean check(Operator op) { return (op instanceof PrimitiveType); }
  };
  public static final Check isNullType = new Check() {
    @Override public boolean check(Operator op) { return (op == TypeDeclaration.prototype); }
  };
  public static final Check isNamedType = new Check() {
    @Override public boolean check(Operator op) { return (op instanceof TypeDeclInterface); }
  };
  
  @Immutable
  public static abstract class Check {
    public abstract boolean check(Operator op);
    public final boolean check(IRNode n) {
      return check(JJNode.tree.getOperator(n));
    }
  }  
  
  @Override
  public IJavaType computeErasure(IJavaType ty) {
    if (!(ty instanceof IJavaReferenceType)) {
      return ty;
    }      
    if (ty instanceof IJavaDeclaredType) {
      return JavaTypeVisitor.computeErasure((IJavaDeclaredType) ty);
    }
    if (ty instanceof IJavaArrayType) {
      IJavaArrayType at = (IJavaArrayType) ty;
      IJavaType base    = computeErasure(at.getBaseType());
      if (base.equals(at.getBaseType())) {
        return ty;
      }
      return JavaTypeFactory.getArrayType(base, at.getDimensions());
    }
    if (ty instanceof IJavaTypeFormal) {
      return computeErasure(ty.getSuperclass(this));
    }
    if (ty instanceof IJavaWildcardType) {
      IJavaWildcardType wt = (IJavaWildcardType) ty;
      if (wt.getUpperBound() != null) {
        return computeErasure(wt.getUpperBound());
      } 
      return getObjectType();
    }
    if (ty instanceof IJavaCaptureType) {
      IJavaCaptureType ct = (IJavaCaptureType) ty;
      // FIX what about other type bounds?
      return computeErasure(ct.getWildcard());
    }
    return ty;
  }
  
  /**
   * Destructively erase an array of types.
   * @param args
   * @return
   */
  public void doErasure(IJavaType[] args) {
	  for (int i=0; i < args.length; ++i) {
		  args[i] = computeErasure(args[i]);
	  }
  }
}


