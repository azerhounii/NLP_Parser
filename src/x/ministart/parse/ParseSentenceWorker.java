package x.ministart.parse;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import x.ministart.outils.Chrono;
import x.ministart.sys.SwingUI;
import x.ministart.sys.SwingWorkerExecutor;

public class ParseSentenceWorker {
		
private SwingUI ui;
	
	public ParseSentenceWorker(final SwingUI ui) {
		this.ui = ui;
		
		SwingWorker<Integer, Object> swingWorker = new ParseSentence();

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

	public class ParseSentence extends SwingWorker<Integer, Object> {
		private Chrono chrono = new Chrono();

		@Override
		protected Integer doInBackground() throws Exception {
			Document doc = ui.main_txtarea.getDocument();
			
			ui.statusInfo.setText ("Parse Parag - running");
			ui.main_txtarea.setDocument(new DefaultStyledDocument());
			this.chrono.start();
			
			int counter = 0;			 
			
			InputStream tokenModelIn = null;
	        InputStream posModelIn = null;
	        
	        try {
	        	FileWriter fw = new FileWriter("Adjectifs.xml");
	        	StringBuilder sb = new StringBuilder();
	        	String source = doc.getText(0, doc.getLength());
	        		            
	            InputStream is = new FileInputStream("fr-sent.bin");
				SentenceModel model = new SentenceModel(is);
				SentenceDetectorME sdetector = new SentenceDetectorME(model);
				
				tokenModelIn = new FileInputStream("fr-token.bin");
	            TokenizerModel tokenModel = new TokenizerModel(tokenModelIn);
	            Tokenizer tokenizer = new TokenizerME(tokenModel);
	            
	            posModelIn = new FileInputStream("fr-pos-maxent.bin");
	            POSModel posModel = new POSModel(posModelIn);
	            POSTaggerME posTagger = new POSTaggerME(posModel);
		
				String sentences[] = sdetector.sentDetect(source);
		 
				for(int i=0;i<sentences.length;i++){
					//System.out.println("phrase: " + sentences[i]);
					
					String tokens[] = tokenizer.tokenize(sentences[i]);
		            String tags[] = posTagger.tag(tokens);
		            double probs[] = posTagger.probs();
		            
		            /*
		            System.out.println("Token\t:\tTag\t:\tProbability\n---------------------------------------------");
		            for(int j=0;j<tokens.length;j++){
		                System.out.println(tokens[j]+"\t:\t"+tags[j]+"\t:\t"+probs[j]);
		            }
					*/
					
		            detectADJ(tokens, tags, sb);
				}
				
				fw.write ("<?xml version=\"1.0\" encoding=\"utf-8\" ?>");
				fw.write ("\r\n");
				fw.write ("<Adjectifs>");
				fw.write ("\r\n");
				fw.write (sb.toString());
				fw.write ("</Adjectifs>");
				fw.write ("\r\n");
				fw.close();
				is.close();
	            
	        }
	        catch (IOException e) {
	            // Model loading failed, handle the error
	            e.printStackTrace();
	        }
	        finally {
	            if (tokenModelIn != null) {
	                try {
	                    tokenModelIn.close();
	                }
	                catch (IOException e) {
	                }
	            }
	            if (posModelIn != null) {
	                try {
	                    posModelIn.close();
	                }
	                catch (IOException e) {
	                }
	            }
	        }

			ui.statusInfo.setText (" Parse Parag - sending to text area");
			ui.main_txtarea.setDocument(doc);
			
			return counter;			
		}
			@Override
			protected void done(){
				int strParsed = 0;
				try {
					strParsed = get();
					this.chrono.stop();
					ui.statusInfo.setText("Document parsed..." + strParsed+ " insertions [" + this.chrono.displayInterval() + 					"]");
				}catch(Exception e){

					JOptionPane.showMessageDialog(ui, 
							"Error :\n" + e.getLocalizedMessage(),
							"Parse", 
							JOptionPane.ERROR_MESSAGE);
					ui.statusInfo.setText("Parse error on \"ParseParag\".");
				}
			}
			
			private void detectADJ(String tokens[], String tags[], StringBuilder sb) {
					int i=0;
					while(i<tags.length){
						if(tags[i].equals("D")){
							if(i+1<tags.length) {
								if((!tags[i+1].equals("V")) && (!tags[i+1].equals("C")) && (!tags[i+1].equals("ADV"))  && (!tags[i+1].equals("P")) && (!tags[i+1].equals("PRO")) && (!tags[i+1].equals("PONCT")) && (!tags[i+1].equals("CL")) && (!tags[i+1].equals("D")) && (!tags[i+1].equals("N"))) {
									if(i+2<tags.length) {
										if(tags[i+2].equals("N")) {
											sb.append ("<Adj>"+tokens[i+1]+"</Adj>");
											sb.append ("\r\n");
											i+=2;
										}
										else {i++;}
									}
									else{i++;}
								}
						
								else if(tags[i+1].equals("ADV")) {
									if(i+3<tags.length) {
										if(tags[i+3].equals("N")) {
											if((!tags[i+2].equals("V")) && (!tags[i+2].equals("C")) && (!tags[i+2].equals("ADV"))  && (!tags[i+2].equals("P")) && (!tags[i+2].equals("PRO")) && (!tags[i+2].equals("PONCT")) && (!tags[i+2].equals("CL")) && (!tags[i+2].equals("D")) && (!tags[i+2].equals("N"))) {
												sb.append ("<Adj>"+tokens[i+2]+"</Adj>");
												sb.append ("\r\n");
												i+=3;
											}
											else {i+=3;}
										}
										else {i+=2;}
									}
									else{i++;}
								}
						
								else if(tags[i+1].equals("N")) {
									if(i+2<tags.length) {
										if((!tags[i+2].equals("V")) && (!tags[i+2].equals("C")) && (!tags[i+2].equals("ADV")) && (!tags[i+2].equals("P")) && (!tags[i+2].equals("PRO")) && (!tags[i+2].equals("PONCT")) && (!tags[i+2].equals("CL")) && (!tags[i+2].equals("D")) && (!tags[i+2].equals("N"))) {
											sb.append ("<Adj>"+tokens[i+2]+"</Adj>");
											sb.append ("\r\n");
											i+=2;
										}
							
										else if(tags[i+2].equals("ADV")) {
											if(i+3<tags.length) {
												if((!tags[i+3].equals("V")) && (!tags[i+3].equals("C")) && (!tags[i+3].equals("ADV"))  && (!tags[i+3].equals("P")) && (!tags[i+3].equals("PRO")) && (!tags[i+3].equals("PONCT")) && (!tags[i+3].equals("CL")) && (!tags[i+3].equals("D")) && (!tags[i+3].equals("N"))) {
													sb.append ("<Adj>"+tokens[i+3]+"</Adj>");
													sb.append ("\r\n");
													i+=3;
												}
												else {i+=2;}
											}
											else {i+=2;}
										}
							
										else if(tags[i+2].equals("V")) {
											if(i+3<tags.length) {
												if((!tags[i+3].equals("V")) && (!tags[i+3].equals("C")) && (!tags[i+3].equals("ADV"))  && (!tags[i+3].equals("P")) && (!tags[i+3].equals("PRO")) && (!tags[i+3].equals("PONCT")) && (!tags[i+3].equals("CL")) && (!tags[i+3].equals("D")) && (!tags[i+3].equals("N"))) {
													sb.append ("<Adj>"+tokens[i+3]+"</Adj>");
													sb.append ("\r\n");
													i+=3;
												}
								
												else if(tags[i+3].equals("ADV")) {
													if(i+4<tags.length) {
														if((!tags[i+4].equals("V")) && (!tags[i+4].equals("C")) && (!tags[i+4].equals("ADV"))  && (!tags[i+4].equals("P")) && (!tags[i+4].equals("PRO")) && (!tags[i+4].equals("PONCT")) && (!tags[i+4].equals("CL")) && (!tags[i+4].equals("D")) && (!tags[i+4].equals("N"))) {
															sb.append ("<Adj>"+tokens[i+4]+"</Adj>");
															sb.append ("\r\n");
															i+=4;
														}
														else {i+=3;}
													}
													else {i+=3;}
												}
												else {i+=2;}
											}
											else{i++;}
										}
										else {i+=1;}	
									}
									else{i++;}
								}
								else {i++;}					
							}
							else{i++;}
						}
					
						else if(tags[i].equals("CL")){
							if(i+1<tags.length) {
								if(tags[i+1].equals("V")) {
									if(i+2<tags.length) {
										if((!tags[i+2].equals("V")) && (!tags[i+2].equals("C")) && (!tags[i+2].equals("ADV")) && (!tags[i+2].equals("P")) && (!tags[i+2].equals("PRO")) && (!tags[i+2].equals("D")) && (!tags[i+2].equals("PONCT")) && (!tags[i+2].equals("CL")) && (!tags[i+2].equals("D")) && (!tags[i+2].equals("N"))) {
											sb.append ("<Adj>"+tokens[i+2]+"</Adj>");
											sb.append ("\r\n");
											i+=2;
										}
										else if(tags[i+2].equals("ADV")) {
											if(i+3<tags.length) {
												if((!tags[i+3].equals("V")) && (!tags[i+3].equals("C")) && (!tags[i+3].equals("ADV"))  && (!tags[i+3].equals("P")) && (!tags[i+3].equals("PRO")) && (!tags[i+3].equals("PONCT")) && (!tags[i+3].equals("CL")) && (!tags[i+3].equals("D")) && (!tags[i+3].equals("N"))) {
													sb.append ("<Adj>"+tokens[i+3]+"</Adj>");
													sb.append ("\r\n");
													i+=3;
												}
												else {i+=2;}
											}
											else {i+=2;}
										}
										else {i++;}
									}
									else {i++;}
								}
								else {i++;}
							}
							else{i++;}
						}
						else{i++;}
					}
			}
	}
}
