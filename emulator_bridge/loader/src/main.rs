#![allow(warnings)]
#![feature(c_variadic)]
#![feature(generic_const_exprs)]

use std::collections::HashMap;
use std::io::Read;
use std::net::TcpStream;
use std::net::Shutdown;
use std::sync::Arc;
use std::sync::mpsc::Sender;
use std::sync::mpsc::{channel, Receiver};
use std::mem::size_of;
use std::io::Write;
use std::os::raw::c_char;
use std::ffi::CStr;

use va_list::VaList;
use paste::paste;
use libloading::Library;
use crossbeam::channel::unbounded;
use parking_lot::Mutex;
use lock_api::*;


type jboolean = u8;
type jchar = u16;
type jshort = i16;
type jfloat = f32;
type jdouble = f64;
type jint = i32;
type jsize = i32;
type jbyte = i8;
type jlong = i64;

type jobject = usize;
type jclass = jobject;
type jthrowable = jobject;
type jstring = jobject;
type jarray = jobject;
type jbooleanArray = jobject;
type jbyteArray = jobject;
type jcharArray = jobject;
type jshortArray = jobject;
type jintArray = jobject;
type jlongArray = jobject;
type jfloatArray = jobject;
type jdoubleArray = jobject;
type jobjectArray = jobject;

type jweak = jobject;

#[repr(C)]
union jvalue {
    z:jboolean,
    b:jbyte,
    c:jchar,
    s:jshort,
    i:jint,
    j:jlong,
    f:jfloat,
    d:jdouble,
    l:jobject
}

type jfieldID = usize;
type jmethodID = usize;

#[repr(i32)]
enum jobjectRefType{
    JNIInvalidRefType    = 0,
    JNILocalRefType      = 1,
    JNIGlobalRefType     = 2,
    JNIWeakGlobalRefType = 3
}

#[repr(C)]
struct JNINativeMethod{
    name:*const c_char,
    signature:*const c_char,
    fnptr:*const ()
}

trait inner_tobytes{
    fn inner_tobytes(&self) -> Option<&[u8]>;
}

macro_rules! create_default {
    (struct $struct_ident:ident {
        $(
            $( $field:ident : unsafe extern "C" fn ($env:ident:$env_t:ty $(, $($param:ident:$param_t:ty),* )?  ) $(-> $return_t:ty)? )?
            $(; $field1:ident : unsafe extern "C" fn ($env1:ident:$env1_t:ty $(, $($param1:ident:$param1_t:ty),* )?  , ... ) $(-> $return1_t:ty)? )?
            $(=> $field2:ident : unsafe extern "C" fn ($env2:ident:$env2_t:ty $(, $($param2:ident:$param2_t:ty),* )? ) $(-> $return2_t:ty)? )? ,
        )*
    }) => {
        struct $struct_ident {
            reserved:[usize;4],
            $(
                $($field2 : unsafe extern "C" fn($env2:$env2_t $(, $($param2:$param2_t),*)?) $(-> $return2_t)? , )?
                $($field : unsafe extern "C" fn($env:$env_t $(, $($param:$param_t),* )?) $(-> $return_t)? , )?
                $($field1 : unsafe extern "C" fn($env1:$env1_t $(, $($param1:$param1_t),*)? , ... ) $(-> $return1_t)? ,)?
            )*
        }

        $(
            $(unsafe extern "C" fn $field($env:$env_t $(, $($param:$param_t),*)?) $(-> $return_t)?{
                //let sender = unsafe{(sender_ptr as *const Sender<Package>).as_ref().unwrap().clone()};
                //let reciever = unsafe{(recv_ptr as *const crossbeam::channel::Receiver<i64>).as_ref().unwrap().clone()};
                let mut stream = unsafe{(stream_ptr as *mut Mutex<TcpStream>).as_ref().unwrap().lock()};
                unsafe{
                    stream.write(&std::mem::transmute::<_, [u8;size_of::<Package>()]>(Package::$field{
                        $($($param),*)?
                    }))
                };
                //let re = reciever.recv().unwrap();
                $(
                    let mut b = [0u8;size_of::<$return_t>()];
                    stream.read(&mut b);
                    return unsafe{std::mem::transmute_copy::<_,$return_t>(&b)};
                )?
            }
            
            )?
        )*

        impl Default for JNINativeInterface_{
            fn default() -> JNINativeInterface_{
                JNINativeInterface_{
                    reserved:[0usize;4],
                    $(
                        $($field2:$field2 ,)?
                        $($field:$field ,)?
                        $($field1:$field1 ,)?
                    )*
                }
            }
        }

        #[repr(u8)]
        enum Package{
            LoadFailed(String),
            $(
                $( $field{$($($param:$param_t),*)?} ,)?
                //$( $field2{$($($param2:$param2_t),*)?} ,)?
            )*
        }
    };
}

