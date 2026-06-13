package io.github.endx.rustedfabricapi.api.diagnostic;

import io.github.endx.rustedfabricapi.api.util.RustedReflection;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RenderGlDiagnostics {
    private static final String[] SHADER_SOURCE_CLASSES = {
            "rustedwarfare.render.gl.ShaderSource",
            "com.corrodinggames.rts.gameFramework.b.h"
    };
    private static final String[] CIRCLE_OUTLINE_SHADER_SOURCE_CLASSES = {
            "rustedwarfare.render.gl.CircleOutlineShaderSource",
            "com.corrodinggames.rts.gameFramework.b.g"
    };
    private static final String[] GL_CANVAS_CLASSES = {
            "rustedwarfare.render.gl.GlCanvas",
            "com.corrodinggames.rts.gameFramework.b.k"
    };
    private static final String[] GLES20_CANVAS_CLASSES = {
            "rustedwarfare.render.gl.Gles20Canvas",
            "com.corrodinggames.rts.gameFramework.b.n"
    };
    private static final String[] BASIC_TEXTURE_CLASSES = {
            "rustedwarfare.render.gl.BasicTexture",
            "com.corrodinggames.rts.gameFramework.b.b"
    };
    private static final String[] RAW_TEXTURE_CLASSES = {
            "rustedwarfare.render.gl.RawTexture",
            "com.corrodinggames.rts.gameFramework.b.x"
    };
    private static final String[] UPLOADED_TEXTURE_CLASSES = {
            "rustedwarfare.render.gl.UploadedTexture",
            "com.corrodinggames.rts.gameFramework.b.ah"
    };
    private static final String[] BITMAP_TEXTURE_CLASSES = {
            "rustedwarfare.render.gl.BitmapTexture",
            "com.corrodinggames.rts.gameFramework.b.e"
    };
    private static final String[] BACKING_TEXTURE_CLASSES = {
            "rustedwarfare.render.gl.BackingTexture",
            "com.corrodinggames.rts.gameFramework.b.ad"
    };
    private static final String[] ATLAS_SUB_TEXTURE_CLASSES = {
            "rustedwarfare.render.gl.AtlasSubTexture",
            "com.corrodinggames.rts.gameFramework.b.ae"
    };
    private static final String[] TEXTURE_ATLAS_CLASSES = {
            "rustedwarfare.render.gl.TextureAtlas",
            "com.corrodinggames.rts.gameFramework.b.ac"
    };
    private static final String[] GL_RENDERER_CLASSES = {
            "rustedwarfare.render.gl.GlRenderer",
            "com.corrodinggames.rts.gameFramework.b.f"
    };
    private static final String[] GL_PAINT_CLASSES = {
            "rustedwarfare.render.gl.GlPaint",
            "com.corrodinggames.rts.gameFramework.b.v"
    };
    private static final String[] DEFERRED_TEXTURE_DELETE_QUEUE_CLASSES = {
            "rustedwarfare.render.gl.DeferredTextureDeleteQueue",
            "com.corrodinggames.rts.gameFramework.b.w"
    };
    private static final String[] GL_PROGRAM_PARAMETER_CLASSES = {
            "rustedwarfare.render.gl.GlProgramParameter",
            "com.corrodinggames.rts.gameFramework.b.q"
    };
    private static final String[] TEXTURE_PROGRAM_HANDLES_CLASSES = {
            "rustedwarfare.render.gl.TextureProgramHandles",
            "com.corrodinggames.rts.gameFramework.b.z"
    };
    private static final String[] SHAPE_PROGRAM_HANDLES_CLASSES = {
            "rustedwarfare.render.gl.ShapeProgramHandles",
            "com.corrodinggames.rts.gameFramework.b.ak"
    };
    private static final String[] TEXTURE_DRAW_BATCH_CLASSES = {
            "rustedwarfare.render.gl.TextureDrawBatch",
            "com.corrodinggames.rts.gameFramework.b.y"
    };
    private static final String[] TEXTURE_VERTEX_BUFFER_CLASSES = {
            "rustedwarfare.render.gl.TextureVertexBuffer",
            "com.corrodinggames.rts.gameFramework.b.aa"
    };
    private static final String[] SHAPE_DRAW_BATCH_CLASSES = {
            "rustedwarfare.render.gl.ShapeDrawBatch",
            "com.corrodinggames.rts.gameFramework.b.aj"
    };
    private static final String[] SHAPE_VERTEX_BUFFER_CLASSES = {
            "rustedwarfare.render.gl.ShapeVertexBuffer",
            "com.corrodinggames.rts.gameFramework.b.al"
    };
    private static final String[] GL_TEXT_CLASSES = {
            "rustedwarfare.render.gl.GlText",
            "com.corrodinggames.rts.gameFramework.b.a.b"
    };
    private static final String[] GL_TEXT_SHADER_PROGRAM_CLASSES = {
            "rustedwarfare.render.gl.GlTextShaderProgram",
            "com.corrodinggames.rts.gameFramework.b.a.a.b"
    };
    private static final String[] GL_TEXT_GLYPH_CLASSES = {
            "rustedwarfare.render.gl.GlTextGlyph",
            "com.corrodinggames.rts.gameFramework.b.a.c"
    };
    private static final String[] GL_TEXT_BATCH_CLASSES = {
            "rustedwarfare.render.gl.GlTextBatch",
            "com.corrodinggames.rts.gameFramework.b.a.d"
    };
    private static final String[] GL_TEXT_PAGE_CLASSES = {
            "rustedwarfare.render.gl.GlTextPage",
            "com.corrodinggames.rts.gameFramework.b.a.e"
    };
    private static final String[] GL_TEXT_VERTEX_BUFFER_CLASSES = {
            "rustedwarfare.render.gl.GlTextVertexBuffer",
            "com.corrodinggames.rts.gameFramework.b.a.g"
    };

    private RenderGlDiagnostics() {
    }

    public static boolean isGlCanvas(Object value) {
        return isAny(value, GL_CANVAS_CLASSES);
    }

    public static boolean isGles20Canvas(Object value) {
        return isAny(value, GLES20_CANVAS_CLASSES);
    }

    public static boolean isBasicTexture(Object value) {
        return isAny(value, BASIC_TEXTURE_CLASSES);
    }

    public static boolean isRawTexture(Object value) {
        return isAny(value, RAW_TEXTURE_CLASSES);
    }

    public static boolean isUploadedTexture(Object value) {
        return isAny(value, UPLOADED_TEXTURE_CLASSES);
    }

    public static boolean isTextureAtlas(Object value) {
        return isAny(value, TEXTURE_ATLAS_CLASSES);
    }

    public static boolean isGlText(Object value) {
        return isAny(value, GL_TEXT_CLASSES);
    }

    public static Map<String, Object> describeShaderSource(Object shaderSource) {
        requireAny(shaderSource, SHADER_SOURCE_CLASSES, "ShaderSource");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", shaderSource.getClass().getName());
        result.put("vertexShaderSource", invokeStringOrEmpty(shaderSource,
                new String[]{"getVertexShaderSource", "a"}));
        result.put("fragmentShaderSource", invokeStringOrEmpty(shaderSource,
                new String[]{"getFragmentShaderSource", "b"}));
        putOptionalFloatField(result, shaderSource, "lineWidth", new String[]{"lineWidth", "a"});
        return Collections.unmodifiableMap(result);
    }

    public static void setCircleOutlineLineWidth(Object shaderSource, float lineWidth) {
        requireAny(shaderSource, CIRCLE_OUTLINE_SHADER_SOURCE_CLASSES, "CircleOutlineShaderSource");
        RustedReflection.invokeInstance(shaderSource, new String[]{"setLineWidth", "a"}, Float.valueOf(lineWidth));
    }

    public static Map<String, Object> describeGlRenderer(Object renderer) {
        requireAny(renderer, GL_RENDERER_CLASSES, "GlRenderer");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", renderer.getClass().getName());
        putField(result, renderer, "canvas", new String[]{"canvas", "a"});
        putField(result, renderer, "bitmapTextureCache", new String[]{"bitmapTextureCache", "b"});
        putSizeField(result, renderer, "bitmapTextureCacheSize", new String[]{"bitmapTextureCache", "b"});
        putField(result, renderer, "shapeShaderSource", new String[]{"shapeShaderSource", "c"});
        putField(result, renderer, "circleShaderSource", new String[]{"circleShaderSource", "d"});
        putField(result, renderer, "defaultTextureFilter", new String[]{"defaultTextureFilter", "e"});
        return Collections.unmodifiableMap(result);
    }

    public static Object glRendererCanvas(Object renderer) {
        requireAny(renderer, GL_RENDERER_CLASSES, "GlRenderer");
        return RustedReflection.invokeInstance(renderer, new String[]{"getCanvas", "b"});
    }

    public static Map<String, Object> describeGlCanvas(Object canvas) {
        requireAny(canvas, GL_CANVAS_CLASSES, "GlCanvas");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", canvas.getClass().getName());
        result.put("gles20Canvas", Boolean.valueOf(isGles20Canvas(canvas)));
        result.put("textureIdGenerator", invokeOrNull(canvas, new String[]{"getTextureIdGenerator", "a"}));
        result.put("currentMatrix", invokeOrNull(canvas, new String[]{"getCurrentMatrix", "i"}));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeBasicTexture(Object texture) {
        requireAny(texture, BASIC_TEXTURE_CLASSES, "BasicTexture");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", texture.getClass().getName());
        result.put("rawTexture", Boolean.valueOf(isRawTexture(texture)));
        result.put("uploadedTexture", Boolean.valueOf(isUploadedTexture(texture)));
        result.put("bitmapTexture", Boolean.valueOf(isAny(texture, BITMAP_TEXTURE_CLASSES)));
        result.put("backingTexture", Boolean.valueOf(isAny(texture, BACKING_TEXTURE_CLASSES)));
        result.put("atlasSubTexture", Boolean.valueOf(isAny(texture, ATLAS_SUB_TEXTURE_CLASSES)));
        putIntField(result, texture, "textureId", new String[]{"textureId", "a"});
        putIntField(result, texture, "loadState", new String[]{"loadState", "b"});
        putIntField(result, texture, "contentWidth", new String[]{"contentWidth", "c"});
        putIntField(result, texture, "contentHeight", new String[]{"contentHeight", "d"});
        putIntField(result, texture, "textureWidth", new String[]{"textureWidth", "e"});
        putIntField(result, texture, "textureHeight", new String[]{"textureHeight", "f"});
        putFloatField(result, texture, "texelWidth", new String[]{"texelWidth", "g"});
        putFloatField(result, texture, "texelHeight", new String[]{"texelHeight", "h"});
        putBooleanField(result, texture, "hasBorder", new String[]{"hasBorder", "l"});
        putBooleanField(result, texture, "recycled", new String[]{"recycled", "m"});
        putIntField(result, texture, "lastDrawFrame", new String[]{"lastDrawFrame", "i"});
        putBooleanField(result, texture, "opaqueHint", new String[]{"opaqueHint", "j"});
        putField(result, texture, "ownerCanvas", new String[]{"ownerCanvas", "k"});
        putOptionalSizeStaticField(result, "liveTextureRegistrySize",
                BASIC_TEXTURE_CLASSES, new String[]{"liveTextureRegistry", "n"});
        result.put("target", Integer.valueOf(invokeIntOrZero(texture, new String[]{"getTarget", "g"})));
        result.put("textureFilter", Integer.valueOf(invokeIntOrZero(texture, new String[]{"getTextureFilter", "h"})));
        result.put("loaded", Boolean.valueOf(invokeBooleanOrFalse(texture, new String[]{"isLoaded", "i"})));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeRawTexture(Object texture) {
        requireAny(texture, RAW_TEXTURE_CLASSES, "RawTexture");
        Map<String, Object> result = mutableCopy(describeBasicTexture(texture));
        putBooleanField(result, texture, "opaque", new String[]{"opaque", "m"});
        putIntField(result, texture, "textureTarget", new String[]{"textureTarget", "n"});
        putBooleanField(result, texture, "contentValid", new String[]{"contentValid", "l"});
        result.put("contentValidMethod", Boolean.valueOf(invokeBooleanOrFalse(texture,
                new String[]{"isContentValid", "k"})));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeUploadedTexture(Object texture) {
        requireAny(texture, UPLOADED_TEXTURE_CLASSES, "UploadedTexture");
        Map<String, Object> result = mutableCopy(describeBasicTexture(texture));
        putField(result, texture, "bitmap", new String[]{"bitmap", "m"});
        putIntField(result, texture, "uploadFormat", new String[]{"uploadFormat", "n"});
        putSizeField(result, texture, "bitmapPoolSize", new String[]{"bitmapPool", "l"});
        putField(result, texture, "scratchPoolKey", new String[]{"scratchPoolKey", "o"});
        result.put("bitmapMethod", invokeOrNull(texture, new String[]{"getBitmap", "k"}));
        result.put("contentValidMethod", Boolean.valueOf(invokeBooleanOrFalse(texture,
                new String[]{"isContentValid", "m"})));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeBitmapTexture(Object texture) {
        requireAny(texture, BITMAP_TEXTURE_CLASSES, "BitmapTexture");
        Map<String, Object> result = mutableCopy(describeUploadedTexture(texture));
        putField(result, texture, "sourceBitmap", new String[]{"sourceBitmap", "l"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeBackingTexture(Object texture) {
        requireAny(texture, BACKING_TEXTURE_CLASSES, "BackingTexture");
        Map<String, Object> result = mutableCopy(describeBasicTexture(texture));
        putIntField(result, texture, "textureFilterField", new String[]{"textureFilter", "l"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeAtlasSubTexture(Object texture) {
        requireAny(texture, ATLAS_SUB_TEXTURE_CLASSES, "AtlasSubTexture");
        Map<String, Object> result = mutableCopy(describeBasicTexture(texture));
        putField(result, texture, "backingTexture", new String[]{"backingTexture", "l"});
        putFloatField(result, texture, "offsetU", new String[]{"offsetU", "m"});
        putFloatField(result, texture, "offsetV", new String[]{"offsetV", "n"});
        putIntField(result, texture, "atlasX", new String[]{"atlasX", "o"});
        putIntField(result, texture, "atlasY", new String[]{"atlasY", "p"});
        return Collections.unmodifiableMap(result);
    }

    public static int getTextureId(Object texture) {
        requireAny(texture, BASIC_TEXTURE_CLASSES, "BasicTexture");
        return invokeIntOrZero(texture, new String[]{"getTextureId", "a"});
    }

    public static boolean isTextureLoaded(Object texture) {
        requireAny(texture, BASIC_TEXTURE_CLASSES, "BasicTexture");
        return invokeBooleanOrFalse(texture, new String[]{"isLoaded", "i"});
    }

    public static int getTextureTarget(Object texture) {
        requireAny(texture, BASIC_TEXTURE_CLASSES, "BasicTexture");
        return invokeIntOrZero(texture, new String[]{"getTarget", "g"});
    }

    public static int getTextureFilter(Object texture) {
        requireAny(texture, BASIC_TEXTURE_CLASSES, "BasicTexture");
        return invokeIntOrZero(texture, new String[]{"getTextureFilter", "h"});
    }

    public static void setTextureFilter(Object texture, int filter) {
        requireAny(texture, BASIC_TEXTURE_CLASSES, "BasicTexture");
        RustedReflection.invokeInstance(texture, new String[]{"setTextureFilter", "b"}, Integer.valueOf(filter));
    }

    public static Map<String, Object> describeTextureAtlas(Object atlas) {
        requireAny(atlas, TEXTURE_ATLAS_CLASSES, "TextureAtlas");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", atlas.getClass().getName());
        putField(result, atlas, "canvas", new String[]{"canvas", "a"});
        putField(result, atlas, "backingTexture", new String[]{"backingTexture", "b"});
        putField(result, atlas, "emptyBitmap", new String[]{"emptyBitmap", "c"});
        putField(result, atlas, "bitmapToSubTexture", new String[]{"bitmapToSubTexture", "d"});
        putSizeField(result, atlas, "bitmapToSubTextureSize", new String[]{"bitmapToSubTexture", "d"});
        putField(result, atlas, "recentlyUsedBitmaps", new String[]{"recentlyUsedBitmaps", "e"});
        putSizeField(result, atlas, "recentlyUsedBitmapsSize", new String[]{"recentlyUsedBitmaps", "e"});
        putIntField(result, atlas, "entryCount", new String[]{"entryCount", "f"});
        putBooleanField(result, atlas, "full", new String[]{"full", "g"});
        putBooleanField(result, atlas, "trackRecentUse", new String[]{"trackRecentUse", "h"});
        putIntField(result, atlas, "cursorX", new String[]{"cursorX", "i"});
        putIntField(result, atlas, "cursorY", new String[]{"cursorY", "j"});
        putIntField(result, atlas, "rowHeight", new String[]{"rowHeight", "k"});
        putIntField(result, atlas, "padding", new String[]{"padding", "l"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeGlPaint(Object paint) {
        requireAny(paint, GL_PAINT_CLASSES, "GlPaint");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putFloatField(result, paint, "lineWidth", new String[]{"lineWidth", "a"});
        putIntField(result, paint, "color", new String[]{"color", "b"});
        putField(result, paint, "style", new String[]{"style", "c"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeDeferredTextureDeleteQueue(Object queue) {
        requireAny(queue, DEFERRED_TEXTURE_DELETE_QUEUE_CLASSES, "DeferredTextureDeleteQueue");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putArrayLengthField(result, queue, "textureIdsLength", new String[]{"textureIds", "a"});
        putIntField(result, queue, "count", new String[]{"count", "b"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeGlProgramParameter(Object parameter) {
        requireAny(parameter, GL_PROGRAM_PARAMETER_CLASSES, "GlProgramParameter");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putIntField(result, parameter, "location", new String[]{"location", "a"});
        putField(result, parameter, "name", new String[]{"name", "b"});
        putIntField(result, parameter, "cachedProgramHandle", new String[]{"cachedProgramHandle", "c"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeTextureProgramHandles(Object handles) {
        requireAny(handles, TEXTURE_PROGRAM_HANDLES_CLASSES, "TextureProgramHandles");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, handles, "positionAttribute", new String[]{"positionAttribute", "a"});
        putField(result, handles, "textureCoordinateAttribute", new String[]{"textureCoordinateAttribute", "b"});
        putField(result, handles, "colorAttribute", new String[]{"colorAttribute", "c"});
        putField(result, handles, "projectionUniform", new String[]{"projectionUniform", "d"});
        putField(result, handles, "textureUniform", new String[]{"textureUniform", "e"});
        putField(result, handles, "allParameters", new String[]{"allParameters", "f"});
        putArrayLengthField(result, handles, "allParametersLength", new String[]{"allParameters", "f"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeShapeProgramHandles(Object handles) {
        requireAny(handles, SHAPE_PROGRAM_HANDLES_CLASSES, "ShapeProgramHandles");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, handles, "positionAttribute", new String[]{"positionAttribute", "a"});
        putField(result, handles, "colorAttribute", new String[]{"colorAttribute", "b"});
        putField(result, handles, "projectionUniform", new String[]{"projectionUniform", "c"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeTextureDrawBatch(Object batch) {
        requireAny(batch, TEXTURE_DRAW_BATCH_CLASSES, "TextureDrawBatch");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, batch, "vertexBuffer", new String[]{"vertexBuffer", "a"});
        putArrayLengthField(result, batch, "verticesLength", new String[]{"vertices", "b"});
        putIntField(result, batch, "vertexFloatCount", new String[]{"vertexFloatCount", "c"});
        putIntField(result, batch, "vertexCount", new String[]{"vertexCount", "d"});
        putField(result, batch, "canvas", new String[]{"canvas", "f"});
        putField(result, batch, "shaderOrHandles", new String[]{"shaderOrHandles", "j"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeTextureVertexBuffer(Object buffer) {
        requireAny(buffer, TEXTURE_VERTEX_BUFFER_CLASSES, "TextureVertexBuffer");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, buffer, "vertexFloatBuffer", new String[]{"vertexFloatBuffer", "a"});
        putField(result, buffer, "indexBuffer", new String[]{"indexBuffer", "b"});
        putIntField(result, buffer, "indexBufferObjectId", new String[]{"indexBufferObjectId", "c"});
        putIntField(result, buffer, "uploadedFloatCount", new String[]{"uploadedFloatCount", "d"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeShapeDrawBatch(Object batch) {
        requireAny(batch, SHAPE_DRAW_BATCH_CLASSES, "ShapeDrawBatch");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, batch, "vertexBuffer", new String[]{"vertexBuffer", "a"});
        putArrayLengthField(result, batch, "verticesLength", new String[]{"vertices", "b"});
        putIntField(result, batch, "vertexFloatCount", new String[]{"vertexFloatCount", "c"});
        putIntField(result, batch, "vertexCount", new String[]{"vertexCount", "d"});
        putField(result, batch, "canvas", new String[]{"canvas", "f"});
        putField(result, batch, "shaderOrHandles", new String[]{"shaderOrHandles", "j"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeShapeVertexBuffer(Object buffer) {
        requireAny(buffer, SHAPE_VERTEX_BUFFER_CLASSES, "ShapeVertexBuffer");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, buffer, "vertexFloatBuffer", new String[]{"vertexFloatBuffer", "a"});
        putField(result, buffer, "indexBuffer", new String[]{"indexBuffer", "b"});
        putIntField(result, buffer, "indexBufferObjectId", new String[]{"indexBufferObjectId", "c"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeGlText(Object text) {
        requireAny(text, GL_TEXT_CLASSES, "GlText");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("className", text.getClass().getName());
        putField(result, text, "assetManager", new String[]{"assetManager", "a"});
        putField(result, text, "batch", new String[]{"batch", "b"});
        putField(result, text, "paint", new String[]{"paint", "r"});
        putField(result, text, "pages", new String[]{"pages", "s"});
        putSizeField(result, text, "pagesSize", new String[]{"pages", "s"});
        putArrayLengthField(result, text, "glyphPagesLength", new String[]{"glyphPages", "t"});
        putBooleanField(result, text, "loaded", new String[]{"loaded", "u"});
        putBooleanField(result, text, "debug", new String[]{"debug", "w"});
        return Collections.unmodifiableMap(result);
    }

    public static float measureText(Object text, String value) {
        requireAny(text, GL_TEXT_CLASSES, "GlText");
        Object result = RustedReflection.invokeInstance(text, new String[]{"measureText", "a"}, value);
        return result instanceof Number ? ((Number) result).floatValue() : 0.0F;
    }

    public static Map<String, Object> describeGlTextGlyph(Object glyph) {
        requireAny(glyph, GL_TEXT_GLYPH_CLASSES, "GlTextGlyph");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, glyph, "character", new String[]{"character", "a"});
        putIntField(result, glyph, "pageIndex", new String[]{"pageIndex", "b"});
        putFloatField(result, glyph, "advance", new String[]{"advance", "c"});
        putFloatField(result, glyph, "u1", new String[]{"u1", "d"});
        putFloatField(result, glyph, "v1", new String[]{"v1", "e"});
        putFloatField(result, glyph, "u2", new String[]{"u2", "f"});
        putFloatField(result, glyph, "v2", new String[]{"v2", "g"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeGlTextBatch(Object batch) {
        requireAny(batch, GL_TEXT_BATCH_CLASSES, "GlTextBatch");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, batch, "glText", new String[]{"glText", "a"});
        putField(result, batch, "vertexBuffer", new String[]{"vertexBuffer", "b"});
        putArrayLengthField(result, batch, "verticesLength", new String[]{"vertices", "c"});
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeGlTextPage(Object page) {
        requireAny(page, GL_TEXT_PAGE_CLASSES, "GlTextPage");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putField(result, page, "canvas", new String[]{"canvas", "c"});
        putField(result, page, "bitmap", new String[]{"bitmap", "d"});
        putIntField(result, page, "pageIndex", new String[]{"pageIndex", "l"});
        result.put("hasRoom", Boolean.valueOf(invokeBooleanOrFalse(page, new String[]{"hasRoom", "a"})));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeGlTextShaderProgram(Object program) {
        requireAny(program, GL_TEXT_SHADER_PROGRAM_CLASSES, "GlTextShaderProgram");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putIntField(result, program, "programHandle", new String[]{"programHandle", "a"});
        putIntField(result, program, "vertexShaderHandle", new String[]{"vertexShaderHandle", "b"});
        putIntField(result, program, "fragmentShaderHandle", new String[]{"fragmentShaderHandle", "c"});
        putBooleanField(result, program, "loaded", new String[]{"loaded", "d"});
        result.put("programHandleMethod", Integer.valueOf(invokeIntOrZero(program,
                new String[]{"getProgramHandle", "b"})));
        return Collections.unmodifiableMap(result);
    }

    public static Map<String, Object> describeGlTextVertexBuffer(Object buffer) {
        requireAny(buffer, GL_TEXT_VERTEX_BUFFER_CLASSES, "GlTextVertexBuffer");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        putIntField(result, buffer, "maxVertices", new String[]{"maxVertices", "a"});
        putIntField(result, buffer, "maxIndices", new String[]{"maxIndices", "b"});
        putIntField(result, buffer, "vertexStrideBytes", new String[]{"vertexStrideBytes", "c"});
        putField(result, buffer, "vertexBuffer", new String[]{"vertexBuffer", "d"});
        putField(result, buffer, "indexBuffer", new String[]{"indexBuffer", "e"});
        putIntField(result, buffer, "vertexCount", new String[]{"vertexCount", "f"});
        putIntField(result, buffer, "indexCount", new String[]{"indexCount", "g"});
        return Collections.unmodifiableMap(result);
    }

    private static boolean isAny(Object value, String[] classNames) {
        return value != null && RustedReflection.isAnyClass(value.getClass(), classNames);
    }

    private static void requireAny(Object value, String[] classNames, String label) {
        if (value == null || !RustedReflection.isAnyClass(value.getClass(), classNames)) {
            throw new IllegalArgumentException("Expected " + label + ", got " + describe(value));
        }
    }

    private static String describe(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }

    private static Map<String, Object> mutableCopy(Map<String, Object> source) {
        return new LinkedHashMap<String, Object>(source);
    }

    private static Object invokeOrNull(Object owner, String[] methodNames, Object... args) {
        try {
            return RustedReflection.invokeInstance(owner, methodNames, args);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String invokeStringOrEmpty(Object owner, String[] methodNames, Object... args) {
        Object value = invokeOrNull(owner, methodNames, args);
        return value != null ? value.toString() : "";
    }

    private static int invokeIntOrZero(Object owner, String[] methodNames, Object... args) {
        Object value = invokeOrNull(owner, methodNames, args);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private static boolean invokeBooleanOrFalse(Object owner, String[] methodNames, Object... args) {
        return Boolean.TRUE.equals(invokeOrNull(owner, methodNames, args));
    }

    private static int sizeOf(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Collection) {
            return ((Collection<?>) value).size();
        }
        if (value instanceof Map) {
            return ((Map<?, ?>) value).size();
        }
        if (value.getClass().isArray()) {
            return Array.getLength(value);
        }
        return 1;
    }

    private static int arrayLength(Object array) {
        return array != null && array.getClass().isArray() ? Array.getLength(array) : 0;
    }

    private static void putField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, RustedReflection.getFieldValue(owner, fieldNames));
    }

    private static void putOptionalFloatField(Map<String, Object> result, Object owner, String key,
                                              String[] fieldNames) {
        try {
            result.put(key, Float.valueOf(RustedReflection.getFloatField(owner, fieldNames)));
        } catch (RuntimeException ignored) {
        }
    }

    private static void putIntField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Integer.valueOf(RustedReflection.getIntField(owner, fieldNames)));
    }

    private static void putFloatField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Float.valueOf(RustedReflection.getFloatField(owner, fieldNames)));
    }

    private static void putBooleanField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Boolean.valueOf(RustedReflection.getBooleanField(owner, fieldNames)));
    }

    private static void putSizeField(Map<String, Object> result, Object owner, String key, String[] fieldNames) {
        result.put(key, Integer.valueOf(sizeOf(RustedReflection.getFieldValue(owner, fieldNames))));
    }

    private static void putArrayLengthField(Map<String, Object> result, Object owner, String key,
                                            String[] fieldNames) {
        result.put(key, Integer.valueOf(arrayLength(RustedReflection.getFieldValue(owner, fieldNames))));
    }

    private static void putOptionalSizeStaticField(Map<String, Object> result, String key,
                                                   String[] classNames, String[] fieldNames) {
        try {
            result.put(key, Integer.valueOf(sizeOf(RustedReflection.getStaticFieldValue(classNames, fieldNames))));
        } catch (RuntimeException ignored) {
        }
    }
}
