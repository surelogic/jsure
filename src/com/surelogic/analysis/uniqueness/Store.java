/* $Header: /cvs/fluid/fluid/src/com/surelogic/analysis/uniqueness/Store.java,v 1.41 2007/07/10 22:16:37 aarong Exp $ */
package com.surelogic.analysis.uniqueness;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import com.surelogic.annotation.rules.UniquenessRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.PlainIRNode;
import edu.cmu.cs.fluid.version.Era;
import edu.cmu.cs.fluid.version.Version;
import edu.cmu.cs.fluid.version.VersionedRegion;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.*;
import static edu.cmu.cs.fluid.util.IteratorUtil.noElement;
import static edu.cmu.cs.fluid.java.JavaGlobals.noNodes;

/** A class representing a nonstandard store as defined in
 * deferred nullification paper.  A store is represented
 * by a record of five things: <ol>
 * <li> stack depth;
 * <li> set of objects
 * <li> set of pairs (local,object);
 * <li> set of triples (object,unique-field,object).
 * </ul>
 * A local is a source local or a stack position.  A source local
 * is represented by the IRNode for the VariableDeclarator node.
 * A stack position is represented by an instance of Integer.
 * An object is a set of the following: <ul>
 *  <li> IRNode -- local declaration nodes (VarDecl)
 *  <li> Integer -- stack value
 *  <li> PseudoVar -- one of three psuedo variables
 * </ul>
 */
@SuppressWarnings("unchecked")
public class Store extends RecordLattice {
  private final IRNode[] locals;
  private final UnionLattice<Object> nodeSet;

  public static final int STACK_SIZE = 0;
  public static final int OBJECTS = 1;
  public static final int FIELD_STORE = 2;
  public static final int STORE_RECORDS = 3; /* size of tuple */

  public static final int STATE_NULL = 0;
  public static final int STATE_UNIQUE = 1;
  public static final int STATE_SHARED = 2;
  public static final int STATE_BORROWED = 3;
  public static final int STATE_UNDEFINED = 4;

  public static final PseudoVariable undefinedVariable =
       new PseudoVariable("undefined",STATE_UNDEFINED);
  public static final PseudoVariable borrowedVariable =
       new PseudoVariable("borrowed",STATE_BORROWED);
  public static final PseudoVariable sharedVariable =
       new PseudoVariable("shared",STATE_SHARED);


  public static final Vector<FlatLattice> stackSizes = new Vector<FlatLattice>();
  static {
    stackSizes.addElement(new FlatLattice(new Integer(0)));
  }
  public static synchronized FlatLattice getStackSize(int n) {
    int i = stackSizes.size();
    while (i <= n) {
      stackSizes.addElement(new FlatLattice(new Integer(i)));
      ++i;
    }
    return stackSizes.elementAt(n);
  }

  
  public Store(IRNode[] locals) {
    super(new Lattice[]{FlatLattice.prototype,
			new UnionLattice(),
			new UnionLattice()});
    this.locals = locals;
    nodeSet = new UnionLattice();
  }

  protected Store(IRNode[] locals, UnionLattice nodeSet, Lattice[] values,
		  RecordLattice top, RecordLattice bottom) {
    super(values,top,bottom);
    this.locals = locals;
    this.nodeSet = nodeSet;
  }

  @Override
  protected RecordLattice newLattice(Lattice[] newValues) {
    return new Store(locals,nodeSet,newValues,top,bottom);
  }


  /* Names for thing in record */

  public FlatLattice getStackSize() {
    return (FlatLattice)getValue(STACK_SIZE);
  }
  protected UnionLattice getObjects() {
    return (UnionLattice)getValue(OBJECTS);
  }
  protected UnionLattice getFieldStore() {
    return (UnionLattice)getValue(FIELD_STORE);
  }

  protected Store setStackSize(FlatLattice ss) {
    return (Store)replaceValue(STACK_SIZE,ss);
  }
  protected Store setObjects(UnionLattice objects) {
    return (Store)replaceValue(OBJECTS,objects);
  }
  protected Store setFieldStore(UnionLattice fieldStore) {
    return (Store)replaceValue(FIELD_STORE,fieldStore);
  }

  Store errorStore(String error) {
    // Hack:
    // Start with something invalid but not quite bottom
    // in order to avoid caching in RecordLattice
    Store not_quite_bottom = ((Store)bottom()).setObjects(nodeSet);
    not_quite_bottom =
      not_quite_bottom.setStackSize(FlatLattice.newInteger(-1));
    return not_quite_bottom.setStackSize(FlatLattice.newBottom(error));
  }

