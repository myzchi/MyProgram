title: "Struts2中过滤器和拦截器的区别"
date: 2014-12-28 02:48:35
tags: [Struts2]
category: 学海无涯
---

拦截器和过滤器的区别：

> * 1.拦截器是基于java的反射机制的，而过滤器是基于函数回调
> * 2.过滤器依赖与servlet容器，而拦截器不依赖与servlet容器
> * 3.拦截器只能对action请求起作用，而过滤器则可以对几乎所有的请求起作用
> * 4.拦截器可以访问action上下文、值栈里的对象，而过滤器不能
> * 5.在action的生命周期中，拦截器可以多次被调用，而过滤器只能在容器初始化时被调用一次

拦截器 ：是在面向切面编程的就是在你的service或者一个方法前调用一个方法，或者在方法后调用一个方法比如动态代理就是拦截器的简单实现，在你调用方法前打印出字符串（或者做其它业务逻辑的操作），也可以在你调用方法后打印出字符串，甚至在你抛出异常的时候做业务逻辑的操作。

下面通过实例来看一下过滤器和拦截器的区别：

使用拦截器进行/admin 目录下jsp页面的过滤
``` bash 
<package name="newsDemo" extends="struts-default"   
        namespace="/admin">   
        <interceptors>   
            <interceptor name="auth" class="com.test.news.util.AccessInterceptor" />   
            <interceptor-stack name="authStack">   
                <interceptor-ref name="auth" />   
            </interceptor-stack>   
        </interceptors>   
        <!-- action -->   
        <action name="newsAdminView!*" class="newsAction"   
            method="{1}">   
            <interceptor-ref name="defaultStack"/>   
            <interceptor-ref name="authStack">   
            </interceptor-ref> 
```
			
下面是自己实现的Interceptor class: 
``` bash   
public class AccessInterceptor extends AbstractInterceptor {   
    private static final long serialVersionUID = -4291195782860785705L;   
    @Override   
    public String intercept(ActionInvocation actionInvocation) throws Exception {   
         ActionContext actionContext = actionInvocation.getInvocationContext();   
         Map session = actionContext.getSession();   
          
        //except login action   
         Object action = actionInvocation.getAction();   
        if (action instanceof AdminLoginAction) {   
            return actionInvocation.invoke();   
         }   
        //check session   
        if(session.get("user")==null ){   
            return "logout";   
         }   
        return actionInvocation.invoke();//go on   
     }   
}   
```
过滤器：是在javaweb中，你传入的request,response提前过滤掉一些信息，或者提前设置一些参数，然后再传入servlet或者struts的 action进行业务逻辑，比如过滤掉非法url（不是login.do的地址请求，如果用户没有登陆都过滤掉）,或者在传入servlet或者 struts的action前统一设置字符集，或者去除掉一些非法字符.
使用过滤器进行/admin 目录下jsp页面的过滤，首先在web.xml进行过滤器配置: 
``` bash 
<filter>   
    <filter-name>access filter</filter-name>   
        <filter-class>   
             com.test.news.util.AccessFilter   
        </filter-class>   
</filter>   
    <filter-mapping>   
        <filter-name>access filter</filter-name>   
        <url-pattern>/admin/*</url-pattern>   
    </filter-mapping>  
```	
下面是过滤的实现类: 
``` bash       
    public void destroy() {   
    }   
    public void doFilter(ServletRequest arg0, ServletResponse arg1,   
             FilterChain filterChain) throws IOException, ServletException {   
         HttpServletRequest request = (HttpServletRequest)arg0;   
         HttpServletResponse response = (HttpServletResponse)arg1;   
         HttpSession session = request.getSession();   
        if(session.getAttribute("user")== null && request.getRequestURI().indexOf("login.jsp")==-1 ){   
             response.sendRedirect("login.jsp");   
            return ;   
         }   
         filterChain.doFilter(arg0, arg1);   
    }   
    public void init(FilterConfig arg0) throws ServletException {   
    }   
}    
```