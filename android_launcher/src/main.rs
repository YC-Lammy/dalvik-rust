#[macro_use] 
extern crate nom;
extern crate leb128;
extern crate byteorder;
extern crate failure;

mod dexparser;
mod android_api;
mod jni_interface;

fn main() {
    dexparser::parse(b"");
}

#[test]
fn test_parser(){
    let b = std::fs::read(r#"C:\Users\YC\Downloads\PdaNet_v5.23_apkpure.com\classes.dex"#).unwrap();
}