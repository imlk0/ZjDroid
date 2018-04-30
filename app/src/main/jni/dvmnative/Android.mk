# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
LOCAL_PATH := $(call my-dir)


# elfinfo
include $(CLEAR_VARS)

LOCAL_MODULE    := elfinfo
LOCAL_SRC_FILES := elfinfo.c

LOCAL_LDLIBS    := -ldl -llog

include $(BUILD_STATIC_LIBRARY)


# dvmnative

include $(CLEAR_VARS)

LOCAL_MODULE    := dvmnative

# 这个地方有毒，我调试了一下午，
# 如果不加$(LOCAL_PATH)则将定位到jni/dvmnative.c
# 加了则定位到jni/dvmnative/dvmnative.c
# 而如果我不加$(LOCAL_PATH)，而把c文件名称换成idvmnative.c则定为到jni/dvmnative/idvmnative.c
# 事实上我只在as里面遇到这个bug，我用ndk-build工具整得好好的，没毛病
LOCAL_SRC_FILES := $(LOCAL_PATH)/dvmnative.c
LOCAL_STATIC_LIBRARIES := libelfinfo
LOCAL_LDLIBS    := -ldl -llog

include $(BUILD_SHARED_LIBRARY)

