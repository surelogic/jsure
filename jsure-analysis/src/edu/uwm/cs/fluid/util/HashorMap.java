package edu.uwm.cs.fluid.util;

import java.util.HashMap;

public class HashorMap<K,V> {
  private final HashMap<Hashor.Wrapper<K>,V> underlying;
  private final Hashor<K> hashor;

  public HashorMap(Hashor<K> h) {
    underlying = new HashMap<Hashor.Wrapper<K>,V>();
    hashor = h;
  }

  public V get(K key) {
    Hashor.Wrapper<K> wrapped = new Hashor.Wrapper<K>(hashor,key);
    return underlying.get(wrapped);
  }

  public V put(K key, V value) {
    Hashor.Wrapper<K> wrapped = new Hashor.Wrapper<K>(hashor,key);
    return underlying.put(wrapped,value);
  }
  
  public V remove(K key) {
    Hashor.Wrapper<K> wrapped = new Hashor.Wrapper<K>(hashor,key);
    return underlying.remove(wrapped);
  }
}
