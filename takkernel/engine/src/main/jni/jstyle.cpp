#include "com_atakmap_map_layer_feature_style_Style.h"
#include "com_atakmap_map_layer_feature_style_BasicStrokeStyle.h"
#include "com_atakmap_map_layer_feature_ogr_style_FeatureStyleParser.h"

#include <feature/Style.h>
#include <util/Memory.h>

#include "common.h"
#include "interop/JNIStringUTF.h"
#include "interop/JNIFloatArray.h"
#include "interop/JNILongArray.h"
#include "interop/Pointer.h"

using namespace TAK::Engine::Util;

using namespace atakmap::feature;

using namespace TAKEngineJNI::Interop;

JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getTESC_1BasicStrokeStyle
  (JNIEnv *env, jclass clazz)
{
    return TESC_BasicStrokeStyle;
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getTESC_1BasicFillStyle
  (JNIEnv *env, jclass clazz)
{
    return TESC_BasicFillStyle;
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getTESC_1BasicPointStyle
  (JNIEnv *env, jclass clazz)
{
    return TESC_BasicPointStyle;
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getTESC_1IconPointStyle
  (JNIEnv *env, jclass clazz)
{
    return TESC_IconPointStyle;
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getTESC_1MeshPointStyle
        (JNIEnv *env, jclass clazz)
{
    return TESC_MeshPointStyle;
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getTESC_1LabelPointStyle
  (JNIEnv *env, jclass clazz)
{
    return TESC_LabelPointStyle;
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getTESC_1CompositeStyle
  (JNIEnv *env, jclass clazz)
{
    return TESC_CompositeStyle;
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getTESC_1PatternStrokeStyle
  (JNIEnv *env, jclass clazz)
{
    return TESC_PatternStrokeStyle;
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getTESC_1LevelOfDetailStyle
        (JNIEnv *env, jclass clazz)
{
    return TESC_LevelOfDetailStyle;
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getTESC_1ArrowStrokeStyle
        (JNIEnv *env, jclass clazz)
{
    return TESC_ArrowStrokeStyle;
}

/*****************************************************************************/
// Style

JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_style_Style_destruct
  (JNIEnv *env, jclass clazz, jobject pointer)
{
    Pointer_destruct_iface<Style>(env, pointer);
}
JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_feature_style_Style_clone
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    Style *style = JLONG_TO_INTPTR(Style, ptr);
    if(!style)
        return NULL;
    StylePtr retval(style->clone(), Style::destructStyle);
    return NewPointer(env, std::move(retval));
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getClass
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    Style *style = JLONG_TO_INTPTR(Style, ptr);
    if(!style) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return 0;
    }
    return style->getClass();
}
JNIEXPORT jboolean JNICALL Java_com_atakmap_map_layer_feature_style_Style_equals
  (JNIEnv *env, jclass clazz, jlong aPtr, jlong bPtr)
{
    const auto a = JLONG_TO_INTPTR(const Style, aPtr);
    const auto b = JLONG_TO_INTPTR(const Style, bPtr);
    if(a == b) // identity and both `nullptr`
        return true;
    else if(!a || !b)
        return false;
    return *a == *b;
}

#define STYLE_IMPL_ACCESSOR_BODY(sc, acc) \
    Style *style = JLONG_TO_INTPTR(Style, ptr); \
    if(!style || style->getClass() != TESC_##sc) { \
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg); \
        return 0; \
    } \
    const sc &impl = static_cast<sc &>(*style); \
    return impl.acc();
    
/*****************************************************************************/
// BasicFillStyle

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_feature_style_Style_BasicFillStyle_1create
  (JNIEnv *env, jclass clazz, jint color)
{
    TAKErr code(TE_Ok);
    StylePtr retval(nullptr, nullptr);
    code = BasicFillStyle_create(retval, color);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    return NewPointer(env, std::move(retval));
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_BasicFillStyle_1getColor
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(BasicFillStyle, getColor)
}

/*****************************************************************************/
// BasicStrokeStyle

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_feature_style_Style_BasicStrokeStyle_1create
  (JNIEnv *env, jclass clazz, jint color, jfloat strokeWidth, jint mextrudeMode)
{
    TAKErr code(TE_Ok);
    StylePtr retval(nullptr, nullptr);
    atakmap::feature::StrokeExtrusionMode cextrudeMode = atakmap::feature::TEEM_Vertex;
    switch(mextrudeMode) {
        case com_atakmap_map_layer_feature_style_BasicStrokeStyle_EXTRUDE_VERTEX :
            cextrudeMode = atakmap::feature::TEEM_Vertex;
            break;
        case com_atakmap_map_layer_feature_style_BasicStrokeStyle_EXTRUDE_ENDPOINT :
            cextrudeMode = atakmap::feature::TEEM_EndPoint;
            break;
        case com_atakmap_map_layer_feature_style_BasicStrokeStyle_EXTRUDE_NONE :
            cextrudeMode = atakmap::feature::TEEM_None;
            break;
        default :
            cextrudeMode = atakmap::feature::TEEM_Vertex;
            break;
    }
    code = BasicStrokeStyle_create(retval, color, strokeWidth, cextrudeMode);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    return NewPointer(env, std::move(retval));
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_BasicStrokeStyle_1getColor
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(BasicStrokeStyle, getColor)
}
JNIEXPORT jfloat JNICALL Java_com_atakmap_map_layer_feature_style_Style_BasicStrokeStyle_1getStrokeWidth
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(BasicStrokeStyle, getStrokeWidth)
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_BasicStrokeStyle_1getExtrudeMode
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    auto *cpattern = JLONG_TO_INTPTR(BasicStrokeStyle, ptr);
    if(!cpattern)
        return com_atakmap_map_layer_feature_style_BasicStrokeStyle_EXTRUDE_VERTEX;
    switch(cpattern->getExtrusionMode()) {
        case atakmap::feature::TEEM_Vertex :
            return com_atakmap_map_layer_feature_style_BasicStrokeStyle_EXTRUDE_VERTEX;
        case atakmap::feature::TEEM_EndPoint :
            return com_atakmap_map_layer_feature_style_BasicStrokeStyle_EXTRUDE_ENDPOINT;
        case atakmap::feature::TEEM_None :
            return com_atakmap_map_layer_feature_style_BasicStrokeStyle_EXTRUDE_NONE;
        default :
            return com_atakmap_map_layer_feature_style_BasicStrokeStyle_EXTRUDE_VERTEX;
    }
}

/*****************************************************************************/
// BasicPointStyle

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_feature_style_Style_BasicPointStyle_1create
  (JNIEnv *env, jclass clazz, jint color, jfloat size)
{
    TAKErr code(TE_Ok);
    StylePtr retval(nullptr, nullptr);
    code = BasicPointStyle_create(retval, color, size);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    return NewPointer(env, std::move(retval));
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_BasicPointStyle_1getColor
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(BasicPointStyle, getColor)
}
JNIEXPORT jfloat JNICALL Java_com_atakmap_map_layer_feature_style_Style_BasicPointStyle_1getSize
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(BasicPointStyle, getSize)
}

/*****************************************************************************/
// IconPointStyle

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_feature_style_Style_IconPointStyle_1create__ILjava_lang_String_2FFFFIIFZ
  (JNIEnv *env, jclass clazz, jint color, jstring juri, jfloat width, jfloat height, jfloat offsetX, jfloat offsetY, jint halign, jint valign, jfloat rotation, jboolean isRotationAbsolute)
{
    TAKErr code(TE_Ok);
    JNIStringUTF uri(*env, juri);
    StylePtr retval(nullptr, nullptr);
    code = IconPointStyle_create(retval, color,
                                         uri,
                                         width,
                                         height,
                                         offsetX, offsetY,
                                         (IconPointStyle::HorizontalAlignment)halign,
                                         (IconPointStyle::VerticalAlignment)valign,
                                         rotation,
                                         isRotationAbsolute);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    return NewPointer(env, std::move(retval));
}
JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_feature_style_Style_IconPointStyle_1create__ILjava_lang_String_2FIIFZ
        (JNIEnv *env, jclass clazz, jint color, jstring juri, jfloat scale, jint halign, jint valign, jfloat rotation, jboolean isRotationAbsolute)
{
    TAKErr code(TE_Ok);
    JNIStringUTF uri(*env, juri);
    StylePtr retval(nullptr, nullptr);
    code = IconPointStyle_create(retval, color,
                                 uri,
                                 scale,
                                 (IconPointStyle::HorizontalAlignment)halign,
                                 (IconPointStyle::VerticalAlignment)valign,
                                 rotation,
                                 isRotationAbsolute);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    return NewPointer(env, std::move(retval));
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_IconPointStyle_1getColor
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(IconPointStyle, getColor)
}
JNIEXPORT jstring JNICALL Java_com_atakmap_map_layer_feature_style_Style_IconPointStyle_1getUri
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    Style *style = JLONG_TO_INTPTR(Style, ptr);
    if(!style || style->getClass() != TESC_IconPointStyle) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return NULL;
    }
    const IconPointStyle &impl = static_cast<IconPointStyle &>(*style);
    return env->NewStringUTF(impl.getIconURI());
}
JNIEXPORT jfloat JNICALL Java_com_atakmap_map_layer_feature_style_Style_IconPointStyle_1getWidth
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(IconPointStyle, getWidth)
}
JNIEXPORT jfloat JNICALL Java_com_atakmap_map_layer_feature_style_Style_IconPointStyle_1getHeight
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(IconPointStyle, getHeight)
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_IconPointStyle_1getHorizontalAlignment
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(IconPointStyle, getHorizontalAlignment)
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_IconPointStyle_1getVerticalAlignment
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(IconPointStyle, getVerticalAlignment)
}
JNIEXPORT jfloat JNICALL Java_com_atakmap_map_layer_feature_style_Style_IconPointStyle_1getRotation
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(IconPointStyle, getRotation)
}
JNIEXPORT jfloat JNICALL Java_com_atakmap_map_layer_feature_style_Style_IconPointStyle_1getScaling
        (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(IconPointStyle, getScaling)
}
JNIEXPORT jboolean JNICALL Java_com_atakmap_map_layer_feature_style_Style_IconPointStyle_1isRotationAbsolute
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(IconPointStyle, isRotationAbsolute)
}
JNIEXPORT jfloat JNICALL Java_com_atakmap_map_layer_feature_style_Style_IconPointStyle_1getIconOffsetX
        (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(IconPointStyle, getOffsetX)
}
JNIEXPORT jfloat JNICALL Java_com_atakmap_map_layer_feature_style_Style_IconPointStyle_1getIconOffsetY
        (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(IconPointStyle, getOffsetY)
}

JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getIconPointStyle_1HorizontalAlignment_1LEFT
  (JNIEnv *env, jclass clazz)
{
    return IconPointStyle::LEFT;
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getIconPointStyle_1HorizontalAlignment_1H_1CENTER
  (JNIEnv *env, jclass clazz)
{
    return IconPointStyle::H_CENTER;
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getIconPointStyle_1HorizontalAlignment_1RIGHT
  (JNIEnv *env, jclass clazz)
{
    return IconPointStyle::RIGHT;
}

JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getIconPointStyle_1VerticalAlignment_1ABOVE
  (JNIEnv *env, jclass clazz)
{
    return IconPointStyle::ABOVE;
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getIconPointStyle_1VerticalAlignment_1V_1CENTER
  (JNIEnv *env, jclass clazz)
{
    return IconPointStyle::V_CENTER;
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getIconPointStyle_1VerticalAlignment_1BELOW
  (JNIEnv *env, jclass clazz)
{
    return IconPointStyle::BELOW;
}

/*****************************************************************************/
// MeshPointStyle

extern "C"
JNIEXPORT jobject JNICALL
Java_com_atakmap_map_layer_feature_style_Style_MeshPointStyle_1create(JNIEnv *env, jclass clazz,
                                                                      jstring juri, jint color,
                                                                      jfloatArray transform)
{
    TAKErr code(TE_Ok);
    JNIStringUTF uri(*env, juri);
    StylePtr retval(nullptr, nullptr);
    JNIFloatArray transformFloatArray(*env, transform, 0);
    if (transformFloatArray.length() != 16) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return NULL;
    }
    code = MeshPointStyle_create(retval, uri, color, transformFloatArray);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    return NewPointer(env, std::move(retval));
}
extern "C"
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_MeshPointStyle_1getColor
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(MeshPointStyle, getColor)
}
extern "C"
JNIEXPORT jstring JNICALL Java_com_atakmap_map_layer_feature_style_Style_MeshPointStyle_1getUri
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    Style *style = JLONG_TO_INTPTR(Style, ptr);
    if(!style || style->getClass() != TESC_MeshPointStyle) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return NULL;
    }
    const MeshPointStyle &impl = static_cast<MeshPointStyle &>(*style);
    return env->NewStringUTF(impl.getMeshURI());
}
extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_atakmap_map_layer_feature_style_Style_MeshPointStyle_1getTransform
        (JNIEnv *env, jclass clazz, jlong ptr)
{
    Style *style = JLONG_TO_INTPTR(Style, ptr);
    if(!style || style->getClass() != TESC_MeshPointStyle) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return NULL;
    }
    const MeshPointStyle &impl = static_cast<MeshPointStyle &>(*style);
    return JNIFloatArray_newFloatArray(env, impl.getTransform(), 16);
}

