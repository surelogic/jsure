/*$Header: /cvs/fluid/fluid/src/com/surelogic/xml/TypeErasure.java,v 1.1 2007/08/01 20:27:48 swhitman Exp $*/
package com.surelogic.xml;

import com.surelogic.common.SLUtility;

import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;


public class TypeErasure {

	/**
	 * 
	 * @param type
	 * @return a string representation of the erasure of type
	 */
	public static String calcTypeErasure(String type, ITypeEnvironment tEnv) 
	{
		/** 
		 * The Erasure of a type T is written as |T|
		 * -The erasure of a parameterized type G<T1, ... ,Tn> is |G|.
		 * -The erasure of a nested type T.C is |T|.C.
		 * -The erasure of an array type T[] is |T|[].
		 * -The erasure of a type variable is the erasure of its leftmost 
		 *  bound.
		 * 	 -The erasure of a type variable is determined by the first type in 
		 *    its bound, and that a class type or type variable may only appear 
		 *    in the first position.
		 * -The erasure of every other type is the type itself.
		 */

		/** Split by space, '<', and '>' chars */
		String [] parts = type.split("<|>|&lt;|&gt;|\\s");
		
//		System.out.println("Parts:");
//		for(int i = 0; i < parts.length; i++)
//			System.out.println(parts[i] + " ");
//		System.out.println("\\n");
		
		if (parts.length <= 1 || !"extends".equals(parts[1])) {
			try {
				IJavaType javaType = tEnv.findJavaTypeByName(parts[0]);

				/* This is probably not a generic if this worked */
				if(javaType != null)
					return parts[0];			

			} catch (NullPointerException n) {
				// Just means this type was not found
			}
		} else {
			// Definitely a type variable
		}
		for(int i = 1; i < parts.length; i++) {
			
			if(parts[i].equals("extends") && (i + 1 < parts.length)) {
				StringBuffer newType = new StringBuffer();
				for(i = i + 1; i < parts.length; i++) {
					newType.append(parts[i] + " ");
				}
				return calcTypeErasure(newType.toString(), tEnv);
			}
//			System.out.println("parts["+i+"] is " + parts[i] + " != extends");
		}
		
		return SLUtility.JAVA_LANG_OBJECT;
	}
	
	public static String calcArgErasure(String args, ITypeEnvironment tEnv) {
		//Split the arguments
		//Calculate the erasure of the types of each argument

		return "";
	}
	
	

}
