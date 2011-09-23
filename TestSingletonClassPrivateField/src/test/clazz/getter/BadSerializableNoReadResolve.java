package test.clazz.getter;

import java.io.Serializable;

import com.surelogic.Singleton;

@Singleton
public final class BadSerializableNoReadResolve implements Serializable {
  private static final BadSerializableNoReadResolve INSTANCE = new BadSerializableNoReadResolve();
  
  private transient int x;
  private transient int y;
  
  public static BadSerializableNoReadResolve getInstance() {
    return INSTANCE;
  }
  
  private BadSerializableNoReadResolve() {
    super();
  }
}