#[repr(C)]
create_default!{struct JNINativeInterface_{
    GetVersion:unsafe extern "C" fn(env:*const JNIEnv) -> jint,
    => DefineClass:unsafe extern "C" fn(env:*const JNIEnv, name:*const c_char, loader:jobject, buf:*const jbyte, len:jsize) -> jclass,
    => FindClass:unsafe extern "C" fn(env:*const JNIEnv, name:*const c_char) -> jclass,
    FromReflectedMethod:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject) -> jmethodID,
    FromReflectedField:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject) -> jfieldID,
    ToReflectedMethod:unsafe extern "C" fn(env:*const JNIEnv, class:jclass, methodID:jmethodID, isStatic:jboolean) -> jobject,
    GetSuperclass:unsafe extern "C" fn(env:*const JNIEnv, class:jclass)  -> jclass,
    IsAssignableFrom:unsafe extern "C" fn(env:*const JNIEnv, sub:jclass, sup:jclass) -> jboolean,
    ToReflectedField:unsafe extern "C" fn(env:*const JNIEnv, class:jclass, fieldID:jfieldID, isStatic:jboolean) -> jobject,

    Throw:unsafe extern "C" fn(env:*const JNIEnv, obj:jthrowable) -> jint,
    => ThrowNew:unsafe extern "C" fn(env:*const JNIEnv, class:jclass, msg:*const c_char) -> jint,
    ExceptionOccurred:unsafe extern "C" fn(env:*const JNIEnv) -> jthrowable,
    ExceptionDescribe:unsafe extern "C" fn(env:*const JNIEnv),
    ExceptionClear:unsafe extern "C" fn(env:*const JNIEnv),
    => FatalError:unsafe extern "C" fn(env:*const JNIEnv, msg:*const c_char),
    PushLocalFrame:unsafe extern "C" fn(env:*const JNIEnv, capacity:jint) -> jint,
    PopLocalFrame:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject) -> jobject,

    NewGlobalRef:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject) -> jobject,
    DeleteGlobalRef:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject),
    DeleteLocalRef:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject),
    IsSameObject:unsafe extern "C" fn(env:*const JNIEnv, obj1:jobject, obj2:jobject) -> jboolean,
    NewLocalRef:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject) -> jobject,
    EnsureLocalCapacity:unsafe extern "C" fn(env:*const JNIEnv, capacity:jint) -> jint,

    => AllocObject:unsafe extern "C" fn(env:*const JNIEnv, class:jclass) -> jobject,
    ; NewObject:unsafe extern "C" fn(env:*const JNIEnv,class: jclass,methodID: jmethodID, ...) -> jobject,
    => NewObjectV:unsafe extern "C" fn(env:*const JNIEnv,class: jclass, methodID:jmethodID, args:va_list::VaList) -> jobject,
    => NewObjectA:unsafe extern "C" fn(env:*const JNIEnv,class: jclass, methodID:jmethodID, args:*const jvalue) -> jobject,

    GetObjectClass:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject) -> jclass,
    IsInstanceOf:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass) -> jboolean,

    => GetMethodID:unsafe extern "C" fn(env:*const JNIEnv, class:jclass, name:*const c_char, sig:*const c_char) -> jmethodID,

    ; CallObjectMethod:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID,...) -> jobject,
    => CallObjectMethodV:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID, args:va_list::VaList) -> jobject,
    => CallObjectMethodA:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID, args:*const jvalue) -> jobject,

    ; CallBooleanMethod:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID,...) -> jboolean,
    => CallBooleanMethodV:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID, args:va_list::VaList) -> jboolean,
    => CallBooleanMethodA:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID, args:*const jvalue) -> jboolean,

    ; CallByteMethod:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID,...) -> jbyte,
    => CallByteMethodV:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID, args:va_list::VaList) -> jbyte,
    => CallByteMethodA:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID, args:*const jvalue) -> jbyte,

    ; CallCharMethod:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID,...) -> jchar,
    => CallCharMethodV:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID, args:va_list::VaList) -> jchar,
    => CallCharMethodA:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID, args:*const jvalue) -> jchar,

    ; CallShortMethod:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID,...) -> jshort,
    => CallShortMethodV:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID, args:va_list::VaList) -> jshort,
    => CallShortMethodA:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID, args:*const jvalue) -> jshort,

    ; CallIntMethod:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID,...) -> jint,
    => CallIntMethodV:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID, args:va_list::VaList) -> jint,
    => CallIntMethodA:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID, args:*const jvalue) -> jint,

    ; CallLongMethod:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID,...) -> jlong,
    => CallLongMethodV:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID, args:va_list::VaList) -> jlong,
    => CallLongMethodA:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID, args:*const jvalue) -> jlong,

    ; CallFloatMethod:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID,...) -> jfloat,
    => CallFloatMethodV:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID, args:va_list::VaList) -> jfloat,
    => CallFloatMethodA:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID, args:*const jvalue) -> jfloat,

    ; CallDoubleMethod:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID,...) -> jdouble,
    => CallDoubleMethodV:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID, args:va_list::VaList) -> jdouble,
    => CallDoubleMethodA:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID, args:*const jvalue) -> jdouble,

    ; CallVoidMethod:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID,...),
    => CallVoidMethodV:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID, args:va_list::VaList),
    => CallVoidMethodA:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, methodID:jmethodID, args:*const jvalue),

    ; CallNonvirtualObjectMethod:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID,...) -> jobject,
    => CallNonvirtualObjectMethodV:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID, args:va_list::VaList) -> jobject,
    => CallNonvirtualObjectMethodA:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID, args:*const jvalue) -> jobject,

    ; CallNonvirtualBooleanMethod:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID,...) -> jboolean,
    => CallNonvirtualBooleanMethodV:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID, args:va_list::VaList) -> jboolean,
    => CallNonvirtualBooleanMethodA:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID, args:*const jvalue) -> jboolean,

    ; CallNonvirtualByteMethod:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID,...) -> jbyte,
    => CallNonvirtualByteMethodV:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID, args:va_list::VaList) -> jbyte,
    => CallNonvirtualByteMethodA:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID, args:*const jvalue) -> jbyte,

    ; CallNonvirtualCharMethod:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID,...) -> jchar,
    => CallNonvirtualCharMethodV:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID, args:va_list::VaList) -> jchar,
    => CallNonvirtualCharMethodA:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID, args:*const jvalue) -> jchar,

    ; CallNonvirtualShortMethod:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID,...) -> jshort,
    => CallNonvirtualShortMethodV:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID, args:va_list::VaList) -> jshort,
    => CallNonvirtualShortMethodA:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID, args:*const jvalue) -> jshort,

    ; CallNonvirtualIntMethod:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID,...) -> jint,
    => CallNonvirtualIntMethodV:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID, args:va_list::VaList) -> jint,
    => CallNonvirtualIntMethodA:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID, args:*const jvalue) -> jint,

    ; CallNonvirtualLongMethod:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID,...) -> jlong,
    => CallNonvirtualLongMethodV:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID, args:va_list::VaList) -> jlong,
    => CallNonvirtualLongMethodA:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID, args:*const jvalue) -> jlong,

    ; CallNonvirtualFloatMethod:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID,...) -> jfloat,
    => CallNonvirtualFloatMethodV:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID, args:va_list::VaList) -> jfloat,
    => CallNonvirtualFloatMethodA:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID, args:*const jvalue) -> jfloat,

    ; CallNonvirtualDoubleMethod:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID,...) -> jdouble,
    => CallNonvirtualDoubleMethodV:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID, args:va_list::VaList) -> jdouble,
    => CallNonvirtualDoubleMethodA:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID, args:*const jvalue) -> jdouble,

    ; CallNonvirtualVoidMethod:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID,...),
    => CallNonvirtualVoidMethodV:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID, args:va_list::VaList),
    => CallNonvirtualVoidMethodA:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID, args:*const jvalue),

    => GetFieldID:unsafe extern "C" fn(env:*const JNIEnv, class:jclass, name:*const c_char, sig:*const c_char) -> jfieldID,

    GetObjectField:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, fieldID:jfieldID) -> jobject,
    GetBooleanField:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, fieldID:jfieldID) -> jboolean,
    GetByteField:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, fieldID:jfieldID) -> jbyte,
    GetCharField:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, fieldID:jfieldID) -> jchar,
    GetShortField:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, fieldID:jfieldID) -> jshort,
    GetIntField:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, fieldID:jfieldID) -> jint,
    GetLongField:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, fieldID:jfieldID) -> jlong,
    GetFloatField:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, fieldID:jfieldID) -> jfloat,
    GetDoubleField:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, fieldID:jfieldID) -> jdouble,

    SetObjectField:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, fieldID:jfieldID, val:jobject),
    SetBooleanField:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, fieldID:jfieldID, val:jboolean),
    SetByteField:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, fieldID:jfieldID, val:jbyte),
    SetCharField:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, fieldID:jfieldID, val:jchar),
    SetShortField:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, fieldID:jfieldID, val:jshort),
    SetIntField:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, fieldID:jfieldID, val:jint),
    SetLongField:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, fieldID:jfieldID, val:jlong),
    SetFloatField:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, fieldID:jfieldID, val:jfloat),
    SetDoubleField:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject, fieldID:jfieldID, val:jdouble),

    => GetStaticMethodID:unsafe extern "C" fn(env:*const JNIEnv, class:jclass, name:*const c_char, sig:*const c_char) -> jmethodID,

    ; CallStaticObjectMethod:unsafe extern "C" fn(env:*const JNIEnv,class: jclass,methodID: jmethodID,...) -> jobject,
    => CallStaticObjectMethodV:unsafe extern "C" fn(env:*const JNIEnv,class: jclass, methodID:jmethodID, args:va_list::VaList) -> jobject,
    => CallStaticObjectMethodA:unsafe extern "C" fn(env:*const JNIEnv,class: jclass, methodID:jmethodID, args:*const jvalue) -> jobject,

    ; CallStaticBooleanMethod:unsafe extern "C" fn(env:*const JNIEnv,class: jclass,methodID: jmethodID,...) -> jboolean,
    => CallStaticBooleanMethodV:unsafe extern "C" fn(env:*const JNIEnv,class: jclass, methodID:jmethodID, args:va_list::VaList) -> jboolean,
    => CallStaticBooleanMethodA:unsafe extern "C" fn(env:*const JNIEnv,class: jclass, methodID:jmethodID, args:*const jvalue) -> jboolean,

    ; CallStaticByteMethod:unsafe extern "C" fn(env:*const JNIEnv,class: jclass,methodID: jmethodID,...) -> jbyte,
    => CallStaticByteMethodV:unsafe extern "C" fn(env:*const JNIEnv,class: jclass, methodID:jmethodID, args:va_list::VaList) -> jbyte,
    => CallStaticByteMethodA:unsafe extern "C" fn(env:*const JNIEnv,class: jclass, methodID:jmethodID, args:*const jvalue) -> jbyte,

    ; CallStaticCharMethod:unsafe extern "C" fn(env:*const JNIEnv,class: jclass,methodID: jmethodID,...) -> jchar,
    => CallStaticCharMethodV:unsafe extern "C" fn(env:*const JNIEnv,class: jclass, methodID:jmethodID, args:va_list::VaList) -> jchar,
    => CallStaticCharMethodA:unsafe extern "C" fn(env:*const JNIEnv,class: jclass, methodID:jmethodID, args:*const jvalue) -> jchar,

    ; CallStaticShortMethod:unsafe extern "C" fn(env:*const JNIEnv,class: jclass,methodID: jmethodID,...) -> jshort,
    => CallStaticShortMethodV:unsafe extern "C" fn(env:*const JNIEnv,class: jclass, methodID:jmethodID, args:va_list::VaList) -> jshort,
    => CallStaticShortMethodA:unsafe extern "C" fn(env:*const JNIEnv,class: jclass, methodID:jmethodID, args:*const jvalue) -> jshort,

    ; CallStaticIntMethod:unsafe extern "C" fn(env:*const JNIEnv,class: jclass,methodID: jmethodID,...) -> jint,
    => CallStaticIntMethodV:unsafe extern "C" fn(env:*const JNIEnv,class: jclass, methodID:jmethodID, args:va_list::VaList) -> jint,
    => CallStaticIntMethodA:unsafe extern "C" fn(env:*const JNIEnv,class: jclass, methodID:jmethodID, args:*const jvalue) -> jint,

    ; CallStaticLongMethod:unsafe extern "C" fn(env:*const JNIEnv,class: jclass,methodID: jmethodID,...) -> jlong,
    => CallStaticLongMethodV:unsafe extern "C" fn(env:*const JNIEnv,class: jclass, methodID:jmethodID, args:va_list::VaList) -> jlong,
    => CallStaticLongMethodA:unsafe extern "C" fn(env:*const JNIEnv,class: jclass, methodID:jmethodID, args:*const jvalue) -> jlong,

    ; CallStaticFloatMethod:unsafe extern "C" fn(env:*const JNIEnv,class: jclass,methodID: jmethodID,...) -> jfloat,
    => CallStaticFloatMethodV:unsafe extern "C" fn(env:*const JNIEnv,class: jclass, methodID:jmethodID, args:va_list::VaList) -> jfloat,
    => CallStaticFloatMethodA:unsafe extern "C" fn(env:*const JNIEnv,class: jclass, methodID:jmethodID, args:*const jvalue) -> jfloat,

    ; CallStaticDoubleMethod:unsafe extern "C" fn(env:*const JNIEnv,class: jclass,methodID: jmethodID,...) -> jdouble,
    => CallStaticDoubleMethodV:unsafe extern "C" fn(env:*const JNIEnv,class: jclass, methodID:jmethodID, args:va_list::VaList) -> jdouble,
    => CallStaticDoubleMethodA:unsafe extern "C" fn(env:*const JNIEnv,class: jclass, methodID:jmethodID, args:*const jvalue) -> jdouble,

    ; CallStaticVoidMethod:unsafe extern "C" fn(env:*const JNIEnv,class: jclass,methodID: jmethodID,...),
    => CallStaticVoidMethodV:unsafe extern "C" fn(env:*const JNIEnv,class: jclass, methodID:jmethodID, args:va_list::VaList),
    => CallStaticVoidMethodA:unsafe extern "C" fn(env:*const JNIEnv,class: jclass, methodID:jmethodID, args:*const jvalue),

    => GetStaticFieldID:unsafe extern "C" fn(env:*const JNIEnv, class:jclass, name:*const c_char, sig:*const c_char) -> jfieldID,
    GetStaticObjectField:unsafe extern "C" fn(env:*const JNIEnv, class:jclass, fieldID:jfieldID) -> jobject,
    GetStaticBooleanField:unsafe extern "C" fn(env:*const JNIEnv, class:jclass, fieldID:jfieldID) -> jboolean,
    GetStaticByteField:unsafe extern "C" fn(env:*const JNIEnv, class:jclass, fieldID:jfieldID) -> jbyte,
    GetStaticCharField:unsafe extern "C" fn(env:*const JNIEnv, class:jclass, fieldID:jfieldID) -> jchar,
    GetStaticShortField:unsafe extern "C" fn(env:*const JNIEnv, class:jclass, fieldID:jfieldID) -> jshort,
    GetStaticIntField:unsafe extern "C" fn(env:*const JNIEnv, class:jclass, fieldID:jfieldID) -> jint,
    GetStaticLongField:unsafe extern "C" fn(env:*const JNIEnv, class:jclass, fieldID:jfieldID) -> jlong,
    GetStaticFloatField:unsafe extern "C" fn(env:*const JNIEnv, class:jclass, fieldID:jfieldID) -> jfloat,
    GetStaticDoubleField:unsafe extern "C" fn(env:*const JNIEnv, class:jclass, fieldID:jfieldID) -> jdouble,

    SetStaticObjectField:unsafe extern "C" fn(env:*const JNIEnv, class:jclass, fieldID:jfieldID, val:jobject),
    SetStaticBooleanField:unsafe extern "C" fn(env:*const JNIEnv, class:jclass, fieldID:jfieldID, val:jboolean),
    SetStaticByteField:unsafe extern "C" fn(env:*const JNIEnv, class:jclass, fieldID:jfieldID, val:jbyte),
    SetStaticCharField:unsafe extern "C" fn(env:*const JNIEnv, class:jclass, fieldID:jfieldID, val:jchar),
    SetStaticShortField:unsafe extern "C" fn(env:*const JNIEnv, class:jclass, fieldID:jfieldID, val:jshort),
    SetStaticIntField:unsafe extern "C" fn(env:*const JNIEnv, class:jclass, fieldID:jfieldID, val:jint),
    SetStaticLongField:unsafe extern "C" fn(env:*const JNIEnv, class:jclass, fieldID:jfieldID, val:jlong),
    SetStaticFloatField:unsafe extern "C" fn(env:*const JNIEnv, class:jclass, fieldID:jfieldID, val:jfloat),
    SetStaticDoubleField:unsafe extern "C" fn(env:*const JNIEnv, class:jclass, fieldID:jfieldID, val:jdouble),

    => NewString:unsafe extern "C" fn(env:*const JNIEnv, unicode:*const jchar, len:jsize) -> jstring,
    GetStringLength:unsafe extern "C" fn(env:*const JNIEnv, str:jstring) -> jsize,
    => GetStringChars:unsafe extern "C" fn(env:*const JNIEnv, str:jstring, isCopy:*mut jboolean) -> *const jchar,
    => ReleaseStringChars:unsafe extern "C" fn(env:*const JNIEnv, str:jstring, chars:*const jchar),

    => NewStringUTF:unsafe extern "C" fn(env:*const JNIEnv, utf:*const jchar) -> jstring,
    GetStringUTFLength:unsafe extern "C" fn(env:*const JNIEnv, str:jstring) -> jsize,
    => GetStringUTFChars:unsafe extern "C" fn(env:*const JNIEnv, str:jstring, isCopy:*mut jboolean) -> *const jchar,
    => ReleaseStringUTFChars:unsafe extern "C" fn(env:*const JNIEnv, str:jstring, chars:*const c_char),


    GetArrayLength:unsafe extern "C" fn(env:*const JNIEnv, array:jarray ) -> jsize,

    NewObjectArray:unsafe extern "C" fn(env:*const JNIEnv, len:jsize, class:jclass, init:jobject) -> jobjectArray,
    GetObjectArrayElement:unsafe extern "C" fn(env:*const JNIEnv, array:jobjectArray, index:jsize),
    SetObjectArrayElement:unsafe extern "C" fn(env:*const JNIEnv, array:jobjectArray, index:jsize, val:jobject),

    NewBooleanArray:unsafe extern "C" fn(env:*const JNIEnv, len:jsize) -> jbooleanArray,
    NewByteArray:unsafe extern "C" fn(env:*const JNIEnv, len:jsize) -> jbyteArray,
    NewCharArray:unsafe extern "C" fn(env:*const JNIEnv, len:jsize) -> jcharArray,
    NewShortArray:unsafe extern "C" fn(env:*const JNIEnv, len:jsize) -> jshortArray,
    NewIntArray:unsafe extern "C" fn(env:*const JNIEnv, len:jsize) -> jintArray,
    NewLongArray:unsafe extern "C" fn(env:*const JNIEnv, len:jsize) -> jlongArray,
    NewFloatArray:unsafe extern "C" fn(env:*const JNIEnv, len:jsize) -> jfloatArray,
    NewDoubleArray:unsafe extern "C" fn(env:*const JNIEnv, len:jsize) -> jdoubleArray,

    => GetBooleanArrayElements:unsafe extern "C" fn(env:*const JNIEnv, array:jbooleanArray, isCopy:*mut jboolean) -> jboolean,
    => GetByteArrayElements:unsafe extern "C" fn(env:*const JNIEnv, array:jbyteArray, isCopy:*mut jboolean) -> jbyte,
    => GetCharArrayElements:unsafe extern "C" fn(env:*const JNIEnv, array:jcharArray, isCopy:*mut jboolean) -> jchar,
    => GetShortArrayElements:unsafe extern "C" fn(env:*const JNIEnv, array:jshortArray, isCopy:*mut jboolean) -> jshort,
    => GetIntArrayElements:unsafe extern "C" fn(env:*const JNIEnv, array:jintArray, isCopy:*mut jboolean) -> jint,
    => GetLongArrayElements:unsafe extern "C" fn(env:*const JNIEnv, array:jlongArray, isCopy:*mut jboolean) -> jlong,
    => GetFloatArrayElements:unsafe extern "C" fn(env:*const JNIEnv, array:jfloatArray, isCopy:*mut jboolean) -> jfloat,
    => GetDoubleArrayElements:unsafe extern "C" fn(env:*const JNIEnv, array:jdoubleArray, isCopy:*mut jboolean) -> jdouble,

    => ReleaseBooleanArrayElements:unsafe extern "C" fn(env:*const JNIEnv, array:jbooleanArray, elems:*const jboolean, mode:jint),
    => ReleaseByteArrayElements:unsafe extern "C" fn(env:*const JNIEnv, array:jbyteArray, elems:*const jbyte, mode:jint),
    => ReleaseCharArrayElements:unsafe extern "C" fn(env:*const JNIEnv, array:jcharArray, elems:*const jchar, mode:jint),
    => ReleaseShortArrayElements:unsafe extern "C" fn(env:*const JNIEnv, array:jshortArray, elems:*const jshort, mode:jint),
    => ReleaseIntArrayElements:unsafe extern "C" fn(env:*const JNIEnv, array:jintArray, elems:*const jint, mode:jint),
    => ReleaseLongArrayElements:unsafe extern "C" fn(env:*const JNIEnv, array:jlongArray, elems:*const jlong, mode:jint),
    => ReleaseFloatArrayElements:unsafe extern "C" fn(env:*const JNIEnv, array:jfloatArray, elems:*const jfloat, mode:jint),
    => ReleaseDoubleArrayElements:unsafe extern "C" fn(env:*const JNIEnv, array:jdoubleArray, elems:*const jdouble, mode:jint),

    => GetBooleanArrayRegion:unsafe extern "C" fn(env:*const JNIEnv, array:jbooleanArray, start:jsize, len:jsize, buf:*const jboolean),
    => GetByteArrayRegion:unsafe extern "C" fn(env:*const JNIEnv, array:jbyteArray, start:jsize, len:jsize, buf:*const jbyte),
    => GetCharArrayRegion:unsafe extern "C" fn(env:*const JNIEnv, array:jcharArray, start:jsize, len:jsize, buf:*const jchar),
    => GetShortArrayRegion:unsafe extern "C" fn(env:*const JNIEnv, array:jshortArray, start:jsize, len:jsize, buf:*const jshort),
    => GetIntArrayRegion:unsafe extern "C" fn(env:*const JNIEnv, array:jintArray, start:jsize, len:jsize, buf:*const jint),
    => GetLongArrayRegion:unsafe extern "C" fn(env:*const JNIEnv, array:jlongArray, start:jsize, len:jsize, buf:*const jlong),
    => GetFloatArrayRegion:unsafe extern "C" fn(env:*const JNIEnv, array:jfloatArray, start:jsize, len:jsize, buf:*const jfloat),
    => GetDoubleArrayRegion:unsafe extern "C" fn(env:*const JNIEnv, array:jdoubleArray, start:jsize, len:jsize, buf:*const jdouble),

    => SetBooleanArrayRegion:unsafe extern "C" fn(env:*const JNIEnv, array:jbooleanArray, start:jsize, len:jsize, buf:*const jboolean),
    => SetByteArrayRegion:unsafe extern "C" fn(env:*const JNIEnv, array:jbyteArray, start:jsize, len:jsize, buf:*const jbyte),
    => SetCharArrayRegion:unsafe extern "C" fn(env:*const JNIEnv, array:jcharArray, start:jsize, len:jsize, buf:*const jchar),
    => SetShortArrayRegion:unsafe extern "C" fn(env:*const JNIEnv, array:jshortArray, start:jsize, len:jsize, buf:*const jshort),
    => SetIntArrayRegion:unsafe extern "C" fn(env:*const JNIEnv, array:jintArray, start:jsize, len:jsize, buf:*const jint),
    => SetLongArrayRegion:unsafe extern "C" fn(env:*const JNIEnv, array:jlongArray, start:jsize, len:jsize, buf:*const jlong),
    => SetFloatArrayRegion:unsafe extern "C" fn(env:*const JNIEnv, array:jfloatArray, start:jsize, len:jsize, buf:*const jfloat),
    => SetDoubleArrayRegion:unsafe extern "C" fn(env:*const JNIEnv, array:jdoubleArray, start:jsize, len:jsize, buf:*const jdouble),

    => RegisterNatives:unsafe extern "C" fn(env:*const JNIEnv, class:jclass, methods:*const JNINativeMethod, nMethods:jint ) -> jint,
    => UnregisterNatives:unsafe extern "C" fn(env:*const JNIEnv, class:jclass) -> jint,

    MonitorEnter:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject) -> jint,
    MonitorExit:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject) -> jint,

    => GetJavaVM:unsafe extern "C" fn(env:*const JNIEnv, vm:*const *const JavaVM) -> jint,

    => GetStringRegion:unsafe extern "C" fn(env:*const JNIEnv, str:jstring, start:jsize, len:jsize, chars:*const jchar),
    => GetStringUTFRegion:unsafe extern "C" fn(env:*const JNIEnv, str:jstring, start:jsize, len:jsize, chars:*const c_char),

    => GetPrimitiveArrayCritical:unsafe extern "C" fn(env:*const JNIEnv, array:jarray, isCopy:*mut jboolean) -> *const (),
    => ReleasePrimitiveArrayCritical:unsafe extern "C" fn(env:*const JNIEnv, array:jarray, carray:*const (), mode:jint),

    => GetStringCritical:unsafe extern "C" fn(env:*const JNIEnv, str:jstring, isCopy:*mut jboolean) -> *const jchar,
    => ReleaseStringCritical:unsafe extern "C" fn(env:*const JNIEnv, str:jstring, chars:*const jchar),

    NewWeakGlobalRef:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject) -> jweak,
    DeleteWeakGlobalRef:unsafe extern "C" fn(env:*const JNIEnv, reference:jweak),

    ExceptionCheck:unsafe extern "C" fn(env:*const JNIEnv) -> jboolean,

    => NewDirectByteBuffer:unsafe extern "C" fn(env:*const JNIEnv, address:*const (), len:jlong) -> jobject,
    => GetDirectBufferAddress:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject) -> *const (),
    GetDirectBufferCapacity:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject) -> jlong,

    /* New JNI 1.6 Features */

    GetObjectRefType:unsafe extern "C" fn(env:*const JNIEnv, obj:jobject) -> jobjectRefType,

    /* Module Features */

    GetModule:unsafe extern "C" fn(env:*const JNIEnv, class:jclass) -> jobject,
}}

