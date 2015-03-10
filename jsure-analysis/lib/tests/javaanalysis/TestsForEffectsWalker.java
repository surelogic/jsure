package fluid.java.analysis.test;

import java.lang.*;
import java.util.*;

public class TestsForEffectsWalker
extends java.lang.Object
{
  public static final int CONSTANT = 1;
  public static Vector VECTOR = new Vector();

  Object car, cdr;

  Object obj;
  String str;

  public TestsForEffectsWalker()
  {
    this( CONSTANT, VECTOR );
  }

  public TestsForEffectsWalker( int num, Vector v )
  {
    super();
    obj = v;
    str = "*" + num + "*";
  }

  public int useStatic()
  {
    return CONSTANT + this.CONSTANT + TestsForEffectsWalker.CONSTANT;
  }

  public int useArrays( int[] a, int x, int y )
  {
    int[] array = new int[] { 3, 4, 5 };
    a[x] = array[0];
    a[y] = array[1];
    array = new int[4];

    return a[array.length];
  }

  public void useInstances()
  {
    TestsForEffectsWalker o = new TestsForEffectsWalker();
    this.obj = o.obj;
    this.str = o.str;
  }

  public TestsForEffectsWalker makePair( Object x, Object y )
  {
    TestsForEffectsWalker pair = new TestsForEffectsWalker( 0, new Vector() );
    pair.car = x;
    pair.cdr = y;
    return pair;
  }

  /* Should have the effects:
   *   writes( t.str )
   *   reads( s.[]    )
   *   reads( Array.[] ) (from notInvisible)
   *   reads( this.str )
   */
  public int inferMyEffects( TestsForEffectsWalker t, int[] s, int x, int y )
  {
    int z = x + y;
    int[] invisible = new int[2];
    int[] notInvisible = new int[3];

    if( z > 10 )
    {
      notInvisible = s;
      invisible = new int[4];
    }

    t.str = "***" + s[1] + notInvisible[2] + invisible[4];  
    t = this;  // should not see writes( t )
    y = this.str.length();
    x = 3;  // should not see this
    
    return 0;
  }
}
