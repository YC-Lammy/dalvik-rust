use std::ffi::CStr;
use std::sync::Arc;

use jni::JNIEnv;
use jni::NativeMethod;
use jni::objects::*;
use jni::sys::*;
use libc::c_void;
use parking_lot::RwLock;

use super::VmManager::JavaVmWorker;
use super::parcel::Parcel;


fn register(env:JNIEnv) -> Result<(), jni::errors::Error>{
    env.register_native_methods("android.os.BinderProxy", &[
        NativeMethod{
            name:"transactNative".into(),
            sig:"(I;Landroid/os/Parcel;Landroid/os/Parcel;I;)Z".into(),
            fn_ptr:transact as *mut c_void
        },
        NativeMethod{
            name:"finalize".into(),
            sig:"()V".into(),
            fn_ptr:finalize as *mut c_void
        },
        NativeMethod{
            name:"pingBinder".into(),
            sig:"()V".into(),
            fn_ptr:pingBinder as *mut c_void
        },
        NativeMethod{
            name:"getInterfaceDescriptor".into(),
            sig:"()Ljava/lang/String".into(),
            fn_ptr:getInterfaceDescriptor as *mut c_void
        },
        NativeMethod{
            name:"linkToDeath".into(),
            sig:"(Landroid/os/Ibinder/DeathRecipient;I;)V".into(),
            fn_ptr:linkToDeath as *mut c_void
        },
        NativeMethod{
            name:"unlinkToDeath".into(),
            sig:"(Landroid/os/Ibinder/DeathRecipient;I;)Z".into(),
            fn_ptr:unlinkToDeath as *mut c_void
        }
    ])
}

pub struct BinderProxy{
    //pub thisVm:Arc<JavaVmWorker>,
    pub thatVm:Arc<JavaVmWorker>,
    pub thatObject:GlobalRef,
    pub service:GlobalRef
}

impl Drop for BinderProxy{
    fn drop(&mut self) {
        let g = self.thatObject.clone();
        let service = self.service.clone();

        // drop the global refernce in the vm's thread to avoid thread attaching
        self.thatVm.run_nonblocking(move|env|{
            let _obj = g.as_obj();
            let ser = service.as_obj();
            let _ = env.call_method(ser, "onBinderDisconnect", "()V", &[]);
        });
    }
}

#[no_mangle]
pub fn finalize(env:JNIEnv, proxy:JObject){
    if let Ok(v) = env.take_rust_field::<_,_,Arc<BinderProxy>>(proxy, "mNativePtr"){
        drop(v)
    }
}

#[no_mangle]
pub fn transact(env:JNIEnv, proxy:JObject, 
    code:jint, data:JObject, reply:JObject, flags:jint
) -> jboolean{
    if let Ok(p) = env.get_rust_field::<_,_,Arc<BinderProxy>>(proxy, "mNativePtr"){
        if let Ok(data ) = Parcel::fromParcelObject(env, data){
            if let Ok(reply) = Parcel::fromParcelObject(env, reply){

                let globref = p.thatObject.clone();

                let re = p.thatVm.run(move |that_env|{

                    let obj = globref.as_obj();
                    let data1 = that_env.new_object(
                        "android/os/Parcel", "(J;)V", &[JValue::Long(0)]).unwrap();
                    let reply1 = that_env.new_object(
                        "android/os/Parcel", "(J;)V", &[JValue::Long(0)]).unwrap();
                    
                    that_env.set_rust_field(data1, "mNativePtr", data.clone());
                    that_env.set_rust_field(reply1, "mNativePtr", reply.clone());

                    let re = that_env.call_method(obj, "onTransact", "(I;Landroid/os/Parcel;Landroid/os/Parcel;I;)Z", &[
                        JValue::Int(code),
                        JValue::Object(data1),
                        JValue::Object(reply1),
                        JValue::Int(flags)
                    ]);
                    if let Ok(v) = re{
                        if let JValue::Bool(b) = v{
                            return b != 0;
                        } else{
                            return false;
                        }
                    } else {
                        return false;
                    }
                });

                if let Some(v) = re{
                    return v as jboolean;
                }
            }
        }
    }
    return 0;
}

#[no_mangle]
pub fn pingBinder(env:JNIEnv, proxy:JObject) -> jboolean{
    if let Ok(p) = env.get_rust_field::<_,_,Arc<BinderProxy>>(proxy, "mNativePtr"){
        let globref = p.thatObject.clone();

        let re = p.thatVm.run(move|env|{
            let obj = globref.as_obj();
            let re = env.call_method(obj, "pingBinder", "()Z", &[]);
            if let Ok(r) = re{
                if let JValue::Bool(v) = r{
                    return v
                }
            }
            return 0;
        });

        if let Some(r) = re{
            return r;
        }
    }
    return 0;
}

#[no_mangle]
pub fn getInterfaceDescriptor(env:JNIEnv, proxy:JObject) -> jstring{
    if let Ok(p) = env.get_rust_field::<_,_,Arc<BinderProxy>>(proxy, "mNativePtr"){
        let globref = p.thatObject.clone();

        let re = p.thatVm.run(move|env|{
            let obj = globref.as_obj();
            let re = env.call_method(obj, "getInterfaceDescriptor", "()Ljava/lang/String", &[]);
            if let Ok(r) = re{
                return String::from(env.get_string(r.l().unwrap().into()).unwrap())
            }
            return "".to_string();
        });

        if let Some(r) = re{
            if (!r.is_empty()){
                if let Ok(s) = env.new_string(r){
                    return s.into_inner();
                }
            }
        }
    }
    return 0 as jstring;
}

#[no_mangle]
pub fn linkToDeath(env:JNIEnv, proxy:JObject, recipient:JObject, flags:jint){

}

#[no_mangle]
pub fn unlinkToDeath(env:JNIEnv, proxy:JObject, recipient:JObject, flags:jint) -> jboolean{
    return 0;
}