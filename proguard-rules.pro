-injars  build/libs/InfernalMobsReloaded.jar
-outjars build/libs/InfernalMobsReloaded-min.jar

-libraryjars "C:\Program Files\Java\jdk1.8.0_291\jre\lib\rt.jar"
-libraryjars "D:\Local Disk\Users\User\Documents\GitHub\InfernalMobsReloaded\libs\paper-1.16.5-707.jar"

-dontwarn net.minecraft.**
-dontwarn org.bukkit.**
-dontwarn com.google.**
-dontwarn com.comphenix.**
-dontwarn android.**
-dontwarn org.hibernate.**
-dontwarn com.sk89q.worldedit**
-dontwarn com.sk89q.worldguard**
-dontwarn net.milkbowl.vault.economy**
-keep class com.github.secretx33.dependencies.infernalmobsreloaded.hikari.metrics.**
-dontwarn com.codahale.metrics.**
-keep class com.codahale.metrics.**
-dontwarn **hikari.metrics**
-dontwarn javax.crypto.**
-dontwarn javassist.**
-dontwarn **slf4j**
-dontwarn io.micrometer.core.instrument.MeterRegistry
-dontwarn org.codehaus.mojo.**
-dontwarn **prometheus**
-dontwarn **configurate.**
-dontwarn **koin.core.time.**
-dontwarn net.Indyuce.**
-dontwarn **xseries.**
-keepnames class com.github.secretx33.dependencies.infernalmobsreloaded.kotlin.coroutines.** { *; }
-dontwarn **kotlinx.coroutines.**
-dontwarn **org.apache.commons.codec**
-dontwarn com.palmergames.bukkit.towny**
-keepnames class com.github.secretx33.dependencies.infernalmobsreloaded.mfmsg**

#-dontshrink
#-dontobfuscate
#-dontoptimize

# Keep your main class
-keep,allowobfuscation,allowoptimization class * extends org.bukkit.plugin.java.JavaPlugin { *; }

# Keep event handlers
-keep,allowobfuscation,allowoptimization class * extends org.bukkit.event.Listener {
    @org.bukkit.event.EventHandler <methods>;
}

# Keep main package name (spigot forum rule)
-keeppackagenames "com.github.secretx33.infernalmobsreloaded"

# Keep public enum names
-keepclassmembers public enum com.github.secretx33.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep all ProtocolLib packet listeners (this was rough to get working, don't turn on optimization, it ALWAYS breaks the sensible ProtocolLib)
-keepclassmembers class com.github.secretx33.infernalmobsreloaded.**  {
    void onPacketSending(com.comphenix.protocol.events.PacketEvent);
    void onPacketReceiving(com.comphenix.protocol.events.PacketEvent);
}

# Keep static fields in custom Events
-keepclassmembers,allowoptimization class com.github.secretx33.infernalmobsreloaded.** extends org.bukkit.event.Event {
    @com.github.secretx33.dependencies.kotlin.jvm.JvmStatic <fields>;
    public static final <fields>;
    @com.github.secretx33.dependencies.kotlin.jvm.JvmStatic <methods>;
    public static <methods>;
}

# Remove dependencies obsfuscation to remove bugs factor
#-keep,allowshrinking class com.github.secretx33.dependencies.** { *; }

# If your goal is obfuscating and making things harder to read, repackage your classes with this rule
-repackageclasses com.github.secretx33.infernalmobsreloaded
-allowaccessmodification
-mergeinterfacesaggressively
-adaptresourcefilecontents **.yml,META-INF/MANIFEST.MF

# Some attributes that you'll need to keep (if I remove *Annotation* Koin dies)
-keepattributes Exceptions,Signature,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod
#-keepattributes Exceptions,Signature,Deprecated,LineNumberTable,*Annotation*,EnclosingMethod
#-keepattributes LocalVariableTable,LocalVariableTypeTable,Exceptions,InnerClasses,Signature,Deprecated,LineNumberTable,*Annotation*,EnclosingMethod
