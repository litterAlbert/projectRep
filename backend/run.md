# 智慧图书管理系统 - 后端服务

## 环境要求
- JDK 17
- Maven 3.6+
- MySQL 8.0
- Redis 6.0+ (需支持 RedisSearch 模块，用于向量存储)

## 快速启动

1. **初始化数据库**
   - 登录 MySQL，执行 `db/init.sql` 脚本，创建 `smart_library` 数据库及表结构，并插入测试数据。

2. **配置环境变量**
   在启动项目前，请在 `src/main/resources/application.yml` 中或通过系统环境变量配置以下关键参数：
   - `MYSQL_PASSWORD`: MySQL `root` 用户的密码（默认 `root`）
   - `REDIS_PASSWORD`: Redis 的密码（如有）
   - `DASHSCOPE_API_KEY`: 阿里云百炼大模型 API Key（用于 AI 助手）
   - `OSS_ENDPOINT`: 阿里云 OSS Endpoint
   - `OSS_ACCESS_KEY_ID`: 阿里云 OSS AccessKey ID
   - `OSS_ACCESS_KEY_SECRET`: 阿里云 OSS AccessKey Secret
   - `OSS_BUCKET_NAME`: 阿里云 OSS Bucket 名称

3. **编译并运行**
   在项目根目录下执行：
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```
   项目默认运行在 `8080` 端口。

## 测试与自检
- 项目包含了基本的单元测试，可通过 `mvn test` 运行验证。
- 可以使用 Postman 或 curl 等工具调用接口进行测试，首先调用 `/user/login` 获取 token，后续请求在 Header 中添加 `Authorization: Bearer <token>` 即可。
