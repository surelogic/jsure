package test;

import com.surelogic.RegionLock;
import com.surelogic.Starts;

@RegionLock("Lock is this protects Instance")
public class Simple {
  int x, y;

  Simple() {
    x = 4;
    y = 3;
  }

  public synchronized int getX() {
    return x;
  }

  public synchronized void setX(int x) {
    this.x = x;
  }

  public synchronized int getY() {
    return y;
  }

  public synchronized void setY(int y) {
    this.y = y;
  }

  @Starts("nothing")
  public void m() {
    setX(getY());
    setY(getX());
  }
}
