package testBinder;

import java.util.*;

public class Capture {	
	public static void reverse(List<? extends B> list) { 
		rev(list).isEmpty();
	}
	
	private static <T extends A & I & J> List<T> rev(List<T> list) {
	/*
	        List<T> tmp = new ArrayList<T>(list);
	        for (int i = 0; i < list.size(); i++) {
	        list.set(i, tmp.get(list.size() - i - 1));
	        }
	        */
	        return list;
	}
    
	public static void reverse2(List<? super A> list) {
		rev2(list);
	}
	
	private static <T> void rev2(List<T> list) {		
	}
	
	List<? super List<?>> foo;
	
	void foo() {
		foo.toString();
	}
	
	void bar(Map<?,?> map) {
		map.get("entries");
	}	
}

class A {}

interface I {}

interface J {}

class B extends A implements I, J {}