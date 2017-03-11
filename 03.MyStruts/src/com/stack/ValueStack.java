package com.stack;

import java.util.ArrayList;
import java.util.List;

public class ValueStack {
	
	private List<Object> list = new ArrayList<Object>();
	
	//弹栈
	public Object pop(){
		return list.remove(0);
	}
	
	
	//压栈
	public void push(Object o){
		list.add(0,o);
	}
	
	//取出顶部对象
	public Object seek(){
		return list.get(0);
	}
	

}
