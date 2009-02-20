package edu.cmu.cs.fluid.dcf.views.coe;

public class DiffResultsView extends ResultsView {
  @Override
  protected IResultsViewContentProvider makeContentProvider() {
    return new DiffResultsViewContentProvider();
  }
}
