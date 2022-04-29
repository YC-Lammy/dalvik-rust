#[cfg(test)]
mod tests {
    #[test]
    fn it_works() {
        let result = 2 + 2;
        assert_eq!(result, 4);
    }
}

mod elf;
mod symtable;
mod vm;
mod elf_reader;
mod emulator;