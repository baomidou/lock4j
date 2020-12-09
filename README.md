<p align="center">

<img src="https://s1.ax1x.com/2018/07/29/Pacq2Q.png" border="0" />

</p>

<p align="center">
	<strong>一种简单的，支持不同方案的高性能分布式锁</strong>
</p>

<p align="center">
    <a href="http://mvnrepository.com/artifact/com.baomidou/lock4j-spring-boot-starter" target="_blank">
        <img src="https://maven-badges.herokuapp.com/maven-central/com.baomidou/lock4j-spring-boot-starter/badge.svg" >
    </a>
    <a href="http://www.apache.org/licenses/LICENSE-2.0.html" target="_blank">
        <img src="http://img.shields.io/:license-apache-brightgreen.svg" >
    </a>
    <a>
        <img src="https://img.shields.io/badge/JDK-1.8+-green.svg" >
    </a>
    <a>
        <img src="https://img.shields.io/badge/springBoot-2.0+-green.svg" >
    </a>
</p>
<p align="center">
	QQ群：336752559
</p>

## 简介

lock4j-spring-boot-starter是一个分布式锁组件，其提供了多种不同的支持以满足不同性能和环境的需求。

立志打造一个简单但富有内涵的分布式锁组件。

## 特性

1. 简单易用，功能强大，扩展性强。
2. 支持redission,redisTemplate,zookeeper。可混用，支持扩展。

## 如何使用

1. 引入相关依赖。

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>lock4j-spring-boot-starter</artifactId>
    <version>${version}</version>
</dependency>

<!--若使用redisTemplate作为分布式锁底层，则需要引入-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
 <!--若使用redisson作为分布式锁底层，则需要引入-->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.13.6</version>
</dependency>
<!--若使用zookeeper作为分布式锁底层，则需要引入-->
<dependency>
    <groupId>org.apache.curator</groupId>
    <artifactId>curator-recipes</artifactId>
    <version>5.1.0</version>
</dependency>
```

2. 根据底层需要配置redis或zookeeper。

```yaml
spring:
  redis:
    host: 127.0.0.1
    ...
  coordinate:
    zookeeper:
      zkServers: 127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183
```

3. 在需要分布式的地方使用Lock4j注解。

```java
@Service
public class DemoService {

    //默认获取锁超时3秒，30秒锁过期
    @Lock4j
    public void simple() {
    	//do something
    }
    
	//完全配置，支持spel
    @Lock4j(keys = {"#user.id", "#user.name"}, expire = 60000, acquireTimeout = 1000)
    public User customMethod(User user) {
        return user;
    }

}
```

## 高级使用

1. 配置全局默认的获取锁超时时间和锁过期时间。

```yaml
lock4j:
  acquire-timeout: 3000 #默认值3s，可不设置
  expire: 30000 #默认值30s，可不设置
  primary-executor: com.baomidou.lock.executor.RedisTemplateLockExecutor #默认redisson>redisTemplate>zookeeper，可不设置
```

2. 自定义执行器。

```java
@Service
public class DemoService {
    
	//可在方法级指定使用某种执行器，若自己实现的需要提前注入到Spring。
    @Lock4j(executor = RedissonLockExecutor.class)
    public Boolean test() {
        return "true";
    }
}
```

3. 自定义锁key生成器。

默认的锁key生成器为 `com.baomidou.lock.DefaultLockKeyBuilder` 。

```java
@Component
public class MyLockKeyBuilder extends DefaultLockKeyBuilder {

    @Override
    protected String getKeyPrefix() {
        return "myKey"; //默认是lock4j开头
    }
}
```

4. 自定义锁获取失败策略。

默认的锁获取失败策略为 `com.baomidou.lock.DefaultLockFailureStrategy` 。

```java
@Component
public class MyLockFailureStrategy implements LockFailureStrategy {

    @Override
    public void onLockFailure(String key, long acquireTimeout, int acquireCount) {
        // write my code
    }
}
```


