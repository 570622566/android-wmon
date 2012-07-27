LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_SRC_FILES:= ubertooth.c ubertooth-util.c
LOCAL_MODULE := ubertooth_util
LOCAL_C_INCLUDES += jni/libusb jni/libbtbb
LOCAL_SHARED_LIBRARIES := libc libusb libbtbb
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog 
include $(BUILD_EXECUTABLE)
