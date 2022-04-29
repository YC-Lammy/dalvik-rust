use std::any::Any;
use std::panic::RefUnwindSafe;
use std::panic::UnwindSafe;
use std::sync::Arc;
use std::thread::Thread;

use jni::JavaVM;

use jni::JNIEnv;
use jni::sys::*;
use jni::objects::*;

use parking_lot::RwLock;
use lock_api::*;
use rustc_hash::FxHashMap;

use super::native_types;
use super::parcel::Parcel;

lazy_static::lazy_static!{
    pub static ref JavaWorkers:RwLock<FxHashMap<String, Arc<JavaVmWorker>>> = RwLock::new(Default::default());
}

pub enum JavaValue{
    Parcel(Arc<Parcel>),
    Byte(jbyte),
    Char(jchar),
    Short(jshort),
    Int(jint),
    Long(jlong),
    Bool(jboolean),
    Float(jfloat),
    Double(jdouble),
    Void,
}


pub type JavaWorkerCallback = dyn Fn(JNIEnv) -> Option<JavaWorkerCallbackResult> + 'static + Send + Sync;
pub type JavaWorkerCallbackResult = Box<dyn Any + 'static + Send + Sync>;

pub struct JavaVmWorker{
    pub joinHandler:std::thread::JoinHandle<()>,
    pub JavaVm:Arc<JavaVM>,
    pub sender:crossbeam::channel::Sender<Arc<JavaWorkerCallback>>,
    pub resultReciever:crossbeam::channel::Receiver<Option<JavaWorkerCallbackResult>>,
}

impl JavaVmWorker{
    pub fn run<T, R>(&self, task:T) -> Option<R>
    where T:Fn(JNIEnv) -> R + 'static +Send +Sync, R:Any+'static+Send+Sync{
        self.sender.send(Arc::new(move |env|{
            let re = task(env);
            return Some(Box::new(re));
        }));
        let mut re = self.resultReciever.recv().unwrap();
        if let Some(mut r) = re{
            let re = *r.downcast::<R>().unwrap();
            return Some(re);
        } else{
            return None;
        }
    }

    pub fn run_nonblocking<T>(&self, task:T)
    where T:Fn(JNIEnv) + 'static +Send +Sync{
        self.sender.send(Arc::new(move |env|{
            task(env);
            return None
        }));
    }
}

impl Drop for JavaVmWorker{
    fn drop(&mut self) {
        
    }
}

impl UnwindSafe for JavaVmWorker{

}

impl RefUnwindSafe for JavaVmWorker{
    
}

/// cretes a new Java worker.
/// 
/// Each java worker owns a thread that is attached to the JavaVm, aka the 'main thread'
/// 
/// All remote calls are done through channel, arguments are passed as Parcel.
pub fn createJavaWorker(package_name:String, classpath:String, libpath:String) -> Result<Arc<JavaVmWorker>, jni::errors::Error>{
    if JavaWorkers.read().contains_key(&package_name){
        return Ok(JavaWorkers.write().get(&package_name).unwrap().clone());
    }

    let args = jni::InitArgsBuilder::new()
        .version(jni::JNIVersion::Invalid(0x000a0000)) // JNI_VERSION_10
        .option(&format!("-Djava.class.path={}", classpath))
        .option(&format!("-Djava.library.path={}", libpath))
        .build().unwrap();

    let vm = Arc::new(JavaVM::new(args)?);

    let (sender, reciever) = crossbeam::channel::unbounded::<Arc<JavaWorkerCallback>>();

    let (Resultsender, resultReciever) = crossbeam::channel::unbounded::<Option<JavaWorkerCallbackResult>>();

    let Javavm = vm.clone();

    let handler = std::thread::spawn(move ||{
        /*
            this is the main thread of an application.
            All remote calls will come through channel.
        */
        
        let env = vm.attach_current_thread().unwrap();

        // do all the init work
        native_types::register(env.clone());

        loop{
            let next = reciever.recv().unwrap();
            let re = next(env.clone());
            if re.is_some(){
                Resultsender.send(re);
            } 
        }
    });
    
    let worker = Arc::new(JavaVmWorker{
        joinHandler:handler,
        JavaVm:Javavm,
        sender,
        resultReciever
    });
    JavaWorkers.write().insert(package_name, worker.clone());

    return Ok(worker);
}

pub fn resolveWorker(env:JNIEnv) -> Option<Arc<JavaVmWorker>>{
    let vm = env.get_java_vm().unwrap();
    for (pkgname, worker) in JavaWorkers.read().iter(){
        if worker.JavaVm.get_java_vm_pointer() == vm.get_java_vm_pointer(){
            return Some(worker.clone());
        }
    }
    return None;
}