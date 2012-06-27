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
import java.io.InputStream;
import java.io.StringReader;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.rtf.RTFEditorKit;

class RTFClippingsReader {

	static TextClippingsReader getRTFReader(InputStream rtf)
			throws IOException, BadLocationException {
		RTFEditorKit rtfParser = new RTFEditorKit();
		Document document = rtfParser.createDefaultDocument();
		rtfParser.read(rtf, document, 0);
		return new TextClippingsReader(new BufferedReader(new StringReader(
				document.getText(0, document.getLength()))));

	}
}
