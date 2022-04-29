

use jni::JNIEnv;
use jni::errors::Error;


pub fn register(env:JNIEnv) -> Result<(), Error>{
    SharedMemory::register(env)?;
    StatFs::register(env)?;
    Trace::register(env)?;
    Clipboard::register(env)?;
    RestrictionManager::register(env)?;
    SQLiteConnection::register(env)?;
    return Ok(())
}

/*
    this is the native implementation of android.os.SharedMemory.
*/
mod SharedMemory{
    use std::ffi::CStr;
    use libc::c_char;
    use libc::c_int;
    use libc::c_void;

    use jni::NativeMethod;
    use jni::JNIEnv;
    use jni::objects::*;
    use jni::sys::{
        jint
    };
    
    pub unsafe fn create(env:JNIEnv, class:JClass, name:JString, size:jint) -> jint{
        let mut fname = "\0".as_ptr() as *const c_char;
        if let Ok(s) = env.get_string(name){
            fname = s.get_raw();
        }
        let fd = libc::memfd_create(fname, 0);
        libc::ftruncate(fd, size as libc::off_t);
        return fd as jint;
    }

    pub unsafe fn map<'a>(env:JNIEnv<'a>, class:JClass, fd:jint, prot:jint, offset:jint, length:jint) -> JByteBuffer<'a>{
        let ptr = libc::mmap(0 as _, 
            length as libc::size_t, 
            prot, 
            libc::MAP_SHARED, 
            fd as libc::c_int, 
            offset as libc::off_t
        );

        if let Ok(buf) = env.new_direct_byte_buffer(std::slice::from_raw_parts_mut(ptr as *mut u8, length as usize)){
            return buf;
        } else{
            env.throw("error creating direct byte buffer.");
            JObject::null().into()
        }
        
    }

    pub unsafe fn unmap(env:JNIEnv, class:JClass, buf:JByteBuffer){
        if let Ok(s) = env.get_direct_buffer_address(buf){
            libc::munmap(s.as_ptr() as *mut libc::c_void, s.len() as libc::size_t);
        }
    }

    pub unsafe fn close(env:JNIEnv, class:JClass, fd:jint){
        libc::close(fd as c_int);
    }

    pub unsafe fn getSize(env:JNIEnv, class:JClass, fd:jint) -> jint{
        let mut stat:libc::stat = std::mem::zeroed();
        libc::fstat(fd as c_int, &mut stat);
        return stat.st_size as jint;
    }

    pub fn register(env:JNIEnv) -> jni::errors::Result<()>{
        env.register_native_methods("android/os/SharedMemory", &[
            NativeMethod{
                name:"nCreate".into(),
                sig:"(Ljava/lang/String;I;)I".into(),
                fn_ptr:create as *mut c_void
            },
            NativeMethod{
                name:"nClose".into(),
                sig:"(I;)V".into(),
                fn_ptr:close as *mut c_void
            },
            NativeMethod{
                name:"nMap".into(),
                sig:"(I;I;I;I;)Ljava/nio/ByteBuffer".into(),
                fn_ptr:map as *mut c_void
            },
            NativeMethod{
                name:"nUnmap".into(),
                sig:"(Ljava/nio/ByteBuffer;)V".into(),
                fn_ptr:unmap as *mut c_void
            },
            NativeMethod{
                name:"nGetSize".into(),
                sig:"(I;)I".into(),
                fn_ptr:getSize as *mut c_void
            }
        ])
    }
}

/*
    state fs is a wrapper around libc::statvfs
 */
mod StatFs{
    use jni::JNIEnv;
    use jni::NativeMethod;
    use jni::objects::*;
    use jni::sys::jlongArray;

    pub unsafe fn getStat(env:JNIEnv,class:JClass, path:JString) -> jlongArray{
        
        if let Ok(s) = env.get_string(path){

            let mut fs :libc::statvfs = std::mem::zeroed();
            libc::statvfs(s.get_raw(), &mut fs);
            let ar = env.new_long_array(4).unwrap();

            let _= env.set_long_array_region(ar, 0, &[
                fs.f_frsize as i64,
                fs.f_blocks as i64,
                fs.f_bfree as i64,
                fs.f_bavail as i64,
            ]);
            return ar;
        } else{
            return env.new_long_array(0).unwrap()
        }
        
    }

    pub fn register(env:JNIEnv) -> Result<(), jni::errors::Error>{
        env.register_native_methods("android/os/StatFs", &[
            NativeMethod{
                name:"nGetStat".into(),
                sig:"(Ljava/lang/String;)[J".into(),
                fn_ptr:getStat as *mut libc::c_void
            }
        ])
    }
}

mod Trace{
    use chrono::Datelike;
    use jni::JNIEnv;
    use jni::NativeMethod;
    use jni::objects::*;
    use jni::sys::jclass;
    use jni::sys::{
        jlong,
        jboolean,
        jint
    };

    use libc::c_int;
    use libc::c_void;

    pub fn GetEnabledTags(env:JNIEnv, class:JClass) -> jlong{
        return 0;
    }

    pub fn SetAppTracingAllowed(env:JNIEnv, class:JClass, b:jboolean){

    }

    pub fn SetTracingEnabled(env:JNIEnv, class:JClass, b:jboolean){
        
    }

    pub fn TraceCounter(env:JNIEnv, class:JClass, tag:jlong, name:JString, value:jint){

    }

    pub fn TraceBegin(env:JNIEnv, class:JClass, tag:jlong, name:JString){
        
    }

    pub fn TraceEnd(env:JNIEnv, class:JClass, tag:jlong){
        
    }

