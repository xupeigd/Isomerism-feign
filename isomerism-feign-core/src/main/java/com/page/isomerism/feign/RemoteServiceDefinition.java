package com.page.isomerism.feign;

/**
 * 远端接口定义
 *
 * @author page.xee
 * @date 2018/8/24
 */
public class RemoteServiceDefinition<T> {

    /**
     * module 模块名称
     */
    private String module;
    /**
     * flag 区分的flag
     * <p>
     */
    private String flag;
    /**
     * T的Class
     */
    private Class<T> clazz;
    /**
     * 创建时间
     */
    private long createTime;
    /**
     * 连接地址
     */
    private String uri;

    /**
     * 远程接口实例
     */
    private T service;

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public T getService() {
        return service;
    }

    public void setService(T service) {
        this.service = service;
    }

    public Class<T> getClazz() {
        return clazz;
    }

    public void setClazz(Class<T> clazz) {
        this.clazz = clazz;
    }

    private RemoteServiceDefinition(String module, String flag, Class<T> clazz, long createTime, String uri, T service) {
        this.module = module;
        this.flag = flag;
        this.clazz = clazz;
        this.createTime = createTime;
        this.service = service;
        this.uri = uri;
    }


    public static <E> RemoteServiceDefinition<E> buildCustomed(String module, String flag, Class<E> clazz, long createTime, String uri, E service) {
        return new RemoteServiceDefinition<>(module, flag, clazz, createTime, uri, service);
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

}