/*****************************************************************************/
// LabelPointStyle

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_feature_style_Style_LabelPointStyle_1create
  (JNIEnv *env, jclass clazz, jstring jtext, jint color, jint bgColor, jint scrollMode, jstring jfontFace, jfloat size, jint style, jfloat offsetX, jfloat offsetY, jint halign, jint valign, jfloat rotation, jboolean isRotationAbsolute, jdouble labelMinRenderResolution, jfloat labelScale)
{
    TAKErr code(TE_Ok);
    JNIStringUTF text(*env, jtext);
    JNIStringUTF fontFace(*env, jfontFace);
    StylePtr retval(nullptr, nullptr);

    code =  LabelPointStyle_create(retval, text,
                                           color,
                                           bgColor,
                                           (LabelPointStyle::ScrollMode)scrollMode,
                                           fontFace,
                                           size,
                                           style,
                                           offsetX, offsetY,
                                           (LabelPointStyle::HorizontalAlignment)halign,
                                           (LabelPointStyle::VerticalAlignment)valign,
                                           rotation,
                                           isRotationAbsolute, 0.0, 0.0,
                                           labelMinRenderResolution,
                                           labelScale);

    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    return NewPointer(env, std::move(retval));
}
JNIEXPORT jstring JNICALL Java_com_atakmap_map_layer_feature_style_Style_LabelPointStyle_1getText
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    Style *style = JLONG_TO_INTPTR(Style, ptr);
    if(!style || style->getClass() != TESC_LabelPointStyle) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return NULL;
    }
    const LabelPointStyle &impl = static_cast<LabelPointStyle &>(*style);
    return env->NewStringUTF(impl.getText());
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_LabelPointStyle_1getTextColor
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(LabelPointStyle, getTextColor)
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_LabelPointStyle_1getBackgroundColor
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(LabelPointStyle, getBackgroundColor)
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_LabelPointStyle_1getScrollMode
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(LabelPointStyle, getScrollMode)
}
JNIEXPORT jfloat JNICALL Java_com_atakmap_map_layer_feature_style_Style_LabelPointStyle_1getTextSize
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(LabelPointStyle, getTextSize)
}
JNIEXPORT jdouble JNICALL Java_com_atakmap_map_layer_feature_style_Style_LabelPointStyle_1getLabelMinRenderResolution
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(LabelPointStyle, getLabelMinRenderResolution)
}
JNIEXPORT jfloat JNICALL Java_com_atakmap_map_layer_feature_style_Style_LabelPointStyle_1getLabelScale
        (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(LabelPointStyle, getLabelScale)
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_LabelPointStyle_1getHorizontalAlignment
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(LabelPointStyle, getHorizontalAlignment)
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_LabelPointStyle_1getVerticalAlignment
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(LabelPointStyle, getVerticalAlignment)
}
JNIEXPORT jfloat JNICALL Java_com_atakmap_map_layer_feature_style_Style_LabelPointStyle_1getRotation
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(LabelPointStyle, getRotation)
}
JNIEXPORT jboolean JNICALL Java_com_atakmap_map_layer_feature_style_Style_LabelPointStyle_1isRotationAbsolute
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(LabelPointStyle, isRotationAbsolute)
}
JNIEXPORT jfloat JNICALL Java_com_atakmap_map_layer_feature_style_Style_LabelPointStyle_1getOffsetX
        (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(LabelPointStyle, getOffsetX)
}
JNIEXPORT jfloat JNICALL Java_com_atakmap_map_layer_feature_style_Style_LabelPointStyle_1getOffsetY
        (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(LabelPointStyle, getOffsetY)
}
JNIEXPORT jstring JNICALL Java_com_atakmap_map_layer_feature_style_Style_LabelPointStyle_1getFontFace
        (JNIEnv *env, jclass clazz, jlong ptr)
{
    Style *style = JLONG_TO_INTPTR(Style, ptr);
    if(!style || style->getClass() != TESC_LabelPointStyle) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return NULL;
    }
    const LabelPointStyle &impl = static_cast<LabelPointStyle &>(*style);
    return env->NewStringUTF(impl.getFontFace());
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_LabelPointStyle_1getStyle
        (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(LabelPointStyle, getStyle)
}

JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getLabelPointStyle_1HorizontalAlignment_1LEFT
  (JNIEnv *env, jclass clazz)
{
    return LabelPointStyle::LEFT;
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getLabelPointStyle_1HorizontalAlignment_1H_1CENTER
  (JNIEnv *env, jclass clazz)
{
    return LabelPointStyle::H_CENTER;
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getLabelPointStyle_1HorizontalAlignment_1RIGHT
  (JNIEnv *env, jclass clazz)
{
    return LabelPointStyle::RIGHT;
}

JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getLabelPointStyle_1VerticalAlignment_1ABOVE
  (JNIEnv *env, jclass clazz)
{
    return LabelPointStyle::ABOVE;
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getLabelPointStyle_1VerticalAlignment_1V_1CENTER
  (JNIEnv *env, jclass clazz)
{
    return LabelPointStyle::V_CENTER;
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getLabelPointStyle_1VerticalAlignment_1BELOW
  (JNIEnv *env, jclass clazz)
{
    return LabelPointStyle::BELOW;
}

JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getLabelPointStyle_1ScrollMode_1DEFAULT
  (JNIEnv *env, jclass clazz)
{
    return LabelPointStyle::DEFAULT;
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getLabelPointStyle_1ScrollMode_1ON
  (JNIEnv *env, jclass clazz)
{
    return LabelPointStyle::ON;
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getLabelPointStyle_1ScrollMode_1OFF
  (JNIEnv *env, jclass clazz)
{
    return LabelPointStyle::OFF;
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getLabelPointStyle_1Style_1BOLD
        (JNIEnv *env, jclass clazz)
{
    return LabelPointStyle::BOLD;
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getLabelPointStyle_1Style_1ITALIC
        (JNIEnv *env, jclass clazz)
{
    return LabelPointStyle::ITALIC;
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getLabelPointStyle_1Style_1UNDERLINE
        (JNIEnv *env, jclass clazz)
{
    return LabelPointStyle::UNDERLINE;
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getLabelPointStyle_1Style_1STRIKETHROUGH
        (JNIEnv *env, jclass clazz)
{
    return LabelPointStyle::STRIKETHROUGH;
}

/*****************************************************************************/
// CompositeStyle

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_feature_style_Style_CompositeStyle_1create
  (JNIEnv *env, jclass clazz, jlongArray jstylePtrs)
{
    if(!jstylePtrs) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return NULL;
    }

    TAKErr code(TE_Ok);
    JNILongArray stylePtrs(*env, jstylePtrs, JNI_ABORT);

    std::vector<const Style *> styles;
    styles.reserve(stylePtrs.length());

    for(std::size_t i = 0u; i < stylePtrs.length(); i++) {
        Style *s = JLONG_TO_INTPTR(Style, stylePtrs[i]);
        styles.push_back(s);
    }

    StylePtr retval(nullptr, nullptr);
    code = CompositeStyle_create(retval, styles.empty() ? nullptr : &styles.at(0), styles.size());
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    return NewPointer(env, std::move(retval));
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_CompositeStyle_1getNumStyles
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(CompositeStyle, getStyleCount)
}
JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_feature_style_Style_CompositeStyle_1getStyle
  (JNIEnv *env, jclass clazz, jlong ptr, jint idx)
{
    Style *style = JLONG_TO_INTPTR(Style, ptr);
    if(!style || style->getClass() != TESC_CompositeStyle) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return NULL;
    }
    const CompositeStyle &impl = static_cast<CompositeStyle &>(*style);
    try {
        const Style &s = impl.getStyle(idx);
        StylePtr retval(s.clone(), Style::destructStyle);
        return NewPointer(env, std::move(retval));
    } catch(...) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_Err);
        return NULL;
    }
}

/*****************************************************************************/
// PatternStrokeStyle


JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_feature_style_Style_PatternStrokeStyle_1create
  (JNIEnv *env, jclass clazz, jint factor, jshort pattern, jint color, jfloat width, jint mextrudeMode)
{
    TAKErr code(TE_Ok);
    StylePtr retval(nullptr, nullptr);
    atakmap::feature::StrokeExtrusionMode cextrudeMode = atakmap::feature::TEEM_Vertex;
    switch(mextrudeMode) {
        case com_atakmap_map_layer_feature_style_BasicStrokeStyle_EXTRUDE_VERTEX :
            cextrudeMode = atakmap::feature::TEEM_Vertex;
            break;
        case com_atakmap_map_layer_feature_style_BasicStrokeStyle_EXTRUDE_ENDPOINT :
            cextrudeMode = atakmap::feature::TEEM_EndPoint;
            break;
        case com_atakmap_map_layer_feature_style_BasicStrokeStyle_EXTRUDE_NONE :
            cextrudeMode = atakmap::feature::TEEM_None;
            break;
        default :
            cextrudeMode = atakmap::feature::TEEM_Vertex;
            break;
    }
    code = PatternStrokeStyle_create(retval, factor, pattern, color, width, cextrudeMode);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    return NewPointer(env, std::move(retval));
}
JNIEXPORT jshort JNICALL Java_com_atakmap_map_layer_feature_style_Style_PatternStrokeStyle_1getPattern
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(PatternStrokeStyle, getPattern)
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_PatternStrokeStyle_1getFactor
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(PatternStrokeStyle, getFactor)
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_PatternStrokeStyle_1getColor
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(PatternStrokeStyle, getColor)
}
JNIEXPORT jfloat JNICALL Java_com_atakmap_map_layer_feature_style_Style_PatternStrokeStyle_1getStrokeWidth
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(PatternStrokeStyle, getStrokeWidth)
}

JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_PatternStrokeStyle_1getExtrudeMode
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    auto *cpattern = JLONG_TO_INTPTR(PatternStrokeStyle, ptr);
    if(!cpattern)
        return com_atakmap_map_layer_feature_style_BasicStrokeStyle_EXTRUDE_VERTEX;
    switch(cpattern->getExtrusionMode()) {
        case atakmap::feature::TEEM_Vertex :
            return com_atakmap_map_layer_feature_style_BasicStrokeStyle_EXTRUDE_VERTEX;
        case atakmap::feature::TEEM_EndPoint :
            return com_atakmap_map_layer_feature_style_BasicStrokeStyle_EXTRUDE_ENDPOINT;
        case atakmap::feature::TEEM_None :
            return com_atakmap_map_layer_feature_style_BasicStrokeStyle_EXTRUDE_NONE;
        default :
            return com_atakmap_map_layer_feature_style_BasicStrokeStyle_EXTRUDE_VERTEX;
    }
}

/*****************************************************************************/
// LevelOfDetailStyle

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_feature_style_Style_LevelOfDetailStyle_1create
        (JNIEnv *env, jclass clazz, jlong jstylePtr, jint minLod, jint maxLod)
{
    TAKErr code(TE_Ok);

    Style *s = JLONG_TO_INTPTR(Style, jstylePtr);

    StylePtr retval(nullptr, nullptr);
    code = LevelOfDetailStyle_create(retval, *s, (const std::size_t)minLod, (const std::size_t)maxLod);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    return NewPointer(env, std::move(retval));
}
JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_feature_style_Style_LevelOfDetailStyle_1getStyle
        (JNIEnv *env, jclass clazz, jlong ptr)
{
    Style *style = JLONG_TO_INTPTR(Style, ptr);
    if(!style || style->getClass() != TESC_LevelOfDetailStyle) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return NULL;
    }
    const LevelOfDetailStyle &impl = static_cast<LevelOfDetailStyle &>(*style);
    try {
        const Style &s = impl.getStyle();
        return NewReference(env, s);
    } catch(...) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_Err);
        return NULL;
    }
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_LevelOfDetailStyle_1getMinLod
        (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(LevelOfDetailStyle, getMinLevelOfDetail)
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_LevelOfDetailStyle_1getMaxLod
        (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(LevelOfDetailStyle, getMaxLevelOfDetail)
}


/*****************************************************************************/
// ArrowStrokeStyle

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_feature_style_Style_ArrowStrokeStyle_1create
        (JNIEnv *env, jclass clazz, jfloat arrowRadius, jint factor, jshort pattern, jint color, jfloat width, jint mextrudeMode, jint arrowHeadMode)
{
    TAKErr code(TE_Ok);

    StylePtr retval(nullptr, nullptr);
    atakmap::feature::StrokeExtrusionMode cextrudeMode;
    switch(mextrudeMode) {
        case com_atakmap_map_layer_feature_style_BasicStrokeStyle_EXTRUDE_VERTEX :
            cextrudeMode = atakmap::feature::TEEM_Vertex;
            break;
        case com_atakmap_map_layer_feature_style_BasicStrokeStyle_EXTRUDE_ENDPOINT :
            cextrudeMode = atakmap::feature::TEEM_EndPoint;
            break;
        case com_atakmap_map_layer_feature_style_BasicStrokeStyle_EXTRUDE_NONE :
            cextrudeMode = atakmap::feature::TEEM_None;
            break;
        default :
            cextrudeMode = atakmap::feature::TEEM_Vertex;
            break;
    }
    code = ArrowStrokeStyle_create(retval, arrowRadius, factor, pattern, color, width, cextrudeMode, (ArrowHeadMode) arrowHeadMode);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    return NewPointer(env, std::move(retval));
}
JNIEXPORT jfloat JNICALL Java_com_atakmap_map_layer_feature_style_Style_ArrowStrokeStyle_1getArrowRadius
        (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(ArrowStrokeStyle, getArrowRadius)
}
JNIEXPORT jshort JNICALL Java_com_atakmap_map_layer_feature_style_Style_ArrowStrokeStyle_1getPattern
        (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(ArrowStrokeStyle, getPattern)
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_ArrowStrokeStyle_1getFactor
        (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(ArrowStrokeStyle, getFactor)
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_ArrowStrokeStyle_1getColor
        (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(ArrowStrokeStyle, getColor)
}
JNIEXPORT jfloat JNICALL Java_com_atakmap_map_layer_feature_style_Style_ArrowStrokeStyle_1getStrokeWidth
        (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(ArrowStrokeStyle, getStrokeWidth)
}

JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_ArrowStrokeStyle_1getExtrudeMode
        (JNIEnv *env, jclass clazz, jlong ptr)
{
    auto *cpattern = JLONG_TO_INTPTR(ArrowStrokeStyle, ptr);
    if(!cpattern)
        return com_atakmap_map_layer_feature_style_BasicStrokeStyle_EXTRUDE_VERTEX;
    switch(cpattern->getExtrusionMode()) {
        case atakmap::feature::TEEM_Vertex :
            return com_atakmap_map_layer_feature_style_BasicStrokeStyle_EXTRUDE_VERTEX;
        case atakmap::feature::TEEM_EndPoint :
            return com_atakmap_map_layer_feature_style_BasicStrokeStyle_EXTRUDE_ENDPOINT;
        case atakmap::feature::TEEM_None :
            return com_atakmap_map_layer_feature_style_BasicStrokeStyle_EXTRUDE_NONE;
        default :
            return com_atakmap_map_layer_feature_style_BasicStrokeStyle_EXTRUDE_VERTEX;
    }
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_ArrowStrokeStyle_1getArrowHeadMode
        (JNIEnv *env, jclass clazz, jlong ptr)
{
    STYLE_IMPL_ACCESSOR_BODY(ArrowStrokeStyle, getArrowHeadMode)
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getArrowStrokeStyle_1ArrowHeadMode_1ONLYLAST
        (JNIEnv *env, jclass clazz)
{
    return ArrowHeadMode::TEAH_OnlyLast;
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_style_Style_getArrowStrokeStyle_1ArrowHeadMode_1PERVERTEX
        (JNIEnv *env, jclass clazz)
{
    return ArrowHeadMode::TEAH_PerVertex;
}


/*************************************************************************************************/
// FeatueStyleParser

JNIEXPORT jstring JNICALL Java_com_atakmap_map_layer_feature_ogr_style_FeatureStyleParser_packStyle
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    const auto cstyle = JLONG_TO_INTPTR(Style, ptr);
    if(!cstyle)
        return nullptr;
    TAK::Engine::Port::String cstyleStr;
    cstyle->toOGR(cstyleStr);
    return cstyleStr ? env->NewStringUTF(cstyleStr) : nullptr;
}
JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_feature_ogr_style_FeatureStyleParser_parseStyle
  (JNIEnv *env, jclass clazz, jstring mstyleStr)
{
    if(!mstyleStr)
        return nullptr;
    TAK::Engine::Port::String cstyleStr;
    JNIStringUTF_get(cstyleStr, *env, mstyleStr);
    StylePtr cstyle(nullptr, nullptr);
    Style_parseStyle(cstyle, cstyleStr);
    return cstyle ? NewPointer(env, std::move(cstyle)) : nullptr;
}
