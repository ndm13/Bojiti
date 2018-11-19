package net.miscfolder.bojiti.test.mvc;

import net.miscfolder.bojiti.test.mvc.swing.FancyCellRenderer;
import net.miscfolder.bojiti.test.mvc.swing.ProgressTableModel;
import net.miscfolder.bojiti.test.mvc.swing.URITableModel;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GUITestDriver extends JFrame{
	private final JButton add = new JButton("Add URI...");
	private final JButton toggle = new JButton("Start");
	private final JSplitPane mainSplit;
	private final JList<URI> found = new JList<>();

	private final Controller controller = new Controller();

	private GUITestDriver(){
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		BorderLayout layout = new BorderLayout();
		layout.addLayoutComponent(buildSidebar(), BorderLayout.WEST);
		layout.addLayoutComponent(buildMenu(), BorderLayout.NORTH);

		setLayout(layout);
		setTitle("GUITestDriver - Bojiti");
		setBounds(new Rectangle(800,600));

		add.addActionListener(e->addButtonClick());
		toggle.addActionListener(e->toggleButtonClick());

		ProgressTableModel downloadingModel = new ProgressTableModel();
		controller.addDownloadListener(downloadingModel);
		JTable downloading = new JTable(downloadingModel);
		downloadingModel.setTableForProgressRepaint(downloading);
		downloading.getColumnModel().getColumn(3).setCellRenderer(new FancyCellRenderer());

		URITableModel parsingModel = new URITableModel();
		controller.addParseListener(parsingModel);
		JTable parsing = new JTable(parsingModel);

		mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		mainSplit.add(new JScrollPane(downloading), JSplitPane.TOP);
		mainSplit.add(new JScrollPane(parsing), JSplitPane.BOTTOM);
		add(mainSplit);
		layout.addLayoutComponent(mainSplit, BorderLayout.CENTER);
	}

	@Override
	public void setVisible(boolean b){
		super.setVisible(b);
		if(b) mainSplit.setDividerLocation(0.5);
	}

	private void addButtonClick(){
		add.setEnabled(false);
		SwingUtilities.invokeLater(()-> {
			String candidate = JOptionPane.showInputDialog(this, "Enter the URI to add:",
					"Add URI", JOptionPane.QUESTION_MESSAGE);
			try{
				URI uri = new URI(candidate);
				controller.add(uri);
			}catch(URISyntaxException e){
				JOptionPane.showMessageDialog(this, "URI invalid!", "Add URI Failed",
						JOptionPane.ERROR_MESSAGE);
			}catch(NullPointerException ignore){
			}finally{
				add.setEnabled(true);
			}
		});
	}

	private void toggleButtonClick(){
		toggle.setEnabled(false);
		SwingUtilities.invokeLater(()-> {
			if(controller.isStarted()){
				try{
					controller.stop();
					toggle.setText("Start");
				}catch(InterruptedException e){
					JOptionPane.showMessageDialog(this, "Shutdown taking longer than usual.",
							"Stop Crawler Failed", JOptionPane.ERROR_MESSAGE);
				}
			}else{
				controller.start();
				toggle.setText("Stop");
			}
			toggle.setEnabled(true);
		});
	}

	private JPanel buildSidebar(){
		JPanel sidebar = new JPanel();
		GridBagLayout sidebarLayout = new GridBagLayout();
		GridBagConstraints sidebarConstraints = new GridBagConstraints();
		sidebarConstraints.fill = GridBagConstraints.HORIZONTAL;
		sidebarConstraints.weightx = 1;
		sidebarConstraints.gridx = 0;

		sidebar.setLayout(sidebarLayout);
		sidebar.add(add, sidebarConstraints);
		sidebar.add(toggle, sidebarConstraints);
		add(sidebar);
		return sidebar;
	}

	private JMenuBar buildMenu(){
		JMenuBar menu = new JMenuBar();
		JMenu file = new JMenu("File");
		JMenuItem exit = new JMenuItem("Exit");
		file.add(exit);
		menu.add(file);
		add(menu);
		return menu;
	}

	public static void main(String[] args) throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException{
		// Set logging level
		for(Handler handler : Logger.getLogger("").getHandlers())
			handler.setLevel(Level.WARNING);

		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		//UIManager.setLookAndFeel(LookAndFeels.getOrSystem("Windows"));
		new GUITestDriver().setVisible(true);
	}

	private static String toNiceSize(long bytes){
		long kbytes = bytes / 1024;
		if(kbytes < 10) return bytes + " bytes";
		long mbytes = kbytes / 1024;
		if(mbytes < 10) return kbytes + " KB";
		long gbytes = mbytes / 1024;
		if(gbytes < 10) return mbytes + " MB";
		return gbytes + " GB";
	}

	private static String toNiceSpeed(long bps){
		long kbps = bps / 1024;
		if(kbps < 10) return bps + " bps";
		long mbps = kbps / 1024;
		if(mbps < 10) return kbps + " kbps";
		long gbps = mbps / 1024;
		if(gbps < 10) return mbps + " mbps";
		return gbps + " gbps";
	}
}
