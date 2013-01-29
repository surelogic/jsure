/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/JavaPromise.java,v 1.57
 * 2003/11/18 22:22:39 dfsuther Exp $
 */
package edu.cmu.cs.fluid.java;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;

import com.surelogic.tree.SyntaxTreeNode;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.promise.IPromiseStorage.TokenInfo;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.tree.SyntaxTreeInterface;
import edu.cmu.cs.fluid.unparse.*;
import edu.cmu.cs.fluid.util.*;

/**
 * Class to hold methods working with promises on top of Java. The package
 * edu.cmu.cs.fluid.java.promises holds additional operators used.
 * 
 * Summary of promise representation
 */
public class JavaPromise extends JavaNode {
	private static final long serialVersionUID = 1L;

// For debugging
	//private StackTraceElement[] trace = new Throwable().getStackTrace();
  //private Version created = Version.getVersion();

  protected JavaPromise(SyntaxTreeInterface tree, Operator op) {
		super(tree, op);
	}
	protected JavaPromise(
		SyntaxTreeInterface tree,
		Operator op,
		IRNode[] children) {
		super(tree, op, children);
	}

	public static JavaNode makeJavaPromise(Operator op) {
    if (JJNode.specializeForSyntaxTree) {
      return new SyntaxTreeNode(op);    
    }
		return new JavaPromise(tree, op);
	}

	public static JavaNode makeJavaPromise(Operator op, IRNode[] children) {
    if (JJNode.specializeForSyntaxTree) {
      return new SyntaxTreeNode(op, children);    
    }
		return new JavaPromise(tree, op, children);
	}

	public static JavaNode makeJavaPromise(SyntaxTreeInterface tree, Operator op) {
    if (JJNode.specializeForSyntaxTree && JJNode.tree == tree) {
      return new SyntaxTreeNode(op);    
    }
		return new JavaPromise(tree, op);
	}

	public static JavaNode makeJavaPromise(SyntaxTreeInterface tree, Operator op, IRNode[] children) {
    if (JJNode.specializeForSyntaxTree && JJNode.tree == tree) {
      return new SyntaxTreeNode(op, children);    
    }
	  return new JavaPromise(tree, op, children);
	}

	/*
  public static final String UNIQUE = "Unique";
  public static final String UNSHARED = "Unshared";
  public static final String IMMUTABLE = "Immutable";
  public static final String BORROWED = "Borrowed";
  public static final String SYNCHRONIZED = "Synchronized";
  public static final String SELF_PROTECTED = "Self-protected";
  public static final String EXHAUSTIVE_THROWS = "Exhaustive throws";
  public static final String FIELD_REGION = "Region mapping for fields";
  public static final String FIELD_AGGREGATION =
    "Region aggregation for fields";
  public static final String PRECONDITION = "Pre-conditions";
  public static final String POSTCONDITION = "Post-conditions";
  public static final String THROW_CONDITION = "Throws condition";
  public static final String CLASS_INVARIANT = "Class invariants";
  public static final String EFFECTS = "Declared effects";
  public static final String CLASS_REGION = "Declared regions";
  public static final String LOCKS = "Declared locks";
  public static final String POLICY_LOCKS = "Declared policy locks";
  public static final String RETURN_LOCK = "Lock as return value";
  public static final String LOCK_PARAM = "Lock as parameter";
  public static final String REQUIRED_LOCKS = "Required locks";
  public static final String COLOR_DECL = "Color";
  public static final String COLOR_GRANT = "Grant";
  public static final String COLOR_REVOKE = "Revoke";
  public static final String COLOR_NOTE = "Note";
  public static final String COLOR_REQUIRE = "Require";
  public static final String COLOR_INCOMPATIBLE = "Colors Incompatible";
  public static final String COLOR_NOTRELEVANT = "Color Not Relevant";

  public static final String[] promiseLabels =
    {
      UNIQUE,
      UNSHARED,
      IMMUTABLE,
      BORROWED,
      SYNCHRONIZED,
      SELF_PROTECTED,
      EXHAUSTIVE_THROWS,
      FIELD_REGION,
      FIELD_AGGREGATION,
      PRECONDITION,
      POSTCONDITION,
      THROW_CONDITION,
      CLASS_INVARIANT,
      EFFECTS,
      CLASS_REGION,
      LOCKS,
      POLICY_LOCKS,
      RETURN_LOCK,
      LOCK_PARAM,
      COLOR_DECL,
      COLOR_GRANT,
      COLOR_REVOKE,
      COLOR_NOTE,
      COLOR_REQUIRE,
      COLOR_INCOMPATIBLE,
      COLOR_NOTRELEVANT,
      };
	 */

	/*****************************************************************************
	 * Boolean SlotInfos for promises
	 ****************************************************************************/

	private static SlotInfo<Boolean> getBooleanVSI(String name) {
		SlotInfo<Boolean> si = getVersionedSlotInfo(name, IRBooleanType.prototype, Boolean.FALSE);
		// Causes bad initializer ordering
		// getBundle().saveAttribute(si); 
		return si;
	}
	/*
  private static boolean isX(SlotInfo si, IRNode n) {
    return ((Boolean) n.getSlotValue(si)).booleanValue();
  }
	 */

	/* parameter or return value is a unique reference. */

	/*
  public static final SlotInfo isUniqueSlotInfo =
    getBooleanVSI("JavaPromise.isUnique");

  /* field is unshared. */

