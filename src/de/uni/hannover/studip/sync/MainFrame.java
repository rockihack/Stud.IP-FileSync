package de.uni.hannover.studip.sync;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import de.uni.hannover.studip.sync.views.Login;
import de.uni.hannover.studip.sync.views.Main;
import de.uni.hannover.studip.sync.views.Settings;

public class MainFrame extends JFrame {
	
	/**
	 * Singleton instance.
	 */
	private static final MainFrame singletonInstance = new MainFrame();

	private static final long serialVersionUID = 1L;
	
	public static final String LOGIN_VIEW = "Login";
	public static final String MAIN_VIEW = "Main";
	public static final String SETTINGS_VIEW = "Settings";
	
	/**
	 * GUI title.
	 */
	private static final String TITLE = "Stud.IP Document Sync 0.1";

	/**
	 * Menu bar.
	 */
	private JMenuBar menuBar;
	private JMenu fileMenu, helpMenu;
	
	/**
	 * File menu items.
	 */
	private JMenuItem settingsItem, exitItem;
	
	/**
	 * Help menu items.
	 */
	private JMenuItem aboutItem;
	
	/**
	 * Singleton instance getter.
	 * 
	 * @return
	 */
	public static MainFrame getInstance() {
		return singletonInstance;
	}

	/**
	 * Create the GUI and show it. For thread safety,
	 * this method should be invoked from the
	 * event-dispatching thread.
	 */
	private MainFrame() {
		/* Create and set up the window. */
		super(TITLE);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		/* Create menu bar. */
		createMenuBar();
		
		/* Create and set up the views. */
		setLayout(new ExtCardLayout());
		add(new Login(), LOGIN_VIEW);
		add(new Main(), MAIN_VIEW);
		add(new Settings(), SETTINGS_VIEW);
		
		/* Set active view. */
		setView(LOGIN_VIEW);
		
		/* Adjust window size. */
		setResizable(false);
		pack();
		
		/* Display the window. */
		setVisible(true);
	}
	
	/**
	 * Create the menu bar.
	 */
	private void createMenuBar() {
		/* Create menu bar. */
		menuBar = new JMenuBar();
		
		/* File menu. */
		fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		
		settingsItem = new JMenuItem("Settings");
		settingsItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				setView(MainFrame.SETTINGS_VIEW);
			}
			
		});
		fileMenu.add(settingsItem);
		
		exitItem = new JMenuItem("Exit");
		exitItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
			
		});
		fileMenu.add(exitItem);
		
		/* Help menu. */
		helpMenu = new JMenu("Help");
		menuBar.add(helpMenu);
		
		aboutItem = new JMenuItem("About");
		aboutItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(null, "About:");
			}
			
		});
		helpMenu.add(aboutItem);
		
		setJMenuBar(menuBar);
	}
	
	/**
	 * Set the active view.
	 * 
	 * @param view
	 */
	public void setView(String view) {
		JPanel contentPane = (JPanel) getContentPane();
		ExtCardLayout cl = (ExtCardLayout) contentPane.getLayout();
		cl.show(contentPane, view);
	}
	
}
