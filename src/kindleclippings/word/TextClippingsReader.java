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
package kindleclippings.word;

import java.io.BufferedReader;
import java.io.IOException;

import kindleclippings.Clipping;

class TextClippingsReader {

	private final BufferedReader reader;

	private final String book;

	TextClippingsReader(BufferedReader text) throws IOException {

		reader = text;

		// first paragraph is the book title
		Clipping c = readClipping();
		book = c.getContent().replaceAll("\r?\n", " ").trim();
	}

	Clipping readClipping() throws IOException {
		String line = reader.readLine();
		if (line == null)
			return null;
		// skip empty lines
		while (line.trim().length() == 0) {
			line = reader.readLine();
			if (line == null)
				return null;
		}
		Clipping result = new Clipping();
		result.setBook(book);

		StringBuilder content = new StringBuilder(1000);
		content.append(line);
		content.append("\n");

		// ends on empty line
		while (true) {
			line = reader.readLine();
			if (line == null)
				break;

			line = line.trim();
			if (line.length() == 0)
				break;
			content.append(line);
			content.append("\n");
		}

		result.setContent(content.toString());
		return result;
	}

}
