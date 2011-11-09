package testBinder.enums;

public enum DuplicateEnum {
	Duplicate;
	
	@Override
	public String toString() {
		// Should match the above, instead of the class in the same package
		return Duplicate.name();
	}
}
