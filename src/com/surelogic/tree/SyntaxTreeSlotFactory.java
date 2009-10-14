/*$Header$*/
package com.surelogic.tree;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.tree.*;

public final class SyntaxTreeSlotFactory extends SimpleSlotFactory {  
  private SyntaxTreeSlotFactory() {
    // just here to ensure there's only one
  }
  public static final SyntaxTreeSlotFactory prototype = new SyntaxTreeSlotFactory();

  static class Storage<T> extends ImplicitSlotStorage<T> {
    Storage(SyntaxTreeSlotFactory factory, T val) {
      super(factory, val);
    }  
    @Override
    public T getUndefinedValue() {
      return super.getUndefinedValue();
    }
    
  }
  private final Storage<Operator> opStorage = 
    new Storage<Operator>(this, Constants.undefinedOperator);
  
  private final Storage<IRLocation> locStorage = 
    new Storage<IRLocation>(this, Constants.undefinedLocation);

  private final Storage<IRSequence<IRNode>> seqStorage = 
    new Storage<IRSequence<IRNode>>(this, Constants.undefinedSequence);
  
  private final Storage<ISrcRef> srcRefStorage = 
    new Storage<ISrcRef>(this, Constants.undefinedSrcRef);
  
  private final Storage<String> stringStorage = 
	    new Storage<String>(this, Constants.undefinedString);
  
  private final Storage<IRNode> nodeStorage = 
	    new Storage<IRNode>(this, Constants.undefinedNode);
  
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
  
  private SlotInfo<ISrcRef> makeSrcRefSI(String name, ISrcRef defaultVal,
		                                 StoredSlotInfo<ISrcRef,ISrcRef> backupSI) 
  throws SlotAlreadyRegisteredException {
	  return new NodeStoredSlotInfo<ISrcRef>(ISrcRef.srcRefName, name, ISrcRef.srcRefType, 
			                                 srcRefStorage, defaultVal, backupSI) { 
		  @Override
		  protected ISrcRef getSlot(SyntaxTreeNode n) {
			  return n.srcRef;
		  }

		  @Override
		  protected void setSlot(SyntaxTreeNode n, ISrcRef slotState) {
			  n.srcRef = slotState;
		  }
	  };
  }
  
  private static final String infoName = "JJNode.info";

  private SlotInfo<String> makeInfoSI(String name, String defaultVal, 
		                              StoredSlotInfo<String,String> backupSI) 
  throws SlotAlreadyRegisteredException {
	  return new NodeStoredSlotInfo<String>(infoName, name, IRStringType.prototype, 
			                                stringStorage, defaultVal, backupSI) { 
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
  
  @Override
  @SuppressWarnings("unchecked")
  public <T> SlotInfo<T> newAttribute(String name, IRType<T> type) throws SlotAlreadyRegisteredException {
	  //StoredSlotInfo<T,T> backup = (StoredSlotInfo<T, T>) super.newAttribute(name+".backup", type);
	  return newAttribute(name, type, null, true);
  }

  @Override
  @SuppressWarnings("unchecked")
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
    else if (type == ISrcRef.srcRefType && ISrcRef.srcRefName.equals(name)) {
    	ISrcRef def = undefined ? Constants.undefinedSrcRef : (ISrcRef) defaultValue;
    	backupSI = makeBackupSI(name, type, defaultValue, undefined);
    	return (SlotInfo<T>) makeSrcRefSI(name, def, (StoredSlotInfo<ISrcRef, ISrcRef>) backupSI);
    }
    else if (type instanceof IRStringType && infoName.equals(name)) {
    	String def = undefined ? Constants.undefinedString : (String) defaultValue;
    	backupSI = makeBackupSI(name, type, defaultValue, undefined);
    	return (SlotInfo<T>) makeInfoSI(name, def, (StoredSlotInfo<String, String>) backupSI);
    }
    
    if (undefined) {    	
        return super.newAttribute(name, type);
    }
    return super.newAttribute(name, type, defaultValue);
  }
}
