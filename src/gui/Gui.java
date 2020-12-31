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
import javax.swing.text.DefaultCaret;

import calc.AlgorithmStarter;

import javax.swing.JButton;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

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
	private JButton btnSave, btnStart;
	private JLabel lblTime;
	private JTextArea taOutput;
	private JProgressBar progressBar;
	
	private EventListener eventListener;
	
	//AlgorithmStarter-Objekt
	private AlgorithmStarter algStarter;
	private Thread algThread;
	private long time = 0, pausetime = 0;
	private boolean updateTime = true;
	
	
	public Gui() {
		super("NQueens Algorithm FAF");
		
		eventListener = new EventListener();
		initGui();
		this.pack();
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
		sliderN.setMaximum(32);
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
		sliderThreadcount.setMaximum(16);
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
		pnlControls.add(btnSave, BorderLayout.NORTH);
		
		btnStart = new JButton("GO");
		btnStart.addActionListener(eventListener);
		pnlControls.add(btnStart, BorderLayout.CENTER);
		
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
	
	private void print(String str, boolean append) {
		if(append)
			taOutput.append(str + "\n");
		else
			taOutput.setText(str);
	}
	private String updateTime() {
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
		
		lblTime.setText(strh + ":" + strm + ":" + strs + "." + strms);
		return strh + ":" + strm + ":" + strs + "." + strms;
	}
	private void startTimeUpdateThread() {
		//Thread zum updaten von lblTime
		new Thread() {
			public void run() {
				pausetime = 0;
				
				//Warte, solange der Algorithmus noch die Startkonstellationen berechnet
				while(algStarter.getStarttime() == 0) {
					try {
						sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				while(updateTime) {
					if(algStarter.isPaused()) {
						long pausestart = System.currentTimeMillis();
						while(algStarter.isPaused()) {
							try {
								sleep(50);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						pausetime += System.currentTimeMillis() - pausestart;
					} else {
						//aktualisiere Zeit und Zeit-Anzeige
						updateTime();
						time = System.currentTimeMillis() - algStarter.getStarttime() - pausetime;
						
						//Warte 1 Millisekunde
						try {
							sleep(70);
						} catch(InterruptedException ie) {
							ie.printStackTrace();
						}
					}
				}
			}
		}.start();
	}
	private void startProgressUpdateThread() {
		//Thread zum updaten von progressBar
		new Thread() {
			public void run() {
				//Warte, solange der Algorithmus noch die Startkonstellationen berechnet
				while(algStarter.getProgress() == 0) {
					try {
						sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				print(algStarter.getStartConstLen() + " Start-Konstellationen gefunden in " + updateTime(), true);
				btnStart.setEnabled(true);
				
				float value = 0;
				int intvalue = 0, tempPercentage = 0;
				while(algThread.isAlive()) {
					value = algStarter.getProgress() * 100;
					intvalue = (int) value;
					if(intvalue % 5 <= 1 && intvalue != progressBar.getValue()) {
						if(intvalue % 5 == 1 && tempPercentage != intvalue - 1) {
							tempPercentage = intvalue - 1;
							print(tempPercentage + "% berechnet      \t[ " + algStarter.getCalculatedStartConstellationsLen() + " von " + algStarter.getStartConstLen() + " in " + updateTime() + " ]", true);
						}
						else if (intvalue % 5 == 0){
							tempPercentage = intvalue;
							print(tempPercentage + "% berechnet      \t[ " + algStarter.getCalculatedStartConstellationsLen() + " von " + algStarter.getStartConstLen() + " in " + updateTime() + " ]", true);
						}	
					}
					progressBar.setValue(intvalue);
					if(value > 100)
						value = 100;
					((TitledBorder)progressBar.getBorder()).setTitle((((int)(value*100)) / 100f) + "%");
					
					
					//wenn Algorithmus fertig, verlasse Endlos-Schleife
					if( ! algThread.isAlive()) 
						break;
					
					//Warte 50 Millisekunden
					try {
						sleep(50);
					} catch(InterruptedException ie) {
						ie.printStackTrace();
					}
				}
			}
		}.start();
	}
	private void startAlgThread(int N) {
		algThread = new Thread() {
			public void run() {
				print("", false);
				//Setze progressBar zurück
				progressBar.setValue(0);
				((TitledBorder)progressBar.getBorder()).setTitle("0%");
				
				//Zeit starten
				updateTime = true;
				
				algStarter.startAlgorithm();
				
				//Zeit stoppen
				updateTime = false;
				time = algStarter.getEndtime() - algStarter.getStarttime() - pausetime;
				updateTime();
				
				progressBar.setValue(100);
				((TitledBorder)progressBar.getBorder()).setTitle("100%");
				print("============================\n" + algStarter.getSolvecounter() + " Lösungen gefunden für N = " + N + "\n============================", true);
				btnStart.setText("GO");
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
			if(e.getSource() == btnSave) {
				//Speichern des Zustandes
			} else if (e.getSource() == btnStart) {
				if(btnStart.getText().equals("Pause")) {
					//Pause
					algStarter.pause();
					btnStart.setText("Weiter");
				} else {
					if(algThread != null && algThread.isAlive()) {
						//Wenn pausiert, dann lass ihn weiterlaufen
						algStarter.go();
						btnStart.setText("Pause");
					} else {
						//wenn Algorithmus noch nicht läuft, starte ihn und die anderen Threads
						btnStart.setText("Pause");
						btnStart.setEnabled(false);
						
						//Hole Parameter von den Input-Komponenten
						int N = Integer.parseInt(tfN.getText());
						int threadcount = Integer.parseInt(tfThreadcount.getText());
						//initialisiere neues AlgorithmStarter-Objekt
						algStarter = new AlgorithmStarter(N, threadcount, false);
						
						//Starte alle Threads
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								startAlgThread(N);
								startTimeUpdateThread();
								startProgressUpdateThread();
							}
						});
					}
				}
			}
		}
	}
}
