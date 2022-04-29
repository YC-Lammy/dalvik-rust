package main

import (
	"debug/elf"
	"os"
)

func main() {
	f, err := elf.Open(os.Args[1])
	check_err(err)
	sym, err := f.ImportedSymbols()
	check_err(err)
	for _, v := range sym {
		println(v.Name)
		println(v.Library)
		//println(v.Section.String())
		//println(v.Size)
		println("")
	}
	syms, err := f.DynamicSymbols()
	check_err(err)
	for _, v := range syms {
		println(v.Name)
		println(v.Library)
		//println(v.Section.String())
		//println(v.Size)
		println("")
	}
	l, err := f.ImportedLibraries()
	check_err(err)
	for _, i := range l {
		println(i)
	}
}

func check_err(e error) {
	if e != nil {
		panic(e)
	}
}
