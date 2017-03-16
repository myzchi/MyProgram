package com.interceptor;

import com.invocation.ActionInvocation;

public interface Interceptor {
	
	public void init();
	
	public String interceptor(ActionInvocation intvocation);
	
	public void destory();

}
