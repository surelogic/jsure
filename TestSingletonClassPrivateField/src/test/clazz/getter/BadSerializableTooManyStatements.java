package test.clazz.getter;

import java.io.Serializable;

import com.surelogic.Singleton;

@Singleton
public final class BadSerializableTooManyStatements implements Serializable {
  private static final BadSerializableTooManyStatements INSTANCE = new BadSerializableTooManyStatements();
  
  private transient int x;
  private transient int y;
  
  public static BadSerializableTooManyStatements getInstance() {
    return INSTANCE;
  }
  
  private BadSerializableTooManyStatements() {
    super();
  }
  
  private Object readResolve() {
    x += 1;
    return INSTANCE;
  }
}
