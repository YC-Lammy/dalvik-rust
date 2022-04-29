


fn parse_elf(path:String) -> Result<(), elf::ParseError>{
    let f = elf::File::open_path(path)?;
    println!("abiversion: {}",f.ehdr.abiversion);
    println!("class: {}", f.ehdr.class.to_string());
    println!("data: {}",f.ehdr.data.to_string());
    println!("type: {}", f.ehdr.elftype.to_string());
    println!("entry: {}", f.ehdr.entry);
    println!("machine: {}", f.ehdr.machine.to_string());
    println!("osabi: {}", f.ehdr.osabi.to_string());
    println!("elf_version: {}", f.ehdr.version);
    for i in &f.sections{
        println!("{}", i.to_string());
        //if i.shdr.shtype == elf::types::SHT_DYNSYM{
            for i in f.get_symbols(i).unwrap(){
                println!("{}", i)
            }
        //}

        if i.shdr.name.as_str() == ".dynamic"{
            let re = get_import_libary_names(&f);
            if let Ok(il) = re{
                for i in il{
                    println!("{}", i);
                }
            }
        };
    }
    return Ok(())
}

fn get_import_libary_names(file:&elf::File) -> Result<Vec<String>, String>{
    let ds = file.get_section(".dynamic").unwrap();
    let mut d = ds.data.as_slice();
    let mut st = Vec::new();
    let dynstr = file.sections[ds.shdr.link as usize].data.as_slice();

    while d.len() > 0{
        let mut v:usize = 0;
        match file.ehdr.class{
            elf::types::ELFCLASS32 => {
                if !unsafe{std::mem::transmute::<[u8;4], u32>(d[0..4].try_into().unwrap())} == 0x01u32{
                    return Err("".to_string())
                };
                v = unsafe{std::mem::transmute::<[u8;4], u32>(d[4..8].try_into().unwrap())} as usize;
                d = &d[8..];
            },
            elf::types::ELFCLASS64 => {
                if !unsafe{std::mem::transmute::<[u8;8], u64>(d[0..8].try_into().unwrap())} == 0x01u64{
                    return Err("".to_string())
                };
                v = unsafe{std::mem::transmute::<[u8;8], u64>(d[8..16].try_into().unwrap())} as usize;
                d = &d[16..];
            },
            _ => unreachable!()
        };
        
        if v >= dynstr.len(){
            println!("expected range {}, got index {}", dynstr.len(), v);
        } else{
            let mut end = v;

            while end < dynstr.len(){
                if dynstr[end] == 0{
                    st.push(std::str::from_utf8(&dynstr[v..end]).unwrap().to_string());
                    break;
                }
                end+=1;
            }
        }
    }
    
    return Ok(st);

}

#[test]
fn test_elf(){
    let re = parse_elf(r#"C:\Users\YC\Downloads\Among Us_v2022.3.29_apkpure.com\config.arm64_v8a\lib\arm64-v8a\libunity.so"#.to_string());
    if let Err(e) = re{
        println!("{:?}", e);
    }
}