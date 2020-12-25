package mainpackage;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class Gui extends JFrame {
	
	//juckt nicht
	private static final long serialVersionUID = 1L;
	//---
	//Gui-Größe und -Position
	Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize();
	Dimension winsize = new Dimension(screensize.width/5, screensize.height/5);
	Point winpos = new Point(screensize.width/2 - winsize.width/2, screensize.height/2 - winsize.height/2 - winsize.height/4);
	//Gui-Komponenten
	Container con;
	JPanel pnlInput, pnlOutput, pnlN, pnlCpu, pnlOutputCenter;
	JButton btnStart, btnPause;
	JTextField tfN, tfCpu;
	JLabel lblN, lblCpu, lblTrys, lblResults, lblTime, lblLoading;
	JCheckBox cbPausable;
	//Listener
	EventListener listener;
	
	//AlgorithmStarter_alt
	AlgorithmStarter_alt algStarter;
	Thread guiUpdater;
	
	
	public Gui() {
		super("N-Damen-Problem");
		this.setSize(winsize);
		this.setLocation(winpos);
		this.setResizable(false);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		//Container
		con = new Container();
		con = this.getContentPane();
		con.setLayout(new BorderLayout());
		
		//Listener
		listener = new EventListener();
		
		//Komponenten
		//links
		pnlInput = new JPanel();
		pnlInput.setLayout(new BoxLayout(pnlInput, BoxLayout.Y_AXIS));
		con.add(pnlInput, BorderLayout.WEST);
		
		pnlInput.add(Box.createRigidArea(new Dimension(5, 5)));			//Freier Raum
		
		btnStart = new JButton("Start");
		btnStart.addActionListener(listener);
		pnlInput.add(btnStart);
		
		pnlInput.add(Box.createRigidArea(new Dimension(5, 5)));			//siehe oben
		
		btnPause = new JButton("Pause");
		btnPause.addActionListener(listener);
		btnPause.setEnabled(false);
		pnlInput.add(btnPause);

		pnlN = new JPanel();
		pnlN.setLayout(new FlowLayout());
		pnlInput.add(pnlN);
		
		pnlCpu = new JPanel();
		pnlCpu.setLayout(new FlowLayout());
		pnlInput.add(pnlCpu);
		
		pnlInput.add(Box.createRigidArea(new Dimension(5, 10)));		//siehe oben
		
		lblN = new JLabel("N = ");
		pnlN.add(lblN);
		
		tfN = new JTextField("16");
		tfN.setPreferredSize(new Dimension(30, 30));
		pnlN.add(tfN);

		pnlInput.add(Box.createRigidArea(new Dimension(5, 5)));			//siehe oben
		
		lblCpu = new JLabel("Cpu = ");
		pnlCpu.add(lblCpu);
		
		tfCpu = new JTextField("8");
		tfCpu.setPreferredSize(new Dimension(30, 30));
		pnlCpu.add(tfCpu);
		
		cbPausable = new JCheckBox("Pausierbar");
		cbPausable.setSelected(true);
		pnlInput.add(cbPausable);
		
		//rechts
		pnlOutput = new JPanel();
		pnlOutput.setPreferredSize(new Dimension(200, 150));
		pnlOutput.setLayout(new GridBagLayout());
		pnlOutput.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.BLACK));
		con.add(pnlOutput, BorderLayout.EAST);
		
		pnlOutputCenter = new JPanel();
		pnlOutputCenter.setLayout(new BoxLayout(pnlOutputCenter, BoxLayout.Y_AXIS));
		pnlOutput.add(pnlOutputCenter);
		
		
		pnlInput.add(Box.createRigidArea(new Dimension(10, 10)));
		
		lblTrys = new JLabel("... Versuche");
		lblTrys.setAlignmentX(Component.CENTER_ALIGNMENT);
		pnlOutputCenter.add(lblTrys);

		pnlInput.add(Box.createRigidArea(new Dimension(10, 10)));
		
		lblResults = new JLabel("... Lösungen");
		lblResults.setAlignmentX(Component.CENTER_ALIGNMENT);
		pnlOutputCenter.add(lblResults);

		pnlInput.add(Box.createRigidArea(new Dimension(10, 10)));
		
		lblTime = new JLabel("Zeit: ");
		lblTime.setAlignmentX(Component.CENTER_ALIGNMENT);
		pnlOutputCenter.add(lblTime);
		
		lblLoading = new JLabel("");
		lblLoading.setAlignmentX(Component.CENTER_ALIGNMENT);
		lblLoading.setForeground(Color.RED);
		pnlOutputCenter.add(lblLoading);
		
		
		this.pack();
	}
	
	public void showResults(long trycounter, long solvecounter, String time) {
		lblTrys.setText("<html><font color='green'>" + trycounter + "</font> Versuche</html>");
		lblResults.setText("<html><font color='green'>" + solvecounter + "</font> Lösungen</html>");
		lblTime.setText("<html>Zeit: <font color='green'>" + time + "</font></html>");
	}
	
	
	class EventListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			
			if(e.getSource() == btnStart) {	
				if( ! (tfN.getText().equals("") || tfCpu.getText().equals("")) ) {
					btnStart.setEnabled(false);
					if(cbPausable.isSelected())
						btnPause.setEnabled(true);
					cbPausable.setEnabled(false);
					
					int N = Integer.parseInt( tfN.getText() );
					int cpu = Integer.parseInt( tfCpu.getText() );
					algStarter = new AlgorithmStarter_alt(N, cpu, cbPausable.isSelected());
					new Thread() {
						public void run() {
							algStarter.startAlgorithm();
						}
					}.start();
					
					guiUpdater = new Thread() {
						public void run() {
							while(true) {
								if(algStarter.isFinished()) {
									showResults(algStarter.getTrycounter(), algStarter.getSolvecounter(), algStarter.getTime());
									break;
								} else {
									if( ! algStarter.isPaused() ) {
										if(lblLoading.getText().equals("o"))
											lblLoading.setText("x");
										else
											lblLoading.setText("o");
									}
								}
								try {
									Thread.sleep(500);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}

							btnStart.setEnabled(true);
							btnPause.setEnabled(false);
							cbPausable.setEnabled(true);
							lblLoading.setText("");
						}
					};
					guiUpdater.setPriority(3);
					guiUpdater.start();
				}
			}
			else if(e.getSource() == btnPause) {
				if( ! algStarter.isPaused() ) {
					algStarter.hold();
					btnPause.setText("Weiter");
				} else {
					algStarter.go();
					btnPause.setText("Pause");
				}
			}
			
		}
	}

	
	
	public static void main(String[] args) {
		Gui gui = new Gui();
		gui.setVisible(true);
	}
}
