package test.clazz.getter;

import java.io.Serializable;

import com.surelogic.Singleton;

@Singleton
public final class BadSerializableReturnsNull implements Serializable {
  private static final BadSerializableReturnsNull INSTANCE = new BadSerializableReturnsNull();
  
  private transient int x;
  private transient int y;
  
  public static BadSerializableReturnsNull getInstance() {
    return INSTANCE;
  }
  
  private BadSerializableReturnsNull() {
    super();
  }
  
  private Object readResolve() {
    return null;
  }
}
