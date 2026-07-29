package com.mobilehub.app.core

/**
 * JNI 桥接：对应 rustcore (libmobilehub_core.so)
 * 网络请求由 Rust (ureq + rustls) 执行，token 加解密由 C 保险库执行。
 */
object NativeCore {
    init {
        System.loadLibrary("mobilehub_core")
    }

    /** 返回 JSON: {"status":Int,"body":String,"headers":{"link":...,"x-ratelimit-remaining":...}} */
    external fun nativeRequest(token: String, method: String, url: String, body: String, accept: String): String?

    external fun nativeSealToken(seed: String, token: String): ByteArray?

    external fun nativeOpenToken(seed: String, sealed: ByteArray): String?
}
