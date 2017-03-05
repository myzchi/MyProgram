title: "慎用数组转换List的Arrays.asList方法"
date: 2016-05-17 12:45:28
category: 学海无涯
toc: false
tags: List
---
最近进行程序测试的时候报了一个异常，发现是list使用remove的是报的错位，信息如下：

``` bash 
java.lang.UnsupportedOperationException
 at java.util.AbstractList.remove(Unknown Source)
```

list为什么使用remove会报这个错呢？查看原因，发现是因为list是使用数组转换List的Arrays.asList方法得到的，查看一下Java API对此方法的描述

``` bash 
asList
public static <T> List<T> asList(T... a)返回一个受指定数组支持的固定大小的列表。（对返回列表的更改会“直接写”到数组。）
此方法同 Collection.toArray() 一起，充当了基于数组的 API 与基于 collection 的 API 之间的桥梁。
返回的列表是可序列化的，并且实现了 RandomAccess。 
此方法还提供了一个创建固定长度的列表的便捷方法，该列表被初始化为包含多个元素： 

     List<String> stooges = Arrays.asList("Larry", "Moe", "Curly");
 
参数：
     a - 支持列表的数组。 
返回：
     指定数组的列表视图。
```

只有该方法的定义，并未解释异常的原因，那就查一下源码，源码如下：
``` bash 
    public static transient List asList(Object aobj[])
    {
        return new ArrayList(aobj);
    }
```
返回值是一个ArrayList
``` bash
public class Arrays
{
    private static class ArrayList extends AbstractList
        implements RandomAccess, Serializable
    {

        public int size()
        {
            return a.length;
        }

        public Object[] toArray()
        {
            return (Object[])((Object []) (a)).clone();
        }

        public Object[] toArray(Object aobj[])
        {
            int i = size();
            if(aobj.length < i)
                return Arrays.copyOf(a, i, ((Object) (aobj)).getClass());
            System.arraycopy(((Object) (a)), 0, ((Object) (aobj)), 0, i);
            if(aobj.length > i)
                aobj[i] = null;
            return aobj;
        }

        public Object get(int i)
        {
            return a[i];
        }

        public Object set(int i, Object obj)
        {
            Object obj1 = a[i];
            a[i] = obj;
            return obj1;
        }

        public int indexOf(Object obj)
        {
            if(obj == null)
            {
                for(int i = 0; i < a.length; i++)
                    if(a[i] == null)
                        return i;

            } else
            {
                for(int j = 0; j < a.length; j++)
                    if(obj.equals(a[j]))
                        return j;

            }
            return -1;
        }

        public boolean contains(Object obj)
        {
            return indexOf(obj) != -1;
        }

        private static final long serialVersionUID = -2764017481108945198L;
        private final Object a[];

        ArrayList(Object aobj[])
        {
            if(aobj == null)
            {
                throw new NullPointerException();
            } else
            {
                a = aobj;
                return;
            }
        }
    }
```
ArrayList是Array中的一个内部类，其中并没有remove方法
``` bash
    public Object remove(int i)
    {
        throw new UnsupportedOperationException();
    }
```
查看父类AbstractList，remove方法在此，它直接抛出了该异常；
至此我们知道，数据转换List得到的不是java.util下面顶级list，而是数据内部一个继承AbstractList的list，而这个种类的list并不具备remove方法。

想了一下，解决办法如下：
重新new一个list，将转换得到的list值转入进去
``` bash
List newList = new ArrayList<>(arrayTolist);
```
这样就可以继续使用remove方法了。



