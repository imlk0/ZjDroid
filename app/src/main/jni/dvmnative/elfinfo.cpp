#include <stdlib.h>
#include <stdio.h>
#include "elfinfo.h"
#include <link.h>
#include <elf.h>
#include "util.h"
#include <android/log.h>

char * readstr(int pid, unsigned long addr) {
	char *str = (char *) malloc(64);
	int i, count;
	long word;
	char *pa;
	i = count = 0;
	pa = (char *) &word;

	while (i <= 60) {
		memcpy(&word, (void*) (addr + count), 4);
		count += 4;
		if (pa[0] == '\0') {
			str[i] = '\0';
			break;
		} else
			str[i++] = pa[0];

		if (pa[1] == '\0') {
			str[i] = '\0';
			break;
		} else
			str[i++] = pa[1];

		if (pa[2] == '\0') {
			str[i] = '\0';
			break;
		} else
			str[i++] = pa[2];

		if (pa[3] == '\0') {
			str[i] = '\0';
			break;
		} else
			str[i++] = pa[3];
	}
	return str;
}

void get_elf_info(int pid, Elf32_Addr base, struct elf_info *einfo) {
	int i = 0;
	einfo->pid = pid;
	einfo->base = base;
	memcpy((void*)&einfo->ehdr, (void*)einfo->base, sizeof(Elf32_Ehdr));
	einfo->phdr_addr = einfo->base + einfo->ehdr.e_phoff;

	memcpy((void*)&einfo->phdr, (void*)einfo->phdr_addr, sizeof(Elf32_Phdr));
	for (i = 0; i < einfo->ehdr.e_phnum; i++) {
		Elf32_Phdr phdr;
		memcpy((void*)&phdr, (void*)(einfo->phdr_addr + i * sizeof(Elf32_Phdr)),
				sizeof(Elf32_Phdr));
	}
	while (einfo->phdr.p_type != PT_DYNAMIC) {
		memcpy((void*)&einfo->phdr, (void*)(einfo->phdr_addr += sizeof(Elf32_Phdr)),
				sizeof(Elf32_Phdr));
	}
	einfo->dynaddr = (IS_DYN(einfo) ? einfo->base : 0) + einfo->phdr.p_vaddr;
	memcpy((void*)&einfo->dyn, (void*)einfo->dynaddr, sizeof(Elf32_Dyn));
	while (einfo->dyn.d_tag != DT_PLTGOT) {
		memcpy((void*)&einfo->dyn, (void*)(einfo->dynaddr + i * sizeof(Elf32_Dyn)),
				sizeof(Elf32_Dyn));
		i++;
	}
	einfo->got = (IS_DYN(einfo) ? einfo->base : 0)
			+ (Elf32_Word) einfo->dyn.d_un.d_ptr;
	memcpy((void*)&einfo->map_addr,(void*)(einfo->got + 4), 4);
}

unsigned long find_sym_in_rel(struct elf_info *einfo, char *sym_name) {
	Elf32_Rel rel;
	Elf32_Sym sym;
	unsigned int i;
	char *str = NULL;
	unsigned long ret;
	struct dyn_info dinfo;
	get_dyn_info(einfo, &dinfo);
	for (i = 0; i < dinfo.nrels; i++) {
		memcpy((void*)&rel, (void*)((unsigned long) (dinfo.jmprel + i * sizeof(Elf32_Rel))),
				sizeof(Elf32_Rel));
		if (ELF32_R_SYM(rel.r_info)) {
			memcpy((void*)&sym,
					(void*)(dinfo.symtab + ELF32_R_SYM(rel.r_info) * sizeof(Elf32_Sym)),
					sizeof(Elf32_Sym));
			str = readstr(einfo->pid, dinfo.strtab + sym.st_name);
			if (strcmp(str, sym_name) == 0) {
				free(str);
				break;
			}
			free(str);
		}
	}
	if (i == dinfo.nrels)
		ret = 0;
	else {
		ret = (IS_DYN(einfo) ? einfo->base : 0) + rel.r_offset;
	}
	return ret;
}

