use std::borrow::Borrow;
use std::mem::size_of;
use std::mem::transmute;
use std::ops::Add;
use std::sync::Arc;

use jni::JNIEnv;
use jni::objects::*;

use jni::sys::*;
use parking_lot::Mutex;
use parking_lot::RwLock;
use lock_api::*;
use paste::paste;

use super::binder::BinderProxy;
use super::native_types::CursorWindow::CursorWindow;

fn addReturn(b:&mut usize, n:usize) -> usize{
    *b+=n;
    return *b;
}

macro_rules! gen_read {
    ($name:ident, $return:ty) => {
        paste!{
            pub fn [<read $name>](&mut self) -> Result<$return, &'static str>{
                if let Some(v) = self.readNext(){
                    match v{
                        ParcelValue::$name(v) => return Ok(v.clone()),
                        v => match v{
                            ParcelValue::Binder(_) => Err(stringify!(Parcel.[<read $name>]: next value expected $name, got Binder.)),
                            ParcelValue::CursorWindow(_) => Err(stringify!(Parcel.[<read $name>]: next value expected $name, got CursorWindow.)),
                            ParcelValue::Int(_) => Err(stringify!(Parcel.[<read $name>]: next value expected $name, got Int.)),
                            ParcelValue::Long(_) => Err(stringify!(Parcel.[<read $name>]: next value expected $name, got Long.)),
                            ParcelValue::Bool(_) => Err(stringify!(Parcel.[<read $name>]: next value expected $name, got Boolean.)),
                            ParcelValue::Float(_) => Err(stringify!(Parcel.[<read $name>]: next value expected $name, got Float.)),
                            ParcelValue::Double(_) => Err(stringify!(Parcel.[<read $name>]: next value expected $name, got Double.)),
                            ParcelValue::String(_) => Err(stringify!(Parcel.[<read $name>]: next value expected $name, got String.)),
                        }
                    }
                } else{
                    return Err("Parcel.next(): all value readed.")
                }
            }
        }
    };
}

#[repr(u8)]
pub enum ParcelValue{
    Int(i32),
    Long(jlong),
    Bool(jboolean),
    Float(f32),
    Double(f64),
    String(String),

    Binder(Arc<BinderProxy>),
    CursorWindow(Arc<RwLock<CursorWindow>>),
}

#[repr(u8)]
pub enum ParcelValueType{
    Int,
    Long,
    Bool,
    Float,
    Double,
    String,
    Binder,
    CursorWindow,
}

#[derive(Clone)]
pub struct Parcel{
    bytes:Vec<u8>,
    pos:usize
}

impl Parcel{
    pub fn fromParcelObject(env:JNIEnv, obj:JObject) -> Result<Arc<RwLock<Parcel>>, jni::errors::Error>{
        let guard = env.get_rust_field::<_,_,Arc<RwLock<Parcel>>>(obj, "mNativePtr")?;
        Ok(guard.clone())
    }

    pub unsafe fn TemporaryParcel(env:JNIEnv, parcel:Arc<RwLock<Parcel>>) -> Result<JObject, jni::errors::Error>{
        let buf = env.new_direct_byte_buffer((parcel.read().bytes.as_slice() as *const [u8] as *mut [u8]).as_mut().unwrap()).unwrap();
        let p = env.new_object("Landroid/os/Parcel", "(J;Ljava/nio/ByteBuffer)V", &[
            JValue::Long(parcel.as_ref() as *const RwLock<Parcel> as i64),
            JValue::Object(buf.into())

            ])?;
        return Ok(p);
    }

    

