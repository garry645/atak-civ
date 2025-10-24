package com.atakmap.lang;

import android.os.Build;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import com.atakmap.coremap.log.Log;

final class UnsafePlatform
{
    private final static String TAG = "UnsafePlatform";

    private static Unsafe.DirectByteBufferApi[] apis =
            {
                    new DirectByteBuffer18(),
                    new DirectByteBuffer23(),
                    new DirectByteBuffer31(),
            };

    private UnsafePlatform() {}

    static Unsafe.DirectByteBufferApi[] directByteBufferApi()
    {
        return apis;
    }

    /**
     * DirectByteBuffer implementation detail for Android 12 (api 31).
     * (should be good back to pie, but an alternate/proven method already exists for pie - 11).
     * In these versions, DirectByteBuffer built using JNI NewDirectByteBuffer() (as we do in
     * outer allocator) ties cleanup of the underlying buffer to the original
     * DirectByteBuffer instance, regardless of how many times said original Buffer is
     * sliced or otherwise used as a window into its internal buffer via other ByteBuffer instances.
     * The internal shared tracking holds open a strong reference to the original DirectByteBuffer
     * and clears it once the child/windowed instances have all ceased utilization.
     * This is in contrast to earlier versions, where no strong ref was held and instead it required
     * reflection acrobatics to get to the internal object (memory ref) that was shared across
     * the various views into the direct buffer memory so that cleanup could be held off until
     * all users were finished.
     */
    private static class DirectByteBuffer31 extends Unsafe.DirectByteBufferApi
    {

        DirectByteBuffer31()
        {
            super("Android 31");
        }

        @Override
        protected boolean isSupportedImpl() throws Throwable
        {
            if (Build.VERSION.SDK_INT < 31)
                return false;

            return true;
        }

        @Override
        public Object getTrackingField(ByteBuffer buffer)
        {
            if (!supported || !DirectByteBuffer_class.isInstance(buffer))
                return null;
            return buffer;
        }

        @Override
        protected void freeImpl(ByteBuffer buffer) throws Throwable
        {
        }

        @Override
        protected Field getByteBufferField(Buffer buffer) throws Throwable
        {
            return null;
        }
    }

    /**
     * DirectByteBuffer implementation detail for Android 23
     */
    private static class DirectByteBuffer23 extends Unsafe.DirectByteBufferApi
    {
        Field DirectByteBuffer_memoryRef = null;
        Method DirectByteBuffer_cleaner = null;
        Class Cleaner_class = null;
        Method Cleaner_clean = null;

        DirectByteBuffer23()
        {
            super("Android 23");
        }

        @Override
        protected boolean isSupportedImpl() throws Throwable
        {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            {
                DirectByteBuffer_memoryRef = DirectByteBuffer_class.getDeclaredField("memoryRef");
            } else
            {
                // the blacklist only checks to see the calling function and in this case
                // the reflection a second time makes the calling function is from the
                // system and not from this application.   Warn users for future SDK's
                // that this might not work when running debug versions - so it can be
                // checked.
                final Method xgetDeclaredField = Class.class
                        .getDeclaredMethod("getDeclaredField",
                                String.class);
                DirectByteBuffer_memoryRef = (Field) xgetDeclaredField.invoke(DirectByteBuffer_class,
                        "memoryRef");
            }
            if (DirectByteBuffer_memoryRef == null)
                return false;
            DirectByteBuffer_memoryRef.setAccessible(true);
            if (!checkField(DirectByteBuffer_memoryRef))
                return false;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            {
                DirectByteBuffer_cleaner = DirectByteBuffer_class.getDeclaredMethod("cleaner");
            } else
            {
                // the blacklist only checks to see the calling function and in this case
                // the reflection a second time makes the calling function is from the
                // system and not from this application.   Warn users for future SDK's
                // that this might not work when running debug versions - so it can be
                // checked.
                final Method xgetDeclaredMethod = Class.class
                        .getDeclaredMethod("getDeclaredMethod",
                                String.class);
                DirectByteBuffer_cleaner = (Method) xgetDeclaredMethod.invoke(DirectByteBuffer_class,
                        "cleaner");
            }
            if (DirectByteBuffer_cleaner == null)
                return false;
            Cleaner_class = Unsafe.getClassLoader(ByteBuffer.class).loadClass("sun/misc/Cleaner");
            if (Cleaner_class == null)
                return false;
            Cleaner_clean = Cleaner_class.getDeclaredMethod("clean");
            if (Cleaner_clean == null)
                return false;

            return true;
        }

        @Override
        public Object getTrackingField(ByteBuffer buffer)
        {
            if (!supported)
                return null;
            try
            {
                return DirectByteBuffer_memoryRef.get(buffer);
            } catch (IllegalAccessException e)
            {
                Log.e(TAG, "Unexpected illegal access on tracking field, memoryRef", e);
                supported = Boolean.FALSE;

                return null;
            } catch (IllegalArgumentException e)
            {
                Log.e(TAG, "Unexpected illegal argument on tracking field, memoryRef", e);
                supported = Boolean.FALSE;

                return null;
            }
        }

        @Override
        protected void freeImpl(ByteBuffer buffer) throws Throwable
        {
            Object cleaner = DirectByteBuffer_cleaner.invoke(buffer);
            if (cleaner == null)
                return;
            Cleaner_clean.invoke(cleaner);
        }

        @Override
        protected Field getByteBufferField(Buffer buffer) throws Throwable
        {
            return buffer.getClass().getDeclaredField("bb");
        }
    }

    private static class DirectByteBuffer18 extends Unsafe.DirectByteBufferApi
    {
        private Class<?> MappedByteBuffer_class = null;
        private Field MappedByteBuffer_block = null;
        private Method DirectByteBuffer_free = null;

        DirectByteBuffer18()
        {
            super("Android 18");
        }

        @Override
        protected boolean isSupportedImpl() throws Throwable
        {
            MappedByteBuffer_class = Unsafe.getClassLoader(ByteBuffer.class).loadClass("java/nio/MappedByteBuffer");
            if (MappedByteBuffer_class == null)
                return false;
            MappedByteBuffer_block = MappedByteBuffer_class.getDeclaredField("block");
            if (MappedByteBuffer_block == null)
                return false;
            MappedByteBuffer_block.setAccessible(true);

            if (!checkField(MappedByteBuffer_block))
                return false;

            DirectByteBuffer_free = DirectByteBuffer_class.getDeclaredMethod("free");
            if (DirectByteBuffer_free == null)
                return false;

            return true;
        }

        @Override
        public Object getTrackingField(ByteBuffer buffer)
        {
            if (!supported)
                return null;
            try
            {
                return MappedByteBuffer_block.get(buffer);
            } catch (IllegalAccessException e)
            {
                Log.e(TAG, "Unexpected illegal access on tracking field, memoryRef", e);
                supported = Boolean.FALSE;

                return null;
            } catch (IllegalArgumentException e)
            {
                Log.e(TAG, "Unexpected illegal argument on tracking field, memoryRef", e);
                supported = Boolean.FALSE;

                return null;
            }
        }

        @Override
        protected void freeImpl(ByteBuffer buffer) throws Throwable
        {
            DirectByteBuffer_free.invoke(buffer);
        }

        @Override
        protected Field getByteBufferField(Buffer buffer) throws Throwable
        {
            return buffer.getClass().getDeclaredField("byteBuffer");
        }
    }
}
