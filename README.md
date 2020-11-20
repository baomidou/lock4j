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

## 如何使用

1. 引入相关依赖。

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>lock4j-spring-boot-starter</artifactId>
    <version>${version}</version>
</dependency>

<!--为支持多种环境redis不会默认引入，虽然初版只支持原生redisTemplate-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

2. 配置原生redis信息。

```yaml
spring:
  redis:
    host: 47.100.20.186
    ...
```

3. 在需要分布式的地方使用Lock4j注解。

```java
@Service
public class DemoService {

    //默认超时3秒，30秒过期
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

## 计划

1. 支持多种限流IP数组限流，基于用户限流。
2. 支持数据库级别，Redission, Zookeeper等多种组件。

## 鸣谢

感谢原作者zzh捐赠项目至苞米豆组织，其是此项目的核心开发者，后续也会主导项目的设计。

本项目参考了 https://gitee.com/kekingcn/spring-boot-klock-starter ，其作者还有很多其他优秀项目。