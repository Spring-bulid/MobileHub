fn main() {
    println!("cargo:rerun-if-changed=src/vault.c");
    cc::Build::new().file("src/vault.c").compile("vault");
}
