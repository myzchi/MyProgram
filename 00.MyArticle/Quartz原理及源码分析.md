title: "Quartz原理及源码分析"
date: 2015-10-17 21:48:12
tags:
---
Quartz是经典的Java版开源定时调度器，项目中作为作业调度管理进行使用。由于之前项目出过一次异常，调试跟进了Quartz中，研究了一下其源码和原理
几个关键概念：
1、Job
表示一个工作，要执行的具体内容。此接口中只有一个方法
void execute(JobExecutionContext context)
 
2、JobDetail
JobDetail表示一个具体的可执行的调度程序，Job是这个可执行程调度程序所要执行的内容，另外JobDetail还包含了这个任务调度的方案和策略。
 
3、Trigger代表一个调度参数的配置，什么时候去调。
 
4、Scheduler代表一个调度容器，一个调度容器中可以注册多个JobDetail和Trigger。当Trigger与JobDetail组合，就可以被Scheduler容器调度了。

最简单的HelloWorld示例
```
public  class  HelloWorldMain {
     Log log = LogFactory.getLog(HelloWorldMain. class );
     
     public  void  run() {
         try  {
             //取得Schedule对象
             SchedulerFactory sf =  new  StdSchedulerFactory();
             Scheduler sch = sf.getScheduler(); 
             
             JobDetail jd =  new  JobDetail( "HelloWorldJobDetail" ,Scheduler.DEFAULT_GROUP,HelloWorldJob. class );
             Trigger tg = TriggerUtils.makeMinutelyTrigger( 1 );
             tg.setName( "HelloWorldTrigger" );
             
             sch.scheduleJob(jd, tg);
             sch.start();
         }  catch  ( Exception e ) {
             e.printStackTrace();
             
         }
     }
     public  static  void  main(String[] args) {
         HelloWorldMain hw =  new  HelloWorldMain();
         hw.run();
     }
}
```
我们看到初始化一个调度器需要用工厂类获取实例:
```
SchedulerFactory sf =  new  StdSchedulerFactory();
Scheduler sch = sf.getScheduler();
sch.start();//启动
``` 

下面跟进StdSchedulerFactory的getScheduler()方法:
```
public  Scheduler getScheduler()  throws  SchedulerException {
         if  (cfg ==  null ) {
             initialize();
         }
         SchedulerRepository schedRep = SchedulerRepository.getInstance();
         //从"调度器仓库"中根据properties的SchedulerName配置获取一个调度器实例
         Scheduler sched = schedRep.lookup(getSchedulerName());
         if  (sched !=  null ) {
             if  (sched.isShutdown()) {
                 schedRep.remove(getSchedulerName());
             }  else  {
                 return  sched;
             }
         }
         //初始化调度器
         sched = instantiate();
         return  sched;
     }
``` 
跟进初始化调度器方法sched = instantiate();发现是一个700多行的初始化方法,涉及到

