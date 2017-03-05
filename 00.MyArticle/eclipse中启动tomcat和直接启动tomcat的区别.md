title: "Eclipse中启动tomcat和直接启动tomcat的区别"
date: 2014-11-14 12:40:55
tags: [eclipse, tomcat]
category: 学海无涯
toc: false
---
最近一次在eclipse启动tomcat，然后打开http://localhost:8080, 竟然404
<img src="http://7xiwo7.com1.z0.glb.clouddn.com/tomcat20150505143457.png"/>
觉得不管项目是不是正常还是失败，最起码tomcat启动起来了吧，但事实并不是如此，查资料才知道在eclipse启动tomcat，其实是eclipse调用tomcat核心的组件，内置到eclipse中，启动和部署时和真正的tomcat毫无关系，下面start.bat启动真正的tomcat
<img src="http://7xiwo7.com1.z0.glb.clouddn.com/tomcat20150505143607.png"/>
原因是因为我们在eclipse设置的tomcat的Server locations路径为: Use workspace metadata
<img src="http://7xiwo7.com1.z0.glb.clouddn.com/tomcat20150505143542.png"/>
将路径选择为第二项，即改为
<img src="http://7xiwo7.com1.z0.glb.clouddn.com/tomcat20150505144451.png"/>
server_path改为tomcat的目录，再启动就OK了




