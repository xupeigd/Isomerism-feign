package com.page.isomerism.feign;

import com.netflix.hystrix.*;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.hystrix.HystrixFeign;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 手动装配Feign Client的Bean
 *
 * @author page.xee
 * @date 2018/8/23
 */
@Service
@ConditionalOnProperty(
        value = {"isomerism.feign.hystrix.enabled"},
        havingValue = "true",
        matchIfMissing = false
)
public final class HystrixFeignRemoteServiceFeignFactory
        extends AbstractRemoteServiceFeignFactory
        implements RemoteServiceHystrixFeignFactory {

    /**
     * 超时时间
     * (單位：秒)
     */
    private int executionTimeoutInSeconds = 8;
    /**
     * 并发请求数量
     */
    private int executionIsolationSemaphoreMaxConcurrentRequests = 30;
    /**
     * 断路器打开的失败比例 (0-100)
     */
    private int circuitBreakerErrorThresholdPercentage = 50;
    /**
     * 斷路器閒置時間(打開/關閉觸發後下次觸發的間隔)
     * (單位：秒)
     */
    private int circuitBreakerSleepWindowInSeconds = 3;

    private ConcurrentMap<String, Object> hystrixRemoteServiceCaches = new ConcurrentHashMap<>(16);

    /**
     * 設置默認的超時時間
     *
     * @param executionTimeoutInSeconds 超时时间(单位:秒)
     */
    @Override
    public void setExecutionTimeoutInSeconds(int executionTimeoutInSeconds) {
        this.executionTimeoutInSeconds = executionTimeoutInSeconds;
    }

    /**
     * 設置併發的請求數量
     *
     * @param executionIsolationSemaphoreMaxConcurrentRequests 最大的并发请求数量
     */
    @Override
    public void setExecutionIsolationSemaphoreMaxConcurrentRequests(int executionIsolationSemaphoreMaxConcurrentRequests) {
        this.executionIsolationSemaphoreMaxConcurrentRequests = executionIsolationSemaphoreMaxConcurrentRequests;
    }

    /**
     * 设置断路器打开的失败比例
     *
     * @param circuitBreakerErrorThresholdPercentage 断路器触发的失败比例
     */
    @Override
    public void setCircuitBreakerErrorThresholdPercentage(int circuitBreakerErrorThresholdPercentage) {
        this.circuitBreakerErrorThresholdPercentage = circuitBreakerErrorThresholdPercentage;
    }

    /**
     * 设置断路器触发的冷却周期
     *
     * @param circuitBreakerSleepWindowInSeconds 断路器触发的冷却周期(单位:秒)
     */
    @Override
    public void setCircuitBreakerSleepWindowInSeconds(int circuitBreakerSleepWindowInSeconds) {
        this.circuitBreakerSleepWindowInSeconds = circuitBreakerSleepWindowInSeconds;
    }

    @Override
    public <T> T constructInstance(Class<T> classOfT, Decoder decoder, Encoder encoder, ErrorDecoder errorDecoder, String url) {
        return constructInstance(classOfT, decoder, encoder, errorDecoder, url, executionTimeoutInSeconds,
                executionIsolationSemaphoreMaxConcurrentRequests, circuitBreakerErrorThresholdPercentage, circuitBreakerSleepWindowInSeconds);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T constructInstance(Class<T> classOfT, Decoder decoder, Encoder encoder, ErrorDecoder errorDecoder, String url,
                                   int timeout, int concurrentRequests, int errorPrecentage, int coolingTime) {
        String hystrixRemoteCacheKey = getHystrixServiceCacheKey(classOfT, decoder, encoder, errorDecoder, url,
                timeout, concurrentRequests, errorPrecentage, coolingTime);
        Object object = hystrixRemoteServiceCaches.get(hystrixRemoteCacheKey);
        if (null != object) {
            return (T) object;
        }
        T remoteService =
                HystrixFeign.builder()
                        .setterFactory((target, method) -> {
                            String groupKey = target.name();
                            String commandKey = Feign.configKey(target.type(), method);
                            return HystrixCommand.Setter
                                    .withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
                                    .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey))
                                    .andCommandPropertiesDefaults(
                                            HystrixCommandProperties.Setter()
                                                    .withExecutionTimeoutEnabled(true)
                                                    .withExecutionTimeoutInMilliseconds(timeout * 1000)
                                                    .withExecutionIsolationSemaphoreMaxConcurrentRequests(concurrentRequests)
                                                    .withCircuitBreakerEnabled(true)
                                                    .withCircuitBreakerErrorThresholdPercentage(errorPrecentage)
                                                    .withCircuitBreakerSleepWindowInMilliseconds(coolingTime * 1000)
                                    ).andThreadPoolPropertiesDefaults(
                                            HystrixThreadPoolProperties.Setter()
                                                    .withCoreSize(30)
                                                    .withMaximumSize(30)
                                                    .withAllowMaximumSizeToDivergeFromCoreSize(true)
                                    );
                        })
                        .contract(new SpringMvcContract())
                        .decoder(null != decoder ? decoder : new SpringDecoder(messageConverters))
                        .encoder(null != encoder ? encoder : new SpringEncoder(messageConverters))
                        .errorDecoder(errorDecoder)
                        .target(classOfT, url);
        hystrixRemoteServiceCaches.put(hystrixRemoteCacheKey, remoteService);
        return remoteService;
    }

    private String getHystrixServiceCacheKey(Class<?> classOfT, Decoder decoder, Encoder encoder, ErrorDecoder errorDecoder, String url,
                                             int timeout, int concurrentRequests, int errorPrecentage, int coolingTime) {
        return classOfT.getName() + "丨" +
                (null == decoder ? SpringDecoder.class.getSimpleName() : decoder.getClass().getSimpleName()) + "丨" +
                (null == encoder ? SpringEncoder.class.getSimpleName() : encoder.getClass().getSimpleName()) + "丨" +
                (null == errorDecoder ? ErrorDecoder.Default.class.getSimpleName() : errorDecoder.getClass().getSimpleName()) + "丨" +
                url + "丨" +
                timeout + "丨" +
                concurrentRequests + "丨" +
                errorPrecentage + "丨" +
                coolingTime;
    }


    @Override
    protected void cleanServiceInstances(Set<String> serviceURIs) {
        super.cleanServiceInstances(serviceURIs);
        //移除已经失效的实例缓存
        hystrixRemoteServiceCaches.entrySet().removeIf(next -> !serviceURIs.contains(next.getKey().split("丨")[4]));
    }

}
