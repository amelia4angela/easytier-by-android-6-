package com.easytier.jni

/** EasyTier JNI 接口类 */
object EasyTierJNI {
    init {
        System.loadLibrary("easytier_android_jni")
    }

    /** 设置 TUN 文件描述符 */
    @JvmStatic external fun setTunFd(instanceName: String, fd: Int): Int

    /** 解析配置字符串 (TOML) */
    @JvmStatic external fun parseConfig(config: String): Int

    /** 运行网络实例 */
    @JvmStatic external fun runNetworkInstance(config: String): Int

    /** 保留指定实例，停止其他 */
    @JvmStatic external fun retainNetworkInstance(instanceNames: Array<String>?): Int

    /** 收集网络信息 JSON */
    @JvmStatic external fun collectNetworkInfos(maxLength: Int): String?

    @JvmStatic external fun getLastError(): String?

    // ── Convenience wrappers (official EasyTierManager pattern) ──

    /** 停止所有网络实例 */
    @JvmStatic
    fun stopAllInstances(): Int {
        return retainNetworkInstance(null)
    }

    /** 只保留一个实例（停止其他所有） */
    @JvmStatic
    fun retainSingleInstance(instanceName: String): Int {
        return retainNetworkInstance(arrayOf(instanceName))
    }
}
