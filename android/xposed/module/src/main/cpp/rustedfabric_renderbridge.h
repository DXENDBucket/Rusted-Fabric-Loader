#pragma once

#include <android/native_window.h>

#ifdef __cplusplus
extern "C" {
#endif

/** Returns an acquired reference to the Activity's current window, or null when detached. */
ANativeWindow* rustedfabric_acquire_native_window(void);

/** Releases a reference returned by rustedfabric_acquire_native_window. */
void rustedfabric_release_native_window(ANativeWindow* window);

#ifdef __cplusplus
}
#endif
