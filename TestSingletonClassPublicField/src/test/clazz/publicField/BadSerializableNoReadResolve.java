package test.clazz.publicField;

import java.io.Serializable;

import com.surelogic.Singleton;

@Singleton
public final class BadSerializableNoReadResolve implements Serializable {
  public static final BadSerializableNoReadResolve INSTANCE = new BadSerializableNoReadResolve();
  
  private transient int x;
  private transient int y;
  
  private BadSerializableNoReadResolve() {
    super();
  }
}
