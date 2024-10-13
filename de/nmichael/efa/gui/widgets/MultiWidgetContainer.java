package de.nmichael.efa.gui.widgets;

import javax.swing.JComponent;

import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.data.LogbookRecord;
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

public class MultiWidgetContainer extends Widget {

	public MultiWidgetContainer() {
	    super(International.getString("Multi-Widget"), "Multi-Widget", International.getString("Multi-Widget"), true,false);
	    
        addHint("MultiWidgetInfo1",IItemType.TYPE_PUBLIC, "", International.getString("Das Multi-Widget kann in einem Platzbereich mehrere Widgets anzeigen."), 3,6,6);
        addHint("MultiWidgetInfo2",IItemType.TYPE_PUBLIC, "", International.getString("Wählen Sie dazu jeweils in den anderen Widgets als Position \"MultiWidget\" aus."), 3,6,6);
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
		// TODO Auto-generated method stub

	}

	@Override
	public JComponent getComponent() {
		// TODO Auto-generated method stub
		return null;
	}

}
