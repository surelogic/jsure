package test.clazz.getter;

import java.io.Serializable;

import com.surelogic.Singleton;

@Singleton
public final class BadSerializableNoReturn implements Serializable {
  private static final BadSerializableNoReturn INSTANCE = new BadSerializableNoReturn();
  
  private transient int x;
  private transient int y;
  
  public static BadSerializableNoReturn getInstance() {
    return INSTANCE;
  }
  
  private BadSerializableNoReturn() {
    super();
  }
  
  private Object readResolve() {
    throw new RuntimeException();
  }
}
