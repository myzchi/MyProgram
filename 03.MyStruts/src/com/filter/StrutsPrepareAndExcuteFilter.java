package com.filter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.config.ActionConfig;
import com.config.ConfigurationManager;
import com.context.ActionContext;
import com.invocation.ActionInvocation;

/**
 * 1.��ʼ�������ļ�
 * 2.��������Ҫ���ʵ�action������action������
 * 3.���ݷ����������ActionInvocation���࣬���ݽ��������ת
 * @author Ethan
 *
 */
public class StrutsPrepareAndExcuteFilter implements Filter{
	//�����ļ��еĹ�������Ϣ
	private List<String> interceptorList;
	//struts�����
	private String extension;
	//
	private Map<String, ActionConfig> actionConfigs;

	@Override
	public void destroy() {
		//1׼��������������
		interceptorList = ConfigurationManager.getInterceptor();
				
		//2׼��constant���ã����ʺ�׺��������Ϣ
		extension = ConfigurationManager.getConstant("struts.action.extension");
		
		//3����action����
		actionConfigs = ConfigurationManager.getActionConfig();
		
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
			throws IOException, ServletException {
		//0ǿתrequest��responseΪHtttpServletRequest��response
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		
		
		//1�������·��
		String path = req.getServletPath();//http://localhost:8080/HelloWorld/HelloAction.action
		
		
		//�ж������Ƿ���Ҫ����action
		
		if(!path.endsWith(extension)){
			//����.action��β������Ҫ����action��Դ��ֱ�ӵ���chain.doFilter
			chain.doFilter(request, response);
			return;
		}else{
			//��׺��.action��β������Ҫ����action
			//3�����Ҫ���ʵ�action
			path = path.substring(1);
			path = path.replace("."+extension, "");
			//4����action��Ӧ��������Ϣ
			ActionConfig config = actionConfigs.get(path);
			if(config == null){
				//1)δ�ҵ�������Ϣ���׳��쳣
				throw new RuntimeException();
			}
			//2)�ҵ�������Ϣ�����������Ϣ����������
			//5����actionInvocationʵ������ɶԹ��������Լ�action�ķ���
			ActionInvocation invocation = new ActionInvocation(interceptorList, config, req, resp);
			//6��ý����
			String result = invocation.invoke(invocation);
			//7��������Ϣ�ҵ��������Ӧ��·��
			
			String dispatcherPath = config.getResult().get(result);
			if(dispatcherPath == null || "".equals(dispatcherPath)){
				//1)�Ҳ������·�������쳣
				throw new RuntimeException();
			}
			//8������ת�������õ�·��
			req.getRequestDispatcher(dispatcherPath).forward(req, resp);
			//�ͷ���Դ
			ActionContext.tl.remove();
			
		}		    
		   
		
		
		
		

		
		
		
		
		
	
		
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
		
	}

}
