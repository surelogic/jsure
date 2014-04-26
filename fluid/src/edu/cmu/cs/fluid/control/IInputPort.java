package edu.cmu.cs.fluid.control;

/**
 * A port which enters a component's internals.
 * For example, the entry port of a component or an exit port of
 * a subcomponent.  The inputs to an input port come from its dual.
 * Thus it directly only has outputs.  This is confusing, but correct. 
 */
public interface IInputPort extends Port {
}