读取配置资源,
生成QuartzScheduler对象,
创建该对象的运行线程,并启动线程;
初始化JobStore,QuartzScheduler,DBConnectionManager等重要组件, 
至此,调度器的初始化工作已完成,初始化工作中quratz读取了数据库中存放的对应当前调度器的锁信息,对应CRM中的表QRTZ2_LOCKS,中的STATE_ACCESS,TRIGGER_ACCESS两个LOCK_NAME.
```
public  void  initialize(ClassLoadHelper loadHelper,
             SchedulerSignaler signaler)  throws  SchedulerConfigException {
         if  (dsName ==  null ) {
             throw  new  SchedulerConfigException( "DataSource name not set." );
         }
         classLoadHelper = loadHelper;
         if (isThreadsInheritInitializersClassLoadContext()) {
             log.info( "JDBCJobStore threads will inherit ContextClassLoader of thread: "  + Thread.currentThread().getName());
             initializersLoader = Thread.currentThread().getContextClassLoader();
         }
         
         this .schedSignaler = signaler;
         // If the user hasn't specified an explicit lock handler, then
         // choose one based on CMT/Clustered/UseDBLocks.
         if  (getLockHandler() ==  null ) {
             
             // If the user hasn't specified an explicit lock handler,
             // then we *must* use DB locks with clustering
             if  (isClustered()) {
                 setUseDBLocks( true );
             }
             
             if  (getUseDBLocks()) {
                 if (getDriverDelegateClass() !=  null  && getDriverDelegateClass().equals(MSSQLDelegate. class .getName())) {
                     if (getSelectWithLockSQL() ==  null ) {
                         //读取数据库LOCKS表中对应当前调度器的锁信息
                         String msSqlDflt =  "SELECT * FROM {0}LOCKS WITH (UPDLOCK,ROWLOCK) WHERE "  + COL_SCHEDULER_NAME +  " = {1} AND LOCK_NAME = ?" ;
                         getLog().info( "Detected usage of MSSQLDelegate class - defaulting 'selectWithLockSQL' to '"  + msSqlDflt +  "'." );
                         setSelectWithLockSQL(msSqlDflt);
                     }
                 }
                 getLog().info( "Using db table-based data access locking (synchronization)." );
                 setLockHandler( new  StdRowLockSemaphore(getTablePrefix(), getInstanceName(), getSelectWithLockSQL()));
             }  else  {
                 getLog().info(
                     "Using thread monitor-based data access locking (synchronization)." );
                 setLockHandler( new  SimpleSemaphore());
             }
         }
     }
``` 
当调用sch.start();方法时,scheduler做了如下工作:

1.通知listener开始启动

2.启动调度器线程

3.启动plugin

4.通知listener启动完成
```
public  void  start()  throws  SchedulerException {
         if  (shuttingDown|| closed) {
             throw  new  SchedulerException(
                     "The Scheduler cannot be restarted after shutdown() has been called." );
         }
         // QTZ-212 : calling new schedulerStarting() method on the listeners
         // right after entering start()
         //通知该调度器的listener启动开始
         notifySchedulerListenersStarting();
         if  (initialStart ==  null ) {
             initialStart =  new  Date();
             //启动调度器的线程
             this .resources.getJobStore().schedulerStarted();            
             //启动plugins
             startPlugins();
         }  else  {
             resources.getJobStore().schedulerResumed();
         }
         schedThread.togglePause( false );
         getLog().info(
                 "Scheduler "  + resources.getUniqueIdentifier() +  " started." );
         //通知该调度器的listener启动完成
         notifySchedulerListenersStarted();
     }
``` 
##调度过程
调度器启动后,调度器的线程就处于运行状态了,开始执行quartz的主要工作–调度任务.

前面已介绍过,任务的调度过程大致分为三步:

1.获取待触发trigger

2.触发trigger

3.实例化并执行Job

下面分别分析三个阶段的源码.