  @Override
  public Lattice meet(Lattice other) {
    Store m = (Store)super.meet(other);
    if (!m.isValid() && !m.equals(top())) {
      // try to preserve cause
      if (!isValid() && !equals(top())) return this;
      if (!((Store)other).isValid() && !other.equals(top())) return other;
      return errorStore("stacksize mismatch");
    }
    return m;
  }


  /** Return true if the state represents a useful program state.
   */
  public boolean isValid() {
    return getStackSize().inDomain() &&
      !getObjects().isInfinite() &&
      !getFieldStore().isInfinite();
  }
    
  /* basic operations */

  public Store push() {
    if (!isValid()) return this;
    int n = ((Integer)getStackSize().getValue()).intValue();
    return setStackSize(getStackSize(n+1));
  }
  
  public Store pop() {
    if (!isValid()) return this;
    int n = ((Integer)getStackSize().getValue()).intValue();
    if (n == 0)
      return errorStore("stack underflow");
    return this
      .apply(new StoreRemove((UnionLattice)nodeSet.addElement(getStackTop())))
      .setStackSize(getStackSize(n-1));
  }
  

  /** return an integer indicating the status of the local: <ul>
   * <li> STATE_NULL - null
   * <li> STATE_UNIQUE - unique
   * <li> STATE_SHARED - shared
   * <li> STATE_BORROWED - borrowed
   * <li> STATE_UNDEFINED - undefined </ul>
   * @see #isUnique
   * @see #isStoreable
   * @see #isDefined
   */
  public int localStatus(Object local) {
    int status = STATE_NULL;
    try {
      Enumeration enm = getObjects().elements();
      while (enm.hasMoreElements()) {
	UnionLattice node = (UnionLattice)enm.nextElement();
	if (node.contains(local)) {
	  int nstatus = nodeStatus(node);
	  if (nstatus > status) status = nstatus;
	}
      }
    } catch (SetException ex) { // only for store = bottom = \top in paper
      status = STATE_UNDEFINED;
    }
    return status;
  }

  /** Return an integer indicating the status of an object
   * description (a set): <ul>
   * <li> STATE_UNIQUE - unique
   * <li> STATE_SHARED - shared
   * <li> STATE_BORROWED - borrowed
   * <li> STATE_UNDEFINED - undefined </ul>
   */
  public int nodeStatus(UnionLattice node) { 
    if (node.contains(undefinedVariable)) {
      return STATE_UNDEFINED;
    } else if (node.contains(sharedVariable)) {
      return STATE_SHARED;
    } else if (node.contains(borrowedVariable)) {
      return STATE_BORROWED;
    }
    return STATE_UNIQUE;
  }

  /** return whether a local must be null or a primitive value.
   * Actually the name is a misnomer.  It probably should be
   * isNoObject() or something to that effect.
   */
  public boolean isNull(Object local) {
    return localStatus(local) == STATE_NULL;
  }

  /** Return whether a local or stack location is unique. */
  public boolean isUnique(Object local) {
    return localStatus(local) <= STATE_UNIQUE;
  }
  /** Return whether a local or stack location is defined and not borrowed. */
  public boolean isStoreable(Object local) {
    return localStatus(local) <= STATE_SHARED;
  }
  /** Return whether local or stack location is defined. */
  public boolean isDefined(Object local) {
    return localStatus(local) <= STATE_BORROWED;
  }

  /** Return current top stack location.
   * @precondition isValid()
   */
  public Integer getStackTop() {
    FlatLattice fl = getStackSize();
    return (Integer)fl.getValue();
  }
  /** Return current stack location next to top.
   * @precondition isValid()
   */
  public Integer getUnderTop() {
    return (Integer)getStackSize(getStackTop().intValue()-1).getValue();
  }


  /** Return whether top of stack is unique. */
  public boolean isUnique() {
    return isUnique(getStackTop());
  }
  /** Return whether top of stack may be stored. */
  public boolean isStoreable() {
    return isStoreable(getStackTop());
  }
  /** Return whether top of statck is defined.
   * (It should also be so.)
   */
  public boolean isDefined() {
    return isDefined(getStackTop());
  }
    

  /* Stack Machine Operations:
   * NB: remember top and bottom are switched from paper
   * to agree with CFA forms.  "top" in the paper is "bottom" here.
   * Instead of using a lifted tuple for the store, we instead
   * raise exceptions when something is illegal.
   */

