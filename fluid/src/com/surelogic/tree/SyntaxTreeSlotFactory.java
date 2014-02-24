/*$Header$*/
package com.surelogic.tree;

import com.surelogic.common.ref.IJavaRef;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.tree.*;

public final class SyntaxTreeSlotFactory extends SimpleSlotFactory {  
  private SyntaxTreeSlotFactory() {
    // just here to ensure there's only one
  }
  public static final SyntaxTreeSlotFactory prototype = new SyntaxTreeSlotFactory();

  static class Storage<T> extends ImplicitSlotStorage<T> {
    Storage(SyntaxTreeSlotFactory factory, T undefVal) {
      super(factory, undefVal);
    }  
    @Override
    public T getUndefinedValue() {
      return super.getUndefinedValue();
    }    
  }
  
  // TODO this only works because it's undefined
  private final Storage<Operator> opStorage = 
    new Storage<Operator>(this, Constants.undefinedOperator);
  
  private final Storage<IRLocation> locStorage = 
    new Storage<IRLocation>(this, Constants.undefinedLocation);

  private final Storage<IRSequence<IRNode>> seqStorage = 
    new Storage<IRSequence<IRNode>>(this, Constants.undefinedSequence);
  
  private final Storage<IJavaRef> srcRefStorage = 
    new Storage<IJavaRef>(this, Constants.undefinedSrcRef);

  private final Storage<IRNode> nodeStorage = 
	    new Storage<IRNode>(this, Constants.undefinedNode);
  
  private SlotInfo<Integer> makeModifiersSI(String name, Integer defaultVal, 
		  StoredSlotInfo<Integer,Integer> backupSI) 
		  throws SlotAlreadyRegisteredException {
	  return new NodeStoredSlotInfo<Integer>(JavaNode.MODIFIERS_ID, name, IRIntegerType.prototype, 
			  new Storage<Integer>(this, defaultVal), 
			  defaultVal, backupSI) { 
		  @Override
		  protected Integer getSlot(SyntaxTreeNode n) {
			  return n.modifiers;
		  }

		  @Override
		  protected void setSlot(SyntaxTreeNode n, Integer slotState) {
			  n.modifiers = slotState;
		  }
	  };
  }
  
  private SlotInfo<Operator> makeOperatorSI(String name, Operator defaultVal,
		                                    StoredSlotInfo<Operator,Operator> backupSI) 
  throws SlotAlreadyRegisteredException {
    return new NodeStoredSlotInfo<Operator>(SyntaxTree.OPERATOR, name, 
                                            IROperatorType.prototype, 
                                            opStorage, defaultVal, backupSI) {
      @Override
      protected final Operator getSlot(SyntaxTreeNode n) {
        return n.op;
      }

      @Override
      protected final void setSlot(SyntaxTreeNode n, Operator slotState) {
        n.op = slotState;
      }
    };
  }

  private SlotInfo<IRLocation> makeLocationSI(String name, IRLocation defaultVal,
                                              StoredSlotInfo<IRLocation,IRLocation> backupSI) 
  throws SlotAlreadyRegisteredException {
    return new NodeStoredSlotInfo<IRLocation>(Tree.LOCATION, name, 
                                              IRLocationType.prototype, 
                                              locStorage, defaultVal, backupSI) {
      @Override
      protected final IRLocation getSlot(SyntaxTreeNode n) {
    	return n.loc;
      }

      @Override
      protected final void setSlot(SyntaxTreeNode n, IRLocation slotState) {
        n.loc = slotState;
      }
    };
  }
  
  private SlotInfo<IRSequence<IRNode>> makeChildrenSI(String name, IRSequence<IRNode> defaultVal,
		                                              StoredSlotInfo<IRSequence<IRNode>,
		                                                             IRSequence<IRNode>> backupSI) 
  throws SlotAlreadyRegisteredException {
    return new NodeStoredSlotInfo<IRSequence<IRNode>>(Digraph.CHILDREN, name, 
                                                      IRSequenceType.nodeSequenceType, 
                                                      seqStorage, defaultVal, backupSI) {      
      @Override
      protected final IRSequence<IRNode> getSlot(SyntaxTreeNode n) {
        return n.children;
      }

      @Override
      protected final void setSlot(SyntaxTreeNode n, IRSequence<IRNode> slotState) {
        n.children = slotState;
      }
    };
  }
  