QuartzSchedulerThread是调度器线程类,调度过程的三个步骤就承载在run()方法中,分析见代码注释:
```
public  void  run() {
         boolean  lastAcquireFailed =  false ;
         //
         while  (!halted.get()) {
             try  {
                 // check if we're supposed to pause...
                 synchronized  (sigLock) {
                     while  (paused && !halted.get()) {
                         try  {
                             // wait until togglePause(false) is called...
                             sigLock.wait(1000L);
                         }  catch  (InterruptedException ignore) {
                         }
                     }
                     if  (halted.get()) {
                         break ;
                     }
                 }
                 //获取当前线程池中线程的数量
                 int  availThreadCount = qsRsrcs.getThreadPool().blockForAvailableThreads();
                 if (availThreadCount >  0 ) {  // will always be true, due to semantics of blockForAvailableThreads...
                     List<OperableTrigger> triggers =  null ;
                     long  now = System.currentTimeMillis();
                     clearSignaledSchedulingChange();
                     try  {
                         //调度器在trigger队列中寻找30秒内一定数目的trigger准备执行调度,
                         //参数1:nolaterthan = now+3000ms,参数2 最大获取数量,大小取线程池线程剩余量与定义值得较小者
                         //参数3 时间窗口 默认为0,程序会在nolaterthan后加上窗口大小来选择trigger
                         triggers = qsRsrcs.getJobStore().acquireNextTriggers(
                                 now + idleWaitTime, Math.min(availThreadCount, qsRsrcs.getMaxBatchSize()), qsRsrcs.getBatchTimeWindow());
                         //上一步获取成功将失败标志置为false;
                         lastAcquireFailed =  false ;
                         if  (log.isDebugEnabled())
                             log.debug( "batch acquisition of "  + (triggers ==  null  ?  0  : triggers.size()) +  " triggers" );
                     }  catch  (JobPersistenceException jpe) {
                         if (!lastAcquireFailed) {
                             qs.notifySchedulerListenersError(
                                 "An error occurred while scanning for the next triggers to fire." ,
                                 jpe);
                         }
                         //捕捉到异常则值标志为true,再次尝试获取
                         lastAcquireFailed =  true ;
                         continue ;
                     }  catch  (RuntimeException e) {
                         if (!lastAcquireFailed) {
                             getLog().error( "quartzSchedulerThreadLoop: RuntimeException "
                                     +e.getMessage(), e);
                         }
                         lastAcquireFailed =  true ;
                         continue ;
                     }
                     if  (triggers !=  null  && !triggers.isEmpty()) {
                         now = System.currentTimeMillis();
                         long  triggerTime = triggers.get( 0 ).getNextFireTime().getTime();
                         long  timeUntilTrigger = triggerTime - now; //计算距离trigger触发的时间
                         while (timeUntilTrigger >  2 ) {
                             synchronized  (sigLock) {
                                 if  (halted.get()) {
                                     break ;
                                 }
                                 //如果这时调度器发生了改变,新的trigger添加进来,那么有可能新添加的trigger比当前待执行的trigger
                                 //更急迫,那么需要放弃当前trigger重新获取,然而,这里存在一个值不值得的问题,如果重新获取新trigger
                                 //的时间要长于当前时间到新trigger出发的时间,那么即使放弃当前的trigger,仍然会导致xntrigger获取失败,
                                 //但我们又不知道获取新的trigger需要多长时间,于是,我们做了一个主观的评判,若jobstore为RAM,那么
                                 //假定获取时间需要7ms,若jobstore是持久化的,假定其需要70ms,当前时间与新trigger的触发时间之差小于
                                 // 这个值的我们认为不值得重新获取,返回false
                                 //这里判断是否有上述情况发生,值不值得放弃本次trigger,若判定不放弃,则线程直接等待至trigger触发的时刻
                                 if  (!isCandidateNewTimeEarlierWithinReason(triggerTime,  false )) {
                                     try  {
                                         // we could have blocked a long while
                                         // on 'synchronize', so we must recompute
                                         now = System.currentTimeMillis();
                                         timeUntilTrigger = triggerTime - now;
                                         if (timeUntilTrigger >=  1 )
                                             sigLock.wait(timeUntilTrigger);
                                     }  catch  (InterruptedException ignore) {
                                     }
                                 }
                             }
                             //该方法调用了上面的判定方法,作为再次判定的逻辑
                             //到达这里有两种情况1.决定放弃当前trigger,那么再判定一次,如果仍然有放弃,那么清空triggers列表并
                             // 退出循环 2.不放弃当前trigger,且线程已经wait到trigger触发的时刻,那么什么也不做
                             if (releaseIfScheduleChangedSignificantly(triggers, triggerTime)) {
                                 break ;
                             }
                             now = System.currentTimeMillis();
                             timeUntilTrigger = triggerTime - now;
                             //这时触发器已经即将触发,值会<2
                         }
                         // this happens if releaseIfScheduleChangedSignificantly decided to release triggers
                         if (triggers.isEmpty())
                             continue ;
                         // set triggers to 'executing'
                         List<TriggerFiredResult> bndles =  new  ArrayList<TriggerFiredResult>();
                         boolean  goAhead =  true ;
                         synchronized (sigLock) {
                             goAhead = !halted.get();
                         }
                         if (goAhead) {
                             try  {
                                 //触发triggers,结果付给bndles,注意,从这里返回后,trigger在数据库中已经经过了锁定,解除锁定,这一套过程
                                 //所以说,quratz定不是等到job执行完才释放trigger资源的占有,而是读取完本次触发所需的信息后立即释放资源
                                 //然后再执行jobs
                                 List<TriggerFiredResult> res = qsRsrcs.getJobStore().triggersFired(triggers);
                                 if (res !=  null )
                                     bndles = res;
                             }  catch  (SchedulerException se) {
                                 qs.notifySchedulerListenersError(
                                         "An error occurred while firing triggers '"
                                                 + triggers +  "'" , se);
                                 //QTZ-179 : a problem occurred interacting with the triggers from the db
                                 //we release them and loop again
                                 for  ( int  i =  0 ; i < triggers.size(); i++) {
                                     qsRsrcs.getJobStore().releaseAcquiredTrigger(triggers.get(i));
                                 }
                                 continue ;
                             }
                         }
                         //迭代trigger的信息,分别跑job
                         for  ( int  i =  0 ; i < bndles.size(); i++) {
                             TriggerFiredResult result =  bndles.get(i);
                             TriggerFiredBundle bndle =  result.getTriggerFiredBundle();
                             Exception exception = result.getException();
                             if  (exception  instanceof  RuntimeException) {
                                 getLog().error( "RuntimeException while firing trigger "  + triggers.get(i), exception);
                                 qsRsrcs.getJobStore().releaseAcquiredTrigger(triggers.get(i));
                                 continue ;
                             }
                             // it's possible to get 'null' if the triggers was paused,
                             // blocked, or other similar occurrences that prevent it being
                             // fired at this time...  or if the scheduler was shutdown (halted)
                             //在特殊情况下,bndle可能为null,看triggerFired方法可以看到,当从数据库获取trigger时,如果status不是
                             //STATE_ACQUIRED,那么会直接返回空.quratz这种情况下本调度器启动重试流程,重新获取4次,若仍有问题,
                             // 则抛出异常.
                             if  (bndle ==  null ) {
                                 qsRsrcs.getJobStore().releaseAcquiredTrigger(triggers.get(i));
                                 continue ;
                             }
                             //执行job
                             JobRunShell shell =  null ;
                             try  {
                                 //创建一个job的Runshell
                                 shell = qsRsrcs.getJobRunShellFactory().createJobRunShell(bndle);
                                 shell.initialize(qs);
                             }  catch  (SchedulerException se) {
                                 qsRsrcs.getJobStore().triggeredJobComplete(triggers.get(i), bndle.getJobDetail(), CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR);
                                 continue ;
                             }
                             //把runShell放在线程池里跑
                             if  (qsRsrcs.getThreadPool().runInThread(shell) ==  false ) {
                                 // this case should never happen, as it is indicative of the
                                 // scheduler being shutdown or a bug in the thread pool or
                                 // a thread pool being used concurrently - which the docs
                                 // say not to do...
                                 getLog().error( "ThreadPool.runInThread() return false!" );
                                 qsRsrcs.getJobStore().triggeredJobComplete(triggers.get(i), bndle.getJobDetail(), CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR);
                             }
                         }
                         continue ;  // while (!halted)
                     }
                 }  else  {  // if(availThreadCount > 0)
                     // should never happen, if threadPool.blockForAvailableThreads() follows contract
                     continue ;  // while (!halted)
                 }
                 //保证负载平衡的方法,每次执行一轮触发后,本scheduler会等待一个随机的时间,这样就使得其他节点上的scheduler可以得到资源.
                 long  now = System.currentTimeMillis();
                 long  waitTime = now + getRandomizedIdleWaitTime();
                 long  timeUntilContinue = waitTime - now;
                 synchronized (sigLock) {
                     try  {
                       if (!halted.get()) {
                         // QTZ-336 A job might have been completed in the mean time and we might have
                         // missed the scheduled changed signal by not waiting for the notify() yet
                         // Check that before waiting for too long in case this very job needs to be
                         // scheduled very soon
                         if  (!isScheduleChanged()) {
                           sigLock.wait(timeUntilContinue);
                         }
                       }
                     }  catch  (InterruptedException ignore) {
                     }
                 }
             }  catch (RuntimeException re) {
                 getLog().error( "Runtime error occurred in main trigger firing loop." , re);
             }
         }  // while (!halted)
         // drop references to scheduler stuff to aid garbage collection...
         qs =  null ;
         qsRsrcs =  null ;
     }
``` 
调度器每次获取到的trigger是30s内需要执行的,所以要等待一段时间至trigger执行前2ms.在等待过程中涉及到一个新加进来更紧急的trigger的处理逻辑.分析写在注释中,不再赘述.

