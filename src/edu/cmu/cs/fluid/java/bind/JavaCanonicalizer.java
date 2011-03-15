/*
 * $header$
 * Created on Jul 12, 2005
 */
package edu.cmu.cs.fluid.java.bind;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.CommonStrings;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.*;
import edu.cmu.cs.fluid.java.util.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.tree.SyntaxTree;
import edu.cmu.cs.fluid.version.Era;
import edu.cmu.cs.fluid.version.Version;
import edu.cmu.cs.fluid.version.VersionedRegionDelta;
import static edu.cmu.cs.fluid.java.JavaGlobals.noNodes;

/**
 * Convert a FAST that was parsed in from external sources into the canonical
 * form so it is ready for analysis. We use the binder to determine how to
 * canonicalize the nodes.
 * XXX: we need to generate boxing around string concatenation arguments.
 * @see edu.cmu.cs.fluid.java.parse.JavaParser
 * @author boyland
 */
public class JavaCanonicalizer {
  private static final Logger LOG = SLLogger.getLogger("FLUID.java.bind");

  private static final IRNode[] none = new IRNode[0];

  private final IBinder binder;

  private final IBinderCache bindCache;
  
  private final ITypeEnvironment tEnv;

  private final Visitor<Boolean> doWork = new DoCanon();

  private final SyntaxTree tree = (SyntaxTree) JJNode.tree; // NB: must be
                                                            // mutable!
  
  // For debugging
  private static boolean isCanonicalizing = false;
  
  private static synchronized void setActive(boolean state) {
    isCanonicalizing = state;
  }

  public static synchronized boolean isActive() {
    return isCanonicalizing;
  }
  
  /**
   * Create a canonicalizer that converts an AST from the raw parsed version to
   * the canonical internal representation. It uses the bindings in the version
   * that is current at the time this constructor is called to perform AST
   * canonicalization.
   * 
   * @param b
   *          binder for the current version.
   */
  public JavaCanonicalizer(IBinder b) {
    binder = fixBinder(b);
    if (binder instanceof IBinderCache) {
    	bindCache = (IBinderCache) binder;
    } else {
    	bindCache = null;
    }
    tEnv = binder.getTypeEnvironment();
  }

  protected IBinder fixBinder(IBinder b) {
	if (!JJNode.versioningIsOn) {
      return CachingBinder.create(b);
	}
    return VersionFixedBinder.fix(b);
  }
  
  interface IBinderCache {
	void init(IRNode tree);
	IBinding checkForBinding(IRNode node);
	IJavaType checkForType(IRNode node);
	void map(IRNode old, IRNode now);
	void addBinding(IRNode node, IBinding b);
	void finish(IRNode tree);	  
  }
  
  private void init(IRNode tree) {	  
	  setActive(true);
	  if (bindCache != null) {
		  bindCache.init(tree);
	  }
  }
  
  private void map(IRNode old, IRNode now) {
	  if (bindCache != null) {
		  bindCache.map(old, now);
	  }
  }
  
  private void addBinding(IRNode n, IBinding b) {
	  if (bindCache != null) {
		  bindCache.addBinding(n, b);
	  }
  }
  
  private void finish(IRNode tree) {
	  if (bindCache != null) {
		  bindCache.finish(tree);
	  }
	  setActive(false);
  }
  
  /**
   * Fix a tree that has been parsed in and bound so that it uses canonical
   * operators. The bindings from the version that was current at the time this
   * canonicalizer was created will be used. As a side-effect, the version is
   * changed so that the current version of the tree is canonical.
   * 
   * @param tree
   *          a (probably non-canonical) Java AST node.
   * @return true if changed
   */
  public boolean canonicalize(IRNode tree) {
	init(tree);
	try {
		return doWork.doAccept(tree);
	} finally {
		finish(tree);
	}	
  }

  protected void checkNode(IRNode node) {
	if (!JJNode.versioningIsOn) {
		return;
	}
    IRRegion reg = IRRegion.getOwner(node);
    if (reg instanceof VersionedRegionDelta) {
      Era e = ((VersionedRegionDelta)reg).getEra();
      Version v = ((VersionFixedBinder)binder).getAtVersion();
      if (e.getRoot().comesFrom(v)) {
        // LOG.severe("visiting nodes that were created during canonicalization!");
        throw new IllegalStateException("re-canon not allowed");
      }
    }
  }
  
  protected void replaceSubtree(IRNode orig, IRNode sub) {
	  tree.replaceSubtree(orig, sub);
	  copySrcRef(orig, sub);
  }

  protected void copySrcRef(IRNode orig, IRNode sub) {
	  ISrcRef ref = JavaNode.getSrcRef(orig);
	  if (ref != null) {
		  JavaNode.setSrcRef(sub, ref);
	  }
  }
  
  // To avoid binding problems, we can do analysis pre-order (before changes)
  // but perform the changes themselves post-order.
  // 
  // Returns true if anything changed
  class DoCanon extends Visitor<Boolean> {
    @Override
    public Boolean doAccept(IRNode node) {
      checkNode(node);
      return super.doAccept(node);
    }

    /**
     * doAcceptForChildren(), except in reverse order
     */
    public Boolean doAcceptForChildren_rev(IRNode node) {
    	return doAcceptForChildren_rev(node, null);
    }
    
    public Boolean doAcceptForChildren_rev(IRNode node, IRNode skip) {
    	int i           = JJNode.tree.numChildren(node) - 1;
    	boolean changed = false;
    	while (i >= 0) {
    	  IRNode c = JJNode.tree.getChild(node, i);
    	  if (!c.equals(skip)) {
    		  changed |= doAccept(c);
    	  }
          i--;
        }
    	return changed;
    }
    
    @Override
    public Boolean visit(IRNode node) {
      Operator operator = tree.getOperator(node);
      if (operator instanceof IllegalCode) {
        LOG.severe("Canonicalizer didn't handle illegal operator: " + operator);
      }
      return doAcceptForChildren_rev(node);
    }

