/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.text;

import android.annotation.IntRange;
import android.annotation.NonNull;

/**
 * Implements a growing array of int primitives.
 *
 * These arrays are NOT thread safe.
 *
 * @hide
 */
public final class AutoGrowArray {
    private static final int MIN_CAPACITY_INCREMENT = 12;
    private static final int MAX_CAPACITY_TO_BE_KEPT = 10000;

    /**
     * Returns next capacity size.
     *
     * The returned capacity is larger than requested capacity.
     */
    private static int computeNewCapacity(int currentSize, int requested) {
        final int targetCapacity = currentSize + (currentSize < (MIN_CAPACITY_INCREMENT / 2)
                ?  MIN_CAPACITY_INCREMENT : currentSize >> 1);
        return targetCapacity > requested ? targetCapacity : requested;
    }

    /**
     * An auto growing byte array.
     */
    public static class ByteArray {

        private @NonNull byte[] mValues;
        private  int mSize;

        /**
         * Creates an empty ByteArray with the default initial capacity.
         */
        public ByteArray() {
            this(10);
        }

        /**
         * Creates an empty ByteArray with the specified initial capacity.
         */
        public ByteArray( int initialCapacity) {
            if (initialCapacity == 0) {
                mValues = new byte[0];
            } else {
                mValues = new byte[initialCapacity];
            }
            mSize = 0;
        }

        /**
         * Changes the size of this ByteArray. If this ByteArray is shrinked, the backing array
         * capacity is unchanged.
         */
        public void resize( int newSize) {
            if (newSize > mValues.length) {
                ensureCapacity(newSize - mSize);
            }
            mSize = newSize;
        }

        /**
         * Appends the specified value to the end of this array.
         */
        public void append(byte value) {
            ensureCapacity(1);
            mValues[mSize++] = value;
        }

        /**
         * Ensures capacity to append at least <code>count</code> values.
         */
        private void ensureCapacity( int count) {
            final int requestedSize = mSize + count;
            if (requestedSize >= mValues.length) {
                final int newCapacity = computeNewCapacity(mSize, requestedSize);
                final byte[] newValues = new byte[newCapacity];
                System.arraycopy(mValues, 0, newValues, 0, mSize);
                mValues = newValues;
            }
        }

        /**
         * Removes all values from this array.
         */
        public void clear() {
            mSize = 0;
        }

        /**
         * Removes all values from this array and release the internal array object if it is too
         * large.
         */
        public void clearWithReleasingLargeArray() {
            clear();
            if (mValues.length > MAX_CAPACITY_TO_BE_KEPT) {
                mValues = new byte[0];
            }
        }

        /**
         * Returns the value at the specified position in this array.
         */
        public byte get( int index) {
            return mValues[index];
        }

        /**
         * Sets the value at the specified position in this array.
         */
        public void set( int index, byte value) {
            mValues[index] = value;
        }

        /**
         * Returns the number of values in this array.
         */
        public  int size() {
            return mSize;
        }

        /**
         * Returns internal raw array.
         *
         * Note that this array may have larger size than you requested.
         * Use size() instead for getting the actual array size.
         */
        public @NonNull byte[] getRawArray() {
            return mValues;
        }
    }

    /**
     * An auto growing int array.
     */
    public static class IntArray {

        private @NonNull int[] mValues;
        private  int mSize;

        /**
         * Creates an empty IntArray with the default initial capacity.
         */
        public IntArray() {
            this(10);
        }

        /**
         * Creates an empty IntArray with the specified initial capacity.
         */
        public IntArray( int initialCapacity) {
            if (initialCapacity == 0) {
                mValues = new int[0];
            } else {
                mValues = new int[initialCapacity];
            }
            mSize = 0;
        }

        /**
         * Changes the size of this IntArray. If this IntArray is shrinked, the backing array
         * capacity is unchanged.
         */
        public void resize( int newSize) {
            if (newSize > mValues.length) {
                ensureCapacity(newSize - mSize);
            }
            mSize = newSize;
        }

