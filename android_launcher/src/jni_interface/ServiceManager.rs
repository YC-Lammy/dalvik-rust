

/*
    The service manager manages all service being provided on all launched apps.
 */

use lazy_static::lazy_static;
use rustc_hash::FxHashMap;
use parking_lot::RwLock;
use lock_api::*;

use jni::{objects::*, JNIEnv};

use super::apps::AppID;

lazy_static!{
    static ref SERVICE_MANAGER:ServiceManager = ServiceManager::new();
}

pub struct ServiceManager{
    pub services:RwLock<FxHashMap<String, Service>>
}

impl ServiceManager{
    pub fn new() -> Self{
        let manager =  Self{
            services:RwLock::new(FxHashMap::default())
        };
        return manager;
    }

    pub fn getService(id:AppID, name:String){

    }
}

unsafe impl Send for ServiceManager{}
unsafe impl Sync for ServiceManager{}

pub struct Service{
    pub appid:AppID,
    pub servicObject:GlobalRef
}

impl Service{
    fn new(env:JNIEnv, class:JClass){
        //env.new_global_ref(obj).unwrap().as_obj()
        
    }
}