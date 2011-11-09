package misc.anonClassMethods;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.RegionLock;
import com.surelogic.RequiresLock;

@RegionLock("T is lockT protects t")
public class Test {
  public final Lock lockT = new ReentrantLock();
  public int t;
   
  @RegionLock("C is lockC protects c")
  public class Container {
    public final Lock lockC = new ReentrantLock();
    public int c;
    
    @RegionLock("F is lockF protects f")
    public abstract class Super {
      public final Lock lockF = new ReentrantLock();
      public int f;
      
      public Super() {
      }
    }
    
    @RegionLock("M1 is lockM1 protects m1")
    public class Middle1 {
      public final Lock lockM1 = new ReentrantLock();
      public int m1;
      
      @RegionLock("M2 is lockM2 protects m2")
      public class Middle2 {
        public final Lock lockM2 = new ReentrantLock();
        public int m2;
       
        public void Middle2(int x, final Container other) {
          /* The immediately enclosing instance of s6 is "this" (a Middle2 object)
           * The immediately enclosing instance with respect to Super is other.
           */
          final Super s6 = other. new Super() {
            @RequiresLock("Container.this:C, Middle2.this:M2")
            public void method2() {
              lockM1.lock();
              try {
                Middle1.this.m1 = 20;
              } finally {
                lockM1.unlock();
              }
              Middle2.this.m2 = 30;
              Test.this.lockT.lock();
              try {
                Test.this.t += 1;
              } finally {
                Test.this.lockT.unlock();
              }
              Container.this.c = 9;
            }
          };
        }
        
        public void stuff2(final Container other) {
          /* The immediately enclosing instance of s6 is "this" (a Middle2 object)
           * The immediately enclosing instance with respect to Super is other.
           */
          final Super s6 = other. new Super() {
            @RequiresLock("Container.this:C, Middle2.this:M2")
            public void method2() {
              lockM1.lock();
              try {
                Middle1.this.m1 = 20;
              } finally {
                lockM1.unlock();
              }
              Middle2.this.m2 = 30;
              Test.this.lockT.lock();
              try {
                Test.this.t += 1;
              } finally {
                Test.this.lockT.unlock();
              }
              Container.this.c = 9;
            }
          };
        }
      }
    }
  }
}
