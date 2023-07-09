/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui.dataedit;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.UUID;

import javax.swing.JDialog;
import javax.swing.JPanel;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeBoolean;
import de.nmichael.efa.core.items.ItemTypeDataRecordTable;
import de.nmichael.efa.core.items.ItemTypeStringAutoComplete;
import de.nmichael.efa.data.BoatRecord;
import de.nmichael.efa.data.BoatReservationRecord;
import de.nmichael.efa.data.BoatReservations;
import de.nmichael.efa.data.BoatStatusRecord;
import de.nmichael.efa.data.Boats;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.gui.SimpleInputDialog;
import de.nmichael.efa.gui.util.AutoCompleteList;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;


// @i18n complete
public class BoatReservationListDialog extends DataListDialog {

    boolean allowNewReservationsWeekly = true;
	protected ItemTypeBoolean showTodaysReservationsOnly;


    public BoatReservationListDialog(Frame parent, AdminRecord admin) {
        super(parent, International.getString("Bootsreservierungen"), Daten.project.getBoatReservations(false), 0, admin);
        iniValues(null, true, true, true);
    }

    public BoatReservationListDialog(JDialog parent, AdminRecord admin) {
        super(parent, International.getString("Bootsreservierungen"), Daten.project.getBoatReservations(false), 0, admin);
        iniValues(null, true, true, true);
    }

    public BoatReservationListDialog(Frame parent, UUID boatId, AdminRecord admin) {
        super(parent, International.getString("Bootsreservierungen"), Daten.project.getBoatReservations(false), 0, admin);
        iniValues(boatId, true, true, true);
    }

    public BoatReservationListDialog(JDialog parent, UUID boatId, AdminRecord admin) {
        super(parent, International.getString("Bootsreservierungen"), Daten.project.getBoatReservations(false), 0, admin);
        iniValues(boatId, true, true, true);
    }

    public BoatReservationListDialog(Frame parent, UUID boatId, boolean allowNewReservations, boolean allowNewReservationsWeekly, boolean allowEditDeleteReservations) {
        super(parent, International.getString("Bootsreservierungen"), Daten.project.getBoatReservations(false), 0, null);
        iniValues(boatId, allowNewReservations, allowNewReservationsWeekly, allowEditDeleteReservations);
    }

    public BoatReservationListDialog(JDialog parent, UUID boatId, boolean allowNewReservations, boolean allowNewReservationsWeekly, boolean allowEditDeleteReservations) {
        super(parent, International.getString("Bootsreservierungen"), Daten.project.getBoatReservations(false), 0, null);
        iniValues(boatId, allowNewReservations, allowNewReservationsWeekly, allowEditDeleteReservations);
    }

