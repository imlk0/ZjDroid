#include <string.h>
#include <jni.h>
#include <dlfcn.h>
#include <pthread.h>
#include <stdint.h>
#include <android/log.h>
#include <stdlib.h>
//#include <utils/Log.h>
#include "util.h"
#include "elfinfo.h"

#define DEX_MAGIC       "dex\n"
#define DEX_MAGIC_VERS  "036\0"
#define DEX_MAGIC_VERS_API_13  "035\0"
#define DEX_OPT_MAGIC   "dey\n"
#define DEX_OPT_MAGIC_VERS  "036\0"
#define DEX_DEP_MAGIC   "deps"

typedef void* (*dvmDecodeIndirectRef_func)(void* self, jobject jobj);
typedef void* (*dvmThreadSelf_func)();
typedef int (*dvmGetInlineOpsTableLengthPtr)();
void* (*dvmDecodeIndirectRef_ptr)(void* self, jobject jobj);
void* (*dvmThreadSelf_ptr)();

typedef struct DexHeader {
    u1 magic[8];           /* includes version number */
    u4  checksum;           /* adler32 checksum */
    u1 signature[20];      /* SHA-1 hash */
    u4  fileSize;           /* length of entire file */
    u4  headerSize;         /* offset to start of next section */
    u4  endianTag;
    u4  linkSize;
    u4  linkOff;
    u4  mapOff;
    u4  stringIdsSize;
    u4  stringIdsOff;
    u4  typeIdsSize;
    u4  typeIdsOff;
    u4  protoIdsSize;
    u4  protoIdsOff;
    u4  fieldIdsSize;
    u4  fieldIdsOff;
    u4  methodIdsSize;
    u4  methodIdsOff;
    u4  classDefsSize;
    u4  classDefsOff;
    u4  dataSize;
    u4  dataOff;
}DexHeader;

typedef struct DexOptHeader {
    u1  magic[8];           /* includes version number */

    u4  dexOffset;          /* file offset of DEX header */
    u4  dexLength;
    u4  depsOffset;         /* offset of optimized DEX dependency table */
    u4  depsLength;
    u4  optOffset;          /* file offset of optimized data tables */
    u4  optLength;

    u4  flags;              /* some info flags */
    u4  checksum;           /* adler32 checksum covering deps/opt */

    /* pad for 64-bit alignment if necessary */
}DexOptHeader;

/*
 * Direct-mapped "map_item".
 */
typedef struct DexMapItem {
    u2 type;              /* type code (see kDexType* above) */
    u2 unused;
    u4 size;              /* count of items of the indicated type */
    u4 offset;            /* file offset to the start of data */
}DexMapItem;

/*
 * Direct-mapped "map_list".
 */
typedef struct DexMapList {
    u4  size;               /* #of entries in list */
    DexMapItem list[1];     /* entries */
}DexMapList;


typedef struct DexStringId {
    u4 stringDataOff;      /* file offset to string_data_item */
}DexStringId;

/*
 * Direct-mapped "type_id_item".
 */
typedef struct DexTypeId {
    u4  descriptorIdx;      /* index into stringIds list for type descriptor */
}DexTypeId;

/*
 * Direct-mapped "field_id_item".
 */
typedef struct DexFieldId {
    u2  classIdx;           /* index into typeIds list for defining class */
    u2  typeIdx;            /* index into typeIds for field type */
    u4  nameIdx;            /* index into stringIds for field name */
}DexFieldId;

/*
 * Direct-mapped "method_id_item".
 */
typedef struct DexMethodId {
    u2  classIdx;           /* index into typeIds list for defining class */
    u2  protoIdx;           /* index into protoIds for method prototype */
    u4    nameIdx;            /* index into stringIds for method name */
}DexMethodId;

/*
 * Direct-mapped "proto_id_item".
 */
typedef struct DexProtoId {
    u4  shortyIdx;          /* index into stringIds for shorty descriptor */
    u4  returnTypeIdx;      /* index into typeIds list for return type */
    u4  parametersOff;      /* file offset to type_list for parameter types */
} DexProtoId;

/*
 * Direct-mapped "class_def_item".
 */
