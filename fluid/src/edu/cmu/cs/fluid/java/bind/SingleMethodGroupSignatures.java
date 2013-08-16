package edu.cmu.cs.fluid.java.bind;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import com.surelogic.common.util.AppendIterator;
import com.surelogic.common.util.SingletonIterator;

/**
 * A representation of a collection of method signatures
 * all with the same name and number of parameters.
 * @author boyland
 */
public interface SingleMethodGroupSignatures extends Collection<IJavaFunctionType> {
	public String getName();
	public int getArity();
	public SingleMethodGroupSignatures add(SingleMethodGroupSignatures other);

	public static class EmptyMethodGroupSignatures 
	extends AbstractCollection<IJavaFunctionType> 
	implements SingleMethodGroupSignatures
	{
		public static SingleMethodGroupSignatures instance =
				new EmptyMethodGroupSignatures();

		@Override
		public String getName() {
			return null;
		}

		@Override
		public int getArity() {
			return -1;
		}

		@Override
		public SingleMethodGroupSignatures add(SingleMethodGroupSignatures other) {
			return other;
		}

		@Override
		public Iterator<IJavaFunctionType> iterator() {
			return Collections.<IJavaFunctionType>emptyList().iterator();
		}

		@Override
		public int size() {
			return 0;
		}
	}
	
	public static class SingletonMethodGroupSignature
	extends AbstractCollection<IJavaFunctionType>
	implements SingleMethodGroupSignatures
	{
		private final String name;
		private final IJavaFunctionType signature;
		
		public SingletonMethodGroupSignature(String n, IJavaFunctionType sig) {
			name = n;
			signature = sig;
		}
		
		@Override
		public String getName() {
			return name;
		}

		@Override
		public int getArity() {
			return signature.getParameterTypes().size();
		}

		@Override
		public SingleMethodGroupSignatures add(SingleMethodGroupSignatures other) {
			if (other.size() == 0) return this;
			if (other.getName().equals(name) && other.getArity() == getArity()) {
				return new JoinedMethodGroupSignatures(this,other);
			}
			return null;
		}

		@Override
		public Iterator<IJavaFunctionType> iterator() {
			return new SingletonIterator<IJavaFunctionType>(signature);
		}

		@Override
		public int size() {
			return 1;
		}
	}
	
	static class JoinedMethodGroupSignatures
	extends AbstractCollection<IJavaFunctionType>
	implements SingleMethodGroupSignatures
	{

		private final SingleMethodGroupSignatures sigs1, sigs2;
		private final int size;
		
		JoinedMethodGroupSignatures(SingleMethodGroupSignatures s1, SingleMethodGroupSignatures s2) {
			if (!s1.getName().equals(s2.getName()) ||
					s1.getArity() != s2.getArity()) {
				throw new IllegalArgumentException("can only combine if name and arity are the same");
			}
			sigs1 = s1;
			sigs2 = s2;
			size = s1.size() + s2.size();
		}
		
		@Override
		public String getName() {
			return sigs2.getName();
		}

		@Override
		public int getArity() {
			return sigs1.getArity();
		}

		@Override
		public SingleMethodGroupSignatures add(SingleMethodGroupSignatures other) {
			if (other.size() == 0) return this;
			if (other.getName().equals(getName()) && other.getArity() == getArity()) {
				return new JoinedMethodGroupSignatures(this,other);
			}
			return null;			
		}

		@Override
		public Iterator<IJavaFunctionType> iterator() {
			return new AppendIterator<IJavaFunctionType>(sigs1.iterator(),sigs2.iterator());
		}

		@Override
		public int size() {
			return size;
		}
		
	}
}
