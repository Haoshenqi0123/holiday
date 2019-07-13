# 获取法定节假日信息

## 使用说明

* 自动更新
* 保存到数据库中，减少对网络依赖
* 只有一个接口，调用简单
* 个人项目，不保证永久维护
* 免费使用

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

## 二次开发

### 注意事项

* 修改你的 spring.datasource
* 需要安装lombok插件 http://plugins.jetbrains.com/plugin/6317-lombok

## 打赏

![Image text](http://biaoqingbao.haoshenqi.top/img/wechat.jpg)

![Image text](http://biaoqingbao.haoshenqi.top/img/alipay.jpg)
