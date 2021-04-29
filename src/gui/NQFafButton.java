package gui;

import java.awt.Color;

import javax.swing.ButtonModel;
import javax.swing.JButton;

public class NQFafButton extends JButton {
	private static final long serialVersionUID = 1L;
	
	public NQFafButton() {
		super();
		config();
	}
	public NQFafButton(String text) {
		super(text);
		config();
	}
	
	private void config() {
		Color bg = getBackground();
		Color disabledBg = new Color(243, 243, 247);
		
		setBackground(bg);
		getModel().addChangeListener(e -> {
			ButtonModel model = (ButtonModel) e.getSource();
			if(!model.isEnabled()) {
				setBackground(disabledBg);
			} else {
				setBackground(bg);
			}
		});
	}
}
