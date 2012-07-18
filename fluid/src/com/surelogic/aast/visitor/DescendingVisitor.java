/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/visitor/DescendingVisitor.java,v 1.21 2007/10/30 20:02:58 chance Exp $*/
package com.surelogic.aast.visitor;

import java.util.List;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.INodeVisitor;
import com.surelogic.aast.java.*;
import com.surelogic.aast.layers.AllowsReferencesFromNode;
import com.surelogic.aast.layers.InLayerNode;
import com.surelogic.aast.layers.LayerNode;
import com.surelogic.aast.layers.MayReferToNode;
import com.surelogic.aast.layers.TargetListNode;
import com.surelogic.aast.layers.TypeSetNode;
import com.surelogic.aast.layers.UnidentifiedTargetNode;
import com.surelogic.aast.layers.UnionTargetNode;
import com.surelogic.aast.promise.*;

public class DescendingVisitor<T> implements INodeVisitor<T> {
  final T defaultValue;
  
  public DescendingVisitor(T defaultVal) {
    defaultValue = defaultVal;
  }
  
  public T doAccept(AASTNode node) {
    if (node == null) { return defaultValue; }
    return node.accept(this);
  }

  protected T combineResults(T before, T next) {
    return (next == null) ? before : next;
  }

  public T visit(QualifiedThisExpressionNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getType()));
    return rv;
  }
 
  public T visit(IntTypeNode n) {
    T rv = defaultValue;
    return rv;
  }

  public T visit(ArrayTypeNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getBase()));
    return rv;
  }
  
  public T visit(BlockImportNode n) {
    T rv = defaultValue;
    if (n.getOfNamesClause() != null) {
      rv = combineResults(rv, doAccept(n.getOfNamesClause()));
    }
    return rv;
  }

  public T visit(ShortTypeNode n) {
    T rv = defaultValue;
    return rv;
  }
  public T visit(LongTypeNode n) {
    T rv = defaultValue;
    return rv;
  }
 
  public T visit(ThisExpressionNode n) {
    T rv = defaultValue;
    return rv;
  }
  
  public T visit(SuperExpressionNode n) {
    T rv = defaultValue;
    return rv;
  }
 
  public T visit(TypeRefNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getBase()));
    return rv;
  }
 
  public T visit(FieldRefNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getObject()));
    return rv;
  }
  public T visit(DoubleTypeNode n) {
    T rv = defaultValue;
    return rv;
  }


  public T visit(VariableUseExpressionNode n) {
    T rv = defaultValue;
    return rv;
  }

  public T visit(BooleanTypeNode n) {
    T rv = defaultValue;
    return rv;
  }
 
  public T visit(NamedTypeNode n) {
    T rv = defaultValue;
    return rv;
  }
 
  public T visit(VoidTypeNode n) {
    T rv = defaultValue;
    return rv;
  }

  public T visit(ByteTypeNode n) {
    T rv = defaultValue;
    return rv;
  }

  public T visit(CharTypeNode n) {
    T rv = defaultValue;
    return rv;
  }


  public T visit(ClassExpressionNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getType()));
    return rv;
  }

  public T visit(FloatTypeNode n) {
    T rv = defaultValue;
    return rv;
  }

  public T visit(ConditionNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getCond()));
    return rv;
  }
  public T visit(TypeQualifierPatternNode n) {
    T rv = defaultValue;
    return rv;
  }
  public T visit(UsedBySpecificationNode n) {
    T rv = defaultValue;
    for(AASTNode c : n.getTypesList()) {
      rv = combineResults(rv, doAccept(c));
    }
    return rv;
  }
  public T visit(EffectSpecificationNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getContext()));
    rv = combineResults(rv, doAccept(n.getRegion()));
    return rv;
  }
  public T visit(ImplicitQualifierNode n) {
    T rv = defaultValue;
    return rv;
  }
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public T visit(ThreadRoleAndNode n) {
    T rv = defaultValue;
    for(AASTNode c : (List<AASTNode>) (List) n.getAndElemsList()) {
      rv = combineResults(rv, doAccept(c));
    }
    return rv;
  }
  public T visit(EnclosingModuleNode n) {
    T rv = defaultValue;
    for(AASTNode c : n.getModulesList()) {
      rv = combineResults(rv, doAccept(c));
    }
    return rv;
  }
  public T visit(RegionReportRolesNode n) {
    T rv = defaultValue;
    for(AASTNode c : n.getTRRegionsList()) {
      rv = combineResults(rv, doAccept(c));
    }
    return rv;
  }
  public T visit(QualifiedRegionNameNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getType()));
    return rv;
  }
  public T visit(FieldMappingsNode n) {
    T rv = defaultValue;
    for(AASTNode c : n.getFieldsList()) {
      rv = combineResults(rv, doAccept(c));
    }
    rv = combineResults(rv, doAccept(n.getTo()));
    return rv;
  }
  public T visit(LockDeclarationNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getField()));
    rv = combineResults(rv, doAccept(n.getRegion()));
    return rv;
  }
  public T visit(InvariantDeclarationNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getCond()));
    return rv;
  }
  public T visit(TaintedNode n) {
    T rv = defaultValue;
    return rv;
  }
  
  public T visit(ThreadRoleImportNode n) {
    T rv = defaultValue;
    return rv;
  }

  public T visit(ThreadRoleIncompatibleNode n) {
    T rv = defaultValue;
    for(AASTNode c : n.getThreadRoleList()) {
      rv = combineResults(rv, doAccept(c));
    }
    return rv;
  }
  public T visit(AnyInstanceExpressionNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getType()));
    return rv;
  }
  public T visit(QualifiedReceiverDeclarationNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getBase()));
    return rv;
  }
  public T visit(ThreadRoleNameNode n) {
    T rv = defaultValue;
    return rv;
  }
  public T visit(RegionMappingNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getFrom()));
    rv = combineResults(rv, doAccept(n.getTo()));
    return rv;
  }
  public T visit(QualifiedLockNameNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getBase()));
    return rv;
  }
  public T visit(TypeDeclPatternNode n) {
    T rv = defaultValue;
    return rv;
  }
  public T visit(QualifiedClassLockExpressionNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getType()));
    return rv;
  }
  public T visit(ScopedPromiseNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getTargets()));
    return rv;
  }
  public T visit(NotTargetNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getTarget()));
    return rv;
  }
  public T visit(ClassInitDeclarationNode n) {
    T rv = defaultValue;
    return rv;
  }
  public T visit(NotTaintedNode n) {
    T rv = defaultValue;
    return rv;
  }
  public T visit(FieldDeclPatternNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getFtype()));
    return rv;
  }
  public T visit(RegionNameNode n) {
    T rv = defaultValue;
    return rv;
  }
  public T visit(PolicyLockDeclarationNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getField()));
    return rv;
  }
  public T visit(ThreadRoleRenameNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getThreadRole()));
    rv = combineResults(rv, doAccept(n.getTRExpr()));
    return rv;
  }
  public T visit(ConstructorDeclPatternNode n) {
    T rv = defaultValue;
    for(AASTNode c : n.getSigList()) {
      rv = combineResults(rv, doAccept(c));
    }
    return rv;
  }
  public T visit(ModuleNode n) {
    T rv = defaultValue;
    return rv;
  }
  public T visit(ModuleWrapperNode n) {
    T rv = defaultValue;
    return rv;
  }
  public T visit(ModuleChoiceNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getModPromise()));
    rv = combineResults(rv, doAccept(n.getModWrapper()));
    rv = combineResults(rv, doAccept(n.getModScope()));
    return rv;
  }
  public T visit(ModulePromiseNode n) {
    T rv = defaultValue;
    return rv;
  }
  
  public T visit(ModuleScopeNode n) {
	    T rv = defaultValue;
	    return rv;
  }
  
  public T visit(VisClauseNode n) {
    T rv = defaultValue;
    return rv;
  }
  public T visit(NoVisClauseNode n) {
    T rv = defaultValue;
    return rv;
  }  public T visit(ExportNode n) {
    T rv = defaultValue;
    return rv;
  }  public T visit(ExportToNode n) {
    T rv = defaultValue;
    return rv;
  }
  public T visit(SimpleLockNameNode n) {
    T rv = defaultValue;
    return rv;
  }
  public T visit(MethodDeclPatternNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getRtype()));
    for(AASTNode c : n.getSigList()) {
      rv = combineResults(rv, doAccept(c));
    }
    return rv;
  }
  public T visit(ThreadRoleCardSpecNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getTRole()));
    rv = combineResults(rv, doAccept(n.getCard()));
    return rv;
  }
  public T visit(ThreadRoleTransparentNode n) {
    T rv = defaultValue;
    return rv;
  }
  public T visit(ThreadRoleRequireNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getTRExpr()));
    return rv;
  }
  public T visit(ThreadRoleRevokeNode n) {
    T rv = defaultValue;
    for(AASTNode c : n.getThreadRoleList()) {
      rv = combineResults(rv, doAccept(c));
    }
    return rv;
  }
  @SuppressWarnings("unchecked")
  public T visit(ThreadRoleOrNode n) {
    T rv = defaultValue;
    for(AASTNode c : (List<AASTNode>) (List) n.getOrElemsList()) {
      rv = combineResults(rv, doAccept(c));
    }
    return rv;
  }
  public T visit(ScopedModuleNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getTargets()));
    return rv;
  }
  public T visit(ReturnValueDeclarationNode n) {
    T rv = defaultValue;
    return rv;
  }
  public T visit(OrTargetNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getTarget1()));
    rv = combineResults(rv, doAccept(n.getTarget2()));
    return rv;
  }
  public T visit(ThreadRoleNotNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getTarget()));
    return rv;
  }
  public T visit(ImplicitClassLockExpressionNode n) {
    T rv = defaultValue;
    return rv;
  }
  public T visit(ReceiverDeclarationNode n) {
    T rv = defaultValue;
    return rv;
  }
  public T visit(InitDeclarationNode n) {
    T rv = defaultValue;
    return rv;
  }
  public T visit(StartsSpecificationNode n) {
    T rv = defaultValue;
    return rv;
  }
  public T visit(ThreadRoleCRNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getTRExpr()));
    for(AASTNode c : n.getTRRegionsList()) {
      rv = combineResults(rv, doAccept(c));
    }
    return rv;
  }
  public T visit(AndTargetNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getTarget1()));
    rv = combineResults(rv, doAccept(n.getTarget2()));
    return rv;
  }
  public T visit(AnyTargetNode n) {
    T rv = defaultValue;
    return rv;
  }
  
  public T visit(AbstractLockDeclarationNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getField()));
    return rv;
  }
  public T visit(ThreadRoleDeclarationNode n) {
    T rv = defaultValue;
    for(AASTNode c : n.getThreadRoleList()) {
      rv = combineResults(rv, doAccept(c));
    }
    return rv;
  }
  public T visit(NewRegionDeclarationNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getRegionParent()));
    return rv;
  }
  public T visit(LockNameNode n) {
    T rv = defaultValue;
    return rv;
  }
  public T visit(IntOrNNode n) {
    T rv = defaultValue;
    return rv;
  }
  public T visit(MappedRegionSpecificationNode n) {
    T rv = defaultValue;
    for(AASTNode c : n.getMappingList()) {
      rv = combineResults(rv, doAccept(c));
    }
    return rv;
  }
  public T visit(ThreadRoleCardNNode n) {
    T rv = defaultValue;
    return rv;
  }
  public T visit(SubtypedBySpecificationNode n) {
    T rv = defaultValue;
    for(AASTNode c : n.getTypesList()) {
      rv = combineResults(rv, doAccept(c));
    }
    return rv;
  }
  public T visit(APINode n) {
    T rv = defaultValue;
    return rv;
  }
  public T visit(ThreadRoleGrantNode n) {
    T rv = defaultValue;
    for(AASTNode c : n.getThreadRoleList()) {
      rv = combineResults(rv, doAccept(c));
    }
    return rv;
  }
  public T visit(ThreadRoleExprNode n) {
    T rv = defaultValue;
    return rv;
  }
  public T visit(ThreadRoleCard1Node n) {
    T rv = defaultValue;
    return rv;
  }
  public T visit(EffectsSpecificationNode n) {
    T rv = defaultValue;
    for(AASTNode c : n.getEffectList()) {
      rv = combineResults(rv, doAccept(c));
    }
    return rv;
  }

  public T visit(BorrowedNode n) {
    T rv = defaultValue;
    return rv;
  }
  
  public T visit(UniqueNode n) {
    T rv = defaultValue;
    return rv;
  }
  
  public T visit(TypeExpressionNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getType()));
    return rv;
  }

  public T visit(InRegionNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getSpec()));
    return rv;
  }
  
  public T visit(UniqueInRegionNode n) {
	  T rv = defaultValue;
	  rv = combineResults(rv, doAccept(n.getSpec()));
	  return rv;
  }
  
  public T visit(UniqueMappingNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getMapping()));
    return rv;
  }

  public T visit(ProhibitsLockNode n) {
	    T rv = defaultValue;
	    for (LockSpecificationNode l : n.getLockList()) {
	      rv = combineResults(rv, doAccept(l));
	    }
	    return rv;
  }
  
  public T visit(ReturnsLockNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getLock()));
    return rv;
  }

  public T visit(RequiresLockNode n) {
    T rv = defaultValue;
    for (LockSpecificationNode l : n.getLockList()) {
      rv = combineResults(rv, doAccept(l));
    }
    return rv;
  }

  public T visit(IsLockNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getLock()));
    return rv;
  }  
  
  public T visit(ReadLockNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getLock()));
    return rv;
  }  
  
  public T visit(WriteLockNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getLock()));
    return rv;
  }

  public T visit(ContainableNode node) {
    T rv = defaultValue;
    return rv;
  }
  
  public T visit(ThreadSafeNode node) {
    T rv = defaultValue;
    return rv;
  }
  
  public T visit(NotThreadSafeNode node) {
	  T rv = defaultValue;
	  return rv;
  }
  
  public T visit(NotContainableNode node) {
	  return defaultValue;
  }
  
  public T visit(MutableNode node) {
	  return defaultValue;
  }
  
  public T visit(ImmutableNode node) {
	  return defaultValue;
  }

  public T visit(ImmutableRefNode node) {
	  return defaultValue;
  }
  
	/* (non-Javadoc)
	 * @see com.surelogic.aast.INodeVisitor#visit(com.surelogic.aast.promise.InPatternNode)
	 */
	public T visit(InPatternNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getInTypePattern()));
    rv = combineResults(rv, doAccept(n.getInPackagePattern()));
    return rv;
	}

	/* (non-Javadoc)
	 * @see com.surelogic.aast.INodeVisitor#visit(com.surelogic.aast.promise.InAndPatternNode)
	 */
	public T visit(InAndPatternNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getTarget1()));
    rv = combineResults(rv, doAccept(n.getTarget2()));
    return rv;
	}

	/* (non-Javadoc)
	 * @see com.surelogic.aast.INodeVisitor#visit(com.surelogic.aast.promise.InOrPatternNode)
	 */
	public T visit(InOrPatternNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getTarget1()));
    rv = combineResults(rv, doAccept(n.getTarget2()));
    return rv;
	}

	/* (non-Javadoc)
	 * @see com.surelogic.aast.INodeVisitor#visit(com.surelogic.aast.promise.InNotPatternNode)
	 */
	public T visit(InNotPatternNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getTarget()));
		return rv;
	}

	/* (non-Javadoc)
	 * @see com.surelogic.aast.INodeVisitor#visit(com.surelogic.aast.promise.InPackagePatternNode)
	 */
	public T visit(InPackagePatternNode n) {
    T rv = defaultValue;
		return rv;
	}

	/* (non-Javadoc)
	 * @see com.surelogic.aast.INodeVisitor#visit(com.surelogic.aast.promise.WildcardTypeQualifierPatternNode)
	 */
	public T visit(
			WildcardTypeQualifierPatternNode wildcardTypeQualifierPatternNode) {
		T rv = defaultValue;
		return rv;
	}

	/* (non-Javadoc)
	 * @see com.surelogic.aast.INodeVisitor#visit(com.surelogic.aast.promise.RegionEffectsNode)
	 */
	public T visit(RegionEffectsNode regionEffectsNode) {
    T rv = defaultValue;
    for(AASTNode c : regionEffectsNode.getEffectsList()) {
      rv = combineResults(rv, doAccept(c));
    }
    return rv;
	}

  public T visit(ThreadRoleExprPromiseNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getTheExprNode()));
    return rv;
  }  

  public T visit(VouchSpecificationNode n) {
	  return defaultValue;
  }
  
  public T visit(MethodCallNode n) {
	  return doAccept(n.getObject());
  }

  public T visit(GuardedByNode n) {
	  return doAccept(n.getLock());
  }

  public T visit(ItselfNode itselfNode) {
	  return defaultValue;
  }

  public T visit(UnidentifiedTargetNode n) {
	  return defaultValue;
  }

  public T visit(UnionTargetNode n) {
	  T rv = defaultValue;
	  for(AASTNode c : n.getUnion()) {
		  rv = combineResults(rv, doAccept(c));
	  }
	  return rv;
  }
  
  public T visit(TargetListNode n) {
	  T rv = defaultValue;
	  for(AASTNode c : n.getUnion()) {
		  rv = combineResults(rv, doAccept(c));
	  }
	  return rv;
  }

  public T visit(InLayerNode n) {
	  return doAccept(n.getLayers());
  }

  public T visit(MayReferToNode n) {
	  return doAccept(n.getTarget());
  }

  public T visit(AllowsReferencesFromNode n) {
	  return doAccept(n.getTarget());
  }

  public T visit(LayerNode n) {
	  return doAccept(n.getTarget());
  }

  public T visit(TypeSetNode n) {
	  return doAccept(n.getTarget());
  }

  public T visit(VouchFieldIsNode node) {
	  return defaultValue;
  }

  public T visit(UtilityNode node) {
	  return defaultValue;
  }

  public T visit(ReadOnlyNode readonlyNode) {
	  return defaultValue;
  }

  public T visit(SingletonNode singletonNode) {
	  return defaultValue;
  }

  public T visit(SimpleBorrowedInRegionNode n) {
	  return doAccept(n.getSpec());
  }

  public T visit(ExplicitBorrowedInRegionNode n) {
	  return doAccept(n.getMapping());
  }

  public T visit(NonNullNode n) {
	  return defaultValue;
  }

  public T visit(NullableNode n) {
	  return defaultValue;
  }

  public T visit(RawNode rawNode) {
	  return defaultValue;
  }
}