typedef struct DexClassDef {
    u4  classIdx;           /* index into typeIds for this class */
    u4  accessFlags;
    u4  superclassIdx;      /* index into typeIds for superclass */
    u4  interfacesOff;      /* file offset to DexTypeList */
    u4  sourceFileIdx;      /* index into stringIds for source file name */
    u4  annotationsOff;     /* file offset to annotations_directory_item */
    u4  classDataOff;       /* file offset to class_data_item */
    u4  staticValuesOff;    /* file offset to DexEncodedArray */
}DexClassDef;


typedef struct DexFile {
    /* directly-mapped "opt" header */
    const DexOptHeader* pOptHeader;

    /* pointers to directly-mapped structs and arrays in base DEX */
    const DexHeader*    pHeader;
    const void*  pStringIds;
    const void*    pTypeIds;
    const void*   pFieldIds;
    const void*  pMethodIds;
    const void*   pProtoIds;
    const void*  pClassDefs;
    const void*  pLinkData;

    /*
     * These are mapped out of the "auxillary" section, and may not be
     * included in the file.
     */
    const void* pClassLookup;
    const void* pRegisterMapPool;       // RegisterMapClassPool

    /* points to start of DEX file data */
    const u1*   baseAddr;
    
      /* track memory overhead for auxillary structures */
    int                 overhead;

    /* additional app-specific data structures associated with the DEX */
    //void*               auxData;
  }DexFile;

typedef struct MemMapping {
    void*   addr;           /* start of data */
    size_t  length;         /* length of data */

    void*   baseAddr;       /* page-aligned base address */
    size_t  baseLength;     /* length of mapping */
}MemMapping;

typedef struct ZipArchive {
    /* open Zip archive */
    int         mFd;

    /* mapped central directory area */
    off_t       mDirectoryOffset;
    MemMapping  mDirectoryMap;

    /* number of entries in the Zip archive */
    int         mNumEntries;

    /*
     * We know how many entries are in the Zip archive, so we can have a
     * fixed-size hash table.  We probe on collisions.
     */
    int         mHashTableSize;
    void*       mHashTable;
}ZipArchive;

typedef struct RawDexFile {
    char*       cacheFileName;
    void*       pDvmDex;
}RawDexFile;

typedef struct JarFile {
    ZipArchive  archive;
    char*       cacheFileName;
    void*       pDvmDex;
}JarFile;


typedef struct DexOrJar {
    char*          fileName;
    bool           isDex;
    bool           okayToFree;
    RawDexFile*    pRawDexFile;
	  JarFile*       pJarFile;
	  u1* pDexMemory; // Android4.0 bytes
} DexOrJar;

//Android 4.0
typedef struct DvmDex {
    /* pointer to the DexFile we're associated with */
    void *pDexFile;

    /* clone of pDexFile->pHeader (it's used frequently enough) */
    void *pHeader;

    /* interned strings; parallel to "stringIds" */
    void *pResStrings;

    /* resolved classes; parallel to "typeIds" */
    void *pResClasses;

    /* resolved methods; parallel to "methodIds" */
    void *pResMethods;

    /* resolved instance fields; parallel to "fieldIds" */
    /* (this holds both InstField and StaticField) */
    void *pResFields;

    /* interface method lookup cache */
    void *pInterfaceCache;

    /* shared memory region with file contents */
    bool                isMappedReadOnly;
    MemMapping          memMap;
}DvmDex;

//Android 4.0
typedef struct DvmDex2 {
    /* pointer to the DexFile we're associated with */
    void *pDexFile;

    /* clone of pDexFile->pHeader (it's used frequently enough) */
    void *pHeader;

    /* interned strings; parallel to "stringIds" */
    void *pResStrings;

    /* resolved classes; parallel to "typeIds" */
    void *pResClasses;

    /* resolved methods; parallel to "methodIds" */
    void *pResMethods;

    /* resolved instance fields; parallel to "fieldIds" */
    /* (this holds both InstField and StaticField) */
    void *pResFields;

    /* interface method lookup cache */
    void *pInterfaceCache;

    /* shared memory region with file contents */
    MemMapping          memMap;
}DvmDex2;

typedef struct ClassObject{
    u4               a[2];
    u4               instanceData[4];
    const char*      descriptor;
    char*            descriptorAlloc;
    u4               accessFlags;
    u4               serialNumber;
    void*            pDvmDex;
}ClassObject;

typedef struct DexProto {
    /* the class we are a part of */
    const DexFile* dexFile;
	u4             protoIdx;
} DexProto;