	/*
  public static final SlotInfo isUnsharedSlotInfo =
    getBooleanVSI("JavaPromise.isUnshared");

  /* field, parameter, return value or class is immutable */
//	public static final SlotInfo isImmutableSlotInfo =
//	getBooleanVSI("JavaPromise.isImmutable");

//	/* parameter will not be stored. */
//	public static final SlotInfo isBorrowedSlotInfo =
//	getBooleanVSI("JavaPromise.isBorrowed");

//	public static final SlotInfo isSynchronizedSlotInfo =
//	getBooleanVSI("JavaPromise.isSynchronized");

//	public static final SlotInfo isThreadSafeSlotInfo =
//	getBooleanVSI("JavaPromise.isThreadSafe");

//	/* whether the throw list for a method is exhuastive. */

//	public static final SlotInfo hasExhaustiveThrowsSlotInfo =
//	getBooleanVSI("JavaPromise.hasExhaustiveThrows");

//	public static boolean hasExhaustiveThrows(IRNode node) {
//	return ((Boolean) node.getSlotValue(hasExhaustiveThrowsSlotInfo))
//	.booleanValue();
//	}
//	public static void setHasExhaustiveThrows(
//	IRNode node,
//	boolean exhaustiveThrows) {
//	Boolean b = exhaustiveThrows ? Boolean.TRUE : Boolean.FALSE;
//	node.setSlotValue(hasExhaustiveThrowsSlotInfo, b);
//	}

//	public static final SlotInfo transparentSlotInfo =
//	getBooleanVSI("JavaPromise.transparent");

	/* true for write, false for read effects */

	static final SlotInfo<Boolean> isWriteSlotInfo = getBooleanVSI("JavaPromise.isWrite");

	public static boolean isWrite(IRNode node) {
		return (node.getSlotValue(isWriteSlotInfo)).booleanValue();
	}
	public static boolean getIsWrite(IRNode node) { // needed by create-operator
		return isWrite(node);
	}

	public static void setIsWrite(IRNode node, boolean write) {
		Boolean b = write ? Boolean.TRUE : Boolean.FALSE;
		node.setSlotValue(isWriteSlotInfo, b);
	}

	private static final Token writeToken = new Keyword("modifies");
	private static final Token readToken = new Keyword("reads");

	public static void unparseIsWrite(IRNode node, JavaUnparser u) {
		(isWrite(node) ? writeToken : readToken).emit(u, node);
	}

	/*****************************************************************************
	 * IRNode SlotInfos for promises
	 ****************************************************************************/

	private static IRNode getXorNull(SlotInfo<IRNode> si, IRNode n) {
		if (n == null) {
			return null;
		}
		/* Functional, but slower by not optimizing for the common case
		if (n.valueExists(si)) {
			return n.getSlotValue(si);
		}
		return null;
        */
		try {
			return n.getSlotValue(si);
		} catch (SlotUndefinedException e) {
			return null;
		}		
 	}

	/* A back reference to the node the promise node is for. */

	static final SlotInfo<IRNode> promisedForSlotInfo =
		JavaNode.getVersionedSlotInfo(
			"JavaPromise.promisedFor",
			IRNodeType.prototype,
			null);
	/**
	 * Return the node for which this promise applies. (Something like the parent
	 * node).
	 */
	public static IRNode getPromisedFor(IRNode promise) {
		return promise.getSlotValue(promisedForSlotInfo);
	}

	public static IRNode getPromisedForOrNull(IRNode promise) {
		return getXorNull(promisedForSlotInfo, promise);
	}

	private static void setPromisedFor(IRNode promise, IRNode decl) {
		promise.setSlotValue(promisedForSlotInfo, decl);
	}

	public static void attachPromiseNode(IRNode node, IRNode promise) {
		attachPromiseNode(node, promise, true);
	}

	public static void attachPromiseNode(IRNode node, IRNode promise, boolean checkOp) {
		if (promise == null)
			return;

		if (checkOp) {
			Operator op = tree.getOperator(promise);
			if (!(op instanceof JavaPromiseOpInterface)) {
				LOG.warning("Promise node does not have a promise operator: "+op.name());
			}    
		}
		JJNode.tree.removeSubtree(promise); // ensure a null parent
		setPromisedFor(promise, node);
	}
	public static void detachPromiseNode(IRNode node, IRNode promise) {
		if (promise != null)
			setPromisedFor(promise, null);
	}

	/* a special node to which to attach promises about the return value. */

	private static final SlotInfo<IRNode> returnNodeSlotInfo =
		getConstantSlotInfo("JavaPromise.returnNode", IRNodeType.prototype);

	public static IRNode getReturnNode(IRNode methodNode)
	throws SlotUndefinedException {
		return methodNode.getSlotValue(returnNodeSlotInfo);
	}
	public static IRNode getReturnNodeOrNull(IRNode methodNode) {
		return getXorNull(returnNodeSlotInfo, methodNode);
	}
	public static void setReturnNode(IRNode methodNode, IRNode returnNode)
	throws SlotImmutableException {
	    /*
		if (IRRegion.hasOwner(methodNode)) {
			System.out.println("Making return node on "+methodNode);
		}
        */
		methodNode.setSlotValue(returnNodeSlotInfo, returnNode);
		attachPromiseNode(methodNode, returnNode);
	}

	/* a special node to which to attach promises about the receiver. */

	private static final SlotInfo<IRNode> receiverNodeSlotInfo =
		getConstantSlotInfo("JavaPromise.receiverNode", IRNodeType.prototype);

	public static IRNode getReceiverNode(IRNode methodNode)
	throws SlotUndefinedException {
//	  if (InitDeclaration.prototype.includes(methodNode)) {
//	    throw new IllegalArgumentException("No receiver on an init method");
//	  }
		try {
			return methodNode.getSlotValue(receiverNodeSlotInfo);
		} catch (SlotUndefinedException e) {
			//System.err.println("Trying to get receiver for
			// "+DebugUnparser.toString(methodNode));
			throw e;
		}
	}
	public static IRNode getReceiverNodeOrNull(IRNode methodNode) {
		return getXorNull(receiverNodeSlotInfo, methodNode);
	}
	public static void setReceiverNode(IRNode methodNode, IRNode receiverNode)
	throws SlotImmutableException {
		methodNode.setSlotValue(receiverNodeSlotInfo, receiverNode);

		attachPromiseNode(methodNode, receiverNode);
	}

	/* a special node to which to attach promises about qualified receivers. */

	private static final SlotInfo<IRNode> qualifiedReceiverNodeSlotInfo =
		getConstantSlotInfo("JavaPromise.qualifiedReceiverNode", IRNodeType.prototype);