macro_rules! repeat_types {
    (static $name:ident - $name1:ident : fn($($param_t:ty),* ,...) ->  = $b:ident) => {
        paste!{
            static [<$name Object $name1>]:unsafe extern "C" fn($($param_t),* ,...) -> jobject = $b::<jobject>;
            static [<$name Boolean $name1>]:unsafe extern "C" fn($($param_t),* ,...) -> jboolean = $b::<jboolean>;
            static [<$name Byte $name1>]:unsafe extern "C" fn($($param_t),* ,...) -> jbyte = $b::<jbyte>;
            static [<$name Char $name1>]:unsafe extern "C" fn($($param_t),* ,...) -> jchar = $b::<jchar>;
            static [<$name Short $name1>]:unsafe extern "C" fn($($param_t),* ,...) -> jshort = $b::<jshort>;
            static [<$name Int $name1>]:unsafe extern "C" fn($($param_t),* ,...) -> jint = $b::<jint>;
            static [<$name Long $name1>]:unsafe extern "C" fn($($param_t),* ,...) -> jlong = $b::<jlong>;
            static [<$name Float $name1>]:unsafe extern "C" fn($($param_t),* ,...) -> jfloat = $b::<jfloat>;
            static [<$name Double $name1>]:unsafe extern "C" fn($($param_t),* ,...) -> jdouble = $b::<jdouble>;
            static [<$name Void $name1>]:unsafe extern "C" fn($($param_t),* ,...) -> () = $b::<()>;
        }
    };
    (static $name:ident - $name1:ident : fn($($param_t:ty),*) ->  = $b:ident) => {
        paste!{
            static [<$name Object $name1>]:unsafe extern "C" fn($($param_t),*) -> jobject = $b::<jobject>;
            static [<$name Boolean $name1>]:unsafe extern "C" fn($($param_t),*) -> jboolean = $b::<jboolean>;
            static [<$name Byte $name1>]:unsafe extern "C" fn($($param_t),*) -> jbyte = $b::<jbyte>;
            static [<$name Char $name1>]:unsafe extern "C" fn($($param_t),*) -> jchar = $b::<jchar>;
            static [<$name Short $name1>]:unsafe extern "C" fn($($param_t),*) -> jshort = $b::<jshort>;
            static [<$name Int $name1>]:unsafe extern "C" fn($($param_t),*) -> jint = $b::<jint>;
            static [<$name Long $name1>]:unsafe extern "C" fn($($param_t),*) -> jlong = $b::<jlong>;
            static [<$name Float $name1>]:unsafe extern "C" fn($($param_t),*) -> jfloat = $b::<jfloat>;
            static [<$name Double $name1>]:unsafe extern "C" fn($($param_t),*) -> jdouble = $b::<jdouble>;
            static [<$name Void $name1>]:unsafe extern "C" fn($($param_t),*) -> () = $b::<()>;
        }
    };

    (static $name:ident - $name1:ident : fn($($param_t:ty),* [] , $($param2_t:ty),*) ->  = $b:ident) => {
        paste!{
            static [<$name Boolean $name1>]:unsafe extern "C" fn($($param_t),* , jbooleanArray, $($param2_t),*) -> jboolean = $b::<jbooleanArray, jboolean>;
            static [<$name Byte $name1>]:unsafe extern "C" fn($($param_t),* , jbyteArray, $($param2_t),*) -> jbyte = $b::<jbyteArray, jbyte>;
            static [<$name Char $name1>]:unsafe extern "C" fn($($param_t),* , jcharArray, $($param2_t),*) -> jchar = $b::<jcharArray, jchar>;
            static [<$name Short $name1>]:unsafe extern "C" fn($($param_t),* , jshortArray, $($param2_t),*) -> jshort = $b::<jshortArray, jshort>;
            static [<$name Int $name1>]:unsafe extern "C" fn($($param_t),* , jintArray, $($param2_t),*) -> jint = $b::<jintArray, jint>;
            static [<$name Long $name1>]:unsafe extern "C" fn($($param_t),* , jlongArray, $($param2_t),*) -> jlong = $b::<jlongArray, jlong>;
            static [<$name Float $name1>]:unsafe extern "C" fn($($param_t),* , jfloatArray, $($param2_t),*) -> jfloat = $b::<jfloatArray, jfloat>;
            static [<$name Double $name1>]:unsafe extern "C" fn($($param_t),* , jdoubleArray, $($param2_t),*) -> jdouble = $b::<jdoubleArray, jdouble>;
        }
    };

    (static $name:ident - $name1:ident : fn($($param_t:ty),* [] , @, $($param1_t:ty),*) = $b:ident) => {
        paste!{
            static [<$name Boolean $name1>]:unsafe extern "C" fn($($param_t),* , jbooleanArray , *const jboolean, $($param1_t),*) = $b::<jbooleanArray, jboolean>;
            static [<$name Byte $name1>]:unsafe extern "C" fn($($param_t),* , jbyteArray , *const jbyte, $($param1_t),*)  = $b::<jbyteArray, jbyte>;
            static [<$name Char $name1>]:unsafe extern "C" fn($($param_t),* , jcharArray , *const jchar, $($param1_t),*)  = $b::<jcharArray, jchar>;
            static [<$name Short $name1>]:unsafe extern "C" fn($($param_t),* , jshortArray , *const jshort, $($param1_t),*)  = $b::<jshortArray, jshort>;
            static [<$name Int $name1>]:unsafe extern "C" fn($($param_t),* , jintArray , *const jint, $($param1_t),*)  = $b::<jintArray, jint>;
            static [<$name Long $name1>]:unsafe extern "C" fn($($param_t),* , jlongArray , *const jlong, $($param1_t),*)  = $b::<jlongArray, jlong>;
            static [<$name Float $name1>]:unsafe extern "C" fn($($param_t),* , jfloatArray , *const jfloat, $($param1_t),*)  = $b::<jfloatArray, jfloat>;
            static [<$name Double $name1>]:unsafe extern "C" fn($($param_t),* , jdoubleArray , *const jdouble, $($param1_t),*)  = $b::<jdoubleArray, jdouble>;
        }
    };
    (static $name:ident - $name1:ident : fn($($param_t:ty),* [] , $($param1_t:ty),*, @) = $b:ident) => {
        paste!{
            static [<$name Boolean $name1>]:unsafe extern "C" fn($($param_t),* , jbooleanArray , $($param1_t),* , *const jboolean) = $b::<jbooleanArray, jboolean>;
            static [<$name Byte $name1>]:unsafe extern "C" fn($($param_t),* , jbyteArray, $($param1_t),* , *const jbyte) = $b::<jbyteArray, jbyte>;
            static [<$name Char $name1>]:unsafe extern "C" fn($($param_t),* , jcharArray, $($param1_t),* , *const jchar) = $b::<jcharArray, jchar>;
            static [<$name Short $name1>]:unsafe extern "C" fn($($param_t),* , jshortArray, $($param1_t),* , *const jshort) = $b::<jshortArray, jshort>;
            static [<$name Int $name1>]:unsafe extern "C" fn($($param_t),* , jintArray, $($param1_t),* , *const jint) = $b::<jintArray, jint>;
            static [<$name Long $name1>]:unsafe extern "C" fn($($param_t),* , jlongArray, $($param1_t),* , *const jlong)  = $b::<jlongArray, jlong>;
            static [<$name Float $name1>]:unsafe extern "C" fn($($param_t),* , jfloatArray, $($param1_t),* , *const jfloat)  = $b::<jfloatArray, jfloat>;
            static [<$name Double $name1>]:unsafe extern "C" fn($($param_t),* , jdoubleArray, $($param1_t),* , *const jdouble)  = $b::<jdoubleArray, jdouble>;
        }
    };
}