可以看到调度器的只要在运行状态,就会不停地执行调度流程.值得注意的是,在流程的最后线程会等待一个随机的时间.这就是quartz自带的负载平衡机制.

以下是三个步骤的跟进:

###触发器的获取
调度器调用:
triggers = qsRsrcs.getJobStore().acquireNextTriggers(
now + idleWaitTime, Math.min(availThreadCount, qsRsrcs.getMaxBatchSize()), qsRsrcs.getBatchTimeWindow());
在数据库中查找一定时间范围内将会被触发的trigger.参数的意义如下:参数1:nolaterthan = now+3000ms,即未来30s内将会被触发.参数2 最大获取数量,大小取线程池线程剩余量与定义值得较小者.参数3 时间窗口 默认为0,程序会在nolaterthan后加上窗口大小来选择trigger.quratz会在每次触发trigger后计算出trigger下次要执行的时间,并在数据库QRTZ2_TRIGGERS中的NEXT_FIRE_TIME字段中记录.查找时将当前毫秒数与该字段比较,就能找出下一段时间内将会触发的触发器.查找时,调用在JobStoreSupport类中的方法:

```
public  List<OperableTrigger> acquireNextTriggers( final  long  noLaterThan,  final  int  maxCount,  final  long  timeWindow)
         throws  JobPersistenceException {
         
         String lockName;
         if (isAcquireTriggersWithinLock() || maxCount >  1 ) {
             lockName = LOCK_TRIGGER_ACCESS;
         }  else  {
             lockName =  null ;
         }
         return  executeInNonManagedTXLock(lockName,
                 new  TransactionCallback<List<OperableTrigger>>() {
                     public  List<OperableTrigger> execute(Connection conn)  throws  JobPersistenceException {
                         return  acquireNextTrigger(conn, noLaterThan, maxCount, timeWindow);
                     }
                 },
                 new  TransactionValidator<List<OperableTrigger>>() {
                     public  Boolean validate(Connection conn, List<OperableTrigger> result)  throws  JobPersistenceException {
                         //...异常处理回调方法
                     }
                 });
     }
``` 
该方法关键的一点在于执行了executeInNonManagedTXLock()方法,这一方法指定了一个锁名,两个回调函数.在开始执行时获得锁,在方法执行完毕后随着事务的提交锁被释放.在该方法的底层,使用 for update语句,在数据库中加入行级锁,保证了在该方法执行过程中,其他的调度器对trigger进行获取时将会等待该调度器释放该锁.此方法是前面介绍的quartz集群策略的的具体实现,这一模板方法在后面的trigger触发过程还会被使用.

