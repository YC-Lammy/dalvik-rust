use std::{path::{PathBuf, Path}, process::Command};
use libc::c_void;
use libc::c_char;
use crate::JavaVM;
use crate::sys::*;

const EXPECTED_JVM_FILENAME:&str = "libjvm.so";

pub struct Loader{
    handle:*mut c_void,
}

impl Loader{
    pub fn newInstant() -> Loader{
        let java_home = match std::env::var("JAVA_HOME") {
            Ok(java_home) => PathBuf::from(java_home),
            Err(_) => find_java_home().expect(
                "Failed to find Java home directory. \
                 Try setting JAVA_HOME",
            ),
        };

        find_libjvm(&java_home).expect("Failed to find libjvm.so. Check JAVA_HOME");

        // On Windows, we need additional file called `jvm.lib`
        // and placed inside `JAVA_HOME\lib` directory.
        #[cfg(windows)]
        let lib_path = java_home.join("lib");

        let libname = if cfg!(target_os = "macos"){
            "jli"
        } else{
            "jvm"
        };

        // search for file

        let filename = format!("{}\0", "");

        let handle = unsafe{libc::dlmopen(
            libc::LM_ID_NEWLM, 
            filename.as_ptr() as *const c_char, 
        libc::RTLD_LAZY | libc::RTLD_LOCAL
        )};
        return Loader { 
            handle 
        };
    }

    pub unsafe fn JNI_CreateJavaVM(
        &self,
        pvm: *mut *mut JavaVM,
        penv: *mut *mut c_void,
        args: *mut c_void,
    ) -> jint{
        let sym = libc::dlsym(self.handle, "JNI_CreateJavaVM\0".as_ptr() as *const c_char);
        let f:extern fn (pvm: *mut *mut JavaVM,
            penv: *mut *mut c_void,
            args: *mut c_void,
        ) -> jint = std::mem::transmute(sym);
        return f(pvm, penv, args);
    }

    pub unsafe fn JNI_GetDefaultJavaVMInitArgs(&self, args: *mut c_void) -> jint{
        let sym = libc::dlsym(self.handle, "JNI_GetDefaultJavaVMInitArgs\0".as_ptr() as *const c_char);
        let f:extern fn (args: *mut c_void) -> jint = std::mem::transmute(sym);
        return f(args);
    }

    
}

/// To find Java home directory, we call
/// `java -XshowSettings:properties -version` command and parse its output to
/// find the line `java.home=<some path>`.
fn find_java_home() -> Option<PathBuf> {
    Command::new("java")
        .arg("-XshowSettings:properties")
        .arg("-version")
        .output()
        .ok()
        .and_then(|output| {
            let stdout = String::from_utf8_lossy(&output.stdout);
            let stderr = String::from_utf8_lossy(&output.stderr);
            for line in stdout.lines().chain(stderr.lines()) {
                if line.contains("java.home") {
                    let pos = line.find('=').unwrap() + 1;
                    let path = line[pos..].trim();
                    return Some(PathBuf::from(path));
                }
            }
            None
        })
}

fn find_libjvm<S: AsRef<Path>>(path: S) -> Option<PathBuf> {
    let walker = walkdir::WalkDir::new(path).follow_links(true);

    for entry in walker {
        let entry = match entry {
            Ok(entry) => entry,
            Err(_e) => continue,
        };

        let file_name = entry.file_name().to_str().unwrap_or("");

        if file_name == EXPECTED_JVM_FILENAME {
            return entry.path().parent().map(Into::into);
        }
    }

    None
}
