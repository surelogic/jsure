package com.surelogic.common.ref;

import java.util.List;

public final class ChattyDeclVisitor extends DeclVisitor {

  public final StringBuilder b = new StringBuilder();

  public boolean visitTypesReturn = true;
  public boolean visitClassReturn = true;
  public boolean visitInterfaceReturn = true;
  public boolean visitMethodReturn = true;
  public boolean visitConstructorReturn = true;
  public boolean visitParametersReturn = true;
  public boolean visitTypeParametersReturn = true;

  @Override
  public void preVisit(IDecl node) {
    b.append("preVisit(").append(node.getClass().getSimpleName());
    b.append(")\n");
  }

  @Override
  public void postVisit(IDecl node) {
    b.append("postVisit(").append(node.getClass().getSimpleName());
    b.append(")\n");
  }

  @Override
  public void visitPackage(IDeclPackage node) {
    b.append("visitPackage(").append(node.getKind());
    b.append(" : ").append(node.getName());
    b.append(")\n");
  }

  @Override
  public boolean visitTypes(List<IDeclType> types) {
    b.append("visitTypes(");
    b.append(types.size()).append(" type(s)");
    b.append(")\n");
    return visitTypesReturn;
  }

  @Override
  public boolean visitClass(IDeclType node) {
    b.append("visitClass(").append(node.getKind());
    b.append(" : ").append(node.getName());
    b.append(")\n");
    return visitClassReturn;
  }

  @Override
  public boolean visitInterface(IDeclType node) {
    b.append("visitInterface(").append(node.getKind());
    b.append(" : ").append(node.getName());
    b.append(")\n");
    return visitInterfaceReturn;
  }

  @Override
  public void visitEnum(IDeclType node) {
    b.append("visitEnum(").append(node.getKind());
    b.append(" : ").append(node.getName());
    b.append(")\n");
  }

  @Override
  public void visitField(IDeclField node) {
    b.append("visitField(").append(node.getKind());
    b.append(" : ").append(node.getName());
    b.append(" : ").append(node.getTypeOf().getFullyQualified());
    b.append(")\n");
  }

  @Override
  public void visitInitializer(IDecl node) {
    b.append("visitInitializer(").append(node.getKind());
    b.append(" : ").append(node.getName());
    b.append(")\n");
  }

  @Override
  public boolean visitMethod(IDeclFunction node) {
    b.append("visitMethod(").append(node.getKind());
    b.append(" : ").append(node.getName());
    b.append(")\n");
    return visitMethodReturn;
  }

  @Override
  public boolean visitConstructor(IDeclFunction node) {
    b.append("visitConstructor(").append(node.getKind());
    b.append(" : ").append(node.getName());
    b.append(")\n");
    return visitConstructorReturn;
  }

  @Override
  public boolean visitParameters(List<IDeclParameter> parameters) {
    b.append("visitParameters(");
    b.append(parameters.size()).append(" parameter(s)");
    b.append(")\n");
    return visitParametersReturn;
  }

  @Override
  public void visitParameter(IDeclParameter node, boolean partOfDecl) {
    b.append("visitParameter(").append(node.getKind());
    b.append(" : ").append(node.getName());
    b.append(" : ").append(node.getTypeOf().getFullyQualified());
    b.append(", partOfDecl=").append(partOfDecl);
    b.append(")\n");
  }

  @Override
  public boolean visitTypeParameters(List<IDeclTypeParameter> typeParameters) {
    b.append("visitTypeParameters(");
    b.append(typeParameters.size()).append(" type parameter(s)");
    b.append(")\n");
    return visitTypeParametersReturn;
  }

  @Override
  public void visitTypeParameter(IDeclTypeParameter node, boolean partOfDecl) {
    b.append("visitTypeParameter(").append(node.getKind());
    b.append(" : ").append(node.getName());
    b.append(", partOfDecl=").append(partOfDecl);
    b.append(")\n");
  }

  @Override
  public void endVisitTypes(List<IDeclType> types) {
    b.append("endVisitTypes(");
    b.append(types.size()).append(" type(s)");
    b.append(")\n");
  }

  @Override
  public void endVisitClass(IDeclType node) {
    b.append("endVisitClass(").append(node.getKind());
    b.append(" : ").append(node.getName());
    b.append(")\n");
  }

  @Override
  public void endVisitInterface(IDeclType node) {
    b.append("endVisitInterface(").append(node.getKind());
    b.append(" : ").append(node.getName());
    b.append(")\n");
  }

  @Override
  public void endVisitMethod(IDeclFunction node) {
    b.append("endVisitMethod(").append(node.getKind());
    b.append(" : ").append(node.getName());
    b.append(")\n");
  }

  @Override
  public void endVisitConstructor(IDeclFunction node) {
    b.append("endVisitConstructor(").append(node.getKind());
    b.append(" : ").append(node.getName());
    b.append(")\n");
  }

  @Override
  public void endVisitParameters(List<IDeclParameter> parameters) {
    b.append("endVisitParameters(");
    b.append(parameters.size()).append(" parameter(s)");
    b.append(")\n");
  }

  @Override
  public void endVisitTypeParameters(List<IDeclTypeParameter> typeParameters) {
    b.append("endVisitTypeParameters(");
    b.append(typeParameters.size()).append(" type parameter(s)");
    b.append(")\n");
  }
}
