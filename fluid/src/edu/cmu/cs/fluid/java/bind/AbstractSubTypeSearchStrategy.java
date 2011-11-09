/*
 * Created on Dec 17, 2003
 *
 */
package edu.cmu.cs.fluid.java.bind;

/**
 * @author chance
 *
 */
public abstract class AbstractSubTypeSearchStrategy<T>
	extends AbstractTypeSearchStrategy<T> implements ISubTypeSearchStrategy<T> 
{
	protected AbstractSubTypeSearchStrategy(IBinder bind, String prefix, String name) {
		super(bind, prefix, name);
	}
}