    pub fn AsyncTraceBegin(env:JNIEnv, class:JClass, tag:jlong, name:JString, cookie:jint){

    }

    pub fn AsyncTraceEnd(env:JNIEnv, class:JClass, tag:jlong, name:JString, cookie:jint){

    }

    pub fn AsyncTraceForTrackBegin(env:JNIEnv, class:JClass, tag:jlong,trackname:JString, name:JString, cookie:jint){

    }

    pub fn AsyncTraceForTrackEnd(env:JNIEnv, class:JClass, tag:jlong,trackname:JString, name:JString, cookie:jint){

    }

    pub fn Instant(env:JNIEnv, class:JClass, tag:jlong, name:JString){
        
    }

    pub fn InstantForTrack(env:JNIEnv, class:JClass, tag:jlong,trackname:JString, name:JString){
        
    }

    pub fn register(env:JNIEnv) -> Result<(), jni::errors::Error>{
        env.register_native_methods("android.os.Trace", &[
            NativeMethod{
                name:"nativeGetEnabledTags".into(),
                sig:"()J".into(),
                fn_ptr:GetEnabledTags as *mut c_void
            },
            NativeMethod{
                name:"nativeSetAppTracingAllowed".into(),
                sig:"(Z;)V".into(),
                fn_ptr:SetAppTracingAllowed as *mut c_void,
            },
            NativeMethod{
                name:"nativeSetTracingEnabled".into(),
                sig:"(Z;)V".into(),
                fn_ptr:SetTracingEnabled as *mut c_void,
            },
            NativeMethod{
                name:"nativeTraceCounter".into(),
                sig:"(J;Ljava/lang/String;I;)V".into(),
                fn_ptr:TraceCounter as *mut c_void,
            },
            NativeMethod{
                name:"nativeTraceBegin".into(),
                sig:"(J;Ljava/lang/String;)V".into(),
                fn_ptr:TraceBegin as *mut c_void,
            },
            NativeMethod{
                name:"nativeTraceEnd".into(),
                sig:"(J;)V".into(),
                fn_ptr:TraceEnd as *mut c_void,
            },
            NativeMethod{
                name:"nativeAsyncTraceBegin".into(),
                sig:"(J;Ljava/lang/String;I;)V".into(),
                fn_ptr:AsyncTraceBegin as *mut c_void,
            },
            NativeMethod{
                name:"nativeAsyncTraceEnd".into(),
                sig:"(J;Ljava/lang/String;I;)V".into(),
                fn_ptr:AsyncTraceEnd as *mut c_void,
            },
            NativeMethod{
                name:"nativeAsyncTraceForTrackBegin".into(),
                sig:"(J;Ljava/lang/String;Ljava/lang/String;I;)V".into(),
                fn_ptr:AsyncTraceForTrackBegin as *mut c_void,
            },
            NativeMethod{
                name:"nativeAsyncTraceForTrackEnd".into(),
                sig:"(J;Ljava/lang/String;Ljava/lang/String;I;)V".into(),
                fn_ptr:AsyncTraceForTrackEnd as *mut c_void,
            },
            NativeMethod{
                name:"nativeInstantForTrack".into(),
                sig:"(J;Ljava/lang/String;Ljava/lang/String;)V".into(),
                fn_ptr:InstantForTrack as *mut c_void,
            },
            NativeMethod{
                name:"nativeInstant".into(),
                sig:"(J;Ljava/lang/String;)V".into(),
                fn_ptr:Instant as *mut c_void,
            },
        ])
    }
}


///  android.content.ClipboardManager
/// 
///  this module stores one single primary ClipboardData as a Parcel without referencing 
///  java objects.
mod Clipboard{
    use std::cell::RefCell;

    use jni::JNIEnv;
    use jni::JavaVM;
    use jni::NativeMethod;
    use jni::objects::*;

    use libc::c_void;
    use lock_api::RwLock;

    use crate::jni_interface::VmManager::JavaWorkers;
    use crate::jni_interface::parcel::Parcel;

    static mut PrimaryClip:Option<Parcel> = None;

    fn onPrimaryClipChanged(){
        let guard =  JavaWorkers.read();
        for (_, vm) in guard.iter(){
            vm.run_nonblocking(|env|{
                let _ = env.call_static_method(
                    "android.content.ClipboardManager", 
                    "reportPrimaryClipChanged", 
                    "()V", &[]
                );
            });
        }
    }

    fn setPrimaryClip(env:JNIEnv, class:JClass, clip:JObject){
        if let Ok(p) = Parcel::fromParcelObject(env, clip){
            unsafe{PrimaryClip = Some(p.read().clone())};
            onPrimaryClipChanged();
        }
    }

    fn clearPrimaryClip(env:JNIEnv, class:JClass){
        unsafe{PrimaryClip = None};    
        onPrimaryClipChanged();
    }

    fn addPrimaryClipChangeListener(env:JNIEnv, class:JClass){
        if let Ok(vm) = env.get_java_vm(){
            
        }
    }

    pub fn register(env:JNIEnv) -> Result<(), jni::errors::Error>{
        env.register_native_methods("android.content.ClipboardManager", &[
            NativeMethod{
                name:"nSetPrimaryClip".into(),
                sig:"(Landroid/os/Parcel;)V".into(),
                fn_ptr:setPrimaryClip as *mut c_void
            },
            NativeMethod{
                name:"nClearPrimaryClip".into(),
                sig:"()V".into(),
                fn_ptr:clearPrimaryClip as *mut c_void
            },
            NativeMethod{
                name:"nAddPrimaryClipChangeListener".into(),
                sig:"()V".into(),
                fn_ptr:addPrimaryClipChangeListener as *mut c_void
            }
        ])
    }
}

