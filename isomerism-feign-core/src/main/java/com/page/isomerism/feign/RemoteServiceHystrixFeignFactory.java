package com.page.isomerism.feign;

import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;

/**
 * RemoteServiceHystrixFeignFactory
 *
 * @author page.xee
 * @date 2018/9/3
 */
public interface RemoteServiceHystrixFeignFactory
        extends RemoteServiceFeignFactory {

    /**
     * 設置默認的超時時間
     *
     * @param executionTimeoutInSeconds 超時時間
     */
    void setExecutionTimeoutInSeconds(int executionTimeoutInSeconds);

    /**
     * 設置併發的請求數量
     *
     * @param executionIsolationSemaphoreMaxConcurrentRequests 最大并发请求数
     */
    void setExecutionIsolationSemaphoreMaxConcurrentRequests(int executionIsolationSemaphoreMaxConcurrentRequests);

    /**
     * 设置断路器打开的失败比例
     *
     * @param circuitBreakerErrorThresholdPercentage 失败比例
     */
    void setCircuitBreakerErrorThresholdPercentage(int circuitBreakerErrorThresholdPercentage);

    /**
     * 设置断路器触发的冷却周期
     *
     * @param circuitBreakerSleepWindowInSeconds 冷却周期(秒)
     */
    void setCircuitBreakerSleepWindowInSeconds(int circuitBreakerSleepWindowInSeconds);

    /**
     * 构造实例
     *
     * @param classOfT     T的class
     * @param decoder      解码器
     * @param encoder      编码器
     * @param errorDecoder 错误Decoder
     * @param url          请求Url
     * @param <T>          接口类型
     * @return proxy instance of T / null
     */
    <T> T constructInstance(Class<T> classOfT, Decoder decoder, Encoder encoder, ErrorDecoder errorDecoder, String url);

    /**
     * 构造HystrixFeign的远程调用代理对象
     *
     * @param classOfT
     * @param decoder
     * @param encoder
     * @param errorDecoder
     * @param url
     * @param timeout            超时时间(秒)
     * @param concurrentRequests 并发请求数
     * @param errorPrecentage    错误百分比
     * @param coolingTime        冷却时间
     * @param <T>                接口类型
     * @return proxy  of T / null
     */
    <T> T constructInstance(Class<T> classOfT, Decoder decoder, Encoder encoder, ErrorDecoder errorDecoder, String url,
                            int timeout, int concurrentRequests, int errorPrecentage, int coolingTime);

}
