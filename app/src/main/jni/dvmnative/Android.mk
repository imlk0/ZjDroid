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
LOCAL_CPP_EXTENSION := .cxx .cpp .cc

LOCAL_MODULE    := elfinfo
LOCAL_SRC_FILES := C:\Users\jxht\AndroidStudioProjects\ZjDroid\app\src\main\jni\dvmnative\elfinfo.cpp
LOCAL_LDLIBS    := -ldl -llog

include $(BUILD_STATIC_LIBRARY)


# dvmnative

include $(CLEAR_VARS)
LOCAL_CPP_EXTENSION := .cxx .cpp .cc

LOCAL_MODULE    := dvmnative

LOCAL_SRC_FILES := dvmnative.cpp
LOCAL_STATIC_LIBRARIES := libelfinfo
LOCAL_LDLIBS    := -ldl -llog

include $(BUILD_SHARED_LIBRARY)