mod RestrictionManager{
    use jni::JNIEnv;
    use jni::NativeMethod;
    use jni::objects::*;
    use jni::sys::*;
    use libc::c_void;

    #[no_mangle]
    fn getApplicationRestrictions(env:JNIEnv, class:JClass, packageName:JString) -> jobject{
        return JObject::null().into_inner();
    }

    #[no_mangle]
    fn HasRestrictionsProvider(env:JNIEnv, class:JClass) -> jboolean{
        return 0;
    }

    #[no_mangle]
    fn RequestPermission(env:JNIEnv, class:JClass, packageName:JString, requestType:JString, requestId:JString, requestData:JObject){

    }

    #[no_mangle]
    fn NotifyPermissionResponse(env:JNIEnv, class:JClass, packageName:JString, requestData:JObject){

    }

    #[no_mangle]
    fn CreateLocalApprovalIntent(env:JNIEnv, class:JClass) -> jobject{
        return 0 as jobject;
    }

    pub fn register(env:JNIEnv) -> Result<(), jni::errors::Error>{
        env.register_native_methods("android.content.RestrictionManager", &[
            NativeMethod{
                name:"nGetApplicationRestrictions".into(),
                sig:"(Ljava/lang/String;)Landroid/os/Bundle".into(),
                fn_ptr:getApplicationRestrictions as *mut c_void
            },
            NativeMethod{
                name:"nHasRestrictionsProvider".into(),
                sig:"()Z".into(),
                fn_ptr:HasRestrictionsProvider as *mut c_void
            },
            NativeMethod{
                name:"nRequestPermission".into(),
                sig:"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Landroid/os/PersistableBundle;)V".into(),
                fn_ptr:RequestPermission as *mut c_void
            },
            NativeMethod{
                name:"nNotifyPermissionResponse".into(),
                sig:"(Ljava/lang/String;Landroid/os/PersistableBundle;)V".into(),
                fn_ptr:NotifyPermissionResponse as *mut c_void
            },
            NativeMethod{
                name:"nCreateLocalApprovalIntent".into(),
                sig:"()Landroid/content/Intent".into(),
                fn_ptr:CreateLocalApprovalIntent as *mut c_void
            }
        ])
    }
}

pub mod CursorWindow{
    use std::mem::size_of;
    use std::sync::Arc;

    use parking_lot::RwLock;
    use lock_api::*;

    use jni::JNIEnv;
    use jni::objects::*;
    use jni::sys::*;

    use crate::jni_interface::parcel::Parcel;

    pub struct CursorWindow{
        pub name:String,
        pub bytes:Vec<u8>,
        pub columns:usize,
        pub rows:usize,
        /// in bytes
        pub row_length:usize,
        pub types:Vec<Types>
    }

    type Types = rusqlite::types::Type;

    pub enum Value{
        Blob(Vec<u8>),
        String(String),
        Int(i64),
        Double(f64),
        Null
    }

    impl CursorWindow {
        pub fn getOffset(&self, column:usize, row:usize) -> Result<usize, String>{
            let mut column_off = 0;
            for t in &self.types{
                match *t{
                    Types::Blob => column_off += size_of::<Vec<u8>>(),
                    Types::Text => column_off += size_of::<String>(),
                    Types::Integer => column_off += size_of::<i64>(),
                    Types::Real => column_off += size_of::<f64>(),
                    Types::Null => unreachable!()
                }
            };
            let mut offx = 0;
            let mut i=0;
            while i<column{
                match self.types[i]{
                    Types::Blob => offx += size_of::<Option<Vec<u8>>>(),
                    Types::Text => offx += size_of::<Option<String>>(),
                    Types::Integer => offx += size_of::<Option<i64>>(),
                    Types::Real => offx += size_of::<Option<f64>>(),
                    Types::Null => unreachable!()
                }
                i+=1;
            };
            let re = column_off*row + offx;
            if re > self.bytes.len(){
                return Err(format!("data exceding allocated bytes: {}, got {}.", self.bytes.len(), re))
            }
            return Ok(re);
        }

        pub unsafe fn write(&self, column:usize, row:usize, value:Value) -> Result<(), String>{
            if self.columns < column{
                return Err(format!("column {} was not allocated.", column));
            }
            match value{
                Value::Blob(b) => {
                    if !(self.types[column-1] == Types::Blob){
                        return Err(format!("column {} is type {}, found Blob.", column, self.types[column-1]))
                    }
                    let ptr = (self.bytes.as_ptr()).add(self.getOffset(column, row)?) as *mut Option<Vec<u8>>;
                    let b = Some(b);
                    ptr.swap(&b as *const Option<Vec<u8>> as *mut _); // swap out the old value to be dropped
                    drop(b);
                },
                Value::String(s) => {
                    if !(self.types[column-1] == Types::Text){
                        return Err(format!("column {} is type {}, found String.", column, self.types[column-1]))
                    }
                    let ptr = (self.bytes.as_ptr()).add(self.getOffset(column, row)?) as *mut Option<String>;
                    let s = Some(s);
                    ptr.swap(&s as *const Option<String> as *mut _); // swap out the old value to be dropped
                    drop(s);
                },
                Value::Int(i) => {
                    if !(self.types[column-1] == Types::Integer){
                        return Err(format!("column {} is type {}, found Int.", column, self.types[column-1]))
                    }
                    let ptr = (self.bytes.as_ptr()).add(self.getOffset(column, row)?) as *mut Option<i64>;
                    *ptr = Some(i);
                },
                Value::Double(d) => {
                    if !(self.types[column-1] == Types::Real){
                        return Err(format!("column {} is type {}, found Double.", column, self.types[column-1]))
                    }
                    let ptr = (self.bytes.as_ptr()).add(self.getOffset(column, row)?) as *mut Option<f64>;
                    *ptr = Some(d);
                },
                Value::Null => {
                    match self.types[column-1]{
                        Types::Text => {
                            let ptr = (self.bytes.as_ptr()).add(self.getOffset(column, row)?) as *mut Option<String>;
                            *ptr = None;
                        },
                        Types::Blob => {
                            let ptr = (self.bytes.as_ptr()).add(self.getOffset(column, row)?) as *mut Option<Vec<u8>>;
                            *ptr = None;
                        },
                        Types::Integer => {
                            let ptr = (self.bytes.as_ptr()).add(self.getOffset(column, row)?) as *mut Option<f64>;
                            *ptr = None;
                        },
                        Types::Real => {
                            let ptr = (self.bytes.as_ptr()).add(self.getOffset(column, row)?) as *mut Option<f64>;
                            *ptr = None;
                        },
                        Types::Null => unreachable!()
                    }
                }
            }
            return Ok(());
        }

