# 数据库外部配置说明

## 多数据源在系统中的切换方式

当前工程通过“为不同业务模块分别注入 `SqlSessionTemplate`”的方式来切换数据库：

1. `RoadSafetyDataSourceConfig` 会在配置文件存在 `spring.datasource.road-safety.url` 时生效（`@ConditionalOnProperty`）。它手动声明了路产安全库的 `DataSource`、`SqlSessionFactory`、`SqlSessionTemplate` 与 `DataSourceTransactionManager`。这样一来，该模板绑定在独立的连接池 `roadSafetyDataSource` 上。 【F:xrcgs-infrastructure/src/main/java/com/xrcgs/infrastructure/config/RoadSafetyDataSourceConfig.java†L31-L89】
2. 路产安全模块在检测到独立模板存在时，会通过 `RoadSafetyMapperConfiguration` 把 Mapper 绑定到 `roadSafetySqlSessionTemplate` 上，从而连到路产安全库。 【F:xrcgs-module-road-safety/src/main/java/com/xrcgs/roadsafety/config/RoadSafetyMapperConfiguration.java†L10-L18】
3. 如果未提供独立模板，`RoadSafetyMapperFallbackConfiguration` 会让同一批 Mapper 回退到 Spring Boot 默认的主库模板，因此它们会落到 `xrcgs_admin` 数据库。 【F:xrcgs-module-road-safety/src/main/java/com/xrcgs/roadsafety/config/RoadSafetyMapperFallbackConfiguration.java†L10-L17】
4. 主工程的 `@MapperScan` 范围排除了路产安全包，只覆盖 IAM、日志、文件、认证等模块，并且显式绑定到默认的 `sqlSessionFactory`，从而始终走主库连接。 【F:xrcgs-boot/src/main/java/com/xrcgs/boot/XrcgsAdminApplication.java†L10-L21】
5. 若 MyBatis-Plus 未能自动装配名为 `sqlSessionFactory` 的 Bean，`PrimaryMybatisFallbackConfig` 会以主数据源参数补齐一份兜底配置，确保上述扫描配置可以顺利注入并连到 `xrcgs_admin`。 【F:xrcgs-infrastructure/src/main/java/com/xrcgs/infrastructure/config/PrimaryMybatisFallbackConfig.java†L1-L86】
6. 多数据源的连接串、账号等配置在 `application-*.yml` 中按模块拆分：`spring.datasource.*` 对应主库，`spring.datasource.road-safety.*` 则供路产安全库使用。通过属性占位符 `${DB_ROAD_SAFETY_USER:${DB_USER:root}}` 等方式，支持按需覆盖或回退默认值。 【F:xrcgs-boot/src/main/resources/application-dev.yml†L1-L21】【F:xrcgs-boot/src/main/resources/application-prod.yml†L1-L21】

### 让路产安全模块回退到主库 `xrcgs_admin`

只要不在配置文件或外部参数中提供 `spring.datasource.road-safety.url`，`RoadSafetyDataSourceConfig` 就不会创建独立的连接池，`RoadSafetyMapperFallbackConfiguration` 便会生效。此时：

1. 路产安全模块与其他模块一样共用默认数据源，所有读写均指向 `xrcgs_admin`。
2. 不需要改动 Mapper 代码，因扫描配置会自动切换到默认模板。
3. 如需重新启用独立库，只要恢复 `spring.datasource.road-safety.*` 配置即可。

借助这种“按模块绑定模板”的方式，无需在代码中手动判断或切换，只要 Mapper 属于路产安全包，就会自动落到对应的数据源；其他模块则继续使用主库。

## 外部配置的解析顺序

Spring Boot 的配置属性支持从多个外部来源加载。`application-dev.yml` 和 `application-prod.yml` 中的数据源用户名写法为 `username: ${DB_USER:root}`，表示：

1. **首先读取环境变量或 JVM 系统属性**：如果在运行应用时设置了 `DB_USER`（或 `DB_ROAD_SAFETY_USER`），Spring Boot 会直接使用这些变量的值。
2. **可以通过启动参数覆盖**：例如使用 `java -jar app.jar --DB_USER=alice` 或者在 `mvn spring-boot:run -Dspring-boot.run.arguments="--DB_USER=alice"` 中传参。
3. **也可以在外部配置文件中定义**：在同一目录放置 `application.yml`、`application-dev.yml` 等文件，或者创建 `application-local.yml` 并在启动时通过 `--spring.profiles.active=local` 指定。属性占位符会从激活的配置文件中解析。
4. **若以上都未提供，则回退到冒号后的默认值**：示例中默认是 `root`。

部署到 Linux / Docker 环境时，通常会在进程启动前导出环境变量，例如：

```bash
export DB_USER=prod_user
export DB_PASSWORD=prod_password
export DB_ROAD_SAFETY_USER=road_user
export DB_ROAD_SAFETY_PASSWORD=road_password
java -jar xrcgs-boot.jar --spring.profiles.active=prod
```

在 IDE 或本地开发环境下，也可以在运行配置中添加环境变量或通过 `application-dev.yml` 覆盖默认值。这些方式都是“外部配置”的来源。