  /** Get an initial state.
   * We do not need to initialize field store because we implicitly
   * add edges (n,field,{}) for all n.
   */
  public Store opStart() {
    UnionLattice objects = (UnionLattice) getObjects().top();
    UnionLattice empty = nodeSet;
    Store temp = (Store) top();
    /*
     * Start with nothing on stack, and just four objects {}, {undefined},
     * {borrowed}, {borrowed,shared}
     */
    temp = temp.setStackSize(getStackSize(0));
    objects = (UnionLattice) objects.
      addElement(empty).
      addElement(empty.addElement(undefinedVariable)).
      addElement(empty.addElement(borrowedVariable)).
      addElement(empty.addElement(sharedVariable).addElement(borrowedVariable));
    temp = temp.setObjects(objects);
    /**
     * now add each parameter or local in turn. Currently undefined locals are
     * held back until the end, when they are made undefined.
     */
    UnionLattice undefinedLocals = nodeSet;
    for (int i = 0; i < locals.length; ++i) {
      IRNode local = locals[i];
      Operator op = JJNode.tree.getOperator(local);
      
      boolean isReceiverFromUniqueReturningConstructor = false;
      if (op instanceof ReceiverDeclaration) {
        /* Check if the receiver is from a constructor, and if so,
         * whether the return node of the constructor is unique 
         */
        final IRNode decl = JavaPromise.getPromisedFor(local);
        if (ConstructorDeclaration.prototype.includes(decl)) {
          // It's from a constructor, look for unique on the return node
          final IRNode returnNode = JavaPromise.getReturnNode(decl);
          if (UniquenessRules.isUnique(returnNode)) {
            isReceiverFromUniqueReturningConstructor = true;
          }
        }
      }
      if (op instanceof ReceiverDeclaration
          || op instanceof ParameterDeclaration) {
        if (isReceiverFromUniqueReturningConstructor || UniquenessRules.isBorrowed(local)) {
          temp = temp.opExisting(borrowedVariable);
        } else if (UniquenessRules.isUnique(local)) {
          temp = temp.opNew();
        } else {
          temp = temp.opExisting(sharedVariable);
        }
        UnionLattice lset = (UnionLattice) nodeSet.addElement(local);
        temp = temp.apply(new StoreAdd(temp.getStackTop(), lset)).pop();
      } else {
        undefinedLocals = (UnionLattice) undefinedLocals.addElement(local);
      }
    }
    temp = temp.apply(new StoreAdd(undefinedVariable, undefinedLocals));
    return temp;
  }

  /** Leave scope of method.  Remove all local bindings.
   */
  public Store opStop() {
    if (!isValid()) return this;
    UnionLattice localSet =
      (UnionLattice)nodeSet.union(new ImmutableHashOrderSet<Object>(locals));
    return apply(new StoreRemove(localSet));
  }

  /** Fetch the value of a local onto stack.
   **/
  public Store opGet(Object local) {
    if (!isValid()) return this;
    if (isDefined(local)) {
      Store temp = push();
      UnionLattice nset = (UnionLattice)nodeSet.addElement(temp.getStackTop());
      return push().apply(new StoreAdd(local,nset));
    } else {
      String name = (local instanceof IRNode) ?
	DebugUnparser.toString((IRNode)local) : local.toString();
      return errorStore("read undefined local: " + name);
    }
  }

  /** Special case of {@link #opGet} for the receiver.
   */
  public Store opThis() {
    if (!isValid()) return this;
    if (locals == null) {
      return errorStore("no 'this' (or anything else) in scope");
    }
    for (int i=0; i < locals.length; ++i) {
      IRNode l = locals[i];
      if (JJNode.tree.getOperator(l) instanceof ReceiverDeclaration) {
	return opGet(l);
      }
    }
    return errorStore("no 'this' in scope");
  }

  /** Duplicate a stack value from further down stack
   * @param fromTop 0 for duplicate top, 1 for under top etc.
   */
  public Store opDup(int fromTop)
  {
    if (!isValid()) return this;
    Integer i = (Integer)getStackSize(getStackTop().intValue()-fromTop).getValue();
    return opGet(i);
  }

  /** Store the top of the stack into a local. */
  public Store opSet(Object local) {
    if (!isValid()) return this;
    UnionLattice lset = (UnionLattice)nodeSet.addElement(local);
    return this
      .apply(new StoreRemove(lset))
      .apply(new StoreAdd(getStackTop(),lset))
      .pop();
  }

