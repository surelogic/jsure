package com.surelogic.common.ref;

import java.util.List;

public final class ChattyDeclVisitor extends DeclVisitor {

  private final StringBuilder b = new StringBuilder();

  public String getResult() {
    return b.toString();
  }

  public boolean visitTypesReturn = true;
  public boolean visitClassReturn = true;
  public boolean visitInterfaceReturn = true;
  public boolean visitLambdaReturn = true;
  public boolean visitMethodReturn = true;
  public boolean visitConstructorReturn = true;
  public boolean visitParametersReturn = true;
  public boolean visitTypeParametersReturn = true;

  @Override
  public void start(IDecl beingVisited) {
    b.append("START ");
  }

  @Override
  public void finish(IDecl wasVisited) {
    b.append("END");
  }

  @Override
  public void visitPackage(IDeclPackage node) {
    b.append("visitPackage(").append(node.getName());
    b.append(") -> ");
  }

  @Override
  public boolean visitTypes(List<IDeclType> types) {
    b.append("visitTypes(count=");
    b.append(types.size());
    b.append(") -> ");
    return visitTypesReturn;
  }

  @Override
  public boolean visitClass(IDeclType node) {
    b.append("visitClass(").append(node.getName());
    b.append(") -> ");
    return visitClassReturn;
  }

  @Override
  public boolean visitInterface(IDeclType node) {
    b.append("visitInterface(").append(node.getName());
    b.append(") -> ");
    return visitInterfaceReturn;
  }

  @Override
  public void visitAnnotation(IDeclType node) {
    b.append("visitAnnotation(").append(node.getName());
    b.append(") -> ");
  }

  @Override
  public void visitEnum(IDeclType node) {
    b.append("visitEnum(").append(node.getName());
    b.append(") -> ");
  }

  @Override
  public void visitField(IDeclField node) {
    b.append("visitField(").append(node.getName());
    b.append(":").append(node.getTypeOf().getCompact());
    b.append(") -> ");
  }

  @Override
  public void visitInitializer(IDecl node) {
    b.append("visitInitializer(").append(node.getName());
    b.append(") -> ");
  }

  @Override
  public boolean visitLambda(IDeclLambda node) {
    b.append("visitLambda(").append(node.getTypeOf().getFullyQualified());
    b.append(") -> ");
    return visitLambdaReturn;
  }

  @Override
  public boolean visitMethod(IDeclFunction node) {
    b.append("visitMethod(").append(node.getName());
    b.append(") -> ");
    return visitMethodReturn;
  }

  @Override
  public boolean visitConstructor(IDeclFunction node) {
    b.append("visitConstructor(").append(node.getName());
    b.append(") -> ");
    return visitConstructorReturn;
  }

  @Override
  public boolean visitParameters(List<IDeclParameter> parameters) {
    b.append("visitParameters(count=");
    b.append(parameters.size());
    b.append(") -> ");
    return visitParametersReturn;
  }

  @Override
  public void visitParameter(IDeclParameter node, boolean partOfDecl) {
    b.append("visitParameter(").append(node.getName());
    b.append(":").append(node.getTypeOf().getCompact());
    b.append(",partOfDecl=").append(partOfDecl);
    b.append(") -> ");
  }

  @Override
  public boolean visitTypeParameters(List<IDeclTypeParameter> typeParameters) {
    b.append("visitTypeParameters(count=");
    b.append(typeParameters.size());
    b.append(") -> ");
    return visitTypeParametersReturn;
  }

  @Override
  public void visitTypeParameter(IDeclTypeParameter node, boolean partOfDecl) {
    b.append("visitTypeParameter(").append(node.getName());
    b.append(", partOfDecl=").append(partOfDecl);
    b.append(") -> ");
  }

  @Override
  public void endVisitTypes(List<IDeclType> types) {
    b.append("endVisitTypes(count=");
    b.append(types.size());
    b.append(") -> ");
  }

  @Override
  public void endVisitClass(IDeclType node) {
    b.append("endVisitClass(").append(node.getName());
    b.append(") -> ");
  }

  @Override
  public void endVisitInterface(IDeclType node) {
    b.append("endVisitInterface(").append(node.getName());
    b.append(") -> ");
  }

  @Override
  public void endVisitLambda(IDeclLambda node) {
    b.append("endVisitLambda(").append(node.getTypeOf().getFullyQualified());
    b.append(") -> ");
  }

  @Override
  public void endVisitMethod(IDeclFunction node) {
    b.append("endVisitMethod(").append(node.getName());
    b.append(") -> ");
  }

  @Override
  public void endVisitConstructor(IDeclFunction node) {
    b.append("endVisitConstructor(").append(node.getName());
    b.append(") -> ");
  }

  @Override
  public void endVisitParameters(List<IDeclParameter> parameters) {
    b.append("endVisitParameters(count=");
    b.append(parameters.size());
    b.append(") -> ");
  }

  @Override
  public void endVisitTypeParameters(List<IDeclTypeParameter> typeParameters) {
    b.append("endVisitTypeParameters(count=");
    b.append(typeParameters.size());
    b.append(") -> ");
  }
}
