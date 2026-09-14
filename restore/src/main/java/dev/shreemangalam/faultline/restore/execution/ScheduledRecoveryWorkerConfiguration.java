package dev.shreemangalam.faultline.restore.execution;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(ScheduledRecoveryWorkerProperties.class)
@ConditionalOnProperty(prefix = "restore.worker", name = "enabled", havingValue = "true")
class ScheduledRecoveryWorkerConfiguration {
}
