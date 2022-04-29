use std::sync::Arc;
use std::io::Read;
use std::io::Write;

use super::types::*;

pub struct Engine{
    
}

impl Engine{
    pub fn new(
        path:Option<String>,
        rootfs:Option<String>,
        code:Option<Vec<u8>>,
        ostype:Option<OS>,
        archtype:Option<Arch>,
        bigendian:bool,
        multithread:bool,
        stdin:Option<Arc<dyn Read>>,
        stdout:Option<Arc<dyn Write>>,
        stderr:Option<Arc<dyn Write>>
    ) -> Engine{
        
    }
}