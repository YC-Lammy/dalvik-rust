use std::io::Write;
use std::io::Read;
use std::path::Path;
use std::sync::Arc;

use super::engine::Engine;

pub enum OS{

}

pub enum Arch{

}

pub struct EngineBuilder{
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
}

impl EngineBuilder{
    fn new() -> Self{
        Self{
            path:None,
            rootfs:None,
            code:None,
            ostype:None,
            archtype:None,
            bigendian:false,
            multithread:false,
            stderr:None,
            stdin:None,
            stdout:None
        }
    }

    fn path<F>(&mut self, path:F) -> &Self where F:Into<String>{
        self.path = Some(path.into());
        return self;
    }

    fn rootfs<F>(&mut self, path:F) -> &Self where F:Into<String>{
        self.rootfs = Some(path.into());
        return self;
    }

    fn code(&mut self, code:&[u8]) -> &Self{
        self.code = Some(code.to_vec());
        return self;
    }

    fn os(&mut self, os:OS) -> &Self{
        self.ostype = Some(os);
        return self;
    }

    fn arch(&mut self, arch:Arch) -> &Self{
        self.archtype = Some(arch);
        return self;
    }

    fn bigendian(&mut self, b:bool) -> &Self{
        self.bigendian = b;
        return self;
    }

    fn multithreaded(&mut self, b:bool) -> &Self{
        self.multithread = b;
        return self;
    }

    fn stdin<R>(&mut self, r:R) -> &Self where R:Read+'static{
        self.stdin = Some(Arc::new(r));
        return self;
    }

    fn stdout<W>(&mut self, w:W) -> &Self where W:Write+'static{
        self.stdout = Some(Arc::new(w));
        return self;
    }

    fn stderr<W>(&mut self, w:W) -> &Self where W:Write+'static{
        self.stderr = Some(Arc::new(w));
        return self;
    }

    fn build(&self) -> Engine{
        Engine::new(
            self.path, 
            self.rootfs, 
            self.code, 
            self.ostype, 
            self.archtype, 
            self.bigendian, 
            self.multithread, 
            self.stdin, 
            self.stdout, 
            self.stderr)
    }
}