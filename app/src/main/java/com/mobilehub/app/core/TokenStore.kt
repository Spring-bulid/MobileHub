package com.mobilehub.app.core

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.io.File

/**
 * token 持久化：明文永不落盘，统一走 C 保险库 (XTEA-CBC)。
 * 密钥种子 = ANDROID_ID + 包名，换机或换应用后密文不可解。
 */
object TokenStore {

    private const val FILE_NAME = "vault.bin"

    @SuppressLint("HardwareIds")
    private fun seed(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        return androidId + "#" + context.packageName
    }

    private fun vaultFile(context: Context) = File(context.filesDir, FILE_NAME)

    fun save(context: Context, token: String): Boolean {
        val sealed = NativeCore.nativeSealToken(seed(context), token) ?: return false
        return runCatching { vaultFile(context).writeBytes(sealed) }.isSuccess
    }

    fun load(context: Context): String? {
        val file = vaultFile(context)
        if (!file.exists()) return null
        val sealed = runCatching { file.readBytes() }.getOrNull() ?: return null
        val token = NativeCore.nativeOpenToken(seed(context), sealed)
        return token?.takeIf { it.isNotBlank() }
    }

    fun clear(context: Context) {
        vaultFile(context).delete()
    }
}
