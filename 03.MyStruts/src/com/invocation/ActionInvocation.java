package com.invocation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.config.ActionConfig;
import com.context.ActionContext;
import com.interceptor.Interceptor;

/**
 * ActionInvocation负责完成拦截器链状调用以及action调用，以及数据中心（ActionContext）的提供
 * @author Ethan
 *
 */
public class ActionInvocation {
	//过滤器链
	private Iterator<Interceptor> inteceptors;
	//即将调用的action实例
	private Object action;
	//action配置信息
	private ActionConfig config;
	//数据中心
	private ActionContext ac;
	
	public  ActionInvocation(List<String> InteceptorClassNames, ActionConfig config,
			HttpServletRequest request,HttpServletResponse response){
		List<Interceptor> inteceptorList = null;
		//1准备Inteceptor链
		if(InteceptorClassNames != null && InteceptorClassNames.size()>0){
			inteceptorList =new ArrayList<Interceptor>();
			for (String className : InteceptorClassNames) {
				Interceptor interceptor;
				try {
					interceptor = (Interceptor) Class.forName(className).newInstance();
					interceptor.init();
				} catch (Exception e) {
					e.printStackTrace();
					throw new RuntimeException("创建interceptor失败");
				} 				
				inteceptorList.add(interceptor);
			}
			this.inteceptors = inteceptorList.iterator();
		}
		
		//2访问action实例
        this.config = config;
        
        try {
			action = Class.forName(config.getClassName()).newInstance();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("action创建失败");
		} 
				
		//3准备数据中心actionContext
        ac = new ActionContext(request,response,action);
        
		
	}
	
	public ActionContext getActionContext(){
		return ac;
	}
	

	public String invoke(ActionInvocation invocation){
		
		//1准备一个变量，接收action结果的路由串
		String result = null;
		
		//2判断拦截器链中是否有下一个拦截器，同时判断拦截器是是否返回了结果串，即变量是否被赋值
		if(inteceptors!=null && inteceptors.hasNext() && result == null){
			//如果有，则调用下一个拦截的拦截方法
			Interceptor it = inteceptors.next();
			result = it.interceptor(invocation);
		}else{
			//没有，调用action实例的处理方法
			//获得将要调用的action方法名称
			String methodName = config.getMethod();
			//根据action对象和方法名称获得对应的method对象
			try {
				Method excuteMethod = action.getClass().getMethod(methodName);
				result = (String) excuteMethod.invoke(action);
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("配置的action方法不存在");
			}
		}		   
		//3将action
		return result;
	}
	
	
	
	

}
