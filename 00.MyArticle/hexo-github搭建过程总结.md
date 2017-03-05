title: "hexo+github搭建过程总结"
date: 2015-05-02 17:34:40
category: 学海无涯
tags: [git, hexo, github]
---
   折腾了3天，遇到各种超级坑的问题，总算把hexo+github pages的框架搭建好了，逃离了WordPress →_→
   
git客户端
---
git有个windows的(git for windows)可视化操作工具：msysgit，github也有一个的客户端github for windows，不知道是不是两者不兼容，两者一起装的时候，在安装hexo后，会报下面的错

``` bash 
$ hexo
sh.exe": hexo: command not found
```

找各种方法都不行，后来把两个客户端都卸载了，只安装msysgit再装hexo就OK了

github pages
---
这是一个疑问，到现在还没有完全弄明白，就是在新建一个github工程后，直接在master分支新建文件作为github pages的项目，包括我在网上看到各种步骤和指引都是这样，但是部署好以后，一直报github 404
我的解决办法是在项目的setting的里面，找到github pages的选项，选择Automatic page generator，自动产生一些github pages的文件，然后再git上传文件覆盖它，再部署就可以了

hexo deploy
---
在hexo安装好以后，把本地生成的文件传到远程github上，执行hexo deploy，不报任何错，然始终传不上去
<img src="http://img.ask.csdn.net/upload/201505/01/1430452763_372695.png"/>
重新配置根目录下的_config.yml即可
``` bash 
deploy: 
  type: git
  repository: git@github.com:corbam/myzchi.github.com.git
  branch: gh-pages
```
其中第二项的repository，很多人配置成

``` bash 
repository: https://github.com/xxx/xxx.github.io.git
```

这种https的url配置后我是上传不上去的，只能安装第一种即ssh的url方式配置



