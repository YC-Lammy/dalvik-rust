

pub struct File<'a> {
    pub bytes: &'a [u8],
    pub ehdr: types::FileHeader,
    pub phdrs: Vec<types::ProgramHeader>,
    pub sections: Vec<Section>,
}
