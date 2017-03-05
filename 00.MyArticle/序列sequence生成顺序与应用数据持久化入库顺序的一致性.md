title: "序列sequence生成顺序与应用数据持久化入库顺序的一致性"
date: 2016-10-24 13:18:14
category: 学海无涯
tags: Oracle
toc: false
---
最近项目中遇到一个问题，数据在不同库之间进行同步的时候，部分数据没有同步过去；

经过检查，发现是同步的时候很不明智的用序列sequence检查数据的先后性，然后增量数据同步，因为依次取数据库sequence的时候，应用程序中的数据入库持久化是有先后顺序的，优先取sequence的应用程序可能执行比较慢，后取sequence的应用程序已经提前完成持久化动作，因此sequence生成顺序和数据持久化入库顺序并不保持一致；

Oracle是如此，mysql呢？mysql设置自增主键是否也是这种情况，于是做了个测试

在mysql建一张表，设置自增主键
``` bash 
CREATE TABLE TEST_ZC (   
    ID NUMBER(19,0) NOT NULL AUTO_INCREMENT, 
    TIME TIMESTAMP NULL DEFAULT NULL,  
);
```
然后应用程序，比较主键和时间

<img src="http://7xiwo7.com1.z0.glb.clouddn.com/sequence.png"/>
可有看到时间和序列之间并不一致，即Oracle中sequence和MySQL自增主键一样，和数据持久化入库时间并不具有一致性。