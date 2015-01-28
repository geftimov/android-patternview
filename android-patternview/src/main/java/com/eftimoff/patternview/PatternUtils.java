/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.eftimoff.patternview;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

class PatternUtils {

	private static final String UTF8 = "UTF-8";

	public static final String SHA1 = "SHA-1";

	public static List<PatternView.Cell> stringToPatternOld(String string) {
		List<PatternView.Cell> result = new ArrayList<>();

		try {
			final byte[] bytes = string.getBytes(UTF8);
            for (byte b : bytes) {
                result.add(PatternView.Cell.of(b / 3, b % 3));
            }
		} catch (UnsupportedEncodingException e) {
		}

		return result;
	}

	// public static String patternToStringOld(List<LockPatternView.Cell>
	// pattern) {
	// if (pattern == null) {
	// return "";
	// }
	// final int patternSize = pattern.size();
	//
	// byte[] res = new byte[patternSize];
	// for (int i = 0; i < patternSize; i++) {
	// LockPatternView.Cell cell = pattern.get(i);
	// res[i] = (byte) (cell.getRow() * 3 + cell.getColumn());
	// }
	// try {
	// return new String(res, UTF8);
	// } catch (UnsupportedEncodingException e) {
	// return "";
	// }
	// }

	

	/**
	 * Converts a string to a pattern
	 * 
	 * @param string
	 * @return
	 */
	public static List<PatternView.Cell> stringToPattern(String string) {
		List<PatternView.Cell> result = new ArrayList<>();

		for (int i = 0; i < string.length(); i++) {
			int b = Integer.valueOf(string.substring(i, i + 1));
			result.add(PatternView.Cell.of(b / 3, b % 3));
		}

		return result;
	}

	/**
	 * Serializes a pattern
	 * 
	 * @param pattern
	 *            The pattern
	 * @return The SHA-1 string of the pattern got from
	 *         {@link #patternToStringOld(java.util.List)}
	 */
	// public static String stringToSha1(String string) {
	// try {
	// MessageDigest md = MessageDigest.getInstance(SHA1);
	// md.update(string.getBytes(UTF8));
	//
	// byte[] digest = md.digest();
	// BigInteger bi = new BigInteger(1, digest);
	// return String.format("%0" + (digest.length * 2) + "x", bi)
	// .toLowerCase(Locale.US);
	// } catch (NoSuchAlgorithmException e) {
	// // never catch this
	// return "";
	// } catch (UnsupportedEncodingException e) {
	// // never catch this
	// return "";
	// }
	// }
}
