package itu.beddernet.approuter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Simple multimap, allows several long values to be stored under one key
 *
 */
public class MultiMap {
	Hashtable<Long, ArrayList<Long>> table;
	ArrayList<Long> list;

	public MultiMap(int initialCapacity) {
		table = new Hashtable<Long, ArrayList<Long>>(initialCapacity);
		list = new ArrayList<Long>();
	}

	public MultiMap() {
		table = new Hashtable<Long, ArrayList<Long>>();
		list = new ArrayList<Long>();
	}

	/**
	 * Adds value to key. If key already had value assigned, 
	 * it is added to a list rather than overwriting old value
	 * 
	 * @param key
	 * @param value
	 */
	public void put(Long key, Long value) {
		if (!table.containsKey(key)) {
			ArrayList<Long> list2 = new ArrayList<Long>();
			list2.add(value);
			table.put(key, list2);
		} else {
			table.get(key).add(value);
		}
	}

	public boolean remove(Long key) {
		if (table.remove(key) == null) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Returns list of values attributed with key
	 * @param key
	 * @return
	 */
	public ArrayList<Long> get(Long key) {
		return table.get(key);

	}

	/**
	 * 
	 * @return Object[]<ArrayList<Long>>
	 */
	public Collection<ArrayList<Long>> getValues() {
		return table.values();		
	}
	
	public Set<Entry<Long, ArrayList<Long>>> getSets(){
		return table.entrySet();
	}

}