unsafe extern "C" fn DefineClass(env:*const JNIEnv, name:*const c_char, loader:jobject, buf:*const jbyte, len:jsize) -> jclass{
    let b = [0u8;size_of::<jclass>()];
    return std::mem::transmute_copy(&b);
}

unsafe extern "C" fn FindClass(env:*const JNIEnv, name:*const c_char) -> jclass{
    let b = [0u8;size_of::<jclass>()];
    return std::mem::transmute_copy(&b);

}
unsafe extern "C" fn ThrowNew(env:*const JNIEnv, class:jclass, msg:*const c_char) -> jint{
    return JNI_OK;
}

unsafe extern "C" fn FatalError(env:*const JNIEnv, msg:*const c_char){

}

unsafe extern "C" fn AllocObject(env:*const JNIEnv, class:jclass) -> jobject{
    let b = [0u8;size_of::<jobject>()];
    return std::mem::transmute_copy(&b);
}



unsafe extern "C" fn NewObject(env:*const JNIEnv,class: jclass, methodID: jmethodID, mut args:...) -> jobject{
    let b = [0u8;size_of::<jobject>()];
    return std::mem::transmute_copy(&b);

}   

unsafe extern "C" fn NewObjectV(env:*const JNIEnv,class: jclass, methodID:jmethodID, args:va_list::VaList) -> jobject{
    let b = [0u8;size_of::<jobject>()];
    return std::mem::transmute_copy(&b);
}

