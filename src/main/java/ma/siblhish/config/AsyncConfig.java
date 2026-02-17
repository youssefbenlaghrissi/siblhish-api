package ma.siblhish.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration pour les opérations asynchrones.
 * Définit un thread pool pour contrôler le nombre de threads utilisés.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);           // Nombre de threads de base
        executor.setMaxPoolSize(10);           // Nombre maximum de threads
        executor.setQueueCapacity(100);        // Taille de la file d'attente
        executor.setThreadNamePrefix("async-"); // Préfixe pour les logs
        executor.setWaitForTasksToCompleteOnShutdown(true); // Attendre la fin des tâches à l'arrêt
        executor.setAwaitTerminationSeconds(60); // Temps d'attente max à l'arrêt
        executor.initialize();
        return executor;
    }
}

