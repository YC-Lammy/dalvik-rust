use std::hash::Hash;
use std::hash::Hasher;
use rustc_hash::FxHasher;


pub struct AppID(u64);

impl AppID{
    pub fn fromPackageName(name:String) -> Self{
        let mut hasher = FxHasher::default();
        name.hash(&mut hasher);
        Self(hasher.finish())
    }
}

pub struct AppManager{

}

unsafe impl Send for AppManager{}

unsafe impl Sync for AppManager{}