        /**
         * Appends the specified value to the end of this array.
         */
        public void append(int value) {
            ensureCapacity(1);
            mValues[mSize++] = value;
        }

        /**
         * Ensures capacity to append at least <code>count</code> values.
         */
        private void ensureCapacity( int count) {
            final int requestedSize = mSize + count;
            if (requestedSize >= mValues.length) {
                final int newCapacity = computeNewCapacity(mSize, requestedSize);
                final int[] newValues = new int[newCapacity];
                System.arraycopy(mValues, 0, newValues, 0, mSize);
                mValues = newValues;
            }
        }

        /**
         * Removes all values from this array.
         */
        public void clear() {
            mSize = 0;
        }

        /**
         * Removes all values from this array and release the internal array object if it is too
         * large.
         */
        public void clearWithReleasingLargeArray() {
            clear();
            if (mValues.length > MAX_CAPACITY_TO_BE_KEPT) {
                mValues = new int[0];
            }
        }

        /**
         * Returns the value at the specified position in this array.
         */
        public int get( int index) {
            return mValues[index];
        }

        /**
         * Sets the value at the specified position in this array.
         */
        public void set( int index, int value) {
            mValues[index] = value;
        }

        /**
         * Returns the number of values in this array.
         */
        public  int size() {
            return mSize;
        }

        /**
         * Returns internal raw array.
         *
         * Note that this array may have larger size than you requested.
         * Use size() instead for getting the actual array size.
         */
        public @NonNull int[] getRawArray() {
            return mValues;
        }
    }

    /**
     * An auto growing float array.
     */
    public static class FloatArray {

        private @NonNull float[] mValues;
        private  int mSize;

        /**
         * Creates an empty FloatArray with the default initial capacity.
         */
        public FloatArray() {
            this(10);
        }

        /**
         * Creates an empty FloatArray with the specified initial capacity.
         */
        public FloatArray( int initialCapacity) {
            if (initialCapacity == 0) {
                mValues = new float[0];
            } else {
                mValues = new float[initialCapacity];
            }
            mSize = 0;
        }

        /**
         * Changes the size of this FloatArray. If this FloatArray is shrinked, the backing array
         * capacity is unchanged.
         */
        public void resize( int newSize) {
            if (newSize > mValues.length) {
                ensureCapacity(newSize - mSize);
            }
            mSize = newSize;
        }

        /**
         * Appends the specified value to the end of this array.
         */
        public void append(float value) {
            ensureCapacity(1);
            mValues[mSize++] = value;
        }

        /**
         * Ensures capacity to append at least <code>count</code> values.
         */
        private void ensureCapacity(int count) {
            final int requestedSize = mSize + count;
            if (requestedSize >= mValues.length) {
                final int newCapacity = computeNewCapacity(mSize, requestedSize);
                final float[] newValues = new float[newCapacity];
                System.arraycopy(mValues, 0, newValues, 0, mSize);
                mValues = newValues;
            }
        }

        /**
         * Removes all values from this array.
         */
        public void clear() {
            mSize = 0;
        }

        /**
         * Removes all values from this array and release the internal array object if it is too
         * large.
         */
        public void clearWithReleasingLargeArray() {
            clear();
            if (mValues.length > MAX_CAPACITY_TO_BE_KEPT) {
                mValues = new float[0];
            }
        }

        /**
         * Returns the value at the specified position in this array.
         */
        public float get( int index) {
            return mValues[index];
        }

        /**
         * Sets the value at the specified position in this array.
         */
        public void set( int index, float value) {
            mValues[index] = value;
        }

        /**
         * Returns the number of values in this array.
         */
        public  int size() {
            return mSize;
        }

        /**
         * Returns internal raw array.
         *
         * Note that this array may have larger size than you requested.
         * Use size() instead for getting the actual array size.
         */
        public @NonNull float[] getRawArray() {
            return mValues;
        }
    }
}
