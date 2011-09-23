package test.enums;

import com.surelogic.Singleton;

@Singleton
public enum ManyMembers_Fancy {
  DEFAULT,
  SMALL(5),
  BIG(15),
  SUPER(20),
  SUPER_DUPER(20) {
    @Override
    public int getSize() {
      return super.getSize() * 10;
    }
  };
  
  
  
  private static final int DEFAULT_SIZE = 10; 
  
  private final int size;
  
  private ManyMembers_Fancy(final int s) {
    size = s;
  }
  
  private ManyMembers_Fancy() {
    this(DEFAULT_SIZE);
  }
  
  public int getSize() {
    return size;
  }
}
