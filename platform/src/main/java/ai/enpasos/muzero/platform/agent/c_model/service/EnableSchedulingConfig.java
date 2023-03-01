package ai.enpasos.muzero.platform.agent.c_model.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.time.Duration;

@Configuration
@EnableScheduling
public class EnableSchedulingConfig implements SchedulingConfigurer {

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


    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(scheduledTaskExecutor());
        taskRegistrar.addFixedDelayTask(
            () -> {

                    modelController.run();

            },
            Duration.ofMillis(1)
        );
    }

}
