package gui;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

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

import org.lwjgl.LWJGLException;

import calc.Solvers;
import util.FAFProcessData;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

import java.awt.Font;
import java.awt.Image;

import javax.swing.JTextArea;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;

// an awesome gui

public class Gui extends JFrame {
    
    private static final long serialVersionUID = 1L;
    
    // time that the helper-threads sleep after 1 iteration
    private final int sleeptime = 128;
    
    // Event-Listener for the components
    private EventListener eventListener;
    
    // gui-components
    private JFrame context;
    private Image iconImg;
    private JTabbedPane tabbedPane;
    // cpu-tab
    private JTextField tfN, tfThreadcount;
    private JSlider sliderN, sliderThreadcount;
    private JPanel pnlControls;
    private JButton btnSave, btnLoad, btnStart, btnCancel;
    private JLabel lblTime;
    private JTextArea taOutput; 
    private JProgressBar progressBar;
    // OpenCL-tab
    private JComboBox<String> cbDeviceChooser;
    
    // components for the waiting-dialog
    private JOptionPane optionPane;
    private JLabel waitlbl;
    private String[] options = {"Back", "Save and Back", "Save and quit instead", "Only quit"};
    private JDialog dialog;
    private Object input;
    
    // Solvers
    private Solvers solvers;
    
    // helper variables
    private long time = 0, pausetime = 0, oldtime = 0;
    private boolean paused = false;
    private int updateTime = 0;
    
    // FileFilter
    private FileFilter filefilter;
    
    // stack-object for print-method
    private static ArrayDeque<String> msgQueue;
    public static ArrayDeque<Float> progressUpdateQueue;
    
