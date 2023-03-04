package ai.enpasos.muzero.platform.agent.c_model.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableScheduling
public class EnableSchedulingConfig  implements SchedulingConfigurer {

    @Autowired
    ModelController modelController;


    @Bean()
    public ThreadPoolTaskScheduler scheduledTaskExecutor() {
        ThreadPoolTaskScheduler t = new ThreadPoolTaskScheduler();
        t.setPoolSize(1);
        t.setThreadNamePrefix("ModelController-");
        t.initialize();

        return t;
    }

//    @PostConstruct
//    void init() {
//        ExecutorService executorService = Executors.newSingleThreadExecutor();
//        //executorService.
////        SimpleAsyncTaskExecutor t = new SimpleAsyncTaskExecutor();
////        t.setThreadNamePrefix("ModelController-");
//
//        try {
//            executorService.submit(  () -> {
//                modelController.run();
//
//            }).get();
//        } catch (Exception e) {
//           e.printStackTrace();
//        }
//
//        executorService.shutdown();
//
//    }


    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(scheduledTaskExecutor());

        taskRegistrar.addFixedDelayTask(
            () -> {
                    modelController.run();

            },
            Duration.ofMillis(Long.MAX_VALUE)
           // Duration.ofMillis(10)
        );
    }

}
