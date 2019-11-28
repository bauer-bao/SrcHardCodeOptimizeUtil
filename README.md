# SrcHardCodeOptimizeUtil

##介绍
对老项目中的string和color等硬编码快捷的添加到strings.xml和colors.xml文件中.

##效果
![record1.gif](https://github.com/bauer-bao/SrcHardCodeOptimizeUtil/blob/master/screenshoots/record1.gif)

##下载
插件市场搜索"XML Hard-code Optimize"

##常见问题
1.使用前先备份代码
2.支持对res | drawableXXX/layoutXXX | XXX.xml 执行插件
3.如果执行string选项的话，只有android:text和android:hint两个属性可以被识别
4.如果执行color选项的话，类似于#xxx/#xxxxxx/#xxxxxxxx的值都可以被识别
5.默认只判断values文件夹中的strings.xml/colors.xml，如果不存在，会主动创建
6.已过滤databinding的样式
7.colors优化操作已对svg的vector | group | path标签过滤
8.欢迎在issue中提意见、建议甚至问题

##更新日志
V1.0: 新建工程
