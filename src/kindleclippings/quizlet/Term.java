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
package kindleclippings.quizlet;

import static kindleclippings.quizlet.TermSet.getJSONString;

import org.json.JSONException;
import org.json.JSONObject;

public class Term {

	private final JSONObject data;

	Term(JSONObject data) {
		this.data = data;
	}

	public Term(String term, String definition) {
		this.data = new JSONObject();
		try {
			data.put("term", term);
			data.put("definition", definition);
		} catch (JSONException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public String getTerm() {
		return getJSONString(data, "term");
	}

	public String getDefinition() {
		return getJSONString(data, "definition");
	}

	@Override
	public String toString() {
		return getTerm();
	}

}
