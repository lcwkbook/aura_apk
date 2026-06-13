# ===== 基础配置 =====
-optimizationpasses 5
-allowaccessmodification
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# ===== 移除调试信息 =====
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-dontwarn **

# ===== 增强混淆 =====
-repackageclasses ''
-overloadaggressively
-mergeinterfacesaggressively
-adaptclassstrings
-adaptresourcefilenames
-adaptresourcefilecontents

# ===== 字典混淆 =====
-obfuscationdictionary proguard-dict.txt
-classobfuscationdictionary proguard-dict.txt
-packageobfuscationdictionary proguard-dict.txt

# ===== 保留Android系统入口 =====
-keep public class * extends android.app.Activity
-keepclassmembers class * extends android.app.Activity {
    public void onCreate(android.os.Bundle);
}

# ===== 保留JSON解析类 =====
-keep class org.json.** { *; }
-dontwarn org.json.**

# ===== 保留native方法 =====
-keepclasseswithmembernames class * {
    native <methods>;
}

# ===== 保留控件构造方法 =====
-keepclassmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# ===== 保留枚举 =====
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

