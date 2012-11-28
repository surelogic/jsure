/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/SingletonMap.java,v 1.4 2007/07/10 22:16:30 aarong Exp $*/
package edu.cmu.cs.fluid.util;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Map.Entry;
import com.surelogic.Starts;

/**
 * A map with only one entry.
 * Unfortunately Map and Set are incompatible interfaces, or this would be easier.
 * @author boyland
 */
public class SingletonMap<K, V> implements Map<K, V>, Entry<K,V> {
  K key;
  V value;
  
  public SingletonMap() {
    key = null;
    value = null;
  }
  
  public SingletonMap(K k, V v) {
    key = k;
    value = v;
  }

  @Starts("nothing")
public Set<Map.Entry<K, V>> entrySet() {
    return new AbstractSet<Map.Entry<K,V>>() {

      @Starts("nothing")
	@Override
      public Iterator<Map.Entry<K, V>> iterator() {
        return new Iterator<Map.Entry<K,V>>() {
          boolean started = false;
          public boolean hasNext() {
            return !started;
          }

          public java.util.Map.Entry<K, V> next() {
            if (started) throw new NoSuchElementException("SingletonMap has only one element");
            started = true;
            return SingletonMap.this;
          }

          public void remove() {
            if (started) {
              clear();
            }
          }
          
        };
      }

      @Starts("nothing")
	@Override
      public int size() {
        return SingletonMap.this.size();
      }
      
    };
  }

  @Starts("nothing")
public K getKey() {
    return key;
  }

  @Starts("nothing")
public V getValue() {
    return value;
  }

  public V setValue(V value) {
    V old = this.value;
    this.value = value;
    return old;
  }

  @Starts("nothing")
public void clear() {
    key = null;
    value = null;
  }

  @Starts("nothing")
public boolean containsKey(Object key) {
    return key.equals(this.key);
  }

  @Starts("nothing")
public boolean containsValue(Object value) {
    if (key == null) return false;
    if (value == this.value) return true;
    if (value == null) return false;
    return (value.equals(this.value));
  }

  @Starts("nothing")
public V get(Object key) {
    if (this.key.equals(key)) return value;
    return null;
  }

  @Starts("nothing")
public boolean isEmpty() {
    return key == null;
  }

  @Starts("nothing")
public Set<K> keySet() {
    return new AbstractSet<K>() {

      @Starts("nothing")
	@Override
      public Iterator<K> iterator() {
        return new Iterator<K>() {
          boolean started = false;
          public boolean hasNext() {
            return !started;
          }

          public K next() {
            if (started) throw new NoSuchElementException("SingletonMap has only one element");
            started = true;
            return key;
          }

          public void remove() {
            if (started) {
              clear();
            }
          }
        };
      }

      @Starts("nothing")
	@Override
      public int size() {
        return SingletonMap.this.size();
      }
      
    };
  }

  public V put(K key, V value) {
    if (this.key == null || this.key.equals(key)) {
      V old = this.value;
      this.value = value;
      return old;
    }
    throw new UnsupportedOperationException("Can't add another item to a singleton map!");
  }

  public void putAll(Map<? extends K, ? extends V> t) {
    for (Map.Entry<? extends K,? extends V> e : t.entrySet()) {
      put(e.getKey(),e.getValue());
    }
  }

  @Starts("nothing")
public V remove(Object key) {
    if (key.equals(this.key)) {
      V old = value;
      clear();
      return old;
    }
    return null;
  }

  @Starts("nothing")
public int size() {
    return key == null ? 0 : 1;
  }

  @Starts("nothing")
public Collection<V> values() {
    return new AbstractCollection<V>() {

      @Starts("nothing")
	@Override
      public Iterator<V> iterator() {
        return new Iterator<V>() {
          boolean started = false;
          public boolean hasNext() {
            return !started;
          }

          public V next() {
            if (started) throw new NoSuchElementException("SingletonMap has only one element");
            started = true;
            return value;
          }

          public void remove() {
            if (started) {
              clear();
            }
          }
        };
      }

      @Starts("nothing")
	@Override
      public int size() {
        // TODO Auto-generated method stub
        return 0;
      }
      
    };
  }

}
