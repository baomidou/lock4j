# v2.2.1

- update: 获取锁失败处理抽出至LockInterceptor
- feat: @Lock4j 新增name字段 支持全局锁、资源锁、方法锁，通过name字段控制锁范围
- update: 自定义key前缀移动至Lock4jProperties配置项，默认配置为lock4j
- update: 获取锁失败处理策略传递了Method和Arguments，方便根据方法参数定义特殊错误提示
- fix: 使用其他aop组件时,aop切了两次. https://gitee.com/baomidou/lock4j/issues/I3TAX2

# v2.2.0

- refactor: 项目结构重大调整
- fix: ZookeeperLockExecutor acquireTimeout时间单位错误
- update: 支持yml配置文件使用 - 替代驼峰字段识别 example: zkServers、zk-servers都可以识别
- update: Executor实现代码优化

# v2.1.0

- feat: 新增Lock4jProperties支持全局配置锁过期、获取锁超时时间、默认锁执行器等
- update: 修改Lock4j注解里的lockClient参数名为executor
- update: 新增锁执行器支持自定义扩展,支持方法级别配置executor
- update: 修改锁执行器为单例
- remove: 移除Lock4j注解里的lockType参数
- remove: 移除Lock4j注解里的keyBuilder参数(扩展keyBuilder需要自行实现LockKeyBuilder接口并声明为spring bean)
- fix: 修复lockExecutor没有引入相关class的情况下抛class no found
- feat: 新增不指定executor前提下,默认优先级redisson>redisTemplate>zookeeper
- remove: 移除Lock4j注解里的LockFailureStrategy参数(自定义lock失败处理需要自行实现LockFailureStrategy接口并声明为spring bean)
- feat: 支持声明式@Lock4j、编程式@Autowired LockTemplate

# v2.0.1

- feat: 添加copyright
- feat: 切面优先级设置为最高

# v2.0.0

- feat: 支持redission 和zookeeper

# v1.0.0

- feat: 初始化支持redisTemplate