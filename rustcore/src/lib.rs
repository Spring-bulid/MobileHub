//! mobilehub-core: GitHub API 网络核心 (Rust)
//!
//! 通过 JNI 暴露给 Kotlin 层:
//! - nativeRequest  : 执行 GitHub REST/GraphQL 请求 (rustls, 无需系统证书)
//! - nativeSealToken / nativeOpenToken : 调用 C 保险库加解密 token

use jni::objects::{JByteArray, JClass, JString};
use jni::sys::{jbyteArray, jstring};
use jni::JNIEnv;
use std::io::Read;
use std::time::Duration;

extern "C" {
    fn vault_seal(
        seed: *const u8, seed_len: usize,
        plain: *const u8, plain_len: usize,
        out: *mut u8, out_cap: usize,
    ) -> i32;
    fn vault_open(
        seed: *const u8, seed_len: usize,
        sealed: *const u8, sealed_len: usize,
        out: *mut u8, out_cap: usize,
    ) -> i32;
}

fn jstr(env: &mut JNIEnv, s: &JString) -> String {
    env.get_string(s).map(|v| v.into()).unwrap_or_default()
}

/// 执行 HTTP 请求，返回 JSON: {"status":N,"body":"...","headers":{"link":"...","x-ratelimit-remaining":"..."}}
fn do_request(token: &str, method: &str, url: &str, body: &str, accept: &str) -> String {
    let agent = ureq::AgentBuilder::new()
        .timeout_connect(Duration::from_secs(15))
        .timeout(Duration::from_secs(60))
        .build();

    let accept_header = if accept.is_empty() { "application/vnd.github+json" } else { accept };
    let mut req = agent
        .request(method, url)
        .set("User-Agent", "MobileHub-Android")
        .set("Accept", accept_header)
        .set("X-GitHub-Api-Version", "2022-11-28");
    if !token.is_empty() {
        req = req.set("Authorization", &format!("Bearer {token}"));
    }

    let result = if body.is_empty() {
        req.call()
    } else {
        req.set("Content-Type", "application/json").send_string(body)
    };

    let response = match result {
        Ok(r) => r,
        Err(ureq::Error::Status(_, r)) => r,
        Err(e) => {
            return serde_json::json!({
                "status": 0,
                "body": format!("network error: {e}"),
                "headers": {}
            })
            .to_string();
        }
    };

    let status = response.status();
    let link = response.header("link").unwrap_or("").to_string();
    let remaining = response.header("x-ratelimit-remaining").unwrap_or("").to_string();
    let content_type = response.header("content-type").unwrap_or("").to_string();

    let mut buf: Vec<u8> = Vec::new();
    let mut reader = response.into_reader();
    let body_text = match reader.read_to_end(&mut buf) {
        Ok(_) => String::from_utf8_lossy(&buf).into_owned(),
        Err(e) => format!("read error: {e}"),
    };

    serde_json::json!({
        "status": status,
        "body": body_text,
        "headers": {
            "link": link,
            "x-ratelimit-remaining": remaining,
            "content-type": content_type
        }
    })
    .to_string()
}

#[no_mangle]
pub extern "system" fn Java_com_mobilehub_app_core_NativeCore_nativeRequest(
    mut env: JNIEnv,
    _class: JClass,
    token: JString,
    method: JString,
    url: JString,
    body: JString,
    accept: JString,
) -> jstring {
    let token = jstr(&mut env, &token);
    let method = jstr(&mut env, &method);
    let url = jstr(&mut env, &url);
    let body = jstr(&mut env, &body);
    let accept = jstr(&mut env, &accept);

    let out = do_request(&token, &method, &url, &body, &accept);
    env.new_string(out)
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_com_mobilehub_app_core_NativeCore_nativeSealToken(
    mut env: JNIEnv,
    _class: JClass,
    seed: JString,
    token: JString,
) -> jbyteArray {
    let seed = jstr(&mut env, &seed);
    let token = jstr(&mut env, &token);
    let mut out = vec![0u8; token.len() + 32];
    let n = unsafe {
        vault_seal(
            seed.as_ptr(), seed.len(),
            token.as_ptr(), token.len(),
            out.as_mut_ptr(), out.len(),
        )
    };
    if n < 0 {
        return std::ptr::null_mut();
    }
    out.truncate(n as usize);
    env.byte_array_from_slice(&out)
        .map(|a| a.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_com_mobilehub_app_core_NativeCore_nativeOpenToken(
    mut env: JNIEnv,
    _class: JClass,
    seed: JString,
    sealed: JByteArray,
) -> jstring {
    let seed = jstr(&mut env, &seed);
    let sealed = env.convert_byte_array(&sealed).unwrap_or_default();
    let mut out = vec![0u8; sealed.len() + 16];
    let n = unsafe {
        vault_open(
            seed.as_ptr(), seed.len(),
            sealed.as_ptr(), sealed.len(),
            out.as_mut_ptr(), out.len(),
        )
    };
    if n < 0 {
        return std::ptr::null_mut();
    }
    out.truncate(n as usize);
    let text = String::from_utf8_lossy(&out).into_owned();
    env.new_string(text)
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
}
