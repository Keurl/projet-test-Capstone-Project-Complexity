package fr.launchmycraft.launcher;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.google.gson.JsonSyntaxException;

import fr.launchmycraft.library.Configuration;
import fr.launchmycraft.library.util.Util;

@SuppressWarnings("serial")
public class OptionsPanel extends JPanel
{
	ExecutableMain launcher;
	
	JCheckBox javaPathBox;
	JTextField javaPathField;
	
	JCheckBox javaMemBox;
	JTextField javaMemMinField;
	JTextField javaMemMaxField;
	
	JCheckBox forceUpdateBox;
	
	OptionsPanel oPanel = this;
	
	Configuration configuration;
	
	public OptionsPanel(final ExecutableMain launcher) throws JsonSyntaxException, FileNotFoundException, IOException
	{
		this.launcher = launcher;
		
		configuration = Util.getConfiguration(launcher.hasPaid, launcher.identifier);
		
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(5, 5, 5, 5));
		setPreferredSize(new Dimension(400, 180));
		
		JPanel optionsPanel = new JPanel();
		optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
		
		//Javapanel
		JPanel javaPanel = new JPanel(new GridBagLayout());
		javaPanel.setBorder(BorderFactory.createTitledBorder("Options systèmes"));
		
		GridBagConstraints jc = new GridBagConstraints();
		jc.insets = new Insets(2, 2, 2, 2);
	    jc.anchor = GridBagConstraints.WEST;
	    
	    jc.gridy = 0;    
	    javaPathBox = new JCheckBox("Chemin vers Java :");
	    javaPathBox.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e)
			{
				javaPathField.setEnabled(javaPathBox.isSelected());			
			}	    	
	    });
	    javaPanel.add(javaPathBox, jc);
	    
	    jc.fill = 2;
	    jc.weightx = 1;
	    javaPathField = new JTextField();
	    javaPanel.add(javaPathField, jc);
	    jc.weightx = 0;
	    jc.fill = 0;
	    
	    if (configuration.javaPath.get(launcher.launcherId) == null)
	    {
	    	javaPathField.setText(OperatingSystem.getCurrentPlatform().getJavaDir());
	    	javaPathField.setEnabled(false);
	    }
	    else
	    {
	    	javaPathField.setText(configuration.javaPath.get(launcher.launcherId));
	    	javaPathBox.setSelected(true);
	    }
		
	    optionsPanel.add(javaPanel);
	    
	    //Launcherpanel
	    JPanel launcherPanel = new JPanel();    
	    launcherPanel.setLayout(new GridBagLayout());
	    launcherPanel.setBorder(BorderFactory.createTitledBorder("Options du launcher"));
	    
	    GridBagConstraints lc = new GridBagConstraints();
	    lc.insets = new Insets(2, 2, 2, 2);
	    lc.weightx = 1;
	    lc.anchor = GridBagConstraints.WEST;
	    
	    forceUpdateBox = new JCheckBox("Forcer le retéléchargement du jeu");
	    forceUpdateBox.setSelected(launcher.forceUpdate);
	    lc.gridy = 0;
	    launcherPanel.add(forceUpdateBox, lc);    
	    
	    JButton consoleButton = new JButton("Afficher la console du launcher");
	    consoleButton.addActionListener(new ActionListener()
	    {
			@Override
			public void actionPerformed(ActionEvent e) 
			{		
				closeWindow();
				ExecutableMain.consoleFrame.setLocationRelativeTo(ExecutableMain.frame);
				ExecutableMain.consoleFrame.setVisible(true);			
			}
	    	
	    });
	    lc.gridy++;
	    launcherPanel.add(consoleButton, lc);
	    
	    optionsPanel.add(launcherPanel);
	    
	    //Savepanel
	    JPanel savePanel = new JPanel();
	    savePanel.setLayout(new GridBagLayout());
	    GridBagConstraints c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.EAST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.weightx = 1;
	    c.gridx = 0;
	    c.gridy = 0;
	    
	    c.insets = new Insets(5, 5, 5, 5);
	    
	    c.gridwidth = 2;
	    savePanel.add(new JSeparator(JSeparator.HORIZONTAL), c);    
	    
	    JButton saveButton = new JButton("Sauvegarder");  
	    saveButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				//Java Path
				if (javaPathBox.isSelected())
				{
					configuration.javaPath.put(launcher.launcherId, javaPathField.getText());
				}
				else
				{
					configuration.javaPath.remove(launcher.launcherId);
				}
				
				//ForceUpdate
				launcher.forceUpdate = forceUpdateBox.isSelected();
				
				//Sauvegarde
				try
				{
					Util.saveConfiguration(configuration, launcher.hasPaid, launcher.identifier);
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
					JOptionPane.showMessageDialog(null, "Impossible d'enregistrer les options (" + ex.getLocalizedMessage() + ").", "Erreur", JOptionPane.ERROR_MESSAGE);
				}
						
				closeWindow();
			}    	
	    });
	    
	    JButton cancelButton = new JButton("Annuler");    
	    cancelButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				closeWindow();
			}    	
	    });
	    
	    c.fill = 0;
	    c.gridy++;	 
	    c.gridwidth = 1;
	    c.weightx = 0;
	    savePanel.add(saveButton, c);
	    
	    c.gridx++;
	    savePanel.add(cancelButton, c);
    
	    this.add(optionsPanel, BorderLayout.NORTH);
	    this.add(savePanel, BorderLayout.SOUTH);
	}
	
	public void closeWindow()
	{
		Window window = (Window)getTopLevelAncestor();
	    window.dispatchEvent(new WindowEvent(window, 201));		
	}
}
