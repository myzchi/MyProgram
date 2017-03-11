package com.config;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;





import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 * 读取Struts.xml配置文件
 * @author Ethan
 *
 */
public class ConfigurationManager {
	
	List<String> interceptors;
	
	//String表示interceptor里面的类名
	public List<String> getInterceptor(){
		//1创建解析器
		Document doc = getDocument();
		
		//3书写xpath
		String xpath ="//interceptor";
		
		//4根据xpath获得拦截器配置
		List<Element> list = doc.selectNodes(xpath);
				
		//5将配置信息封装到list集合中
		
		if(list != null && list.size()>0){
			interceptors = new ArrayList<String>();
			for (Element element : list) {
				String className = element.attributeValue("class");
				interceptors.add(className);
			}
		}
		return interceptors;
	}
	
	public static Document getDocument(){
		//1创建解析器
		SAXReader reader = new SAXReader();
		//2加载配置文件=》document
		  //获得配置文件流
		InputStream in = ConfigurationManager.class.getResourceAsStream("/struts.xml");
		Document doc = null;
		try {
			doc = reader.read(in);
		} catch (DocumentException e) {
			e.printStackTrace();
			throw new RuntimeException("配置文件加载失败");
		}
		return doc;
	}
	
	
	//读取constant
	public static String getConstant(String key){
		Document doc = getDocument();
		
		//3书写xpath
		String xpath ="//constant[@name='"+key+"']";
		
		Element constant = (Element) doc.selectSingleNode(xpath);
		if(constant!=null){
			return constant.attributeValue("value");
		}else{
			return null;
		}
	}
	
	//读取action
	public Map<String, ActionConfig> getActionConfig(){
		
		Map<String, ActionConfig> actionMap;
		
		Document doc = getDocument();
		
		//3书写xpath
		String xpath ="//action";
		
		//4根据xpath获得拦截器配置
		List<Element> list = doc.selectNodes(xpath);
		if(list == null || list.size()==0){
			return null;
		}else{
			actionMap = new HashMap<String, ActionConfig>();
		}
		
		for (Element element : list) {
			ActionConfig action = new ActionConfig();
			action.setName(element.attributeValue("name"));	
			action.setClassName(element.attributeValue("class"));
			String method = element.attributeValue("method");
			action.setMethod(method==null || method.trim().equals("") ? "execute":"");
			
			List<Element> results = element.elements("result");
			for (Element result : results) {
				action.getResult().put(result.attributeValue("name"), result.getName());
			}
			actionMap.put(action.getName(), action);
		}
		
		
		return null;
	}

}