  /** Load a field from memory
   * @precondition isValid();
   **/
  public Store opLoad(IRNode fieldDecl) {
    if (!isValid()) return this;
    if (UniquenessRules.isUnique(fieldDecl)) {
      Integer n = getStackTop();
      UnionLattice affected = nodeSet; // X' in paper
      UnionLattice objects = getObjects();
      try {
	Enumeration enm = getFieldStore().elements();
	while (enm.hasMoreElements()) {
	  Triple t = (Triple)enm.nextElement();
	  UnionLattice object = (UnionLattice)t.first();
	  if (object.contains(n) &&
	      objects.contains(object) &&
	      t.second().equals(fieldDecl)) {
	    affected = (UnionLattice)affected.union((UnionLattice)t.third());
	  }
	}
      } catch (SetException ex) {
	return this;
      }
      if (nodeStatus(affected) != STATE_UNIQUE)
	return errorStore("loaded compromised field");
      Store temp = this
	.apply(new StoreRemove(affected))
	.apply(new StoreAdd(undefinedVariable,affected))
	.opNew();
      UnionLattice fieldStore = temp.getFieldStore();
      UnionLattice uniqueNode =
	(UnionLattice)nodeSet.addElement(temp.getStackTop());
      // System.out.println(temp);
      try {
	Enumeration enm = temp.getObjects().elements();
	while (enm.hasMoreElements()) {
	  UnionLattice object = (UnionLattice)enm.nextElement();
	  if (object.contains(n)) {
	    Triple t = new Triple(object,fieldDecl,uniqueNode);
	    // System.out.println("  adding " + tripleToString(t));
	    fieldStore = (UnionLattice)fieldStore.addElement(t);
	  }
	}
      } catch (SetException ex) {
	return this;
      }
      temp = temp.setFieldStore(fieldStore);
      // System.out.println(temp);
      return temp.opSet(n);
    } else { // shared
      return this
	.opRelease()
	.opExisting(sharedVariable);
    }
  }

  public Store opStore(IRNode fieldDecl) {
    if (!isValid()) return this;
    Store temp = this;
    if (UniquenessRules.isUnique(fieldDecl)) {
      UnionLattice fieldStore = getFieldStore();
      Integer undertop = getUnderTop();
      try {
        for (Enumeration enm = fieldStore.elements(); enm.hasMoreElements();) {
          Triple t = (Triple) enm.nextElement();
          if (!t.second().equals(fieldDecl)) continue; // irrelevant
          UnionLattice object = (UnionLattice) t.first();
          if (isStaticField(fieldDecl) || object.contains(undertop)) {
            fieldStore = (UnionLattice) fieldStore.removeElement(t);
          }
        }
      } catch (SetException ex) {
        return this;
      }
      temp = setFieldStore(fieldStore).opUndefine();
    } else {
      temp = temp.opCompromise();
    }
    return temp.opRelease();
  }

  /** Return the store after reading everything reachable from
   * the top of the stack and then popping this value.
   * In essence, any variable referring to structure reachable
   * from the top of the stack is made undefined (alias burying).
   * Used to implement read (and write) effects.
   */
  public Store opLoadReachable() {
    if (!isValid()) return this;
    Integer n = getStackTop();
    UnionLattice affected = nodeSet;
    java.util.Set<UnionLattice> found = new java.util.HashSet<UnionLattice>();
    boolean done;
    do {
      done = true;
      try {
	Enumeration enm = getFieldStore().elements();
	while (enm.hasMoreElements()) {
	  Triple t = (Triple) enm.nextElement();
	  UnionLattice object = (UnionLattice)t.first();
	  if (object.contains(n) || found.contains(object)) {
	    UnionLattice newObject = (UnionLattice)t.third();
	    if (found.add(newObject)) done = false;
	    affected = (UnionLattice)affected.union(newObject);
	  }
	}
      } catch (SetException ex) {
	return errorStore("internal error: loadReachable");
      }
    } while (!done);
    if (nodeStatus(affected) != STATE_UNIQUE)
      return errorStore("loaded compromised field");
    return this
      .apply(new StoreRemove(affected))
      .apply(new StoreAdd(undefinedVariable,affected))
      .opRelease();
  }

  /** Push the value "null" onto the top of the stack. */
  public Store opNull() {
    if (!isValid()) return this;
    return push();
  }

  /** "Allocate" a new unique value not reachable
   * from anywhere else, but possible having pointers to
   * existing things.
   */
  public Store opNew() {
    if (!isValid()) return this;
    Store temp = push();
    Integer n = temp.getStackTop();
    UnionLattice nset = (UnionLattice)nodeSet.addElement(n);
    temp = temp.setObjects((UnionLattice)temp.getObjects().addElement(nset));
    UnionLattice fieldStore = temp.getFieldStore();
    try {
      for (Enumeration enm=fieldStore.elements(); enm.hasMoreElements();) {
	Triple t = (Triple)enm.nextElement();
	if (t.first().equals(nodeSet)) {
	  fieldStore = (UnionLattice)fieldStore
	    .addElement(new Triple(nset,t.second(),t.third()));
	}
      }
    } catch (SetException ex) {
      return this;
    }
    return temp.setFieldStore(fieldStore);
  }

  /** Evaluate a psuedo-variable onto the top of the stack.
   * A psuedo-variable can have multiple values.
   */
  public Store opExisting(PseudoVariable pv) {
    if (!isValid()) return this;
    Store temp = this.push();
    UnionLattice nset =
      (UnionLattice)nodeSet.addElement(temp.getStackTop());
    return (Store)temp.meet(temp.apply(new StoreAdd(pv,nset)));
  }

