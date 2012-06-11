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

import java.util.AbstractList;
import java.util.Collection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TermSet {

	private final JSONObject data;

	TermSet(JSONObject data) {
		this.data = data;
	}

	static String getJSONString(JSONObject data, String key) {
		try {
			return data.getString(key);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	public String getTitle() {
		return getJSONString(data, "title");
	}

	public String getId() {
		return getJSONString(data, "id");
	}

	public int getSize() {
		try {
			return data.getInt("term_count");
		} catch (JSONException e) {
			return 0;
		}
	}

	public Collection<Term> getTerms() {
		try {
			final JSONArray terms = data.getJSONArray("terms");
			return new AbstractList<Term>() {

				@Override
				public Term get(int index) {
					try {
						return new Term(terms.getJSONObject(index));
					} catch (JSONException e) {
						throw new RuntimeException(e);
					}
				}

				@Override
				public int size() {
					return terms.length();
				}
			};
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return getTitle() + " (" + getSize() + ")";
	}

}
