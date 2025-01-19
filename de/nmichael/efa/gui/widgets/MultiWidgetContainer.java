package de.nmichael.efa.gui.widgets;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeInteger;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.gui.ImagesAndIcons;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
/*
 *
 * This is a Widget that works as a container for other widgets.
 * It can only be placed in the middle of efaBths.
 * Functionality:
 * - gets Widgets which are set to be shown in "MultiWidget container"
 * - keeps them in a card layout (so that only one single widget is shown at a time)
 * - scrolls through the widgets every X seconds
 * - enables the user to select a specific widget by klicking left/right buttons
 * - all Widgets get the same height.
 * 
 * The basic idea behind this widget is that there are usually more widgets
 * than there is place for in efaBths. For instance, current weather, weather forecast for today
 * and one /multiple water levels and maybe the are too much space consuming to be displayed sumulaneously.
 * 
 * So the widgets are shown in the same place, but the user or the system may scroll through them.
 * 
 */
import de.nmichael.efa.util.Logger;

public class MultiWidgetContainer extends Widget {

	static final String PARAM_AUTOCHANGE = "AutomaticChangeAfterSeconds";
	
	private JPanel mainPanel;
	private JPanel cardPanel = new JPanel(new CardLayout());
	private MultiWidgetPanelUpdater panelUpdater=null;

	public MultiWidgetContainer() {
	    super(International.getString("Multi-Widget"), "Multi-Widget", International.getString("Multi-Widget"), true,false);
	    
        addHint("MultiWidgetInfo1",IItemType.TYPE_PUBLIC, "", International.getString("Das Multi-Widget kann in einem Platzbereich mehrere Widgets anzeigen."), 3,6,6);
        addHint("MultiWidgetInfo2",IItemType.TYPE_PUBLIC, "", International.getString("Wählen Sie dazu jeweils in den anderen Widgets als Position \"MultiWidget\" aus."), 3,6,6);
        
        addParameterInternal(new ItemTypeInteger(PARAM_AUTOCHANGE, 15, 0, 90, false,
                IItemType.TYPE_PUBLIC, "",
                PARAM_AUTOCHANGE+" (s)"));
        
        super.setEnabled(true);
        super.setPosition(IWidget.POSITION_CENTER);
	}
	
	@Override
	public void runWidgetWarnings(int mode, boolean actionBegin, LogbookRecord r) {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void construct() {
		 mainPanel = new JPanel();
		 CardLayout cardLayout = new CardLayout();
		 cardPanel = new JPanel(cardLayout);
		 
		 //mainpanel: 
		 //  left:   button, vertically centered
		 //  middle: cards
		 //  right:  button, vertically centered
		 
		 mainPanel.setLayout(new GridBagLayout());
		 JButton leftButton=new JButton();
		 JButton rightButton = new JButton();
		 leftButton.setIcon(ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_WIDGET_ARROW_LEFT));
		 rightButton.setIcon(ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_WIDGET_ARROW_RIGHT));		 		 
		 leftButton.setMinimumSize(new Dimension(30,60));
		 leftButton.setPreferredSize(leftButton.getMinimumSize());
		 rightButton.setMinimumSize(leftButton.getMinimumSize());
		 rightButton.setPreferredSize(rightButton.getMinimumSize());		 
		 mainPanel.add(leftButton,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2,2,2,2), 0, 0)); 
		 mainPanel.add(cardPanel,   new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2,2,2,2), 0, 0));
		 mainPanel.add(rightButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2,2,2,2), 0, 0)); 
		
		 //Clicking on left/right buttons selects a new card and restarts the auto-update timer
		 leftButton.addActionListener(e -> {cardLayout.previous(cardPanel); panelUpdater.interrupt();});
		 rightButton.addActionListener(e -> {cardLayout.next(cardPanel); panelUpdater.interrupt(); });
		 mainPanel.addMouseListener(new MouseAdapter() {
			    //entering the control with the mouse shall restart the auto-update-timer, so the user has time to run some action...
			 	@Override
			    public void mouseEntered(MouseEvent e) {
			    	panelUpdater.interrupt();
			    }
		 });
		 
		 leftButton.addMouseListener(new MouseAdapter() {
			    //entering the control with the mouse shall restart the auto-update-timer, so the user has time to run some action...
			 	@Override
			    public void mouseEntered(MouseEvent e) {
			    	panelUpdater.interrupt();
			    }
		 });

		 rightButton.addMouseListener(new MouseAdapter() {
			    //entering the control with the mouse shall restart the auto-update-timer, so the user has time to run some action...
			 	@Override
			    public void mouseEntered(MouseEvent e) {
			    	panelUpdater.interrupt();
			    }	 
		 });

		 
    	try {
            panelUpdater = new MultiWidgetPanelUpdater(cardLayout, cardPanel, this.getUpdateInterval());
            panelUpdater.start();
            
        } catch(Exception e) {
            Logger.log(e);
        }

	}

	@Override
	public JComponent getComponent() {
		return mainPanel;
	}

	public void addWidget(JPanel panel) {
		cardPanel.add(panel);
	}
	
	public void clearWidgets() {
		cardPanel.removeAll();
	}

    public int getUpdateInterval() {
        return ((ItemTypeInteger)getParameterInternal(PARAM_AUTOCHANGE)).getValue();
    }
	
	
    class MultiWidgetPanelUpdater extends Thread {

        private volatile boolean keepRunning = true;
        private volatile int updateIntervalInSeconds = 15;

        private boolean sunriseError = false;
        private CardLayout cardLayout;
        private JPanel cardPanel;

        public MultiWidgetPanelUpdater(CardLayout cardLayout, JPanel cardPanel, int updateInterval) {
        	this.cardLayout = cardLayout;
        	this.cardPanel = cardPanel;
        	this.updateIntervalInSeconds = updateInterval*1000;
        }

        public void run() {
        	this.setName("MultiWidgetContainer.MultiWidgetPanelUpdater");

            while (keepRunning) {
            	
            	try {
            		Thread.sleep(updateIntervalInSeconds);
	            		            	
	            	//Use invokelater as swing threadsafe ways
            		SwingUtilities.invokeLater(
            			    new Runnable(){
            			        public void run(){
            			            cardLayout.next(cardPanel);
            			        }
            			    });

            	} catch (InterruptedException e) {
                	//This is when the thread gets interrupted when it is sleeping.
                	EfaUtil.foo();            
                } catch (Exception e) {
                	Throwable t = e.getCause();
                	if (t.getClass().getName().equalsIgnoreCase("java.lang.InterruptedException")) {
                		EfaUtil.foo();
                	} else {
                		Logger.logdebug(e);
                	}
                }
	                
            }
        }
        
        public synchronized void stopRunning() {
            keepRunning = false;
            interrupt(); // wake up thread
        }

    }
}