  /** discard the value on the top of the stack
   * from the set of objects and from the field store,
   * and then pop the stack.
   */
  public Store opRelease() {
    if (!isValid()) return this;
    UnionLattice nset =
      (UnionLattice)nodeSet.addElement(getStackTop());
    return apply(new StoreRemove(nset)).pop();
  }

  /** Ensure the top of the stack is at least borrowed
   * and then pop the stack.
   */
  public Store opBorrow() {
    if (!isValid()) return this;
    Integer n = getStackTop();
    if (localStatus(n) > STATE_BORROWED) // cannot be shared
      return errorStore("Undefined value on stack borrowed");
    return opRelease();
  }

  /** Compromise the value on the top of the stack.
   */
  public Store opCompromiseNoRelease() {
    if (!isValid()) return this;
    Integer n = getStackTop();
    if (localStatus(n) > STATE_BORROWED) // cannot be shared
      return errorStore("Undefined value on stack shared");
    if (localStatus(n) > STATE_SHARED) // cannot be shared
      return errorStore("Borrowed value on stack shared");
    return apply(new StoreAdd(n,(UnionLattice)nodeSet.addElement(sharedVariable)));
  }

  /** Compromise the value on the top of the stack
   * and then pop it off.
   */
  public Store opCompromise() {
    return opCompromiseNoRelease().opRelease();
  }

  /** Make the top of the stack undefined and then pop it.
   * The value is being requested as a unique object
   * and no one is allowed to reference it *ever again*
   */
  public Store opUndefine() {
    if (!isValid()) return this;
    Integer n = getStackTop();
    if (localStatus(n) > STATE_BORROWED) // cannot be unique
      return errorStore("Undefined value on stack not unique");
    if (localStatus(n) > STATE_SHARED) // cannot be shared
      return errorStore("Borrowed value on stack not unique");
    if (localStatus(n) > STATE_UNIQUE) 
      return errorStore("Shared value on stack not unique");
    return apply(new StoreAdd(n,(UnionLattice)nodeSet.addElement(undefinedVariable)))
      .opRelease();
  }

  /** Return a new store where we assume the two topmost
   * stack elements are equal/nonequal and then are popped.
   * @param areEqual true if the two elements are assumed equal,
   * otherwise they are assumed unequal.
   */
  public Store opEqual(boolean areEqual) {
    if (!isValid()) return this;
    Integer n = getStackTop();
    Integer undertop = getUnderTop();
    return this
      .filter(new StoreEqual(n,undertop,areEqual))
      .opRelease()
      .opRelease();
  }


  /* generic operations */

  /** Apply a node-set changing operation to the state */
  protected Store apply(final StoreApply c) {
    if (!isValid())
      return this;

    UnionLattice objects = getObjects();
    UnionLattice newObjects = (UnionLattice) objects.top();
    try {
    	/*
      for (Enumeration enum = objects.elements(); enum.hasMoreElements();) {
        UnionLattice node = (UnionLattice) enum.nextElement();
        newObjects = (UnionLattice) newObjects.addElement(c.apply(node));
      }
      */
      final Enumeration enm = objects.elements();
      final Iterator it = new SimpleRemovelessIterator() {
        @Override
        protected Object computeNext() {        
        	if (enm.hasMoreElements()) {
        		return c.apply((UnionLattice) enm.nextElement());
        	}
          return noElement;
        }
      };
			newObjects  = (UnionLattice) newObjects.addElements(it);
    } catch (SetException ex) {
      return this;
    }
    
    UnionLattice fieldStore = getFieldStore();
    UnionLattice newFieldStore = (UnionLattice) ((Lattice) fieldStore).top();
    try {
    	/*
      for (Enumeration enum = fieldStore.elements(); enum.hasMoreElements();) {
        Triple t = (Triple) enum.nextElement();
        newFieldStore =
          (UnionLattice) newFieldStore.addElement(
            new Triple(
              c.apply((UnionLattice) t.first()),
              t.second(),
              c.apply((UnionLattice) t.third())));
      }
      */
			final Enumeration enm = fieldStore.elements();
			final Iterator it = new SimpleRemovelessIterator() {
				@Override
        protected Object computeNext() {        			
					if (enm.hasMoreElements()) {
						Triple t  = (Triple) enm.nextElement();
						return new Triple(c.apply((UnionLattice) t.first()),
 							                 t.second(),
							                 c.apply((UnionLattice) t.third()));		
					}
					return noElement;
				}
			};
			newFieldStore = (UnionLattice) newFieldStore.addElements(it);
    } catch (SetException ex) {
      return this;
    }
    return this.setObjects(newObjects).setFieldStore(newFieldStore).check();
  }

