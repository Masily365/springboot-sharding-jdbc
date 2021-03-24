# 主从复制、读写分离

## 基本环境

- `java`: `1.8`
- `maven`：`3.6.0`
- `mysql`: `8.0`
- `springboot`:`2.4.4`
- `docker`: `20.10.5`
- `docker-compose`: `1.28.5`
- `sharding-jdbc`: `sharding-jdbc-spring-boot-starter:4.1.1`

## 主从复制

### 环境搭建

- 目录结构
    ```text
    主从复制
     │  docker-compose.yml
     │
     ├─master
     │      Dockerfile
     │      my.cnf
     │
     ├─slave-1
     │      Dockerfile
     │      my.cnf
     │
     └─slave-2
             Dockerfile
             my.cnf
    ```
- `docker-compose.yml`

```yaml
version: '3'
services:

  master:
    build:
      context: ./master
      dockerfile: Dockerfile
    image: mysql-master:1.0
    volumes:
      - "./master/mysql-files:/var/lib/mysql-files/"
    container_name: mysql-master
    links:
      - slave-1
      - slave-2
    ports:
      - "33077:3306"
    restart: always
    hostname: mysql-master
    environment:
      - "MYSQL_ROOT_PASSWORD=123456"

  slave-1:
    build:
      context: ./slave-1
      dockerfile: Dockerfile
    image: mysql-slave-1:1.0
    volumes:
      - "./slave-1/mysql-files:/var/lib/mysql-files/"
    ports:
      - "33070:3306"
    restart: always
    hostname: mysql-slave-1
    container_name: mysql-slave-1
    environment:
      - "MYSQL_ROOT_PASSWORD=123456"

  slave-2:
    build:
      context: ./slave-2
      dockerfile: Dockerfile
    image: mysql-slave-2:1.0
    volumes:
      - "./slave-2/mysql-files:/var/lib/mysql-files/"
    restart: always
    ports:
      - "33071:3306"
    hostname: mysql-slave-2
    container_name: mysql-slave-2
    environment:
      - "MYSQL_ROOT_PASSWORD=123456"
```

- master中DockerFile和my.cnf

DockerFile

```dockerfile
FROM mysql:latest
MAINTAINER masily
ADD ./my.cnf /etc/mysql/my.cnf
#ADD ./master/mysql-files /var/lib/mysql-files/
```

my.cnf

```shell
[mysqld]
## 设置server_id，一般设置为IP，注意要唯一
server-id = 10

## 开启二进制日志功能，可以随便取，最好有含义（关键就是这里了）
log-bin = mysql-bin

## 主从复制的格式（mixed,statement,row，默认格式是statement）
## binlog_format = row
```

- slave-1中DockerFile和my.cnf

DockerFile

```dockerfile
FROM mysql:latest
MAINTAINER masily
ADD ./my.cnf /etc/mysql/my.cnf
#ADD ./slave-1/mysql-files /var/lib/mysql-files/
```

my.cnf

```shell
[mysqld]
## 设置server_id，一般设置为IP，注意要唯一
server-id = 11

## 开启二进制日志功能，可以随便取，最好有含义（关键就是这里了）
log-bin = mysql-bin
```

- slave-2中DockerFile和my.cnf

DockerFile

```dockerfile
FROM mysql:latest
MAINTAINER masily
ADD ./my.cnf /etc/mysql/my.cnf
#ADD ./slave-2/mysql-files /var/lib/mysql-files/
```

my.cnf

```shell
[mysqld]
## 设置server_id，一般设置为IP，注意要唯一
server-id = 12

## 开启二进制日志功能，可以随便取，最好有含义（关键就是这里了）
log-bin = mysql-bin
```

### 环境配置

- 运行 `docker-compose up -d`
- 停止 `docker-compose down`

- `master` 配置