typedef struct Method {
    /* the class we are a part of */
    ClassObject*    clazz;
    u4              accessFlags;
    u2             methodIndex;
    u2              registersSize;  /* ins + locals */
    u2              outsSize;
    u2              insSize;
    const char*     name;
    DexProto        prototype;
    const char*     shorty;
    const u2*       insns;          /* instructions, in memory-mapped .dex */
}Method;

static void descDexOrJar(DexOrJar* pDexOrJar){
  if(pDexOrJar != NULL ){
     LOGV("the pDexOrJar fileName =%s",pDexOrJar->fileName);
     LOGV("the pDexOrJar isDex =%d",pDexOrJar->isDex);
     LOGV("the pDexOrJar okayToFree =%d",pDexOrJar->okayToFree);
     LOGV("the pDexOrJar pRawDexFile =%d",pDexOrJar->pRawDexFile);
     LOGV("the pDexOrJar pJarFile =%d",pDexOrJar->pJarFile);
  }
}

static void descRawDexFile(RawDexFile* pRawDexFile){
  if(pRawDexFile != NULL){
     LOGV("the pRawDexFile cacheFileName =%s",pRawDexFile->cacheFileName);
     LOGV("the pRawDexFile pDvmDex =%d",pRawDexFile->pDvmDex);
  }
}

static void descJarFile(JarFile* pJarFile){
  if(pJarFile != NULL){
    LOGV("the pJarFile cacheFileName =%s",pJarFile->cacheFileName);
    LOGV("the pJarFile pDvmDex =%d",pJarFile->pDvmDex);
  }
}

static void descDvmDex(DvmDex* pDvmDex){
  if(pDvmDex != NULL){
	 LOGV("the pDvmDex memMap addr =%d",pDvmDex->memMap.addr);
     LOGV("the pDvmDex memMap length =%d",pDvmDex->memMap.length);
     LOGV("the pDvmDex memMap baseAddr =%d",pDvmDex->memMap.baseAddr);
     LOGV("the pDvmDex memMap baseLength =%d",pDvmDex->memMap.baseLength);
  }
}

static void descDvmDex2(DvmDex2* pDvmDex){
  if(pDvmDex != NULL){
	 LOGV("the pDvmDex memMap addr =%d",pDvmDex->memMap.addr);
     LOGV("the pDvmDex memMap length =%d",pDvmDex->memMap.length);
     LOGV("the pDvmDex memMap baseAddr =%d",pDvmDex->memMap.baseAddr);
     LOGV("the pDvmDex memMap baseLength =%d",pDvmDex->memMap.baseLength);
  }
}

static bool dexHasValidMagic(const DexHeader* pHeader)
{
    const u1* magic = pHeader->magic;
    const u1* version = &magic[4];
    if (memcmp(magic, DEX_MAGIC, 4) != 0) {
        LOGV("unrecognized magic number (%02x %02x %02x %02x)",
            magic[0], magic[1], magic[2], magic[3]);
        return 0;
    }
    if ((memcmp(version, DEX_MAGIC_VERS, 4) != 0) &&
            (memcmp(version, DEX_MAGIC_VERS_API_13, 4) != 0)) {
        LOGV("unsupported dex version (%02x %02x %02x %02x)",
            version[0], version[1], version[2], version[3]);
        return 0;
    }
    return 1;
}

static bool odexHasValidMagic(const DexOptHeader* pHeader)
{
    const u1* magic = pHeader->magic;
    const u1* version = &magic[4];
    if (memcmp(magic, DEX_OPT_MAGIC, 4) != 0) {
        LOGV("unrecognized magic number (%02x %02x %02x %02x)",
            magic[0], magic[1], magic[2], magic[3]);
        return 0;
    }
    if (memcmp(version, DEX_OPT_MAGIC_VERS, 4) != 0) {
        LOGV("unsupported dex version (%02x %02x %02x %02x)",
            version[0], version[1], version[2], version[3]);
        return 0;
    }
    return 1;
}