	/* XXX: Edwin says that this should be favored over getQualifiedReceiverNodeOrNull
	 * if you are getting the type node by binding a QualifiedThisExpression or 
	 * a QualifiedThisExpressionNode that has methodNode as an ancestor.  
	 * 
	 * He says to use getQualifiedReceiverNodeOrNull when you are conjuring up
	 * types, such as from an XML file.
	 */
	public static IRNode getQualifiedReceiverNodeByName(IRNode methodNode, IRNode typeDecl) {
		String typeName = JavaNames.getFullTypeName_local(typeDecl);
		return getQualifiedReceiverNodeByName(methodNode, typeName);
	}

	/**
	 * Work around for bug 1392: &lt;init&gt; method nodes do not behave the same as 
	 * regular method nodes.  On a regular method from class C, you can ask for
	 * the qualified receiver for class C, and the noraml ReceiverDeclaration
	 * node will be correctly returned.  But this doesn't work for &lt;init&gt;
	 * method nodes.  This needs to be fixed by updating the construction and 
	 * initialization of &lt;init&gt; nodes.
	 * 
	 * <p>In the meantime, here is a work around.  It sucks because it requires a 
	 * traversal up the tree to find the enclosing type of the method declaration.
	 * It avoids the problem by not invoking {@link #getQualifiedReceiverNodeByName(IRNode, IRNode)} 
	 * to get the regular receiver.
	 * 
	 * @param methodNode
	 * @param type
	 * @return
	 */
	public static IRNode getQualifiedReceiverNodeByName_Bug1392WorkAround(
			final IRNode methodNode, final IRNode typeDecl) {
		String typeName = JavaNames.getFullTypeName_local(typeDecl);
		return getQualifiedReceiverNodeByName_Bug1392WorkAround(methodNode, typeName);
	}	
	
	public static IRNode getQualifiedReceiverNodeByName_Bug1392WorkAround(
			final IRNode methodNode, final String typeName) {
		IRNode enclosingT        = VisitUtil.getEnclosingType(methodNode);
		String enclosingTypeName = JavaNames.getFullTypeName_local(enclosingT);
		if (typeName.equals(enclosingTypeName)) {
			return JavaPromise.getReceiverNodeOrNull(methodNode);
		} else {
			return JavaPromise.getQualifiedReceiverNodeByName(methodNode, typeName);
		}
	}
	
	// 1. We're looking at a constructor -> check for the IPQR, then the below
	// 2. We're looking at a method -> find the "field", starting from the enclosing class
	// 3. We're looking at a type -> find the "field", starting from this class
	public static IRNode getQualifiedReceiverNodeByName(IRNode declNode, String typeName) {
		if (declNode == null || typeName == null) {
			return null;
		}

		final Operator op = tree.getOperator(declNode);
		IRNode type;
		if (!TypeDeclaration.prototype.includes(op)) { 
			type = VisitUtil.getEnclosingType(declNode);
		} else {
			type = declNode;
		}
		/*	
		if (!TypeDeclaration.prototype.includes(declNode)) { 
		  // decl is inside of a type
		  IRNode enclosingT        = VisitUtil.getEnclosingType(declNode);
		  String enclosingTypeName = JavaNames.getFullTypeName(enclosingT);
		  if (typeName.equals(enclosingTypeName)) {
		    return getReceiverNode(declNode);
		  }
		}
		 */
		// Check if it's actually a normal receiver node
		String name = JavaNames.getFullTypeName_local(type);
		if (typeName.equals(name)) {
			if (type != declNode) {
				return getReceiverNode(declNode);
			} else {
				throw new IllegalStateException("No receiver node to get from "+JavaNames.getFullTypeName(type));
			}
		}
		
		// typeName may contain : for local classes, due to getFullTypeName()
		if (typeName.indexOf(':') >= 0) {
			//System.out.println("Got "+typeName);
			typeName = typeName.replace(':', '.');
		}		
		
		// Check for match with IFQR
		else if (ConstructorDeclaration.prototype.includes(op)) {
			IRNode qr = lookForQualifiedReceiver(declNode, typeName);
			if (qr != null) {
				return qr;
			}
			// Skip the qr for this type, since the constructor's hides that one
			type = VisitUtil.getEnclosingType(type);
		}
		
		// Compare names with each qualified receiver
		while (type != null) {
			IRNode qr = lookForQualifiedReceiver(type, typeName);
			if (qr != null) {
				return qr;
			}
			type = VisitUtil.getEnclosingType(type);
		}
		return null;
	}    

	private static IRNode lookForQualifiedReceiver(IRNode decl, String typeNameToMatch) {
		IRNode qr = getQualifiedReceiverNodeOrNull(decl);
		if (qr == null) {
			return null;
		}
		IRNode base = QualifiedReceiverDeclaration.getBase(qr);
		String name = JavaNames.unparseType(base);
		// A hack to match . to $   			
		//if (Pattern.matches(name, qname)) {
		if (name.equals(typeNameToMatch)) {
			return qr;
		}
		return null;
	}
	
	/**
	 * Find all the qualified receiver nodes available from this node
	 */
	public static Iteratable<IRNode> getQualifiedReceiverNodes(final IRNode declNode) {
		return getQualifiedReceiverNodes(declNode, tree.getOperator(declNode));
	}
	
	private static Iteratable<IRNode> getQualifiedReceiverNodes(final IRNode declNode, Operator op) {
		IRNode initial = null;
		final IRNode startType;
		if (ConstructorDeclaration.prototype.includes(op)) {
			initial = getQualifiedReceiverNodeOrNull(declNode);

			// Skip the qr for this type, since the constructor's hides that one
			IRNode thisType = VisitUtil.getEnclosingType(declNode);
			startType = VisitUtil.getEnclosingType(thisType);
		} else {
			startType = TypeDeclaration.prototype.includes(op) ? declNode : VisitUtil.getEnclosingType(declNode);
		}
		return new SimpleRemovelessIterator<IRNode>(initial) {
			IRNode type = startType;
			
			@Override
			protected Object computeNext() {
				while (type != null) {
					IRNode qr = getQualifiedReceiverNodeOrNull(type);
					type = VisitUtil.getEnclosingType(type);
					if (qr != null) {
						return qr;
					}
				}
				return IteratorUtil.noElement;
			}			
		};
	}	
	
