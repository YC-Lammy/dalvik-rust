#include<iostream>
#include<mutex>
#include<condition_variable>
#include<chrono>
#include<string>
#include<rttr/registration>
using namespace std;

namespace java{
    namespace lang{
        class Class;
        class ClassLoader;
        class String;

        class Object{

            public:
                Object(){

                }

                ~Object(){
                    this->finalize();
                }

                virtual bool operator==(Object *obj){
                    
                    return this->equals(obj);
                }

                virtual bool equals(Object *obj){
                    return obj == this;
                }

                virtual Class* getClass();

                virtual int hashCode(){
                    return (int)this;
                }

                virtual void notify(){
                    cv.notify_one();
                };

                virtual void notifyAll(){
                    std::lock_guard<mutex> guard(notifier);
                    cv.notify_all();
                }

                virtual String* toString();

                virtual void wait(){
                    std::unique_lock<mutex> lock(notifier);
                    cv.wait(lock);
                };

                virtual void wait(long time){
                    std::unique_lock<mutex> lock(notifier);
                    chrono::milliseconds mil(time);
                    cv.wait_for(lock, mil);
                };

                virtual void wait(long time, int nanos){
                    std::unique_lock<mutex> lock(notifier);
                    chrono::nanoseconds nan(1000000*time+nanos);
                    cv.wait_for(lock, nan);
                };

                virtual void finalize(){

                }
            private:
                std::condition_variable cv;
                std::mutex notifier;
        };
        
        RTTR_REGISTRATION{
               rttr::registration::class_<Object>("java.lang.Object")
               .constructor<>()
               .method("equals", &Object::equals)
               .method("getClass", &Object::getClass)
               .method("hashCode", &Object::hashCode)
               .method("notify", &Object::notify)
               .method("notifyAll", &Object::notifyAll)
               .method("toString", &Object::toString)
               .method("wait", rttr::select_overload<void(void)>(&Object::wait))
               .method("wait", rttr::select_overload<void(long)>(&Object::wait))
               .method("wait", rttr::select_overload<void(long, int)>(&Object::wait))
               .method("finalize", &Object::finalize);
        }
        
        class String: public Object{
            public:
                String(){

                }

                bool operator==(String* s){
                    return s->value == this-> value;
                }

                virtual bool equals(Object *obj){
                    return false;
                };
                
                char* value;
        };

        

        //template <class T>
        class Class : public Object{
            public:
                Class(rttr::type *t){
                    this->typ = t;
                }

                ~Class(){

                }

                static Class* forName(String* name){
                    rttr::type * t = (rttr::type*)malloc(sizeof(rttr::type));
                    *t = rttr::type::get_by_name(name->value);
                    if (!t->is_valid()){

                    }
                    return new Class(t);
                }

                virtual bool equals(Class *obj){
                    return true;
                }

                template<class T>
                T* cast(Object *obj){
                    return (T*)obj;
                }
            private:
                rttr::type* typ;
        };

        Class * Object::getClass(){
            rttr::type * t = (rttr::type*)malloc(sizeof(rttr::type));
            *t = rttr::type::get<Object>();
            return new Class(t);
        }
        
        String * Object::toString(){
            
        }
    }
}