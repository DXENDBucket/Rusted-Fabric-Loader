package io.github.endx.rustedfabricapi.api.event;

public final class RenderImageLifecycleEvents {
    private RenderImageLifecycleEvents() {
    }

    public static final RustedFabricEvent<ImageEvent> BEFORE_RELEASE_IMAGE_DATA = imageEvent();
    public static final RustedFabricEvent<ImageEvent> AFTER_RELEASE_IMAGE_DATA = imageEvent();
    public static final RustedFabricEvent<ImageEvent> BEFORE_DISCARD_PIXEL_BUFFER = imageEvent();
    public static final RustedFabricEvent<ImageEvent> AFTER_DISCARD_PIXEL_BUFFER = imageEvent();
    public static final RustedFabricEvent<ImageEvent> BEFORE_DROP_PIXEL_BUFFER = imageEvent();
    public static final RustedFabricEvent<ImageEvent> AFTER_DROP_PIXEL_BUFFER = imageEvent();
    public static final RustedFabricEvent<ImageEvent> BEFORE_RELEASE_BITMAP = imageEvent();
    public static final RustedFabricEvent<ImageEvent> AFTER_RELEASE_BITMAP = imageEvent();
    public static final RustedFabricEvent<ImageEvent> BEFORE_FLUSH_PIXEL_BUFFER_TO_BITMAP = imageEvent();
    public static final RustedFabricEvent<ImageEvent> AFTER_FLUSH_PIXEL_BUFFER_TO_BITMAP = imageEvent();
    public static final RustedFabricEvent<ImageEvent> BEFORE_ENSURE_IMAGE_DATA_AVAILABLE = imageEvent();
    public static final RustedFabricEvent<ImageEvent> AFTER_ENSURE_IMAGE_DATA_AVAILABLE = imageEvent();
    public static final RustedFabricEvent<ImageEvent> BEFORE_FORCE_LOAD = imageEvent();
    public static final RustedFabricEvent<ImageEvent> AFTER_FORCE_LOAD = imageEvent();
    public static final RustedFabricEvent<ImageEvent> BEFORE_CHECK_AND_RELOAD_IF_FILE_CHANGED = imageEvent();
    public static final RustedFabricEvent<ImageEvent> AFTER_CHECK_AND_RELOAD_IF_FILE_CHANGED = imageEvent();
    public static final RustedFabricEvent<ImageEvent> BEFORE_ENABLE_AUTO_RELEASE_ON_FINALIZE = imageEvent();
    public static final RustedFabricEvent<ImageEvent> AFTER_ENABLE_AUTO_RELEASE_ON_FINALIZE = imageEvent();

    public static final RustedFabricEvent<ImageEvent> BEFORE_ENSURE_SLICK_IMAGE_DATA_LOADED = imageEvent();
    public static final RustedFabricEvent<ImageEvent> AFTER_ENSURE_SLICK_IMAGE_DATA_LOADED = imageEvent();
    public static final RustedFabricEvent<ImageEvent> BEFORE_DROP_SLICK_IMAGE_DATA_IF_ALLOWED = imageEvent();
    public static final RustedFabricEvent<ImageEvent> AFTER_DROP_SLICK_IMAGE_DATA_IF_ALLOWED = imageEvent();
    public static final RustedFabricEvent<ImageEvent> BEFORE_RECREATE_SLICK_IMAGE_DATA_FROM_TEXTURE = imageEvent();
    public static final RustedFabricEvent<ImageEvent> AFTER_RECREATE_SLICK_IMAGE_DATA_FROM_TEXTURE = imageEvent();
    public static final RustedFabricEvent<ImageEvent> BEFORE_RELOAD_SLICK_IMAGE = imageEvent();
    public static final RustedFabricEvent<ImageEvent> AFTER_RELOAD_SLICK_IMAGE = imageEvent();
    public static final RustedFabricEvent<SetSlickImageData> BEFORE_SET_SLICK_IMAGE_DATA =
            setSlickImageDataEvent();
    public static final RustedFabricEvent<SetSlickImageData> AFTER_SET_SLICK_IMAGE_DATA =
            setSlickImageDataEvent();

    public static final RustedFabricEvent<LazyTeamColorLoad> BEFORE_LOAD_LAZY_TEAM_COLOR_IMAGE =
            lazyTeamColorLoadEvent();
    public static final RustedFabricEvent<LazyTeamColorLoad> AFTER_LOAD_LAZY_TEAM_COLOR_IMAGE =
            lazyTeamColorLoadEvent();

