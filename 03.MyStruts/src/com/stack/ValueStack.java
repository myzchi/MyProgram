package com.stack;

import java.util.ArrayList;
import java.util.List;

public class ValueStack {
	
	private List<Object> list = new ArrayList<Object>();
	
	//��ջ
	public Object pop(){
		return list.remove(0);
	}
	
	
	//ѹջ
	public void push(Object o){
		list.add(0,o);
	}
	
	//ȡ����������
	public Object seek(){
		return list.get(0);
	}
	

}
