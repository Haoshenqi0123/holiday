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
        
## [配合ios快捷操作实现法定节假日闹钟]

(https://github.com/Haoshenqi0123/holiday/wiki/%E6%94%AF%E6%8C%81IOS%E6%B3%95%E5%AE%9A%E8%8A%82%E5%81%87%E6%97%A5%E9%97%B9%E9%92%9F)

## 二次开发

### 注意事项

* 修改你的 spring.datasource
* 需要安装lombok插件 http://plugins.jetbrains.com/plugin/6317-lombok

## 打赏

<img src="https://blog.babyrabbit.cn/upload/2020/05/3b5onscktghpdqtsdp1ih82ij7.jpg" width = "400" height = "400" alt="图片名称" align=center>

<img src="https://blog.babyrabbit.cn/upload/2020/05/41cguoniu4gi4okrt4j57cvkk8.jpg" width = "400" height = "400" alt="图片名称" align=center>
