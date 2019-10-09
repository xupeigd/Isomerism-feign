package com.page.isomerism.feign;

import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 抽象的RemoteServiceFeignFactory
 *
 * @author page.xee
 * @date 2018/11/22
 */
public abstract class AbstractRemoteServiceFeignFactory
        implements RemoteServiceFeignFactory {

    private Logger logger = LoggerFactory.getLogger(AbstractRemoteServiceFeignFactory.class);

    @Autowired
    protected DiscoveryClient discoveryClient;
    @Autowired
    protected ObjectFactory<HttpMessageConverters> messageConverters;

    private String defaultModule = "";
    private String defaultFlag = "";
    private final byte[] objectLock = new byte[1];
    private final static ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private int scanPeriod = 10;

    /**
     * 远端接口存照
     * <p>
     * key ${RemoteServiceDefinition.service}-${RemoteServiceDefinition.flag}-${RemoteServiceDefinition.clazz}
     * value RemoteServiceDefinition
     */
    private final ConcurrentHashMap<String, RemoteServiceDefinition<?>> remoteServiceCaches = new ConcurrentHashMap<>();

    /**
     * 模块节点定义存照
     * <p>
     * key ${ModuleDefinition.service}-${ModuleDefinition.flag}
     * value ModuleDefinition
     */
    private ConcurrentHashMap<String, ModuleDefinition> moduleClientDefinitions = new ConcurrentHashMap<>();

    /**
     * 自定义远端接口存照
     * <p>
     * key ${RemoteServiceDefinition.service}-${RemoteServiceDefinition.flag}-${RemoteServiceDefinition.clazz}-${encoder.class}-${decoder.class}-${errorDecoder.class}
     * value RemoteServiceDefinition
     */
    private ConcurrentHashMap<String, RemoteServiceDefinition<?>> customRemoteServiceCaches = new ConcurrentHashMap<>();

    @PostConstruct
    protected void initScheduleJob() {
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            if (0 < scanPeriod) {
                long currentTimeMillis = System.currentTimeMillis();
                currentTimeMillis -= currentTimeMillis % 1000L;
                if (0 == (currentTimeMillis / 1000L) % scanPeriod) {
                    scanModules();
                }
            }
        }, 3 * 1000L, 1000L, TimeUnit.MILLISECONDS);
    }

    /**
     * 设置扫描周期
     * <p>
     * 单位 秒
     * (默认 30s)
     *
     * @param seconds 扫描间隔
     */
    @Override
    public void setScanPeriod(int seconds) {
        this.scanPeriod = Math.abs(seconds);
    }

    /**
     * 扫描注册在Eureka上面的模块服务
     */
    private void scanModules() {
        Set<ModuleDefinition> moduleDefinitions = new HashSet<>();
        List<String> services = discoveryClient.getServices();
        /* 扫描Eureka的注册节点情况，获取相应的模块节点定义 */
        Set<String> serviceURIs = new HashSet<>();
        for (String service : services) {
            List<ServiceInstance> instances = discoveryClient.getInstances(service);
            for (ServiceInstance serviceInstance : instances) {
                String nodeModule = serviceInstance.getMetadata().get(ModuleDefinition.FIELD_NODE_MODULE);
                String nodePrjkey = serviceInstance.getMetadata().get(ModuleDefinition.FIELD_NODE_FLAG);
                /* 三个字段不齐全不能通过这里进行实例化 */
                if (StringUtils.isEmpty(nodeModule) || StringUtils.isEmpty(nodePrjkey)) {
                    continue;
                }
                boolean sameModule = defaultModule.equalsIgnoreCase(nodeModule);
                ModuleDefinition moduleClientDefinition = new ModuleDefinition();
                moduleClientDefinition.setFlag(nodePrjkey.toUpperCase());
                moduleClientDefinition.setModule(nodeModule.toUpperCase());
                moduleClientDefinition.setUri(String.valueOf(serviceInstance.getUri()));
                moduleClientDefinition.setSameModule(sameModule);
                moduleDefinitions.add(moduleClientDefinition);
                serviceURIs.add(String.valueOf(serviceInstance.getUri()));
            }
        }
        if (!moduleDefinitions.isEmpty()) {
            synchronized (objectLock) {
                this.moduleClientDefinitions.clear();
                for (ModuleDefinition moduleDefinition : moduleDefinitions) {
                    String definitionKey = moduleDefinition.getModule() + "-" + moduleDefinition.getPrjKey();
                    this.moduleClientDefinitions.put(definitionKey, moduleDefinition);
                }
            }
        }
        cleanServiceInstances(serviceURIs);
    }

    protected void cleanServiceInstances(Set<String> serviceURIs) {
        //检查已经初始化的模块实例(移除失效的uri)
        if (!remoteServiceCaches.isEmpty()) {
            remoteServiceCaches.entrySet().removeIf(next -> !serviceURIs.contains(next.getValue().getUri()));
        }
        if (!customRemoteServiceCaches.isEmpty()) {
            customRemoteServiceCaches.entrySet().removeIf(next -> !serviceURIs.contains(next.getValue().getUri()));
        }
    }

    @Override
    public <T> T constructRemoteServiceInstance(String service, String flag, Class<T> classOfT, Decoder decoder, Encoder encoder) {
        return constructRemoteServiceInstance(service, flag, classOfT, decoder, encoder, new ErrorDecoder.Default());
    }

    /**
     * 尝试构造实例
     *
     * @param service  对应的模块名称
     * @param flag     flag
     * @param classOfT 接口的Class
     * @param <T>      接口的类
     * @return 成功: RemoteServiceDefinition 失败 null
     */
    @SuppressWarnings("unchecked")
    private <T> RemoteServiceDefinition<T> constructInstance(String service, String flag, Class<T> classOfT) {
        /*
         * 检查模块接口定义存在是否存在
         */
        String moduleCacheKey = service + "-" + flag;
        ModuleDefinition moduleDefinition = moduleClientDefinitions.get(moduleCacheKey);
        if (null == moduleDefinition) {
            return null;
        }
        String serviceCacheKey = service + "-" + flag + classOfT.getName();
        T remoteService = constructRemoteServiceInstance(service, flag, classOfT, new SpringDecoder(messageConverters), new SpringEncoder(messageConverters), new ErrorDecoder.Default());
        if (null == remoteService) {
            return null;
        }
        RemoteServiceDefinition<T> tRemoteServiceDefinition = RemoteServiceDefinition.buildCustomed(service, flag, classOfT, System.currentTimeMillis(), moduleDefinition.getUri(), remoteService);
        /*
         * 再次检查在初始化期间是否有其他实例生成
         */
        RemoteServiceDefinition<T> remoteServiceDefinition = (RemoteServiceDefinition<T>) remoteServiceCaches.get(serviceCacheKey);
        if (null != remoteServiceDefinition) {
            return remoteServiceDefinition;
        }
        synchronized (remoteServiceCaches) {
            remoteServiceDefinition = (RemoteServiceDefinition<T>) remoteServiceCaches.get(serviceCacheKey);
            if (null != remoteServiceDefinition) {
                return remoteServiceDefinition;
            }
            remoteServiceCaches.put(serviceCacheKey, tRemoteServiceDefinition);
            return tRemoteServiceDefinition;
        }
    }

    private String getServiceCacheKey(String module, String flag, Class<?> classOfT) {
        return module + "-" + flag + classOfT.getName();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getRemoteServiceInstance(String module, String flag, Class<T> classOfT) {
        if (StringUtils.isEmpty(module) || StringUtils.isEmpty(flag)) {
            return null;
        }
        module = module.toUpperCase();
        flag = flag.toUpperCase();
        //检查远端接口存照是否存在对应的接口实例
        String cacheKey = getServiceCacheKey(module, flag, classOfT);
        RemoteServiceDefinition<?> remoteServiceDefinition = remoteServiceCaches.get(cacheKey);
        if (null != remoteServiceDefinition) {
            return (T) remoteServiceDefinition.getService();
        }
        //远端接口存照不存在，则尝试实例化
        RemoteServiceDefinition<T> tRemoteServiceDefinition = constructInstance(module, flag, classOfT);
        if (null != tRemoteServiceDefinition) {
            return tRemoteServiceDefinition.getService();
        }
        return null;
    }

    private void checkDefault() throws Exception {
        if (StringUtils.isEmpty(defaultModule) && StringUtils.isEmpty(defaultFlag)) {
            throw new Exception("Default Module & Default PrjKey is empty! ");
        }
    }

    @Override
    public void setDefault(String module, String flag) {
        this.defaultModule = module;
        this.defaultFlag = flag;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getRemoteServiceInstance(String module, Class<T> classOfT) throws Exception {
        checkDefault();
        return getRemoteServiceInstance(module, defaultFlag, classOfT);
    }

    protected <T> T constructInstance(Class<T> classOfT, Decoder decoder, Encoder encoder, ErrorDecoder errorDecoder, String url) {
        return Feign.builder()
                .contract(new SpringMvcContract())
                .decoder(null != decoder ? decoder : new SpringDecoder(messageConverters))
                .encoder(null != encoder ? encoder : new SpringEncoder(messageConverters))
                .errorDecoder(errorDecoder)
                .target(classOfT, url);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T constructRemoteServiceInstance(String service, String flag, Class<T> classOfT, Decoder decoder, Encoder encoder, ErrorDecoder errorDecoder) {
        String moduleCacheKey = service + "-" + flag;
        ModuleDefinition moduleDefinition = moduleClientDefinitions.get(moduleCacheKey);
        if (null == moduleDefinition) {
            return null;
        }
        String instanceCacheKey = moduleCacheKey
                + "-" + classOfT.getSimpleName()
                + "-" + (null == encoder ? "" : decoder.getClass().getSimpleName())
                + "-" + (null == encoder ? "" : encoder.getClass().getSimpleName())
                + "-" + (null == errorDecoder ? "" : errorDecoder.getClass().getSimpleName());
        RemoteServiceDefinition<?> remoteServiceDefinition = customRemoteServiceCaches.get(instanceCacheKey);
        if (null != remoteServiceDefinition) {
            logger.info("fetch service from caches,cacheKey:{}", instanceCacheKey);
            return (T) remoteServiceDefinition.getService();
        }
        T remoteService = constructInstance(classOfT, decoder, encoder, errorDecoder, moduleDefinition.getUri());
        if (null == remoteService) {
            return null;
        }
        RemoteServiceDefinition<T> tRemoteServiceDefinition = RemoteServiceDefinition.buildCustomed(service, flag, classOfT, System.currentTimeMillis(), moduleDefinition.getUri(), remoteService);
        customRemoteServiceCaches.put(instanceCacheKey, tRemoteServiceDefinition);
        logger.info("put service to caches,cacheKey:{}", instanceCacheKey);
        return remoteService;
    }

}