    protected boolean generateBoxUnbox(IRNode node) {
      Operator op = tree.getOperator(node);
      if (op instanceof Expression) {
        IJavaType t = binder.getJavaType(node);
        if (isPrimitive(node, t)) { /// XXX: Bug!: this is requested on new nodes, can crash versioned fixed binder
          if (contextIsReference(node)) {
            boxExpression(node);
            return true;
          }
        } else if (couldBeUnboxed(t)) {
          if (contextIsPrimitive(node)) {
            unboxExpression(node);
            return true;
          }
        }
      }
      return false;
    }

    /**
     * @param node
     */
    private void unboxExpression(IRNode node) {
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("Adding unboxing around " + DebugUnparser.toString(node));
      }
      IRNode newUnbox = JavaNode.makeJavaNode(UnboxExpression.prototype);
      replaceSubtree(node, newUnbox);
      UnboxExpression.setOp(newUnbox, node);
    }

    /**
     * Force the given expression to be boxed, regardless of its type.
     * @param node
     */
    private void boxExpression(IRNode node) {
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("Adding boxing around " + DebugUnparser.toString(node));
      }
      IRNode newBox = JavaNode.makeJavaNode(BoxExpression.prototype);
      replaceSubtree(node, newBox);
      BoxExpression.setOp(newBox, node);
    }

    /**
     * Check if it's one of the possible declared types
     */
    private boolean couldBeUnboxed(IJavaType t) {
      if (t instanceof IJavaDeclaredType) {
        IJavaDeclaredType dt = (IJavaDeclaredType) t;
        //System.out.println(dt.getName());
        return JavaTypeFactory.hasCorrespondingPrimType(dt);
      }
      return false;
    }

    protected boolean generateToString(IRNode node, IJavaType type) {
      if (type == tEnv.getStringType()) {
        return false;
      }
      /*
      if (type instanceof IJavaPrimitiveType) {
        generateConversionToString(node, (IJavaPrimitiveType) type);
        return;
      }
      */
      // Constructed this way to preserve the location of 'node'
      // BUG! We need to avoid calling toSTring if receiver is null (dynamically).
      LOG.fine("Adding toString around " + DebugUnparser.toString(node));
      IRNode newArgs = Arguments.createNode(noNodes);
      JavaNode.setImplicit(newArgs);
      IRNode mc = JavaNode.makeJavaNode(NonPolymorphicMethodCall.prototype);
      replaceSubtree(node, mc);
      finishToString(mc, node, newArgs);
      return true;
    }
    
    private void generateConversionToString(IRNode node, IJavaPrimitiveType type) {
      String typeName = type.getCorrespondingTypeName();
      if (typeName == null) {
        throw new IllegalArgumentException("No type for "+type);
      }
      // Constructed this way to preserve the location of 'node'
      IRNode nt = NamedType.createNode(typeName);
      IRNode te = TypeExpression.createNode(nt);
      JavaNode.setImplicit(te);
      IRNode mc = JavaNode.makeJavaNode(NonPolymorphicMethodCall.prototype);
      replaceSubtree(node, mc);
      
      IRNode args = Arguments.createNode(new IRNode[] { node });
      finishToString(mc, te, args);
    }

    private void finishToString(IRNode mc, IRNode base, IRNode args) {
      NonPolymorphicMethodCall.setObject(mc, base);
      JJNode.setInfo(mc, "toString");
      NonPolymorphicMethodCall.setArgs(mc, args);
    }

    private boolean isStatic(IRNode context, IRNode bodyDecl) {
        if (bodyDecl == null) {
        	throw new IllegalArgumentException("No enclosing body decl: "+context);
        }
        Operator op = JJNode.tree.getOperator(bodyDecl);
        if (MethodDeclaration.prototype.includes(op)) {
           	return JavaNode.getModifier(bodyDecl, JavaNode.STATIC);
        }
        else if (ConstructorDeclaration.prototype.includes(op)) {
        	return false;
        }
        else if (ClassInitializer.prototype.includes(op)) {
        	return JavaNode.getModifier(bodyDecl, JavaNode.STATIC);
        }
        else if (FieldDeclaration.prototype.includes(op)) {
        	return JavaNode.getModifier(bodyDecl, JavaNode.STATIC);
        }
        else if (VariableDeclarator.prototype.includes(op)) {
        	return JavaNode.getModifier(VariableDeclarator.getMods(bodyDecl), 
        			                    JavaNode.STATIC);        	
        }
        else if (AnonClassExpression.prototype.includes(op)) {
        	return false;
        }
        else if (EnumConstantDeclaration.prototype.includes(op)) {
        	return true;
        }
        throw new IllegalArgumentException("Unexpected body decl: "+context);
    }
    
    /**
     * Create a ThisExpression or QualifiedThisExpression so that its
     * type is the type given (or a subtype)
     * @param context location in tree where expression will live
     * @param type required type of this expression
     * @param 
     * @return new node for ThisExpression or QualifiedThisExpression
     *         or TypeExpression (if static)
     */
    protected IRNode createThisExpression(IRNode context, IJavaDeclaredType type, IRNode to) {
      boolean isStatic = to == null ? isStatic(context, VisitUtil.getEnclosingClassBodyDecl(context)) : 
    	                              isStatic(to, to);            
      if (isStatic) {
    	IRNode nt = CogenUtil.createNamedType(type.getDeclaration());
      	return TypeExpression.createNode(nt);
      }
      IRNode thisClass = VisitUtil.getEnclosingType(context);
      IJavaType thisClassType = JavaTypeFactory.getMyThisType(thisClass);
      if (tEnv.isRawSubType(thisClassType,type)) {
        return ThisExpression.prototype.createNode();
      }
      IRNode qThis = thisClass;
      for (;;) {
        qThis = VisitUtil.getEnclosingType(qThis);
        if (qThis == null) {
          LOG.severe("Cannot find compatible This for " + type);
          return null;
        }
        IJavaType qThisType = JavaTypeFactory.getMyThisType(qThis);
        if (tEnv.isRawSubType(qThisType,type)) {
          IRNode nt = CogenUtil.createNamedType(qThis);
          return QualifiedThisExpression.createNode(nt);
        }
      }
    }

    protected boolean addImplicitOuterSpecifier(IRNode call) {
      IRNode p = tree.getParent(call);
      IRNode gp = tree.getParent(p);
      Operator pop = tree.getOperator(p);
      Operator gpop = tree.getOperator(gp);
      if (pop instanceof OuterObjectSpecifier
          || gpop instanceof OuterObjectSpecifier) {
        return false; // nothing to do.
      }
      // if this call is a newly constructed node, this may crash.
      //XXX: Right now I don't see any of these created in the code
      //XXX (especially since adding OOS is commented out for constructor calls.)
      IBinding binding = binder.getIBinding(call);
      if (binding == null) {
        //TODO: handle this case, rather than give up.
        //XXX: Doing so requires knowing exactly how this node is created,
        // so we can avoid asking for types of new nodes.
        LOG.warning("Cannot handle adding OOS to " + DebugUnparser.toString(call));
        return false;
      }
      IJavaDeclaredType calleeThis = binding.getContextType();
      if (!(calleeThis instanceof IJavaNestedType)) return false; // nothing to do
      if (JavaNode.getModifier(calleeThis.getDeclaration(), JavaNode.STATIC)) {
        return false;
      }
      IJavaNestedType calleeNested = ((IJavaNestedType)calleeThis);
      IRNode thisNode = createThisExpression(call,calleeNested.getOuterType(), null); // FIX
       if (pop instanceof AnonClassExpression) {
        call = p;
      }
      // first replace it and then set the nodes (so we don't lose our place in the tree!)
      // XXX: have OOS been moved around when AnonClassExpression was rearranged?
      IRNode newOOS = JavaNode.makeJavaNode(OuterObjectSpecifier.prototype);
      replaceSubtree(call,newOOS);
      OuterObjectSpecifier.setCall(newOOS,call);
      OuterObjectSpecifier.setObject(newOOS,thisNode);
      return true;
    }

    protected IRNode getImplicitSource(IRNode from, IRNode to) {
      IRNode thisExpr = createThisExpression(from,JavaTypeFactory.getThisType(to), to);
      if (thisExpr == null) {
        createThisExpression(from,JavaTypeFactory.getThisType(to), to);
      } else {
        JavaNode.setImplicit(thisExpr);
      }
      copySrcRef(from, thisExpr);
      return thisExpr;
    }

    /**
     * Convert a name AST into an Expression AST
     * 
     * @param name
     *          a simple or qualified name AST node
     * @return an expression node
     */
    protected IRNode nameToExpr(IRNode name) {
      IRNode rv = null;
      try {
    	  IRNode decl = binder.getBinding(name);
    	  Operator op = tree.getOperator(decl);
    	  if (op instanceof TypeDeclInterface && !(op instanceof EnumConstantClassDeclaration)) {
    		  return rv = TypeExpression.createNode(nameToType(name));
    	  }
    	  String string = VariableDeclaration.getId(decl);    
    	  if (op instanceof EnumConstantDeclaration) {
    		  IRNode implicitSource = getImplicitSource(name, decl);
    		  return rv = FieldRef.createNode(implicitSource, string);
    	  }  
    	  if (tree.getOperator(name) instanceof SimpleName) {    	
    		  IRNode parent = tree.getParent(decl);
    		  IRNode gp     = tree.getParent(parent);
    		  if (tree.getOperator(gp) instanceof FieldDeclaration) {
    			  /*
          if ("oldException".equals(string)) {
        	  System.out.println("Found ?.oldException");
          }
    			   */
    			  IRNode implicitSource = getImplicitSource(name, decl);
    			  return rv = FieldRef.createNode(implicitSource, string);
    		  } else {
    			  return rv = VariableUseExpression.createNode(string);
    		  }
    	  } else {
    		  IRNode source = nameToExpr(QualifiedName.getBase(name));
    		  return rv = FieldRef.createNode(source, string);
    	  }
      } finally {
    	  if (rv != null) {
    		  copySrcRef(name, rv);
    	  }
      }
    }

    /**
     * Convert a name AST into a Type AST.
     * @param nameNode
     *          a simple or qualified name AST node
     * @return a type AST node
     */
    protected IRNode nameToType(IRNode nameNode) {
      IBinding b = binder.getIBinding(nameNode);
      if (b == null) {
        LOG.severe("Found no binding for " + DebugUnparser.toString(nameNode));
        return NamedType.createNode(DebugUnparser.toString(nameNode));
      }
      IRNode namedType = createNamedType(b);
      copySrcRef(namedType, nameNode);
      return namedType;
    }

    /**
     * Derived from CogenUtil.createNamedType();
     */
    private IRNode createNamedType(final IBinding b) {
    	final IRNode tdecl = b.getNode();    	
    	Operator op        = JJNode.tree.getOperator(tdecl);
    	IRNode result      = null;
    	try {
    		if (op instanceof AnonClassExpression) {
    			// Use base type
    			IRNode base = AnonClassExpression.getType(tdecl);
    			if (ParameterizedType.prototype.includes(base)) {
    				base = ParameterizedType.getBase(base);
    			}
    			// FIX no bindings for subnodes
    			return result = JJNode.copyTree(base);
    		}
    		String name = JJNode.getInfo(tdecl);
    		if (op instanceof NestedTypeDeclInterface ||
    				op instanceof NestedEnumDeclaration) {
    			// Check if a local class
    			IRNode enclosing = VisitUtil.getEnclosingClassBodyDecl(tdecl);
    			if (enclosing != null && 
    				(SomeFunctionDeclaration.prototype.includes(enclosing) || ClassInitializer.prototype.includes(enclosing))) {
    				//System.out.println("Converting type within a function");
    				return result = NamedType.createNode(name); 
    			}
    			IRNode enclosingT   = VisitUtil.getEnclosingType(tdecl);
    			IBinding enclosingB = makeEnclosingBinding(b, enclosingT); 
    			return result = TypeRef.createNode(createNamedType(enclosingB), name);    		
    		}
    		if (TypeUtil.isOuter(tdecl)) {
    			String qname = TypeUtil.getQualifiedName(tdecl);
    			qname = CommonStrings.intern(qname);
    			return result = NamedType.createNode(qname);
    		}
    		name = CommonStrings.intern(name);
    		return result = NamedType.createNode(name);
    	} finally {
    		if (result != null) {
    			addBinding(result, b);
    		}
    	}
    }
    
    /**
     * @param b The IBinding for the nested type
     */
    private IBinding makeEnclosingBinding(IBinding b, IRNode enclosingT) {
    	IJavaDeclaredType dt    = b.getContextType();    	
    	/*
    	if (dt == null) {
    		System.out.println("null context type");
    	}
    	*/
    	IJavaDeclaredType outer = dt == null ? null : dt.getOuterType();
    	return IBinding.Util.makeBinding(enclosingT, outer, tEnv);
	}

	@Override
    public Boolean visitExpression(IRNode node) {
      IRNode p        = tree.getParent(node);
      boolean changed = false;
      if (!ExprStatement.prototype.includes(p)) {
        changed |= generateBoxUnbox(node);
      }
      changed |= super.visitExpression(node);
      return changed;
    }

    @Override
    public Boolean visitAddExpression(IRNode node) {
      LOG.finer("visiting add: " + node + ": " + DebugUnparser.toString(node));
      if (binder.getJavaType(node) == tEnv.getStringType()) {
        IRNode op1 = AddExpression.getOp1(node);
        IRNode op2 = AddExpression.getOp2(node);
        tree.removeSubtree(op1);
        tree.removeSubtree(op2);
        IRNode scnode = StringConcat.createNode(op1, op2);
        replaceSubtree(node, scnode);
        visitStringConcat(scnode); 
        return true;
      } else {
        return super.visitAddExpression(node);
      }
    }

    @Override
    public Boolean visitAnonClassExpression(IRNode node) {
    	// Reordered to process names before modifying 'extends' or 'implements'
    	return doAcceptForChildren_rev(node);      
    }
    
    @Override
    public Boolean visitArguments(IRNode node) {
		final int numArgs = tree.numChildren(node);
    	final IRNode lastArg = numArgs > 0 ? tree.getChild(node, numArgs-1) : null;
    	boolean changed = super.visitArguments(node);
    	
    	// Check for var args
    	IRNode call   = tree.getParent(node);
    	IBinding b    = binder.getIBinding(call);
    	if (b == null) {
    		return false;
    	}
    	IRNode params = SomeFunctionDeclaration.getParams(b.getNode());
    	int numParams = tree.numChildren(params);
    	if (numParams == 0) {
    		// No varargs
    		return changed; 
    	}
    	IRLocation last = tree.lastChildLocation(params);    	
    	IRNode lastP  = tree.getChild(params, last);
    	if (VarArgsType.prototype.includes(ParameterDeclaration.getType(lastP))) {
    		//System.out.println("Var binding: "+DebugUnparser.toString(b.getNode()));
    		
    		// Reorganize arguments
    		final IRNode[] newArgs = new IRNode[numParams];
    		final int numLastParam = numParams - 1;
    		List<IRNode> varArgs;
    		if (numArgs == numLastParam) {
    			varArgs = Collections.emptyList();
    		} else {
    			if (numArgs == numParams) {
    				// Check if using an array for the var args parameter
    				IJavaType paramType = binder.getJavaType(lastP);
    				IJavaType argType   = binder.getJavaType(lastArg);    			
    				if (tEnv.isCallCompatible(paramType, argType)) {
    					// The last arg matches the var arg type, so no need to do anything
    					return changed;
    				}
    			}    			
    			varArgs = new ArrayList<IRNode>(numArgs - numLastParam);
    		}
    		
    		int i = 0;
    		for(IRNode arg : tree.children(node)) {    			
    			if (i < numLastParam) {
    				newArgs[i] = arg;
    			} else {
    				varArgs.add(arg);
    			}
    			i++;
    		}
    		tree.removeChildren(node);    		
    		newArgs[numLastParam] = VarArgsExpression.createNode(varArgs.toArray(new IRNode[varArgs.size()]));
    		replaceSubtree(node, Arguments.createNode(newArgs));
    		changed = true;
    	}
    	return changed;
    }
    
    @Override
    public Boolean visitAssignExpression(IRNode node) {
      boolean changed = generateBoxUnbox(node);
      // we must visit the RHS first or else the box-unbox code for the RHS will see
      // the changed LHS, and crash.
      changed |= doAccept(AssignExpression.getOp2(node));
      changed |= doAccept(AssignExpression.getOp1(node));
      return changed;
    }

    @Override
    public Boolean visitClassDeclaration(IRNode node) {
      // do default traversal (possibly finishing the constructor)
      boolean changed = super.visitClassDeclaration(node);
      // check to see if there is a constructor declaration, if not,
      // add a public parameterless one.
      //XXX: the parser really needs to do this: otherwise, 
      // when the binder is called on the uncanonicalized code, it will fail to find it.
      boolean found = false;
      IRNode body = ClassDeclaration.getBody(node);
      for (Iterator<IRNode> it = tree.children(body); it.hasNext();) {
        IRNode element = it.next();
        if (tree.getOperator(element) instanceof ConstructorDeclaration) {
          found = true;
          break;
        }
      }
      if (!found) {
        String name = JJNode.getInfo(node);
        IRNode supercall = CogenUtil.makeDefaultSuperCall();
        IRNode empty = MethodBody.createNode(BlockStatement.createNode(new IRNode[]{supercall}));
        IRNode cdNode = CogenUtil.makeConstructorDecl(noNodes, JavaNode.PUBLIC, none,
            name, none, none, empty);
        ReceiverDeclaration.getReceiverNode(cdNode); 
        ReturnValueDeclaration.getReturnNode(cdNode);
        tree.insertChild(body, cdNode);
        changed = true;
      }
      return changed;
    }

    @Override
    public Boolean visitConstructorCall(IRNode node) {
      return super.visitConstructorCall(node);
      //addImplicitOuterSpecifier(node);
    }

    @Override
    public Boolean visitConstructorDeclaration(IRNode node) {      
      // check to see that first instruction is a ConstructorCall
      /* It's a little complicated because:
       * 1) we have to look in the BlockStatement inside the MethodBody
       * 2) we have to look inside the first ExprStatement
       * 3) the constructor call may be inside an OuterObjectSpecifier
       * On the other hand, if we generate a constructor call, we can leave
       * the inference of an OOS to visitConstructorCall.
       */
      IRNode mb = ConstructorDeclaration.getBody(node);
      if (tree.getOperator(mb) instanceof MethodBody) {
        IRNode bs = MethodBody.getBlock(mb);
        boolean found = false;
        if (tree.hasChildren(bs)) {
          IRNode first = tree.getChild(bs, 0);
          if (tree.getOperator(first) instanceof ExprStatement) {
            IRNode expr = ExprStatement.getExpr(first);
            if (tree.getOperator(expr) instanceof OuterObjectSpecifier) {
              expr = OuterObjectSpecifier.getCall(expr);
            }
            if (tree.getOperator(expr) instanceof ConstructorCall) {
              found = true;
            }
          }
        }
        // after determining *whether* we need to add something,
        // we do changes inside the constructor
        boolean changed = super.visitConstructorDeclaration(node);
        // and only then perform the change.
        if (!found) {
          IRNode type = VisitUtil.getEnclosingType(node);
          String name = JavaNames.getQualifiedTypeName(type);
          if ("java.lang.Object".equals(name)) {
        	  return changed;
          }
          // actually this will be rare because the parser stick in implicit
          // calls to super().
          LOG.fine("Canon: adding default super call");
          IRNode sc = CogenUtil.makeDefaultSuperCall();
          //IRNode es = ExprStatement.createNode(sc);
          tree.insertChild(bs, sc);
          changed = true;
        }
        return changed;
      } else {
        return super.visitConstructorDeclaration(node);
      }
    }

    @Override 
    public Boolean visitForEachStatement(IRNode stmt) {    	    	
        IRNode expr = ForEachStatement.getCollection(stmt);
        IJavaType t = binder.getJavaType(expr);        
        IRNode result;
        if (t instanceof IJavaArrayType) {
        	result = createArrayLoopFromForEach(stmt, t);        
        } else { // Assume to be Iterable
        	result = createIterableLoopFromForEach(stmt, (IJavaDeclaredType) t);
        }
        replaceSubtree(stmt, result);
        return true;
    }
    
    private IRNode makeDecl(int mods, String name, IRNode initE, IJavaType t) {
        IRNode type = CogenUtil.createType(binder.getTypeEnvironment(), t);
        IRNode rv   = makeDecl(mods, name, initE, type);
        return rv;
      }
    
    private IRNode makeDecl(int mods, String name, IRNode expr, IRNode type) { 
        IRNode init = Initialization.createNode(expr);
        IRNode vd   = VariableDeclarator.createNode(name, 0, init);
        IRNode vars = VariableDeclarators.createNode(new IRNode[] { vd });
        IRNode rv   = DeclStatement.createNode(Annotations.createNode(noNodes), mods, type, vars);
        return rv;
    }
    
    private IRNode adaptParamDeclToDeclStatement(IRNode pdecl, IRNode init) {
    	int mods    = ParameterDeclaration.getMods(pdecl); 
    	String name = ParameterDeclaration.getId(pdecl);
    	IRNode type = ParameterDeclaration.getType(pdecl);
    	tree.removeSubtree(type);
    	// FIX destroy pdecl 
    	return makeDecl(mods, name, init, type);
    }
    
    private IRNode createArrayLoopFromForEach(IRNode stmt, final IJavaType collT) {
        //System.out.println("Translating array loop: "+stmt.toString());
    	// handle children    	
        doAcceptForChildren(stmt);
        
        final String unparse = DebugUnparser.toString(stmt);
        
        // Create decl for array
        final String array = "array"+unparse.hashCode();
        IRNode collection  = ForEachStatement.getCollection(stmt);
    	tree.removeSubtree(collection);
        IRNode arrayDecl   = makeDecl(JavaNode.FINAL, array, collection, collT);
        
        // Create decl for counter
        final String i   = "i"+unparse.hashCode();
        IRNode iDecl     = makeDecl(JavaNode.ALL_FALSE, i, IntLiteral.createNode("0"), IntType.prototype.jjtCreate());
        
        // Create condition for while loop
        IRNode arrayLen  = ArrayLength.createNode(VariableUseExpression.createNode(array));
        IRNode cond      = LessThanExpression.createNode(VariableUseExpression.createNode(i), arrayLen);    
        
        // Create initializer for parameter
        IRNode paramInit = ArrayRefExpression.createNode(VariableUseExpression.createNode(array), 
                                                         VariableUseExpression.createNode(i));
        
        IRNode whileLoop = makeEquivWhileLoop(stmt, cond, paramInit);    
        IRNode result    = BlockStatement.createNode(new IRNode[] { arrayDecl, iDecl, whileLoop });
        return result;
      }
      
      private IRNode makeEquivWhileLoop(IRNode stmt, IRNode cond, IRNode paramInit) {
          // Combine parameter and original body into the new body of the while loop
          IRNode paramDecl = adaptParamDeclToDeclStatement(ForEachStatement.getVar(stmt), paramInit);    
          copySrcRef(stmt, paramDecl);
          
          IRNode origBody  = ForEachStatement.getLoop(stmt);
          tree.removeSubtree(origBody);
          IRNode body      = BlockStatement.createNode(new IRNode[] { paramDecl, origBody });          
          IRNode whileLoop = edu.cmu.cs.fluid.java.operator.WhileStatement.createNode(cond, body);
          copySrcRef(stmt, whileLoop);
          return whileLoop;
      }

      private IRNode makeSimpleCall(final String object, String method) {
    	  return makeSimpleCall(VariableUseExpression.createNode(object), method);
      }
      
      private IRNode makeSimpleCall(IRNode object, String method) {
        IRNode args = Arguments.createNode(noNodes);
        IRNode rv   = NonPolymorphicMethodCall.createNode(object, method, args);
        return rv;
      }

      private abstract class MethodBinding implements IBinding {
    	  IJavaDeclaredType recType;
    	  IJavaTypeSubstitution subst;
    	  public MethodBinding(IJavaDeclaredType t) {
			recType = t;
    	  }
    	  public IJavaReferenceType getReceiverType() {
    		  return recType;
    	  }
    	  public void updateRecType(IJavaDeclaredType t) {
    		  // FIX check t;
    		  recType = t;
    	  }
    	  public IJavaType convertType(IJavaType ty) {
    		  if (subst == null) {
    			  IJavaDeclaredType ct = getContextType();
    			  if (ct != null) {
    				  subst = JavaTypeSubstitution.create(getTypeEnvironment(), ct);
    			  }
    		  }
    		  if (subst != null) {
    			  return ty.subst(subst);    			  
    		  }
    		  return null;
    	  }
      }
      
      private MethodBinding findNoArgMethod(final IJavaDeclaredType type, final String name) {
        if (type == null) {
          return null;
        }       
        IRNode tdecl = type.getDeclaration();
        for (final IRNode m : VisitUtil.getClassMethods(tdecl)) {
          final String id = SomeFunctionDeclaration.getId(m);
          //System.out.println("Looking at "+id);
          if (name.equals(id)) {
        	int numParams;
        	if (ConstructorDeclaration.prototype.includes(m)) {
        		numParams = JJNode.tree.numChildren(ConstructorDeclaration.getParams(m));
        	} else {
        		numParams = JJNode.tree.numChildren(MethodDeclaration.getParams(m));
        	}
        	if (numParams == 0) {
        		return new MethodBinding(type) {
					public IJavaDeclaredType getContextType() {
						return type;
					}
					public IRNode getNode() {
						return m;
					}
					public ITypeEnvironment getTypeEnvironment() {
						return binder.getTypeEnvironment();
					}        			
        		};
        	}
          }
        }
        for(IJavaType stype : type.getSupertypes(binder.getTypeEnvironment())) {
          MethodBinding mb = findNoArgMethod((IJavaDeclaredType) stype, name);
          if (mb != null) {
        	mb.updateRecType(type);
        	return mb;
          }
        }
        return null;
      }

      private IRNode createIterableLoopFromForEach(IRNode stmt, final IJavaDeclaredType collT) {
        //System.out.println("Translating iterable loop: "+stmt.toString());
    	final String unparse = DebugUnparser.toString(stmt);
    	  
    	// Do any analysis before handling children    	
        IBinding mb         = findNoArgMethod(collT, "iterator");
        if (mb == null) {
          findNoArgMethod(collT, "iterator");
          LOG.severe("Unable to find iterator() on "+collT);
          return null;
        }
        IRNode rtype        = MethodDeclaration.getReturnType(mb.getNode());
        IJavaType rtypeT    = binder.getJavaType(rtype);
        IJavaDeclaredType itTB = (IJavaDeclaredType) mb.convertType(rtypeT);        
        
      	// handle children    	
    	doAcceptForChildren(stmt);
          
        // Create decl for Iterable
    	//final String iterable  = "iterable"+unparse.hashCode();
        IRNode collection      = ForEachStatement.getCollection(stmt);
     	tree.removeSubtree(collection);
     	
        //IRNode iterableDecl    = makeDecl(JavaNode.FINAL, iterable, collection, collT);
        //copySrcRef(stmt, iterableDecl);
        
        // Create decl for iterator
        final String it     = "it"+unparse.hashCode();
        final IRNode itType = //CogenUtil.createType(binder.getTypeEnvironment(), itTB);
        	// Iterator<?>
        	ParameterizedType.createNode(NamedType.createNode("java.util.Iterator"), 
        			TypeActuals.createNode(new IRNode[] { WildcardType.prototype.jjtCreate() }));
        
        IRNode itCall       = makeSimpleCall(collection, "iterator");//makeSimpleCall(iterable, "iterator");
        copySrcRef(stmt, itCall);
        
        IRNode itDecl       = makeDecl(JavaNode.FINAL, it, itCall, itType);
        copySrcRef(stmt, itDecl);
        
        // Create condition for while loop
        IRNode cond      = makeSimpleCall(it, "hasNext"); 
        copySrcRef(stmt, cond);
        
        // Create initializer for parameter
        IRNode paramInit = makeSimpleCall(it, "next");     
        copySrcRef(stmt, paramInit);

        // Introduce cast to the real type        
        IRNode castType = CogenUtil.createType(binder.getTypeEnvironment(), computeIteratorType(binder.getTypeEnvironment(), itTB));
        paramInit = CastExpression.createNode(castType, paramInit);
        
        IRNode whileLoop = makeEquivWhileLoop(stmt, cond, paramInit); 
        IRNode result    = BlockStatement.createNode(new IRNode[] { /*iterableDecl,*/ itDecl, whileLoop });
        return result;
      }

	private IJavaType computeIteratorType(ITypeEnvironment te, IJavaDeclaredType type) {
		if (type.getName().startsWith("java.util.Iterator")) {
			return type.getTypeParameters().get(0);
		}
		for(IJavaType st : type.getSupertypes(te)) {
			IJavaType value = computeIteratorType(te, (IJavaDeclaredType) st);
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	@Override
    public Boolean visitImplicitReceiver(IRNode node) {
      IRNode parent = tree.getParent(node);
      IRNode method = binder.getBinding(parent);
      replaceSubtree(node, getImplicitSource(node, method));
      return true;
    }

    @Override
    public Boolean visitMethodDeclaration(IRNode node) {
      return super.visitMethodDeclaration(node);
    }
    
    @Override
    public Boolean visitNameExpression(IRNode node) {
      generateBoxUnbox(node); // generate boxing as needed
      IRNode name = NameExpression.getName(node);
      if (binder.getIBinding(node).getNode() == null) return false;
      IRNode replacement = nameToExpr(name);
      replaceSubtree(node, replacement);
      return true;
    }

    @Override
    public Boolean visitNameType(IRNode node) {
      replaceSubtree(node, nameToType(NameType.getName(node)));
      return true;
    }

    @Override
    public Boolean visitNewExpression(IRNode node) {
      IRNode old      = NewExpression.getType(node);
      boolean changed = doAcceptForChildren_rev(node);
      map(old, NewExpression.getType(node));
      changed |= addImplicitOuterSpecifier(node);
      return changed;
    }

    @Override
    public Boolean visitNonPolymorphicConstructorCall(IRNode node) {
      boolean changed = visitConstructorCall(node);
      // TODO perhaps replace with polyumorphic one
      // must do after the previous call because if there
      // are polymorphic parameters, we need to replace the node.
      return changed;
    }

    @Override
    public Boolean visitNonPolymorphicMethodCall(IRNode node) {
      boolean changed = generateBoxUnbox(node);
      // TODO perhaps replace with polymorphic one
      
      /* FIX need to process children in reverse order?
      changed |= doAccept(AssignExpression.getOp2(node));
      changed |= doAccept(AssignExpression.getOp1(node));
      */
      changed |= super.visitNonPolymorphicMethodCall(node);
      return changed;
    }

    @Override
    public Boolean visitNonPolymorphicNewExpression(IRNode node) {
      boolean changed = visitNewExpression(node);
      // TODO check to see if there are polymorphic implicit arguments.
      return changed;
    }

    @Override
    public Boolean visitStringConcat(IRNode node) {
      IRNode op1   = StringConcat.getOp1(node);
      IJavaType t1 = binder.getJavaType(op1);
      IRNode op2   = StringConcat.getOp2(node);
      IJavaType t2 = binder.getJavaType(op2);

      // This *may* generate boxing
      LOG.finer("visiting first operand of SC: " + tree.getOperator(op1));
      boolean changed = doAccept(op1); 
      LOG.finer("visiting second operand of SC: " + tree.getOperator(op2));
      changed |= doAccept(op2);      
      
      // Need to be reloaded, since they might have been boxed, and may be otherwise changed
      IRNode newOp1   = StringConcat.getOp1(node);
      IRNode newOp2   = StringConcat.getOp2(node);
      
      if (t1 instanceof IJavaPrimitiveType && !(tree.getOperator(newOp1) instanceof BoxExpression)) {
        boxExpression(newOp1);
        newOp1  = StringConcat.getOp1(node);
        changed = true;
      }
      if (t2 instanceof IJavaPrimitiveType && !(tree.getOperator(newOp2) instanceof BoxExpression)) {
        boxExpression(newOp2);
        newOp2  = StringConcat.getOp1(node);
        changed = true;
      }
            
      changed |= generateToString(newOp1, t1);     
      changed |= generateToString(newOp2, t2);
      return changed;
    }
    
    @Override
    public Boolean visitTypeDeclaration(IRNode node) {
      // Reordered to process names before modifying 'extends' or 'implements'
  	  return doAcceptForChildren_rev(node);
    }
  }
  
  // helper methods
  protected boolean isPrimitive(IRNode n) {
    return isPrimitive(n, binder.getJavaType(n));
  }
  
  protected boolean isPrimitive(IRNode n, IJavaType t) {
    try {
      return t instanceof IJavaPrimitiveType;
    } catch (RuntimeException e) {
      LOG.severe("Cannot tell primitive: " + DebugUnparser.toString(n));
      LOG.severe("Operator = " + JJNode.tree.getOperator(n));
      throw e;
    }
  }

  protected boolean isReference(IRNode n) {
    return binder.getJavaType(n) instanceof IJavaReferenceType;
  }

  protected synchronized boolean contextIsPrimitive(IRNode n) {
    contextVisitor.loc = tree.getLocation(n);
    return contextVisitor.doAccept(tree.getParent(n)) == PRIMITIVE_CONTEXT;
  }

  protected synchronized boolean contextIsReference(IRNode n) {
    contextVisitor.loc = tree.getLocation(n);
    return contextVisitor.doAccept(tree.getParent(n)) == REFERENCE_CONTEXT;
  }

  static final int REFERENCE_CONTEXT = 1;

  static final int PRIMITIVE_CONTEXT = -1;

  static final int ANY_CONTEXT = 0;

  class ContextVisitor extends Visitor<Integer> {
    IRLocation loc;

    // get the context value from this node's type
    private int contextFromNode(IRNode node) {
      return isPrimitive(node) ? PRIMITIVE_CONTEXT : REFERENCE_CONTEXT;
    }

    @Override
    public Integer visit(IRNode node) {
      LOG.warning("ContextVisitor didn't classify node with operator "
          + tree.getOperator(node));
      return ANY_CONTEXT;
    }

    @Override
    public Integer visitAddExpression(IRNode node) {
      if (binder.getJavaType(node) instanceof IJavaPrimitiveType) {
        return PRIMITIVE_CONTEXT;
      } else {
        return REFERENCE_CONTEXT;
      }
    }

    @Override
    public Integer visitAnonClassExpression(IRNode node) {
      return REFERENCE_CONTEXT;
    }

    @Override
    public Integer visitArguments(IRNode node) {
      IRNode call = tree.getParent(node);
      IRNode callee = binder.getBinding(call);
      if (callee == null) return ANY_CONTEXT;
      IRNode formals = SomeFunctionDeclaration.getParams(callee);
      final int i    = tree.childLocationIndex(node, loc);
      final int n    = tree.numChildren(formals);
      final int last = n-1; 
      if (i < last) {
          IRNode formal = tree.getChild(formals, i);
          return contextFromNode(formal);
      } 
      // Could be varargs
      IRNode formal = tree.getChild(formals, last);
      IRNode fType  = ParameterDeclaration.getType(formal);
      boolean vargs = VarArgsType.prototype.includes(fType);     
      if (vargs) {    	  
    	  return contextFromNode(VarArgsType.getBase(fType));
      }
      // Not varargs, so do the same as above
      return contextFromNode(formal);
    }

    @Override
    public Integer visitArrayCreationExpression(IRNode node) {
      return ANY_CONTEXT;
    }

    @Override
    public Integer visitArrayInitializer(IRNode node) {
      IRNode p = tree.getParent(node);
      Operator pop = tree.getOperator(p);
      IJavaType atype;
      if (pop instanceof ArrayCreationExpression) {
        atype = binder.getJavaType(p);
      } else if (pop instanceof Initialization) {
        atype = binder.getJavaType(tree.getParent(p));
      } else if (pop instanceof ArrayInitializer) {
    	atype = binder.getJavaType(p);
      } else {
        throw new FluidError("ArrayInitializer inside a " + pop);
      }
      if (atype instanceof IJavaArrayType) {
        IJavaType etype = ((IJavaArrayType) atype).getElementType();
        return etype instanceof IJavaPrimitiveType ? PRIMITIVE_CONTEXT
            : REFERENCE_CONTEXT;
      } else {
        LOG.warning("Array initializer used in non-array context: "
            + DebugUnparser.toString(p));
        return ANY_CONTEXT;
      }
    }

    @Override
    public Integer visitArrayLength(IRNode node) {
      return REFERENCE_CONTEXT;
    }

    @Override
    public Integer visitArrayRefExpression(IRNode node) {
      if (loc == ArrayRefExpression.arrayLocation)
        return REFERENCE_CONTEXT;
      else if (loc == ArrayRefExpression.indexLocation) 
        return PRIMITIVE_CONTEXT;
      else {
        LOG.severe("ArrayRef has a weird location: " + loc);
        return ANY_CONTEXT;
      }
    }

    @Override
    public Integer visitAssignExpression(IRNode node) {
      if (loc.equals(AssignExpression.op1Location)) return ANY_CONTEXT;
      return contextFromNode(AssignExpression.getOp1(node));
    }

    @Override
    public Integer visitBinopExpression(IRNode node) {
      return PRIMITIVE_CONTEXT; // unless otherwise indicated
    }

    @Override
    public Integer visitBoxExpression(IRNode node) {
      return PRIMITIVE_CONTEXT;
    }

    @Override
    public Integer visitCastExpression(IRNode node) {
      return contextFromNode(node);
    }

    @Override
    public Integer visitClassExpression(IRNode node) {
      return REFERENCE_CONTEXT;
    }

    @Override
    public Integer visitConditionalExpression(IRNode node) {
      if (binder.getJavaType(node) instanceof IJavaPrimitiveType) {
        return PRIMITIVE_CONTEXT;
      } else {
        return REFERENCE_CONTEXT;
      }
    }
    
    @Override
    public Integer visitConstantLabel(IRNode node) {
      return ANY_CONTEXT;
    }
    
    @Override
    public Integer visitConstructorCall(IRNode node) {
      return REFERENCE_CONTEXT;
    }

    @Override
    public Integer visitDefaultValue(IRNode node) {
    	return doAccept(DefaultValue.getValue(node));
    }
    
    @Override
    public Integer visitDimExprs(IRNode node) {
      return PRIMITIVE_CONTEXT;
    }

    @Override
    public Integer visitDoStatement(IRNode node) {
      return PRIMITIVE_CONTEXT;
    }

    @Override
    public Integer visitElementValuePair(IRNode node) {
      return PRIMITIVE_CONTEXT;
    }

    @Override 
    public Integer visitStatementExpressionList(IRNode node) {
      return ANY_CONTEXT;
    }
    
    @Override
    public Integer visitEqualityExpression(IRNode node) {
      return ANY_CONTEXT;
    }

    @Override
    public Integer visitFieldRef(IRNode node) {
      return REFERENCE_CONTEXT;
    }

    @Override
    public Integer visitForEachStatement(IRNode node) {
      return REFERENCE_CONTEXT;
    }

    @Override
    public Integer visitForStatement(IRNode node) {
      if (loc.equals(ForStatement.condLocation)) {
        return PRIMITIVE_CONTEXT;
      }
      return ANY_CONTEXT;
    }

    @Override
    public Integer visitIfStatement(IRNode node) {
      return PRIMITIVE_CONTEXT;
    }

    @Override
    public Integer visitInitialization(IRNode node) {
      return contextFromNode(tree.getParent(node));
    }

    @Override
    public Integer visitInstanceOfExpression(IRNode node) {
      return REFERENCE_CONTEXT;
    }

    @Override
    public Integer visitMethodCall(IRNode node) {
      return REFERENCE_CONTEXT;
    }

    @Override
    public Integer visitOpAssignExpression(IRNode node) {
      // TODO: Can't really handle this:
      // What about
      // { Integer x; ... x += 34; }
      // We put an UnboxExpression around the LHS which is strange.
      if (binder.getJavaType(node) == tEnv.getStringType()) {
        return REFERENCE_CONTEXT;
      } else {
        return PRIMITIVE_CONTEXT;
      }
    }

    @Override
    public Integer visitOuterObjectSpecifier(IRNode node) {
      return REFERENCE_CONTEXT;
    }

    @Override
    public Integer visitParenExpression(IRNode node) {
      return ANY_CONTEXT;
    }

    @Override
    public Integer visitPrimLiteral(IRNode node) {
    	return PRIMITIVE_CONTEXT;
    }
    
    @Override
    public Integer visitReturnStatement(IRNode node) {
      IRNode rdecl = binder.getBinding(node);
      IRNode rtype = ReturnValueDeclaration.getType(rdecl);
      Operator top = tree.getOperator(rtype);
      return top instanceof PrimitiveType ? PRIMITIVE_CONTEXT : ANY_CONTEXT;
    }

    @Override
    public Integer visitStatement(IRNode node) {
      return ANY_CONTEXT;
    }

    @Override
    public Integer visitStringConcat(IRNode node) {
      return REFERENCE_CONTEXT;
    }

    @Override
    public Integer visitUnboxExpression(IRNode node) {
      return REFERENCE_CONTEXT;
    }

    @Override
    public Integer visitUnopExpression(IRNode node) {
      return PRIMITIVE_CONTEXT;
    }

    @Override
    public Integer visitVarArgsExpression(IRNode node) {
      return REFERENCE_CONTEXT;
    }

    @Override
    public Integer visitWhileStatement(IRNode node) {
      return PRIMITIVE_CONTEXT;
    }
  }

  private final ContextVisitor contextVisitor = new ContextVisitor();

}