void get_dyn_info(struct elf_info *einfo, struct dyn_info *dinfo) {
	Elf32_Dyn dyn;
	int i = 0;
	memcpy((void*)&dyn, (void*)(einfo->dynaddr + i * sizeof(Elf32_Dyn)), sizeof(Elf32_Dyn));
	i++;
	while (dyn.d_tag) {
		switch (dyn.d_tag) {
		case DT_SYMTAB:
			dinfo->symtab = (IS_DYN(einfo) ? einfo->base : 0) + dyn.d_un.d_ptr;
			break;
		case DT_STRTAB:
			dinfo->strtab = (IS_DYN(einfo) ? einfo->base : 0) + dyn.d_un.d_ptr;
			break;
		case DT_JMPREL:
			dinfo->jmprel = (IS_DYN(einfo) ? einfo->base : 0) + dyn.d_un.d_ptr;
			break;
		case DT_PLTRELSZ:
			dinfo->totalrelsize = dyn.d_un.d_val;
			break;
		case DT_RELAENT:
			dinfo->relsize = dyn.d_un.d_val;
			break;
		case DT_RELENT:
			dinfo->relsize = dyn.d_un.d_val;
			break;
		}
		memcpy((void*)&dyn, (void*)(einfo->dynaddr + i * sizeof(Elf32_Dyn)), sizeof(Elf32_Dyn));
		i++;
	}
	if (dinfo->relsize == 0) {
		dinfo->relsize = 8;
	}
	dinfo->nrels = dinfo->totalrelsize / dinfo->relsize;

}

bool replace_all_rels(char *libpath, char *fucation_name, void *newFun_ptr) {
	LOGV("get into replace_all_rels");
	FILE *m = NULL;
	char maps[80];
	char line[200];
	char soaddrs[20];
	char soaddr[10];
	char soname[60];
	char prop[10];
	long soaddval;
	long base;
	int result = false;
	memset(maps, 0, sizeof(maps));
	memset(soaddrs, 0, sizeof(soaddrs));
	memset(soaddr, 0, sizeof(soaddr));
	sprintf(maps, "/proc/self/maps");
	m = fopen(maps, "r");
	if (!m) {
		LOGE("open maps error");
		return result;
	}
	while (fgets(line, sizeof(line), m)) {
		int found = 0;
		struct elf_info einfo;
		long tmpaddr = 0;

		if (strstr(line, ".so") == NULL)
			continue;
		if (strstr(line, "r-xp") == NULL)
			continue;
		if (strstr(line, libpath) != NULL) {
			found = 1;
		}
		if (!found) {
			continue;
		}
		sscanf(line, "%s %s %*s %*s %*s %s", soaddrs, prop, soname);
		sscanf(soaddrs, "%[^-]", soaddr);
		LOGV("#### %s %s %s\n", soaddr, prop, soname);
		base = strtoul(soaddr, NULL, 16);
		get_elf_info(1, base, &einfo);
		
		tmpaddr = find_sym_in_rel(&einfo, fucation_name);
		if (tmpaddr != 0) {
			memcpy((void*)tmpaddr, (void*)&newFun_ptr, 4);
            result = true;
            LOGV(" the function %s is hook sucessfully",fucation_name);
		} else {
			LOGV(" the function %s is hook fail",fucation_name);
		}
		return result;
	}
}

bool replace_certain_rels(char *libpath, char* fucation_name[], u4 newFun_ptr[], int size) {
	LOGV("get into replace_all_rels");
	FILE *m = NULL;
	char maps[80];
	char line[200];
	char soaddrs[20];
	char soaddr[10];
	char soname[60];
	char prop[10];
	long soaddval;
	long base;
	int result = false;
	memset(maps, 0, sizeof(maps));
	memset(soaddrs, 0, sizeof(soaddrs));
	memset(soaddr, 0, sizeof(soaddr));
	sprintf(maps, "/proc/self/maps");
	m = fopen(maps, "r");
	if (!m) {
		LOGE("open maps error");
		return result;
	}
	while (fgets(line, sizeof(line), m)) {
		int found = 0;
		struct elf_info einfo;
		long tmpaddr = 0;

		if (strstr(line, ".so") == NULL)
			continue;
		if (strstr(line, "r-xp") == NULL)
			continue;
		if (strstr(line, libpath) != NULL) {
			found = 1;
		}
		if (!found) {
			continue;
		}
		sscanf(line, "%s %s %*s %*s %*s %s", soaddrs, prop, soname);
		sscanf(soaddrs, "%[^-]", soaddr);
		LOGV("#### %s %s %s\n", soaddr, prop, soname);
		base = strtoul(soaddr, NULL, 16);
		puint(base);
		get_elf_info(1, base, &einfo);
		int i =0;
		for(i=0; i<size; i++){
		   	tmpaddr = find_sym_in_rel(&einfo, fucation_name[i]);
		    if (tmpaddr != 0) {
			   memcpy((void*)tmpaddr, (void*)newFun_ptr[i], 4);
               LOGV(" the function %s is hook sucessfully",fucation_name[i]);
		    } else {
		      return result;
			  LOGV(" the function %s is hook fail",fucation_name[i]);
		    }
		}
		result = true;
		return result;
	}
}

