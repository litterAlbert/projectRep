- 本项目使用 Java 17。
- 遵循标准的 Java 命名规范：类名用 PascalCase，方法和变量用 camelCase，常量全大写加下划线。
- 所有 public 方法必须包含完整的 Javadoc 注释，包括 @param、@return 和 @throws。
- 避免使用 Lombok；显式编写 getter/setter 和构造函数。
- 异常处理应具体，避免捕获通用 Exception，优先抛出或记录有意义的异常。
- 使用 logback 进行日志记录，而不是 System.out。
- 每个业务类都应有对应的测试类。

- 包结构应基本上遵循下面结构，可根据需要增加包。
  org.example.backend
  ├── controller
  ├── service
  ├── pojo
  ├── mapper
  ├── tools
  └── config
- Controller 层只负责 HTTP 映射和参数校验，不包含业务逻辑。
- 全局异常处理通过 @ControllerAdvice 实现。
- 构建工具为 Maven，maven配置本地已经配好了。

