use std::sync::Arc;

pub mod Object{
    pub fn new() -> Object{

    }

    pub struct Object{
        
    }

    impl Object{

    }

    pub trait Object_wait<T,U>{
        fn wait(args:T) -> U;
    }

    impl Object_wait<(), ()> for Object{
        fn wait(args:()) -> () {
            
        }
    }
}
