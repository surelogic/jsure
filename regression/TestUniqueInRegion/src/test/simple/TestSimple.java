package test.simple;

import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.UniqueInRegion;

@Regions({
  @Region("public A"),
  @Region("public B extends A"),
  @Region("public static S")
})
@SuppressWarnings("unused")
public class TestSimple {
  public final int finalField = 0;
  public volatile int volatileField;
  public int normalField;
  
  
  // === Field must have reference type
  
  // GOOD: Field is reference typed
  @UniqueInRegion("Instance")
  private final Object good1 = new Object();
  
  // GOOD: Field is reference typed
  @UniqueInRegion("Instance")
  private final int[] good2 = { 0 };
  
  // BAD: Field is primitive
  @UniqueInRegion("Instance")
  private final int bad1 = 0;

  // BAD: Field cannot be volatile
  @UniqueInRegion("Instance")
  private volatile Object bad2 = new Object();
  
  // GOOD: Field is non-final, non-volatile
  @UniqueInRegion("Instance")
  private Object good3 = new Object();
  


  // === Region must exist

  // GOOD: Exists
  @UniqueInRegion("Instance")
  private final Object good10 = new Object();
  
  // GOOD: Exists
  @UniqueInRegion("A")
  private final Object good11 = new Object();
  
  // GOOD: Exists
  @UniqueInRegion("B")
  private final Object good12 = new Object();
  
  // BAD: Doesn't exist
  @UniqueInRegion("NON_EXISTENT")
  private final Object bad10 = new Object();



  // === Region must not be final or volatile

  // GOOD: Abstract region
  @UniqueInRegion("A")
  private final Object good20 = new Object();
  
  // GOOD: normal field
  @UniqueInRegion("normalField")
  private final Object good21 = new Object();
  
  // BAD: Final
  @UniqueInRegion("finalField")
  private final Object bad20 = new Object();
  
  // BAD: volatile
  @UniqueInRegion("volatileField")
  private final Object bad21 = new Object();
  
  
  
  // === Region must be static if the field is static
  
  // GOOD: instance field, instance region
  @UniqueInRegion("A")
  private final Object good30 = new Object();
  
  // GOOD: instance field, static region
  @UniqueInRegion("S")
  private final Object good31 = new Object();
  
  // GOOD: static field, static region
  @UniqueInRegion("S")
  private final static Object good32 = new Object();
  
  // BAD: static field, instance region
  @UniqueInRegion("A")
  private final static Object bad30 = new Object();
}
