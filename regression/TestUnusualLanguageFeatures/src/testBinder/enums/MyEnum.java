package testBinder.enums;

public enum MyEnum {
	First, Second, Third() {
		public String toString() {
			return "Something else";
		}
	}
}
