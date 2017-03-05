title: "mysql卸载重新安装后start service报错的问题"
date: 2015-02-02 17:45:28
category: 学海无涯
toc: false
tags: MySQL
---
安装MySQL时无法启动服务，在安装mysql时，到最后一步执行时，在start service，出现如下错误：

``` bash 
Could not start the service
```

在网上找了很多方法，包括要把所有相关的文件夹、服务、注册表等都要卸载干净等等，但是都无效。

后来在一个不太显眼的地方找到一个方法，解决办法如下：

使用services.msc打开服务窗口，查看MySQL service是否已经存在。如已经存在并已启动，则先停止该服务，然后到注册表

``` bash 
("HKEY_LOCAL_MACHINE/SYSTEM /CurrentControlSet/Services")
```

中删除对应服务，并使用命令sc delete MySQL,然后继续进行安装，就OK了。