unsafe extern "C" fn NewObjectA(env:*const JNIEnv,class: jclass, methodID:jmethodID, args:*const jvalue) -> jobject{
    let b = [0u8;size_of::<jobject>()];
    return std::mem::transmute_copy(&b);
}

unsafe extern "C" fn GetMethodID(env:*const JNIEnv, class:jclass, name:*const c_char, sig:*const c_char) -> jmethodID{
    let b = [0u8;size_of::<jmethodID>()];
    return std::mem::transmute_copy(&b);
}

unsafe extern "C" fn CallMethod<T>(env:*const JNIEnv, obj:jobject, methodID:jmethodID, mut args:...) -> T
where [();size_of::<T>()]:, T:Sized
{
    let signature = method_signature_table.lock().get(&methodID);

    let b = [0u8;size_of::<T>()];
    return std::mem::transmute_copy(&b);
}
unsafe extern "C" fn CallMethodV<T>(env:*const JNIEnv, obj:jobject, methodID:jmethodID, mut args:va_list::VaList) -> T
where [();size_of::<T>()]:, T:Sized
{
    let signature = method_signature_table.lock().get(&methodID);

    let b = [0u8;size_of::<T>()];
    return std::mem::transmute_copy(&b);
}
unsafe extern "C" fn CallMethodA<T>(env:*const JNIEnv, obj:jobject, methodID:jmethodID, mut args:*const jvalue) -> T
where [();size_of::<T>()]:, T:Sized
{
    let signature = method_signature_table.lock().get(&methodID);

    let b = [0u8;size_of::<T>()];
    return std::mem::transmute_copy(&b);
}

