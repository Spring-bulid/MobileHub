/*
 * vault.c - token 保险库（C 实现）
 *
 * 使用 XTEA 分组密码 (CBC 模式) 对 GitHub token 做本地加密存储。
 * 密钥由调用方传入的设备指纹派生 (FNV-1a 展开为 128bit)。
 */
#include <stdint.h>
#include <stddef.h>
#include <string.h>
#include <stdlib.h>

#define XTEA_ROUNDS 64
#define BLOCK 8

static void xtea_encrypt_block(uint32_t v[2], const uint32_t key[4]) {
    uint32_t v0 = v[0], v1 = v[1], sum = 0, delta = 0x9E3779B9;
    for (int i = 0; i < XTEA_ROUNDS; i++) {
        v0 += (((v1 << 4) ^ (v1 >> 5)) + v1) ^ (sum + key[sum & 3]);
        sum += delta;
        v1 += (((v0 << 4) ^ (v0 >> 5)) + v0) ^ (sum + key[(sum >> 11) & 3]);
    }
    v[0] = v0; v[1] = v1;
}

static void xtea_decrypt_block(uint32_t v[2], const uint32_t key[4]) {
    uint32_t v0 = v[0], v1 = v[1], delta = 0x9E3779B9, sum = delta * XTEA_ROUNDS;
    for (int i = 0; i < XTEA_ROUNDS; i++) {
        v1 -= (((v0 << 4) ^ (v0 >> 5)) + v0) ^ (sum + key[(sum >> 11) & 3]);
        sum -= delta;
        v0 -= (((v1 << 4) ^ (v1 >> 5)) + v1) ^ (sum + key[sum & 3]);
    }
    v[0] = v0; v[1] = v1;
}

/* FNV-1a 哈希，将任意长度种子扩展为 128bit 密钥 */
static void derive_key(const uint8_t *seed, size_t len, uint32_t key[4]) {
    uint64_t h = 0xCBF29CE484222325ULL;
    for (size_t i = 0; i < len; i++) {
        h ^= seed[i];
        h *= 0x100000001B3ULL;
    }
    for (int k = 0; k < 4; k++) {
        h ^= (uint64_t)(k + 1) * 0x9E3779B97F4A7C15ULL;
        h *= 0x100000001B3ULL;
        h ^= h >> 29;
        key[k] = (uint32_t)(h ^ (h >> 32));
    }
}

/*
 * 加密: 输入明文, 输出缓冲区由调用方提供 (容量 >= ((len/8)+2)*8)。
 * 格式: [8字节原始长度][密文...]，返回输出总字节数。
 */
int vault_seal(const uint8_t *seed, size_t seed_len,
               const uint8_t *plain, size_t plain_len,
               uint8_t *out, size_t out_cap) {
    uint32_t key[4];
    derive_key(seed, seed_len, key);

    size_t padded = ((plain_len + BLOCK - 1) / BLOCK) * BLOCK;
    size_t total = BLOCK + padded;
    if (out_cap < total) return -1;

    memset(out, 0, total);
    uint64_t n = (uint64_t)plain_len;
    memcpy(out, &n, sizeof(n));
    memcpy(out + BLOCK, plain, plain_len);

    /* CBC: IV 取自长度块加密结果 */
    uint32_t prev[2];
    memcpy(prev, out, BLOCK);
    xtea_encrypt_block(prev, key);
    memcpy(out, prev, BLOCK);

    for (size_t off = BLOCK; off < total; off += BLOCK) {
        uint32_t blk[2];
        memcpy(blk, out + off, BLOCK);
        blk[0] ^= prev[0];
        blk[1] ^= prev[1];
        xtea_encrypt_block(blk, key);
        memcpy(out + off, blk, BLOCK);
        prev[0] = blk[0]; prev[1] = blk[1];
    }
    return (int)total;
}

/* 解密: 返回明文长度, 失败返回 -1 */
int vault_open(const uint8_t *seed, size_t seed_len,
               const uint8_t *sealed, size_t sealed_len,
               uint8_t *out, size_t out_cap) {
    if (sealed_len < BLOCK * 2 || sealed_len % BLOCK != 0) return -1;
    uint32_t key[4];
    derive_key(seed, seed_len, key);

    uint32_t head[2];
    memcpy(head, sealed, BLOCK);
    uint32_t iv[2] = { head[0], head[1] };
    xtea_decrypt_block(head, key);
    uint64_t plain_len;
    memcpy(&plain_len, head, sizeof(plain_len));
    if (plain_len > sealed_len - BLOCK || plain_len > out_cap) return -1;

    uint32_t prev[2] = { iv[0], iv[1] };
    uint8_t tmp[BLOCK];
    size_t written = 0;
    for (size_t off = BLOCK; off < sealed_len; off += BLOCK) {
        uint32_t blk[2], cipher[2];
        memcpy(blk, sealed + off, BLOCK);
        cipher[0] = blk[0]; cipher[1] = blk[1];
        xtea_decrypt_block(blk, key);
        blk[0] ^= prev[0];
        blk[1] ^= prev[1];
        memcpy(tmp, blk, BLOCK);
        size_t take = plain_len - written < BLOCK ? plain_len - written : BLOCK;
        memcpy(out + written, tmp, take);
        written += take;
        prev[0] = cipher[0]; prev[1] = cipher[1];
        if (written >= plain_len) break;
    }
    return (int)plain_len;
}