  private SlotInfo<IRNode> makeParentSI(String name, IRNode defaultVal,
		                                StoredSlotInfo<IRNode,IRNode> backupSI) 
  throws SlotAlreadyRegisteredException {
    return new NodeStoredSlotInfo<IRNode>(Tree.PARENTS, name, 
                                          IRNodeType.prototype, 
                                          nodeStorage, defaultVal, backupSI) { 
      @Override
      protected IRNode getSlot(SyntaxTreeNode n) {
        return n.parent;
      }

      @Override
      protected void setSlot(SyntaxTreeNode n, IRNode slotState) {
        n.parent = slotState;
      }
    };
  }
  
  private SlotInfo<IJavaRef> makeSrcRefSI(String name, IJavaRef defaultVal,
		                                 StoredSlotInfo<IJavaRef,IJavaRef> backupSI) 
      throws SlotAlreadyRegisteredException {
    return new NodeStoredSlotInfo<IJavaRef>(JavaNode.Consts.FLUID_JAVA_REF_SLOT_NAME, name, 
    		JavaNode.Consts.FLUID_JAVA_REF_SLOT_TYPE, srcRefStorage, defaultVal, backupSI) {
      @Override
      protected IJavaRef getSlot(SyntaxTreeNode n) {
    	// Unable to build the ref here due to sync issues
        return n.srcRef;
      }

      @Override
      public IJavaRef getSlotValue(final IRNode node) throws SlotUndefinedException {
    	  IJavaRef rv;
    	  synchronized (node) {
    		  rv = getSlotValue_unsync(node);
    	  }
    	  // Might be built twice, but should get the same value
    	  if (SkeletonJavaRefUtility.useSkeletonsAsJavaRefPlaceholders && rv instanceof SkeletonJavaRefUtility.JavaRefSkeletonBuilder) {
    		  rv = ((SkeletonJavaRefUtility.JavaRefSkeletonBuilder) rv).buildOrNullOnFailure(node);
    		  synchronized (node) {
    			  setSlotValue_unsync(node, rv);
    		  }
    	  }
    	  return rv;
      }
      
      @Override
      protected void setSlot(SyntaxTreeNode n, IJavaRef slotState) {
        n.srcRef = slotState;
      }
    };
  }
  
  private static final String infoName = "JJNode.info";
  /*
  private SlotInfo<String> makeInfoSI(String name, String defaultVal, 
		                              StoredSlotInfo<String,String> backupSI) 
  throws SlotAlreadyRegisteredException {
	  return new NodeStoredSlotInfo<String>(infoName, name, IRStringType.prototype, 
			  new Storage<String>(SyntaxTreeSlotFactory.this, Constants.undefinedString), 
			                                defaultVal, backupSI) { 
		  @Override
		  protected String getSlot(SyntaxTreeNode n) {
			  return n.info;
		  }

		  @Override
		  protected void setSlot(SyntaxTreeNode n, String slotState) {
			  n.info = slotState;
		  }
	  };
  }
  */
  
  @Override
  public <T> SlotInfo<T> newAttribute(String name, IRType<T> type) throws SlotAlreadyRegisteredException {
	  //StoredSlotInfo<T,T> backup = (StoredSlotInfo<T, T>) super.newAttribute(name+".backup", type);
	  return newAttribute(name, type, null, true);
  }

  @Override
  public <T> SlotInfo<T> newAttribute(String name, IRType<T> type, T defaultValue) throws SlotAlreadyRegisteredException {
	  //StoredSlotInfo<T,T> backup = (StoredSlotInfo<T, T>) super.newAttribute(name+".backup", type, defaultValue);
	  return newAttribute(name, type, defaultValue, false);
  }
  
