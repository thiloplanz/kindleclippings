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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;

public class QuizletAPI {

	private final String accessToken;

	public QuizletAPI(String accessToken) {
		this.accessToken = accessToken;
	}

	public Collection<TermSet> getSets(String userName)
			throws ClientProtocolException, IOException, JSONException {

		JSONArray sets = callArrayAPI("/users/" + userName + "/sets");
		int length = sets.length();
		Collection<TermSet> result = new ArrayList<TermSet>(length);
		for (int i = 0; i < length; i++) {
			result.add(new TermSet(sets.getJSONObject(i)));
		}
		return result;
	}

	private JSONArray callArrayAPI(String path) throws ClientProtocolException,
			IOException, JSONException {
		HttpGet get = new HttpGet("https://api.quizlet.com/2.0" + path);
		get.addHeader("Authorization", "Bearer " + accessToken);
		HttpResponse response = new DefaultHttpClient().execute(get);
		if (response.getStatusLine().getStatusCode() != 200) {
			response.getEntity().writeTo(System.err);
			throw new IOException(response.getStatusLine().toString());
		}
		ByteArrayOutputStream buffer = new ByteArrayOutputStream(32000);
		response.getEntity().writeTo(buffer);
		return new JSONArray(new String(buffer.toByteArray(), "UTF-8"));
	}

	private HttpResponse post(String path, List<? extends NameValuePair> params)
			throws ClientProtocolException, IOException {
		HttpPost post = new HttpPost("https://api.quizlet.com/2.0" + path);
		post.addHeader("Authorization", "Bearer " + accessToken);
		post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
		return new DefaultHttpClient().execute(post);
	}

	public void addTermToSet(String setId, Term term)
			throws ClientProtocolException, IOException {

		HttpResponse response = post("/sets/" + setId + "/terms",
				Arrays.asList(
						new BasicNameValuePair("term", term.getTerm()),
						new BasicNameValuePair("definition", term
								.getDefinition())));
		if (response.getStatusLine().getStatusCode() != 201) {
			System.err.println("failed to add " + term.getTerm());
			response.getEntity().writeTo(System.err);
			throw new IOException(response.getStatusLine().toString());
		}
	}

	public void addSet(String title, String lang_terms,
			String lang_definitions, String description, String visibility,
			String editable, List<Term> terms) throws IOException {

		if (terms.size() < 2)
			throw new IllegalArgumentException("need at least two terms");

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("title", title));
		params.add(new BasicNameValuePair("lang_terms", lang_terms));
		params.add(new BasicNameValuePair("lang_definitions", lang_definitions));
		params.add(new BasicNameValuePair("description", description));
		params.add(new BasicNameValuePair("visibility", visibility));
		params.add(new BasicNameValuePair("editable", editable));
		for (Term t : terms) {
			params.add(new BasicNameValuePair("terms[]", t.getTerm()));
			params.add(new BasicNameValuePair("definitions[]", t
					.getDefinition()));
		}

		HttpResponse response = post("/sets", params);
		if (response.getStatusLine().getStatusCode() != 201)
			throw new IOException(response.getStatusLine().toString());
	}

	public void deleteSet(String setId) throws IOException {
		HttpDelete delete = new HttpDelete("https://api.quizlet.com/2.0/sets/"
				+ setId);
		delete.addHeader("Authorization", "Bearer " + accessToken);
		HttpResponse response = new DefaultHttpClient().execute(delete);
		if (response.getStatusLine().getStatusCode() != 204)
			throw new IOException(response.getStatusLine().toString());
	}

}
