/** A class that uses every node that we defined for Java.
 * It doesn't do anything useful per se.
 */
package fluid.java.analysis.test;

import java.util.Vector;

import fluid.*;

public class Everything implements EverythingInterface {
  public Everything() {
    super();
  }
  public Everything(double d) {
    this();
  }

  private static boolean initialized;
  static {
    initialized = true;
  }

  private char sign;
  {
      sign = '-';
  }

  public int allOperators(int x, byte y, short z) {
    return (x < y && x <= z || x > y && !(x >= z) ||
	    x == y && x != z) ?
      x += ((y++ >> 1) & (z-- << 1)) ^ ~((++y >>> 1) | --z) :
      (x /= (y + z) / (y - z) * (+y % -z));      
  }

  public long asLong() {
    return 0L;
  }

  Everything[] createArray(boolean x) {
    try {
      if (x) 
	return new Everything[]{(Everything)super.clone(), 
				new Everything() {
	  void somethingMore() {
	    return;
	  }
	}};
      return new Everything[12];
    } catch (CloneNotSupportedException ex) {
      Everything y[] = {null,this,new Everything((float)3.14159)};
      return y;
    } finally {
      try {
	synchronized(EverythingNested.class) {
	  throw new NullPointerException("not really");
	}
      } catch (RuntimeException ex) {
	System.out.println("Ignoring exception " + ex);
      }
      x = false;
    }
  }

  public Everything otherExpressions() throws CloneNotSupportedException {
    /* do nothing */ ;
    if (this instanceof Cloneable) {
      return (Everything)clone();
    } else {
      return createArray(initialized)[1];
    }
  }

  public EverythingNested create() {
    throw new FluidError("Not implemented");
  }

  public interface EverythingInterfaceNested extends EverythingInterface {
  }

  public class EverythingNested {
    public void loop() {
      int i;
      loop1: for (i=0,++i; i < 10; ++i) {
	switch (i) {
	default:
	  break;
	case 1:
	  break loop1;
	case 2:
	  continue;
	case 3:
	  continue loop1;
	}
      }
      while (i != i) ;
      do {
	Everything e = Everything.this;
	e.new EverythingNested();
      } while (false);
    }
  }
}

interface EverythingInterface {
  public long asLong();
  Everything.EverythingNested create();
}

  /* UnnamedPackageDeclaration.op */

