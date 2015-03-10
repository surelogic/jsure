/*
 * $header$
 * Created on Jan 9, 2005
 */
package edu.cmu.cs.fluid.java.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.fluid.ir.IRBooleanType;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.ir.SlotNotRegisteredException;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.AssignmentInterface;
import edu.cmu.cs.fluid.parse.JJNode;

/**
 * A simple analysis that determines whether a variable is assigned or not.
 * @author boyland
 */
public class UnassignedVariables extends CachedProceduralAnalysis<Boolean,UnassignedVariables.Results> {

  private static final String SLOT_INFO_NAME = "fluid.java.UnassignedVariables";
  
  /**
   * @param b bindings to use for analysis
   * @throws SlotAlreadyRegisteredException
   */
  public UnassignedVariables(IBinder b)
      throws SlotAlreadyRegisteredException {
    super(SLOT_INFO_NAME, IRBooleanType.prototype,b);
  }
 
  @SuppressWarnings("unchecked")
  public synchronized static UnassignedVariables getInstance(IBinder b) {
    UnassignedVariables uv;
    try {
      uv = (UnassignedVariables)SlotInfo.<Boolean>findSlotInfo(SLOT_INFO_NAME);
    } catch (SlotNotRegisteredException e) {
      try {
        uv = new UnassignedVariables(b);
      } catch (SlotAlreadyRegisteredException e1) {
        // can't get here
        uv = null;
      }
    }
    return uv;
  }
  
  @Override
  protected Results computeResults(IRNode proc) {
    Map<IRNode,Boolean> unassigned = new HashMap<IRNode,Boolean>();
    ProcedureVisitor v = new FindAssignmentsVisitor(unassigned);
    v.doAccept(proc);
    return new Results(proc,unassigned);
  }
  
  public static class Results extends CachedProceduralAnalysis.SimpleResults<Boolean> {
    private final Set<IRNode> unassigned;
    Results() {
      super(null,null);
      unassigned = null;
    }
    Results(IRNode proc, Map<IRNode,Boolean> unassignedMap) {
      super(proc,unassignedMap);
      unassigned = unassignedMap.keySet();
    }
    public Set<IRNode> getUnassigned() {
      return unassigned;
    }
  }
  
  @Override
  protected Results makePlaceholder() {
    return new Results();
  }

  private class FindAssignmentsVisitor extends ProcedureVisitor<Void> {
    private final Map<IRNode,Boolean> unassignedMap;
    public FindAssignmentsVisitor(Map<IRNode,Boolean> unassigned) {
      this.unassignedMap = unassigned;
    }
    
    @Override
    public Void visitExpression(IRNode node) {
      if (JJNode.tree.getOperator(node) instanceof AssignmentInterface)
        visitAssignment(node);
      super.visit(node);
      return null;
    }
    
    @Override
    public Void visitVariableDeclarator(IRNode node) {
      unassignedMap.put(node,true);
      super.visitVariableDeclarator(node);
      return null;
    }

    @Override
    public Void visitAssignment(IRNode node) {
      IRNode target = ((AssignmentInterface)JJNode.tree.getOperator(node)).getTarget(node);
      IRNode decl = binder.getBinding(target);
      if (decl != null) {
        unassignedMap.put(node,false);
      }
      return null;
    }
  }
 }