  /** Keep only nodes which fulfil the filter */
  protected Store filter(StoreFilter f) {
    if (!isValid()) return this;
    UnionLattice objects = getObjects();
    UnionLattice newObjects = (UnionLattice)objects.top();
    try {
      for (Enumeration enm = objects.elements(); enm.hasMoreElements();) {
	UnionLattice node = (UnionLattice)enm.nextElement();
	if (f.filter(node))
	  newObjects = (UnionLattice)newObjects.addElement(node);
      }
    } catch (SetException ex) {
      return this;
    }
    UnionLattice fieldStore = getFieldStore();
    UnionLattice newFieldStore = (UnionLattice)fieldStore.top();
    try {
      for (Enumeration enm=fieldStore.elements(); enm.hasMoreElements();) {
	Triple t = (Triple)enm.nextElement();
	if (f.filter((UnionLattice)t.first()) &&
	    f.filter((UnionLattice)t.third())) {
	  newFieldStore = (UnionLattice)newFieldStore.addElement(t);
	}
      }
    } catch (SetException ex) {
      return this;
    }
    return this
      .setObjects(newObjects)
      .setFieldStore(newFieldStore)
      .check();
  }

  static final ImmutableHashOrderSet pseudos =
       new ImmutableHashOrderSet(new Object[]{undefinedVariable,
						borrowedVariable,
						sharedVariable});

  /** Check that there are no compromised fields on nodes
   * known only through pseudo-variables.
   */
  protected Store check() {
    if (!isValid()) return this;
    UnionLattice fieldStore = getFieldStore();
    try {
      for (Enumeration enm=fieldStore.elements(); enm.hasMoreElements();) {
	Triple t = (Triple)enm.nextElement();
	UnionLattice from = (UnionLattice)t.first();
	if (pseudos.includes(from) &&
	    nodeStatus((UnionLattice)t.third()) > STATE_UNIQUE)
	  return errorStore("compromised field has been lost");
      }
    } catch (SetException ex) {
      return this;
    }
    return this;
  }

  /**
   * Check if the field node (or actually, the VariableDeclarator
   * node inside of a FieldDeclaration) is a static field.
   * (This method probably should be put somewhere more generic.)
   * @param node VariableDeclarator node inside of a FieldDeclaration
   * @return true if declared static
   */
  public static boolean isStaticField(IRNode node) {
    return JavaNode.getModifier(JJNode.tree.getParent(JJNode.tree.getParent(node)), JavaNode.STATIC);
  }

  @Override
  public String toString() {
    if (equals(top())) return "top";
    FlatLattice ss = getStackSize();
    if (ss.equals(ss.bottom())) return ss.toString();
    StringBuilder sb = new StringBuilder();
    sb.append("Stack depth: " + getStackSize()); sb.append('\n');
    sb.append("Objects:\n");
    try {
      for (Enumeration enm=getObjects().elements(); enm.hasMoreElements();) {
	sb.append("  " + nodeToString((UnionLattice)enm.nextElement()));
	sb.append('\n');
      }
    } catch (SetException ex) {
      sb.append("  <infinite>\n");
    }
    sb.append("Store:\n");
    try {
      for (Enumeration en=getFieldStore().elements(); en.hasMoreElements();) {
	sb.append("  " + tripleToString((Triple)en.nextElement()));
	sb.append('\n');
      }
    } catch (SetException ex) {
      sb.append("  <infinite>\n");
    }
    if (locals != null) {
      sb.append("Summary:\n");
      for (int i=0; i < locals.length; ++i) {
	sb.append("  " + localToString(locals[i]) + ": ");
	switch (localStatus(locals[i])) {
	case STATE_NULL: sb.append("null\n"); break;
	case STATE_UNIQUE: sb.append("unique\n"); break;
	case STATE_SHARED: sb.append("shared\n"); break;
	case STATE_BORROWED: sb.append("borrowed\n"); break;
	case STATE_UNDEFINED: sb.append("undefined\n"); break;
	default: sb.append("!!unknwon status\n"); break;
	}
      }
    }
    return sb.toString();
  }

  public static String tripleToString(Triple t) {
    UnionLattice node1 = (UnionLattice)t.first();
    IRNode field = (IRNode)t.second();
    UnionLattice node2 = (UnionLattice)t.third();
    return nodeToString(node1) + "." + VariableDeclarator.getId(field) +
      " = " + nodeToString(node2);
  }

  public static String nodeToString(UnionLattice node) {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    boolean first = true;
    try {
      for (Enumeration e2=node.elements(); e2.hasMoreElements();) {
	if (first) first = false; else sb.append(",");
	sb.append(localToString(e2.nextElement()));
      }
    } catch (SetException ex) {
      sb.append("...");
    }
    sb.append("}");
    return sb.toString();
  }