unsafe extern "C" fn CallNonvirtualMethod<T>(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID, mut args:...) -> T
where [();size_of::<T>()]:, T:Sized
{
    let signature = method_signature_table.lock().get(&methodID);

    let b = [0u8;size_of::<T>()];
    return std::mem::transmute_copy(&b);
}
unsafe extern "C" fn CallNonvirtualMethodV<T>(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID, mut args:va_list::VaList) -> T
where [();size_of::<T>()]:, T:Sized
{
    let signature = method_signature_table.lock().get(&methodID);

    let b = [0u8;size_of::<T>()];
    return std::mem::transmute_copy(&b);
}
unsafe extern "C" fn CallNonvirtualMethodA<T>(env:*const JNIEnv, obj:jobject, class:jclass, methodID:jmethodID, mut args:*const jvalue) -> T
where [();size_of::<T>()]:, T:Sized
{
    let signature = method_signature_table.lock().get(&methodID);

    let b = [0u8;size_of::<T>()];
    return std::mem::transmute_copy(&b);
}

unsafe extern "C" fn GetFieldID(env:*const JNIEnv, class:jclass, name:*const c_char, sig:*const c_char) -> jfieldID{
    let b = [0u8;size_of::<jfieldID>()];
    return std::mem::transmute_copy(&b);

}

