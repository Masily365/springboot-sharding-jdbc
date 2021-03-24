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
# 添加用户/权限
CREATE USER 'repl'@'%' IDENTIFIED WITH 'mysql_native_password' BY '123456';#创建用户

GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';#分配权限

FLUSH PRIVILEGES;#刷新权限

# 查看主库状态
SHOW MASTER STATUS;

SHOW VARIABLES LIKE '%server_uuid%';
SHOW VARIABLES LIKE '%server_id%';
```

- `slave` 配置(所有从库都一样)

```sql
SHOW SLAVE STATUS;
STOP SLAVE;
CHANGE MASTER TO MASTER_HOST = 'mysql-master',
MASTER_USER = 'repl',
MASTER_PASSWORD = '123456',
MASTER_LOG_FILE = 'mysql-bin.000003',
MASTER_LOG_POS = 818;
START SLAVE;

SHOW VARIABLES LIKE '%server_uuid%';
SHOW VARIABLES LIKE '%server_id%';
```


## 读写分离环境配置

## 常见问题

1. 主从复制原理
   
2. 主从复制机制
   
3. 解决主从延迟


