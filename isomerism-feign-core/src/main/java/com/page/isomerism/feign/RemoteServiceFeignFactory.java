package com.page.isomerism.feign;

import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;

/**
 * RemoteServiceFeignFactory
 *
 * @author page.xee
 * @date 2018/9/3
 */
public interface RemoteServiceFeignFactory {

    /**
     * 设置默认配置
     *
     * @param module module
     * @param flag   区分的flag
     */
    void setDefault(String module, String flag);

    /**
     * 设置发现扫描的时间间距
     * (单位 秒)
     * (默认 10)
     *
     * @param seconds 扫描的时间间距
     */
    void setScanPeriod(int seconds);

    /**
     * 获取接口的实现远端
     * (该实现屏蔽异常，无法获得远端实现代理将返回null)
     * (该方法建议用于明确module，flag的情况下调用)
     *
     * @param module   模块名称
     * @param flag     区分的flag
     * @param classOfT 远端接口Class
     * @param <T>      接口类型
     * @return 远端接口的代理实例
     */
    <T> T getRemoteServiceInstance(String module, String flag, Class<T> classOfT);

    /**
     * 获取接口的实现远端
     * (该方法建议用于调用同flag的实现)
     * (调用前请先调用setDefault)
     *
     * @param module   模块名称
     * @param classOfT 远端接口Class
     * @param <T>      接口类型
     * @return 远端接口的代理实例
     * @throws Exception Exception
     */
    <T> T getRemoteServiceInstance(String module, Class<T> classOfT) throws Exception;

    /**
     * 构造远程实现代理
     *
     * @param service  模块名称
     * @param flag     区分的flag
     * @param classOfT 目标接口类
     * @param decoder  协议节码器
     * @param encoder  协议编码器
     * @param <T>      泛型T
     * @return T /null
     */
    <T> T constructRemoteServiceInstance(String service, String flag, Class<T> classOfT, Decoder decoder, Encoder encoder);

    /**
     * 构造远程实现代理
     *
     * @param service      模块名称
     * @param flag         区分的flag
     * @param classOfT     目标接口类
     * @param decoder      协议节码器
     * @param encoder      协议编码器
     * @param <T>          泛型T
     * @param errorDecoder 错误Decoder
     * @param <T>          泛型T
     * @return T /null
     */
    <T> T constructRemoteServiceInstance(String service, String flag, Class<T> classOfT, Decoder decoder, Encoder encoder, ErrorDecoder errorDecoder);

}