unsafe extern "C" fn GetStaticMethodID(env:*const JNIEnv, class:jclass, name:*const c_char, sig:*const c_char) -> jmethodID{
    let b = [0u8;size_of::<jmethodID>()];
    return std::mem::transmute_copy(&b);
}

unsafe extern "C" fn CallStaticMethod<T>(env:*const JNIEnv,class: jclass,methodID: jmethodID, mut args:...) -> T
where [();size_of::<T>()]:, T:Sized
{
    let b = [0u8;size_of::<T>()];
    return std::mem::transmute_copy(&b);
}

unsafe extern "C" fn CallStaticMethodV<T>(env:*const JNIEnv,class: jclass, methodID:jmethodID, args:va_list::VaList) -> T
where [();size_of::<T>()]:, T:Sized
{
    let b = [0u8;size_of::<T>()];
    return std::mem::transmute_copy(&b);
}

unsafe extern "C" fn CallStaticMethodA<T>(env:*const JNIEnv,class: jclass, methodID:jmethodID, args:*const jvalue) -> T
where [();size_of::<T>()]:, T:Sized
{
    let b = [0u8;size_of::<T>()];
    return std::mem::transmute_copy(&b);
}

unsafe extern "C" fn GetStaticFieldID(env:*const JNIEnv, class:jclass, name:*const c_char, sig:*const c_char) -> jfieldID{
    let b = [0u8;size_of::<jfieldID>()];
    return std::mem::transmute_copy(&b);
}

unsafe extern "C" fn NewString(env:*const JNIEnv, unicode:*const jchar, len:jsize) -> jstring{
    let b = [0u8;size_of::<jstring>()];
    return std::mem::transmute_copy(&b);
}

unsafe extern "C" fn GetStringChars(env:*const JNIEnv, str:jstring, isCopy:*mut jboolean) -> *const jchar{
    return 0 as *const jchar;
}

unsafe extern "C" fn ReleaseStringChars(env:*const JNIEnv, str:jstring, chars:*const jchar){
}

unsafe extern "C" fn NewStringUTF(env:*const JNIEnv, utf:*const jchar) -> jstring{
    let b = [0u8;size_of::<jstring>()];
    return std::mem::transmute_copy(&b);
}

unsafe extern "C" fn GetStringUTFChars(env:*const JNIEnv, str:jstring, isCopy:*mut jboolean) -> *const jchar{
    return 0 as *const jchar;
}

unsafe extern "C" fn ReleaseStringUTFChars(env:*const JNIEnv, str:jstring, chars:*const c_char){
}

unsafe extern "C" fn GetArrayElements<A,T>(env:*const JNIEnv, array:A, isCopy:*mut jboolean) -> T
where [();size_of::<T>()]:, T:Sized
{
    let b = [0u8;size_of::<T>()];
    return std::mem::transmute_copy(&b);
}

unsafe extern "C" fn ReleaseArrayElements<A, T>(env:*const JNIEnv, array:A, elems:*const T, mode:jint)
where [();size_of::<T>()]:, T:Sized
{

}

unsafe extern "C" fn GetArrayRegion<A, T>(env:*const JNIEnv, array:A, start:jsize, len:jsize, buf:*const T)
where [();size_of::<T>()]:, T:Sized
{

}

unsafe extern "C" fn SetArrayRegion<A, T>(env:*const JNIEnv, array:A, start:jsize, len:jsize, buf:*const T)
where [();size_of::<T>()]:, T:Sized
{

}

unsafe extern "C" fn RegisterNatives(env:*const JNIEnv, class:jclass, methods:*const JNINativeMethod, nMethods:jint ) -> jint{
    return JNI_OK;
}
unsafe extern "C" fn UnregisterNatives(env:*const JNIEnv, class:jclass) -> jint{
    return JNI_OK;
}

unsafe extern "C" fn GetJavaVM(env:*const JNIEnv, vm:*const *const JavaVM) -> jint{
    return JNI_OK;
}

unsafe extern "C" fn GetStringRegion(env:*const JNIEnv, str:jstring, start:jsize, len:jsize, chars:*const jchar){

}
unsafe extern "C" fn GetStringUTFRegion(env:*const JNIEnv, str:jstring, start:jsize, len:jsize, chars:*const c_char){

}

unsafe extern "C" fn GetPrimitiveArrayCritical(env:*const JNIEnv, array:jarray, isCopy:*mut jboolean) -> *const (){
    return 0 as *const ();
}
unsafe extern "C" fn ReleasePrimitiveArrayCritical(env:*const JNIEnv, array:jarray, carray:*const (), mode:jint){

}

unsafe extern "C" fn GetStringCritical(env:*const JNIEnv, str:jstring, isCopy:*mut jboolean) -> *const jchar{
    return 0 as *const jchar;
}
unsafe extern "C" fn ReleaseStringCritical(env:*const JNIEnv, str:jstring, chars:*const jchar){

}

unsafe extern "C" fn NewDirectByteBuffer(env:*const JNIEnv, address:*const (), len:jlong) -> jobject{
    return 0;
}
unsafe extern "C" fn GetDirectBufferAddress(env:*const JNIEnv, obj:jobject) -> *const (){
    return 0 as *const ();
}

repeat_types!(static Call - Method: fn(*const JNIEnv, jobject, jmethodID,...)-> = CallMethod);
repeat_types!(static Call - MethodV: fn(*const JNIEnv, jobject, jmethodID, VaList)-> = CallMethodV);
repeat_types!(static Call - MethodA: fn(*const JNIEnv, jobject, jmethodID, *const jvalue)-> = CallMethodA);
repeat_types!(static CallNonvirtual - Method: fn(*const JNIEnv, jobject, jclass, jmethodID,...)-> = CallNonvirtualMethod);
repeat_types!(static CallNonvirtual - MethodV: fn(*const JNIEnv, jobject, jclass, jmethodID, VaList)-> = CallNonvirtualMethodV);
repeat_types!(static CallNonvirtual - MethodA: fn(*const JNIEnv, jobject, jclass, jmethodID, *const jvalue)-> = CallNonvirtualMethodA);
repeat_types!(static CallStatic - Method: fn(*const JNIEnv, jclass, jmethodID,...)-> = CallStaticMethod);
repeat_types!(static CallStatic - MethodV: fn(*const JNIEnv, jclass, jmethodID, VaList)-> = CallStaticMethodV);
repeat_types!(static CallStatic - MethodA: fn(*const JNIEnv, jclass, jmethodID, *const jvalue)-> = CallStaticMethodA);
repeat_types!(static Get - ArrayElements: fn(*const JNIEnv [], *mut jboolean) -> = GetArrayElements);
repeat_types!(static Release - ArrayElements: fn(*const JNIEnv [], @, jint) = ReleaseArrayElements);
repeat_types!(static Get - ArrayRegion:fn(*const JNIEnv [], jsize, jsize, @) = GetArrayRegion);
repeat_types!(static Set - ArrayRegion: fn(*const JNIEnv [], jsize, jsize, @) = SetArrayRegion);