    // other
    private StringBuilder strbuilder;
    
    
    public Gui() {
        super("NQueens Algorithm FAF");
        context = this;
        
        // initialize things that are needed for the initialization of the gui
        eventListener = new EventListener();
        iconImg = Toolkit.getDefaultToolkit().getImage(Gui.class.getResource("/res/queenFire_FAF_beschnitten.png"));
        
        // initialize Solvers
        solvers = new Solvers();
        
        // initialize GuiAlt
        initGui();
        this.pack();
        Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation((int) (screensize.getWidth()/2 - this.getWidth()/2), (int) (screensize.getHeight()/2 - this.getHeight()/2));
        
        // filefilter for the JFileChooser
        filefilter = new FileFilter() {
            @Override
            public String getDescription() {
                return "Fast as f*** - Files (.faf)";
            }
            @Override
            public boolean accept(File f) {
                if(f.isDirectory() || f.getName().endsWith(".faf"))
                    return true;
                return false;
            }
        };
        
        // Queue for printing in taOutput
        msgQueue = new ArrayDeque<String>();
        // Queue displaying the progress
        progressUpdateQueue = new ArrayDeque<Float>();
        // start the thread that updates the GuiAlt's components
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                startGuiUpdateThread();
            }
        });
    }
    
    private void initGui() {
        this.setIconImage(iconImg);
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.addWindowListener(new WindowListener() {
			@Override
			public void windowOpened(WindowEvent e) {}
			@Override
			public void windowClosing(WindowEvent e) {
				setVisible(false);
				
				try {
					// platform specific binary
					String prefix = "";
					String suffix = "";
					String os = System.getProperty("os.name").toLowerCase();
					if(os.contains("win")) {
						// windows
						prefix = "wscript.exe ";
						suffix = ".vbs";
					} else if(os.contains("mac")) {
						// mac
						prefix = "sh ";
						suffix = ".sh";
					} else if(os.contains("nix") || os.contains("nux") || os.contains("aix")) {
						// unix (linux etc)
						prefix = "sh ";
						suffix = ".sh";
					} else if(os.contains("sunos")) {
						// solaris
						prefix = "sh ";
						suffix = ".sh";
					} else {
						// unknown os
						System.err.println("No cleanup-executable available for this operating system (" + os + ").");
					}
					
					// if there is a binary for this operating system, use it to clean up the temporary files created by this program ( -> lwjgl-binaries)
					if(suffix.length() > 0) {
						// clean the temp-directory that was created for the lwjgl-native binary
						InputStream in = Solvers.class.getClassLoader().getResourceAsStream("bin/NQueensFaf_Cleanup" + suffix);
						byte[] buffer = new byte[1024];
						int read = -1;
						File file = File.createTempFile("NQueensFaf_Cleanup", suffix);
						FileOutputStream fos = new FileOutputStream(file);
						while((read = in.read(buffer)) != -1) {
							fos.write(buffer, 0, read);
						}
						fos.close();
						in.close();

						Runtime.getRuntime().exec(prefix + file.getAbsolutePath());
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			@Override
			public void windowClosed(WindowEvent e) {}

			@Override
			public void windowIconified(WindowEvent e) {}
			@Override
			public void windowDeiconified(WindowEvent e) {}

			@Override
			public void windowActivated(WindowEvent e) {}
			@Override
			public void windowDeactivated(WindowEvent e) {}
        });
        this.getContentPane().setLayout(new BorderLayout());    
        
        // overall + cpu-tab
        JSplitPane splitPane = new JSplitPane();
        splitPane.setEnabled(false);
        
        JPanel pnlInput = new JPanel();
        splitPane.setLeftComponent(pnlInput);
        pnlInput.setLayout(new BorderLayout(0, 0));
        
        JPanel pnlTop = new JPanel();
        pnlInput.add(pnlTop, BorderLayout.NORTH);
        pnlTop.setLayout(new BorderLayout(0, 0));
        
        JPanel pnlN = new JPanel();
        pnlN.setBorder(new TitledBorder(null, "Board size N", TitledBorder.LEADING, TitledBorder.TOP, null, null));
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
        pnlThreadcount.setBorder(new TitledBorder(null, "Number of threads", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        pnlTop.add(pnlThreadcount, BorderLayout.CENTER);
        
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
        
        pnlControls = new JPanel();
        pnlControls.setBorder(new TitledBorder(null, "Controls", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        pnlInput.add(pnlControls, BorderLayout.CENTER);
        pnlControls.setLayout(new BorderLayout(0, 0));
        
        btnSave = new JButton("Save");
        btnSave.addActionListener(eventListener);
        btnSave.setEnabled(false);
        pnlControls.add(btnSave, BorderLayout.NORTH);
        
        btnLoad = new JButton("Load from file...");
        btnLoad.addActionListener(eventListener);
        pnlControls.add(btnLoad, BorderLayout.SOUTH);
        
        btnStart = new JButton("START");
        btnStart.addActionListener(eventListener);
        pnlControls.add(btnStart, BorderLayout.CENTER);
        
        btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(eventListener);
        btnCancel.setEnabled(false);
        pnlControls.add(btnCancel, BorderLayout.WEST);
        
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
        progressBar.setForeground(new Color(0, 255, 127));
        progressBar.setBackground(new Color(245, 245, 220));
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        TitledBorder border = new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)), "0%", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0));
        border.setBorder(new LineBorder(border.getTitleColor(), 0));
        progressBar.setBorder(border);
        pnlOutput.add(progressBar, BorderLayout.SOUTH);
        
        // for the dialog
        optionPane = new JOptionPane();
        optionPane.setMessageType(JOptionPane.PLAIN_MESSAGE);
        optionPane.setMessage("Things you can do instead of waiting:");
        optionPane.setOptions(options);
        optionPane.setOptionType(JOptionPane.YES_NO_CANCEL_OPTION);
        optionPane.setValue(JOptionPane.YES_OPTION);
        
        waitlbl = new JLabel();
        optionPane.add(waitlbl);
        
        // OpenCL-tab
        cbDeviceChooser = new JComboBox<String>();
        cbDeviceChooser.setBorder(new TitledBorder(null, "Device - Chooser", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        try {
			for(String device_info : solvers.listDevices()) {
				cbDeviceChooser.addItem(device_info);
			}
		} catch (LWJGLException e1) {
			e1.printStackTrace();
		}
        cbDeviceChooser.setVisible(false);
        pnlTop.add(cbDeviceChooser, BorderLayout.SOUTH);
        
        // other
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab(" CPU ", splitPane);
        tabbedPane.addTab(" OpenCL ", null);
        if(cbDeviceChooser.getItemCount() == 0)						// if no opencl-devices are available, disbale the OpenCL-tab
        	tabbedPane.setEnabledAt(1, false);
        tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if(tabbedPane.getSelectedIndex() == 0) {
					solvers.setMode(Solvers.USE_CPU);
					// show important gui-components
					cbDeviceChooser.setVisible(false);
					pnlThreadcount.setVisible(true);
					btnSave.setVisible(true);
					btnLoad.setVisible(true);
					btnCancel.setVisible(true);
				} else if(tabbedPane.getSelectedIndex() == 1) {
					solvers.setMode(Solvers.USE_GPU);
					// hide unnessesary gui-components
					cbDeviceChooser.setVisible(true);
					pnlThreadcount.setVisible(false);
					btnSave.setVisible(false);
					btnLoad.setVisible(false);
					btnCancel.setVisible(false);
				}
			}
		});
        this.getContentPane().add(tabbedPane);
    }
    private void startGuiUpdateThread() {
        new Thread() {
            public void run() {
                float value;
                int tempvalue = 0;
                String msg;
                long pausestart = 0;
                
                while(true) {
                	// update time and check if the user paused the application
                	if(paused) {
                		if(pausestart == 0) {
                			pausestart = System.currentTimeMillis();
                    		updateTime();
                		}
                		else {
                			pausetime = System.currentTimeMillis() - pausestart;
                		}
                	} else {
                		pausestart = 0;
                		if(updateTime == 1 && !paused) {
                    		// display and update time
                    		updateTime();
                    	} else {
                    		updateTime = 0;
                    	}
                	}
                	updateTimeLbl();


                    // Updating the progress (progressBar, text, percentage in console[taOutput])
                    if(progressUpdateQueue.size() > 0) {
                        value = progressUpdateQueue.removeFirst();
                        if(value == 128f) {
                        	value = solvers.getProgress();
                        }
                        
                        // update progressBar, text and the Windows-Taskbar-Icon-Progressbar
                    	progressBar.setValue((int)value);
                    	String progressData = "Progress: " + (((int)(value*10000)) / 10000f) + "%    ";
                    	((TitledBorder)progressBar.getBorder()).setTitle(progressData);
                        if(solvers.isReady()) {
                        	progressData += "[ " + solvers.getSolvedStartConstCount() + " of " + solvers.getStartConstCount() + " ]        ";
                        	progressData += "[ solutions: " + getSolvecounterStr(solvers.getSolvecounter()) + " ]";
                        	((TitledBorder)progressBar.getBorder()).setTitle(progressData);

                        	// output
                        	if((int)value >= tempvalue + 5 || (int) value < tempvalue) {
                        		print((int)value + "% calculated      \t[ " + solvers.getSolvedStartConstCount() + " of " + solvers.getStartConstCount() + " in " + getTimeStr() + " ]", true);
                        		tempvalue = (int) value;
                        	}
                        	
                        	if(((int) value) == 100 || ((int) value) == 0) {
                        		tempvalue = 0;
                        	}
                        }
                    	progressBar.repaint();


                    }

                    // fetch new progress-values
                    if(solvers.isReady()) {
                        updateProgress();
                    }
                    
                    // output string from queue
                    if(msgQueue.size() > 0) {
                        msg = msgQueue.removeFirst();
                        if(msg.equals("_CLEAR_"))
                            taOutput.setText("");
                        else
                            taOutput.append(msg);
                    }
                    
                    // wait short time
                    try {
                        Thread.yield();
                        Thread.sleep(sleeptime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }
    
    public static void print(String msg, boolean append) {
        if(append) {
            msgQueue.add(msg + "\n");
        } else {
            msgQueue.add("_CLEAR_");
            msgQueue.add(msg + "\n");
        }
    }
    public String getTimeStr() {
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
    public String getSolvecounterStr(long solvecounter) {
        strbuilder = new StringBuilder( Long.toString(solvecounter) );
        int len = strbuilder.length();
        for(int i = len-3; i > 0; i -= 3) {
            strbuilder.insert(i, ".");
        }
        return strbuilder.toString();
    }
    private void updateTime() {
    	time = System.currentTimeMillis() - solvers.getStarttime() - pausetime + oldtime;
    }
    private void updateTimeLbl() {
        lblTime.setText(getTimeStr());
    }
    // calculate and update progress
    public static void updateProgress() {
        progressUpdateQueue.add(128f);
    }
    private static void updateProgress(float value) {
        progressUpdateQueue.add(value);
    }
    
    private void startSolver() {
    	new Thread() {
    		public void run() {
    			String mode = null;
    			switch(solvers.getMode()) {
    			case Solvers.USE_CPU:
    				mode = "CPU";
    				break;
    			case Solvers.USE_GPU:
    				mode = "GPU";
    				break;
    			}
                print("Starting " + mode + "-Solver...", false);
                
                // start time updates
                time = 0;
                updateTime = 1;
                
                // start solver
                solvers.solve();
                
                // update gui objects and variables
                // stop time updates
                updateTime = 2;
                while(updateTime == 2) {
                    // wait before cheking again
                    try {
                        sleep(sleeptime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                // update gui objects and variables
                time = solvers.getEndtime() - solvers.getStarttime() - pausetime + oldtime;
                updateTimeLbl();
                pausetime = 0;
                oldtime = 0;
                updateProgress();

                print("============================\n" + solvers.getSolvecounter() + " solutions found for N = " + solvers.getN() + "\n============================", true);
                
                // reset gui objects
                btnStart.setText("START");
                btnStart.setEnabled(true);
                btnCancel.setEnabled(false);
                btnSave.setEnabled(false);
                btnLoad.setEnabled(true);
                unlockTabs();							// unlock tabs so that user can switch again between them
    		}
    	}.start();
    }
    
    // save state of running algorithm instance
    private void save() {
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
        
        // store fafprocessdata in path filename
        if( ! filepath.equals("")) {
            FAFProcessData fafprocessdata = new FAFProcessData();
            ArrayDeque<Integer> constellations = solvers.getUnsolvedStartConstellations();
            int len = constellations.size();
            for(int i = 0; i < len; i++) {
                fafprocessdata.add(constellations.removeFirst());
            }
            fafprocessdata.N = solvers.getN();
            fafprocessdata.solvecounter = solvers.getSolvecounter();
            fafprocessdata.startConstCount = solvers.getStartConstCount();
            fafprocessdata.calculatedStartConstCount = solvers.getSolvedStartConstCount();
            fafprocessdata.time = time;
            fafprocessdata.save(filepath);
            
            print("# Current process was successfully saved in file " + filename + ".", true);
        }
    }
    // load state of old algorithm instance
    private void load() {
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
            
            // load FAFProcessData from filepath filename
            FAFProcessData fafprocessdata = FAFProcessData.load(filepath);
            
            // initialize CpuSolverAlt with the loaded data
            int threadcount = Integer.parseInt(tfThreadcount.getText());
            solvers.setN(fafprocessdata.N);
            solvers.setThreadcount(threadcount);
            solvers.load(fafprocessdata);
            
            // update gui to the loaded values
            sliderN.setValue(fafprocessdata.N);
            tfN.setText(fafprocessdata.N + "");
            updateProgress();
            
            oldtime = fafprocessdata.time;
            time = oldtime;
            updateTimeLbl();
            
            print("# Old process was successfully loaded from File " + filechooser.getSelectedFile().getName().toString() + ". ", false);
            print("# Press START to continue it ", true);
        } else {
            print("# Loading file was canceled ", true);
        }
    }
    
    private void showWaitingDialog(int code) {
        // code = 0  := pausing
        // code = 1  := canceling
        // show Dialog
        new Thread() {
            public void run() {
                
                dialog = optionPane.createDialog(null, "Waiting for the Algorithm to finish current start-constellation...");
                dialog.setLocation(context.getX() + context.getWidth()/2 - dialog.getWidth()/2, context.getY() + context.getHeight()/2 - dialog.getHeight()/2);
                input = JOptionPane.UNINITIALIZED_VALUE;
                
                // for hte loading animation
                int counter = 0, countertemp = 0;
                
                new Thread() {
                    public void run() {
                        dialog.setVisible(true);
                        input = optionPane.getValue();
                    }
                }.start();

                // make buttons not pressable
                for(Component c : pnlControls.getComponents()) {
                    c.setEnabled(false);
                }
                
                while(true) {
                    if(input == null) {
                        // back to main gui
                        if(code == 0)
                            solvers.go();
                        else
                            solvers.dontCancel();
                        break;
                    } else if ( input.equals(options[0]) ) {
                        // back to main gui
                        if(code == 0)
                            solvers.go();
                        else
                            solvers.dontCancel();
                        break;
                    } else if( input.equals(options[1]) ) {
                        // save and back
                        save();
                        if(code == 0)
                            solvers.go();
                        else
                            solvers.dontCancel();
                    } else if( input.equals(options[2]) ) {
                        // save and quit
                        save();
                        System.exit(0);
                    } else if( input.equals(options[3]) ) {
                        // only quit
                        System.exit(0);
                    }
                    
                    // if the algorithm responds, close the waiting-dialog
                    if(solvers.responds()) {
                        if(code == 0) {
                            paused = true;
                            btnStart.setText("Continue");
                        } else {
                            paused = false;
                        }
                        dialog.dispose();
                        break;
                    }
                    // if the algorithm is done, close the dialog
                    if(solvers.getEndtime() != 0) {
                        paused = false;
                        dialog.dispose();
                        break;
                    }
                    
                    if(input != JOptionPane.UNINITIALIZED_VALUE) {
                        dialog.dispose();
                        break;
                    }
                    
                    // waiting animation on the waitLbl
                    waitlbl.setText("");
                    countertemp = counter % 24;
                    for(int i = 0; i < 20; i++) {
                        if(i == countertemp) {
                            waitlbl.setText(waitlbl.getText() + ") ");
                        } else if(i == countertemp-1) {
                            waitlbl.setText(waitlbl.getText() + "? ");
                        } else if(i == countertemp-2) {
                            waitlbl.setText(waitlbl.getText() + "? ");
                        } else if(i == countertemp-3) {
                            waitlbl.setText(waitlbl.getText() + "? ");
                        } else if(i == countertemp-4) {
                            waitlbl.setText(waitlbl.getText() + "( ");
                        } else {
                            waitlbl.setText(waitlbl.getText() + "_ ");
                        }
                    }
                    counter++;
                    
                    try {
                        Thread.sleep(sleeptime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // reset the respond-variables of each CpuSolverThread
                solvers.resetRespond();
                
                // print message
                if(code == 1 && input != null && !input.equals(options[0]) && !input.equals(options[1])) {
                    print("##### Algorithm canceled #####", true);
                }
                
                // make buttons pressable again
                if(solvers.getEndtime() == 0) {
                    for(Component c : pnlControls.getComponents()) {
                        if(c != btnLoad)
                            c.setEnabled(true);
                    }
                }
            }
        }.start();
    }
    
    private void lockTabs() {
    	if(tabbedPane.getTabCount() > 0) {
    		for(int i = 0; i < tabbedPane.getTabCount(); i++) {
    			if(i != tabbedPane.getSelectedIndex())
    				tabbedPane.setEnabledAt(i, false);
    		}
    	}
    }
    private void unlockTabs() {
    	if(tabbedPane.getTabCount() > 0) {
    		for(int i = 0; i < tabbedPane.getTabCount(); i++) {
    			if(i != tabbedPane.getSelectedIndex())
    				tabbedPane.setEnabledAt(i, true);
    		}
    	}
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
                            // do nothing
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
                            // do nothing
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
                            // do nothing
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
                            // do nothing
                        }
                    }
                }
            }
        }

        //Focus-Listener of the text fields
        @Override
        public void focusGained(FocusEvent e) {}
        public void focusLost(FocusEvent e) {
            if(e.getSource() == tfN) {
                try {
                    //if tfN contains Integer, do nothing
                    Integer.parseInt(tfN.getText());
                } catch(NumberFormatException nfe) {
                    // if not, insert the slider value
                    tfN.setText(sliderN.getValue() + "");
                }
            } else if(e.getSource() == tfThreadcount) {
                try {
                    //if tfN contains Integer, do nothing
                    Integer.parseInt(tfThreadcount.getText());
                } catch(NumberFormatException nfe) {
                    // if not, insert the slider value
                    tfThreadcount.setText(sliderThreadcount.getValue() + "");
                }
            }
        }

        //ActionListener
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == btnStart) {
                if(btnStart.getText().equals("Pause")) {
                    // pause
                    solvers.pause();
                    
                    // show dialog for pause-option
                    showWaitingDialog(0);
                } else {
                    if(paused) {
                        // if paused, continue
                        paused = false;
                        solvers.go();
                        btnStart.setText("Pause");
                    } else {
                    	// update gui objects
                        btnCancel.setEnabled(true);
                        btnLoad.setEnabled(false);
                        btnSave.setEnabled(true);
                    	lockTabs();								// lock tabs so that the user cant use other solvers while using one
                    	print("", false);						// clean up taOutput
                        progressUpdateQueue.clear();			// reset progressBar
                        updateProgress(0);

                        switch(solvers.getMode()) {
                        case Solvers.USE_CPU:
                        	btnStart.setText("Pause");
                        	int threadcount = Integer.parseInt(tfThreadcount.getText());
                        	solvers.setThreadcount(threadcount);
                        	break;
                        case Solvers.USE_GPU:
                            btnStart.setText(". . .");
                            btnStart.setEnabled(false);
                    		solvers.setDevice(cbDeviceChooser.getSelectedIndex());
                        	break;
                        }
                        int N = Integer.parseInt(tfN.getText());
                        solvers.setN(N);
                        
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                startSolver();
                            }
                        });
                    }
                }
            }
            else if(e.getSource() == btnCancel) {
                solvers.cancel();
                if(solvers.isPaused())
                    solvers.go();
                
                // show dialog for cancel-option
                showWaitingDialog(1);
            }
            else if(e.getSource() == btnSave){
                new Thread() {
                    public void run() {
                        save();
                    }
                }.start();
            }
            else if(e.getSource() == btnLoad) {
                load();
            }
        }
    }
}
