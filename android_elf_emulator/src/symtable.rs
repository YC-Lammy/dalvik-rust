
use rustc_hash::FxHashMap;

pub struct SymTable{
    pub table:FxHashMap<String, usize>
}