        pub unsafe fn read(&self, column:usize, row:usize, ty:Types) -> Result<Value, String>{
            if self.columns < column{
                return Err(format!("column {} was not allocated.", column));
            }
            match ty{
                Types::Blob => {
                    if !(self.types[column-1] == ty){
                        return Err(format!("column {} is type {}, found Blob.", column, self.types[column-1]))
                    }
                    let ptr = (self.bytes.as_ptr()).add(self.getOffset(column, row)?) as *mut Option<Vec<u8>>;
                    let ptr = ptr.as_ref().unwrap();
                    if let Some(v) = ptr{
                        return Ok(Value::Blob(v.clone()))
                    } else{
                        return  Ok(Value::Null);
                    }
                },
                Types::Text => {
                    if !(self.types[column-1] == Types::Text){
                        return Err(format!("column {} is type {}, found String.", column, self.types[column-1]))
                    }
                    let ptr = (self.bytes.as_ptr()).add(self.getOffset(column, row)?) as *mut Option<String>;
                    let ptr = ptr.as_ref().unwrap();
                    if let Some(v) = ptr{
                        return Ok(Value::String(v.clone()))
                    } else{
                        return  Ok(Value::Null);
                    }
                },
                Types::Integer => {
                    if !(self.types[column-1] == Types::Integer){
                        return Err(format!("column {} is type {}, found Int.", column, self.types[column-1]))
                    }
                    let ptr = (self.bytes.as_ptr()).add(self.getOffset(column, row)?) as *mut Option<i64>;
                    if let Some(v) = *ptr{
                        return Ok(Value::Int(v))
                    } else{
                        return  Ok(Value::Null);
                    }
                },
                Types::Real => {
                    if !(self.types[column-1] == Types::Real){
                        return Err(format!("column {} is type {}, found Double.", column, self.types[column-1]))
                    }
                    let ptr = (self.bytes.as_ptr()).add(self.getOffset(column, row)?) as *mut Option<f64>;
                    if let Some(v) = *ptr{
                        return Ok(Value::Double(v))
                    } else{
                        return  Ok(Value::Null);
                    }
                },
                Types::Null => {
                    return Ok(Value::Null);
                }
            }
            return Ok(Value::Null);
        }
    }

    

    #[no_mangle]
    fn Create(env:JNIEnv, obj:JObject, name:JString, size:jint){
        if let Ok(name) = env.get_string(name){
            let mut window = CursorWindow{
                name:name.to_str().unwrap().to_string(),
                bytes:Vec::with_capacity(size as usize),
                columns:0,
                row_length:0,
                rows:0,
                types:Vec::new()
            };
            window.bytes.resize(size as usize, 0u8);
            let window = Arc::new(RwLock::new(window));
            env.set_rust_field(obj, "mNativePtr", window);
        }
    }

    #[no_mangle]
    fn Dispose(env:JNIEnv, obj:JObject){
        if let Ok(v) = env.take_rust_field::<_,_,Arc<RwLock<CursorWindow>>>(obj, "mNativePtr"){
            drop(v);
        }
    }

    #[no_mangle]
    fn CreateFromParcel(env:JNIEnv, obj:JObject, parcel:JObject){
        if let Ok(parcel) = Parcel::fromParcelObject(env, parcel){
            if let Ok(c) = parcel.write().readCursorWindow(){
                env.set_rust_field(obj, "mNativePtr", c);
            }
        }
    }

    #[no_mangle]
    fn WriteToParcel(env:JNIEnv, obj:JObject, parcel:JObject){
        if let Ok(parcel) = Parcel::fromParcelObject(env, parcel){
            if let Ok(w) = env.get_rust_field::<_,_,Arc<RwLock<CursorWindow>>>(obj, "mNativePtr"){
                parcel.write().writeCursorWindow(w.clone())
            }
        }
    }

    #[no_mangle]
    fn GetName(env:JNIEnv, obj:JObject) -> jstring{
        if let Ok(v) = env.get_rust_field::<_,_,Arc<RwLock<CursorWindow>>>(obj, "mNativePtr"){
            if let Ok(s) = env.new_string(v.read().name.as_str()){
                return s.into_inner();
            }
        }
        return 0 as jstring;
    }

