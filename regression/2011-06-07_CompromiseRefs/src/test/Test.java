package test;
import com.surelogic.RegionEffects;
import com.surelogic.Unique;


// Test the error reporting in the StoreLattice.check() method

// Also test that we can (later) figure out which fields are lost...
public class Test {
  private @Unique Object u;
  private @Unique Object v;
  private @Unique Object w;
  
  
  
  @RegionEffects("none")
  private static void compromiseRef(final Object o) {
    // do nothing
  }

  
  
  private void bad1() {
	  compromiseRef(u);
	  compromiseRef(v);
	  compromiseRef(w);
	  // u, v, and w are lost
  }
  
  private void bad2() {
	  Object newObject = new Object();
	  try {
		compromiseRef(u);
	  } finally {
		u = newObject;
	  }

	  compromiseRef(v);
	  compromiseRef(w);

	  // v, and w are lost
  }

  private void bad3() {
	  Object newObject = new Object();
	  try {
		compromiseRef(u);
	  } finally {
		u = newObject;
	  }
	  
	  newObject = new Object();
	  try {
		compromiseRef(v);
	  } finally {
		v = newObject;
	  }
	  
	  compromiseRef(w);

	  // w is lost
  }

  private void good() {
	  Object newObject = new Object();
	  try {
		compromiseRef(u);
	  } finally {
		u = newObject;
	  }
	  
	  newObject = new Object();
	  try {
		compromiseRef(v);
	  } finally {
		v = newObject;
	  }
	  
	  newObject = new Object();
	  try {
		compromiseRef(w);
	  } finally {
		w = newObject;
	  }
  }

}


