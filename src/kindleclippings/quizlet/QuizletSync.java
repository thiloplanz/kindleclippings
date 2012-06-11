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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.filechooser.FileNameExtensionFilter;

import kindleclippings.Clipping;
import kindleclippings.Clipping.ClippingType;
import kindleclippings.MyClippingsReader;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

public class QuizletSync {

	private static void addSet(QuizletAPI api, String book,
			List<Clipping> clippings) throws IOException {
		List<Term> terms = new ArrayList<Term>();
		for (Clipping c : clippings)
			terms.add(makeTerm(c));
		api.addSet(book, "en", "en", "Kindle notes", "only_me", "only_me",
				terms);

		System.err.println("created set [" + book + "]");
	}

	/**
	 * Quizlet does not like some swear words and will delete them so we
	 * sanitize them first to preserve them better
	 * 
	 */
	private static String sanitize(String s) {
		for (char c : " ,:.;".toCharArray()) {
			s = s.replace(" fuck" + c, " f***" + c)
					.replace(" shit" + c, " sh*t" + c)
					.replace(" fucking" + c, " f***ing" + c)
					.replace(" motherfucka" + c, "motherf****" + c);
		}
		return s;
	}

	private static Term makeTerm(Clipping cl) {
		String x = cl.getContent();
		// split in half
		int half = x.length() / 2;
		// try to find a comma or period in the first half
		String term = x.substring(0, half);
		half = term.lastIndexOf('.');
		int c = term.lastIndexOf(',');
		if (c > half)
			half = c;
		if (half > 10) {
			term = term.substring(0, half + 1);
		}
		// make sure we split between words
		int nextSpace = x.indexOf(' ', term.length());
		if (nextSpace >= x.length())
			// go backwards
			nextSpace = x.lastIndexOf(' ', term.length());

		term = x.substring(0, nextSpace).trim();
		String def = x.substring(nextSpace + 1).trim();
		if (def.length() == 0)
			def = term;

		return new Term(sanitize(term), sanitize(def));
	}

	private static void addTerm(QuizletAPI api, TermSet termSet, Clipping cl)
			throws ClientProtocolException, IOException {
		Term term = makeTerm(cl);
		api.addTermToSet(termSet.getId(), term);
		System.err.println("added '" + term + "' to [" + cl.getBook() + "]");
	}

	private static Map<String, List<Clipping>> readClippingsFile()
			throws IOException {
		// try to find it
		File cl = new File("/Volumes/Kindle/documents/My Clippings.txt");
		if (!cl.canRead()) {
			JFileChooser fc = new JFileChooser();
			fc.setFileFilter(new FileNameExtensionFilter("Kindle Clippings",
					"txt"));
			int result = fc.showOpenDialog(null);
			if (result != JFileChooser.APPROVE_OPTION) {
				return null;
			}
			cl = fc.getSelectedFile();
		}
		Reader f = new InputStreamReader(new FileInputStream(cl), "UTF-8");
		try {
			MyClippingsReader r = new MyClippingsReader(f);

			Map<String, List<Clipping>> books = new TreeMap<String, List<Clipping>>();

			Clipping l;
			while ((l = r.readClipping()) != null) {
				if (l.getType() != ClippingType.highlight
						&& l.getType() != ClippingType.note) {
					System.err.println("ignored " + l.getType() + " ["
							+ l.getBook() + "]");
					continue;
				}
				String lct = l.getContent().trim();
				if (lct.length() == 0) {
					System.err.println("ignored empty " + l.getType() + " ["
							+ l.getBook() + "]");
					continue;
				}
				if (lct.length() < 10 || !lct.contains(" ")) {
					System.err.println("ignored too short " + l.getType() + " "
							+ l.getContent() + " [" + l.getBook() + "]");
					continue;
				}
				List<Clipping> clippings = books.get(l.getBook());
				if (clippings == null) {
					clippings = new ArrayList<Clipping>();
					books.put(l.getBook(), clippings);
				}
				clippings.add(l);
			}
			return books;
		} finally {
			f.close();
		}
	}

