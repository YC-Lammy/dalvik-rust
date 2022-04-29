use std::sync::Arc;

type Vector = [u64;2];


/// All A64 instructions have a width of32 bits.
#[repr(u32)]
pub enum AArch64Inst{
    ADR
}

pub struct VirtualMachine{
    pub mem:Arc<[u8]>,

    pub pc:u64, 
    /// general registers x0..x31 / w0..w31 while 'x' holds 64-bit and 'w' holds 32-bit
    pub registers:[u64; 31],
    /// SIMD and floating-point register
    pub vector_registers:[Vector;32],
    pub Fpcr:u32,
    pub Fpsr:u32,
    pub Pstate:u32,
}