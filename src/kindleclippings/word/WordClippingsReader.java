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

		// first paragraph is the book title
		Clipping c = readClipping();
		book = c.getContent().replace('\n', ' ').trim();
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
