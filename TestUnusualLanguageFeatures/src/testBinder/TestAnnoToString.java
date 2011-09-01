package testBinder;

@interface Anno {
	
}

public class TestAnnoToString {
	Anno anno;
	
	public String toString() {
		return super.toString() + anno;
	}
}
