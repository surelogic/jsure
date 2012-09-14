package edu.cmu.cs.fluid.sea;

/**
 * This marker interface can be implemented by a {@link PromiseDrop} subtype,
 * <i>p</i>, to request that the UI show <i>p</i> results at the top level (in
 * the overall category folder) even if it is a prerequisite assertion of
 * another promise, <i>a</i>, and <i>p</i> results would normally only be shown
 * nested under the <i>a</i>.
 */
public interface UiShowAtTopLevel extends IPromiseDrop {
  // marker interface
}
