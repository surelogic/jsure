/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/visitor/DescendingVisitor.java,v 1.21 2007/10/30 20:02:58 chance Exp $*/
package com.surelogic.aast.visitor;

import java.util.List;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.INodeVisitor;
import com.surelogic.aast.java.*;
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
  
  @SuppressWarnings("unchecked")
  public T visit(ColorAndNode n) {
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
  public T visit(ColorizedRegionNode n) {
    T rv = defaultValue;
    for(AASTNode c : n.getCRegionsList()) {
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
  
  public T visit(ColorImportNode n) {
    T rv = defaultValue;
    return rv;
  }

  public T visit(ColorIncompatibleNode n) {
    T rv = defaultValue;
    for(AASTNode c : n.getColorList()) {
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
  public T visit(ColorNameNode n) {
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
    rv = combineResults(rv, doAccept(n.getType()));
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
  public T visit(ColorRenameNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getColor()));
    rv = combineResults(rv, doAccept(n.getCExpr()));
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
    return rv;
  }
  public T visit(ModulePromiseNode n) {
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
    rv = combineResults(rv, doAccept(n.getType()));
    for(AASTNode c : n.getSigList()) {
      rv = combineResults(rv, doAccept(c));
    }
    return rv;
  }
  public T visit(ColorCardSpecNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getColor()));
    rv = combineResults(rv, doAccept(n.getCard()));
    return rv;
  }
  public T visit(TransparentNode n) {
    T rv = defaultValue;
    return rv;
  }
  public T visit(ColorRequireNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getCExpr()));
    return rv;
  }
  public T visit(ColorRevokeNode n) {
    T rv = defaultValue;
    for(AASTNode c : n.getColorList()) {
      rv = combineResults(rv, doAccept(c));
    }
    return rv;
  }
  @SuppressWarnings("unchecked")
  public T visit(ColorOrNode n) {
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
  public T visit(ColorNotNode n) {
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
  public T visit(ColorCRNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getCExpr()));
    for(AASTNode c : n.getCRegionsList()) {
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
  public T visit(ColorDeclarationNode n) {
    T rv = defaultValue;
    for(AASTNode c : n.getColorList()) {
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
  public T visit(ColorCardNNode n) {
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
  public T visit(ColorGrantNode n) {
    T rv = defaultValue;
    for(AASTNode c : n.getColorList()) {
      rv = combineResults(rv, doAccept(c));
    }
    return rv;
  }
  public T visit(ColorExprNode n) {
    T rv = defaultValue;
    return rv;
  }
  public T visit(ColorCard1Node n) {
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
  
  public T visit(AssumeFinalNode n) {
    T rv = defaultValue;
    return rv;
  }
  
  public T visit(UniqueNode n) {
    T rv = defaultValue;
    return rv;
  }

  public T visit(SingleThreadedNode n) {
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

  public T visit(AggregateNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getSpec()));
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

  public T visit(SelfProtectedNode node) {
    T rv = defaultValue;
    return rv;
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
    rv = combineResults(rv, doAccept(n.getInTypePattern()));
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
	 * @see com.surelogic.aast.INodeVisitor#visit(com.surelogic.aast.promise.NotUniqueNode)
	 */
	public T visit(NotUniqueNode notUniqueNode) {
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

  public T visit(ColorExprPromiseNode n) {
    T rv = defaultValue;
    rv = combineResults(rv, doAccept(n.getTheExprNode()));
    return rv;
  }  
	
}