public  static  final  String SELECT_FOR_LOCK =  "SELECT * FROM "
             + TABLE_PREFIX_SUBST + TABLE_LOCKS +  " WHERE "  + COL_SCHEDULER_NAME +  " = "  + SCHED_NAME_SUBST
             +  " AND "  + COL_LOCK_NAME +  " = ? FOR UPDATE" ;
进一步解释:quratz在获取数据库资源之前,先要以for update方式访问LOCKS表中相应LOCK_NAME数据将改行锁定.如果在此前该行已经被锁定,那么等待,如果没有被锁定,那么读取满足要求的trigger,并把它们的status置为STATE_ACQUIRED,如果有tirgger已被置为STATE_ACQUIRED,那么说明该trigger已被别的调度器实例认领,无需再次认领,调度器会忽略此trigger.调度器实例之间的间接通信就体现在这里.

JobStoreSupport.acquireNextTrigger()方法中:

int rowsUpdated = getDelegate().updateTriggerStateFromOtherState(conn, triggerKey, STATE_ACQUIRED, STATE_WAITING);

最后释放锁,这时如果下一个调度器在排队获取trigger的话,则仍会执行相同的步骤.这种机制保证了trigger不会被重复获取.按照这种算法正常运行状态下调度器每次读取的trigger中会有相当一部分已被标记为被获取.

