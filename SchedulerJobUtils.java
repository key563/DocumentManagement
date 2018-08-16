package com.npacn.amc.common.utils;

import com.npacn.amc.modules.sys.entity.ReminderWarning;
import com.npacn.amc.modules.sys.service.ReminderWarningService;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Calendar;

import static com.npacn.amc.common.utils.DateCaculate.*;

/**
 * Created by wj on 2017/4/7.
 */
public class SchedulerJobUtils {
    private static Logger logger = LoggerFactory.getLogger(SchedulerJobUtils.class);

    //    public static Scheduler scheduler = (Scheduler) SystemContent.applicationContext.getBean("schedulerBeanFactory");
    public static Scheduler scheduler = (Scheduler) SpringContextHolder.getBean("schedulerBeanFactory");
    public static ReminderWarningService reminderWarningService = SpringContextHolder.getBean(ReminderWarningService.class);
    public static String dateFormate = "ss mm HH dd MM ? yyyy";
    public static SimpleDateFormat cronSdf = new SimpleDateFormat(dateFormate);
    public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 数据库放入一个指定时间的任务，任务只执行一次
     * 适合对数据进行定时删除，修改等等
     * @param <T>
     * @param clazz  要执行任务类class，任务类必须继承Job类
     * @param jobName 任务名称
     * @param groupName 分组名称
     * @param executeTime 执行时间，格式:yyyy-MM-dd HH:mm:ss
     * @param cronTime  触发器时间表达式
     * @return
     */
    public static<T> boolean putSpecifyTimeSchedulerJobToDB(Class<? extends Job> clazz, String jobName, String groupName, String executeTime, String cronTime) throws SchedulerException {

        //定义一个job
        JobDetail job = JobBuilder.newJob(clazz).withIdentity(jobName, groupName).build();
        //定义一个TriggerKey
        TriggerKey triggerKey = TriggerKey.triggerKey(jobName, groupName);

        ReminderWarning schedulerJob = new ReminderWarning();
        schedulerJob.setReminderName(jobName);
        schedulerJob.setGroupName(groupName);
        job.getJobDataMap().put("schedulerJob", schedulerJob);

        Date executeDate = null;

        try {
            executeDate = sdf.parse(executeTime);
        } catch (ParseException e) {
            logger.info("method putSchedulerJobToDB execute error!exception={}",e);
        }
        String dbExecuteTime = cronSdf.format(executeDate);
        CronTrigger cronTrigger = TriggerBuilder.newTrigger().withIdentity(triggerKey).withSchedule(CronScheduleBuilder.cronSchedule(dbExecuteTime)).build();

        try {
            scheduler.scheduleJob(job, cronTrigger);
        } catch (SchedulerException e) {
            logger.info("method putSchedulerJobToDB execute error!exception={}",e);
        }
        return true;
    }

    /**
     * 数据库放入一条指定开始时间，指定间隔时间，指定次数的任务
     * @param <T>
     * @param clazz 任务类
     * @param jobName  任务名
     * @param groupName 分组名
     * @param seconds   间隔时间，以秒为单位
     * @param count 执行次数，0为一直执行
     * @param startDayToNow 开始时间(距离现在?天)
     * @return
     * @throws SchedulerException
     */
    @SuppressWarnings("unchecked")
    public static<T> boolean putPeriodSchedulerJobToDB(Class<? extends Job> clazz, String jobName, String groupName,
                                                       int seconds, int count, int startDayToNow) throws SchedulerException {

        JobDetail jobDetail= JobBuilder.newJob(clazz).withIdentity(jobName,groupName).build();
        TriggerBuilder triggerBuilder = TriggerBuilder.newTrigger().withIdentity(jobName,groupName);
        if(startDayToNow==0){
            triggerBuilder.startNow();
        }else{
            triggerBuilder.startAt(DateBuilder.futureDate(startDayToNow, DateBuilder.IntervalUnit.SECOND));
        }
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(seconds);
        if(count==0){
            scheduleBuilder.repeatForever();
        }else{
            scheduleBuilder.withRepeatCount(count);
        }
        Trigger trigger = triggerBuilder.withSchedule(scheduleBuilder).build();
        try {
            scheduler.scheduleJob(jobDetail,trigger);
        } catch (SchedulerException e) {
            logger.info("put putPeriodSchedulerJobToDB error,error={}",e);
        }
        return true;
    }

