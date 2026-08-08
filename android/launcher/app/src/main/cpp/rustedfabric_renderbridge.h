#pragma once

#include <android/native_window.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/** Returns an acquired reference to the Activity's current window, or null when detached. */
ANativeWindow* rustedfabric_acquire_native_window(void);

/** Atomically returns the current window and the generation to which it belongs. */
ANativeWindow* rustedfabric_acquire_native_window_for_generation(uint64_t* generation);

/** Releases a reference returned by rustedfabric_acquire_native_window. */
void rustedfabric_release_native_window(ANativeWindow* window);

/** Monotonically changes whenever Android attaches or detaches a Surface. */
uint64_t rustedfabric_native_window_generation(void);

#ifdef __cplusplus
}
#endif
