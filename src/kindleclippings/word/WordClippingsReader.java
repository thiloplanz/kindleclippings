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

import java.io.IOException;
import java.io.InputStream;

import kindleclippings.Clipping;

import org.apache.poi.hwpf.extractor.WordExtractor;

class WordClippingsReader {

	private final String[] paragraphs;

	private final String book;

	private int index = 0;

	WordClippingsReader(InputStream doc) throws IOException {

		WordExtractor extractor = new WordExtractor(doc);

		paragraphs = extractor.getParagraphText();

		// join all paragraphs up to the first empty one to make up the title
		int i = 0;
		StringBuilder title = new StringBuilder();
		for (String a : paragraphs) {
			if (a.trim().isEmpty()) {
				if (title.length() > 0)
					break;
			} else {
				title.append(" ");
			}
			title.append(a);
			i++;
		}

		book = title.toString().replaceAll("\r?\n", " ").trim();
		index = i;
	}

	Clipping readClipping() {
		if (index >= paragraphs.length)
			return null;

		Clipping result = new Clipping();
		result.setBook(book);

		result.setContent(paragraphs[index].trim());
		index++;
		if (result.getContent().isEmpty())
			return readClipping();
		return result;

	}

}
