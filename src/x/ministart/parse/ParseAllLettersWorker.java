package x.ministart.parse;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DefaultStyledDocument;

import x.ministart.sys.SwingUI;
import x.ministart.sys.SwingWorkerExecutor;
import x.ministart.outils.Chrono;

/**
 * Parse all the letters.<br>
 * Add a "_" between them.
 * 
 * TODO Performance Boost (cf. http://docs.oracle.com/javase/6/docs/api/javax/swing/text/Document.html)	
 * @author Ingénierie des langues
 *
 */
public class ParseAllLettersWorker{
	
	private SwingUI ui;

	public ParseAllLettersWorker(final SwingUI ui) {
		this.ui = ui;
		
		SwingWorker<Integer, Object> swingWorker = new ParseAllLetters();

		SwingWorkerExecutor.instance().execute(swingWorker);

		swingWorker.addPropertyChangeListener( new PropertyChangeListener() {
			@Override
			public  void propertyChange(PropertyChangeEvent evt) {
				if ("progress".equals(evt.getPropertyName())) {
					if (ui.progressBar != null){
						ui.progressBar.setValue((Integer)evt.getNewValue());
					}
				}
			}

		});	
	}
	public class ParseAllLetters extends SwingWorker<Integer, Object> {

		private Chrono chrono = new Chrono();

		@Override
		protected Integer doInBackground() throws Exception {
			Document doc = ui.main_txtarea.getDocument();
			ui.main_txtarea.setDocument(new DefaultStyledDocument());

			ui.statusInfo.setText ("Parse ALL - running");
			this.chrono.start();
			
			int 
			original_txt_lenght = doc.getLength(),
			read_count = 0,
			insertion_count = 0,
			parse_ind = 0;
		try {
			String start_balise = "<token>";
			String end_balise = "</token>";
			String xml_header = "<?xml version='1.0' encoding='ISO-8859-1' ?>\n";
			String startMainTag = "<words>\n";
			String endMainTag = "\n</words>\n";
			
			doc.insertString(parse_ind, xml_header , null);
			parse_ind += xml_header.length(); 
			doc.insertString(parse_ind, startMainTag, null);
			parse_ind += startMainTag.length();
			
;
			boolean openTag = false;
			while(read_count < original_txt_lenght) {
				setProgress( (read_count+1) * 100 / original_txt_lenght);// update the progress

				char s_char = doc.getText(parse_ind, 1).charAt(0);
				if(s_char == 9 ||  s_char == 32 || s_char == 10 ) {
						if (openTag) {
							doc.insertString(parse_ind, end_balise , null);
							++insertion_count;
							parse_ind += end_balise.length()+1;
							openTag = false;
						}
						else
							++parse_ind;
				}
				else {
					if (openTag)
						++parse_ind;
					
					else{
						doc.insertString(parse_ind, start_balise , null);
						++insertion_count;
						parse_ind += start_balise.length()+1;
						openTag = true;
					}	
				}
				read_count++;

			}
			if (openTag){
				doc.insertString(doc.getLength(), end_balise , null);
				++insertion_count;
				openTag = false;
			}
			doc.insertString(doc.getLength(), endMainTag, null);

			} catch (BadLocationException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}

			ui.statusInfo.setText ("Parse ALL - sending to text area");
			ui.main_txtarea.setDocument(doc);

			return insertion_count;
			//			  textComponent.setCaretPosition(doc.getLength() - 1);


		}

		@Override
		protected void done(){
			int strParsed = 0;
			try {
				strParsed = get();
				this.chrono.stop();
				ui.statusInfo.setText("Document parsed..." + strParsed+ " insertions [" + this.chrono.displayInterval() + "]");
			}catch(Exception e){

				JOptionPane.showMessageDialog(ui, 
						"Error :\n" + e.getLocalizedMessage(),
						"Parse", 
						JOptionPane.ERROR_MESSAGE);
				ui.statusInfo.setText("Parse error on \"ParseAllLettersWorker\".");
			}
		}
	}
}