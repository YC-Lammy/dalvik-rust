mod parser;
mod error;
mod result_types;
mod bytecode_decoder;

pub use error::DexParserError;
pub use result_types::*;
pub use nom::Endianness;

pub fn parse(buf: &[u8]) -> Result<DexFile, DexParserError> {
    parser::parse(buf)
}

// TODO: validate checksum/signature