  public static String localToString(Object l) {
    if (l instanceof IRNode) {
      IRNode n = (IRNode)l;
      Operator op = JJNode.tree.getOperator((IRNode)l);
      if (op instanceof VariableDeclarator) {
	return VariableDeclarator.getId(n);
      } else if (op instanceof ParameterDeclaration) {
	return ParameterDeclaration.getId(n);
      } else if (op instanceof ReceiverDeclaration) {
	return "this";
      } else if (op instanceof ReturnValueDeclaration) {
	return "return";
      }
    }
    return l.toString();
  }
}

class PseudoVariable {
  public final String name;
  public final int index;
  public PseudoVariable(String n, int i) {
    name = n;
    index = i;
  }
  @Override
  public String toString() {
    return name;
  }
}

interface StoreApply {
  UnionLattice apply(UnionLattice other);
}

@SuppressWarnings("unchecked")
class StoreAdd implements StoreApply {
  private final Object var;
  private final UnionLattice additional;
  StoreAdd(Object v, UnionLattice add) {
    var = v;
    additional = add;
  }
  public UnionLattice apply(UnionLattice other) {
    if (other.contains(var)) return (UnionLattice)other.union(additional);
    return other;
  }
}
@SuppressWarnings("unchecked")
class StoreRemove implements StoreApply {
  private final UnionLattice old;
  StoreRemove(UnionLattice rid) {
    old = rid;
  }
  public UnionLattice apply(UnionLattice other) {
    return (UnionLattice)other.difference(old);
  }
}

interface StoreFilter {
  boolean filter(UnionLattice other);
}

class StoreEqual implements StoreFilter {
  private final Object v1, v2;
  private final boolean both;
  StoreEqual(Object x1, Object x2, boolean areEqual) {
    v1 = x1;
    v2 = x2;
    both = areEqual;
  }
  public boolean filter(UnionLattice node) {
    return (node.contains(v1)==node.contains(v2))==both;
  }
}

/* Test the uniqueness store */
class TestStore {
  public static void main(String[] args) {
    // avoid problems with versioning:
    Version.setDefaultEra(new Era(Version.getVersion()));
    PlainIRNode.setCurrentRegion(new VersionedRegion());

    // run a test.
    new TestStore().run(args);
  }

  // set up some useful nodes with promises

  IRNode bufField = VariableDeclarator.createNode("buf",0,null);
  { UniquenessRules.setIsUnique(bufField,true); }
  IRNode sharedField = VariableDeclarator.createNode("f",0,null);
  IRNode recDecl = ReceiverDeclaration.prototype.createNode();
  IRNode brecDecl = ReceiverDeclaration.prototype.createNode();
  { UniquenessRules.setIsBorrowed(brecDecl,true); }
  IRNode retDecl = ReturnValueDeclaration.prototype.createNode();
  { UniquenessRules.setIsUnique(retDecl,true); }
  IRNode sretDecl = ReturnValueDeclaration.prototype.createNode();
  IRNode param = ParameterDeclaration.createNode(Annotations.createNode(noNodes), 0,null,"n");
  { UniquenessRules.setIsUnique(param,true); }
  IRNode local = VariableDeclarator.createNode("local",0,null);

  public void run(String[] args) {
    if (args.length == 0 || args[0].equals("paper")) {
      papertest();
    } else if (args[0].equals("destructive")) {
      effectstest(true);
    } else if (args[0].equals("borrowing")) {
      effectstest(false);
    } else if (args[0].equals("this")) {
      System.out.println("Testing storing a borrowed this:\n");
      thistest(brecDecl,true);
      System.out.println("\n*********************************\n");
      System.out.println("Testing storing a shared this:\n");
      thistest(recDecl,true);
      System.out.println("\n*********************************\n");
      System.out.println("Testing returning a borrowed this:\n");
      thistest(brecDecl,false);
      System.out.println("\n*********************************\n");
      System.out.println("Testing returning a shared this:\n");
      thistest(recDecl,false);
    } else if (args[0].equals("zero")) {
      zerotest();
    } else {
      System.err.println("Test.Store: unknown test");
    }
  }