    public static final RustedFabricEvent<BackendEvent> BEFORE_LOAD_ERROR_IMAGES = backendEvent();
    public static final RustedFabricEvent<BackendEvent> AFTER_LOAD_ERROR_IMAGES = backendEvent();
    public static final RustedFabricEvent<BackendEvent> BEFORE_FLUSH_IMAGE_DATA_DISCARDS = backendEvent();
    public static final RustedFabricEvent<BackendEvent> AFTER_FLUSH_IMAGE_DATA_DISCARDS = backendEvent();
    public static final RustedFabricEvent<BackendEvent> BEFORE_FLUSH_PENDING_IMAGE_DATA_DISCARDS = backendEvent();
    public static final RustedFabricEvent<BackendEvent> AFTER_FLUSH_PENDING_IMAGE_DATA_DISCARDS = backendEvent();
    public static final RustedFabricEvent<BackendImageEvent> BEFORE_QUEUE_IMAGE_DATA_DISCARD = backendImageEvent();
    public static final RustedFabricEvent<BackendImageEvent> AFTER_QUEUE_IMAGE_DATA_DISCARD = backendImageEvent();

    public static final RustedFabricEvent<LoadImageByResourceId> BEFORE_LOAD_IMAGE_BY_RESOURCE_ID =
            loadImageByResourceIdEvent();
    public static final RustedFabricEvent<LoadImageByResourceIdResult> AFTER_LOAD_IMAGE_BY_RESOURCE_ID =
            loadImageByResourceIdResultEvent();
    public static final RustedFabricEvent<LoadImageByResourceId> BEFORE_LOAD_SLICK_IMAGE_BY_RESOURCE_ID =
            loadImageByResourceIdEvent();
    public static final RustedFabricEvent<LoadImageByResourceIdResult> AFTER_LOAD_SLICK_IMAGE_BY_RESOURCE_ID =
            loadImageByResourceIdResultEvent();
    public static final RustedFabricEvent<LoadSlickImageData> BEFORE_LOAD_SLICK_IMAGE_DATA =
            loadSlickImageDataEvent();
    public static final RustedFabricEvent<LoadSlickImageDataResult> AFTER_LOAD_SLICK_IMAGE_DATA =
            loadSlickImageDataResultEvent();
    public static final RustedFabricEvent<LoadImageFromStream> BEFORE_LOAD_IMAGE_FROM_STREAM =
            loadImageFromStreamEvent();
    public static final RustedFabricEvent<LoadImageFromStreamResult> AFTER_LOAD_IMAGE_FROM_STREAM =
            loadImageFromStreamResultEvent();
    public static final RustedFabricEvent<CreateSlickImageFromData> BEFORE_CREATE_SLICK_IMAGE_FROM_DATA =
            createSlickImageFromDataEvent();
    public static final RustedFabricEvent<CreateSlickImageFromDataResult> AFTER_CREATE_SLICK_IMAGE_FROM_DATA =
            createSlickImageFromDataResultEvent();
    public static final RustedFabricEvent<CreateImage> BEFORE_CREATE_IMAGE = createImageEvent();
    public static final RustedFabricEvent<CreateImageResult> AFTER_CREATE_IMAGE = createImageResultEvent();

    private static RustedFabricEvent<ImageEvent> imageEvent() {
        return RustedFabricEvent.create(listeners -> image -> {
            for (ImageEvent listener : listeners) {
                listener.onEvent(image);
            }
        });
    }

    private static RustedFabricEvent<LazyTeamColorLoad> lazyTeamColorLoadEvent() {
        return RustedFabricEvent.create(listeners -> (image, allowShader) -> {
            for (LazyTeamColorLoad listener : listeners) {
                listener.onEvent(image, allowShader);
            }
        });
    }

    private static RustedFabricEvent<SetSlickImageData> setSlickImageDataEvent() {
        return RustedFabricEvent.create(listeners -> (image, imageData, name, fallback) -> {
            for (SetSlickImageData listener : listeners) {
                listener.onEvent(image, imageData, name, fallback);
            }
        });
    }

    private static RustedFabricEvent<BackendEvent> backendEvent() {
        return RustedFabricEvent.create(listeners -> backend -> {
            for (BackendEvent listener : listeners) {
                listener.onEvent(backend);
            }
        });
    }

    private static RustedFabricEvent<BackendImageEvent> backendImageEvent() {
        return RustedFabricEvent.create(listeners -> (backend, image) -> {
            for (BackendImageEvent listener : listeners) {
                listener.onEvent(backend, image);
            }
        });
    }

    private static RustedFabricEvent<LoadImageByResourceId> loadImageByResourceIdEvent() {
        return RustedFabricEvent.create(listeners -> (backend, resourceId, smooth) -> {
            for (LoadImageByResourceId listener : listeners) {
                listener.onEvent(backend, resourceId, smooth);
            }
        });
    }

    private static RustedFabricEvent<LoadImageByResourceIdResult> loadImageByResourceIdResultEvent() {
        return RustedFabricEvent.create(listeners -> (backend, resourceId, smooth, image) -> {
            for (LoadImageByResourceIdResult listener : listeners) {
                listener.onEvent(backend, resourceId, smooth, image);
            }
        });
    }

