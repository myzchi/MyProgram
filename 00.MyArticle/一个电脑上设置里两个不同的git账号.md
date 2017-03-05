title: "一个电脑上设置里两个不同的git账号"
date: 2016-03-14 19:55:00
tags:
---
   最近公司新项目也要使用git进行版本管理，由于之前已经有github在电脑上生存了一个公钥，但是用户名和邮箱都是自己的额，现在需要用公司邮箱重新生成公钥，发现在同一个电脑上使用2个git账号还真折腾了半天功夫
   
首先需要生成SSH key，由于之前已经存在一个github的公钥，所以还要再生成一个工作的key
``` bash 
ssh-keygen -t rsa -C "your-email-address"
```
注意不要覆盖之前的id_rsa，使用一个新的名字，比如id_rsa_work

把id_rsa_work.pub加到你的work账号上，即把该key加到ssh agent上。由于不是使用默认的.ssh/id_rsa，所以你需要显示告诉ssh agent你的新key的位置
``` bash 
ssh-add ~/.ssh/id_rsa_work
```
但是这个步骤有个问题，总是执行不成功，提示错误如下
``` bash 
Could not open a connection to your authentication agent
```
根据网上提示，出现这样的错误，说明ssh-agent没有启动起来，需要手动启动ssh-agent
``` bash 
eval $(ssh-agent)
```
这个方法确实可以解决问题，但我发现其实只是启动了一个临时的线程服务，电脑重启后就失效了
所以需要将这个服务一直保持启动状态，需要在git启动的配置文件里面加入启动脚本
``` bash 
SSH_ENV="$HOME/.ssh/environment"

function start_agent {
     echo "Initialising new SSH agent..."
     /usr/bin/ssh-agent | sed 's/^echo/#echo/' > "${SSH_ENV}"
     echo succeeded
     chmod 600 "${SSH_ENV}"
     . "${SSH_ENV}" > /dev/null
     /usr/bin/ssh-add;
}

# Source SSH settings, if applicable

if [ -f "${SSH_ENV}" ]; then
     . "${SSH_ENV}" > /dev/null
     #ps ${SSH_AGENT_PID} doesn't work under cywgin
     ps -ef | grep ${SSH_AGENT_PID} | grep ssh-agent$ > /dev/null || {
         start_agent;
     }
else
     start_agent;
fi
```
在这里为止，问题终于解决，一个电脑上使用两个git账号
