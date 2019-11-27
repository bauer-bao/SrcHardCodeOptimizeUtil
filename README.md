# SrcHardCodeOptimizeUtil
src hard code optimize


func: [ok]1.支持string 只考虑text 和 hint-----只支持layout目录
            [ok]2.支持color 只适配  "#  并且长度为6或者8位。或者可以考虑正则匹配下     drawable & layout文件夹
            3.对string/color支持添加自定义属性
            [ok]4.支持对文件夹或者文件操作
            [ok]5.color 需要可以对res  drawable/layout  单文件执行操作

        bug:
            [ok]1.目标文件夹不存在，主动创建
            [ok]2.已存在的，是否可以去重
            [ok]3.验证码将发送到123****1232，没法处理成功
            [ok]4.@{questionItem.name} databinding需要过滤
            [ok]5.没有进度显示（显示的）
            [ok]6.weight  +1  替换错乱的问题    因为weight为1，但是全部替换的时候，也是为1，因此，全局替换掉了
            [ok]7.空格需要过滤
            [ok]8.如果目标value在注释代码中，则有问题，需要判断<!-- -->这种形式
            [ok]9.color svg过滤   <vector   <group  <path
            [ok]10.重复文件没有处理
