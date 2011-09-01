package testBinder;

import java.util.*;

public class TestCaptureInForEachLoop {
	class BidirectionalHashMap<K,V> extends HashMap<K,V> {		  
		public void putAll(Map<? extends K, ? extends V> m) {
			if (m == null) {
				return;
			}
			for(Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
				this.put(e.getKey(), e.getValue());
			}
			/*
			Iterator<?> it = m.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<? extends K, ? extends V> e = 
					(Map.Entry<? extends K, ? extends V>) it.next();
				this.put(e.getKey(), e.getValue());
			}
			
			Iterable<?> i = m.entrySet();
			Iterator<?> it2 = i.iterator();
			while (it2.hasNext()) {
				Map.Entry<? extends K, ? extends V> e = 
					(Map.Entry<? extends K, ? extends V>) it.next();
				this.put(e.getKey(), e.getValue());
			}
			*/
		}
	}
}