    private static RustedFabricEvent<LoadSlickImageData> loadSlickImageDataEvent() {
        return RustedFabricEvent.create(listeners -> (backend, inputStream) -> {
            for (LoadSlickImageData listener : listeners) {
                listener.onEvent(backend, inputStream);
            }
        });
    }

    private static RustedFabricEvent<LoadSlickImageDataResult> loadSlickImageDataResultEvent() {
        return RustedFabricEvent.create(listeners -> (backend, inputStream, imageData) -> {
            for (LoadSlickImageDataResult listener : listeners) {
                listener.onEvent(backend, inputStream, imageData);
            }
        });
    }

    private static RustedFabricEvent<LoadImageFromStream> loadImageFromStreamEvent() {
        return RustedFabricEvent.create(listeners -> (backend, inputStream, smooth) -> {
            for (LoadImageFromStream listener : listeners) {
                listener.onEvent(backend, inputStream, smooth);
            }
        });
    }

    private static RustedFabricEvent<LoadImageFromStreamResult> loadImageFromStreamResultEvent() {
        return RustedFabricEvent.create(listeners -> (backend, inputStream, smooth, image) -> {
            for (LoadImageFromStreamResult listener : listeners) {
                listener.onEvent(backend, inputStream, smooth, image);
            }
        });
    }

    private static RustedFabricEvent<CreateSlickImageFromData> createSlickImageFromDataEvent() {
        return RustedFabricEvent.create(listeners -> (backend, imageData, name) -> {
            for (CreateSlickImageFromData listener : listeners) {
                listener.onEvent(backend, imageData, name);
            }
        });
    }

    private static RustedFabricEvent<CreateSlickImageFromDataResult> createSlickImageFromDataResultEvent() {
        return RustedFabricEvent.create(listeners -> (backend, imageData, name, image) -> {
            for (CreateSlickImageFromDataResult listener : listeners) {
                listener.onEvent(backend, imageData, name, image);
            }
        });
    }

    private static RustedFabricEvent<CreateImage> createImageEvent() {
        return RustedFabricEvent.create(listeners -> (backend, width, height, smooth, bufferBacked) -> {
            for (CreateImage listener : listeners) {
                listener.onEvent(backend, width, height, smooth, bufferBacked);
            }
        });
    }

    private static RustedFabricEvent<CreateImageResult> createImageResultEvent() {
        return RustedFabricEvent.create(listeners -> (backend, width, height, smooth, bufferBacked, image) -> {
            for (CreateImageResult listener : listeners) {
                listener.onEvent(backend, width, height, smooth, bufferBacked, image);
            }
        });
    }

    @FunctionalInterface
    public interface ImageEvent {
        void onEvent(Object image);
    }

    @FunctionalInterface
    public interface LazyTeamColorLoad {
        void onEvent(Object image, boolean allowShader);
    }

    @FunctionalInterface
    public interface SetSlickImageData {
        void onEvent(Object image, Object imageData, String name, boolean fallback);
    }

    @FunctionalInterface
    public interface BackendEvent {
        void onEvent(Object backend);
    }

    @FunctionalInterface
    public interface BackendImageEvent {
        void onEvent(Object backend, Object image);
    }

    @FunctionalInterface
    public interface LoadImageByResourceId {
        void onEvent(Object backend, int resourceId, boolean smooth);
    }

    @FunctionalInterface
    public interface LoadImageByResourceIdResult {
        void onEvent(Object backend, int resourceId, boolean smooth, Object image);
    }

    @FunctionalInterface
    public interface LoadSlickImageData {
        void onEvent(Object backend, Object inputStream);
    }

    @FunctionalInterface
    public interface LoadSlickImageDataResult {
        void onEvent(Object backend, Object inputStream, Object imageData);
    }

    @FunctionalInterface
    public interface LoadImageFromStream {
        void onEvent(Object backend, Object inputStream, boolean smooth);
    }

    @FunctionalInterface
    public interface LoadImageFromStreamResult {
        void onEvent(Object backend, Object inputStream, boolean smooth, Object image);
    }

    @FunctionalInterface
    public interface CreateSlickImageFromData {
        void onEvent(Object backend, Object imageData, String name);
    }

    @FunctionalInterface
    public interface CreateSlickImageFromDataResult {
        void onEvent(Object backend, Object imageData, String name, Object image);
    }

    @FunctionalInterface
    public interface CreateImage {
        void onEvent(Object backend, int width, int height, boolean smooth, boolean bufferBacked);
    }

    @FunctionalInterface
    public interface CreateImageResult {
        void onEvent(Object backend, int width, int height, boolean smooth, boolean bufferBacked, Object image);
    }
}
