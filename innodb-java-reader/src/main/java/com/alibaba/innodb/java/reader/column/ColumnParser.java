/*
 * Copyright (C) 1999-2019 Alibaba Group Holding Limited
 */
package com.alibaba.innodb.java.reader.column;

import com.alibaba.innodb.java.reader.schema.Column;
import com.alibaba.innodb.java.reader.util.SliceInput;

/**
 * Parser for decoding from a {@link SliceInput} which contains byte array
 * to a specific data type. For example, parse 4 bytes to an Integer.
 *
 * @author xu.zx
 */
public interface ColumnParser<V> {

  /**
   * Read value from byte array input with length and charset.
   *
   * @param input   slice input
   * @param len     length
   * @param charset charset
   * @return value
   */
  V readFrom(SliceInput input, int len, String charset);

  /**
   * Read value from byte array input.
   *
   * @param input  slice input
   * @param column column
   * @return value
   */
  V readFrom(SliceInput input, Column column);

  V readFrom(byte[] input);

  /**
   * For row-oriented columnar storage format, there should be a way to
   * skip value from byte array input with length and charset,
   *
   * @param input   slice input
   * @param len     length
   * @param charset charset
   */
  void skipFrom(SliceInput input, int len, String charset);

  /**
   * For row-oriented columnar storage format, there should be a way to
   * skip value from byte array input.
   *
   * @param input  slice input
   * @param column column
   */
  void skipFrom(SliceInput input, Column column);

  /**
   * Returned value class.
   *
   * @return class
   */
  Class<?> typeClass();

}
