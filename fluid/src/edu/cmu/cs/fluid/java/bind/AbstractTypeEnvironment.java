package edu.cmu.cs.fluid.java.bind;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.Pair;
import com.surelogic.common.SLUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.util.*;
import static com.surelogic.common.util.IteratorUtil.noElement;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.*;
import com.surelogic.*;

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
	  return 0; // TODO
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
  
  /**
   * Method invocation conversion is applied to each argument value in
   * a method or constructor invocation (ï¿½8.8.7.1, ï¿½15.9, ï¿½15.12): 
   * the type of the argument expression must be converted to the type 
   * of the corresponding parameter. Method invocation contexts allow 
   * the use of one of the following:
   * 
   * an identity conversion (ï¿½5.1.1)
   * a widening primitive conversion (ï¿½5.1.2)
   * a widening reference conversion (ï¿½5.1.5)
   * a boxing conversion (ï¿½5.1.7) optionally followed by widening reference conversion
   * an unboxing conversion (ï¿½5.1.8) optionally followed by a widening primitive conversion. 
   * 
   * If, after the conversions listed above have been applied, the resulting type is a raw type
   * (ï¿½4.8), an unchecked conversion (ï¿½5.1.9) may then be applied. It is a compile time error 
   * if the chain of conversions contains two parameterized types that are not not in the subtype
   * relation.
   * 
   * If the type of an argument expression is either float or double, then value set conversion 
   * (ï¿½5.1.13) is applied after the type conversion:
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
  public boolean isCallCompatible(IJavaType t1, IJavaType t2) {
    if (t1.isEqualTo(this, t2)) return true; 
    if (t2 == JavaTypeFactory.anyType) {
      return true;
    }
    if (t1 instanceof IJavaPrimitiveType) {
      if (!(t2 instanceof IJavaPrimitiveType)) return false;
      return arePrimTypesCompatible((IJavaPrimitiveType) t1, (IJavaPrimitiveType) t2, null);
    } else {
      IJavaType erasure = computeErasure(t1);
      return isSubType(t2, erasure);
    }
  }
  
  private boolean arePrimTypesCompatible(IJavaPrimitiveType t1, IJavaPrimitiveType t2, IRNode n2) {
    PrimitiveType op1 = t1.getOp();
    PrimitiveType op2 = t2.getOp();
    assert (op1 != op2); // assuming JavaTypeFactory caches correctly.
    if (op1 instanceof BooleanType || op2 instanceof BooleanType) {
      return false;
    }    
    if (n2 != null) {
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
	  return JavaTypeFactory.getFunctionType(
			  null, computeErasure(ft.getReturnType()), 
			  Arrays.asList(ptypes), ft.isVariable(), 
			  new HashSet<IJavaType>(Arrays.asList(etypes)));
  }
  
  @Override
  public IJavaFunctionType isFunctionalType(IJavaType t) {
	  // TODO Auto-generated method stub
	  return null;
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
   *    Then, for all i, 1²i²n, 
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
   * the signature of m1 is the same as the erasure (¤4.6) of the signature of m2.
   */
  @Override
  public boolean isSubsignature(IJavaFunctionType ft1, IJavaFunctionType ft2) {
	  return isSameSignature(ft1,ft2) ||
			  isSameSignature(ft1,computeErasure(ft2));
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


class SupertypesIterator extends SimpleIterator<IJavaType> {
    Iterator<IRNode> nodes;
    IJavaTypeSubstitution subst;
    
    SupertypesIterator(Iterator<IRNode> nodes, IJavaTypeSubstitution subst) {
      this.nodes = nodes;
      this.subst = subst;
    }
    SupertypesIterator(IJavaType first, Iterator<IRNode> nodes, IJavaTypeSubstitution s) {
      super(first);
      this.nodes = nodes;
      this.subst = s;
    }
    
    @Override
    protected Object computeNext() {
      if (!nodes.hasNext()) {
        return noElement;
      }
      IJavaType type = convertNodeTypeToIJavaType(nodes.next());
      if (type == null) return computeNext(); // skip this one
      return type.subst(subst);
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
      return binder.getJavaType(n);
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
      return new TripleIterator<IJavaType>(javalangobjectType, 
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
    IJavaTypeSubstitution subst = JavaTypeSubstitution.create(this, dt);
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
    } else {
      LOG.severe("Unknown type declaration node " + op);
      return new EmptyIterator<IJavaType>();
    }
    Iterator<IRNode> ch = JJNode.tree.children(superinterfaces);
    return new SupertypesIterator(superclass,ch,subst);
  }
  
  @Override
  public IJavaDeclaredType getSuperclass(IJavaDeclaredType dt) {
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
  
  protected boolean isSubType(IJavaType s, IJavaType t, final boolean ignoreGenerics) {
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
	// subtyping is only true for reference types.
	if (!(s instanceof IJavaReferenceType) || !(t instanceof IJavaReferenceType)) {
		return false;
	}
	if (s instanceof IJavaNullType || t == getObjectType()) { 
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
				if (tl.isEmpty()) return result = true; // raw types
				Iterator<IJavaType> sli = sl.iterator();
				Iterator<IJavaType> tli = tl.iterator();
				// if we find any non-matches, we fail
				while (sli.hasNext() && tli.hasNext()) {
					IJavaType ss = sli.next();
					IJavaType tt = tli.next();
					if (!typeArgumentContained(ss,tt)) return result = false;
				}
				return result = true;
			}
		}

		// now we do the easy, and slow, thing:
		for (Iterator<IJavaType> it = getSuperTypes(s); it.hasNext(); ) {
			IJavaType supertype = it.next();
			if (isSubType(supertype,t,ignoreGenerics)) return result = true;
		}
		return result = false;
	} finally {
		if (!ignoreGenerics) {
			subTypeCache.put(Pair.getInstance(s, t), result);			
		}
	}
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
   */
  protected boolean typeArgumentContained(IJavaType ss, IJavaType tt) {
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
     */
    if (ss.isEqualTo(this, tt)) return true; // (3)
    if (ss instanceof IJavaWildcardType) {
      IJavaWildcardType sw = (IJavaWildcardType) ss;
      if (tt instanceof IJavaWildcardType) {
        IJavaWildcardType tw = (IJavaWildcardType) tt;
        if (sw.getUpperBound() != null) {
          if (tw.getUpperBound() == null) return true; // was false; (1a)
          return isSubType(sw.getUpperBound(), tw.getUpperBound()); // (1)
        } else {
          if (tw.getLowerBound() == null) return false; //(2a)
          return isSubType(tw.getLowerBound(), sw.getLowerBound()); // (2)
        }
      } else {
        return false;
      }
    } else {
      if (!(tt instanceof IJavaWildcardType)) return false;
      IJavaWildcardType tw = (IJavaWildcardType)tt;
      if (tw.getUpperBound() != null) {
        return isSubType(ss,tw.getUpperBound()); // (4)
      } else if (tw.getLowerBound() != null) {        
        return isSubType(tw.getLowerBound(),ss); // (5)
      } else {
        return (ss instanceof IJavaReferenceType); // Anything matches ?
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


