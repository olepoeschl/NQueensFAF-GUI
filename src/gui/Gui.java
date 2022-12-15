package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import javax.swing.UIManager;
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
import de.nqueensfaf.compute.SymSolver;
import main.Config;

public class Gui extends JFrame {

	private static final long serialVersionUID = 1L;
	
	private final JFrame self = this;
	
	// gui-components
	private Image iconImg;
	private JTabbedPane tabbedPane;
	private NQFafConfigPanel pnlConfig;
	// cpu-tab
	private JTextField tfN, tfThreadcount;
	private JSlider sliderN, sliderThreadcount;
	private JPanel pnlControls, pnlStatus;
	private JButton btnStore, btnRestore, btnStart, btnPause, btnCancel;
	private JLabel lblTime, lblStatus;
	private JTextArea taOutput; 
	private JProgressBar progressBar;
	// gpu-tab
	private JComboBox<String> cboxDeviceChooser;
	// colors
	private Color clrRunning, clrPausing, clrPaused, clrCanceling, clrCanceled, clrRestored, clrProgress;
	// other
	private FileFilter filefilter;

	// Solvers
	private CpuSolver cpuSolver;
	private GpuSolver gpuSolver;
	private SymSolver symSolver;
	private Solver solver;
	// for printing a msg each 10 %
	private float lastTenPercent;
	// for printing all messages in the correct order
	private ArrayDeque<Message>msgQueue;
	// for determining if last used tab was the config tab
	private boolean configTabActive = false;
	// for determining if gpu is enabled or not
	private boolean gpuEnabled = true;
	
	public Gui() {
		super("NQueensFAF - Superfast N-Queens-problem solver");
	}
	