获取trigger的过程进行完毕.

###触发trigger:
QuartzSchedulerThread line336:

List<TriggerFiredResult> res = qsRsrcs.getJobStore().triggersFired(triggers);

调用JobStoreSupport类的triggersFired()方法:
```
public  List<TriggerFiredResult> triggersFired( final  List<OperableTrigger> triggers)  throws  JobPersistenceException {
         return  executeInNonManagedTXLock(LOCK_TRIGGER_ACCESS,
                 new  TransactionCallback<List<TriggerFiredResult>>() {
                     public  List<TriggerFiredResult> execute(Connection conn)  throws  JobPersistenceException {
                         List<TriggerFiredResult> results =  new  ArrayList<TriggerFiredResult>();
                         TriggerFiredResult result;
                         for  (OperableTrigger trigger : triggers) {
                             try  {
                               TriggerFiredBundle bundle = triggerFired(conn, trigger);
                               result =  new  TriggerFiredResult(bundle);
                             }  catch  (JobPersistenceException jpe) {
                                 result =  new  TriggerFiredResult(jpe);
                             }  catch (RuntimeException re) {
                                 result =  new  TriggerFiredResult(re);
                             }
                             results.add(result);
                         }
                         return  results;
                     }
                 },
                 new  TransactionValidator<List<TriggerFiredResult>>() {
                     @Override
                     public  Boolean validate(Connection conn, List<TriggerFiredResult> result)  throws  JobPersistenceException {
                         //...异常处理回调方法
                     }
                 });
     }
``` 
此处再次用到了quratz的行为规范:executeInNonManagedTXLock()方法,在获取锁的情况下对trigger进行触发操作.其中的触发细节如下:
```
protected  TriggerFiredBundle triggerFired(Connection conn,
             OperableTrigger trigger)
         throws  JobPersistenceException {
         JobDetail job;
         Calendar cal =  null ;
         // Make sure trigger wasn't deleted, paused, or completed...
         try  {  // if trigger was deleted, state will be STATE_DELETED
             String state = getDelegate().selectTriggerState(conn,
                     trigger.getKey());
             if  (!state.equals(STATE_ACQUIRED)) {
                 return  null ;
             }
         }  catch  (SQLException e) {
             throw  new  JobPersistenceException( "Couldn't select trigger state: "
                     + e.getMessage(), e);
         }
         try  {
             job = retrieveJob(conn, trigger.getJobKey());
             if  (job ==  null ) {  return  null ; }
         }  catch  (JobPersistenceException jpe) {
             try  {
                 getLog().error( "Error retrieving job, setting trigger state to ERROR." , jpe);
                 getDelegate().updateTriggerState(conn, trigger.getKey(),
                         STATE_ERROR);
             }  catch  (SQLException sqle) {
                 getLog().error( "Unable to set trigger state to ERROR." , sqle);
             }
             throw  jpe;
         }
         if  (trigger.getCalendarName() !=  null ) {
             cal = retrieveCalendar(conn, trigger.getCalendarName());
             if  (cal ==  null ) {  return  null ; }
         }
         try  {
             getDelegate().updateFiredTrigger(conn, trigger, STATE_EXECUTING, job);
         }  catch  (SQLException e) {
             throw  new  JobPersistenceException( "Couldn't insert fired trigger: "
                     + e.getMessage(), e);
         }
         Date prevFireTime = trigger.getPreviousFireTime();
         // call triggered - to update the trigger's next-fire-time state...
         trigger.triggered(cal);
         String state = STATE_WAITING;
         boolean  force =  true ;
         
         if  (job.isConcurrentExectionDisallowed()) {
             state = STATE_BLOCKED;
             force =  false ;
             try  {
                 getDelegate().updateTriggerStatesForJobFromOtherState(conn, job.getKey(),
                         STATE_BLOCKED, STATE_WAITING);
                 getDelegate().updateTriggerStatesForJobFromOtherState(conn, job.getKey(),
                         STATE_BLOCKED, STATE_ACQUIRED);
                 getDelegate().updateTriggerStatesForJobFromOtherState(conn, job.getKey(),
                         STATE_PAUSED_BLOCKED, STATE_PAUSED);
             }  catch  (SQLException e) {
                 throw  new  JobPersistenceException(
                         "Couldn't update states of blocked triggers: "
                                 + e.getMessage(), e);
             }
         }
             
         if  (trigger.getNextFireTime() ==  null ) {
             state = STATE_COMPLETE;
             force =  true ;
         }
         storeTrigger(conn, trigger, job,  true , state, force,  false );
         job.getJobDataMap().clearDirtyFlag();
         return  new  TriggerFiredBundle(job, trigger, cal, trigger.getKey().getGroup()
                 .equals(Scheduler.DEFAULT_RECOVERY_GROUP),  new  Date(), trigger
                 .getPreviousFireTime(), prevFireTime, trigger.getNextFireTime());
     }
``` 
该方法做了以下工作:

