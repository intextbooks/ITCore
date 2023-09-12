/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pdfbox.text;

import java.util.Comparator;

import org.apache.commons.lang3.tuple.Pair;

/**
 * This class is a comparator for TextPosition operators.  It handles
 * pages with text in different directions by grouping the text based
 * on direction and sorting in that direction. This allows continuous text
 * in a given direction to be more easily grouped together.  
 *
 * @author @author alpiz001
 */
public class TextPositionXComparator implements Comparator<TextPosition>
{


	@Override
	public int compare(TextPosition o1, TextPosition o2) {
		return Float.compare(o1.getX(), o2.getX());
	}
}
