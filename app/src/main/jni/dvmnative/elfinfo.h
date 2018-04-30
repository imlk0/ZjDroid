#ifndef ELFINFO_H_
#define ELFINFO_H_
#include <elf.h>
#include "util.h"

struct dyn_info {
	Elf32_Addr symtab;
	Elf32_Addr strtab;
	Elf32_Addr jmprel;
	Elf32_Word totalrelsize;
	Elf32_Word relsize;
	Elf32_Word nrels;

};

struct elf_info {
	int pid;
	Elf32_Addr base;
	Elf32_Ehdr ehdr;
	Elf32_Phdr phdr;
	Elf32_Dyn dyn;
	Elf32_Addr dynaddr;
	Elf32_Word got;
	Elf32_Addr phdr_addr;
	Elf32_Addr map_addr;
	Elf32_Word nchains;

};

#define IS_DYN(_einfo) (_einfo->ehdr.e_type == ET_DYN)
void get_elf_info(int pid, Elf32_Addr base, struct elf_info *einfo);
unsigned long find_sym_in_rel(struct elf_info *einfo, char *sym_name);
void get_dyn_info(struct elf_info *einfo, struct dyn_info *dinfo);
bool replace_all_rels(char* libpath, char *fucation_name, void *newFun_ptr);
bool replace_certain_rels(char *libpath, char* fucation_name[], u4 newFun_ptr[], int size);

#endif