	public static IRNode getQualifiedReceiverNodeOrNull(IRNode declNode) {
		if (declNode == null) {
			return null;
		}
		if (declNode.valueExists(qualifiedReceiverNodeSlotInfo)) {
			return declNode.getSlotValue(qualifiedReceiverNodeSlotInfo);
		}
		return null;
	}

	/**
	 * Doesn't check to see if it really is an enclosing type
	 */
	public static void setQualifiedReceiverNode(IRNode declNode, IRNode qDecl) {
		if (declNode == null || qDecl == null) {
			return;
		}

		declNode.setSlotValue(qualifiedReceiverNodeSlotInfo, qDecl);
		attachPromiseNode(declNode, qDecl);
	}

	/*
	 * a special node to which to attach promises about the instance initializer
	 * for a class.
	 */

	private static final SlotInfo<IRNode> initMethodSlotInfo =
		getConstantSlotInfo("JavaPromise.initMethod", IRNodeType.prototype);

	public static IRNode getInitMethod(IRNode classNode)
	throws SlotUndefinedException {
		return classNode.getSlotValue(initMethodSlotInfo);
	}
	public static IRNode getInitMethodOrNull(IRNode classNode) {
		return getXorNull(initMethodSlotInfo, classNode);
	}
	public static void setInitMethod(IRNode classNode, IRNode initMethod)
	throws SlotImmutableException {
		classNode.setSlotValue(initMethodSlotInfo, initMethod);
		attachPromiseNode(classNode, initMethod);
	}

	/*
	 * a special node to which to attach promises about the class initializer for
	 * a class.
	 */

	private static final SlotInfo<IRNode> classInitMethodSlotInfo =
		getConstantSlotInfo("JavaPromise.classInitMethod", IRNodeType.prototype);

	public static IRNode getClassInitMethod(IRNode classNode)
	throws SlotUndefinedException {
		return classNode.getSlotValue(classInitMethodSlotInfo);
	}
	public static IRNode getClassInitOrNull(IRNode classNode) {
		return getXorNull(classInitMethodSlotInfo, classNode);
	}
	public static void setClassInitMethod(
		IRNode classNode,
		IRNode classInitMethod)
	throws SlotImmutableException {
		if (classNode == null) {
			return;
		}
		classNode.setSlotValue(classInitMethodSlotInfo, classInitMethod);
		attachPromiseNode(classNode, classInitMethod);
	}

	/* Method pre- and post- conditions */

//	static final SlotInfo preconditionSlotInfo =
//	getVersionedSlotInfo(
//	"JavaPromise.precondition",
//	IRNodeType.prototype,
//	null);

//	public static IRNode getPrecondition(IRNode methodNode) {
//	return (IRNode) methodNode.getSlotValue(preconditionSlotInfo);
//	}

//	public static IRNode getPreconditionOrNull(IRNode methodNode) {
//	return getXorNull(preconditionSlotInfo, methodNode);
//	}

//	public static void setPrecondition(IRNode methodNode, IRNode condition) {
//	detachPromiseNode(methodNode, getPrecondition(methodNode));
//	methodNode.setSlotValue(preconditionSlotInfo, condition);
//	attachPromiseNode(methodNode, condition);
//	}

//	static final SlotInfo postconditionSlotInfo =
//	getVersionedSlotInfo(
//	"JavaPromise.postcondition",
//	IRNodeType.prototype,
//	null);

//	public static IRNode getPostcondition(IRNode methodNode) {
//	return (IRNode) methodNode.getSlotValue(postconditionSlotInfo);
//	}

//	public static IRNode getPostconditionOrNull(IRNode methodNode) {
//	return getXorNull(postconditionSlotInfo, methodNode);
//	}

//	public static void setPostcondition(IRNode methodNode, IRNode condition) {
//	detachPromiseNode(methodNode, getPostcondition(methodNode));
//	methodNode.setSlotValue(postconditionSlotInfo, condition);
//	attachPromiseNode(methodNode, condition);
//	}

//	/* Throw conditions (necessary condition to throw an exception) */

//	static final SlotInfo throwConditionSlotInfo =
//	getVersionedSlotInfo(
//	"JavaPromise.throwCondition",
//	IRNodeType.prototype,
//	null);

//	public static IRNode getThrowCondition(IRNode typeNode) {
//	return (IRNode) typeNode.getSlotValue(throwConditionSlotInfo);
//	}

//	public static IRNode getThrowConditionOrNull(IRNode typeNode) {
//	return getXorNull(throwConditionSlotInfo, typeNode);
//	}

//	public static void setThrowCondition(IRNode typeNode, IRNode condition) {
//	detachPromiseNode(typeNode, getThrowCondition(typeNode));
//	typeNode.setSlotValue(throwConditionSlotInfo, condition);
//	attachPromiseNode(typeNode, condition);
//	}

//	static final SlotInfo fieldRegionSlotInfo =
//	getVersionedSlotInfo("JavaPromise.fieldRegion", IRNodeType.prototype);

//	static final SlotInfo fieldAggregationSlotInfo =
//	getVersionedSlotInfo("JavaPromise.fieldAggregation", IRNodeType.prototype);

//	/*****************************************************************************
//	* IRSequence(IRNode) SlotInfos for promises
//	****************************************************************************/

//	private static Enumeration getEnum(SlotInfo si, IRNode n) {
//	if (n.valueExists(si)) {
//	IRSequence s = (IRSequence) n.getSlotValue(si);
//	return s.elements();
//	}
//	return EmptyEnumeration.prototype;
//	}

//	private static void addToEnum(SlotInfo si, IRNode n, IRNode elt) {
//	IRSequence s;
//	if (n.valueExists(si)) {
//	s = (IRSequence) n.getSlotValue(si);
//	} else {
//	s = new IRList(treeSlotFactory);
//	n.setSlotValue(si, s);
//	}
//	s.appendElement(elt);
//	attachPromiseNode(n, elt);
//	treeChanged.noteChange(n);
//	}

//	private static boolean removeFromEnum(SlotInfo si, IRNode n, IRNode elt) {
//	if (!n.valueExists(si)) {
//	return false;
//	}
//	final IRSequence s = (IRSequence) n.getSlotValue(si);
//	try {
//	IRLocation loc = s.firstLocation();
//	while (true) {
//	if (s.elementAt(loc).equals(elt)) {
//	s.removeElementAt(loc);
//	detachPromiseNode(n, elt);
//	treeChanged.noteChange(n);
//	return true;
//	}
//	loc = s.nextLocation(loc);
//	}
//	} catch (IRSequenceException e) {
//	return false;
//	}
//	}

