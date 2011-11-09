package test.clazz.getter;

import java.io.Serializable;

import com.surelogic.Singleton;

@Singleton
public final class BadSerializableFieldNotTransient implements Serializable {
  private static final BadSerializableFieldNotTransient INSTANCE = new BadSerializableFieldNotTransient();
  
  private transient int x;
  private int y;
  
  public static BadSerializableFieldNotTransient getInstance() {
    return INSTANCE;
  }
  
  private BadSerializableFieldNotTransient() {
    super();
  }
  
  private Object readResolve() {
    return INSTANCE;
  }
}