static DexFile* queryDexFilePoint(int cookie, int ver)
{  
    DexOrJar* pDexOrJar = (DexOrJar*) cookie;
    LOGV("the pDexOrJar mCookie=%d",pDexOrJar);
	  descDexOrJar(pDexOrJar);	
	  DexFile* pDexFile;	
	  if(ver >=14 ){
	  	 DvmDex *pDvmDex;
		   if(pDexOrJar->isDex){
		      descRawDexFile((RawDexFile*)pDexOrJar->pRawDexFile);
		      pDvmDex = (DvmDex*)pDexOrJar->pRawDexFile->pDvmDex;
		   }else{
		        descJarFile((JarFile*)pDexOrJar->pJarFile);
                pDvmDex = (DvmDex*)pDexOrJar->pJarFile->pDvmDex;
				descDvmDex(pDvmDex);
		   }
		   pDexFile = pDvmDex->pDexFile;		   
	}else{
	  	 DvmDex2 *pDvmDex;
		   if(pDexOrJar->isDex){
		      descRawDexFile((RawDexFile*)pDexOrJar->pRawDexFile);
		      pDvmDex = (DvmDex2*)pDexOrJar->pRawDexFile->pDvmDex;
		   }else{
		        descJarFile((JarFile*)pDexOrJar->pJarFile);
            pDvmDex = (DvmDex2*)pDexOrJar->pJarFile->pDvmDex;
		   }
		   pDexFile = pDvmDex->pDexFile;
  }
  return pDexFile;
}

//find the memMap of dexfile which load the certain class;
static jobject dump_ClassObject_DvmDex_MemMap(JNIEnv* env, jclass obj, jclass arg0, jint version)
{  
     char* dvm_lib_path = "libdvm.so";
     void* handle;
	 int ver = version;
     handle = dlopen(dvm_lib_path, 0);
     if (handle == 0) {
     	 LOGV("dlopen the libdvm error");
         return 0;
     }
     dvmDecodeIndirectRef_ptr = (dvmDecodeIndirectRef_func)dlsym(handle, "_Z20dvmDecodeIndirectRefP6ThreadP8_jobject");
     if (dvmDecodeIndirectRef_ptr == 0 ) {
     	 LOGV("dlsym the dvmDecodeIndirectRef_ptr error");
         return 0;
     }
     
     dvmThreadSelf_ptr = (dvmThreadSelf_func)dlsym(handle, "_Z13dvmThreadSelfv");
     if (dvmThreadSelf_ptr == 0 ) {
     	 LOGV("dlsym the dvmThreadSelf_ptr error");
         return 0;
     }
      
     ClassObject* classobj = (ClassObject*) dvmDecodeIndirectRef_ptr(dvmThreadSelf_ptr(), arg0);
	   if(ver >=14 ){
          DvmDex* dvm_dex = (DvmDex*)classobj->pDvmDex;
          if (dvm_dex == NULL) {
        	  LOGV("can not find the pDvmDex in the ClassObject*");
              return NULL;
        }
        LOGV("the pDvmDex in ClassObject =%d",dvm_dex);
        descDvmDex(dvm_dex);
        jobject byte_buffer = (*env)->NewDirectByteBuffer(env,dvm_dex->memMap.addr, dvm_dex->memMap.length);
        if(byte_buffer == NULL){
          return NULL;
        }
	      return byte_buffer;
	   }else{
	      DvmDex2* dvm_dex = (DvmDex2*)classobj->pDvmDex;
        if(dvm_dex == NULL){
           return NULL;
        }
        LOGV("the pDvmDex in ClassObject =%d",dvm_dex);
        descDvmDex(dvm_dex);
        jobject byte_buffer = (*env)->NewDirectByteBuffer(env,dvm_dex->memMap.addr, dvm_dex->memMap.length);
        if(byte_buffer == NULL) {
           return NULL;
        }
	    return byte_buffer;
    }
}