    #[no_mangle]
    unsafe fn GetBlob(env:JNIEnv, obj:JObject, row:jint, column:jint) -> jbyteArray{
        if let Ok(v) = env.get_rust_field::<_,_,Arc<RwLock<CursorWindow>>>(obj, "mNativePtr"){

            let re = v.read().read(column as usize, row as usize, Types::Blob);

            if let Ok(v) = re{
                
                match v{
                    Value::Blob(v) => {
                        return env.byte_array_from_slice(v.as_slice()).unwrap();
                    },
                    Value::String(s) => {
                        return env.byte_array_from_slice(s.as_bytes()).unwrap();
                    },
                    Value::Null => return 0 as jbyteArray,
                    Value::Int(i) => {env.throw(("android/database/sqlite/SQLiteException", "expected type Blob but field is of type Int."));},
                    Value::Double(i) => {env.throw(("android/database/sqlite/SQLiteException", "expected type Blob but field is of type Double."));},
                }
            } else{
                env.throw(("android/database/sqlite/SQLiteException", re.err().unwrap()));
            }
        }
        return 0 as jbyteArray;
    }
    
    #[no_mangle]
    unsafe fn GetString(env:JNIEnv, obj:JObject, row:jint, column:jint) -> jstring{
        if let Ok(v) = env.get_rust_field::<_,_,Arc<RwLock<CursorWindow>>>(obj, "mNativePtr"){

            let re = v.read().read(column as usize, row as usize, Types::Blob);

            if let Ok(v) = re{
                
                match v{
                    Value::Blob(v) => {env.throw(("android/database/sqlite/SQLiteException", "expected type String but field is of type Blob."));},
                    Value::String(s) => {
                        return env.new_string(s).unwrap().into_inner();
                    },
                    Value::Null => return 0 as jbyteArray,
                    Value::Int(i) => return env.new_string(i.to_string()).unwrap().into_inner(),
                    Value::Double(i) => return env.new_string(i.to_string()).unwrap().into_inner(),
                }
            } else{
                env.throw(("android/database/sqlite/SQLiteException", re.err().unwrap()));
            }
        }
        return 0 as jstring;
    }

    #[no_mangle]
    unsafe fn CopyStringToBuffer(env:JNIEnv, obj:JObject, row:jint, column:jint, buffer:jcharArray) -> jint{
        if let Ok(v) = env.get_rust_field::<_,_,Arc<RwLock<CursorWindow>>>(obj, "mNativePtr"){

            let re = v.read().read(column as usize, row as usize, Types::Blob);

            if let Ok(v) = re{
                
                match v{
                    Value::Blob(v) => {env.throw(("android/database/sqlite/SQLiteException", "expected type String but field is of type Blob."));},
                    Value::String(s) => {
                        let mut chars = Vec::new();
                        for i in s.chars(){
                            chars.push(i as jchar);
                        }
                        env.set_char_array_region(buffer, 0, chars.as_slice());
                        return chars.len() as jint;
                    },
                    Value::Null => return 0,
                    Value::Int(i) => {
                        let s = i.to_string();
                        let mut chars = Vec::new();
                        for i in s.chars(){
                            chars.push(i as jchar);
                        }
                        env.set_char_array_region(buffer, 0, chars.as_slice());
                        return chars.len() as jint;
                    },
                    Value::Double(i) => {
                        let s = i.to_string();
                        let mut chars = Vec::new();
                        for i in s.chars(){
                            chars.push(i as jchar);
                        }
                        env.set_char_array_region(buffer, 0, chars.as_slice());
                        return chars.len() as jint;
                    },
                }
            } else{
                env.throw(("android/database/sqlite/SQLiteException", re.err().unwrap()));
            }
        }
        return 0;
    }
}

#[allow(unused, non_camel_case_types, non_snake_case)]
mod SQLiteConnection{
    use std::sync::Arc;

    use jni::JNIEnv;
    use jni::NativeMethod;
    use jni::objects::*;
    use jni::sys::*;
    use libc::c_char;
    use libc::c_schar;
    use libc::c_void;

    use parking_lot::Mutex;
    use lock_api::*;

    use rusqlite::*;
    use rusqlite::types::ToSqlOutput;

    use crate::jni_interface::VmManager;
    extern crate libsqlite3_sys;

