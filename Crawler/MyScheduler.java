package crawler;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

/**
 *
 * @author menna
 */
public class MyScheduler {
    public static void main(String[] args) throws SchedulerException
    {
        JobDetail J = JobBuilder.newJob(Master.class).build();
        Trigger t =TriggerBuilder.newTrigger()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInHours(24).repeatForever())
                .build();
        Scheduler scheduler =StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();
        scheduler.scheduleJob(J, t);
    }
}