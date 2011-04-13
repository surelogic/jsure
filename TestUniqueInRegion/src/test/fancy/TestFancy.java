package test.fancy;

import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.UniqueInRegion;

@Regions({
  @Region("public A"),
  @Region("public B extends A"),
  @Region("public static S")
})
@SuppressWarnings("unused")
public class TestFancy {
  public final int finalField = 0;
  public volatile int volatileField;
  public int normalField;
  
  
  // === Field must have reference type
  
  // GOOD: Field is reference typed
  @UniqueInRegion("Instance into Instance")
  private final Object good1 = new Object();
  
  // GOOD: Field is reference typed
  @UniqueInRegion("Instance into Instance")
  private final int[] good2 = { 0 };
  
  // BAD: Field is primitive
  @UniqueInRegion("Instance into Instance")
  private final int bad1 = 0;
  
  
  
  // === Field must be final
  
  // GOOD: Field is final
  @UniqueInRegion("Instance into Instance")
  private final Object good10 = new Object();
  
  // BAD: Field is not final
  @UniqueInRegion("Instance into Instance")
  private Object bad10 = new Object();
  
  
  
  // === Regions must exist
  
  // BAD: X does not exist
  @UniqueInRegion("Instance into X")
  private final Object bad20 = new Object();
  
  // BAD: Y does not exist
  @UniqueInRegion("Y into Instance")
  private final Object bad21 = new Object();
  
  // BAD: X and Y do not exist
  @UniqueInRegion("X into Y")
  private final Object bad22 = new Object();
  
  
  
  // === Source region must not be static
  
  // GOOD: Not static
  @UniqueInRegion("Instance into A")
  private final Object good30 = new Object();
  
  // BAD: Static
  @UniqueInRegion("Instance into Instance, S into A")
  private final C bad30 = new C(1, 1);
  
  
  
  // === Source regions cannot be duplicated
  
  @UniqueInRegion("Instance into Instance, Instance into A")
  private final Object bad40 = new Object();
  
  @UniqueInRegion("Instance into Instance, R1 into A, R1 into B")
  private final C bad41 = new C(0, 1);
  
  
  
  // === Dest regions cannot be final or volatile
  
  // GOOD: non-final, non-volatile region
  @UniqueInRegion("Instance into normalField")
  private final Object good50 = new Object();
  
  // BAD: volatile region
  @UniqueInRegion("Instance into volatileField")
  private final Object bad50 = new Object();
  
  // BAD: final region
  @UniqueInRegion("Instance into finalField")
  private final Object bad51 = new Object();
  
  // GOOD: non-final, non-volatile region
  @UniqueInRegion("Instance into Instance, R1 into normalField")
  private final C good51 = new C(0, 0);
  
  // BAD: volatile region
  @UniqueInRegion("Instance into Instance, R1 into volatileField")
  private final C bad52 = new C(1, 2);
  
  // BAD: final region
  @UniqueInRegion("Instance into Instance, R1 into finalField")
  private final C bad53 = new C(3, 4);

  

  // === Region must be static if the field is static
  
  // GOOD: instance field, instance region
  @UniqueInRegion("Instance into A")
  private final Object good60 = new Object();
  
  // GOOD: instance field, static region
  @UniqueInRegion("Instance into S")
  private final Object good61 = new Object();
  
  // GOOD: static field, static region
  @UniqueInRegion("Instance into S")
  private final static Object good62 = new Object();
  
  // BAD: static field, instance region
  @UniqueInRegion("Instance into A")
  private final static Object bad60 = new Object();
  
  
  
  // Instance region must be mapped
  
  // BAD: No instance region
  @UniqueInRegion("R1 into A, R2 into B")
  private final C bad70 = new C(10, 11);
}
