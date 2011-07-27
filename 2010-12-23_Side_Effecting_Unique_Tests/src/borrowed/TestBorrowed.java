package borrowed;

import com.surelogic.Borrowed;
import com.surelogic.RegionEffects;
import com.surelogic.Unique;

public class TestBorrowed {
	@Unique
	private Other uniqueField;
	
	
	
	@RegionEffects("reads t:Instance")
	public static void borrowedParam(@Borrowed Other t) {}
	
	public static void killUnique(Other t) {}
	
	
	public void bad1() {
		// First one is bad: opBorrow() bad via popReceiver() 
		this.uniqueField.borrowedReceiver(
				this.uniqueField);
	}
	
	public void bad2() {
		// First one is bad: opBorrow() bad via popReceiver() 
		borrowedReceiver2(
				this.uniqueField,
				this.uniqueField);
	}

	
	public void bad1a() {
		Other a = this.uniqueField;
		// First one is bad: opBorrow() bad via popReceiver() 
		a.borrowedReceiver(
				this.uniqueField);
	}
	
	public void bad2a() {
		Other a = this.uniqueField;
		// First one is bad: opBorrow() bad via popReceiver() 
		borrowedReceiver2(
				a, 
				this.uniqueField);
	}

	
	@RegionEffects("none")
	public static void borrowedReceiver2(@Borrowed Other a, @Borrowed Other b) {}
}



class Other {
	@Unique("return")
	@RegionEffects("none")
	public Other() {}
	
	
	
	@Borrowed("this")
	@RegionEffects("reads nothing")
	public void borrowedReceiver(@Borrowed Other o) {
		// blah
	}
}