	public void init() {
		// initialize colors
		clrRunning = Color.YELLOW;
		clrPausing = new Color(230, 220, 100);
		clrPaused = Color.ORANGE;
		clrCanceling = new Color(220, 130, 130);
		clrCanceled = new Color(255, 40, 40);
		clrRestored = Color.CYAN;
		
		// initialize solvers
		cpuSolver = new CpuSolver();
		cpuSolver.setThreadcount(1);
		cpuSolver.addOnPauseCallback(() -> {
			pnlStatus.setBackground(clrPaused);
			lblStatus.setText("paused");
		});
		gpuSolver = new GpuSolver();
		try {
			gpuSolver.setDevice(0);
		} catch (IllegalStateException e) {
			// when this exception is thrown, there is no opencl device available. 
			// But this warning is already printed by the static code in GpuSolver.java, so we just ignore it here
		}
		symSolver = new SymSolver();
		
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
				if(progress < 1f && (int) ((progress * 100) / 10) != lastTenPercent) {
					lastTenPercent = (int) (progress * 100) / 10;
					print("Completed " + ((int) (progress*100)) + "% in " + getTimeStr(solver.getDuration()));
				}
				// update color of the progressBar
				if(solver == cpuSolver && cpuSolver.wasCanceled())
					return;
				clrProgress = getProgressColor(progress);
				pnlStatus.setBackground(clrProgress);
				progressBar.setForeground(clrProgress);
			}).addInitializationCallback(() -> {
				initializationCB();
			}).addTerminationCallback(() -> {
				terminationCB();
			});
		}
		solver = cpuSolver;

		// filefilter for the JFileChooser
		filefilter = new FileFilter() {
			@Override
			public String getDescription() {
				return "NQueensFAF - Files (.faf)";
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
	
	private void initializationCB() {
		pnlStatus.setBackground(clrRunning);
		lblStatus.setText("running .  .  .");
		progressBar.setForeground(clrRunning);
		// disable the tabs that are not selected
		lockTabs();
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
		btnPause.setEnabled(true);
		btnCancel.setEnabled(true);
		// print global work size if gpuSolver is used
		if(solver == gpuSolver) {
			new Thread(() -> {
				while(true) {
					if(gpuSolver.getGlobalWorkSize() != 0) {
						print("Enqueued " + gpuSolver.getGlobalWorkSize() + " work-items");
						break;
					}
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}
	
	private void terminationCB() {
		lblStatus.setText("finished");
		sliderN.setEnabled(true);
		sliderThreadcount.setEnabled(true);
		tfN.setEditable(true);
		tfThreadcount.setEditable(true);
		cboxDeviceChooser.setEnabled(true);
		btnStart.setEnabled(true);
		btnStore.setEnabled(false);
		btnRestore.setEnabled(true);
		btnPause.setEnabled(false);
		btnPause.setText("Pause");
		btnCancel.setEnabled(false);
		if(solver == cpuSolver) {
			if(cpuSolver.wasCanceled()) {
				progressBar.setForeground(clrCanceled);
				pnlStatus.setBackground(clrCanceled);
				lblStatus.setText("canceled");
			}
		}
		// print finishing message
		print("============================\n" + getSolutionsStr(solver.getSolutions()) + " solutions found for N = " + solver.getN());
		// enable the other tabs again
		unlockTabs();
		// show final values in gui
		progressBar.setValue((int) (solver.getProgress() * 100));
		String progressText = "progress: " + (((int)(solver.getProgress()*100*10000)) / 10000f) + "%    solutions: " + getSolutionsStr(solver.getSolutions());
		((TitledBorder) progressBar.getBorder()).setTitle(progressText);
		progressBar.repaint();
		lblTime.setText(getTimeStr(solver.getDuration()));
		
		// print solution counts 
		long solutionsUnique = symSolver.getUniqueSolutionsTotal(solver.getSolutions());
		print("\nUnique Solutions:");
		print("      With  90° symmetry: " + getSolutionsStr(symSolver.getSolutions90()));
		print("      With 180° symmetry: " + getSolutionsStr(symSolver.getSolutions180()));
		print("      Without   symmetry: " + getSolutionsStr(solutionsUnique-symSolver.getSolutions90() - symSolver.getSolutions180()));
		print("      In total: " + getSolutionsStr(solutionsUnique));
		print("============================");
	}
	
	private void initGui() {
		iconImg = Toolkit.getDefaultToolkit().getImage(Gui.class.getResource("/res/queenFire_FAF_beschnitten.png"));
		setIconImage(iconImg);
		setResizable(true);
//		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				self.setVisible(false);
				solver.finishStoring();
				System.exit(0);
			}
		});
		getContentPane().setLayout(new BorderLayout());
		
		// overall
		JPanel pnlContent = new JPanel();
		pnlContent.setLayout(new BorderLayout());
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setEnabled(false);
		pnlContent.add(splitPane, BorderLayout.CENTER);

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
				} catch(NumberFormatException nfe) {}
			}
		});
		tfN.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {}
			@Override
			public void focusLost(FocusEvent e) {
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
				} catch(NumberFormatException nfe) {}
			}
		});
		tfN.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {}
			@Override
			public void focusLost(FocusEvent e) {
				try {
					//if tfN contains Integer, update the slider
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

		btnPause = new NQFafButton("Pause");
		btnPause.addActionListener((e) -> {
			if(btnPause.getText().equals("Pause")) {
				pause();
			} else if (btnPause.getText().equals("Resume")) {
				resume();
			}
		});
		btnPause.setEnabled(false);
		pnlControls.add(btnPause, BorderLayout.WEST);

		btnCancel = new NQFafButton("Cancel");
		btnCancel.addActionListener((e) -> {
			cancel();
		});
		btnCancel.setEnabled(false);
		pnlControls.add(btnCancel, BorderLayout.EAST);
		
		JPanel pnlBottom = new JPanel();
		pnlBottom.setLayout(new BorderLayout());
		pnlInput.add(pnlBottom, BorderLayout.SOUTH);

		JPanel pnlTime = new JPanel();
		pnlTime.setBorder(new TitledBorder(null, "Time", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		pnlBottom.add(pnlTime, BorderLayout.NORTH);
		
		lblTime = new JLabel("00:00:00.000");
		lblTime.setFont(new Font("Tahoma", Font.BOLD, 20));
		pnlTime.add(lblTime, BorderLayout.CENTER);

		pnlStatus = new JPanel();
		pnlStatus.setBorder(new TitledBorder(null, "Status", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		pnlBottom.add(pnlStatus, BorderLayout.SOUTH);
		
		lblStatus = new JLabel(".  .  .");
		pnlStatus.add(lblStatus, BorderLayout.SOUTH);

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
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
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
		try {
			for(String device_info : gpuSolver.getAvailableDevices()) {
				cboxDeviceChooser.addItem(device_info);
			}
		} catch(IllegalStateException e) {
			// no OpenCL-capable device was found
			// a warning is written by the NQueensFAF library, so we don't need to print anything here
		}
		cboxDeviceChooser.setBackground(new Color(245, 245, 230));
		cboxDeviceChooser.setVisible(false);
		cboxDeviceChooser.addItemListener((e) -> {
			if(e.getSource() == cboxDeviceChooser.getItemAt(0)) {
				gpuSolver.setDevice(0);
			} else if(e.getSource() == cboxDeviceChooser.getItemAt(1)) {
				gpuSolver.setDevice(0);
			}
		});
		pnlTop.add(cboxDeviceChooser, BorderLayout.SOUTH);

		// config tab
		pnlConfig = new NQFafConfigPanel();
		// fill the config panel inputs with the current config values
		for(var cb : pnlConfig.cboxes) {
			cb.setSelected((boolean) Config.getValue(cb.getName()));
		}
		for(var input : pnlConfig.inputs) {
			input.txtField.setText(Config.getValue(input.getName()).toString());
		}
		
		// tabbedPane
		tabbedPane = new JTabbedPane();
		tabbedPane.addTab(" CPU ", pnlContent);
		tabbedPane.addTab(" GPU ", null);
		tabbedPane.addTab(" Config ", null);
		if(cboxDeviceChooser.getItemCount() == 0) {						// if no GPUs are available, disable the GPU-tab
			tabbedPane.setEnabledAt(1, false);
			gpuEnabled = false;
		}
		tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if(tabbedPane.getSelectedIndex() == 0) {
					if(configTabActive) {
						pnlContent.remove(pnlConfig);
						pnlContent.add(splitPane);
						configTabActive = false;
					}
					// show cpu gui-components
					cboxDeviceChooser.setVisible(false);
					pnlThreadcount.setVisible(true);
					btnPause.setVisible(true);
					btnCancel.setVisible(true);
					solver = cpuSolver;
				} else if(tabbedPane.getSelectedIndex() == 1) {
					if(configTabActive) {
						pnlContent.remove(pnlConfig);
						pnlContent.add(splitPane);
						configTabActive = false;
					}
					// show gpu gui-components
					cboxDeviceChooser.setVisible(true);
					pnlThreadcount.setVisible(false);
					btnPause.setVisible(false);
					btnCancel.setVisible(false);
					solver = gpuSolver;
				} else {
					// show config panel
					pnlContent.remove(splitPane);
					pnlContent.add(pnlConfig);
					configTabActive = true;
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
		filechooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
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
				solver.store(filepath, true);
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
		filechooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
		filechooser.setAcceptAllFileFilterUsed(false);
		filechooser.addChoosableFileFilter(filefilter);
		filechooser.setFileFilter(filefilter);
		if(filechooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			filepath = filechooser.getSelectedFile().getAbsolutePath();
			// restore progress
                        try {
			        gpuSolver.setWorkgroupSize((int) Config.getValue("gpuWorkgroupSize"));
		        } catch(IllegalArgumentException e) {
			        int defaultVal = (int) Config.getDefaultValue("gpuWorkgroupSize");
			        gpuSolver.setWorkgroupSize(defaultVal);
			        pnlConfig.inputGpuWorkgroupSize.txtField.setText("" + defaultVal);
			        Config.resetValue("gpuWorkgroupSize");
		        }
			try {
				solver.restore(filepath);
			} catch(ClassCastException e) {
				print("! can not restore CPU-Solver using GPU-Solver-file and vice versa !");
				return;
			} catch (ClassNotFoundException | IOException e) {
				print("! unable to restore Solver from file !");
				e.printStackTrace();
				return;
			}
			// disable the tab that is not selected
			lockTabs();
			// update gui to the restored values
			sliderN.setValue(solver.getN());
			tfN.setText(solver.getN() + "");
			progressBar.setValue((int) (solver.getProgress() * 100));
			progressBar.setForeground(clrRestored);
			pnlStatus.setBackground(clrRestored);
			lblStatus.setText("restored");
			String progressText = "progress: " + (((int)(solver.getProgress()*100*10000)) / 10000f) + "%    solutions: " + getSolutionsStr(solver.getSolutions());
			((TitledBorder) progressBar.getBorder()).setTitle(progressText);
			progressBar.repaint();
			lblTime.setText(getTimeStr(solver.getDuration()));
			sliderN.setEnabled(false);
			tfN.setEditable(false);
			if(solver == gpuSolver)
				btnCancel.setVisible(true);
			btnCancel.setEnabled(true);
			print("> Progress successfully restored from file '" + filechooser.getSelectedFile().getName().toString() + "'. ", true);
		}
	}
	
	private void start() {
		if(!solver.isIdle())
			return;
		symSolver.reset();
		if(!solver.isRestored()) {
			solver.reset();
			// reset progress
			progressBar.setValue(0);
			String progressText = "progress: 0.0%    solutions: 0";
			((TitledBorder) progressBar.getBorder()).setTitle(progressText);
			progressBar.repaint();
			lblTime.setText("00:00:00.000");
		} else {
			if(solver == gpuSolver)
				btnCancel.setVisible(false);
		}
		lastTenPercent = (int) (solver.getProgress() * 100) / 10;

		// apply config values
		solver.setProgressUpdatesEnabled((boolean) Config.getValue("progressUpdatesEnabled"));
		try {
			solver.setTimeUpdateDelay((long) Config.getValue("timeUpdateDelay"));
		} catch(IllegalArgumentException e) {
			long defaultVal = (long) Config.getDefaultValue("timeUpdateDelay");
			solver.setTimeUpdateDelay(defaultVal);
			pnlConfig.inputTimeUpdateDelay.txtField.setText("" + defaultVal);
			Config.resetValue("timeUpdateDelay");
		}
		try {
			solver.setProgressUpdateDelay((long) Config.getValue("progressUpdateDelay"));
		} catch(IllegalArgumentException e) {
			long defaultVal = (long) Config.getDefaultValue("progressUpdateDelay");
			solver.setProgressUpdateDelay(defaultVal);
			pnlConfig.inputProgressUpdateDelay.txtField.setText("" + defaultVal);
			Config.resetValue("progressUpdateDelay");
		}
		solver.setAutoSaveEnabled((boolean) Config.getValue("autoSaveEnabled"));
		try {
			solver.setAutoSavePercentageStep((int) Config.getValue("autoSavePercentageStep"));
		} catch(IllegalArgumentException e) {
			int defaultVal = (int) Config.getDefaultValue("autoSavePercentageStep");
			solver.setAutoSavePercentageStep(defaultVal);
			pnlConfig.inputAutoSavePercentageStep.txtField.setText("" + defaultVal);
			Config.resetValue("autoSavePercentageStep");
		}
		solver.setAutoSaveFilename((String) Config.getValue("autoSaveFilename"));
		solver.setAutoDeleteEnabled((boolean) Config.getValue("autoDeleteEnabled"));
		try {
			gpuSolver.setWorkgroupSize((int) Config.getValue("gpuWorkgroupSize"));
		} catch(IllegalArgumentException e) {
			int defaultVal = (int) Config.getDefaultValue("gpuWorkgroupSize");
			gpuSolver.setWorkgroupSize(defaultVal);
			pnlConfig.inputGpuWorkgroupSize.txtField.setText("" + defaultVal);
			Config.resetValue("gpuWorkgroupSize");
		}
		try {
			gpuSolver.setNumberOfPresetQueens((int) Config.getValue("presetQueens"));
		} catch(IllegalArgumentException e) {
			int defaultVal = (int) Config.getDefaultValue("presetQueens");
			gpuSolver.setNumberOfPresetQueens(defaultVal);
			pnlConfig.inputPresetQueens.txtField.setText("" + defaultVal);
			Config.resetValue("presetQueens");
		}
		
		// set N and choose solver 
		solver.setN(sliderN.getValue());
		symSolver.setN(sliderN.getValue());
		if(tabbedPane.getSelectedIndex() == 0) {			// CPU-Tab is selected
			cpuSolver.setThreadcount(sliderThreadcount.getValue());
		} else if(tabbedPane.getSelectedIndex() == 1) {		// GPU-Tab is selected
			gpuSolver.setDevice(cboxDeviceChooser.getSelectedIndex());
		}
		
		// start solver 
		try {
			symSolver.solveAsync();
			solver.solveAsync();
		} catch (Exception e) {
			print("! " + e.getMessage() + " !");
		}
	}
	
	private void pause() {
		if(solver != cpuSolver)
			return;
		if(!solver.isRunning()) {
			return;
		}
		progressBar.setForeground(clrPausing);
		btnPause.setText("Resume");
		pnlStatus.setBackground(clrPausing);
		lblStatus.setText("pausing .  .  .");
		cpuSolver.pause();
	}
	
	private void resume() {
		if(solver != cpuSolver)
			return;
		if(!solver.isRunning()) {
			return;
		}
		progressBar.setForeground(clrRunning);
		btnPause.setText("Pause");
		btnCancel.setEnabled(true);
		pnlStatus.setBackground(clrRunning);
		lblStatus.setText("running .  .  .");
		cpuSolver.resume();
	}
	
	private void cancel() {
		// if the user restored() a faf file but now wants to "unload" it so that the gui is in its default state again
		if(solver.isRestored() && !solver.isRunning()) {
			solver.reset();
			// reset progress
			progressBar.setValue(0);
			String progressText = "progress: 0.0%    solutions: 0";
			((TitledBorder) progressBar.getBorder()).setTitle(progressText);
			progressBar.repaint();
			
			print("", true);
			lblTime.setText("00:00:00.000");
			lblStatus.setText(".  .  .");
			pnlStatus.setBackground(UIManager.getColor( "Panel.background" ));
			sliderN.setEnabled(true);
			sliderThreadcount.setEnabled(true);
			tfN.setEditable(true);
			tfThreadcount.setEditable(true);
			cboxDeviceChooser.setEnabled(true);
			btnStart.setEnabled(true);
			btnStore.setEnabled(false);
			btnRestore.setEnabled(true);
			btnPause.setEnabled(false);
			btnPause.setText("Pause");
			btnCancel.setEnabled(false);
			if(solver == gpuSolver)
				btnCancel.setVisible(false);
			unlockTabs();	// enable the other tabs again
			return;
		}
		if(solver != cpuSolver)
			return;
		if(!solver.isRunning()) {
			return;
		}
		btnPause.setText("Resume");
		btnCancel.setEnabled(false);
		pnlStatus.setBackground(clrCanceling);
		lblStatus.setText("canceling .  .  .");
		cpuSolver.cancel();
	}
	
	// utility methods
	private void lockTabs() {
		for(int i = 0; i < tabbedPane.getTabCount(); i++) {
			if(i != tabbedPane.getSelectedIndex()) {
				tabbedPane.setEnabledAt(i, false);
			}
		}
	}
	
	private void unlockTabs() {
		for(int i = 0; i < tabbedPane.getTabCount(); i++) {
			if(i != tabbedPane.getSelectedIndex()) {
				if(i == 1)	// if i is index of gpuTab
					tabbedPane.setEnabledAt(i, gpuEnabled);
				else
					tabbedPane.setEnabledAt(i, true);
			}
		}
	}
	
	private Color getProgressColor(double progress) {
	    double H = progress * 0.3; // Hue (note 0.4 = Green, see huge chart below)
	    double S = 0.9; // Saturation
	    double B = 0.9; // Brightness

	    return Color.getHSBColor((float)H, (float)S, (float)B);
	}

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
	
	public void print(String msg, boolean clear) {
		synchronized(msgQueue){
			msgQueue.addLast(new Message(msg, clear));
		}
	}
	
	public void print(String msg) {
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
