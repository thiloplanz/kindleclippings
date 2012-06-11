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

import java.awt.Desktop;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

class GetAccessToken {

	// see https://quizlet.com/api/2.0/docs/authorization_code_flow/
	private static final String clientId = "GET THIS FROM YOUR QUIZLET DEVELOPER DASHBOARD";

	// see https://quizlet.com/api/2.0/docs/authorization_code_flow/
	// looks like "Basic abcd......"
	private static final String authHeader = "GET THIS FROM YOUR QUIZLET DEVELOPER DASHBOARD";

	static JSONObject oauthDance() throws IOException, URISyntaxException,
			InterruptedException, JSONException {

		// start HTTP server, so when can get the authorization code
		InetSocketAddress addr = new InetSocketAddress(7777);
		HttpServer server = HttpServer.create(addr, 0);
		AuthCodeHandler handler = new AuthCodeHandler();
		server.createContext("/", handler);
		ExecutorService ex = Executors.newCachedThreadPool();
		server.setExecutor(ex);
		server.start();
		String authCode;
		try {
			Desktop.getDesktop().browse(
					new URI(new StringBuilder("https://quizlet.com/authorize/")
							.append("?scope=read%20write_set")
							.append("&client_id=" + clientId)
							.append("&response_type=code")
							.append("&state=" + handler.state).toString()));

			authCode = handler.result.take();
		} finally {
			server.stop(0);
			ex.shutdownNow();
		}

		if (authCode == null || authCode.length() == 0)
			return null;

		HttpPost post = new HttpPost("https://api.quizlet.com/oauth/token");
		post.setHeader("Authorization", authHeader);

		post.setEntity(new UrlEncodedFormEntity(Arrays.asList(
				new BasicNameValuePair("grant_type", "authorization_code"),
				new BasicNameValuePair("code", authCode))));
		HttpResponse response = new DefaultHttpClient().execute(post);

		ByteArrayOutputStream buffer = new ByteArrayOutputStream(1000);
		response.getEntity().writeTo(buffer);
		return new JSONObject(new String(buffer.toByteArray(), "UTF-8"));
	}

	static class AuthCodeHandler implements HttpHandler {

		final String state = UUID.randomUUID().toString();

		final BlockingQueue<String> result = new ArrayBlockingQueue<String>(1);

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			String requestMethod = exchange.getRequestMethod();
			OutputStream responseBody = exchange.getResponseBody();

			if (requestMethod.equalsIgnoreCase("GET")) {
				String uri = exchange.getRequestURI().toString();
				int code = uri.indexOf("&code=");
				if (code > -1 && uri.contains("state=" + state)) {
					result.add(uri.substring(code + 6));
					exchange.sendResponseHeaders(200, 0);
					responseBody.write("ok.\n You can close this window now."
							.getBytes());
				} else {
					exchange.sendResponseHeaders(400, 0);
					responseBody.write(("invalid state " + uri).getBytes());
					result.add("");
				}
			} else {
				exchange.sendResponseHeaders(500, 0);
				responseBody.write(("ignored unexpected request "
						+ exchange.getRequestMethod() + " " + exchange
						.getRequestURI()).getBytes());
			}
			responseBody.close();

		}

	}

}
