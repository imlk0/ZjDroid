# ZjDroid
凑齐了ZjDroid的源码，并在原版的基础上进行了一些改进。

# 有什么不同？
1. 通过源码可以编译出多个平台的so文件，使得在Android x86虚拟机上脱壳成为可能。
2. 修改源码，解决了原来的版本**拖出来之后拿到的是壳子的问题**（原版原理上其实是没有问题的，就是一个小细节没有处理好）。\
详细原因请看我的文章：\
[https://blog.csdn.net/u010746456/article/details/80150250](https://blog.csdn.net/u010746456/article/details/80150250)
3. **增加dexinfo时显示mCookie**。
4. dump时使用mCookie作为参数。

# 局限
1. **只能在dalvik虚拟机上使用（Android4.4及以下）**

# 用法
**除了将索引dex文件的方式改为用mCookie,在用法上基本和原版无差别**

1.获取APK当前加载DEX文件信息：
```
adb shell am broadcast -a com.zjdroid.invoke --ei <target-pid> --es cmd '{"action":"dump_dexinfo"}'
```
2.获取指定mCookie对应的DEX文件包含的可加载类名：
```
adb shell am broadcast -a com.zjdroid.invoke --ei <target-pid> --es cmd '{"action":"dump_class","mCookie":"*****"}'
```
3.根据Dalvik相关内存指针动态反编译指定DEX，并以文件形式保存。
```
adb shell am broadcast -a com.zjdroid.invoke --ei <target-pid> --es cmd '{"action":"backsmali","mCookie":"*****"}'
```
4.Dump指定DEX内存中的数据并保存到文件（数据为odex格式，可在pc上反编译）。
```
adb shell am broadcast -a com.zjdroid.invoke --ei <target-pid> --es cmd '{"action":"dump_dex","mCookie":"*****"}'
```
5.Dump指定内存空间区域数据到文件。
```
adb shell am broadcast -a com.zjdroid.invoke --ei <target-pid> --es cmd '{"action":"dump_mem","start":1234567,"length":123}'
```
6.Dump Dalvik堆栈信息到文件，文件可以通过java heap分析工具分析处理。
```
adb shell am broadcast -a com.zjdroid.invoke --ei <target-pid> --es cmd '{"action":"dump_heap"}'
```
7.运行时动态调用Lua脚本 该功能可以通过Lua脚本动态调用java代码。 使用场景： 可以动态调用解密函数，完成解密。 可以动态触发特定逻辑。
luajava相关使用方法： http://www.keplerproject.org/luajava/
```
adb shell am broadcast -a com.zjdroid.invoke --ei <target-pid> --es cmd '{"action":"invoke","filepath":"****"}'
```
8.敏感API调用自动监控

# 执行结果查看：

1.命令执行结果： 
```
adb shell logcat -s zjdroid-shell-{package name}
```
2.敏感API调用监控输出结果： 
```
adb shell logcat -s zjdroid-apimonitor-{package name}
```

# 相关开源项目
halfkiss/ZjDroid：\
[https://github.com/halfkiss/ZjDroid](https://github.com/halfkiss/ZjDroid)\
mikusjelly/HeyGirl:\
[https://github.com/mikusjelly/HeyGirl](https://github.com/mikusjelly/HeyGirl)\
mkottman/AndroLua:\
[https://github.com/mkottman/AndroLua](https://github.com/mkottman/AndroLua)

# LICENSE
Apache License, Version 2.0
