package edu.cmu.cs.fluid.java.bind;

/**
 * A Strategy for matching a single method in a type
 */
public class FindMethodStrategy extends FindMethodsStrategy
{	
  public FindMethodStrategy(IBinder bind, String mname, IJavaType[] types) {
    super(bind, mname, types);
  }

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.eclipse.bind.ITypeSearchStrategy#getResult()
	 */
	@Override
  public Object getResult() {
		int size = methods.size();
		switch (size) {
	  case 0:
	  	return null;
	  case 1:
	  	return methods.iterator().next();
		default:
			LOG.warning("Searching for a single match, but found "+size);
			return null;
		}
	}
}
