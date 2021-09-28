package com.strandls.taxonomy.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.strandls.taxonomy.pojo.Rank;
import com.strandls.taxonomy.service.RankSerivce;
import com.strandls.taxonomy.service.exception.UnRecongnizedRankException;
import com.strandls.utility.ApiException;
import com.strandls.utility.controller.UtilityServiceApi;
import com.strandls.utility.pojo.ParsedName;

public class TaxonomyCache {

	private List<Rank> ranks;

	private Map<String, TaxonomyParsedNameCache> rankToCache;
	private static long cacheHit = 0;
	private static long cacheMiss = 0;

	@Inject
	public TaxonomyCache(RankSerivce rankSerivce, UtilityServiceApi utilityServiceApi) {
		ranks = rankSerivce.getAllRank(null);
		rankToCache = new HashMap<>();
		for (Rank rank : ranks) {
			int size = (int) (rank.getRankValue() * 5);
			size = size == 0 ? 1 : size;
			rankToCache.put(rank.getName(), new TaxonomyParsedNameCache(utilityServiceApi, size));
		}
	}

	public ParsedName getName(String rank, String name) throws UnRecongnizedRankException {
		if (!rankToCache.containsKey(rank))
			throw new UnRecongnizedRankException("Unknown rank");

		TaxonomyParsedNameCache cache = rankToCache.get(rank);
		return cache.getValue(name);
	}

	public List<Rank> getRanks() {
		return ranks;
	}

	public static void increamentCacheHit() {
		cacheHit++;
	}

	public static void increamentCacheMiss() {
		cacheMiss++;
	}

	public static long getCacheHit() {
		return cacheHit;
	}

	public static long getCacheMiss() {
		return cacheMiss;
	}
}

class TaxonomyParsedNameCache extends Cache<String, ParsedName> {

	private UtilityServiceApi utilityServiceApi;

	public TaxonomyParsedNameCache(UtilityServiceApi utilityServiceApi, int size) {
		super(size);
		this.utilityServiceApi = utilityServiceApi;
	}

	@Override
	public ParsedName getNewValue(String k) {
		try {
			return utilityServiceApi.getNameParsed(k);
		} catch (ApiException e) {
			return null;
		}
	}
}

abstract class Cache<K, V> {

	private Map<K, DLLNode<K, V>> hashTable;
	private DoublyLinkedList<K, V> doublyLinkedList;

	protected Cache(int size) {
		hashTable = new HashMap<>();
		doublyLinkedList = new DoublyLinkedList<>(size);
	}

	public V getValue(K k) {
		DLLNode<K, V> node;
		if (hashTable.containsKey(k)) {
			TaxonomyCache.increamentCacheHit();
			node = hashTable.get(k);
			if (node.getValue() == null) {
				doublyLinkedList.remove(node);
				hashTable.remove(node);
				return getValue(k);
			}
			doublyLinkedList.remove(node);
			doublyLinkedList.addLast(node);
		} else {
			TaxonomyCache.increamentCacheMiss();
			V v = getNewValue(k);
			node = new DLLNode<>(k, v);
			if (doublyLinkedList.isFull()) {
				DLLNode<K, V> firstNode = doublyLinkedList.removeFirst();
				hashTable.remove(firstNode.getKey());
				doublyLinkedList.addLast(node);
				hashTable.put(k, node);
			} else {
				doublyLinkedList.addLast(node);
				hashTable.put(k, node);
			}
		}
		return node.getValue();
	}

	public abstract V getNewValue(K k);
}

class DLLNode<K, V> {
	private DLLNode<K, V> prev;
	private DLLNode<K, V> next;
	private K key;
	private V value;

	public DLLNode(K k, V v) {
		this.key = k;
		this.value = v;
		this.next = null;
		this.prev = null;
	}

	public K getKey() {
		return key;
	}

	public V getValue() {
		return value;
	}

	public DLLNode<K, V> getPrev() {
		return prev;
	}

	public void setPrev(DLLNode<K, V> prev) {
		this.prev = prev;
	}

	public DLLNode<K, V> getNext() {
		return next;
	}

	public void setNext(DLLNode<K, V> next) {
		this.next = next;
	}
}

class DoublyLinkedList<K, V> {

	private DLLNode<K, V> first;
	private DLLNode<K, V> last;
	private int size;
	private int currSize;

	public DoublyLinkedList(int size) {
		this.size = size;
	}

	public boolean isFull() {
		return currSize >= size;
	}

	public DLLNode<K, V> remove(DLLNode<K, V> node) {
		if (currSize == 0) {
			throw new IllegalArgumentException("Can't remove from the empty list");
		}

		if (node.getNext() != null)
			node.getNext().setPrev(node.getPrev());
		else
			last = node.getPrev();

		if (node.getPrev() != null)
			node.getPrev().setNext(node.getNext());
		else
			first = node.getNext();
		currSize--;

		node.setNext(null);
		node.setPrev(null);
		return node;
	}

	public DLLNode<K, V> addLast(DLLNode<K, V> node) {
		node.setPrev(last);
		node.setNext(null);

		if (last == null) {
			first = node;
			last = node;
		} else {
			last.setNext(node);
			last = node;
		}

		currSize++;
		return node;
	}

	public DLLNode<K, V> removeFirst() {
		if (currSize == 0) {
			throw new IllegalArgumentException("Can't remove from the empty list");
		}
		DLLNode<K, V> node = first;
		first = first.getNext();
		if (first == null)
			last = null;
		node.setNext(null);
		node.setPrev(null);
		currSize--;
		return node;
	}
}