static jobject dump_DexFile_mCookie_DexOrJar_memMap(JNIEnv* env, jclass obj, jint cookie, jint version)
{  
    DexOrJar* pDexOrJar = (DexOrJar*) cookie;
    LOGV("the pDexOrJar mCookie=%d",pDexOrJar);
	  descDexOrJar(pDexOrJar);	
	  int ver = version;	
	  if(ver >=14 ){
		   MemMapping memMap;
		   DvmDex* dvm_dex;
		   if(pDexOrJar->isDex){
		      if(pDexOrJar->pDexMemory == NULL){
		         descRawDexFile((RawDexFile*)pDexOrJar->pRawDexFile);
             dvm_dex = (DvmDex*)pDexOrJar->pRawDexFile->pDvmDex;
			       memMap = dvm_dex->memMap;
             jobject byte_buffer = (*env)->NewDirectByteBuffer(env,memMap.addr, memMap.length);
             if (byte_buffer == NULL) {
                 return NULL;
             }
	           return byte_buffer;			       
			    }else{
			         DexHeader* dexHeader = (DexHeader*)pDexOrJar->pDexMemory;
			         jobject byte_buffer = (*env)->NewDirectByteBuffer(env,pDexOrJar->pDexMemory,dexHeader->fileSize);
               if(byte_buffer == NULL){
                  return NULL;
               }
	             return byte_buffer;
			    }
		   }else{
		        descJarFile((JarFile*)pDexOrJar->pJarFile);
                dvm_dex = (DvmDex*)pDexOrJar->pJarFile->pDvmDex;
                descDvmDex(dvm_dex);
		        memMap = dvm_dex->memMap;
		   }
       if(dvm_dex == NULL) {
          return NULL;
       }
       jobject byte_buffer = (*env)->NewDirectByteBuffer(env,memMap.addr, memMap.length);
       if (byte_buffer == NULL) {
          return NULL;
       }
	   return byte_buffer;
	}else{
		   MemMapping memMap;
		   DvmDex2* dvm_dex;
		   if(pDexOrJar->isDex){
		      descRawDexFile((RawDexFile*)pDexOrJar->pRawDexFile);
		      dvm_dex = (DvmDex2*)pDexOrJar->pRawDexFile->pDvmDex;
		   }else{
		        descJarFile((JarFile*)pDexOrJar->pJarFile);
            dvm_dex = (DvmDex2*)pDexOrJar->pJarFile->pDvmDex;
		   }
       if(dvm_dex == NULL) {
           return NULL;
       }
       memMap = dvm_dex->memMap;
       jobject byte_buffer = (*env)->NewDirectByteBuffer(env,memMap.addr, memMap.length);
       if(byte_buffer == NULL) {
          return NULL;
       }
	   return byte_buffer;
  }
}

static jobject dump_Memory(JNIEnv* env, jclass obj, jint start, jint length)
{  
   jobject byte_buffer = (*env)->NewDirectByteBuffer(env,(u1*)start,length);
   if(byte_buffer == NULL){
     return NULL;
   }
   return byte_buffer;
}


static jobject getHeaderItemPtr(JNIEnv* env, jclass obj, jint mCookie, jint version)
{  
	 DexFile* pDexFile = queryDexFilePoint(mCookie,version);
	 jclass  dexFileHeadersPointer_class = (*env)->FindClass(env,"com/android/reverse/smali/DexFileHeadersPointer");  
	 jobject dexFileItemInfo_obj = (*env)->AllocObject(env,dexFileHeadersPointer_class);  
	 
	 jfieldID stringIdField = (*env)->GetFieldID(env,dexFileHeadersPointer_class,"pStringIds","I"); 
	 (*env)->SetIntField(env,dexFileItemInfo_obj,stringIdField,pDexFile->pStringIds);
	 
	 jfieldID typeIdField = (*env)->GetFieldID(env,dexFileHeadersPointer_class,"pTypeIds","I"); 
	 (*env)->SetIntField(env,dexFileItemInfo_obj,typeIdField,pDexFile->pTypeIds);
	 
	 jfieldID fieldIdField = (*env)->GetFieldID(env,dexFileHeadersPointer_class,"pFieldIds","I"); 
	 (*env)->SetIntField(env,dexFileItemInfo_obj,fieldIdField,pDexFile->pFieldIds);
	 
	 jfieldID methodIdField = (*env)->GetFieldID(env,dexFileHeadersPointer_class,"pMethodIds","I"); 
	 (*env)->SetIntField(env,dexFileItemInfo_obj,methodIdField,pDexFile->pMethodIds);
	 
	 jfieldID protoIdField = (*env)->GetFieldID(env,dexFileHeadersPointer_class,"pProtoIds","I"); 
	 (*env)->SetIntField(env,dexFileItemInfo_obj,protoIdField,pDexFile->pProtoIds);
	 
	 jfieldID classdefsField = (*env)->GetFieldID(env,dexFileHeadersPointer_class,"pClassDefs","I"); 
	 (*env)->SetIntField(env,dexFileItemInfo_obj,classdefsField,pDexFile->pClassDefs);
	 
	 jfieldID baseAddrField = (*env)->GetFieldID(env,dexFileHeadersPointer_class,"baseAddr","I"); 
	 (*env)->SetIntField(env,dexFileItemInfo_obj,baseAddrField,pDexFile->baseAddr);
	 
	 jfieldID classCountField = (*env)->GetFieldID(env,dexFileHeadersPointer_class,"classCount","I"); 
	 (*env)->SetIntField(env,dexFileItemInfo_obj,classCountField,pDexFile->pHeader->classDefsSize);
	 
	 return dexFileItemInfo_obj;
}