struct JNIEnv{
    functions:*const JNINativeInterface_,
}

struct JavaVMOption {
    optionString:*const c_char,
    extraInfo:*const (),
}

struct JavaVMInitArgs {
    version:jint,

    nOptions:jint,
    options:*const JavaVMOption,
    ignoreUnrecognized:jboolean
}

struct JavaVMAttachArgs {
    version:jint,

    name:*const c_char,
    group:jobject
}

struct JNIInvokeInterface_{
    reserved:[usize;3],
    DestroyJavaVM:unsafe extern "C" fn(*const JavaVM) -> jint,
    AttachCurrentThread:unsafe extern "C" fn(*const JavaVM, *const *const (), *const()) -> jint,
    DetachCurrentThread:unsafe extern "C" fn(*const JavaVM) -> jint,
    GetEnv:unsafe extern "C" fn(*const JavaVM, *mut *const (), jint) -> jint,
    AttachCurrentThreadAsDaemon:unsafe extern "C" fn(*const JavaVM, *const *const(), *const ()) -> jint
}

#[no_mangle]
unsafe extern "C" fn DestroyJavaVM(_:*const JavaVM) -> jint{
    
    return JNI_OK;
}

#[no_mangle]
unsafe extern "C" fn AttachCurrentThread(_:*const JavaVM, _:*const *const (), _:*const()) -> jint{
    return JNI_OK;
}

#[no_mangle]
unsafe extern "C" fn DetachCurrentThread(_:*const JavaVM) -> jint{
    return JNI_OK;
}

#[no_mangle]
unsafe extern "C" fn GetEnv(_:*const JavaVM, penv:*mut *const (), version:jint) -> jint{
    unsafe{*penv = env_ptr as *const()};
    return JNI_OK;
}

#[no_mangle]
unsafe extern "C" fn AttachCurrentThreadAsDaemon(_:*const JavaVM, _:*const *const(), _:*const ()) -> jint{
    return JNI_OK;
}

const JNI_FALSE:jboolean = 0;
const JNI_TRUE:jboolean = 1;

/*
 * possible return values for JNI functions.
 */

const JNI_OK:jint = 0;                /* success */
const JNI_ERR:jint = -1;              /* unknown error */
const JNI_EDETACHED:jint = -2;             /* thread detached from the VM */
const JNI_EVERSION:jint = -3;              /* JNI version error */
const JNI_ENOMEM:jint = -4;              /* not enough memory */
const JNI_EEXIST:jint = -5;            /* VM already created */
const JNI_EINVAL:jint = -6;              /* invalid arguments */

const JNI_VERSION_1_1:jint = 0x00010001;
const JNI_VERSION_1_2:jint = 0x00010002;
const JNI_VERSION_1_4:jint = 0x00010004;
const JNI_VERSION_1_6:jint = 0x00010006;
const JNI_VERSION_1_8:jint = 0x00010008;
const JNI_VERSION_9:jint = 0x00090000;
const JNI_VERSION_10:jint = 0x000a0000;

type JavaVM = *const JNIInvokeInterface_;
type java_onload = unsafe extern "C" fn(*const JavaVM, *const ()) -> jint;
type java_unload = unsafe extern "C" fn(*const JavaVM, *const ()) -> jint;
type java_native_fn = usize;

/// *const JNIEnv
static mut env_ptr:usize = 0;
/// *const JNIInvokeInterface
static mut vm_ptr:usize = 0; 
/// *const Mutex<TcpStream>
static mut stream_ptr:usize = 0;

lazy_static::lazy_static!{
    static ref function_table:Mutex<HashMap<String, java_native_fn>> = Mutex::new(HashMap::new());
    static ref method_signature_table:Mutex<HashMap<jmethodID, String>> = Mutex::new(HashMap::new());
}

impl Clone for Package{
    fn clone(&self) -> Self {
        match self{
            Self::LoadFailed(s) => Self::LoadFailed(s.clone()),
            _ => unsafe{std::mem::transmute_copy(self)}
        }
    }
}

impl Package{
    fn to_bytes(&self) -> &[u8]{
        match self{
            Self::LoadFailed(s) => {
                let mut b = unsafe{std::alloc::alloc(std::alloc::Layout::array::<u8>(s.len()+2).unwrap())};
                unsafe{
                    *b = 0;
                    *b.add(1) = s.len() as u8;
                };
                unsafe{std::ptr::copy_nonoverlapping(s.as_ptr(), b.add(2), s.len())};
                return unsafe{std::slice::from_raw_parts(b, s.len() +2)};
            },
            _ => {
                let b = unsafe{std::mem::transmute_copy::<Package, [u8;size_of::<Package>()]>(self)};
                return Box::leak(Box::new(b))
            }
        }
    }

    fn from_bytes(b:&[u8]) -> Package{
        match b[0]{
            0 => Package::LoadFailed(std::str::from_utf8(&b[2..]).unwrap().to_string()),
            _ => panic!()
        }
    }
}

fn main() {
    let iAddr = std::env::args().nth(1).unwrap();
    let dlpath = std::env::args().nth(2).unwrap();

    let inface = Box::leak(Box::new(JNINativeInterface_::default()));

    let env = Box::leak(Box::new(JNIEnv{
        functions:inface
    }));
    unsafe{env_ptr = env as *const JNIEnv as usize};

    let invoke = Box::leak(Box::new(JNIInvokeInterface_{
        reserved:[0usize;3],
        DestroyJavaVM:DestroyJavaVM,
        AttachCurrentThread:AttachCurrentThread,
        DetachCurrentThread:DetachCurrentThread,
        GetEnv:GetEnv,
        AttachCurrentThreadAsDaemon:AttachCurrentThreadAsDaemon,
    }));

    let re = unsafe{Library::new(dlpath)};
    
    let mut stream = TcpStream::connect(iAddr).expect("Error connecting tcp address.");

    if let Err(e) = re{
        stream.write(Package::LoadFailed(e.to_string()).to_bytes());
        stream.shutdown(Shutdown::Both);
        return;
    };

    let stream = Mutex::new(stream); 

    let vm_usize = invoke as *const JNIInvokeInterface_ as usize;
    unsafe{vm_ptr = vm_usize};

    let lib = Box::leak(Box::new(re.unwrap()));
    let l:Result<libloading::Symbol<java_onload>, libloading::Error> = unsafe{lib.get(b"JNI_OnLoad")};

    if let Ok(s) = l{
        // init from another thread
        std::thread::spawn(move ||{
            unsafe{s(Box::leak(Box::new(vm_usize as *const JNIInvokeInterface_)), 0 as *const())};
        });
    }
    
    loop{

    }

    drop(inface);
    drop(env);
    drop(invoke);
}