	/* Class regions */

//	static final SlotInfo classRegionsSlotInfo =
//	getConstantSlotInfo(
//	"JavaPromise.classRegions",
//	new IRSequenceType(IRNodeType.prototype));

//	/* Color Declarations */
//	static final SlotInfo colorDeclSlotInfo =
//	getConstantSlotInfo(
//	"JavaPromise.colorDecl",
//	new IRSequenceType(IRNodeType.prototype));

//	/* Color Revokes */
//	static final SlotInfo colorRevokeSlotInfo =
//	getConstantSlotInfo(
//	"JavaPromise.colorRevoke",
//	new IRSequenceType(IRNodeType.prototype));

//	/* Color Grants */
//	static final SlotInfo colorGrantSlotInfo =
//	getConstantSlotInfo(
//	"JavaPromise.colorGrant",
//	new IRSequenceType(IRNodeType.prototype));

//	/* Color Notes */
//	static final SlotInfo colorNoteSlotInfo =
//	getConstantSlotInfo(
//	"JavaPromise.colorNote",
//	new IRSequenceType(IRNodeType.prototype));

//	/* Color Requires */
//	static final SlotInfo colorRequireSlotInfo =
//	getConstantSlotInfo(
//	"JavaPromise.colorRequire",
//	new IRSequenceType(IRNodeType.prototype));

//	static final SlotInfo colorIncompatibleSlotInfo =
//	getConstantSlotInfo(
//	"JavaPromise.colorIncompatible",
//	new IRSequenceType(IRNodeType.prototype));

//	/* Class invariants */

//	static final SlotInfo classInvariantsSlotInfo =
//	getConstantSlotInfo(
//	"JavaPromise.classInvariants",
//	new IRSequenceType(IRNodeType.prototype));

//	public static Enumeration classInvariants(IRNode classNode) {
//	return getEnum(classInvariantsSlotInfo, classNode);
//	}

	/**
	 * Add a invariant declaration node to the list of invariants for this class
	 * declaration node. It does not check to see this invariant declaration node
	 * is already in the list.
	 */
//	public static void addClassInvariant(
//	IRNode classNode,
//	IRNode invariantNode) {
//	addToEnum(classInvariantsSlotInfo, classNode, invariantNode);
//	}

//	/**
//	* Remove a invariant declaration node from the list of invariants for this
//	* class declaration node. It returns true if the invariant node was found
//	* (and removed).
//	*/
//	public static boolean removeClassInvariant(
//	IRNode classNode,
//	IRNode invariantNode) {
//	return removeFromEnum(classInvariantsSlotInfo, classNode, invariantNode);
//	}

//	/*
//	* Method effects.
//	* 
//	* NOTE this is different from the other sequences
//	* 
//	* The slot stores a sequence. If that sequence is empty, then nothing is
//	* promised for the method. Otherwise, the first element is null and the rest
//	* of the elements are effects nodes.
//	*/
//	static final SlotInfo methodEffectsSlotInfo =
//	getVersionedSlotInfo(
//	"JavaPromise.methodEffects",
//	new IRSequenceType(IRNodeType.prototype),
//	EmptyIRSequence.prototype);

//	/* Lock declarations */

//	static final SlotInfo lockDeclarationsSlotInfo =
//	getConstantSlotInfo(
//	"JavaPromise.lockDeclarations",
//	new IRSequenceType(IRNodeType.prototype));

//	/* Lock declarations */

//	static final SlotInfo policyLockDeclarationsSlotInfo =
//	getConstantSlotInfo(
//	"JavaPromise.PolicyLockDeclarations",
//	new IRSequenceType(IRNodeType.prototype));

//	static final SlotInfo requiredLocksSlotInfo =
//	getConstantSlotInfo(
//	"JavaPromise.requiredLocks",
//	new IRSequenceType(IRNodeType.prototype));

//	static final SlotInfo lockReturnSlotInfo =
//	getVersionedSlotInfo("JavaPromise.lockReturn", IRNodeType.prototype);

//	static final SlotInfo lockParamSlotInfo =
//	getVersionedSlotInfo("JavaPromise.lockParam", IRNodeType.prototype);

	/*****************************************************************************
	 * Stuff to operate over the SlotInfos
	 * 
	 * promiseBooleanInfo and promiseChildrenInfo are all of them (others are
	 * subsets)
	 ****************************************************************************/

	/**
	 * All promise slots which are Boolean-valued
	 */
//	static SlotInfo[] promiseBooleanInfo =
//	{
//	isUniqueSlotInfo,
//	isUnsharedSlotInfo,
//	isImmutableSlotInfo,
//	isBorrowedSlotInfo,
//	isSynchronizedSlotInfo,
//	isThreadSafeSlotInfo,
//	hasExhaustiveThrowsSlotInfo,
//	isWriteSlotInfo,
//	transparentSlotInfo,
//	};

//	/** subset */
//	static SlotInfo[] promiseIRNodeInfo = {
//	// These are handled specially
//	// returnNodeSlotInfo,
//	// receiverNodeSlotInfo,
//	initMethodSlotInfo,
//	classInitMethodSlotInfo,
//	preconditionSlotInfo,
//	postconditionSlotInfo,
//	throwConditionSlotInfo,
//	fieldRegionSlotInfo,
//	fieldAggregationSlotInfo,
//	lockReturnSlotInfo,
//	lockParamSlotInfo,
//	};

//	/** subset */
//	static SlotInfo[] promiseIRSequenceInfo =
//	{
//	classRegionsSlotInfo,
//	classInvariantsSlotInfo,
//	methodEffectsSlotInfo,
//	lockDeclarationsSlotInfo,
//	policyLockDeclarationsSlotInfo,
//	requiredLocksSlotInfo,
//	colorDeclSlotInfo,
//	colorGrantSlotInfo,
//	colorRevokeSlotInfo,
//	colorNoteSlotInfo,
//	colorRequireSlotInfo,
//	colorIncompatibleSlotInfo,
//	};

