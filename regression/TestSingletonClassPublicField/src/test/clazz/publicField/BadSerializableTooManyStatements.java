package test.clazz.publicField;

import java.io.Serializable;

import com.surelogic.Singleton;

@Singleton
public final class BadSerializableTooManyStatements implements Serializable {
  public static final BadSerializableTooManyStatements INSTANCE = new BadSerializableTooManyStatements();
  
  private transient int x;
  private transient int y;
  
  private BadSerializableTooManyStatements() {
    super();
  }
  
  private Object readResolve() {
    x += 1;
    return INSTANCE;
  }
}
