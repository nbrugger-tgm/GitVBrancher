package com.niton.gitbrancher.model.algo;

import java.util.*;

public class TwoGradeIdMap<K extends TwoGradeIdMap.Identifiable,KE extends Enum<KE>,V extends TwoGradeIdMap.Keyable<K,KE>> {
	public interface Keyable<K,KE> {
		K getKey();
		KE getEnumKey();
	}

	/**
	 * Instance of Classes implementing this interface will be able to produce a 100% unique identity
	 */
	public interface Identifiable  {
		/**
		 * @return a identity which is 100% unique for each object with different values in it. keep the size of the returned data as small as possible (preferably,int or long or if neccesary String)
		 */
		Object identity();
	}
	private Object[] data = new Object[0];
	private final Map<Object,Integer> index = new HashMap<>();
	private int nextIndex = 0;
	public void insert(V entry){
		K key = entry.getKey();
		KE eKey = entry.getEnumKey();
		int pIndex;
		index.put(key.identity(),pIndex = nextIndex++);
		int subIndex = eKey.ordinal();
		if(pIndex+subIndex>=data.length){
			enlargeArray(eKey.getClass().getEnumConstants().length);
		}
		data[pIndex+subIndex] = entry;
	}

	private void enlargeArray(int length) {
		Object[] newArray = new Object[data.length+length];
		System.arraycopy(data,0,newArray,0,data.length);
		data = newArray;
	}

	public V get(K key,KE keyEnum){
		return (V)data[indexOf(key, keyEnum)];
	}
	public void remove(V value){
		remove(value.getKey(), value.getEnumKey());
	}
	public void remove(K key,KE keyEnum){
		data[indexOf(key,keyEnum)] = null;
	}
	private int indexOf(K key,KE eKey) {
		return (index.get(key.identity())+eKey.ordinal());
	}

}
