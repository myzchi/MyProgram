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
 * ��ȡStruts.xml�����ļ�
 * @author Ethan
 *
 */
public class ConfigurationManager {
	
	List<String> interceptors;
	
	//String��ʾinterceptor���������
	public List<String> getInterceptor(){
		//1����������
		Document doc = getDocument();
		
		//3��дxpath
		String xpath ="//interceptor";
		
		//4����xpath�������������
		List<Element> list = doc.selectNodes(xpath);
				
		//5��������Ϣ��װ��list������
		
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
		//1����������
		SAXReader reader = new SAXReader();
		//2���������ļ�=��document
		  //��������ļ���
		InputStream in = ConfigurationManager.class.getResourceAsStream("/struts.xml");
		Document doc = null;
		try {
			doc = reader.read(in);
		} catch (DocumentException e) {
			e.printStackTrace();
			throw new RuntimeException("�����ļ�����ʧ��");
		}
		return doc;
	}
	
	
	//��ȡconstant
	public static String getConstant(String key){
		Document doc = getDocument();
		
		//3��дxpath
		String xpath ="//constant[@name='"+key+"']";
		
		Element constant = (Element) doc.selectSingleNode(xpath);
		if(constant!=null){
			return constant.attributeValue("value");
		}else{
			return null;
		}
	}
	
	//��ȡaction
	public Map<String, ActionConfig> getActionConfig(){
		
		Map<String, ActionConfig> actionMap;
		
		Document doc = getDocument();
		
		//3��дxpath
		String xpath ="//action";
		
		//4����xpath�������������
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
