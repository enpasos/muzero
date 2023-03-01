package ai.enpasos.muzero.platform.agent.rational.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class EnableAsyncConfig {

    @Bean
    public Executor taskExecutor() {
        // Async thread pool configuration
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //  Setting "queueCapacity" to 0 mimics Executors.newCachedThreadPool(), with immediate scaling of threads in
        //  the pool to a potentially very high number. Consider also setting a "maxPoolSize" at that point, as well as
        //  possibly a higher "corePoolSize" (see also the "allowCoreThreadTimeOut" mode of scaling).
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("GameRunner-");
        executor.initialize();
        executor.setWaitForTasksToCompleteOnShutdown(false);
        return executor;
    }




}
