title: "Redis学习总结"
date: 2015-05-24 13:18:14
category: 学海无涯
tags: Redis
---
最近做流量项目，系统用redis作为缓存，花了几天时间学习了redis的基本内容，整理和总结一下

#Redis简介#
Redis是一种非关系型数据库（即nosql），存储类型为键值(key-value)，其中value支持多种数据类型，包括string、hash、list、set、zset，估计这也是流量项目使用redis的原因之一

#Redis适用场景#
1、取最新N个数据的操作
2、排行榜应用（好像和第一个有点重复）
3、需要精确设定过期时间的应用
4、计数器的应用（新浪微博主要使用）
5、Uniq操作，获取某段时间所有数据排重值
6、实时系统，反垃圾系统
7、Pub/Sub构建实时消息系统
8、构建队列系统
9、缓存
当前公司使用的就是Redis的第9个应用，其他功能及应用还在学习ing

#Redis安装#
---
目前Redis稳定版已经到了3.0，下载地址：http://redis.io/download
下载后解压并编译
``` bash 
$ tar xzf redis-3.0.1.tar.gz
$ cd redis-3.0.1
$ make
```
现在可以启动redis服务了
``` bash 
$ src/redis-server
```
然后另开一个窗口
``` bash 
$ src/redis-cli
```
显示
``` bash 
127.0.0.1:6379> 
```
说明已经成功了

如果不想另开窗口，可将redis.conf中属性"daemonize no"中no改为yes[指后台运行]

第一个示例
``` bash 
127.0.0.1:6379> set first "Hello World"
OK
127.0.0.1:6379> get first
"Hello World"
```
#Redis参数配置#