    private void iniValues(UUID boatId, boolean allowNewReservations, boolean allowNewReservationsWeekly, boolean allowEditDeleteReservations) {
        if (boatId != null) {
            this.filterFieldName  = BoatReservationRecord.BOATID;
            this.filterFieldValue = boatId.toString();
            if (Daten.project != null) {
                Boats boats = Daten.project.getBoats(false);
                if (boats != null) {
                    BoatRecord r = boats.getBoat(boatId, System.currentTimeMillis());
                    if (r != null) {
                        this.filterFieldDescription = International.getString("Boot") + ": " +
                                r.getQualifiedName();
                    }
                }
            }
        }
        if (allowNewReservations && allowEditDeleteReservations) {
            if (admin != null) {
                // default: ADD, EDIT, DELETE, IMPORT, EXPORT
            } else {
                actionText = new String[]{
                            ItemTypeDataRecordTable.ACTIONTEXT_NEW,
                            ItemTypeDataRecordTable.ACTIONTEXT_EDIT,
                            ItemTypeDataRecordTable.ACTIONTEXT_DELETE
                        };
                actionType = new int[]{
                            ItemTypeDataRecordTable.ACTION_NEW,
                            ItemTypeDataRecordTable.ACTION_EDIT,
                            ItemTypeDataRecordTable.ACTION_DELETE
                        };
            }
        } else if (allowNewReservations) {
            actionText = new String[] { ItemTypeDataRecordTable.ACTIONTEXT_NEW };
            actionType = new int[] { ItemTypeDataRecordTable.ACTION_NEW };
        } else if (allowEditDeleteReservations) {
            actionText = new String[] { ItemTypeDataRecordTable.ACTIONTEXT_EDIT, ItemTypeDataRecordTable.ACTIONTEXT_DELETE };
            actionType = new int[] { ItemTypeDataRecordTable.ACTION_EDIT, ItemTypeDataRecordTable.ACTION_DELETE };
        } else {
            actionText = new String[] { };
            actionType = new int[] { };
        }
        this.allowNewReservationsWeekly = allowNewReservationsWeekly;
        
		//From and to columns should be wider than default
		this.minColumnWidths = new int[] {150,120,120,120,12};   
    }


    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    public DataEditDialog createNewDataEditDialog(JDialog parent, StorageObject persistence, DataRecord record) {
        boolean newRecord = (record == null);
        if (record == null && persistence != null && filterFieldValue != null) {
            record = ((BoatReservations)persistence).createBoatReservationsRecord(UUID.fromString(filterFieldValue));
        }
        if (record == null) {
            long now = System.currentTimeMillis();
            ItemTypeStringAutoComplete boat = new ItemTypeStringAutoComplete("BOAT", "", IItemType.TYPE_PUBLIC,
                    "", International.getString("Boot"), true);
            boat.setAutoCompleteData(new AutoCompleteList(Daten.project.getBoats(false).data(), now, now));
            if (SimpleInputDialog.showInputDialog(this, International.getString("Boot auswählen"), boat)) {
                String s = boat.toString();
                try {
                    if (s != null && s.length() > 0) {
                        Boats boats = Daten.project.getBoats(false);
                        record = ((BoatReservations)persistence).createBoatReservationsRecord(boats.getBoat(s, now).getId());
                    }
                } catch(Exception e) {
                    Logger.logdebug(e);
                }
            }
        }
        if (record == null) {
            return null;
        }
        if (admin == null) {
            try {
                Boats boats = Daten.project.getBoats(false);
                BoatRecord b = boats.getBoat(((BoatReservationRecord)record).getBoatId(), System.currentTimeMillis());
                if (b.getOwner() != null && b.getOwner().length() > 0 &&
                    !Daten.efaConfig.getValueMembersMayReservePrivateBoats()) {
                    Dialog.error(International.getString("Privatboote dürfen nicht reserviert werden!"));
                    return null;
                }
                BoatStatusRecord bs = b.getBoatStatus();
                if (bs != null && BoatStatusRecord.STATUS_NOTAVAILABLE.equals(bs.getBaseStatus()) &&
                    Dialog.yesNoDialog(International.getString("Boot nicht verfügbar"), 
                            International.getMessage("Das ausgewählte Boot '{boat}' ist derzeit nicht verfügbar:", b.getQualifiedName()) + "\n" +
                            International.getString("Status") + ": " + bs.getComment() + "\n" +
                            International.getString("Möchtest Du das Boot trotzdem reservieren?")) != Dialog.YES) {
                    return null;
                }
            } catch(Exception e) {
                Logger.logdebug(e);
                return null;
            }
        }

        try {
            return new BoatReservationEditDialog(parent, (BoatReservationRecord) record,
                    newRecord, allowNewReservationsWeekly, admin);
        } catch (Exception e) {
            Dialog.error(e.getMessage());
            return null;
        }
    }
	protected void createSpecificItemTypeRecordTable() {
        table = new BoatReservationItemTypeDataRecordTable("TABLE",
                persistence.createNewRecord().getGuiTableHeader(),
                persistence, validAt, admin,
                filterFieldName, filterFieldValue, // defaults are null
                actionText, actionType, actionImage, // default actions: new, edit, delete
                this,
                IItemType.TYPE_PUBLIC, "BASE_CAT", getTitle());
	}
	
	protected void iniDialog() throws Exception {
		super.iniDialog();
		//show only matching items by default in BoatDamageListDialog 
		table.setIsFilterSet(true);
	}
    protected void iniControlPanel() {
    	// we want to put an additional element after the control panel
    	super.iniControlPanel();
    	this.iniBoatDamageListFilter();
    }
	
	private void iniBoatDamageListFilter() {
		JPanel myControlPanel= new JPanel();
    	
		showTodaysReservationsOnly = new ItemTypeBoolean("SHOW_TODAYS_RESERVATIONS_ONLY",
                false,
                IItemType.TYPE_PUBLIC, "", International.getString("nur heutige Reservierungen anzeigen"));
		showTodaysReservationsOnly.setPadding(0, 0, 0, 0);
		showTodaysReservationsOnly.displayOnGui(this, myControlPanel, 0, 0);
		showTodaysReservationsOnly.registerItemListener(this);
        mainPanel.add(myControlPanel, BorderLayout.NORTH);
	}
	
    public void itemListenerAction(IItemType itemType, AWTEvent event) {
    	
    	// handle our special filter for today's reservations, else use default item handler
    	if (itemType.equals(showTodaysReservationsOnly)) {
    		
    		 if (event.getID() == ActionEvent.ACTION_PERFORMED) {
    			 showTodaysReservationsOnly.getValueFromGui();
    			 if (showTodaysReservationsOnly.getValue()) {
    				 table.getSearchField().setValue(DataTypeDate.today().toString());
    				 table.getFilterBySearch().setValue(true);
    				 table.updateData();
    				 table.showValue();
    			 } else {
    				 table.getSearchField().setValue("");
    				 table.getFilterBySearch().setValue(false);
    				 table.updateData();
    				 table.showValue();
    			 }
    		 }
     		
     	} else {
     		super.itemListenerAction(itemType, event);
     	}
     }	    		
}
