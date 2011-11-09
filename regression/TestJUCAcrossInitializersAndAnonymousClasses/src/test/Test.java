package test;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.Borrowed;
import com.surelogic.RegionLock;

@RegionLock("L is lockField protects Instance")
@SuppressWarnings("unused")
public class Test {
  public final Lock lockField = new ReentrantLock();
  
  private int x = 0; // protected for Test(int); unprotected for Test()
  
  {
  	x = 11; // protected for Test(int); unprotected for Test()
  }
  
  { 
  	lockField.lock();
  	try {
  		x = 12; // always protected
  	} finally {
  		lockField.unlock();
  	}
  }
  
  // Non-singlethreaded constructor
  public Test() {
  	x = 1; // unprotected 
  }
  
  // Singlethreaded constructor
  @Borrowed("this")
  public Test(int a) {
  	x = a; // protected
  }

  public void m() {
  	lockField.lock();
  	try {
  		x = 12; // protected
  	} finally {
  		lockField.unlock();
    }
  }
  
  public Test crazy(final int zzz) {
  	return new Test() {
  		{ x = zzz; } // unprotected
  		
  		int q = 10; // unprotected
  	};
  }
  
  public Test crazy2(final int zzz) {
  	return new Test() {
  		int xx;
  		
  		{ 
  			lockField.lock();
  			try {
  				xx = zzz; // protected
  			} finally {
  				lockField.unlock();
  			}
  		}
  		
  		int f1 = 10; // unprotected

  		{ 
  			lockField.lock();
  			try {
  				xx = zzz; // protected
  			} finally {
  				lockField.unlock();
  			}
  		}

  		int f2 = 10; // unprotected

  		{ 
  			lockField.lock();
  			try {
  				xx = zzz; // protected
  			} finally {
  				lockField.unlock();
  			}
  		}

  		int f3 = 10; // unprotected

  		{ 
  			lockField.lock();
  			try {
  				xx = zzz; // protected
  			} finally {
  				lockField.unlock();
  			}
  		}

  		int q = 10; // unprotected
  	};
  }
}