typedef struct InlineOperation {
    void *          func;
    const char*     classDescriptor;
    const char*     methodName;
    const char*     methodSignature;
} InlineOperation;

typedef const InlineOperation* (*dvmGetInlineOpsTablePtr)();


static jobject getInlineOperation(JNIEnv* env, jclass obj) {
	int i;
	void *libdvm = dlopen("libdvm.so", RTLD_LAZY);
	if (libdvm == NULL) {
		LOGV("Failed to load libdvm\n");
		return NULL;
	}
	dvmGetInlineOpsTablePtr dvmGetInlineOpsTable = dlsym(libdvm, "dvmGetInlineOpsTable");
	if (dvmGetInlineOpsTable == NULL) {
		dvmGetInlineOpsTable = dlsym(libdvm, "_Z20dvmGetInlineOpsTablev");
	}
	if (dvmGetInlineOpsTable == NULL) {
		LOGV("Failed to load dvmGetInlineOpsTable\n");
		dlclose(libdvm);
		return NULL;
	}
	dvmGetInlineOpsTableLengthPtr dvmGetInlineOpsTableLength = dlsym(libdvm, "dvmGetInlineOpsTableLength");
	if (dvmGetInlineOpsTableLength == NULL) {
		dvmGetInlineOpsTableLength = dlsym(libdvm, "_Z26dvmGetInlineOpsTableLengthv");
	}
	if (dvmGetInlineOpsTableLength == NULL) {
		printf("Failed to load dvmGetInlineOpsTableLength\n");
		dlclose(libdvm);
		return NULL;
	}
    jclass  stringBuilder_class = (*env)->FindClass(env,"java/lang/StringBuilder"); 
    jmethodID initMethod = (*env)->GetMethodID(env,stringBuilder_class,"<init>","()V");  
	jobject stringBuilder_obj = (*env)->NewObject(env,stringBuilder_class,initMethod); 
	jmethodID stringbuilder_append =  (*env)->GetMethodID(env,stringBuilder_class,"append","(Ljava/lang/String;)Ljava/lang/StringBuilder;");
	jmethodID tostring_method =  (*env)->GetMethodID(env,stringBuilder_class,"toString","()Ljava/lang/String;");
	const InlineOperation *inlineTable = dvmGetInlineOpsTable();
	int length = dvmGetInlineOpsTableLength();
    char* buffer = malloc(400);
	for (i=0; i<length; i++) {
		InlineOperation *item = &inlineTable[i];
		sprintf(buffer,"%s->%s%s\n", item->classDescriptor, item->methodName, item->methodSignature);
		jstring descror = (*env)->NewStringUTF(env,buffer);
		(*env)->CallObjectMethod(env,stringBuilder_obj,stringbuilder_append,descror);
	}
	dlclose(libdvm);
	return (*env)->CallObjectMethod(env,stringBuilder_obj,tostring_method);
}


