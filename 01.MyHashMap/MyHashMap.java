package hash;

public class MyHashMap {
	
	//初始数组大小
	private static final int size = 16;
	private Entry[] table = new Entry[size];
	
	class Entry{
		String key;
		String value;
		Entry next;
		
		Entry(String k,String v){
			key = k;
			value = v;
		}
	}
	
	public Entry get(String k){
		int hash = k.hashCode() % size;
		Entry e = table[hash];
		
		while(e!=null){
			//如果原来有值，则前值被替换掉
			if(e.key.equals(k)){
				return e;
			}
			e = e.next;
		}	
		return null;
	}
	
	public void put(String k,String v){
		int hash = k.hashCode() % size;
		Entry e = table[hash];
		if(e!=null){
			while(e.next != null){
				if (e.key.contains(k)){
					e.value = v;
					return ;
				}
				e = e.next;
			}
			Entry entryInOldBucket = new Entry(k,v);
			table[hash] = entryInOldBucket;
		}else{
			Entry entryInNewBucket = new Entry(k,v);
			table[hash] = entryInNewBucket;
		}
	}
	

}
