package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.DefaultCaret;

import de.nqueensfaf.Solver;
import de.nqueensfaf.compute.CpuSolver;
import de.nqueensfaf.compute.GpuSolver;

public class Gui extends JFrame {

	private static final long serialVersionUID = 1L;
	
	// gui-components
	private Image iconImg;
	private JTabbedPane tabbedPane;
	// cpu-tab
	private JTextField tfN, tfThreadcount;
	private JSlider sliderN, sliderThreadcount;
	private JPanel pnlControls;
	private JButton btnStore, btnRestore, btnStart;
	private JLabel lblTime;
	private JTextArea taOutput; 
	private JProgressBar progressBar;
	// gpu-tab
	private JComboBox<String> cboxDeviceChooser;
	// other
	private FileFilter filefilter;

	// Solvers
	private CpuSolver cpuSolver;
	private GpuSolver gpuSolver;
	private Solver solver;
	// for printing a msg each 10 %
	private float lastPercentageStep;
	// for printing all messages in the correct order
	private ArrayDeque<Message>msgQueue;
	
	public Gui() {
		super("NQueensFAF - Superfast N-Queens-problem solver");
		
		cpuSolver = new CpuSolver();
		cpuSolver.setThreadcount(1);
		gpuSolver = new GpuSolver();
		gpuSolver.setDevice(0);
		// initialize solver callbacks
		var solvers = new Solver[2];
		solvers[0] = cpuSolver;
		solvers[1] = gpuSolver;
		for(Solver solver : solvers) {
			solver.setOnTimeUpdateCallback((duration) -> {
				lblTime.setText(getTimeStr(duration));
			}).setOnProgressUpdateCallback((progress, solutions) -> {
				progressBar.setValue((int) (progress * 100));
				String progressText = "progress: " + (((int)(progress*100*10000)) / 10000f) + "%    solutions: " + getSolutionsStr(solver.getSolutions());
				((TitledBorder) progressBar.getBorder()).setTitle(progressText);
				progressBar.repaint();
				if(progress < 1.0 && progress >= lastPercentageStep+0.1) {
					lastPercentageStep = (float) (Math.round(progress / 0.1) * 0.1);
					print("Completed " + ((int) (lastPercentageStep*100)) + "% in " + getTimeStr(solver.getDuration()));
				}
			}).addInitializationCallback(() -> {
				// disable the tab that is not selected
				tabbedPane.setEnabledAt((tabbedPane.getSelectedIndex()-1)*-1, false);
				if(solver == cpuSolver)
					print("Starting CPU-Solver for N=" + solver.getN() + "...", true);
				else if(solver == gpuSolver)
					print("Starting GPU-Solver for N=" + solver.getN() + "...", true);
				sliderN.setEnabled(false);
				sliderThreadcount.setEnabled(false);
				tfN.setEditable(false);
				tfThreadcount.setEditable(false);
				cboxDeviceChooser.setEnabled(false);
				btnStart.setEnabled(false);
				btnStore.setEnabled(true);
				btnRestore.setEnabled(false);
			}).addTerminationCallback(() -> {
				sliderN.setEnabled(true);
				sliderThreadcount.setEnabled(true);
				tfN.setEditable(true);
				tfThreadcount.setEditable(true);
				cboxDeviceChooser.setEnabled(true);
				btnStart.setEnabled(true);
				btnStore.setEnabled(false);
				btnRestore.setEnabled(true);
				print("============================\n" + solver.getSolutions() + " solutions found for N = " + solver.getN() + "\n============================");
				// enable the other tab again
				tabbedPane.setEnabledAt((tabbedPane.getSelectedIndex()-1)*-1, true);	
			});
		}
		solver = cpuSolver;

		// filefilter for the JFileChooser
		filefilter = new FileFilter() {
			@Override
			public String getDescription() {
				return "Fast as fuck - Files (.faf)";
			}
			@Override
			public boolean accept(File f) {
				if(f.isDirectory() || f.getName().endsWith(".faf"))
					return true;
				return false;
			}
		};

		initGui();
		pack();
		Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((int) (screensize.getWidth()/2 - getWidth()/2), (int) (screensize.getHeight()/2 - getHeight()/2));
		
		// for printing all messages in order
		msgQueue = new ArrayDeque<Message>();
		new Thread(() -> {
			while(true) {
				if(msgQueue.size() > 0) {
					Message msg = msgQueue.removeFirst();
					if(msg.clear) {
						taOutput.setText(msg.msg + "\n");
					} else {
						taOutput.setText(taOutput.getText() + msg.msg + "\n");
					}
				}
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	private void initGui() {
		iconImg = Toolkit.getDefaultToolkit().getImage(Gui.class.getResource("/res/queenFire_FAF_beschnitten.png"));
		setIconImage(iconImg);
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout());
		
		// overall + cpu-tab
		JSplitPane splitPane = new JSplitPane();
		splitPane.setEnabled(false);

		JPanel pnlInput = new JPanel();
		pnlInput.setLayout(new BorderLayout(0, 0));
		splitPane.setLeftComponent(pnlInput);

		JPanel pnlTop = new JPanel();
		pnlTop.setLayout(new BorderLayout(0, 0));
		pnlInput.add(pnlTop, BorderLayout.NORTH);

		JPanel pnlN = new JPanel();
		pnlN.setBorder(new TitledBorder(null, "Board size N", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		pnlTop.add(pnlN, BorderLayout.NORTH);

		sliderN = new JSlider();
		sliderN.setValue(16);
		sliderN.setMinimum(1);
		sliderN.setMaximum(31);
		sliderN.addChangeListener((e) -> {
			tfN.setText(sliderN.getValue() + "");
		});
		pnlN.add(sliderN);

		tfN = new JTextField();
		tfN.setText("16");
		tfN.setColumns(2);
		tfN.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {}
			@Override
			public void keyPressed(KeyEvent e) {}
			@Override
			public void keyReleased(KeyEvent e) {
				try {
					//if tfN contains Integer, update the slider
					sliderN.setValue(Integer.parseInt(tfN.getText()));
				} catch(NumberFormatException nfe) {
					// if not, insert the slider value
					tfN.setText(sliderN.getValue() + "");
				}
			}
		});
		pnlN.add(tfN);

		JPanel pnlThreadcount = new JPanel();
		pnlThreadcount.setBorder(new TitledBorder(null, "Number of threads", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		pnlTop.add(pnlThreadcount, BorderLayout.CENTER);

		sliderThreadcount = new JSlider();
		sliderThreadcount.setValue(1);
		sliderThreadcount.setMinimum(1);
		sliderThreadcount.setMaximum( Runtime.getRuntime().availableProcessors() );
		sliderThreadcount.addChangeListener((e) -> {
			tfThreadcount.setText(sliderThreadcount.getValue() + "");
		});
		pnlThreadcount.add(sliderThreadcount);

		tfThreadcount = new JTextField();
		tfThreadcount.setText("1");
		tfThreadcount.setColumns(2);
		tfThreadcount.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {}
			@Override
			public void keyPressed(KeyEvent e) {}
			@Override
			public void keyReleased(KeyEvent e) {
				try {
					//if tfThreadcount contains Integer, update the slider
					sliderThreadcount.setValue(Integer.parseInt(tfThreadcount.getText()));
				} catch(NumberFormatException nfe) {
					// if not, insert the slider value
					tfThreadcount.setText(sliderThreadcount.getValue() + "");
				}
			}
		});
		pnlThreadcount.add(tfThreadcount);

		pnlControls = new JPanel();
		pnlControls.setBorder(new TitledBorder(null, "Controls", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		BorderLayout controlsLayout = new BorderLayout(0, 0);
		controlsLayout.setHgap(2);
		controlsLayout.setVgap(2);
		pnlControls.setLayout(controlsLayout);
		pnlInput.add(pnlControls, BorderLayout.CENTER);

		JPanel pnlControlsTop = new JPanel();
		pnlControlsTop.setLayout(new BorderLayout());
		pnlControls.add(pnlControlsTop, BorderLayout.NORTH);

		btnStore = new NQFafButton("Store");
		btnStore.addActionListener((e) -> {
			store();
		});
		btnStore.setEnabled(false);
		pnlControlsTop.add(btnStore, BorderLayout.SOUTH);

		btnRestore = new NQFafButton("Restore from file...");
		btnRestore.addActionListener((e) -> {
			restore();
		});
		pnlControls.add(btnRestore, BorderLayout.SOUTH);

		btnStart = new NQFafButton("Start");
		btnStart.addActionListener((e) -> {
			start();
		});
		pnlControls.add(btnStart, BorderLayout.CENTER);

		JPanel pnlTime = new JPanel();
		pnlTime.setBorder(new TitledBorder(null, "Time", TitledBorder.LEADING, TitledBorder.TOP, null, null));
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
		pnlOutput.add(taOutput, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane(taOutput);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setWheelScrollingEnabled(true);
		scrollPane.setBackground(Color.BLACK);
		scrollPane.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)), "Console", TitledBorder.LEADING, TitledBorder.TOP, null, Color.LIGHT_GRAY));
		pnlOutput.add(scrollPane);

		DefaultCaret caret = (DefaultCaret)taOutput.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		progressBar = new JProgressBar();
		progressBar.setForeground(Color.GREEN);
		progressBar.setBackground(new Color(245, 245, 230));
		progressBar.setMinimum(0);
		progressBar.setMaximum(100);
		TitledBorder border = new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)), "0.0%", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0));
		border.setBorder(new LineBorder(border.getTitleColor(), 0));
		progressBar.setBorder(border);
		pnlOutput.add(progressBar, BorderLayout.SOUTH);

		// gpu-tab
		cboxDeviceChooser = new JComboBox<String>();
		cboxDeviceChooser.setBorder(new TitledBorder(null, "Device", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		for(String device_info : gpuSolver.getAvailableDevices()) {
			cboxDeviceChooser.addItem(device_info);
		}
		cboxDeviceChooser.setBackground(new Color(243, 243, 247));
		cboxDeviceChooser.setVisible(false);
		cboxDeviceChooser.addItemListener((e) -> {
			if(e.getSource() == cboxDeviceChooser.getItemAt(0)) {
				gpuSolver.setDevice(0);
			} else if(e.getSource() == cboxDeviceChooser.getItemAt(1)) {
				gpuSolver.setDevice(0);
			}
		});
		pnlTop.add(cboxDeviceChooser, BorderLayout.SOUTH);

		// tabbedPane
		tabbedPane = new JTabbedPane();
		tabbedPane.addTab(" CPU ", splitPane);
		tabbedPane.addTab(" GPU ", null);
		if(cboxDeviceChooser.getItemCount() == 0)						// if no GPUs are available, disable the GPU-tab
			tabbedPane.setEnabledAt(1, false);
		tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if(tabbedPane.getSelectedIndex() == 0) {
					// show important gui-components
					cboxDeviceChooser.setVisible(false);
					pnlThreadcount.setVisible(true);
					solver = cpuSolver;
				} else if(tabbedPane.getSelectedIndex() == 1) {
					// hide unnessesary gui-components
					cboxDeviceChooser.setVisible(true);
					pnlThreadcount.setVisible(false);
					solver = gpuSolver;
				}
			}
		});
		getContentPane().add(tabbedPane, BorderLayout.CENTER);
	}

	private void store() {
		// choose file path
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

		// store progress data in path filename
		if(!filepath.equals("")) {
			try {
				showLoadingAnimation();
				solver.store(filepath);
				hideLoadingAnimation();
			} catch (IOException e) {
				print("! unable to restore Solver from file '" + filepath + "': " + e.getMessage() + " !");
				return;
			}
			print("> Progress successfully saved in file '" + filename + "'.");
		}
	}

	private void restore() {
		// choose filepath
		String filepath = "";
		JFileChooser filechooser = new JFileChooser();
		filechooser.setMultiSelectionEnabled(false);
		filechooser.setCurrentDirectory(null);
		filechooser.setAcceptAllFileFilterUsed(false);
		filechooser.addChoosableFileFilter(filefilter);
		filechooser.setFileFilter(filefilter);
		if(filechooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			filepath = filechooser.getSelectedFile().getAbsolutePath();
			// restore progress
			try {
				solver.restore(filepath);
			} catch(ClassCastException e) {
				print("! can not restore CPU-Solver using GPU-Solver-file and vice versa !");
				return;
			} catch (ClassNotFoundException | IOException e) {
				print("! unable to restore Solver from file '" + filepath + "': " + e.getMessage() + " !");
				return;
			}
			// disable the tab that is not selected
			tabbedPane.setEnabledAt((tabbedPane.getSelectedIndex()-1)*-1, false);
			// update gui to the restored values
			sliderN.setValue(solver.getN());
			tfN.setText(solver.getN() + "");
			progressBar.setValue((int) (solver.getProgress() * 100));
			String progressText = "progress: " + (((int)(solver.getProgress()*100*10000)) / 10000f) + "%    solutions: " + getSolutionsStr(solver.getSolutions());
			((TitledBorder) progressBar.getBorder()).setTitle(progressText);
			progressBar.repaint();
			lblTime.setText(getTimeStr(solver.getDuration()));
			sliderN.setEnabled(false);
			tfN.setEditable(false);
			print("> Progress successfully restored from file '" + filechooser.getSelectedFile().getName().toString() + "'. ", true);
		}
	}
	
	private void start() {
		if(!solver.isRestored()) {
			solver.reset();
			// reset progress
			progressBar.setValue(0);
			String progressText = "progress: 0.0%    solutions: 0";
			((TitledBorder) progressBar.getBorder()).setTitle(progressText);
			progressBar.repaint();
		}
		lastPercentageStep = (float) (Math.round(solver.getProgress() / 0.1) * 0.1);
		
		solver.setN(sliderN.getValue());
		if(tabbedPane.getSelectedIndex() == 0) {			// CPU-Tab is selected
			cpuSolver.setThreadcount(sliderThreadcount.getValue());
		} else if(tabbedPane.getSelectedIndex() == 1) {		// GPU-Tab is selected
			gpuSolver.setDevice(cboxDeviceChooser.getSelectedIndex());
		}
		
		try {
			solver.solveAsync();
		} catch (Exception e) {
			print("! " + e.getMessage() + " !");
		}
	}
	
	// utility methods
	private String getTimeStr(long time) {
		long h = time/1000/60/60;
		long m = time/1000/60%60;
		long s = time/1000%60;
		long ms = time%1000;

		String strh, strm, strs, strms;
		// hours
		if(h == 0) {
			strh = "00";
		} else if((h+"").toString().length() == 3) {
			strh = "" + h;
		} else if((h+"").toString().length() == 2) {
			strh = "0" + h;
		} else {
			strh = "00" + h;
		}
		// minutes
		if((m+"").toString().length() == 2) {
			strm = "" + m;
		}  else {
			strm = "0" + m;
		}
		// seconds
		if((s+"").toString().length() == 2) {
			strs = "" + s;
		} else {
			strs = "0" + s;
		}
		// milliseconds
		if((ms+"").toString().length() == 3) {
			strms = "" + ms;
		} else if((ms+"").toString().length() == 2) {
			strms = "0" + ms;
		} else {
			strms = "00" + ms;
		}

		return strh + ":" + strm + ":" + strs + "." + strms;
	}
	
	private String getSolutionsStr(long solvecounter) {
		StringBuilder strbuilder = new StringBuilder(Long.toString(solvecounter));
		int len = strbuilder.length();
		for(int i = len-3; i > 0; i -= 3) {
			strbuilder.insert(i, ".");
		}
		return strbuilder.toString();
	}
	
	private void showLoadingAnimation() {
		// TODO
	}
	
	private void hideLoadingAnimation() {
		// TODO
	}
	
	private void print(String msg, boolean clear) {
		synchronized(msgQueue){
			msgQueue.addLast(new Message(msg, clear));
		}
	}
	
	private void print(String msg) {
		synchronized(msgQueue){
			msgQueue.addLast(new Message(msg, false));
		}
	}
	
	private class Message {
		String msg;
		boolean clear;
		public Message(String msg, boolean clear) {
			this.msg = msg;
			this.clear = clear;
		}
	}
}
