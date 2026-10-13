package com.kw.ebook.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    /** 注入 Clock 而非直接呼叫 Instant.now()，利於測試以固定時鐘驗證時間相關行為。 */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
