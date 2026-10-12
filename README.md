# 获取法定节假日信息

> **⚠️ 服务迁移公告 · 2026-10-12**
>
> **`http://api.haoshenqi.top/holiday` 将于 2026-11-03 下线**，届时该地址不再保证任何响应。
>
> 项目已重写为 **Cloudflare Workers** 版本，代码在 [`cloudflare-workers` 分支](https://github.com/Haoshenqi0123/holiday/tree/cloudflare-workers)：不需要服务器、不需要 MySQL，免费套餐即可长期运行，接口路径与响应格式与本 Java 版保持兼容。
>
> **请 fork 那个分支，按 [DEPLOY.md](https://github.com/Haoshenqi0123/holiday/blob/cloudflare-workers/DEPLOY.md) 部署到你自己账号下的 Worker** —— 全程约十分钟：建一个 KV 命名空间、把它的 ID 填进一个构建变量，代码一行都不用改。
>
> 为什么不再提供公共实例：Workers 与 KV 的免费额度按账号计算，且超额只会返回错误、不会产生费用 —— 公共入口一旦被刷，所有使用者会同时不可用，而作者无法靠付费兜底。各自账号内部署反而互不影响。

本分支（`master`）保留的是 **Java + MySQL 老版本**，随上述公共服务一起进入维护末期。仍想自己跑这套的，按下面的说明操作。

## 使用说明

* 自动更新
* 保存到数据库中，减少对网络依赖
* 只有一个接口，调用简单
* 个人项目，不保证永久维护
* 开源免费

## 直接使用

### 接口地址

        http://api.haoshenqi.top/holiday        # 2026-11-03 下线，请改用自行部署的 Worker

### 参数

        eg:
            http://api.haoshenqi.top/holiday?date=2019-05-01
            http://api.haoshenqi.top/holiday?date=2019-05
            http://api.haoshenqi.top/holiday?date=2019

### 响应

        eg:
            {
                "date": "2019-05-01",
                "year": 2019,
                "month": 5,
                "day": 1,
                "status": 3
            }

        status: 0普通工作日1周末双休日2需要补班的工作日3法定节假日

### 2.0 新功能

            http://api.haoshenqi.top/holiday/today

### 2.0.1 更新
            1. 跟随百度更新，修改自动获取法定节假日方式。
            2. 新增一个借口，可以手动更新次年的法定节假日日期。但是为了避免滥用，此接口（http://api.haoshenqi.top/holiday/update）注释掉了，需要的可以本地自行打开。

### 响应

        eg:
            工作
        -- 今天需要上班
            休息
        -- 今天不用上班

        -- today 可以换成 tomorrow yesterday

## [使用容器(docker-compose)运行](https://github.com/Haoshenqi0123/holiday/wiki/%E4%BD%BF%E7%94%A8%E5%AE%B9%E5%99%A8%E8%BF%90%E8%A1%8C)

## [配合ios快捷操作实现法定节假日闹钟](https://github.com/Haoshenqi0123/holiday/wiki/%E6%94%AF%E6%8C%81IOS%E6%B3%95%E5%AE%9A%E8%8A%82%E5%81%87%E6%97%A5%E9%97%B9%E9%92%9F)

## 二次开发

### 注意事项

```diff 
+ 百度API接口修改了，已经更新了最新的api

+ 修改你的 spring.datasource
+ 需要安装lombok插件 http://plugins.jetbrains.com/plugin/6317-lombok
- 服务器资源有限，访问过于频繁会把ip加入黑名单，一天后自动解除。
``` 

## 打赏
![支付宝](https://github.com/user-attachments/assets/81be686e-427e-4302-8ab7-aac20ad3d153)
![微信](https://github.com/user-attachments/assets/19b1a090-03e6-4390-bc11-21d42ea2a680)
