package test.clazz.publicField;

import com.surelogic.Singleton;

@Singleton
public final class BadMakesExtraInstances {
  public static final BadMakesExtraInstances INSTANCE = new BadMakesExtraInstances();
  
  private BadMakesExtraInstances() {
    super();
  }
  
  public static Object doStuff() {
    return new BadMakesExtraInstances();
  }
  
  public void x() {
    System.out.println(new BadMakesExtraInstances());
  }
}