	/**
	 * All promise slots which involve nodes or sequences of nodes.
	 */
	static SlotInfo[] promiseChildrenInfo =
	{
//    promisedForSlotInfo,
	returnNodeSlotInfo,
	receiverNodeSlotInfo,
	qualifiedReceiverNodeSlotInfo,
	initMethodSlotInfo,
	classInitMethodSlotInfo,
/*
	preconditionSlotInfo,
	postconditionSlotInfo,
	throwConditionSlotInfo,
	fieldRegionSlotInfo,
	fieldAggregationSlotInfo,
	lockReturnSlotInfo,
	lockParamSlotInfo,
	// these below are sequences
	classRegionsSlotInfo,
	classInvariantsSlotInfo,
	methodEffectsSlotInfo,
	lockDeclarationsSlotInfo,
	policyLockDeclarationsSlotInfo,
	requiredLocksSlotInfo,
	colorDeclSlotInfo,
	colorGrantSlotInfo,
	colorRevokeSlotInfo,
	colorNoteSlotInfo,
	colorRequireSlotInfo,
	colorIncompatibleSlotInfo,
*/
	};

	public static SlotInfo[] getPromiseChildrenInfos() {
		return promiseChildrenInfo; // TODO
	}

	public static void saveAttributes(Bundle b) {
		b.saveAttribute(promisedForSlotInfo);
		b.saveAttribute(isWriteSlotInfo);
		
		for(SlotInfo si : promiseChildrenInfo) {
			b.saveAttribute(si);
		}
		/*
    for (int i = 0; i < promiseBooleanInfo.length; i++) {
      b.saveAttribute(promiseBooleanInfo[i]);
    }
    for (int i = 0; i < promiseChildrenInfo.length; i++) {
      b.saveAttribute(promiseChildrenInfo[i]);
    }
		 */
	}

	/*
	 * public static void main(String args[]) { Bundle b = new Bundle();
	 * FileLocator floc = IRPersistent.fluidFileLocator; saveAttributes(b); try {
	 * b.store(floc); } catch (IOException ex) {
	 * System.out.println(ex.toString()); System.out.println("Please press return
	 * to try again"); try { System.in.read(); b.store(floc); } catch
	 * (IOException ex2) { System.out.println(ex2.toString()); } } }
	 */

	private static Bundle promisebundle = null;

	public static Bundle getBundle() {
		if (promisebundle == null) {
			UniqueID id = UniqueID.parseUniqueID("javapromise");
			try {
				promisebundle =
					Bundle.loadBundle(
						id,
						IRPersistent.fluidFileLocator);
			} catch (Throwable t) {
				JavaGlobals.JAVA.fine(t.toString());

				promisebundle = Bundle.findBundle(id);
				saveAttributes(promisebundle);
			}
		}
		return promisebundle;
	}

	public static void main(String args[]) {
		JavaNode.main(args);
		Bundle b = getBundle();
		if (b != null)
			b.describe(System.out);
	}

	public static IRNode getParentOrPromisedFor(IRNode node) {
		/*
		IRNode p = getPromisedForOrNull(node);
		if (p == null)
			return tree.getParentOrNull(node);
		else
			return p;
		*/
		IRNode p = tree.getParentOrNull(node);
		if (p == null)
			return getPromisedForOrNull(node);
		else
			return p;
	}

	public static Iteratable<IRNode> promiseChildren(IRNode node) {
		return new JavaPromiseChildrenIterator(node, getPromiseChildrenInfos());
	}

	public static Iteratable<IRNode> promisesBottomUp(IRNode node) {
		return new JavaPromiseDescendantsIterator(node, getPromiseChildrenInfos());
	}

	/**
	 * Return an enumeration of all nodes in the tree or in promises attached to
	 * the tree.
	 */
	public static Iteratable<IRNode> bottomUp(IRNode node) {
		return new JavaPromiseTreeIterator(node, true);
	}

	private static final Token returnToken = new Keyword("returns");
	private static final Token receiverToken = new Keyword("receiver");  
	private static final Token methodEffectsStartToken =
		new Combined(
			new Keyword("effects"),
			new UnitedBP(1, "effects", Glue.UNIT, Glue.INDENT));
	private static final Token methodEffectsSeparatorToken =
		new Combined(
			new Combined(IndepBP.JUXTBP, new Keyword(",")),
			new UnitedBP(2, "effects", Glue.UNIT, Glue.JUXT));
	private static final Token methodEffectsStopToken =
		new UnitedBP(1, "effects", Glue.UNIT, Glue.JUXT);