    struct SQLStatment<'a>{
        inner:Statement<'a>,
        binding:Vec<ToSqlOutput<'a>>
    }

    fn Open(env:JNIEnv, obj:JObject, path:JString, openFlags:jint, label:JString,
        enableTrace:jboolean, enableProfile:jboolean, lookasideSlotSize:jint,
        lookasideSlotCount:jint)
    {
        if let Ok(path) = env.get_string(path){
            let mut flags = rusqlite::OpenFlags::empty();
            if openFlags == 0{
                flags &= rusqlite::OpenFlags::SQLITE_OPEN_READ_WRITE;
            }
            if (openFlags & 0x00000001) != 0{
                flags &= rusqlite::OpenFlags::SQLITE_OPEN_READ_ONLY;
            }
            if (openFlags & 0x10000000) != 0{
                flags &= rusqlite::OpenFlags::SQLITE_OPEN_CREATE;
            }
            let re = rusqlite::Connection::open_with_flags(path.to_str().unwrap(), flags);
            if let Ok(c) = re{
                env.set_rust_field(obj, "mNativePtr", Arc::new(Mutex::new(c)));
            }else{
                let _ = env.throw(("android/database/sqlite/SQLiteCantOpenDatabaseException", re.err().unwrap().to_string()));
            }
        } else{
            let _ = env.throw(("android/database/sqlite/SQLiteCantOpenDatabaseException","cannot read property path of type String."));
        }
        
    }

    #[no_mangle]
    unsafe fn Close(env:JNIEnv, obj:JObject){
        if let Ok(v) = env.take_rust_field::<_, _, Arc<Mutex<Connection>>>(obj, "mNativePtr"){

        }
    }

    #[no_mangle]
    unsafe fn RegisterCustomScalarFunction(env:JNIEnv, conn:JObject, name:JString, function:JObject){
        let worker = VmManager::resolveWorker(env).unwrap();
        let gref = env.new_global_ref(function).unwrap();

        if let Ok(connection) = env.get_rust_field::<_,_,Arc<Mutex<Connection>>>(conn, "mNativePtr"){
            if let Ok(s) = env.get_string(name){

                connection.lock().create_scalar_function(
                    s.to_str().unwrap(), 
                    1, 
                    functions::FunctionFlags::SQLITE_UTF8, 
                    move|ctx|{

                        // clone everything needed to be moved
                        let s = ctx.get::<String>(0).unwrap();
                        let gref = gref.clone();

                        // execute in the attached java worker thread
                        let re = worker.run(move |env|{

                            let obj = gref.as_obj();

                            // call the UnaryOperation<String>.apply method
                            let re = env.call_method(obj, "apply", "(Ljava/lang/String;)Ljava/lang/String", &[
                                env.new_string(s.clone()).unwrap().into()
                            ]);

                            // unwrap result
                            if let Ok(re) = re{
                                return Ok(env.get_string(re.l().unwrap().into()).unwrap().to_str().unwrap().to_string());
                            } else{
                                return Err(re.err().unwrap())
                            }

                        });

                        // process result from worker 
                        if let Some(v) = re{
                            if let Ok(v) = v{
                                return Ok(v);
                            } else{
                                return Err(rusqlite::Error::UserFunctionError((Box::new(v.err().unwrap()))));
                            }
                        } else{
                            // todo: find a more apropriate error
                            return Err(rusqlite::Error::UnwindingPanic)
                        }
                });
            }
        }
    }

    /// wrapper struct performs reduce function
    struct Aggregator{
        caller:Box<dyn Fn(&mut functions::Context, &mut String) -> Result<()>+'static>
    }

    impl rusqlite::functions::Aggregate<String, String> for Aggregator{
        fn init(&self, ctx: &mut functions::Context<'_>) -> Result<String> {
            return Ok("the first step.".to_owned());
        }

        fn step(&self, ctx: &mut functions::Context<'_>, red: &mut String) -> Result<()> {
            if red.as_str() == "the first step"{
                *red = ctx.get(0)?;
                return Ok(());
            }
            self.caller.as_ref()(ctx, red);
            return Ok(());
        }

        fn finalize(&self, ctx: &mut functions::Context<'_>, red: Option<String>) -> Result<String> {
            if let Some(v) = red{
                return Ok(v)
            } else{
                // todo: find a more apropriate error
                return Err(rusqlite::Error::GetAuxWrongType);
            }
        }
    }

    /// Register a custom aggregate function that can be called from SQL
    /// it implements a reduce function that calles the custom BinaryOperation
    #[no_mangle]
    unsafe fn RegisterCustomAggregateFunction(env:JNIEnv, conn:JObject, name:JString, function:JObject){

        let worker = VmManager::resolveWorker(env).unwrap();
        let gref = env.new_global_ref(function).unwrap();

        if let Ok(connection) = env.get_rust_field::<_,_,Arc<Mutex<Connection>>>(conn, "mNativePtr"){
            if let Ok(s) = env.get_string(name){

                connection.lock().create_aggregate_function(
                    s.to_str().unwrap(), 
                    1, 
                    functions::FunctionFlags::SQLITE_UTF8, 

                    // wrap the function into a struct
                    Aggregator{
                        caller:Box::new( move|ctx, reduced|{

                            // clone all params needed to move to worker thread
                            let s = ctx.get::<String>(0).unwrap();
                            let gref = gref.clone();
                            let re_s = reduced.clone();

                            // the worker executes in the attached java thread
                            let re = worker.run(move |env|{

                                let obj = gref.as_obj();

                                // call the BinaryOperation<String>.apply method
                                let re = env.call_method(obj, "apply", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String", &[
                                    env.new_string(re_s.clone()).unwrap().into(),
                                    env.new_string(s.clone()).unwrap().into()
                                ]);

                                if let Ok(re) = re{
                                    return Ok(env.get_string(re.l().unwrap().into()).unwrap().to_str().unwrap().to_string());
                                } else{
                                    return Err(re.err().unwrap())
                                }
                                
                            });

                            // process the result from worker
                            if let Some(v) = re{
                                if let Ok(v) = v{
                                    *reduced = v;
                                    return Ok(());
                                } else{
                                    return Err(rusqlite::Error::UserFunctionError((Box::new(v.err().unwrap()))));
                                }
                            } else{
                                // todo: find a more apropriate error
                                return Err(rusqlite::Error::UnwindingPanic)
                            }
                        })
                    }
                );
            }
        }
    }

    /// register the "LOCALIZED" collator, todo
    #[no_mangle]
    unsafe fn RegisterLocalizedCollators(env:JNIEnv, conn:JObject, locale:JString){

        if let Ok(connection) = env.get_rust_field::<_,_,Arc<Mutex<Connection>>>(conn, "mNativePtr"){

            connection.lock().create_collation("LOCALIZED", move |s1, s2|{
                return std::cmp::Ordering::Equal;
            });
        }
    }

    // creates a SQLStatment and move it into java
    #[no_mangle]
    unsafe fn PrepareStatement(env:JNIEnv, conn:JObject, sql:JString) -> jlong{

        if let Ok(connection) = env.get_rust_field::<_,_,Arc<Mutex<Connection>>>(conn, "mNativePtr"){
            if let Ok(s) = env.get_string(sql){

                let guard = connection.lock();
                let re = guard.prepare(s.to_str().unwrap());

                if let Ok(r) = re{

                    let c = r.parameter_count();

                    let stmt = Box::leak(Box::new(SQLStatment{
                        inner:r,
                        binding:Vec::new(),
                    }));

                    stmt.binding.resize(c, ToSqlOutput::Owned(types::Value::Null));
                    return stmt as *mut SQLStatment as jlong;

                } else{
                    env.throw(("android/database/sqlite/SQLiteException", re.err().unwrap().to_string().as_str()));
                    return 0;
                }
            } else{
                env.throw(("android/database/sqlite/SQLiteException", "cannot read String."));
                return 0;
            }
        } else{
            env.throw(("android/database/sqlite/SQLiteException", "failed to get connection."));
            return 0;
        }
    }

    /// moves out the SQLStatment and drops it
    unsafe fn FinalizeStatement(env:JNIEnv, conn:JObject, stmt:jlong){
        if let Some(stmt) = (stmt as *mut SQLStatment).as_mut(){
            std::ptr::drop_in_place(stmt);
        }
    }

    unsafe fn GetParameterCount(env:JNIEnv, conn:JObject, stmt:jlong) -> jint{
        if let Some(stmt) = (stmt as *mut SQLStatment).as_mut(){
            return stmt.inner.parameter_count() as jint;
        } else{
            return -1;
        }
    }

    unsafe fn IsReadOnly(env:JNIEnv, conn:JObject, stmt:jlong) -> jboolean{
        if let Some(stmt) = (stmt as *mut SQLStatment).as_mut(){
            return 0;
        } else{
            return 0;
        }
    }

    unsafe fn GetColumnCount(env:JNIEnv, conn:JObject, stmt:jlong) -> jint{
        if let Some(stmt) = (stmt as *mut SQLStatment).as_mut(){
            return stmt.inner.column_count() as jint;
        } else{
            return -1;
        }
    }

    unsafe fn GetColumnName(env:JNIEnv, conn:JObject, stmt:jlong, index:jint) -> jstring{
        if let Some(stmt) = (stmt as *mut SQLStatment).as_mut(){
            if let Ok(s) = stmt.inner.column_name(index as usize){
                if let Ok(s)  = env.new_string(s){
                    return s.into_inner();
                } else{
                    env.throw(("android/database/sqlite/SQLiteException", "error creating String."));
                }
            } else{
                env.throw(("android/database/sqlite/SQLiteException", "index out of range."));
            }
            
        } else{
            env.throw(("android/database/sqlite/SQLiteException", "error getting Statment."));
        }
        return 0 as jstring;
    }

    /// a lazy function to implement bindings to different types
    unsafe fn bind<T: rusqlite::ToSql>(env:JNIEnv, stmt:jlong, index:jint, value:T){
        if let Some(stmt) = (stmt as *mut SQLStatment).as_mut(){
            if index < 0 || stmt.binding.len() < index as usize{
                env.throw(("android/database/sqlite/SQLiteException", "binding params index out of range."));
            }
            stmt.binding[index as usize] = value.to_sql().unwrap();
        } else{
            env.throw(("android/database/sqlite/SQLiteException", "error getting Statment."));
        }
    }

    #[no_mangle]
    unsafe fn BindNull(env:JNIEnv, conn:JObject, stmt:jlong, index:jint){
        bind::<rusqlite::types::Null>(env, stmt, index, rusqlite::types::Null);
    }

    #[no_mangle]
    unsafe fn BindLong(env:JNIEnv, conn:JObject, stmt:jlong, index:jint, value:jlong){
        bind::<jlong>(env, stmt, index, value);
    }

    #[no_mangle]
    unsafe fn BindDouble(env:JNIEnv, conn:JObject, stmt:jlong, index:jint, value:jdouble){
        bind::<jdouble>(env, stmt, index, value);
    }

    #[no_mangle]
    unsafe fn BindString(env:JNIEnv, conn:JObject, stmt:jlong, index:jint, value:JString){
        if let Ok(s) = env.get_string(value){
            bind::<&str>(env, stmt, index, s.to_str().unwrap());
        } else{
            env.throw(("android/database/sqlite/SQLiteException", "error reading String."));
        }
    }

    #[no_mangle]
    unsafe fn BindBlob(env:JNIEnv, conn:JObject, stmt:jlong, index:jint, array:jbyteArray){
        let mut b = Vec::new();
        b.resize(env.get_array_length(array).unwrap() as usize, 0i8);
        env.get_byte_array_region(array, 0, b.as_mut_slice());
        bind::<Vec<u8>>(env, stmt, index, std::mem::transmute(b));
    }

    #[no_mangle]
    unsafe fn ResetStatementAndClearBindings(env:JNIEnv, conn:JObject, stmt:jlong){
        if let Some(stmt) = (stmt as *mut SQLStatment).as_mut(){
            let c = stmt.binding.len();
            stmt.binding.clear();
            stmt.binding.resize(c, ToSqlOutput::Owned(types::Value::Null));

        } else{
            env.throw(("android/database/sqlite/SQLiteException", "error getting Statment."));
        }
    }

    unsafe fn Execute(env:JNIEnv, conn:JObject, stmt:jlong){
        if let Some(stmt) = (stmt as *mut SQLStatment).as_mut(){

            let re = stmt.inner.execute(params_from_iter(stmt.binding.as_slice()));
            if let Ok(v) = re{

            } else{
                env.throw(("android/database/sqlite/SQLiteException", re.err().unwrap().to_string()));
            }
        } else{
            env.throw(("android/database/sqlite/SQLiteException", "error getting Statment."));
        }
    }

    unsafe fn ExecuteForLong(env:JNIEnv, conn:JObject, stmt:jlong) -> jlong{

        if let Some(stmt) = (stmt as *mut SQLStatment).as_mut(){

            let re = stmt.inner.query(params_from_iter(stmt.binding.as_slice()));
            if let Ok(mut v) = re{
                if let Ok(i) = v.next().unwrap().unwrap().get(0){
                    return i;
                }
                
            } else{
                env.throw(("android/database/sqlite/SQLiteException", re.err().unwrap().to_string()));
            }
        } else{
            env.throw(("android/database/sqlite/SQLiteException", "error getting Statment."));
        }
        return 0;
    }

    unsafe fn ExecuteForString(env:JNIEnv, conn:JObject, stmt:jlong) -> jstring{

        if let Some(stmt) = (stmt as *mut SQLStatment).as_mut(){

            let re = stmt.inner.query(params_from_iter(stmt.binding.as_slice()));
            if let Ok(mut v) = re{
                if let Ok(i) = v.next().unwrap().unwrap().get::<_,String>(0){
                    return env.new_string(i).unwrap().into_inner();
                }
                
            } else{
                env.throw(("android/database/sqlite/SQLiteException", re.err().unwrap().to_string()));
            }
        } else{
            env.throw(("android/database/sqlite/SQLiteException", "error getting Statment."));
        }
        return 0 as jstring;
    }

    unsafe fn ExecuteForBlobFileDescriptor(env:JNIEnv, conn:JObject, stmt:jlong) -> jint{

        if let Some(stmt) = (stmt as *mut SQLStatment).as_mut(){

            let re = stmt.inner.query(params_from_iter(stmt.binding.as_slice()));
            if let Ok(mut v) = re{

                let re = v.next().unwrap().unwrap().get::<_,Vec<u8>>(0);

                if let Ok(blob) = re{
                    let name = chrono::Utc::now().timestamp_millis().to_string()+"\0";
                    let fd = libc::memfd_create(name.as_ptr() as *const c_char, 0);
                    drop(name);
                    libc::ftruncate(fd, blob.len() as libc::off_t);
                    libc::write(fd, blob.as_ptr() as *const c_void, blob.len());
                    return fd as jint;
                } else{
                    env.throw(("android/database/sqlite/SQLiteException", re.err().unwrap().to_string()));
                }
                
            } else{
                env.throw(("android/database/sqlite/SQLiteException", re.err().unwrap().to_string()));
            }
        } else{
            env.throw(("android/database/sqlite/SQLiteException", "error getting Statment."));
        }
        return -1;
    }

    unsafe fn ExecuteForChangedRowCount(env:JNIEnv, conn:JObject, stmt:jlong) -> jint{
        if let Some(stmt) = (stmt as *mut SQLStatment).as_mut(){

            let re = stmt.inner.execute(params_from_iter(stmt.binding.as_slice()));
            if let Ok(v) = re{
                return v as jint;
            } else{
                env.throw(("android/database/sqlite/SQLiteException", re.err().unwrap().to_string()));
            }
        } else{
            env.throw(("android/database/sqlite/SQLiteException", "error getting Statment."));
        }
        return 0;
    }

    unsafe fn ExecuteForLastInsertedRowId(env:JNIEnv, conn:JObject, stmt:jlong) -> jlong{
        let conn = 
        if let Ok(v) = env.get_rust_field::<_,_,Arc<Mutex<Connection>>>(conn, "mNativePtr"){
            v
        } else{
            env.throw(("android/database/sqlite/SQLiteException", "error reading connection."));
            return -1;
        };

        if let Some(stmt) = (stmt as *mut SQLStatment).as_mut(){

            let re = stmt.inner.execute(params_from_iter(stmt.binding.as_slice()));

            if re.is_ok(){
                return conn.lock().last_insert_rowid();

            } else{
                env.throw(("android/database/sqlite/SQLiteException", re.err().unwrap().to_string()));
            }
        } else{
            env.throw(("android/database/sqlite/SQLiteException", "error getting Statment."));
        }
        return 0;
    }

    #[no_mangle]
    unsafe fn ExecuteForCursorWindow(env:JNIEnv, conn:JObject, stmt:jlong, window_ptr:jlong, start:jint, require:jint, countAllRows:jboolean) -> jlong{
        if let Some(stmt) = (stmt as *mut SQLStatment).as_mut(){

            let re = stmt.inner.query(params_from_iter(stmt.binding.as_slice()));
            if let Ok(v) = re{
                return 0;
            } else{
                env.throw(("android/database/sqlite/SQLiteException", re.err().unwrap().to_string()));
            }
        } else{
            env.throw(("android/database/sqlite/SQLiteException", "error getting Statment."));
        }
        return 0;
    }

    #[no_mangle]
    unsafe fn GetDbLookaside(env:JNIEnv, conn:JObject) -> jint{
        if let Ok(conn) = env.get_rust_field::<_,_,Arc<Mutex<Connection>>>(conn, "mNativePtr"){
            
        };
        return 0;
    }

    #[no_mangle]
    unsafe fn Cancel(env:JNIEnv, conn:JObject){
        if let Ok(conn) = env.get_rust_field::<_,_,Arc<Mutex<Connection>>>(conn, "mNativePtr"){
            conn.lock().get_interrupt_handle().interrupt()
        };
    }

    #[no_mangle]
    fn ResetCancel(env:JNIEnv, conn:JObject){

    }

    pub fn register(env:JNIEnv) -> Result<(), jni::errors::Error>{
        env.register_native_methods("android.database.sqlite.SQLiteConnection", &[
            
        ])
    }
}