	private static Preferences getPrefs() throws IOException,
			URISyntaxException, InterruptedException, JSONException,
			BackingStoreException {
		Preferences prefs = Preferences.userNodeForPackage(QuizletSync.class);

		String token = prefs.get("access_token", null);
		if (token == null) {
			JSONObject o = GetAccessToken.oauthDance();
			if (o == null) {
				JOptionPane.showMessageDialog(null,
						"Failed authorization to access Quizlet",
						"QuizletSync", JOptionPane.ERROR_MESSAGE);
				System.exit(0);
			}
			prefs.put("access_token", o.getString("access_token"));
			prefs.put("user_id", o.getString("user_id"));
			prefs.flush();
		}

		return prefs;
	}

	private static void clearPrefs() throws BackingStoreException {
		Preferences prefs = Preferences.userNodeForPackage(QuizletSync.class);
		prefs.clear();
		prefs.flush();
	}

	public static void main(String[] args) throws IOException, JSONException,
			URISyntaxException, InterruptedException, BackingStoreException {

		ProgressMonitor progress = new ProgressMonitor(null, "QuizletSync",
				"loading Kindle clippings file", 0, 100);
		progress.setMillisToPopup(0);
		progress.setMillisToDecideToPopup(0);
		progress.setProgress(0);
		try {

			Map<String, List<Clipping>> books = readClippingsFile();

			if (books == null)
				return;

			if (books.isEmpty()) {
				JOptionPane.showMessageDialog(null,
						"no clippings to be uploaded", "QuizletSync",
						JOptionPane.OK_OPTION);
				return;
			}
			progress.setNote("checking Quizlet account");
			progress.setProgress(5);

			Preferences prefs = getPrefs();

			QuizletAPI api = new QuizletAPI(prefs.get("access_token", null));

			Collection<TermSet> sets = null;
			try {
				progress.setNote("checking Quizlet library");
				progress.setProgress(10);
				sets = api.getSets(prefs.get("user_id", null));
			} catch (IOException e) {
				if (e.toString().contains("401")) {
					// Not Authorized => Token has been revoked
					clearPrefs();
					prefs = getPrefs();
					api = new QuizletAPI(prefs.get("access_token", null));
					sets = api.getSets(prefs.get("user_id", null));
				} else {
					throw e;
				}
			}

			progress.setProgress(15);
			progress.setMaximum(15 + books.size());
			progress.setNote("uploading new notes");

			Map<String, TermSet> indexedSets = new HashMap<String, TermSet>(
					sets.size());

			for (TermSet t : sets) {
				indexedSets.put(t.getTitle(), t);
			}

			int pro = 15;
			int createdSets = 0;
			int createdTerms = 0;
			int updatedTerms = 0;
			for (List<Clipping> c : books.values()) {

				String book = c.get(0).getBook();
				progress.setNote(book);
				progress.setProgress(pro++);

				TermSet termSet = indexedSets.get(book);
				if (termSet == null) {
					if (c.size() < 2) {
						System.err.println("ignored [" + book
								+ "] (need at least two notes)");
						continue;
					}

					addSet(api, book, c);
					createdSets++;
					createdTerms += c.size();
					continue;
				} // compare against existing terms
				clipping: for (Clipping cl : c) {
					// case-insensitive and ignore non-letters
					String check = sanitize(cl.getContent().toLowerCase())
							.replaceAll("\\W", "");
					Collection<Term> terms = termSet.getTerms();
					for (Term t : terms) {
						String x = t.getTerm() + t.getDefinition();
						x = x.toLowerCase().replaceAll("\\W", "");
						if (x.startsWith(check)) {
							if (x.length() == check.length())
								continue clipping;
							if (x.equals(check + check))
								continue clipping;
						}
					}
					addTerm(api, termSet, cl);
					updatedTerms++;

				}
			}
			progress.setProgress(pro++);

			if (createdSets == 0 && updatedTerms == 0) {
				JOptionPane.showMessageDialog(null,
						"Done.\nNo new data was uploaded", "QuizletSync",
						JOptionPane.OK_OPTION);
			} else if (createdSets > 0) {
				JOptionPane
						.showMessageDialog(
								null,
								String.format(
										"Done.\nCreated %d new sets with %d cards, and added %d cards to existing sets",
										createdSets, createdTerms, updatedTerms),
								"QuizletSync", JOptionPane.OK_OPTION);
			} else {
				JOptionPane.showMessageDialog(null,
						String.format("Done.\nAdded %d cards to existing sets",
								updatedTerms), "QuizletSync",
						JOptionPane.OK_OPTION);
			}
		} finally {
			progress.close();
		}

		System.exit(0);
	}
}
