package suggest_juc_lock_fields;

public class Other {
  public void doStuff() {
    final C1 c1 = new C1();
    synchronized (c1) { // want special warning here
      // do Stuff
    }

    final C2 c2 = new C2();
    synchronized (c2) { // want special warning here
      // do Stuff
    }

    final C3 c3 = new C3();
    synchronized (c3) { // want special warning here
      // do Stuff
    }

    final C4 c4 = new C4();
    synchronized (c4) { // want special warning here
      // do Stuff
    }
  }
}
