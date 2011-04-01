package test;

import com.surelogic.Immutable;

@Immutable
public class Point3D extends Point {
  private final int z;
  
  public Point3D(final int a, final int b, final int c) {
    super(a, b);
    this.z = c;
  }
  
  public int getZ() {
    return z;
  }
}
