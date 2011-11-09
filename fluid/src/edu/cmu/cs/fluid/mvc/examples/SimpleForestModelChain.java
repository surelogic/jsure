package edu.cmu.cs.fluid.mvc.examples;

import edu.cmu.cs.fluid.mvc.predicate.PredicateModel;
import edu.cmu.cs.fluid.mvc.tree.ForestModel;
import edu.cmu.cs.fluid.mvc.version.VersionTrackerModel;
import edu.cmu.cs.fluid.mvc.visibility.VisibilityModel;

import edu.cmu.cs.fluid.render.StyleSetModel;

public interface SimpleForestModelChain {
  VersionTrackerModel getTracker();
  ForestModel getBaseModel();
  ForestModel getFixedModel();
  StyleSetModel getPalette();
  PredicateModel getPredModel();
  VisibilityModel getVisibilityModel();
  ForestModel getConfigurableView();
}