    pub fn readNext(&mut self) -> Option<ParcelValue>{
        if let Some(header) = self.bytes.get(self.pos){
            self.pos+=1;
            let start = self.pos;
            unsafe{
                return Some(match transmute::<u8, ParcelValueType>(*header){
                    ParcelValueType::Int => {
                        self.pos += size_of::<jint>();
                        ParcelValue::Int(transmute::<[u8;4],_>(self.bytes[start..self.pos].try_into().unwrap()))
                    },
                    ParcelValueType::Long => {
                        self.pos += size_of::<jlong>();
                        ParcelValue::Long(transmute::<[u8;8],_>(self.bytes[start..self.pos].try_into().unwrap()))
                    },
                    ParcelValueType::Bool => {
                        self.pos += size_of::<jboolean>();
                        ParcelValue::Bool(transmute::<[u8;1],_>(self.bytes[start..self.pos].try_into().unwrap()))
                    },
                    ParcelValueType::Float => {
                        self.pos = size_of::<jfloat>();
                        ParcelValue::Float(transmute::<[u8;4],_>(self.bytes[start..self.pos].try_into().unwrap()))
                    },
                    ParcelValueType::Double => {
                        self.pos += size_of::<jdouble>();
                        ParcelValue::Double(transmute::<[u8;8],_>(self.bytes[start..self.pos].try_into().unwrap()))
                    },
                    ParcelValueType::String => {
                        self.pos += size_of::<jint>();
                        let len:jint = transmute::<[u8;4],_>(self.bytes[start..self.pos].try_into().unwrap());
                        let b:&[u8] = &self.bytes[self.pos..len as usize];
                        self.pos += len as usize;
                        ParcelValue::String(String::from_utf8(b.to_vec()).unwrap())
                    },
                    ParcelValueType::Binder => {
                        self.pos += size_of::<jlong>();
                        let ptr:jlong = transmute::<[u8;8],_>(self.bytes[start..self.pos].try_into().unwrap());
                        let ptr = transmute(ptr as isize);
                        let re = ParcelValue::Binder(Clone::clone(&ptr));
                        std::mem::forget(ptr);
                        re
                    },
                    ParcelValueType::CursorWindow => {
                        self.pos += size_of::<jlong>();
                        let ptr:jlong = transmute::<[u8;8], _>(self.bytes[start..self.pos].try_into().unwrap());
                        let ptr = transmute(ptr as isize);
                        let re = ParcelValue::CursorWindow(Clone::clone(&ptr));
                        std::mem::forget(ptr);
                        re
                    },
                })
            }
        }
        return None;
    }

    gen_read!(Binder, Arc<BinderProxy>);
    gen_read!(CursorWindow, Arc<RwLock<CursorWindow>>);
    gen_read!(Int, jint);
    gen_read!(Long, jlong);
    gen_read!(Bool, jboolean);
    gen_read!(Float, jfloat);
    gen_read!(Double, jdouble);
    gen_read!(String, String);

    pub fn writeCursorWindow(&mut self, value:Arc<RwLock<CursorWindow>>){
        self.bytes.push(ParcelValueType::CursorWindow as u8);
        self.bytes.extend(unsafe{transmute::<_,[u8;8]>(value.as_ref() as *const RwLock<CursorWindow> as i64)});
    }

    // native methods
    pub unsafe fn create(env:JNIEnv, obj:JObject){
        let mut p = Arc::new(RwLock::new(Parcel{
            bytes:vec![0u8;256],
            pos:0
        }));

        env.set_rust_field(obj, "mNativePtr", p.clone());
        let buf = env.new_direct_byte_buffer((p.read().bytes.as_slice() as *const [u8] as *mut [u8]).as_mut().unwrap()).unwrap();
        env.set_field(obj, "mBuffer", "Ljava/nio/ByteBuffer", JValue::Object(buf.into()));
        env.set_field(obj, "mIsOwned", "Z", JValue::Bool(1));
    }

    pub fn finalize(env:JNIEnv, class:JClass, obj:JObject){
        if let Ok(v) = env.take_rust_field::<_,_,Arc<RwLock<Parcel>>>(obj, "mNativePtr"){
            drop(v);
        }
    }
}