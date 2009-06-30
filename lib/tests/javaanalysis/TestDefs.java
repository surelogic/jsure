/* $Header: /cvs/fluid/fluid/lib/tests/javaanalysis/TestDefs.java,v 1.1 2001/11/19 22:44:44 aarong Exp $ */
package fluid.java.analysis.test;

public class TestDefs
{
  public static void main( String[] args )
  {
    int x = 0;
    int y = 1;
    int z;
    int w;

    if( z != 2 ) {
      w = 3;
    } else {
      w = 4;
    }

    z = w + x + y;
  }
}
