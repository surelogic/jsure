package edu.cmu.cs.fluid.sea;

/**
 * This marker interface can be implemented by a {@link PromiseDrop} subtype,
 * <i>p</i>, to request that the UI show all the instances of <i>p</i> that are
 * a prerequisite assertion of another promise, <i>a</i>, in a sub-folder under
 * <i>a</i> rather than individually under <i>a</i> which is the default.
 */
public interface UiPlaceInASubFolder extends IPromiseDrop {
  // marker interface
}
