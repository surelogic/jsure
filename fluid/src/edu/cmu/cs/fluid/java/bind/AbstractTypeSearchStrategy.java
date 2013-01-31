package edu.cmu.cs.fluid.java.bind;

/**
 */
public abstract class AbstractTypeSearchStrategy<T> implements ITypeSearchStrategy<T> {
  protected final IBinder binder;
  protected final String prefix, name;

  /**
   * @param prefix A title for the Java element we're looking for (e.g. "method")
   * @param name The name of the Java element we're looking for
   */
  protected AbstractTypeSearchStrategy(IBinder bind, String prefix, String name) {
    binder = bind;
    this.prefix = prefix;
    this.name   = name;
  }
	/**
	 * @see edu.cmu.cs.fluid.java.bind.ITypeSearchStrategy#getLabel()
	 */
	@Override
  public final String getLabel() {
		return prefix + " " + name;
	}
	
	public abstract void reset();
}
