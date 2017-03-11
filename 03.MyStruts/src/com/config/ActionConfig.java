package com.config;

import java.util.HashMap;
import java.util.Map;

/**
 * action比较多，封装成一个对象
 * @author Ethan
 *
 */
public class ActionConfig {
	
	private String name;
	
	private String method;
	
	private String className;
	
	private Map<String, String> result = new HashMap<String, String>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public Map<String, String> getResult() {
		return result;
	}

	public void setResult(Map<String, String> result) {
		this.result = result;
	}
	
	
	

}