	/*
  private static final Token uniqueToken = new Keyword("unique");
  private static final Token unsharedToken = new Keyword("unshared");
  private static final Token immutableToken = new Keyword("immutable");
  private static final Token borrowedToken = new Keyword("borrowed");
  private static final Token synchronizedToken = new Keyword("synchronized");
  private static final Token threadSafeToken = new Keyword("threadSafe");
  private static final Token hasExhaustiveThrowsToken =
    new Keyword("hasExhaustiveThrows");

  private static final Token preconditionToken = new Keyword("precondition");
  private static final Token postconditionToken = new Keyword("postcondition");
  private static final Token throwConditionToken = new Keyword("condition");
  private static final Token fieldRegionToken = new Keyword("mapInto");
  private static final Token fieldAggregationToken = new Keyword("aggregate");
  private static final Token classRegionToken = new Keyword("region");
  private static final Token classInvariantToken = new Keyword("invariant");
  private static final Token lockDeclarationToken = new Keyword("lock");
  private static final Token policyLockDeclarationToken =
    new Keyword("policyLock");
  private static final Token lockReturnToken = new Keyword("returnsLock");
  private static final Token lockParamToken = new Keyword("isLock");
  private static final Token requiredLocksToken = new Keyword("requiresLock");
  private static final Token colorDeclToken = new Keyword("color");
  private static final Token colorGrantToken = new Keyword("grant");
  private static final Token colorRevokeToken = new Keyword("revoke");
  private static final Token colorNoteToken = new Keyword("note");
  private static final Token colorRequireToken = new Keyword("requiresColor");
  private static final Token colorIncompatibleToken =
    new Keyword("incompatibleColors");
  private static final Token transparentToken =
    new Keyword("transparent");

  private static class TokenInfo {
    public final String index;
    public final SlotInfo si;
    public final Token token;
    TokenInfo(String i, SlotInfo sinfo, Token t) {
      index = i;
      si = sinfo;
      token = t;
    }
  }

  private static final TokenInfo[] varPromiseTokens =
    {
      new TokenInfo(UNIQUE, isUniqueSlotInfo, uniqueToken),
      new TokenInfo(IMMUTABLE, isImmutableSlotInfo, immutableToken),
      new TokenInfo(BORROWED, isBorrowedSlotInfo, borrowedToken),
      new TokenInfo(
         COLOR_NOTRELEVANT,
         transparentSlotInfo,
         transparentToken),
      };

  private static final TokenInfo[] boolPromiseTokens =
    {
      new TokenInfo(UNSHARED, isUnsharedSlotInfo, unsharedToken),
      new TokenInfo(SYNCHRONIZED, isSynchronizedSlotInfo, synchronizedToken),
      new TokenInfo(
        SELF_PROTECTED,
        isThreadSafeSlotInfo,
        threadSafeToken),
      new TokenInfo(
        EXHAUSTIVE_THROWS,
        hasExhaustiveThrowsSlotInfo,
        hasExhaustiveThrowsToken),
      };

  private static final TokenInfo[] nodePromiseTokens =
    {
      new TokenInfo(PRECONDITION, preconditionSlotInfo, preconditionToken),
      new TokenInfo(POSTCONDITION, postconditionSlotInfo, postconditionToken),
      new TokenInfo(
        THROW_CONDITION,
        throwConditionSlotInfo,
        throwConditionToken),
      new TokenInfo(FIELD_REGION, fieldRegionSlotInfo, fieldRegionToken),
      new TokenInfo(
        FIELD_AGGREGATION,
        fieldAggregationSlotInfo,
        fieldAggregationToken),
      new TokenInfo(RETURN_LOCK, lockReturnSlotInfo, lockReturnToken),
      new TokenInfo(LOCK_PARAM, lockParamSlotInfo, lockParamToken),
      };

  private static final TokenInfo[] seqPromiseTokens =
    {
      new TokenInfo(
        CLASS_INVARIANT,
        classInvariantsSlotInfo,
        classInvariantToken),
      new TokenInfo(CLASS_REGION, classRegionsSlotInfo, classRegionToken),
      new TokenInfo(LOCKS, lockDeclarationsSlotInfo, lockDeclarationToken),
      new TokenInfo(
        POLICY_LOCKS,
        policyLockDeclarationsSlotInfo,
        policyLockDeclarationToken),
      new TokenInfo(REQUIRED_LOCKS, requiredLocksSlotInfo, requiredLocksToken),
      new TokenInfo(COLOR_DECL, colorDeclSlotInfo, colorDeclToken),
      new TokenInfo(COLOR_GRANT, colorGrantSlotInfo, colorGrantToken),
      new TokenInfo(COLOR_REVOKE, colorRevokeSlotInfo, colorRevokeToken),
      new TokenInfo(COLOR_NOTE, colorNoteSlotInfo, colorNoteToken),
      new TokenInfo(COLOR_REQUIRE, colorRequireSlotInfo, colorRequireToken),
      new TokenInfo(
        COLOR_INCOMPATIBLE,
        colorIncompatibleSlotInfo,
        colorIncompatibleToken)};
	 */

	/**
	 * Unparse promises into a fmt stream 
	 */
	public static void unparsePromises(final IRNode node, final JavaUnparser u) {
		unparsePromises(node, u, false, null, false);
	}

