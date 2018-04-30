#ifndef DALVIK_TYPE_WARP_H_
#define DALVIK_TYPE_WARP_H_
#include <stdio.h>
#include <android/log.h>

typedef uint8_t             u1;
typedef uint16_t            u2;
typedef uint32_t            u4;
typedef uint64_t            u8;
typedef int8_t              s1;
typedef int16_t             s2;
typedef int32_t             s4;
typedef int64_t             s8;
typedef char bool;

#define true (bool)1
#define false (bool)0

typedef union JValue {
    u1      z;
    s1      b;
    u2      c;
    s2      s;
    s4      i;
    s8      j;
    float   f;
    double  d;
    void*   l;
} JValue;

#define LOGTAG "zjdroid"

#define LOGW(...)  __android_log_print(ANDROID_LOG_DEBUG, LOGTAG,"W/" __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_DEBUG, LOGTAG,"E/" __VA_ARGS__)
#define LOGV(...) __android_log_print(ANDROID_LOG_DEBUG, LOGTAG,"V/" __VA_ARGS__)
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOGTAG,"D/" __VA_ARGS__)
#define LOGVV(...)  __android_log_print(ANDROID_LOG_DEBUG, LOGTAG,"D/" __VA_ARGS__)

#define pint(_x)  __android_log_print(ANDROID_LOG_DEBUG, "ELF","[%20s( %04d )]  %-30s = %d (0x%08x)\n",__FUNCTION__,__LINE__, #_x, (int)(_x), (int)(_x))
#define puint(_x) __android_log_print(ANDROID_LOG_DEBUG, "ELF","[%20s( %04d )]  %-30s = %u (0x%08x)\n",__FUNCTION__,__LINE__, #_x, (unsigned int)(_x), (unsigned int)(_x))
#define pstr(_x)  __android_log_print(ANDROID_LOG_DEBUG, "ELF","[%20s( %04d )]  %-30s = %s \n",__FUNCTION__,__LINE__, #_x, (char*)(_x))

#endif
