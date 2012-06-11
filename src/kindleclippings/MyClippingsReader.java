/**
 * Copyright (c) 2012, Howtobewebsmart.com, L.L.C. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License, Version 2.0
 * as published by the Apache Software Foundation (the "License").
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * You should have received a copy of the License along with this program.
 * If not, see <http://www.apache.org/licenses/LICENSE-2.0>.
 */
package kindleclippings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import kindleclippings.Clipping.ClippingType;

public class MyClippingsReader {

	private final BufferedReader reader;

	public MyClippingsReader(Reader reader) {
		this.reader = reader instanceof BufferedReader ? (BufferedReader) reader
				: new BufferedReader(reader);
	}

	public Clipping readClipping() throws IOException {
		String book = reader.readLine();
		if (book == null)
			return null;
		// sometimes starts with "\uFEFF" 'ZERO WIDTH NO-BREAK SPACE'
		book = book.replace("\uFEFF", "");
		String type = reader.readLine();
		String empty = reader.readLine();
		if (empty.length() > 0) {
			System.err.println("empty line was not empty, but " + empty);
		}
		String content = reader.readLine();
		content = content.replace("\uFEFF", "");
		while (true) {
			String x = reader.readLine();
			if ("==========".equals(x)) {
				Clipping c = new Clipping();
				c.setBook(book);
				c.setContent(content);
				if (type.startsWith("- Your Highlight ")
						|| type.startsWith("- Highlight")) {
					c.setType(ClippingType.highlight);
				} else if (type.startsWith("- Your Note ")
						|| type.startsWith("- Note")) {
					c.setType(ClippingType.note);
				} else if (type.startsWith("- Your Bookmark ")
						|| type.startsWith("- Bookmark")) {
					c.setType(ClippingType.bookmark);
				} else {
					System.err.println("unknown type " + book + "\n" + type);
					return readClipping();
				}
				return c;
			}
		}
	}

}
