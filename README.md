# 项目介绍
isomerism-feign项目是作为feign的增强扩展，用于支持骨干网结构的微服务调用。
# 打包
```shell
mvn clean package
```
# 已知问题
- 骨干网多端不支持ribbon做负载均衡
- 骨干网多端多实例不支持定向调用

# 使用方式
## 依赖
使用项目中，必须存在以下依赖(版本可高于指定版本)
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-openfeign-core</artifactId>
    <version>2.1.0.RELEASE</version>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
    <version>2.1.0.RELEASE</version>
    <optional>true</optional>
</dependency>
<dependency>
    <groupId>com.netflix.hystrix</groupId>
    <artifactId>hystrix-core</artifactId>
    <version>1.5.18</version>
</dependency>
```
## 集成
- 前提：骨干网对应的多端在eureka的注册信息中加入注册两要素
其中 module为对应的模块名称，flag为对应的区分key
```properties
eureka.instance.metadata-map.node.module=user
eureka.instance.metadata-map.node.flag=flag0
```
- 依赖本项目的core包(版本可依赖对应的打包版本)
```xml
<dependency>
    <groupId>com.page</groupId>
    <artifactId>isomerism-feign-core</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```
- 在Spring-Boot项目中将对应的包路径加入扫描
```java
@ComponentScan({"com.page.isomerism.feign"})
```
- 在需要进行调用的位置注入RemoteServiceFeignFactory
```java
@Autowired
RemoteServiceFeignFactory remoteServiceFeignFactory;
```
- 通过注入的remoteServiceFeignFactory构造远端代理实例,并进行调用
```java
UserService userService = remoteServiceFeignFactory.getRemoteServiceInstance("user","flag0",UserService.class);
userService.invokeCustomedMethod();
```

- (可选) 通过在application.properties或对应的配置中增加isomerism.feign.hystrix.enabled=true,将可开启对应的hystrix支持
```java
isomerism.feign.hystrix.enabled=true
```


