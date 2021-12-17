package gui;

import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import main.Config;

public class NQFafConfigPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	// all components
	JCheckBox cboxProgressUpdatesEnabled, cboxAutoSaveEnabled, cboxAutoDeleteEnabled;
	LabelWithTxtInput inputTimeUpdateDelay, inputProgressUpdateDelay, inputAutoSavePercentageStep, inputAutoSaveFileName, inputGpuWorkgroupSize;
	
	JCheckBox[] cboxes;
	LabelWithTxtInput[] inputs;
	
	public NQFafConfigPanel() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		// add button for restoring all default values
		JButton btnReset = new JButton("Restore default values");
		btnReset.addActionListener((e) -> {
			for(var cb : cboxes) {
				cb.setSelected((boolean) Config.getDefaultValue(cb.getName()));
			}
			for(var input : inputs) {
				input.txtField.setText(Config.getDefaultValue(input.getName()).toString());
			}
			Config.loadDefaultValues();
		});
		add(btnReset);
		
		// add inputs for all existent configs
		cboxProgressUpdatesEnabled = new JCheckBox("enable progress updates");
		cboxProgressUpdatesEnabled.setName("progressUpdatesEnabled");
		add(cboxProgressUpdatesEnabled);
		inputTimeUpdateDelay = new LabelWithTxtInput("delay between time updates: ", "ms");
		inputTimeUpdateDelay.setName("timeUpdateDelay");
		add(inputTimeUpdateDelay);
		inputProgressUpdateDelay = new LabelWithTxtInput("delay between progress updates: ", "ms");
		inputProgressUpdateDelay.setName("progressUpdateDelay");
		add(inputProgressUpdateDelay);

		cboxAutoSaveEnabled = new JCheckBox("enable automatic saving of solvers at progress specifc progress steps");
		cboxAutoSaveEnabled.setName("autoSaveEnabled");
		add(cboxAutoSaveEnabled);
		inputAutoSavePercentageStep = new LabelWithTxtInput("percentage step trigger for auto saving: ", "%");
		inputAutoSavePercentageStep.setName("autoSavePercentageStep");
		add(inputAutoSavePercentageStep);
		inputAutoSaveFileName = new LabelWithTxtInput("filename for auto saving (#N#=actual N): ", "");
		inputAutoSaveFileName.setName("autoSaveFilename");
		add(inputAutoSaveFileName);
		cboxAutoDeleteEnabled = new JCheckBox("enable automatic deletion of auto save files when the solver is done");
		cboxAutoDeleteEnabled.setName("autoDeleteEnabled");
		add(cboxAutoDeleteEnabled);

		inputGpuWorkgroupSize = new LabelWithTxtInput("workgroup size used by the GPU", "");
		inputGpuWorkgroupSize.setName("gpuWorkgroupSize");
		add(inputGpuWorkgroupSize);
		
		// add listeners to all input components
		cboxes = new JCheckBox[] {
				cboxProgressUpdatesEnabled, 
				cboxAutoSaveEnabled, 
				cboxAutoDeleteEnabled
		};
		inputs = new LabelWithTxtInput[] {
				inputTimeUpdateDelay, 
				inputProgressUpdateDelay, 
				inputAutoSavePercentageStep, 
				inputAutoSaveFileName, 
				inputGpuWorkgroupSize
		};
		for(var cb : cboxes) {
			cb.addChangeListener((e) -> {
				try {
					Config.setValue(cb.getName(), cb.isSelected());
				} catch(IllegalArgumentException e1) {}
			});
		}
		for(var input : inputs) {
			input.txtField.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void insertUpdate(DocumentEvent e) {
					if(input.txtField.getText().length() > 0)
						try {
							Config.setValue(input.getName(), input.txtField.getText());
						} catch (IllegalArgumentException e1) {}
				}
				@Override
				public void removeUpdate(DocumentEvent e) {
					if(input.txtField.getText().length() > 0)
						try {
							Config.setValue(input.getName(), input.txtField.getText());
						} catch (IllegalArgumentException e1) {}
				}
				@Override
				public void changedUpdate(DocumentEvent e) {
					if(input.txtField.getText().length() > 0)
						try {
							Config.setValue(input.getName(), input.txtField.getText());
						} catch (IllegalArgumentException e1) {}
				}
			});
		}
	}
	
	class LabelWithTxtInput extends JPanel {
		private static final long serialVersionUID = 1L;
		
		JLabel lbl, lblUnit;
		JTextField txtField;
		
		public LabelWithTxtInput(String lblText, String unit) {
			setLayout(null);
			setMaximumSize(new Dimension(1200, 20));
			
			lbl = new JLabel(lblText);
			lbl.setBounds(0, 0, 400, 20);
			txtField = new JTextField();
			txtField.setBounds(250, 0, 150, 20);
			txtField.setHorizontalAlignment(JTextField.RIGHT);
			lbl.setLabelFor(txtField);
			lblUnit = new JLabel(unit);
			lblUnit.setBounds(400, 0, 100, 20);
			
			add(lbl);
			add(txtField);
			add(lblUnit);
		}
	}
}
