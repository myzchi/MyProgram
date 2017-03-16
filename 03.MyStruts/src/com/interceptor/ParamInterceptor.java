package com.interceptor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.beanutils.BeanUtils;

import com.context.ActionContext;
import com.invocation.ActionInvocation;
import com.stack.ValueStack;

/**
 * �Զ��������еĲ�����װ��action������
 * @author Ethan
 *
 */
public class ParamInterceptor implements Filter{
	
	public String interceptor (ActionInvocation invocation){
		//1��ò���
		//2���action����
		
		ActionContext ac = invocation.getActionContext();
		ValueStack vs = ac.getStack();
		Object action = vs.seek();
		
		//3��װ
		try {
			BeanUtils.populate(action, ac.getRequest().getParameterMap());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//4����
		return invocation.invoke(invocation);
		
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
		
	}

}
