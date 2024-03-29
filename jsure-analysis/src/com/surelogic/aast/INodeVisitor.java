/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/INodeVisitor.java,v 1.17 2007/10/30 20:02:58 chance Exp $*/
package com.surelogic.aast;

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

@SuppressWarnings("deprecation")
public interface INodeVisitor<T> {
  T visit(ThisExpressionNode node);
  T visit(SuperExpressionNode node);
  T visit(QualifiedThisExpressionNode n);
  T visit(IntTypeNode n);
  T visit(ArrayTypeNode n);
  T visit(BlockImportNode n);
  T visit(ShortTypeNode n);
  T visit(LongTypeNode n);
  T visit(TypeRefNode n); 
  T visit(FieldRefNode n);
  T visit(DoubleTypeNode n);
  T visit(VariableUseExpressionNode n);
  T visit(BooleanTypeNode n);
  T visit(NamedTypeNode n);
  T visit(VoidTypeNode n);
  T visit(ByteTypeNode n);
  T visit(CharTypeNode n);
  T visit(ClassExpressionNode n);
  T visit(FloatTypeNode n);
  T visit(ConditionNode n);
  T visit(TypeQualifierPatternNode n);
  T visit(EffectSpecificationNode n);
  T visit(ImplicitQualifierNode n);
  T visit(EnclosingModuleNode n);
  T visit(RegionReportRolesNode n); 
  T visit(QualifiedRegionNameNode n);
  T visit(FieldMappingsNode n);
  T visit(LockDeclarationNode n);
  T visit(InvariantDeclarationNode n);
  T visit(TaintedNode n);
  T visit(ThreadRoleImportNode n);
  T visit(AnyInstanceExpressionNode n);
  T visit(QualifiedReceiverDeclarationNode n);
  T visit(RegionMappingNode n);
  T visit(QualifiedLockNameNode n);
  T visit(TypeDeclPatternNode n);
  T visit(QualifiedClassLockExpressionNode n);
  T visit(ScopedPromiseNode n);
  T visit(NotTargetNode n);
  T visit(AnyTargetNode n);
  T visit(ClassInitDeclarationNode n);
  T visit(NotTaintedNode n);
  T visit(FieldDeclPatternNode n);
  T visit(RegionNameNode n);
  T visit(PolicyLockDeclarationNode n);
  T visit(ReturnsLockNode n);
  T visit(RequiresLockNode n);
  T visit(ProhibitsLockNode n);
  T visit(IsLockNode n);
  T visit(ConstructorDeclPatternNode n);
  T visit(ModuleNode n);
  T visit(SimpleLockNameNode n);
  T visit(MethodDeclPatternNode n);
  T visit(ThreadRoleTransparentNode n);
  T visit(ScopedModuleNode n);
  T visit(ReturnValueDeclarationNode n);
  T visit(OrTargetNode n);
  T visit(ModuleWrapperNode n);
  T visit(ModuleChoiceNode n);
  T visit(ExportNode n);
  T visit(ExportToNode n);
  T visit(VisClauseNode n);
  T visit(NoVisClauseNode n);
  T visit(ModulePromiseNode n);
  T visit(ImplicitClassLockExpressionNode n);
  T visit(ReceiverDeclarationNode n);
  T visit(InitDeclarationNode n);
  T visit(StartsSpecificationNode n);
  T visit(AndTargetNode n);
  T visit(AbstractLockDeclarationNode n);
  T visit(NewRegionDeclarationNode n);
  T visit(LockNameNode n);
  T visit(IntOrNNode n);
  T visit(MappedRegionSpecificationNode n);
  T visit(ThreadRoleCardNNode n);
  T visit(SubtypedBySpecificationNode n);
  T visit(APINode n);
  T visit(ThreadRoleCard1Node n);
  T visit(EffectsSpecificationNode n);
  T visit(BorrowedNode n);
  T visit(UniqueNode n);
  T visit(TypeExpressionNode n);
  T visit(InRegionNode node);
  T visit(UniqueMappingNode node);   
  T visit(ReadLockNode node); 
  T visit(WriteLockNode node);
  T visit(ContainableNode node);
  T visit(ThreadSafeNode node); 
  T visit(NotThreadSafeNode node); 
  T visit(ImmutableNode node); 
  T visit(InPatternNode node);
  T visit(InAndPatternNode node);
  T visit(InOrPatternNode node);
  T visit(InNotPatternNode node);
  T visit(InPackagePatternNode node);
	T visit(WildcardTypeQualifierPatternNode wildcardTypeQualifierPatternNode);
	T visit(RegionEffectsNode regionEffectsNode);
	T visit(UniqueInRegionNode n);
	T visit(VouchSpecificationNode n);
	T visit(MethodCallNode methodCallNode);
	T visit(GuardedByNode guardedByNode);
	T visit(ItselfNode itselfNode);
	T visit(UnidentifiedTargetNode unidentifiedTargetNode);
	T visit(UnionTargetNode unionTargetNode);
	T visit(InLayerNode inLayerNode);
	T visit(MayReferToNode mayReferToNode);
	T visit(AllowsReferencesFromNode allowsReferencesFromNode);
	T visit(LayerNode layerNode);
	T visit(TypeSetNode typeSetNode);
	T visit(TargetListNode targetListNode);
	T visit(ModuleScopeNode moduleScopeNode);
	T visit(NotContainableNode node);
	T visit(MutableNode node);
	T visit(VouchFieldIsNode node);
	T visit(UtilityNode utilityNode);
	T visit(SingletonNode singletonNode);
	T visit(NonNullNode nonNullNode);
	T visit(NullableNode nullableNode);
	T visit(RawNode rawNode);
	T visit(AnnotationBoundsNode n);
	T visit(ValueObjectNode n);
	T visit(RefObjectNode n);
	T visit(MustInvokeOnOverrideNode n);
	T visit(ThreadConfinedNode n);
	T visit(NoTargetNode n);
	T visit(CastNode n);
	T visit(TrackPartiallyInitializedNode n);
}