	@SuppressWarnings("unchecked")
	public static boolean unparsePromises(final IRNode node, final JavaUnparser u, 
		boolean isSub, Token promiseToken, boolean hasPromises) 
	{
		final boolean debug = LOG.isLoggable(Level.FINE);
		final JavaUnparseStyle style = u.getStyle();
		if (!style.unparsePromises()) {
			return false;
		}
		
		final Operator op = u.getTree().getOperator(node);
		final Iterator<TokenInfo> tokenInfos = EmptyIterator.prototype();
		//PromiseFramework.getInstance().getTokenInfos(op);

		/*
		while (style.unparsePromises() && tokenInfos.hasNext()) {
			final TokenInfo info = tokenInfos.next();
			final IRType type = info.si.getType();

			if (type instanceof IRBooleanType) {				
				if (AbstractPromiseAnnotation.isX_filtered(info.si, node)) { // && style.getUnparsePromiseOption(info.index)) {
					startPromise(node, u, hasPromises, promiseToken, isSub);
					hasPromises = true;
					promiseToken = null;
					info.token.emit(u, node);
					if (debug) {
						LOG.fine("Unparsing tokens for "+info.index); 
					}
				}			
			}
			/*
			else if (type instanceof IRIntegerType) {				
			}
			
			else if (type instanceof IRNodeType) {					  
				IRNode sub = AbstractPromiseAnnotation.getXorNull_filtered(info.si, node);

				if (sub != null) { // && style.getUnparsePromiseOption(info.index)) {
					startPromise(node, u, hasPromises, promiseToken, isSub);
					hasPromises = true;
					promiseToken = null;
					info.token.emit(u, node);
					u.unparse(sub);
					if (debug) {
						LOG.fine("Unparsing tokens for "+info.index); 
					}
				}
			}
			else if (type instanceof IRSequenceType) {
				final Iterator<IRNode> e = AbstractPromiseAnnotation.getEnum_filtered(info.si, node);
				while (e.hasNext()) {
					IRNode elt = e.next();
					if (elt != null) {
						startPromise(node, u, hasPromises, promiseToken, isSub);
						hasPromises = true;
						promiseToken = null;
						info.token.emit(u, node);
						u.unparse(elt);
						if (debug) {
							LOG.fine("Unparsing tokens for "+info.index); 
						}
					}
				}
			}			
			else {
				LOG.warning("Got unexpected type for unparsing: "+type);
			}
		}
		
		// handle enumerated method effects
		Iterator<IRNode> fx = EffectsAnnotation.methodEffects(node);
		if (style.unparsePromiseEffects() && fx != null) {
			startPromise(node, u, hasPromises);
			hasPromises = true;
			methodEffectsStartToken.emit(u, node);

			if (!fx.hasNext()) {
				// nothing
				(new Keyword("@reads nothing")).emit(u, node);
			} else {
				for (;;) {
					IRNode effect = fx.next();
					if (effect != null) {
						u.unparse(effect);
					}
					if (!fx.hasNext()) {
						break;
					}
					methodEffectsSeparatorToken.emit(u, node);
				}
			}
			methodEffectsStopToken.emit(u, node);
		}
        */
		if (!isSub) {
			IRNode subnode;
			if (MethodDeclaration.prototype.includes(op) ||
				ConstructorDeclaration.prototype.includes(op)) {
				subnode = getReceiverNodeOrNull(node);
				if (subnode != null) {
					hasPromises = unparsePromises(subnode, u, true, receiverToken, hasPromises);
				}
			}

			if (MethodDeclaration.prototype.includes(op)) {
				subnode = getReturnNodeOrNull(node);
				if (subnode != null) {
					hasPromises = unparsePromises(subnode, u, true, returnToken, hasPromises);
				}
			}
		}

		/* instance and class inits not looked at (unused) */
		if (!isSub) {
			stopPromises(node, u, hasPromises);
		}
		return hasPromises;
	}

	private static Glue promiseIndent = new Glue(2);
	private static final Token startPromises =
		new UnitedBP(1, "promise", Glue.UNIT, promiseIndent);
	private static final Token startPromise =
		new Combined(
			new UnitedBP(2, "promise", Glue.UNIT, Glue.UNIT),
			new Combined(new Keyword("@"), IndepBP.JUXTBP));
	private static final Token startSubPromise =
		new Combined(
			new UnitedBP(2, "promise", Glue.UNIT, Glue.UNIT),
			new Combined(new Keyword("@@"), IndepBP.JUXTBP));
	private static final Token stopPromises =
		new Combined(
			new UnitedBP(1, "promise", Glue.UNIT, Glue.JUXT),
			IndepBP.DEFAULTBP);

	private static void startPromise(
		IRNode node,
		JavaUnparser u,
		boolean hasPromises,
		Token promiseToken,
		boolean isSub) {
		if (isSub) {
			startSubPromise(node, u, hasPromises, promiseToken);
		} else {
			startPromise(node, u, hasPromises);
		}
	}

	private static void startPromise(
		IRNode node,
		JavaUnparser u,
		boolean hasPromises) {
		if (!hasPromises) {
			startPromises.emit(u, node);
		}
		startPromise.emit(u, node);
	}

	private static void startSubPromise(
		IRNode node,
		JavaUnparser u,
		boolean hasPromises,
		Token promiseToken) {
		if (promiseToken != null) {
			startPromise(node, u, hasPromises);
			promiseToken.emit(u, node);
		}
		startSubPromise.emit(u, node);
	}

	private static void stopPromises(
		IRNode node,
		JavaUnparser u,
		boolean hasPromises) {
		if (hasPromises) {
			stopPromises.emit(u, node);
		}
	}
}

class JavaPromiseDescendantsIterator extends ProcessIterator<IRNode> {
	final SlotInfo[] promiseChildrenInfo;

	JavaPromiseDescendantsIterator(IRNode n, SlotInfo[] pci) {
		super(JJNode.tree.bottomUp(n));
		promiseChildrenInfo = pci;
	}

	@Override
	protected Iterator<IRNode> getNextIter(Object o) {
		return 
		new JavaPromiseChildrenIterator((IRNode) o, promiseChildrenInfo);
	}

	@Override
	protected Object select(Object o) {
		return (o == null) ? notSelected : o;
	}
}

class JavaPromiseChildrenIterator extends AbstractRemovelessIterator<IRNode> {
	final IRNode node;
	final SlotInfo[] promiseChildrenInfo;

	JavaPromiseChildrenIterator(IRNode n, SlotInfo[] pci) {
		node = n;
		promiseChildrenInfo = pci;
	}

	Object next = null;
	boolean nextIsValid = false;
	int info = 0;

	@SuppressWarnings("unchecked")
	private Object getNext() {
		if ((sub != null) && sub.hasNext()) {
			return sub.next();
		} else {
			sub = null;
		}

		while (info < promiseChildrenInfo.length) {
			SlotInfo childSI = promiseChildrenInfo[info++];
			if (node.valueExists(childSI)) {
				Object value = node.getSlotValue(childSI);
				if (value instanceof IRSequence) {
					sub = ((IRSequence) value).elements();
					return getNext();
				}
				if (value != null)
					return value;
			}
		}
		return noNextElement;
	}
	private static Object noNextElement = new Object();

	@Override
  public boolean hasNext() {
		if (nextIsValid) {
			return true;
		}
		next = getNext();
		nextIsValid = (next != noNextElement);
		return nextIsValid;
	}

	Iterator<IRNode> sub = null;

	@Override
  public IRNode next() {
		if (nextIsValid || hasNext()) {
			nextIsValid = false;
			return (IRNode) next;
		}
		throw new NoSuchElementException("no more promise children");
	}
}
