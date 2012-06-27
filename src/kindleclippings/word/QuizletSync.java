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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;

import kindleclippings.Clipping;
import kindleclippings.quizlet.QuizletAPI;
import kindleclippings.quizlet.Term;
import kindleclippings.quizlet.TermSet;

import org.json.JSONException;

public class QuizletSync {

	private static void addSet(QuizletAPI api, String book,
			List<Clipping> clippings) throws IOException {
		List<Term> terms = new ArrayList<Term>();
		for (Clipping c : clippings)
			terms.add(kindleclippings.quizlet.QuizletSync.makeTerm(c));
		api.addSet(book, "en", "en", "Word notes", "only_me", "only_me", terms);

		System.err.println("created set [" + book + "]");
	}

	private static List<Clipping> readClippingsFile(File cl)
			throws IOException, BadLocationException {

		InputStream in = new FileInputStream(cl);

		try {
			List<Clipping> clippings = new ArrayList<Clipping>();

			if (cl.getName().toLowerCase().endsWith(".doc")) {
				WordClippingsReader r = new WordClippingsReader(in);
				Clipping l;
				while ((l = r.readClipping()) != null) {
					String lct = l.getContent().trim();
					if (lct.length() == 0) {
						System.err.println("ignored empty " + l.getType()
								+ " [" + l.getBook() + "]");
						continue;
					}
					if (lct.length() < 10 || !lct.contains(" ")) {
						System.err.println("ignored too short " + l.getType()
								+ " " + l.getContent() + " [" + l.getBook()
								+ "]");
						continue;
					}
					clippings.add(l);
				}
			} else {
				TextClippingsReader r = cl.getName().toLowerCase()
						.endsWith(".rtf") ? RTFClippingsReader.getRTFReader(in)
						: new TextClippingsReader(new BufferedReader(
								new InputStreamReader(in, "UTF-8")));
				Clipping l;
				while ((l = r.readClipping()) != null) {
					String lct = l.getContent().trim();
					if (lct.length() == 0) {
						System.err.println("ignored empty " + l.getType()
								+ " [" + l.getBook() + "]");
						continue;
					}
					if (lct.length() < 10 || !lct.contains(" ")) {
						System.err.println("ignored too short " + l.getType()
								+ " " + l.getContent() + " [" + l.getBook()
								+ "]");
						continue;
					}
					clippings.add(l);
				}
			}

			return clippings;
		} finally {
			in.close();
		}
	}

	public static void main(String[] args) throws IOException, JSONException,
			URISyntaxException, InterruptedException, BackingStoreException,
			BadLocationException {

		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new FileNameExtensionFilter("Word documents", "doc",
				"rtf", "txt"));
		fc.setMultiSelectionEnabled(true);
		int result = fc.showOpenDialog(null);
		if (result != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File[] clf = fc.getSelectedFiles();
		if (clf == null || clf.length == 0)
			return;

		ProgressMonitor progress = new ProgressMonitor(null, "QuizletSync",
				"loading notes files", 0, 100);
		progress.setMillisToPopup(0);
		progress.setMillisToDecideToPopup(0);
		progress.setProgress(0);
		try {

			progress.setNote("checking Quizlet account");
			progress.setProgress(5);

			Preferences prefs = kindleclippings.quizlet.QuizletSync.getPrefs();

			QuizletAPI api = new QuizletAPI(prefs.get("access_token", null));

			Collection<TermSet> sets = null;
			try {
				progress.setNote("checking Quizlet library");
				progress.setProgress(10);
				sets = api.getSets(prefs.get("user_id", null));
			} catch (IOException e) {
				if (e.toString().contains("401")) {
					// Not Authorized => Token has been revoked
					kindleclippings.quizlet.QuizletSync.clearPrefs();
					prefs = kindleclippings.quizlet.QuizletSync.getPrefs();
					api = new QuizletAPI(prefs.get("access_token", null));
					sets = api.getSets(prefs.get("user_id", null));
				} else {
					throw e;
				}
			}

			progress.setProgress(15);
			progress.setMaximum(15 + clf.length * 10);
			progress.setNote("uploading new notes");

			int pro = 15;

			int addedSets = 0;
			int updatedTerms = 0;
			int updatedSets = 0;

			for (File f : clf) {
				progress.setProgress(pro);
				List<Clipping> clippings = readClippingsFile(f);

				if (clippings == null) {
					pro += 10;
					continue;
				}

				if (clippings.isEmpty()) {
					pro += 10;
					continue;
				}

				if (clippings.size() < 2) {
					pro += 10;
					continue;
				}

				String book = clippings.get(0).getBook();
				progress.setNote(book);

				TermSet termSet = null;
				String x = book.toLowerCase().replaceAll("\\W", "");

				for (TermSet t : sets) {
					if (t.getTitle().toLowerCase().replaceAll("\\W", "")
							.equals(x)) {
						termSet = t;
						break;
					}
				}

				if (termSet == null) {

					addSet(api, book, clippings);
					addedSets++;
					pro += 10;
					continue;
				}

				// compare against existing terms
				boolean hasUpdated = false;
				for (Clipping cl : clippings) {
					if (!kindleclippings.quizlet.QuizletSync.checkExistingTerm(
							cl, termSet)) {
						kindleclippings.quizlet.QuizletSync.addTerm(api,
								termSet, cl);
						updatedTerms++;
						hasUpdated = true;
					}
				}

				pro += 10;

				if (hasUpdated)
					updatedSets++;

			}

			if (updatedTerms == 0 && addedSets == 0) {
				JOptionPane.showMessageDialog(null,
						"Done.\nNo new data was uploaded", "QuizletSync",
						JOptionPane.OK_OPTION);
			} else {
				if (addedSets > 0) {
					JOptionPane
							.showMessageDialog(
									null,
									String.format(
											"Done.\nCreated %d new sets and added %d cards to %d existing sets",
											addedSets, updatedSets,
											updatedTerms), "QuizletSync",
									JOptionPane.OK_OPTION);
				} else {
					JOptionPane.showMessageDialog(null, String.format(
							"Done.\nAdded %d cards to %d existing sets",
							updatedTerms, updatedSets), "QuizletSync",
							JOptionPane.OK_OPTION);
				}
			}
		} finally {
			progress.close();
		}

		System.exit(0);
	}
}
