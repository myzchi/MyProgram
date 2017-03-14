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
 * ActionInvocation���������������״�����Լ�action���ã��Լ��������ģ�ActionContext�����ṩ
 * @author Ethan
 *
 */
public class ActionInvocation {
	//��������
	private Iterator<Interceptor> inteceptors;
	//�������õ�actionʵ��
	private Object action;
	//action������Ϣ
	private ActionConfig config;
	//��������
	private ActionContext ac;
	
	public  ActionInvocation(List<String> InteceptorClassNames, ActionConfig config,
			HttpServletRequest request,HttpServletResponse response){
		List<Interceptor> inteceptorList = null;
		//1׼��Inteceptor��
		if(InteceptorClassNames != null && InteceptorClassNames.size()>0){
			inteceptorList =new ArrayList<Interceptor>();
			for (String className : InteceptorClassNames) {
				Interceptor interceptor;
				try {
					interceptor = (Interceptor) Class.forName(className).newInstance();
					interceptor.init();
				} catch (Exception e) {
					e.printStackTrace();
					throw new RuntimeException("����interceptorʧ��");
				} 				
				inteceptorList.add(interceptor);
			}
			this.inteceptors = inteceptorList.iterator();
		}
		
		//2����actionʵ��
        this.config = config;
        
        try {
			action = Class.forName(config.getClassName()).newInstance();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("action����ʧ��");
		} 
				
		//3׼����������actionContext
        ac = new ActionContext(request,response,action);
        
		
	}
	
	public ActionContext getActionContext(){
		return ac;
	}
	

	public String invoke(ActionInvocation invocation){
		
		//1׼��һ������������action�����·�ɴ�
		String result = null;
		
		//2�ж������������Ƿ�����һ����������ͬʱ�ж����������Ƿ񷵻��˽�������������Ƿ񱻸�ֵ
		if(inteceptors!=null && inteceptors.hasNext() && result == null){
			//����У��������һ�����ص����ط���
			Interceptor it = inteceptors.next();
			result = it.interceptor(invocation);
		}else{
			//û�У�����actionʵ���Ĵ�����
			//��ý�Ҫ���õ�action��������
			String methodName = config.getMethod();
			//����action����ͷ������ƻ�ö�Ӧ��method����
			try {
				Method excuteMethod = action.getClass().getMethod(methodName);
				result = (String) excuteMethod.invoke(action);
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("���õ�action����������");
			}
		}		   
		//3��action
		return result;
	}
	
	
	
	

}
