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
 * 1.初始化配置文件
 * 2.分析请求要访问的action，加载action的配置
 * 3.根据分析结果调用ActionInvocation的类，根据结果进行跳转
 * @author Ethan
 *
 */
public class StrutsPrepareAndExcuteFilter implements Filter{
	//配置文件中的过滤器信息
	private List<String> interceptorList;
	//struts处理的
	private String extension;
	//
	private Map<String, ActionConfig> actionConfigs;

	@Override
	public void destroy() {
		//1准备过滤器链配置
		interceptorList = ConfigurationManager.getInterceptor();
				
		//2准备constant配置，访问后缀的配置信息
		extension = ConfigurationManager.getConstant("struts.action.extension");
		
		//3加载action配置
		actionConfigs = ConfigurationManager.getActionConfig();
		
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
			throws IOException, ServletException {
		//0强转request和response为HtttpServletRequest和response
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		
		
		//1获得请求路径
		String path = req.getServletPath();//http://localhost:8080/HelloWorld/HelloAction.action
		
		
		//判断请求是否需要访问action
		
		if(!path.endsWith(extension)){
			//不以.action结尾，则不需要访问action资源，直接调用chain.doFilter
			chain.doFilter(request, response);
			return;
		}else{
			//后缀以.action结尾，则需要访问action
			//3获得需要访问的action
			path = path.substring(1);
			path = path.replace("."+extension, "");
			//4查找action对应的配置信息
			ActionConfig config = actionConfigs.get(path);
			if(config == null){
				//1)未找到配置信息，抛出异常
				throw new RuntimeException();
			}
			//2)找到配置信息，获得配置信息，继续处理
			//5创建actionInvocation实例，完成对过滤器链以及action的方法
			ActionInvocation invocation = new ActionInvocation(interceptorList, config, req, resp);
			//6获得结果串
			String result = invocation.invoke(invocation);
			//7从配置信息找到结果串对应的路径
			
			String dispatcherPath = config.getResult().get(result);
			if(dispatcherPath == null || "".equals(dispatcherPath)){
				//1)找不到结果路径，抛异常
				throw new RuntimeException();
			}
			//8将请求转发到配置的路径
			req.getRequestDispatcher(dispatcherPath).forward(req, resp);
			//释放资源
			ActionContext.tl.remove();
			
		}		    
		   
		
		
		
		

		
		
		
		
		
	
		
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
		
	}

}