  private final <T> SlotInfo<T> makeBackupSI(String name, IRType<T> type, 
                                             T defaultValue, boolean undefined) 
                                throws SlotAlreadyRegisteredException 
  {
	  if (undefined) {
		  return super.newAttribute(name+".backup", type);
	  } else {
		  return super.newAttribute(name+".backup", type, defaultValue);
	  }
  }
  
  @SuppressWarnings("unchecked")
  private final <T> SlotInfo<T> newAttribute(String name, IRType<T> type, 
		                                     T defaultValue, boolean undefined) 
  throws SlotAlreadyRegisteredException {
	/*
	if (!undefined) {
		System.out.println("Default to: "+defaultValue);
	}
	*/
	SlotInfo<T> backupSI;
    if (type == IROperatorType.prototype && name.endsWith(SyntaxTree.OPERATOR)) {
      Operator def = undefined ? Constants.undefinedOperator : (Operator) defaultValue;
      backupSI = makeBackupSI(name, type, defaultValue, undefined);
      return (SlotInfo<T>) makeOperatorSI(name, def, (StoredSlotInfo<Operator, Operator>) backupSI);
    }
    else if (type == IRLocationType.prototype && name.endsWith(Tree.LOCATION)) {
      IRLocation def = undefined ? Constants.undefinedLocation : (IRLocation) defaultValue;
      backupSI = makeBackupSI(name, type, defaultValue, undefined);
      return (SlotInfo<T>) makeLocationSI(name, def, (StoredSlotInfo<IRLocation, IRLocation>) backupSI);
    }
    else if (type == IRNodeType.prototype && name.endsWith(Tree.PARENTS)) {
        IRNode def = undefined ? Constants.undefinedNode : (IRNode) defaultValue;
        backupSI = makeBackupSI(name, type, defaultValue, undefined);
        return (SlotInfo<T>) makeParentSI(name, def, (StoredSlotInfo<IRNode, IRNode>) backupSI);
      }
    else if (type instanceof IRSequenceType) {
      @SuppressWarnings("rawtypes")
      IRSequenceType t = (IRSequenceType) type;
      IRSequence<IRNode> def = undefined ? Constants.undefinedSequence : 
    	                       (IRSequence<IRNode>) defaultValue;
      if (t.getElementType() instanceof IRNodeType) {
        if (name.endsWith(Digraph.CHILDREN)) {
          backupSI = makeBackupSI(name, type, defaultValue, undefined);
          return (SlotInfo<T>) makeChildrenSI(name, def, (StoredSlotInfo<IRSequence<IRNode>, 
        		                                                         IRSequence<IRNode>>) backupSI);
        } 
        /*
        else if (name.endsWith(Tree.PARENTS)) {
          return (SlotInfo<T>) makeParentSI(name, def);
        } 
        */
      }
    }
    else if (type == JavaNode.Consts.FLUID_JAVA_REF_SLOT_TYPE && JavaNode.Consts.FLUID_JAVA_REF_SLOT_NAME.equals(name)) {
    	IJavaRef def = undefined ? Constants.undefinedSrcRef : (IJavaRef) defaultValue;
    	backupSI = makeBackupSI(name, type, defaultValue, undefined);
    	return (SlotInfo<T>) makeSrcRefSI(name, def, (StoredSlotInfo<IJavaRef, IJavaRef>) backupSI);
    }
    /*
    else if (type instanceof IRStringType && infoName.equals(name)) {
    	String def = undefined ? Constants.undefinedString : (String) defaultValue;
    	backupSI = makeBackupSI(name, type, defaultValue, undefined);
    	return (SlotInfo<T>) makeInfoSI(name, def, (StoredSlotInfo<String, String>) backupSI);
    }
    */
    else if (type instanceof IRIntegerType && JavaNode.MODIFIERS_ID.equals(name)) {
     	Integer def = undefined ? Constants.undefinedInteger : (Integer) defaultValue;
    	backupSI = makeBackupSI(name, type, defaultValue, undefined);
    	return (SlotInfo<T>) makeModifiersSI(name, def, (StoredSlotInfo<Integer, Integer>) backupSI);
    }
    
    if (undefined) {    	
        return super.newAttribute(name, type);
    }
    return super.newAttribute(name, type, defaultValue);
  }
}
