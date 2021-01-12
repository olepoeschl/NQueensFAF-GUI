package gui;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.DefaultCaret;

import calc.AlgorithmStarter;
import util.FAFProcessData;

import javax.swing.JButton;
import javax.swing.JFileChooser;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.ArrayDeque;

import javax.swing.JLabel;
import java.awt.Font;

import javax.swing.JTextArea;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;

import java.awt.Color;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;

public class Gui extends JFrame {
	
	private static final long serialVersionUID = 1L;
	
	
	//Gui-Komponenten
	private JTextField tfN, tfThreadcount;
	private JSlider sliderN, sliderThreadcount;
	private JButton btnSave, btnLoad, btnStart, btnCancel;
	private static JLabel lblTime;
	private JTextArea taOutput;
	private static JProgressBar progressBar;
	
	private EventListener eventListener;
	
	//AlgorithmStarter-Objekt
	private static AlgorithmStarter algStarter;
	private Thread algThread;
	private static long time = 0;
	private long pausetime = 0, oldtime = 0;
	private boolean load = false;
	private int updateTime = 0;
	
	//FileFilter-Objekt
	private FileFilter filefilter;
	
	//Stack-Objekt für print-Methode
	private static ArrayDeque<String> msgQueue;
	public static ArrayDeque<Float> progressUpdateQueue;
	
	
	public Gui() {
		super("NQueens Algorithm FAF");
		
		eventListener = new EventListener();
		initGui();
		this.pack();
		
		filefilter = new FileFilter() {
			@Override
			public String getDescription() {
				return "Fast as fuck - Dateien (.faf)";
			}
			@Override
			public boolean accept(File f) {
				if(f.isDirectory() || f.getName().endsWith(".faf"))
					return true;
				return false;
			}
		};
		
		//Queue fürs printen in taOutput
		msgQueue = new ArrayDeque<String>();
		new Thread() {
			public void run() {
				String msg;
				while(true) {
					if(msgQueue.size() > 0) {
						msg = msgQueue.removeFirst();
						if(msg.equals("_CLEAR_"))
							taOutput.setText("");
						else
							taOutput.append(msg);
					}
					try {
						sleep(128);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
		
		//Queue fürs Anzeigen des Fortschritts
		progressUpdateQueue = new ArrayDeque<Float>();
		new Thread() {
			public void run() {
				float value;
				while(true) {
					if(progressUpdateQueue.size() > 0 && algStarter.isReady()) {
						value = progressUpdateQueue.removeFirst();
						if(value == 128f) {
							value = algStarter.getProgress()*100;
						}
						
						//aktualisiere progressBar und ihre Text-Anzeige
						if((int)value == 100 || value == 0) {
							progressBar.setValue((int)value);
							((TitledBorder)progressBar.getBorder()).setTitle("Fortschritt: " + (int)value + "%");
							progressBar.repaint();
						} else {
							progressBar.setValue((int)value);
							((TitledBorder)progressBar.getBorder()).setTitle("Fortschritt: " + (((int)(value*10000)) / 10000f) + "% \t[ " + algStarter.getCalculatedStartConstCount() + " von " + algStarter.getStartConstCount() + " ]");
							progressBar.repaint();
						}
						
						//Ausgabe
						if((int)value != 100 && value != 0) {
							print((int)value + "% berechnet      \t[ " + algStarter.getCalculatedStartConstCount() + " von " + algStarter.getStartConstCount() + " in " + Gui.getTimeStr() + " ]", true);
						}
					}
					try {
						sleep(128);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}
	
	private void initGui() {
		this.setIconImage(Toolkit.getDefaultToolkit().getImage(Gui.class.getResource("/res/faf.jpg")));
		this.setResizable(false);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		getContentPane().setLayout(new BorderLayout(0, 0));
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setEnabled(false);
		getContentPane().add(splitPane, BorderLayout.CENTER);
		
		JPanel pnlInput = new JPanel();
		splitPane.setLeftComponent(pnlInput);
		pnlInput.setLayout(new BorderLayout(0, 0));
		
		JPanel pnlTop = new JPanel();
		pnlInput.add(pnlTop, BorderLayout.NORTH);
		pnlTop.setLayout(new BorderLayout(0, 0));
		
		JPanel pnlN = new JPanel();
		pnlN.setBorder(new TitledBorder(null, "Brettgr\u00F6\u00DFe N", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		pnlTop.add(pnlN, BorderLayout.NORTH);
		
		sliderN = new JSlider();
		sliderN.setValue(16);
		sliderN.setMinimum(1);
		sliderN.setMaximum(31);
		sliderN.addChangeListener(eventListener);
		pnlN.add(sliderN);
		
		tfN = new JTextField();
		tfN.setText("16");
		tfN.setColumns(2);
		tfN.addKeyListener(eventListener);
		tfN.addFocusListener(eventListener);
		pnlN.add(tfN);
		
		JPanel pnlThreadcount = new JPanel();
		pnlThreadcount.setBorder(new TitledBorder(null, "Anzahl an Threads", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		pnlTop.add(pnlThreadcount, BorderLayout.SOUTH);
		
		sliderThreadcount = new JSlider();
		sliderThreadcount.setValue(1);
		sliderThreadcount.setMinimum(1);
		sliderThreadcount.setMaximum( Runtime.getRuntime().availableProcessors() );
		sliderThreadcount.addChangeListener(eventListener);
		pnlThreadcount.add(sliderThreadcount);
		
		tfThreadcount = new JTextField();
		tfThreadcount.setText("1");
		tfThreadcount.setColumns(2);
		tfThreadcount.addKeyListener(eventListener);
		tfThreadcount.addFocusListener(eventListener);
		pnlThreadcount.add(tfThreadcount);
		
		JPanel pnlControls = new JPanel();
		pnlControls.setBorder(new TitledBorder(null, "Controls", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		pnlInput.add(pnlControls, BorderLayout.CENTER);
		pnlControls.setLayout(new BorderLayout(0, 0));
		
		btnSave = new JButton("Speichern");
		btnSave.addActionListener(eventListener);
		btnSave.setEnabled(false);
		pnlControls.add(btnSave, BorderLayout.NORTH);
		
		btnLoad = new JButton("Lade aus Datei...");
		btnLoad.addActionListener(eventListener);
		pnlControls.add(btnLoad, BorderLayout.SOUTH);
		
		btnStart = new JButton("GO");
		btnStart.addActionListener(eventListener);
		pnlControls.add(btnStart, BorderLayout.CENTER);
		
		btnCancel = new JButton("Abbruch");
		btnCancel.addActionListener(eventListener);
		btnCancel.setEnabled(false);
		pnlControls.add(btnCancel, BorderLayout.WEST);
		
		JPanel pnlTime = new JPanel();
		pnlTime.setBorder(new TitledBorder(null, "Zeit", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		pnlInput.add(pnlTime, BorderLayout.SOUTH);
		
		lblTime = new JLabel("00:00:00.000");
		lblTime.setFont(new Font("Tahoma", Font.BOLD, 20));
		pnlTime.add(lblTime);
		
		JPanel pnlOutput = new JPanel();
		splitPane.setRightComponent(pnlOutput);
		pnlOutput.setLayout(new BorderLayout(0, 0));
		
		taOutput = new JTextArea();
		taOutput.setFont(new Font("Microsoft YaHei UI Light", Font.PLAIN, 13));
		taOutput.setForeground(new Color(102, 205, 170));
		taOutput.setColumns(40);
		taOutput.setRows(15);
		taOutput.setBackground(Color.BLACK);
		taOutput.setEditable(false);
		taOutput.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)), "Konsole", TitledBorder.LEADING, TitledBorder.TOP, null, Color.LIGHT_GRAY));
		pnlOutput.add(taOutput, BorderLayout.NORTH);
		
		JScrollPane scrollPane = new JScrollPane(taOutput);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setWheelScrollingEnabled(true);
		pnlOutput.add(scrollPane);

		DefaultCaret caret = (DefaultCaret)taOutput.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		progressBar = new JProgressBar();
		progressBar.setForeground(new Color(0, 255, 127));
		progressBar.setBackground(new Color(245, 245, 220));
		progressBar.setMinimum(0);
		progressBar.setMaximum(100);
		TitledBorder border = new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)), "0%", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0));
		border.setBorder(new LineBorder(border.getTitleColor(), 0));
		progressBar.setBorder(border);
		pnlOutput.add(progressBar, BorderLayout.SOUTH);
	}
	
	public static void print(String msg, boolean append) {
		if(append) {
			msgQueue.add(msg + "\n");
		} else {
			msgQueue.add("_CLEAR_");
			msgQueue.add(msg + "\n");
		}
	}
	public static String getTimeStr() {
		long h = time/1000/60/60;
		long m = time/1000/60%60;
		long s = time/1000%60;
		long ms = time%1000;
		
		String strh, strm, strs, strms;
		//Stunden-Anzeige
		if(h == 0) {
			strh = "00";
		} else if((h+"").toString().length() == 3) {
			strh = "" + h;
		} else if((h+"").toString().length() == 2) {
			strh = "0" + h;
		} else {
			strh = "00" + h;
		}
		//Minuten-Anzeige
		if((m+"").toString().length() == 2) {
			strm = "" + m;
		}  else {
			strm = "0" + m;
		}
		//Sekunden-Anzeige
		if((s+"").toString().length() == 2) {
			strs = "" + s;
		} else {
			strs = "0" + s;
		}
		//Millisekunden-Anzeige
		if((ms+"").toString().length() == 3) {
			strms = "" + ms;
		} else if((ms+"").toString().length() == 2) {
			strms = "0" + ms;
		} else {
			strms = "00" + ms;
		}

		return strh + ":" + strm + ":" + strs + "." + strms;
	}
	private void updateTime() {
		lblTime.setText(getTimeStr());
	}
	//Berechne und aktualisiere Progress
	public static void updateProgress() {
		progressUpdateQueue.add(128f);
	}
	private static void updateProgress(float value) {
		progressUpdateQueue.add(value);
	}
	
	private void startTimeUpdateThread() {
		//Thread zum updaten von lblTime
		new Thread() {
			public void run() {
				pausetime = 0;
				
				//Warte, solange der Algorithmus noch die Startkonstellationen berechnet
				while(algStarter.getStarttime() == 0) {
					try {
						sleep(128);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				while(updateTime == 1) {
					if(algStarter.isPaused()) {
						long pausestart = System.currentTimeMillis();
						while(algStarter.isPaused()) {
							try {
								sleep(128);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						pausetime += System.currentTimeMillis() - pausestart;
					} else {
						//aktualisiere Zeit und Zeit-Anzeige
						updateTime();
						time = System.currentTimeMillis() - algStarter.getStarttime() - pausetime + oldtime;
						
						//Warte x Millisekunden
						try {
							sleep(128);
						} catch(InterruptedException ie) {
							ie.printStackTrace();
						}
					}
				}
				updateTime = 0;
			}
		}.start();
	}
	private void startAlgThread() {
		algThread = new Thread() {
			public void run() {
				//Mach taOutput wieder clean
				print("Starte Thread(s)...", false);
				
				//Btn zum abbrechen aktivieren
				btnCancel.setEnabled(true);
				
				//Setze progressBar zurück
				progressUpdateQueue.clear();
				if(!load)
					updateProgress(0);

				//Zeit starten
				time = 0;
				updateTime = 1;

				algStarter.startAlgorithm();
				
				//Zeit stoppen
				updateTime = 2;
				while(updateTime == 2) {
					//warte solange
					try {
						sleep(128);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				if(algStarter.getEndtime() != 0)
					time = algStarter.getEndtime() - algStarter.getStarttime() - pausetime + oldtime;
				updateTime();
				//oldtime zurücksetzen
				oldtime = 0;
				
				updateProgress(100);
				print("============================\n" + algStarter.getSolvecounter() + " Lösungen gefunden für N = " + algStarter.getN() + "\n============================", true);
				
				//Buttons zurücksetzen
				btnStart.setText("GO");
				btnStart.setEnabled(true);
				btnCancel.setEnabled(false);
				btnSave.setEnabled(false);
				btnLoad.setEnabled(true);
				
				//boolean load zurücksetzen
				load = false;
			}
		};
		algThread.start();
	}
	
	public static void main(String[] args) {
		Gui gui = new Gui();
		gui.setVisible(true);
	}
	
	private class EventListener implements ChangeListener, KeyListener, FocusListener, ActionListener {
		
		//ChangeListener
		@Override
		public void stateChanged(ChangeEvent e) {
			if(e.getSource() == sliderN) {
				tfN.setText(sliderN.getValue() + "");
			} else if(e.getSource() == sliderThreadcount) {
				tfThreadcount.setText(sliderThreadcount.getValue() + "");
			}
		}
		
		//KeyListener
		@Override
		public void keyTyped(KeyEvent e) {}
		@Override
		public void keyPressed(KeyEvent e) {}
		@Override
		public void keyReleased(KeyEvent e) {
			if(e.getSource() == tfN) {
				if(tfN.getText().length() < 3) {
					try {
					     int N = Integer.parseInt(tfN.getText());
					     sliderN.setValue(N);
					} catch (NumberFormatException nfe) {
						try {
							tfN.setText(tfN.getText().substring(0, tfN.getText().length()-1));
						} catch (StringIndexOutOfBoundsException sioofe) {
							//tue nichts
						}
					}
				} else {
					while(true) {
						tfN.setText(tfN.getText().substring(0, tfN.getText().length() - 1));
						try {
							Integer.parseInt(tfN.getText());
							if(tfN.getText().length() < 3)
								break;
						} catch(NumberFormatException nfe) {
							//tue nichts
						}
					}
				}
			} else if(e.getSource() == tfThreadcount) {
				if(tfThreadcount.getText().length() < 3) {
					try {
					     int threadcount = Integer.parseInt(tfThreadcount.getText());
					     sliderThreadcount.setValue(threadcount);
					}
					catch (NumberFormatException nfe) {
						try {
							tfThreadcount.setText(tfThreadcount.getText().substring(0, tfThreadcount.getText().length()-1));
						} catch (StringIndexOutOfBoundsException sioofe) {
							//tue nichts
						}
					}
				} else {
					while(true) {
						tfThreadcount.setText(tfThreadcount.getText().substring(0, tfThreadcount.getText().length() - 1));
						try {
							Integer.parseInt(tfThreadcount.getText());
							if(tfThreadcount.getText().length() < 3)
								break;
						} catch(NumberFormatException nfe) {
							//tue nichts
						}
					}
				}
			}
		}

		//Focus-Listener der TextFelder
		@Override
		public void focusGained(FocusEvent e) {}

		@Override
		public void focusLost(FocusEvent e) {
			if(e.getSource() == tfN) {
				try {
					//wenn tfN Integer enthält, tue nichts
					Integer.parseInt(tfN.getText());
				} catch(NumberFormatException nfe) {
					//wenn nicht, setze Slider-Wert ein
					tfN.setText(sliderN.getValue() + "");
				}
			} else if(e.getSource() == tfThreadcount) {
				try {
					//wenn tfN Integer enthält, tue nichts
					Integer.parseInt(tfThreadcount.getText());
				} catch(NumberFormatException nfe) {
					//wenn nicht, setze Slider-Wert ein
					tfThreadcount.setText(sliderThreadcount.getValue() + "");
				}
			}
		}

		//ActionListener
		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == btnStart) {
				if(btnStart.getText().equals("Pause")) {
					//Pause
					algStarter.pause();
					btnStart.setText("Weiter");
					btnSave.setEnabled(true);
				} else {
					if(algThread != null && algThread.isAlive()) {
						//Wenn pausiert, dann lass ihn weiterlaufen
						algStarter.go();
						btnStart.setText("Pause");
						btnSave.setEnabled(false);
					} else {
						//wenn Algorithmus noch nicht läuft, starte ihn und die anderen Threads
						btnStart.setText("Pause");
						btnLoad.setEnabled(false);
												
						//Wenn keine Datei geladen wurde:
						//Inputdaten von den GUI-Komponenten holen und AlgorithmStarter initialisieren
						int threadcount = Integer.parseInt(tfThreadcount.getText());
						if(!load) {
							int N = Integer.parseInt(tfN.getText());
							
							//initialisiere neues AlgorithmStarter-Objekt
							algStarter = new AlgorithmStarter(N, threadcount);
						}
						
						
						//Starte alle Threads
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								startAlgThread();
								startTimeUpdateThread();
							}
						});
					}
				}
			}
			else if(e.getSource() == btnCancel) {
				algStarter.cancel();
				if(algStarter.isPaused())
					algStarter.go();
				print("##### Abgebrochen #####", true);
			}
			else if(e.getSource() == btnSave){
				new Thread() {
					public void run() {
						//Dateipfad auswählen
						String filepath = "", filename = "";
						JFileChooser filechooser = new JFileChooser();
						filechooser.setMultiSelectionEnabled(false);
						filechooser.setCurrentDirectory(null);
						filechooser.setAcceptAllFileFilterUsed(false);
						filechooser.addChoosableFileFilter(filefilter);
						filechooser.setFileFilter(filefilter);
						if(filechooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
							filepath = filechooser.getSelectedFile().getAbsolutePath();
							filename = filechooser.getSelectedFile().getName().toString();
							if( ! filepath.endsWith(".faf") ) {
								filepath = filepath + ".faf";
								filename = filename + ".faf";
							}
						}
						
						//Speichere fafprocessdata in Dateipfad filename
						FAFProcessData fafprocessdata = new FAFProcessData();
						fafprocessdata.addAll(algStarter.getUncalculatedStartConstellations());
						fafprocessdata.N = algStarter.getN();
						fafprocessdata.solvecounter = algStarter.getSolvecounter();
						fafprocessdata.startConstCount = algStarter.getStartConstCount();
						fafprocessdata.calculatedStartConstCount = algStarter.getCalculatedStartConstCount();
						fafprocessdata.time = time;
						fafprocessdata.save(filepath);
						
						print("/- Aktueller Prozess wurde erfolgreich in Datei " + filename + " gespeichert. \\-", true);
					}
				}.start();
			}
			else if(e.getSource() == btnLoad) {
				//Dateipfad auswählen
				String filepath = "";
				JFileChooser filechooser = new JFileChooser();
				filechooser.setMultiSelectionEnabled(false);
				filechooser.setCurrentDirectory(null);
				filechooser.setAcceptAllFileFilterUsed(false);
				filechooser.addChoosableFileFilter(filefilter);
				filechooser.setFileFilter(filefilter);
				if(filechooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
					filepath = filechooser.getSelectedFile().getAbsolutePath();
					
					//Lade FAFProcessData us Dateipfad filename
					FAFProcessData fafprocessdata = FAFProcessData.load(filepath);
					
					//initialisiere AlgorithmStarter mit den geladenen Daten
					int threadcount = Integer.parseInt(tfThreadcount.getText());
					algStarter = new AlgorithmStarter(fafprocessdata.N, threadcount);
					algStarter.load(fafprocessdata);
					
					//aktualisiere Gui mit den geladenen Werten
					sliderN.setValue(fafprocessdata.N);
					tfN.setText(fafprocessdata.N + "");
					updateProgress(0);
					
					oldtime = fafprocessdata.time;
					
					//Datei geladen
					load = true;
					
					print("/- Alter Prozess wurde erfolgreich aus Datei " + filechooser.getSelectedFile().getName().toString() + " geladen. \\-", false);
					print("/- Zum Starten GO drücken \\-", true);
				} else {
					print("/- Laden abgebrochen \\-", true);
				}
			}
		}
	}
}