  void papertest() {
    Store store = new Store(new IRNode[]{recDecl,retDecl,param});
    System.out.println("Pristine:");
    System.out.println(store);

    System.out.println("Initial:");
    store = store.opStart();
    System.out.println(store);
    
    System.out.println("  this");
    store = store.opGet(recDecl);
    System.out.println(store);
    
    System.out.println("  .buf");
    store = store.opLoad(bufField);
    System.out.println(store);
    
    System.out.println("  .sync();");
    store = store.opRelease();
    System.out.println(store);
    
    System.out.println("");
    
    System.out.println("  this");
    store = store.opGet(recDecl);
    System.out.println(store);
    
    System.out.println("  .buf");
    store = store.opLoad(bufField);
    System.out.println(store);
    
    System.out.println("  .getFile()");
    store = store.opUndefine();
    store = store.opNew();
    System.out.println(store);
    
    System.out.println("  -> return value;");
    store = store.opSet(retDecl);
    System.out.println(store);
    
    System.out.println("");
    
    System.out.println("  #1=this");
    store = store.opGet(recDecl);
    System.out.println(store);
    
    System.out.println("  new Buffer()");
    store = store.opNew();
    System.out.println(store);
    
    System.out.println("  -> #1#.buf;");
    store = store.opStore(bufField);
    System.out.println(store);
    
    System.out.println("  return;");
    store = store.opGet(retDecl);
    System.out.println(store);
    
    System.out.println("final:");
    store = store.opStop();
    System.out.println(store);
    
    System.out.println("consume return value");
    store = store.opUndefine();
    System.out.println(store);
  }

  void effectstest(boolean destructive) {
    Store store = new Store(new IRNode[]{recDecl,retDecl,local});
    System.out.println("Pristine:");
    System.out.println(store);

    System.out.println("Initial:");
    store = store.opStart();
    System.out.println(store);

    System.out.println("  this");
    store = store.opGet(recDecl);
    System.out.println(store);
    
    System.out.println("  .buf");
    store = store.opLoad(bufField);
    System.out.println(store);
    
    System.out.println("  -> local;");
    store = store.opSet(local);
    System.out.println(store);

    if (destructive) {
      // try a destructive read
      System.out.println("  this");
      store = store.opGet(recDecl);
      System.out.println(store);
      
      System.out.println("  .buf = null");
      store = store.opNull();
      System.out.println(store);
      
      System.out.println("  ;");
      store = store.opStore(bufField);
      System.out.println(store);
    }
    
    System.out.println("  local");
    store = store.opGet(local);
    System.out.println(store);
    
    System.out.println("  .equals(something) with effects ...");
    store = store.opExisting(Store.borrowedVariable);
    System.out.println(store);

    System.out.println("   reads this.Instance");
    store = store.opDup(1);
    store = store.opLoadReachable();
    System.out.println(store);

    System.out.println("   reads other.Instance");
    store = store.opDup(0);
    store = store.opLoadReachable();
    System.out.println(store);

    System.out.println("   [popping argument and receiver]");
    store = store.opBorrow().opBorrow();
    System.out.println(store);

    System.out.println("");

    if (destructive) {
      // now write back field:
      System.out.println("  this");
      store = store.opGet(recDecl);
      System.out.println(store);
      
      System.out.println("  .buf = local");
      store = store.opGet(local);
      System.out.println(store);
      
      System.out.println("  ;");
      store = store.opStore(bufField);
      System.out.println(store);
    
      System.out.println("");
    }
    
    System.out.println("  return null;");
    store = store.opNull();
    System.out.println(store);
    
    System.out.println("final:");
    store = store.opStop();
    System.out.println(store);
    
    System.out.println("consume return value");
    store = store.opUndefine();
    System.out.println(store);
  }

  void thistest(IRNode recDecl, boolean doStore)
  {
    Store store = new Store(new IRNode[]{recDecl});
    System.out.println("Pristine:");
    System.out.println(store);

    System.out.println("Initial:");
    store = store.opStart();
    System.out.println(store);

    if (doStore) {
      System.out.println("  something");
      store = store.opExisting(Store.borrowedVariable);
      System.out.println(store);

      System.out.println("  .f = this // not storing yet");
      store = store.opGet(recDecl);
      System.out.println(store);

      System.out.println("  ; // do store");
      store = store.opStore(sharedField);
      System.out.println(store);      

      System.out.println("  return null // not returned yet");
      store = store.opNull();
      System.out.println(store);
    } else {
      System.out.println("  return this // not returned yet");
      store = store.opGet(recDecl);
      System.out.println(store);      
    }

    System.out.println("final:");
    store = store.opStop();
    System.out.println(store);
    
    System.out.println("consume return value");
    store = store.opCompromise();
    System.out.println(store);
  }

  public void zerotest() {
    Store store = new Store(new IRNode[]{local});
    System.out.println("Pristine:");
    System.out.println(store);

    System.out.println("Initial:");
    store = store.opStart();
    System.out.println(store);

    System.out.println("  0");
    store = store.push();
    System.out.println(store);

    System.out.println("  -> local");
    store = store.opSet(local);
    System.out.println(store);
  }
}
