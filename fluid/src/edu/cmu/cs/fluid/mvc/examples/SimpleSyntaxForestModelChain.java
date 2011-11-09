package edu.cmu.cs.fluid.mvc.examples;

import edu.cmu.cs.fluid.mvc.tree.syntax.SyntaxForestModel;

public interface SimpleSyntaxForestModelChain extends SimpleForestModelChain {
  public SyntaxForestModel getBaseSyntaxModel();
  public SyntaxForestModel getFixedSyntaxModel();
  public SyntaxForestModel getConfigurableSyntaxView();

}