```sql
#
添加用户/权限
CREATE
USER 'repl'@'%' IDENTIFIED WITH 'mysql_native_password' BY '123456';#创建用户

GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';#分配权限

FLUSH PRIVILEGES;#刷新权限

# 查看主库状态
SHOW MASTER STATUS;

SHOW
VARIABLES LIKE '%server_uuid%';
SHOW
VARIABLES LIKE '%server_id%';
```

- `slave` 配置(所有从库都一样)

```sql
SHOW
SLAVE STATUS;
STOP
SLAVE;
CHANGE
MASTER TO MASTER_HOST = 'mysql-master',
MASTER_USER = 'repl',
MASTER_PASSWORD = '123456',
MASTER_LOG_FILE = 'mysql-bin.000003',
MASTER_LOG_POS = 818;
START
SLAVE;

SHOW
VARIABLES LIKE '%server_uuid%';
SHOW
VARIABLES LIKE '%server_id%';
```

## 读写分离环境配置

### maven 依赖

```xml

<dependencies>
    <dependency>
        <groupId>org.apache.shardingsphere</groupId>
        <artifactId>sharding-jdbc-spring-boot-starter</artifactId>
        <version>4.1.1</version>
    </dependency>

    <!-- 没有用druid使用该数据库连接池启动报错，后期排查 -->
    <!--<dependency>-->
    <!--    <groupId>com.alibaba</groupId>-->
    <!--    <artifactId>druid-spring-boot-starter</artifactId>-->
    <!--    <version>1.2.5</version>-->
    <!--</dependency>-->
</dependencies>
```

### yml 配置

```yaml
spring:
  jpa:
    database: mysql
    hibernate:
      ddl-auto: update

  shardingsphere:
    enabled: true
    # 数据源配置
    datasource:
      names: ds0,ds1,ds2
      ds0:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:33077/test?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
        username: root
        password: 123456
      ds1:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:33070/test?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
        username: root
        password: 123456
      ds2:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://localhost:33071/test?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
        username: root
        password: 123456
    # 读写分离
    masterslave:
      name: ds
      # 主库数据源名称
      master-data-source-name: ds0
      # 从库数据源名称列表
      slave-data-source-names:
        - ds1
        - ds2
      # 读写分离负载算法的属性配置
      load-balance-algorithm-type: ROUND_ROBIN
    props:
      sql.show: true
```

> 启动试试
> 
> complete！ 🎉🎉🎉

## 常见问题

1. **主从复制原理**

   [![6bgY6S.png](https://z3.ax1x.com/2021/03/24/6bgY6S.png)](https://imgtu.com/i/6bgY6S)

- 主库更新事件写入到`binlog`中
- 从库发起连接，连接到主库
- 主库创建一个SQL线程（`binlog dump thread`线程），把发送`binlog`内容到从库中；从库创建两个线程
- 从库IO线程：当START SLAVE 执行后，从库创建一个I/O线程，读取主库发送过来的`binlog`内容写入到`relay log`(中继日志)中
- 从库SQL线程：从库创建SQL线程，从`relay log`里读取内容从`Exec_Master_Log_Pos`位置开始执行读取到的更新事件，将更新内容写入到从库中

  [![6bw6yR.png](https://z3.ax1x.com/2021/03/24/6bw6yR.png)](https://imgtu.com/i/6bw6yR)

> 主从同步事件有3种形式:statement、row、mixed。
> 1. statement：会将对数据库操作的sql语句写入到binlog中。
> 2. row：会将每一条数据的变化写入到binlog中。
> 3. mixed：statement与row的混合。Mysql决定什么时候写statement格式的，什么时候写row格式的binlog。

2. **主从复制机制**

- 同步策略：Master会等待所有的Slave都回应后才会提交，这个主从的同步的性能会严重的影响。
- 半同步策略：Master至少会等待一个Slave回应后提交。
- 异步策略：Master不用等待Slave回应就可以提交。
- 延迟策略：Slave要落后于Master指定的时间。

3. **解决主从延迟**

   在使用的`sharding-jdbc`中已有处理机制，更新和查询处于同一事务的话，强行走主库


