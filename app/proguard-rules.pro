# 保留 Activity 入口
-keep class com.aa.Aurakernel.MainActivity { *; }

# 保留 JSON 解析
-keep class org.json.** { *; }

# 保留内部类
-keep class com.aa.Aurakernel.MainActivity$* { *; }

# 保留所有 public 方法（防止反射问题）
-keepclassmembers class * {
    public *;
}

# 混淆字符串常量（推荐）
-adaptclassstrings
