package edu.cmu.cs.fluid.analysis.structure;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;

import edu.cmu.cs.fluid.dc.AbstractAnalysisModule;

public final class Structure extends AbstractAnalysisModule {

  /**
   * Extracts the string literal value represented by the expression tree. This
   * might include several string literals concatenated together. For example
   * the expression <code>("foo" + "bar")</code> would result in the String
   * <code>"foobar"</code>. This is a simple, but helpful, utility method.
   * 
   * @param e
   *          the tree representing a String literal
   * @return a literal String value
   */
  private static String extractIntent(Expression e) {
    StringBuilder result = new StringBuilder();
    if (e instanceof StringLiteral) {
      result.append(((StringLiteral) e).getLiteralValue());
    } else if (e instanceof InfixExpression) {
      InfixExpression concat = (InfixExpression) e;
      if (concat.getOperator() == Operator.PLUS) {
        result.append(Structure.extractIntent(concat.getLeftOperand()));
        result.append(Structure.extractIntent(concat.getRightOperand()));
      } else {
        throw new IllegalStateException("Expected a (String + String) found \""
            + e + "\" is the expression of type String?");
      }
    }
    return result.toString();
  }

  private static Vis VIS = new Vis();

  private static class Vis extends ASTVisitor {

    @Override
    public boolean visit(PackageDeclaration node) {
      System.out.println(node.structuralPropertiesForType());
      return super.visit(node);
    }

    @Override
    public boolean visit(ArrayInitializer node) {
      // TODO Auto-generated method stub
      return super.visit(node);
    }

    @Override
    public boolean visit(SingleMemberAnnotation node) {
      System.out.println(node.getTypeName());
      System.out.println(node.toString());
      Expression e = node.getValue();
      if (e instanceof ArrayInitializer) {
        ArrayInitializer ai = (ArrayInitializer) e;
        for (Object o : ai.expressions()) {
          System.out.println(" ArrayInit \""
              + Structure.extractIntent((Expression) o));
        }
        System.out.println("ArrayInitializer is argument");
      } else {
        String intent = Structure.extractIntent(e);
        System.out.println(" Single String = \"" + intent + "\"");
      }
      return super.visit(node);
    }
  }

  @Override
  public void analyzeCompilationUnit(ICompilationUnit file, CompilationUnit ast) {
    try {
      if (file.getCorrespondingResource().getName().endsWith(
          "package-info.java")) {
        // special "package-info.java" file
        ast.accept(VIS);
      }
    } catch (JavaModelException e) {
      // TODO not sure what to do here?
      e.printStackTrace();
    }
  }

  @Override
  public boolean analyzeResource(IResource resource, int kind) {
    return (kind == IResourceDelta.REMOVED || kind == IResourceDelta.MOVED_FROM);
  }
}