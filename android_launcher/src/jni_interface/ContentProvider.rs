use std::ffi::CStr;
use std::sync::Arc;

use jni::NativeMethod;
use libc::c_void;
use parking_lot::RwLock;
use lock_api::*;

use rustc_hash::FxHashMap;

use jni::JavaVM;
use jni::JNIEnv;
use jni::sys::*;
use jni::objects::*;

use super::VmManager::JavaVmWorker;

lazy_static::lazy_static!{
    pub static ref ContentProviders:RwLock<rustc_hash::FxHashMap<String, ContentProvider>> = RwLock::new(Default::default());
}

pub struct ContentProvider{
    pub VM:Arc<JavaVmWorker>,
    pub object:GlobalRef
}

fn register(env:JNIEnv) -> Result<(), jni::errors::Error>{
    env.register_native_methods("android.content.ContentProviderClient", &[
        NativeMethod{
            name:"nGetLocalContentProvider".into(),
            sig:"(Ljava/lang/String;)Landroid/content/ContentProvider".into(),
            fn_ptr:GetLocalContentProvider as *mut c_void
        }
    ])
}

fn GetLocalContentProvider(env:JNIEnv, class:JClass, authority:JString) -> jobject{
    let guard = ContentProviders.read();
    if let Ok(s) = env.get_string(authority){
        if let Some(provider) = guard.get(&s.to_str().unwrap().to_string()){
            if provider.VM.JavaVm.get_java_vm_pointer() == env.get_java_vm().unwrap().get_java_vm_pointer(){
                return provider.object.as_obj().into_inner();
            }
        }
    }
    return JObject::null().into_inner();
}