1.获取trigger当前状态

2.通过trigger中的JobKey读取trigger包含的Job信息

3.将trigger更新至触发状态

4.结合calendar的信息触发trigger,涉及多次状态更新

5.更新数据库中trigger的信息,包括更改状态至STATE_COMPLETE,及计算下一次触发时间.

6.返回trigger触发结果的数据传输类TriggerFiredBundle

 

从该方法返回后,trigger的执行过程已基本完毕.回到执行quratz操作规范的executeInNonManagedTXLock方法,将数据库锁释放.

trigger触发操作完成

###Job执行过程:
再回到线程类QuartzSchedulerThread的 line353这时触发器都已出发完毕,job的详细信息都已就位
```
QuartzSchedulerThread line:368
qsRsrcs.getJobStore().releaseAcquiredTrigger(triggers.get(i));
shell.initialize(qs);
``` 

为每个Job生成一个可运行的RunShell,并放入线程池运行.

在最后调度线程生成了一个随机的等待时间,进入短暂的等待,这使得其他节点的调度器都有机会获取数据库资源.如此就实现了quratz的负载平衡.

这样一次完整的调度过程就结束了.调度器线程进入下一次循环.、


总结
简单地说,quartz的分布式调度策略是以数据库为边界资源的一种异步策略.各个调度器都遵守一个基于数据库锁的操作规则保证了操作的唯一性.同时多个节点的异步运行保证了服务的可靠