    /**
     * 启动所有定时任务
     */
    public static void startAllScheduler(){
        try{
            scheduler.start();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    /**
     * 中止定所有时任务
     */
    public static void pauseScheduler() {
        try {
            if(!scheduler.isShutdown()){
                scheduler.shutdown();
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    /**
     * 终止某个定时任务
     * @param jobName   任务名称
     * @param groupName 任务组名称
     */
    public  static  void pauseScheduler(String jobName,String groupName){
        try {
            scheduler.pauseJob(JobKey.jobKey(jobName,groupName));
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }


    /**
     * 数据库放入一个指定时间的任务，任务只执行一次
     * 适合对数据进行定时删除，修改等等
     * @param <T>
     * @param clazz  要执行任务类class，任务类必须继承Job类
     * @param jobName 任务名称
     * @param groupName 分组名称
     * @param ruleType 时效提醒类型
     * @param  map  相关的实体类
     * @param date  作为提醒的参考日期（一般为提前提醒时作为参数）
     * @return
     */
    public static<T> boolean put(Class<? extends Job> clazz, String jobName, String groupName, String ruleType, Map<String,T> map, Date date) throws SchedulerException, ParseException {
        ReminderWarning re = reminderWarningService.findByRuleType(ruleType);
        //判断时效提醒类型是否存在
        if(re != null && !re.equals("")){

            //定义一个job
            JobDetail job = JobBuilder.newJob(clazz)
                    .withIdentity(jobName, groupName)
                    .storeDurably() //对于一个JobDetail配置多个触发器时的操作
                    .build();
            Set<String> set = map.keySet();
            //传参
            for(String key:set){
                job.getJobDataMap().put(key,map.get(key));
            }

            //首先添加job
            scheduler.addJob(job,true);
            //定义触发器
            CronTrigger cronTrigger ;

            //获取触发表达式
            List<String> expressions = cronTriggeExpression(ruleType);
            if(expressions==null || (expressions!= null && expressions.size()==0)){
                scheduler.deleteJob(JobKey.jobKey(jobName,groupName));
            }
            //根据触发表达式的数量，生成对应数量的触发操作
            for(String expr:expressions) {
                CronExpression expression = new CronExpression(expr);
                //定义一个TriggerKey
                String id = IdGen.uuid();
                TriggerKey triggerKey = TriggerKey.triggerKey(id, id);
                //获取触发时间
                if (re.getReminderType() != null && re.getReminderType().equals("1")) {
                    //为提前提醒，触发的时间应该是提前一定的天数才触发
                    String beforeType = re.getBeforeType();
                    String repeatType = re.getRepeatType();
                    Date start = startTime(date,Integer.valueOf(re.getReminderNum()),true,beforeType);
                    if(StringUtils.isNotBlank(repeatType) && repeatType.equals("1")){
                        //不重复,即执行一次
                        Trigger trigger = TriggerBuilder.newTrigger()
                                .withIdentity(triggerKey)
                                .forJob(job)
                                .startAt(start)
                                .build();
                        scheduler.scheduleJob(trigger);
                    }else if(repeatType.equals("2")){
                        //每天重复，开始时间为提前的日期，结束时间则为date这个参考日期（单是否结束通过代码来判断，而不是在这里直接设置结束）
                        cronTrigger = TriggerBuilder.newTrigger()
                                .withIdentity(triggerKey)
                                .forJob(job)
                                .withSchedule(CronScheduleBuilder.cronSchedule(expression))
                                .startAt(start)
                                .build();
                        try {
                            scheduler.scheduleJob(cronTrigger);
                        } catch (SchedulerException e) {
                            logger.info("method putSchedulerJobToDB execute error!exception={}", e);
                        }
                    }
                } else {
                    //其余的则是立即触发
                    cronTrigger = TriggerBuilder.newTrigger()
                            .withIdentity(triggerKey)
                            .forJob(job)    //表示对应某个Job任务
                            .withSchedule(CronScheduleBuilder.cronSchedule(expression))
                            .startNow()
                            .build();
                    try {
                        scheduler.scheduleJob(cronTrigger);
                    } catch (SchedulerException e) {
                        logger.info("method putSchedulerJobToDB execute error!exception={}", e);
                    }
                }

            }

        }
        return true;
    }

    /**
     * 返回触发开始时间,以天为单位
     * @param date  参考日期
     * @param num   提前或推迟天数
     * @param flag  true表示提前，false表示推迟
     * @return
     */
    public static Date startTime(Date date,int num,Boolean flag){
        Calendar calendar = new GregorianCalendar();
        if(date != null){
            calendar.setTime(date);
        }else{
            calendar.setTime(new Date());
        }

        if(flag){
            calendar.add(Calendar.DATE,-num);
        }else{
            calendar.add(Calendar.DATE,num);
        }
        return calendar.getTime();
    }



    /**
     * 返回触发开始时间，以周为单位
     * @param date  参考日期
     * @param num   提前或推迟的周数
     * @param flag  true表示提前，false表示推迟
     * @param type  提前类型，天，周，月
     * @return
     */
    public static Date startTime(Date date,int num,Boolean flag,String type){
        if(type == null){
            return date;
        }
        if(type.equals("1")){
            //提前天数
            if(flag){
                return addDays(date,num);
            }else{
                return delDays(date,num);
            }
        }else if(type.equals("2")){
            //提前周数
            if(flag){
                return addWeeks(date,num);
            }else{
                return delWeeks(date,num);
            }
        }else if(type.equals("3")){
            //提前月数
            if(flag){
                return addMonths(date,num);
            }else{
                return delMonths(date,num);
            }
        }
        return date;
    }


    /**
     * 根据时效类型，返回对应的触发时间表达式
     * 默认：通知时间为该天的0点
     * @param ruleType  时效规则类型
     * @return  返回触发表达式（可以是多个表达式）
     */
    public static List<String> cronTriggeExpression(String ruleType){


        List<String> list = new ArrayList<>();
        String expression = "0 0 0 * * ?";
        Calendar calendar = new GregorianCalendar();

        if(ruleType != null && !ruleType.equals("")){
            ReminderWarning re= reminderWarningService.findByRuleType(ruleType);
            if(re != null && re.getReminderType()!= null && !re.getReminderType().equals("")){
                //根据时效提醒的类型，返回对应的触发时间表达式
                String reminderType = re.getReminderType();
                if(reminderType.equals("1")){
                    //提前天数提醒,提前多少天开始触发通知
                    //这里具有不确定性，因为提前是根据具体的某个时间开始提前的，所以要有一个参考值
                    //每天0点触发
                    //重复方式，不同的重复方式，触发的时效不同，主要有不重复，每天重复，每周重复...

                    String repeatType = re.getRepeatType();
                    if(StringUtils.isNotBlank(repeatType)){
                        if(reminderType.equals("1")){
                            //不重复，只会触发一次，则
                            expression = "0 0 0 * * ?";
                        }else if(reminderType.equals("2")){
                            //每天重复
                            expression = "0 0 0 * * ?";
                        }
                    }


                    list.add(expression);
                    //
                }else if(reminderType.equals("2")){
                    //每年提醒,在每年的某天提醒
                    Date annual = re.getReminderDate();
                    calendar.setTime(annual);
                    //Calender计算的月份从0开始的,需要+1
                    int month = calendar.get(Calendar.MONTH)+1;
                    int day = calendar.get(Calendar.DATE);
                    //每年的某天触发
                    expression = "0 0 0 "+day+" "+month+" ?";
                    list.add(expression);
                }else if(reminderType.equals("3")){
                    //每季度提醒,每个季度的第几月的几号提醒,即每三个月提醒一次
                    String month = re.getMonths();
                    String days = re.getDays();
                    if(month.equals("1")){
                        month = "1/3";
                    }else if(month.equals("2")){
                        month = "2/3";
                    }else if(month.equals("3")){
                        month = "3/3";
                    }

                    if(days.equals("0")){
                        //月初
                        days = "1";
                        expression = "0 0 0 "+days+" "+month+" ?";
                        list.add(expression);
                    }else if(days.equals("32") || days.equals("31")){
                        //月末
                        days = "L";
                        expression = "0 0 0 "+days+" "+month+" ?";
                        list.add(expression);
                    }else if(days.equals("30") || days.equals("29")){
                        expression = "0 0 0 "+days+" "+month+" ?";
                        if(month.equals("1/3")){
                            //1,4,7,10月
                            //1,7,10分别为31天,4月则30天,均有30号或29号,不需要额外触发
                            expression = "0 0 0 "+days+" "+month+" ?";
                            list.add(expression);
                        }else if(month.equals("2/3")){
                            //2,5,8,11,，，
                            //5,8月为31天,11月为30天
                            expression = "0 0 0 "+days+" 5,8,11 ?";
                            list.add(expression);
                            //2月为29或28天,直接设置为月末,需要额外触发
                            expression = "0 0 0 L 2 ?";
                            list.add(expression);

                        }else if(month.equals("0/3")){
                            //3,6,9,12月
                            //3,6,9月为30天，12月为31天,均有30号或29号,不需要额外触发
                            expression = "0 0 0 "+days+" "+month+" ?";
                            list.add(expression);
                        }
                    }else{
                        expression = "0 0 0 "+days+" "+month+" ?";
                        list.add(expression);
                    }

                }else if(reminderType.equals("4")){
                    //每月提醒,每个月的某天提醒
                    String days = re.getDays();
                    expression = "0 0 0 "+days+" * ?";
                    if(days.equals("0")){
                        //月初
                        expression = "0 0 0 1 * ?";
                        list.add(expression);
                    }else if(days.equals("32") || days.equals("31")){
                        //月末
                        expression = "0 0 0 L * ?";
                        list.add(expression);
                    }else if(days.equals("30") || days.equals("29")){
                        list.add(expression);
                        //需要将那些没有30/29号的月份，设置为月末执行,即需要两个触发器
                        expression = "0 0 0 L 2 ?";
                        list.add(expression);
                    }else{
                        expression = "0 0 0 "+days+" * ?";
                       list.add(expression);
                    }
                }else if(reminderType.equals("5")){
                    //每周提醒，每周的星期几执行
                    String weeks = re.getWeek();
                    expression = "0 0 0 ? * "+weeks;
                    list.add(expression);
                }
            }
        }

        return list;
    }

//    /**
//     * 添加一个借阅的定时任务
//     * @param documentBorrowed
//     */
//    private void addDocumentBorrowQuartz(DocumentBorrowed documentBorrowed) throws SchedulerException, ParseException {
//        //借阅的定时任务是以天为单位的，即从时效提醒中读取借阅时效提醒的类型和数值，然后添加对应的触发器和定时任务
//        ReminderWarning re = reminderWarningService.findByRuleType("document_borrowed_warning");
//        if (re != null) {
//            Scheduler scheduler = SpringContextHolder.getBean("schedulerBeanFactory");
//
//            //这里先直接假设已知提醒类型为提前提醒
//            String reminderType = re.getReminderType();
//            //根据借阅提醒规则，添加定时任务
//            Date date = documentBorrowed.getExpectRestoreDate();
//
//            Date now = new Date();
//            //需要计算应该从第几天开始出发此定时任务
//            Calendar calender = new GregorianCalendar();
//            calender.setTime(date);
//            //计算触发任务的时间
//            calender.add(Calendar.DATE, -Integer.parseInt(re.getReminderNum()));
//
//            JobDetail jobDetail = JobBuilder.newJob(DocumentBorrowedJob.class)
//                    .withIdentity(documentBorrowed.getId(), documentBorrowed.getId())
//                    .build();
//            jobDetail.getJobDataMap().put("documentBorrowed", documentBorrowed);
//            //关于触发时间的表达式
//            //借阅文档应该为每天0点触发一次通知
//            CronExpression ce = new CronExpression("*/10 * * * * ? ");
//            Trigger trigger = TriggerBuilder.newTrigger()
//                    .withIdentity(documentBorrowed.getId(), documentBorrowed.getId())
//                    .withSchedule(CronScheduleBuilder.cronSchedule(ce))
//                    .startAt(calender.getTime())
//                    .build();
//            scheduler.scheduleJob(jobDetail, trigger);
//            scheduler.start();
//        }
//    }
//

}
