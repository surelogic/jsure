/*
 * Created on Aug 5, 2003
 *
 */
package edu.cmu.cs.fluid.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author chance
 *
 */
@SuppressWarnings("all")
public class CustomHashMap2 extends CopiedHashMap2 implements ICustomHashMap2 {
  public static final HashEntryFactory defaultFactory = new DefaultFactory();
  
  private final HashEntryFactory factory;
  
  public CustomHashMap2(HashEntryFactory f) {
    factory = f;
  }
  
  public CustomHashMap2() {
    this(defaultFactory);
  }
  
  public CustomHashMap2(int initialCapacity, float loadFactor, HashEntryFactory f) {
    super(initialCapacity, loadFactor);
    factory = f;
  }

  public CustomHashMap2(Map m, HashEntryFactory f) {
    this(Math.max((int) (m.size() / DEFAULT_LOAD_FACTOR) + 1,
                  DEFAULT_INITIAL_CAPACITY), DEFAULT_LOAD_FACTOR, f);
    putAllForCreate(m);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.util.CustomMap#putEntry(java.util.Map.Entry)
   */
  public void putEntry(Map.Entry me) {
    if (!factory.isValid(me)) {
      return;
    }    
		IHashEntry entry = (IHashEntry) me;
		Object k = maskNull(entry.getKey());
		int hash = hash(k);
		int i = indexFor(hash, table.length);

		for (IHashEntry e = table[i]; e != null; e = e.getNext()) {
				if (e.getHash() == hash && eq(k, e.getKey())) {
          /*
						Object oldValue = e.value;
						e.value = entry.value;
            */
            factory.copy(me, e);
						//e.recordAccess(this);
						// return oldValue;
				}
		}
		modCount++;
		addEntry(entry, i);   
		// return null;
  }

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.util.CustomMap#getEntry(java.lang.Object)
	 */
	public Map.Entry getEntry(Object key) {
		return getEntry0(key);
	}

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.util.CustomMap#getEntryAlways(java.lang.Object)
   */
  public Map.Entry getEntryAlways(Object key) {
		Object k = maskNull(key);
		int hash = hash(k);
		int i = indexFor(hash, table.length);
		IHashEntry e = table[i]; 
		 while (e != null && !(e.getHash() == hash && eq(k, e.getKey())))
				 e = e.getNext();
				 
		 if (e == null) {
		 	 e = factory.create(key, null, hash);
       modCount++;
       addEntry(e, i);       
		 }
		return e;
  }
  
  @Override
  protected IHashEntry makeEntry(int hash, Object key, Object value, int bucketIndex) {
    IHashEntry e = factory.create(key, value, hash);
    e.setNext(table[bucketIndex]);
    return e;
  }  
  
  private static class DefaultFactory implements HashEntryFactory {
		/* (non-Javadoc)
		 * @see edu.cmu.cs.fluid.util.ICustomHashMap.HashEntryFactory#create(java.lang.Object, java.lang.Object, int)
		 */
		public IHashEntry create(Object key, Object value, int hash) {
      return new CopiedHashMap2.Entry(hash, key, value);
		}

		/* (non-Javadoc)
		 * @see edu.cmu.cs.fluid.util.ICustomHashMap.HashEntryFactory#copy(java.util.Map.Entry, java.util.Map.Entry)
		 */
		public void copy(Map.Entry from, Map.Entry to) {
		  to.setValue(from.getValue());
		}

		/* (non-Javadoc)
		 * @see edu.cmu.cs.fluid.util.ICustomMap.EntryFactory#create(java.lang.Object, java.lang.Object)
		 */
		public Map.Entry create(Object key, Object value) {
      return new CopiedHashMap2.Entry(key, value);
		}

		/* (non-Javadoc)
		 * @see edu.cmu.cs.fluid.util.ICustomMap.EntryFactory#isValid(java.util.Map.Entry)
		 */
		public boolean isValid(Map.Entry entry) {
			return (entry instanceof IHashEntry);
		}
  }
  
  static class TestEntry extends CopiedHashMap2.Entry {
    boolean flag;
    
		/**
		 * @param hash
		 * @param key
		 * @param val
		 */
		public TestEntry(int hash, Object key, Object val, boolean f) {
			super(hash, key, val);
      flag = f;
		}

    public TestEntry(Object key, Object val, boolean f) {
      super(key, val);
      flag = f;
    }
  }
  
  static String[] testData = {
    "hello",
    "goodbye",
    "whatsup",
    "tired",
    "huh",
  };
  
  public static void main(String[] args) {
    HashEntryFactory factory = new HashEntryFactory() {
			public IHashEntry create(Object key, Object value, int hash) {
        return new TestEntry(hash, key, value, false);
			}

			public void copy(Map.Entry from, Map.Entry to) {
				TestEntry e0 = (TestEntry) from;
        TestEntry e1 = (TestEntry) to;
				e1.value = e0.value;
        e1.flag  = e0.flag;
			}

			public Map.Entry create(Object key, Object value) {
        return new TestEntry(key, value, false);
			}

			public boolean isValid(Map.Entry entry) {
				return (entry instanceof TestEntry);
			}
    };
    
    Map m         = new HashMap();
    ICustomMap m2 = new CustomHashMap2(factory);
    for(int i=0; i<testData.length; i++) {    
      m.put(testData[i], testData[i]);

      Map.Entry e = m2.getEntryAlways(testData[i]);
      e.setValue(testData[i]);
    }    
    Iterator it = m.keySet().iterator();
    while (it.hasNext()) {
      Object o = it.next();
      System.out.println(m.get(o)+" ?= "+m2.get(o));
    }

    for(int i=0; i<testData.length; i++) {    
      TestEntry e = new TestEntry(testData[i], testData[i], (testData[i].hashCode() % 2) == 1);                  
      m2.putEntry(e);
    }    
    it = m.keySet().iterator();
    while (it.hasNext()) {
      Object o = it.next();
      System.out.println(m.get(o)+" ?= "+m2.get(o)+" : "+((TestEntry) m2.getEntry(o)).flag);
    }
  }
}
