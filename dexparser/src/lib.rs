#[cfg(test)]
mod tests {
    #[test]
    fn it_works() {
        let result = 2 + 2;
        assert_eq!(result, 4);
    }
}

#[macro_use] 
extern crate nom;
extern crate leb128;
extern crate byteorder;
extern crate failure;

mod parser;
mod bytecode_decoder;
mod error;
mod result_types;

#[test]
fn test_parser(){
    let b = std::fs::read(r#"C:\Users\YC\Downloads\PdaNet_v5.23_apkpure.com\classes.dex"#).unwrap();
    let re = parser::parse(b.as_slice()).unwrap();
    println!("checksum: {}", re);
}