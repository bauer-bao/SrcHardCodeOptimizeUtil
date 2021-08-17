# SrcHardCodeOptimizeUtil

## 介绍

对老项目中的string、color和dimen等硬编码快捷的添加到strings.xml、colors.xml和dimens.xml文件中.

## 效果

![record1.gif](https://github.com/bauer-bao/SrcHardCodeOptimizeUtil/blob/master/screenshoots/record1.gif)

## 下载

插件市场搜索"XML Hard-Code Optimize"

## 常见问题

1.使用前先备份代码

2.支持对res | drawableXXX/layoutXXX | XXX.xml文件夹或文件执行插件

3.如果执行string选项的话，只有android:text和android:hint两个属性可以被识别，并且注释代码也可被识别

4.如果执行color选项的话，类似于#xxx/#xxxxxx/#xxxxxxxx的值都可以被识别，并且注释代码也可被识别。如果#xxxxxx前后存在空格（比如" #123456 "），则不能识别

5.如果执行dimen选项的话，类似于xxdp/xxsp的值都可以被识别，并且注释代码也可被识别，支持小数（比如1.2dp）。如果dp/sp后面存在空格（比如"30dp "），则不能识别。生成的新数据会自动排序

6.默认只判断values文件夹中的strings.xml/colors.xml/dimens.xml，如果不存在，会主动创建

7.已过滤databinding的样式

8.colors/dimens优化操作已对svg的vector | group | path标签过滤

9.colors.xml中的name以color_xxx来命名，strings.xml中的name以 layout's name_text_index来命名，index在单个文件中会自增长，dimens.xml以dp_x或者text_sp_x来命名

10.没有添加默认快捷键，如需要，可在AS中自行添加

11.欢迎在issue中提意见、建议甚至问题

## 更新日志

V1.0.6:

    1.移除3方库，减少插件体积
    2.支持在编辑区域和文件名tab栏上右击执行插件

V1.0.5:

    1.增加Dimen Optimize选项处理dp和sp
    2.支持对dp和sp新生成的内容进行排序，dp在前sp在后，按照数值从小到大排序
    3.支持dimen/color/string的去重。针对string，因为key是自增长的，所以没法针对value同key不同的情况去重

V1.0.4:

    1.兼容dataBinding中"@={}"的格式

V1.0.3:
    
    1.兼容Android Studio 4.1

V1.0.2:
    
    1.strings操作对注释的优化处理

V1.0.1: 

    1.values文件夹不存在时，会主动创建
    2.string和color操作支持注释

V1.0.0: 

    1.新建工程
