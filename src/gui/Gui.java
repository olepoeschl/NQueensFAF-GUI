package gui;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.JButton;
import java.awt.Toolkit;
import javax.swing.JLabel;
import java.awt.Font;
import javax.swing.JTextArea;
import javax.swing.JProgressBar;
import java.awt.Color;
import javax.swing.border.EtchedBorder;
import javax.swing.UIManager;

public class Gui extends JFrame {
	private JTextField textField;
	private JTextField textField_1;

	
	
	public Gui() {
		super("NQueens Algorithm FAF");
		setIconImage(Toolkit.getDefaultToolkit().getImage(Gui.class.getResource("/res/faf.jpg")));
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		initGui();
		this.pack();
	}
	
	private void initGui() {
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
		
		JPanel pnlFieldsize = new JPanel();
		pnlFieldsize.setBorder(new TitledBorder(null, "Brettgr\u00F6\u00DFe N", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		pnlTop.add(pnlFieldsize, BorderLayout.NORTH);
		
		JSlider slider = new JSlider();
		slider.setValue(16);
		slider.setMinimum(1);
		slider.setMaximum(32);
		pnlFieldsize.add(slider);
		
		textField = new JTextField();
		textField.setText("16");
		pnlFieldsize.add(textField);
		textField.setColumns(2);
		
		JPanel pnlThreadcount = new JPanel();
		pnlThreadcount.setBorder(new TitledBorder(null, "Anzahl an Threads", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		pnlTop.add(pnlThreadcount, BorderLayout.SOUTH);
		
		JSlider slider_1 = new JSlider();
		slider_1.setValue(1);
		slider_1.setMinimum(1);
		slider_1.setMaximum(16);
		pnlThreadcount.add(slider_1);
		
		textField_1 = new JTextField();
		textField_1.setText("1");
		pnlThreadcount.add(textField_1);
		textField_1.setColumns(2);
		
		JPanel pnlControls = new JPanel();
		pnlControls.setBorder(new TitledBorder(null, "Controls", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		pnlInput.add(pnlControls, BorderLayout.CENTER);
		
		JButton btnNewButton = new JButton("Start");
		pnlControls.add(btnNewButton);
		
		JButton btnNewButton_1 = new JButton("Speichern");
		pnlControls.add(btnNewButton_1);
		
		JPanel pnlTime = new JPanel();
		pnlTime.setBorder(new TitledBorder(null, "Zeit", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		pnlInput.add(pnlTime, BorderLayout.SOUTH);
		
		JLabel lblTime = new JLabel("00:00.000");
		lblTime.setFont(new Font("Tahoma", Font.BOLD, 20));
		pnlTime.add(lblTime);
		
		JPanel pnlOutput = new JPanel();
		splitPane.setRightComponent(pnlOutput);
		pnlOutput.setLayout(new BorderLayout(0, 0));
		
		JTextArea taOutput = new JTextArea();
		taOutput.setFont(new Font("Microsoft YaHei UI Light", Font.PLAIN, 13));
		taOutput.setText("Hallo ich bin cool fick dich ");
		taOutput.setForeground(new Color(102, 205, 170));
		taOutput.setColumns(40);
		taOutput.setBackground(Color.BLACK);
		taOutput.setEditable(false);
		taOutput.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)), "Konsole", TitledBorder.LEADING, TitledBorder.TOP, null, Color.LIGHT_GRAY));
		taOutput.setRows(15);
		pnlOutput.add(taOutput, BorderLayout.NORTH);
		
		JProgressBar progressBar = new JProgressBar();
		progressBar.setForeground(new Color(0, 255, 127));
		progressBar.setBackground(Color.LIGHT_GRAY);
		progressBar.setValue(50);
		pnlOutput.add(progressBar, BorderLayout.SOUTH);
	}
	
	
	public static void main(String[] args) {
		Gui gui = new Gui();
		gui.setVisible(true);
	}
}
