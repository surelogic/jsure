package fluid.java.analysis.test;

public class TestsForEffectsChecker
{
  public int x, y, z;

  public void foo()
  {
    //@ reads( this.x, this.y );
    x = 1;
    y = 2;
    z = 3;
  }
}
