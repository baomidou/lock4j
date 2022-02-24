<p align="center">

<img src="https://s1.ax1x.com/2018/07/29/Pacq2Q.png" border="0" />

</p>

<p align="center">
	<strong>一种简单的，支持不同方案的高性能分布式锁</strong>
</p>

<p align="center">
    <a href="http://mvnrepository.com/artifact/com.baomidou/lock4j" target="_blank">
        <img src="https://maven-badges.herokuapp.com/maven-central/com.baomidou/lock4j/badge.svg" >
    </a>
    <a href="http://www.apache.org/licenses/LICENSE-2.0.html" target="_blank">
        <img src="http://img.shields.io/:license-apache-brightgreen.svg" >
    </a>
    <a href="https://search.maven.org/search?q=insp" target="_blank">
        <img src="https://img.shields.io/maven-central/v/com.baomidou/lock4j" />
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

lock4j是一个分布式锁组件，其提供了多种不同的支持以满足不同性能和环境的需求。

立志打造一个简单但富有内涵的分布式锁组件。

## 特性

1. 简单易用，功能强大，扩展性强。
2. 支持redission,redisTemplate,zookeeper。可混用，支持扩展。

## 如何使用

1. 引入相关依赖(支持同时存在,不同方法不同锁实现)。

```xml

<dependencies>
    <!--若使用redisTemplate作为分布式锁底层，则需要引入-->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>lock4j-redis-template-spring-boot-starter</artifactId>
        <version>${latest.version}</version>
    </dependency>
    <!--若使用redisson作为分布式锁底层，则需要引入-->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>lock4j-redisson-spring-boot-starter</artifactId>
        <version>${latest.version}</version>
    </dependency>
    <!--若使用zookeeper作为分布式锁底层，则需要引入-->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>lock4j-zookeeper-spring-boot-starter</artifactId>
        <version>${latest.version}</version>
    </dependency>
</dependencies>

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
  lock-key-prefix: lock4j #锁key前缀, 默认值lock4j，可不设置
```

acquire-timeout 可以理解为排队时长，超过这个时才就退出排队，抛出获取锁超时异常。

为什么必须要有这个参数？现实你会一直排队等下去吗？所有人都一直排队有没有问题 ？

expire 锁过期时间 。 主要是防止死锁。 建议估计好你锁方法运行时常，正常没有复杂业务的增删改查最多几秒，留有一定冗余，10秒足够。 我们默认30秒是为了兼容绝大部分场景。

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
	public String buildKey(MethodInvocation invocation, String[] definitionKeys) {
		String key = super.buildKey(invocation, definitionKeys);
        // do something
		return key;
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

5. 手动上锁解锁。

```java

@Service
public class ProgrammaticService {
    @Autowired
    private LockTemplate lockTemplate;

    public void programmaticLock(String userId) {
        // 各种查询操作 不上锁
        // ...
        // 获取锁
        final LockInfo lockInfo = lockTemplate.lock(userId, 30000L, 5000L, RedissonLockExecutor.class);
        if (null == lockInfo) {
            throw new RuntimeException("业务处理中,请稍后再试");
        }
        // 获取锁成功，处理业务
        try {
            System.out.println("执行简单方法1 , 当前线程:" + Thread.currentThread().getName() + " , counter：" + (counter++));
        } finally {
            //释放锁
            lockTemplate.releaseLock(lockInfo);
        }
        //结束
    }
}
```