static jobject getSyslinkSnapshot(JNIEnv* env, jclass obj) {

    jclass  hashmap_class = (*env)->FindClass(env,"java/util/HashMap"); 
    jmethodID init_hashmap_Method = (*env)->GetMethodID(env,hashmap_class,"<init>","()V");  
	jmethodID put_method =  (*env)->GetMethodID(env,hashmap_class,"put","(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
	jobject hashmap_obj = (*env)->NewObject(env,hashmap_class,init_hashmap_Method); 
	jclass  long_class = (*env)->FindClass(env,"java/lang/Integer"); 
    jmethodID init_long_Method = (*env)->GetMethodID(env,long_class,"<init>","(I)V"); 
	
    FILE *m = NULL;
	char maps[80];
	char line[200];
	char soaddrs[20];
	char soaddr[10];
	char soname[60];
	char prop[10];
	long soaddval;
	long base;
	memset(maps, 0, sizeof(maps));
	memset(soaddrs, 0, sizeof(soaddrs));
	memset(soaddr, 0, sizeof(soaddr));
	sprintf(maps, "/proc/self/maps");
	m = fopen(maps, "r");
	if (!m) {
		LOGE("open maps error");
		return hashmap_obj;
	}
	while (fgets(line, sizeof(line), m)) {
		int found = 0;
		struct elf_info einfo;
		long tmpaddr = 0;

		if (strstr(line, ".so") == NULL)
			continue;
		if (strstr(line, "r-xp") == NULL)
			continue;
		sscanf(line, "%s %s %*s %*s %*s %s", soaddrs, prop, soname);
		sscanf(soaddrs, "%[^-]", soaddr);
		
		jstring so_name_jstr = (*env)->NewStringUTF(env,soname);
		jobject syslist_obj = (*env)->NewObject(env,hashmap_class,init_hashmap_Method);
		(*env)->CallObjectMethod(env,hashmap_obj,put_method,so_name_jstr,syslist_obj);
		
		base = strtoul(soaddr, NULL, 16);
		get_elf_info(1, base, &einfo);
		
		Elf32_Rel rel;
	    Elf32_Sym sym;
	    unsigned int i;
	    char *sym_name = NULL;
	    unsigned int fuction_point;
	    struct dyn_info dinfo;
	    get_dyn_info(&einfo, &dinfo);
	    for (i = 0; i < dinfo.nrels; i++) {
		     memcpy((void*)&rel, (void*)((unsigned int) (dinfo.jmprel + i * sizeof(Elf32_Rel))),
				sizeof(Elf32_Rel));
		     if (ELF32_R_SYM(rel.r_info)) {
			     memcpy((void*)&sym,
					(void*)(dinfo.symtab + ELF32_R_SYM(rel.r_info) * sizeof(Elf32_Sym)),
					sizeof(Elf32_Sym));
			     sym_name = readstr(einfo.pid, dinfo.strtab + sym.st_name);
				 jstring sym_name_jstr = (*env)->NewStringUTF(env,sym_name);
				 fuction_point = ((einfo.ehdr.e_type == ET_DYN) ? einfo.base : 0) + rel.r_offset;
				 jobject long_obj = (*env)->NewObject(env,long_class,init_long_Method,fuction_point);
				 (*env)->CallObjectMethod(env,syslist_obj,put_method,sym_name_jstr,long_obj);
				 (*env)->ReleaseStringUTFChars(env,sym_name_jstr,sym_name);
				 (*env)->DeleteLocalRef(env,sym_name_jstr);
				 (*env)->DeleteLocalRef(env,long_obj);
		    }
	    }
		(*env)->ReleaseStringUTFChars(env,so_name_jstr,soname);
		(*env)->DeleteLocalRef(env,so_name_jstr);
		(*env)->DeleteLocalRef(env,syslist_obj);
	}
	return hashmap_obj;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) 
{
	JNIEnv* env = NULL;
	jint result = -1;
	if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_4) != JNI_OK) {
		return JNI_ERR;
	}
	JNINativeMethod gMethods[] = { { "dumpDexFileByClass", "(Ljava/lang/Class;I)Ljava/nio/ByteBuffer;", (void*) dump_ClassObject_DvmDex_MemMap },
	                               { "dumpDexFileByCookie", "(II)Ljava/nio/ByteBuffer;", (void*) dump_DexFile_mCookie_DexOrJar_memMap },
	                               { "dumpMemory", "(II)Ljava/nio/ByteBuffer;", (void*) dump_Memory }, 
	                               { "getHeaderItemPtr", "(II)Lcom/android/reverse/smali/DexFileHeadersPointer;", (void*) getHeaderItemPtr },
	                               { "getInlineOperation", "()Ljava/lang/String;", (void*) getInlineOperation },
                                   { "getSyslinkSnapshot", "()Ljava/util/HashMap;", (void*) getSyslinkSnapshot },
//                                   { "getMethodInst", "(Ljava/lang/reflect/Method;)Ljava/nio/ByteBuffer;;", (void*) getMethodInst },
	};
	jclass clazz = (*env)->FindClass(env,"com/android/reverse/util/NativeFunction");
	if (clazz == NULL) {
		return JNI_ERR;
	}
	if ((*env)->RegisterNatives(env, clazz, gMethods,
			sizeof(gMethods) / sizeof(gMethods[0])) < 0) {
		return JNI_ERR;
	}
	result = JNI_VERSION_1_4;
	return result;
}