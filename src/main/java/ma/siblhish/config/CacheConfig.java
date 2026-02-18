package ma.siblhish.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CATEGORIES = "categories";
    public static final String BUDGETS = "budgets";
    public static final String USERS = "users";
    public static final String EXPENSES = "expenses";
    public static final String INCOMES = "incomes";
    public static final String GOALS = "goals";
    public static final String BALANCE = "balance";
    public static final String RECENT_TRANSACTIONS = "recentTransactions";
    public static final String SCHEDULED_PAYMENTS = "scheduledPayments";
    public static final String NOTIFICATIONS = "notifications";
    public static final String STATISTICS = "statistics";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCacheNames(List.of(
                CATEGORIES, BUDGETS, USERS, EXPENSES, INCOMES, GOALS,
                BALANCE, RECENT_TRANSACTIONS, SCHEDULED_PAYMENTS, NOTIFICATIONS, STATISTICS
        ));
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES));
        return manager;
    }
}
