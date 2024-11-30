# 获取法定节假日信息

## 使用说明

* 自动更新
* 保存到数据库中，减少对网络依赖
* 只有一个接口，调用简单
* 个人项目，不保证永久维护
* 开源免费

## 直接使用

### 接口地址

            http://api.haoshenqi.top/holiday

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

<img src="https://oss.haoshenqi.top/pay/IMG_2690.JPG" width = "400" height = "400" alt="支付宝" align=center>

<img src="https://oss.haoshenqi.top/pay/IMG_2692.JPG" width = "400" height = "400" alt="微信" align=center>
