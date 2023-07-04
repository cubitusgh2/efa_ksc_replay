/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core.items;

import java.util.*;
import de.nmichael.efa.*;
import de.nmichael.efa.core.config.*;
import de.nmichael.efa.util.*;
import de.nmichael.efa.gui.*;
import de.nmichael.efa.data.*;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataKeyIterator;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeIntString;
import de.nmichael.efa.data.types.DataTypeList;
import java.awt.Color;
import java.util.UUID;

public class ItemTypeBoatstatusList extends ItemTypeList {

    public static final int SEATS_OTHER = 99;
    public static final String TYPE_OTHER = "";
    public static final String RIGGER_OTHER = "";
    private static final String STR_DESTINATION_DELIMITER=     	"     -> ";
    EfaBoathouseFrame efaBoathouseFrame;
    private String STR_RESERVIERT_FUER=International.getString("Reserviert für").toLowerCase();
    private String STR_BOOTSSCHADEN=International.getString("Bootsschaden");    

    public ItemTypeBoatstatusList(String name,
            int type, String category, String description,
            EfaBoathouseFrame efaBoathouseFrame) {
        super(name, type, category, description);
        this.efaBoathouseFrame = efaBoathouseFrame;
    }

    public ItemTypeBoatstatusList(String name,
            int type, String category, String description,
            EfaBoathouseFrame efaBoathouseFrame, boolean showFilterField, boolean showPrettyList) {
        super(name, type, category, description, showFilterField, showPrettyList);
        this.efaBoathouseFrame = efaBoathouseFrame;
    }
    
    public void setBoatStatusData(Vector<BoatStatusRecord> v, Logbook logbook, String other) {
        Vector<ItemTypeListData> vdata = sortBootsList(v, logbook);
        if (other != null) {
            BoatListItem item = new BoatListItem();
            item.text = other;
            vdata.add(0, new ItemTypeListData(other, null, null, item, false, -1));//tooltip can be set to null as this function is only called but updateBoatLists for <anderes boot>
            this.other_item_text=other;
        }
        clearIncrementalSearch();
        list.setSelectedIndex(-1);
        setItems(vdata);
        showValue();
        list.repaint();
    }

    private Vector<ItemTypeListData> sortBootsList(Vector<BoatStatusRecord> v, Logbook logbook) {
    	try {
    	// return empty list if no data available.
    	if (v == null || v.size() == 0 || logbook == null) {
            return new Vector<ItemTypeListData>();
        }

        long now = System.currentTimeMillis();
        Boats boats = Daten.project.getBoats(false);

        Groups groups = Daten.project.getGroups(false);
        Boolean buildToolTips = Daten.efaConfig.getValueEfaBoathouseExtdToolTips();
        Boolean showDestination = Daten.efaConfig.getValueEfaDirekt_showZielnameFuerBooteUnterwegs();
        Boolean showReservation = Daten.efaConfig.getValueEfaBoathouseBoatListReservationInfo();
        
        Hashtable<UUID, Color> groupColors = new Hashtable<UUID, Color>();
        try {
            DataKeyIterator it = groups.data().getStaticIterator();
            for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                GroupRecord gr = (GroupRecord)groups.data().get(k);
                if (gr != null && gr.isValidAt(now)) {
                    String cs = gr.getColor();
                    if (cs != null && cs.length() > 0) {
                        groupColors.put(gr.getId(), EfaUtil.getColorOrGray(cs));
                    }
                }
            }
            this.iconWidth = (groupColors.size() > 0 ? Daten.efaConfig.getValueEfaDirekt_fontSize() : 0);
            this.iconHeight = this.iconWidth;
        } catch(Exception e) {
            Logger.logdebug(e);
        }

        Vector<BoatString> bsv = new Vector<BoatString>();
        for (int i = 0; i < v.size(); i++) {
            BoatStatusRecord curBoatStatusRecord = v.get(i);

            BoatRecord curBoatRecord = boats.getBoat(curBoatStatusRecord.getBoatId(), now);
            Hashtable<Integer,Integer> allSeats = new Hashtable<Integer,Integer>(); // seats -> variant
            // find all seat variants to be shown...
            if (curBoatRecord != null) {
                if (curBoatRecord.getNumberOfVariants() == 1) {
                    allSeats.put(curBoatRecord.getNumberOfSeats(0, SEATS_OTHER), curBoatRecord.getTypeVariant(0));
                } else {
                    if (curBoatStatusRecord.getCurrentStatus().equals(BoatStatusRecord.STATUS_AVAILABLE)) {
                        for (int j = 0; j < curBoatRecord.getNumberOfVariants(); j++) {
                            // if the boat is available, show the boat in all seat variants
                            allSeats.put(curBoatRecord.getNumberOfSeats(j, SEATS_OTHER), curBoatRecord.getTypeVariant(j));
                        }
                    } else {
                        if (curBoatStatusRecord.getCurrentStatus().equals(BoatStatusRecord.STATUS_ONTHEWATER)) {
                            // if the boat is on the water, show the boat in the variant that it is currently being used in
                            DataTypeIntString entry = curBoatStatusRecord.getEntryNo();
                            if (entry != null && entry.length() > 0) {
                                LogbookRecord lr = logbook.getLogbookRecord(curBoatStatusRecord.getEntryNo());
                                if (lr != null && lr.getBoatVariant() > 0 && lr.getBoatVariant() <= curBoatRecord.getNumberOfVariants()) {
                                    allSeats.put(curBoatRecord.getNumberOfSeats(curBoatRecord.getVariantIndex(lr.getBoatVariant()), SEATS_OTHER),
                                            lr.getBoatVariant());
                                }
                            }
                        }
                    }
                }
                if (allSeats.size() == 0) {
                    // just show the boat in any variant
                    int vd = curBoatRecord.getDefaultVariant();
                    if (vd < 1) {
                        vd = curBoatRecord.getTypeVariant(0);
                    }
                    allSeats.put(curBoatRecord.getNumberOfSeats(0, SEATS_OTHER), vd);
                }
            } else {
                if (curBoatStatusRecord.getUnknownBoat()) {
                    // unknown boat
                    allSeats.put(SEATS_OTHER, -1);
                } else {
                    // BoatRecord not found; may be a boat which has a status, but is invalid at timestamp "now"
                    // don't add seats for this boat; it should *not* appear in the list
                }
            }

            Integer[] seats = allSeats.keySet().toArray(new Integer[0]);
            for (int j=0; j<seats.length; j++) {
                int variant = allSeats.get(seats[j]);

                if (curBoatRecord != null && seats.length < curBoatRecord.getNumberOfVariants()) {
                    // we have multiple variants, but all with the same number of seats
                    if (curBoatRecord.getDefaultVariant() > 0) {
                        variant = curBoatRecord.getDefaultVariant();
                    }
                }

                BoatString curBoatString = new BoatString();

                // Seats
                int seat = seats[j];
                if (seat == 0) {
                    seat = SEATS_OTHER;
                }
                if (seat < 0) {
                    seat = 0;
                }
                if (seat > SEATS_OTHER) {
                    seat = SEATS_OTHER;
                }
                curBoatString.seats = seat;
                curBoatString.variant = variant;
                curBoatString.type = (curBoatRecord != null ? curBoatRecord.getTypeType(0) : EfaTypes.TYPE_BOAT_OTHER);
                curBoatString.rigger = (curBoatRecord != null ? curBoatRecord.getTypeRigging(0) : EfaTypes.TYPE_RIGGING_OTHER);

                // for BoatsOnTheWater, don't use the "real" boat name, but rather what's stored in the boat status as "BoatText"
                curBoatString.name = (curBoatStatusRecord.getCurrentStatus().equals(BoatStatusRecord.STATUS_ONTHEWATER) || curBoatRecord == null ? curBoatStatusRecord.getBoatText() : curBoatRecord.getQualifiedName());

                curBoatString.sortBySeats = (Daten.efaConfig.getValueEfaDirekt_sortByAnzahl());
                curBoatString.sortByRigger = (Daten.efaConfig.getValueEfaDirekt_sortByRigger());
                curBoatString.sortByType = (Daten.efaConfig.getValueEfaDirekt_sortByType());
                if (!curBoatString.sortBySeats) {
                    curBoatString.seats = SEATS_OTHER;
                }
                if (!curBoatString.sortByRigger) {
                    curBoatString.rigger = RIGGER_OTHER;
                }
                if (!curBoatString.sortByType) {
                    curBoatString.type = TYPE_OTHER;
                }

                // Colors for Groups
                ArrayList<Color> aColors = new ArrayList<Color>();
                if (curBoatRecord != null) {
                    DataTypeList<UUID> grps = curBoatRecord.getAllowedGroupIdList();
                    if (grps != null && grps.length() > 0) {
                        for (int g=0; g<grps.length(); g++) {
                            UUID id = grps.get(g);
                            Color c = groupColors.get(id);
                            if (c != null) {
                                aColors.add(c);
                            }
                        }
                    }
                }
                Color[] colors = (aColors.size() > 0 ? aColors.toArray(new Color[0]) : null);

                BoatListItem item = new BoatListItem();
                item.list = this;
                item.text = curBoatString.name;
                item.boat = curBoatRecord;//SGB
                item.boatStatus = curBoatStatusRecord;
                item.boatVariant = curBoatString.variant;
                curBoatString.colors = colors;
                curBoatString.record = item;

                if (Daten.efaConfig.getValueEfaDirekt_showZielnameFuerBooteUnterwegs() &&
                    BoatStatusRecord.STATUS_ONTHEWATER.equals(curBoatStatusRecord.getCurrentStatus()) &&
                    curBoatStatusRecord.getEntryNo() != null && curBoatStatusRecord.getEntryNo().length() > 0) {
                    LogbookRecord lr = logbook.getLogbookRecord(curBoatStatusRecord.getEntryNo());
                    if (lr != null) {
                        String dest = lr.getDestinationAndVariantName();
                        if (dest != null && dest.length() > 0) {
                            curBoatString.name += STR_DESTINATION_DELIMITER + dest;              	
                    
                        }
                    }
                }

                bsv.add(curBoatString);
                if (!curBoatString.sortBySeats) {
                    break;
                }
            }
        }

        BoatString[] boatStringArray = new BoatString[bsv.size()];
        for (int i=0; i<boatStringArray.length; i++) {
            boatStringArray[i] = bsv.get(i);
        }
        Arrays.sort(boatStringArray);

        Vector<ItemTypeListData> vv = new Vector<ItemTypeListData>();
        int anz = -1;
        String lastSep = null;
        for (int i = 0; i < boatStringArray.length; i++) {
            String s = null;

            // sort by seats?
            if (Daten.efaConfig.getValueEfaDirekt_sortByAnzahl()) {
                switch (boatStringArray[i].seats) {
                    case 1:
                        s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_1);
                        break;
                    case 2:
                        s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_2);
                        break;
                    case 3:
                        s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_3);
                        break;
                    case 4:
                        s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_4);
                        break;
                    case 5:
                        s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_5);
                        break;
                    case 6:
                        s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_6);
                        break;
                    case 8:
                        s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_8);
                        break;
                    default:
                        if (boatStringArray[i].seats < 99) {
                            s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, Integer.toString(boatStringArray[i].seats));
                        }
                }
            }

            // sort by rigger?
            if (Daten.efaConfig.getValueEfaDirekt_sortByRigger() && boatStringArray[i].rigger != null) {
                if (Daten.efaConfig.getValueEfaDirekt_sortByAnzahl()) {
                    if (EfaTypes.getSeatsKey(boatStringArray[i].seats, boatStringArray[i].rigger) != null) {
                        s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.getSeatsKey(boatStringArray[i].seats, boatStringArray[i].rigger));
                    }
                } else {
                    s = (s == null ? "" : s + " ") + Daten.efaTypes.getValue(EfaTypes.CATEGORY_RIGGING, boatStringArray[i].rigger);
                }
            }
            // sort by type?
            if (Daten.efaConfig.getValueEfaDirekt_sortByType() && boatStringArray[i].type != null) {
                s = (s == null ? "" : s + " ") + Daten.efaTypes.getValue(EfaTypes.CATEGORY_BOAT, boatStringArray[i].type);
            }

            if (s == null || s.equals(EfaTypes.getStringUnknown())) {
                /* @todo (P5) Doppeleinträge currently not supported in efa2
                 DatenFelder d = Daten.fahrtenbuch.getDaten().boote.getExactComplete(removeDoppeleintragFromBootsname(a[i].name));
                 if (d != null) {
                 s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, d.get(Boote.ANZAHL));
                 } else {
                 */
                s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_OTHER);
                if (Daten.efaConfig.getValueEfaDirekt_boatListIndividualOthers() && boatStringArray[i].type != null) {
                    s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_BOAT, boatStringArray[i].type);
                }

                //}
            }
            anz = boatStringArray[i].seats;
            if (Daten.efaConfig.getValueEfaDirekt_sortByAnzahl()
                    || Daten.efaConfig.getValueEfaDirekt_sortByType()) {
                String newSep = LIST_SECTION_STRING +" "+ s  + " " + LIST_SECTION_STRING;
                if (!newSep.equals(lastSep)) {
                    vv.add(new ItemTypeListData(newSep, null, null, null, true, anz));
                }
                lastSep = newSep;
            }
            BoatListItem bi=(BoatListItem) boatStringArray[i].record;
            vv.add(new ItemTypeListData(boatStringArray[i].name, 
            							(buildToolTips ? buildToolTipText(boatStringArray[i], showReservation) : ""), 
            							getSecondaryItem(bi.boatStatus, showDestination, showReservation), 
            							boatStringArray[i].record, false, -1, null, boatStringArray[i].colors));
        }
        return vv;
        } catch (Exception ee) {
        	Logger.logdebug(ee);
    		return null;
    	}
    	
    }
    
    // is the current comment beginning with "Bootsschaden" in the corresponding locale? 
    private boolean isCommentBoatDamage(String s) {
        return (s != null && s.startsWith(STR_BOOTSSCHADEN + ": "));
    }

    // is the current comment beginning with "Reserviert für" in the corresponding locale? 
    private boolean isCommentBoardReservation(String s) {
    	return (s != null && s.toLowerCase().startsWith(STR_RESERVIERT_FUER));	
    }
        
    
    private String getSecondaryItem(BoatStatusRecord bs, Boolean showDestination, Boolean showReservationInfo) {
    	if (bs.getShowInList().equals(BoatStatusRecord.STATUS_ONTHEWATER) && showDestination) {
    		//Boat is on the water: we show the destination as secondary item
    		String value=bs.getDestination();
    		if (value!=null && !value.isEmpty()) {
    			return value; // 
    		} else { 
    			return "";
    		}
    	} else if (bs.getShowInList().equals(BoatStatusRecord.STATUS_AVAILABLE)) {
        	if (showReservationInfo) { 
	    		//available list: show next reservation within 8 hours as secondary item
	    		return getBoatReservation(bs.getBoatId(), 480, false);
        	} else { return ""; }
        	
    	} else if (bs.getShowInList().equals(BoatStatusRecord.STATUS_NOTAVAILABLE)) {
    		//not available list: show "Bootsschaden" for defect boats,
    		//or the end of the current reservation
    		if (isCommentBoatDamage(bs.getComment())) {
    			return International.getString("Bootsschaden");
    			
    		} else if (isCommentBoardReservation(bs.getComment())) {
    			if (showReservationInfo) { 
    				return getBoatReservation(bs.getBoatId(), 0, false);
    			} else {
    				return "";
    			}
    		} else {
    			
    			if (showDestination) { 
	    			//Boat is not available, but neither damage nor reservation.
	    			//so maybe it's a boat on a multi-day tour, regatta or whatsoever.
	    			//if the current BoatStatus has a destination set, show the destination.
	        		String value=bs.getDestination();
	        		if (value!=null && !value.isEmpty()) {
	        			return value; // 
	        		} else { 
	        			return "";
	        		}
    			} else { 
    				return "";
    			}
    		}

    	}
    	return "";
    }
    
    private String getBoatReservation(UUID boatID, long lookAheadMinutes, Boolean buildForTooltip) {
        BoatReservations boatReservationDB = (Daten.project != null ? Daten.project.getBoatReservations(false) : null);
        //aktuelle Reservierung holen
        
        Long now = System.currentTimeMillis();
        DataTypeDate today = new DataTypeDate(now);
        
        BoatReservationRecord[] reservations = boatReservationDB.getBoatReservations(boatID, now, lookAheadMinutes);
        if (reservations == null || reservations.length == 0) {
        	return "";
        } else {
        	BoatReservationRecord res = reservations[0];
      	String prefix = "";
        	
        	if(buildForTooltip) {
	        	if (lookAheadMinutes>0) {
	        		prefix=International.getString("Reserviert von") + " ";
	            	return prefix + res.getPersonAsName() + " " + International.getMessage("ab {timestamp}", res.getDateTimeFromDescription()) + " ("+ res.getReason()+ ")";
	        	} else {
	        		prefix = International.getString("Reserviert von") + " ";
	            	return prefix + res.getPersonAsName() + " " + International.getMessage("bis {timestamp}", res.getDateTimeToDescription())+ " ("+ res.getReason()+ ")";
	        	}
        	} else { // build for secondary Item in List
        		//search for Reservations in the future
        		if (res.getType().equals(BoatReservationRecord.TYPE_ONETIME)) {
        		
	        		//if (lookAheadMinutes<=0) {//aktuell laufende Reservierungen?
        			if (res.getReservationValidInMinutes()<=0) {
	        			if ((res.getDateTo().compareTo(today)==0) && (res.getTimeTo() != null)) {
	        				//Reservierung endet heute? dann nur noch Uhrzeit anzeigen
	        				return International.getMessage("Reserviert(r)_bis_{timestamp}", res.getTimeTo().toString(false)).trim();
	        			} else {
	        				//sonst das vollständige Datum.
	        				return International.getMessage("Reserviert(r)_bis_{timestamp}", res.getDateTimeToDescription()).trim();
	        			}
	        		} else {
	            		return International.getMessage("Reserviert(r)_ab_{timestamp}", res.getTimeFrom().toString(false)).trim();
	        		}
	        	} else if ((res.getType().equals(BoatReservationRecord.TYPE_WEEKLY)) || (res.getType().equals(BoatReservationRecord.TYPE_WEEKLY_LIMITED))){
	        		if (res.getReservationValidInMinutes()<=0) {//aktuell laufende Reservierungen? //weekly ist immer am aktuellen Tag..
	        			return International.getMessage("Reserviert(r)_bis_{timestamp}", res.getTimeTo().toString(false)).trim();
	        		} else {
	            		return International.getMessage("Reserviert(r)_ab_{timestamp}", res.getTimeFrom().toString(false)).trim();	
	        		}
	        	} 
	        }
        	return "";
        }
        
    }    
    
    /*
     * Creates a tooltip for either
     * - boatlist
     * - boats on water list
     * 
     * Where are the attributes obtained from?
     * 
     * Boat list
     * ---------
     * Boat name    - BoatString->BoatStatus->BoatName  or BoatString.name if boat is not in the boat list
     * Boat variant - BoatString->Boat->getDetailedBoatType
     * Boat groups  - BoatString->Boat->getAllowedGroupsAsNameString
     * 
     * 
     * Boats on water list/Trip
     * ---------
     * Boat name    - If boat on the water, split BoatString->Name into Boat name and destination using the unique delimiter
     * Destination  - If boat on the water, split BoatString->Name into Boat name and destination using the unique delimiter
     * 				  (works flawlessly)
     * lastmodified - BoatString->BoatStatus->getLastModified
     * Start time,
     * Cox & Crew	- BoatString->BoatStatus->Comment
     * 					- remove from comment: boat name, boat destination,  boat status text.
     * 					- replace all ; by , with a line break, so that the crew/cox are shown one per line
     * 				  works flawlessly with any international representation of Boatstatus.comment (built by BoatStatusRecord.createStatusString).
     * 				  works even after changing EFA language from de to en to any other language and starting new trips in each language.
     * 
     * The creation of tooltip text is somewhat prone to nullpointer exceptions, as it is not guaranteed that all of the attributes
     * are set with a content. To avoid those nullpointers to pop up and create an empty boat on water list, a general try-catch statement
     * gets these exceptions, logs them as warning and returns an empty tooltip string.
     * 
     */
    private String buildToolTipText(BoatString bs, Boolean showReservation) {

    	try {

    		if (bs!=null) {

    		BoatListItem bli = (BoatListItem) bs.record;
            
    		String boatName=bli.boatStatus.getBoatNameAsString(System.currentTimeMillis());
    		if (boatName==null) {
    			//determining boatname via BoatStatus can be empty if we have a manually entered boat name
    			boatName=bs.name;
    		}
    		String boatDestination="";
    		String boatVariant="";
    		String boatStatus=bli.boatStatus.getCurrentStatus();
    		String boatRuderErlaubnis="";
    		String boatReservation="";

    		//data if boat is on the water
    		String boatTimeEntry=EfaUtil.getTimeStamp(bli.boatStatus.getLastModified());
    		String boatComment=bli.boatStatus.getComment();
    		if (boatComment==null) {boatComment="";}
    		
    		// nur wenn das boot auf dem Wasser ist, interessieren Reservierungen.
    		// und wenn der Anwender wünscht, dass Reservierungsinfos auch angezeigt werden sollen.
    		// diese Einstellung betrifft auch den Tooltipp.
			if (bli.boat!=null && (! boatStatus.equals(BoatStatusRecord.STATUS_ONTHEWATER)) && showReservation )  {
				boatReservation= getBoatReservation(bli.boat.getId(), 480,true);
			} else {
				boatReservation="";
			}
    		
    		if (boatStatus.equals(BoatStatusRecord.STATUS_ONTHEWATER)) {
    			/*   	    			String[] itemParts= bs.name.split(STR_DESTINATION_DELIMITER);
	        		String firstPart="";
	        		String secondPart="";
	        		if (itemParts.length>1) {
	        			for (int i=0; i<itemParts.length-1; i++) {
	        				firstPart=firstPart.concat(itemParts[i]);
	        			}
	        			firstPart=firstPart.trim();
	        			secondPart=itemParts[itemParts.length-1].trim();
	        			
	        		} else {
	        			//itemparts=0 oder 1
	        			firstPart=boatName;
	        			secondPart="";
	        		}*/
	        		if(bli.boat != null) {
   	    			boatName=bli.boat.getName();
   	        		boatDestination=bli.boatStatus.getDestination();
	        		} else {
	        			boatName=bs.name;
	        			boatDestination="";
	        		}
    		} else if (boatStatus.equals(BoatStatusRecord.STATUS_AVAILABLE)) {
    			boatTimeEntry="";
    			if (bli.boat!=null) {
	    			boatVariant=bli.boat.getDetailedBoatType(bli.boat.getVariantIndex(bli.boatVariant));
	    			
	    			String groups = bli.boat.getAllowedGroupsAsNameString(System.currentTimeMillis());
	                if (groups.length() > 0) {
	                	boatRuderErlaubnis = (boatRuderErlaubnis.length() > 0 ? boatRuderErlaubnis + ", "
	                            : "; " + International.getMessage("nur für {something}", groups));
	                }
    			}
    		}
    		//Build tooltip
    		StringBuilder sb = new StringBuilder();
    		sb.append("<html><body><table border=\"0\"><tr><td align=\"left\"><b>");
    		sb.append(EfaUtil.escapeHtml(boatName));
    		sb.append("</b></td><td align=\"right\">");
    		sb.append(EfaUtil.escapeHtml(boatTimeEntry));
    		sb.append("</td></tr><tr><td colspan=2><hr></td></tr>");
    		if (!boatReservation.isEmpty()) {
    			sb.append("<tr><td align=\"left\" colspan=2>");
    			sb.append(EfaUtil.escapeHtml(boatReservation));
    			sb.append("</td></tr>");
    		}
    		if (!boatVariant.isEmpty()) {
    			sb.append("<tr><td align=\"left\" colspan=2>");
    			sb.append(EfaUtil.escapeHtml(boatVariant));
    			sb.append("</td></tr>");
    		}
    		if (!boatRuderErlaubnis.isEmpty()) {
    			sb.append("<tr><td align=\"left\" colspan=2>");
    			sb.append(EfaUtil.escapeHtml(boatRuderErlaubnis));
    			sb.append("</td></tr>");
    		}
    		
    		if (!boatDestination.isEmpty()) {
    			//den Text vor der destination entfernen
    			if (!boatDestination.isEmpty() && !boatComment.isEmpty()) {
    				int iPos=boatComment.indexOf(boatDestination);
    				if (iPos>0) {
    					boatComment=boatComment.substring(iPos);
    				}
    				try {
	    	   	    	String boatStatusText=bli.boatStatus.getStatusDescription(boatStatus);   	    					
    					boatComment=boatComment.replace(boatName, "").replace(boatStatusText,"").replace(boatDestination, "").replaceAll(";", ";<br>");
    				} catch (Exception e){
    					Logger.log(e);
    				}
    				
    			}
	    			sb.append("<tr><td colspan=2>");
	    			sb.append(EfaUtil.escapeHtml(boatDestination));
	    			sb.append("</td></tr><tr><td align=\"left\" colspan=2>");
	    			sb.append(EfaUtil.escapeHtml(boatComment));
	    			sb.append("</td></tr>");
    		} else {
	    		if (boatComment!=null) {
   	    			sb.append("<tr><td align=\"left\" colspan=2>");
   	    			sb.append(EfaUtil.escapeHtml(boatComment));
   	    			sb.append("</td></tr>");
	    		}
    		}
    	
    		sb.append("</table></body></html>");
    		
    		return sb.toString();
    		
    	} else {//BoatString is null
    		return null;
    	}
		} catch (Exception pe) {
   			//just in case some item of the BoatString could not be resolved as they may be 
   			//unexpectedly null
            Logger.log(Logger.WARNING, Logger.MSG_ERROR_EXCEPTION, pe.getMessage()+ " "+ (pe.getCause()));
            return null;
		}        	
    
    }
    
    public void setPersonStatusData(Vector<PersonRecord> v, String other) {
        Vector<ItemTypeListData> vdata = sortMemberList(v);
        if (other != null) {
            vdata.add(0, new ItemTypeListData(other, other, null, null, false, -1));
            this.other_item_text=other;
        }
        clearIncrementalSearch();
        list.setSelectedIndex(-1);
        setItems(vdata);
        showValue();
    }

    Vector sortMemberList(Vector<PersonRecord> v) {
        if (v == null || v.size() == 0) {
            return v;
        }
        BoatString[] a = new BoatString[v.size()];
        Boolean buildToolTips = Daten.efaConfig.getValueEfaBoathouseExtdToolTips();
        
        for (int i = 0; i < v.size(); i++) {
            PersonRecord pr = v.get(i);
            a[i] = new BoatString();
            a[i].seats = SEATS_OTHER;
            a[i].name = pr.getQualifiedName();
            a[i].sortBySeats = false;
            BoatListItem item = new BoatListItem();
            item.list = this;
            item.text = a[i].name;
            item.person = pr;
            a[i].record = item;
        }
        Arrays.sort(a);

        Vector<ItemTypeListData> vv = new Vector<ItemTypeListData>();
        char lastChar = ' ';
        for (int i = 0; i < a.length; i++) {
            String name = a[i].name;
            if (name.length() > 0) {
                if (name.toUpperCase().charAt(0) != lastChar) {
                    lastChar = name.toUpperCase().charAt(0);
                    vv.add(new ItemTypeListData("---------- " + lastChar + " ----------", null, null, null, true, SEATS_OTHER));
                }
                //Build tooltips only if wanted
                vv.add(new ItemTypeListData(name, 
                						    (buildToolTips ? getPersonToolTip(name, (BoatString) a[i]) : ""), 
                						    null, a[i].record,  false, SEATS_OTHER));
            }
        }
        return vv;
    }

    private String getPersonToolTip(String name, BoatString bs) {
    	
    	if (bs == null) {
    		return name;
    	} else {
    		BoatListItem bli = (BoatListItem) bs.record; 
    		
    		if (bli ==null) {
    			return name;
    		} else {
        		String alias = bli.person.getInputShortcut();
        		if ((alias != null) && (!alias.isEmpty())) {
        			return name + " ("+alias+")";
        		}
    		}
    	}
    	return name;
    }
    
    public BoatListItem getSelectedBoatListItem() {
        if (list == null || list.isSelectionEmpty()) {
            return null;
        } else {
            Object o = getSelectedValue();
            if (o != null) {
                return (BoatListItem)o;
            }
            return null;
        }
    }
    
    public static BoatListItem createBoatListItem(int mode) {
        BoatListItem b = new BoatListItem();
        b.mode = mode;
        return b;
    }

    public static class BoatListItem {
        public int mode;
        public ItemTypeBoatstatusList list;
        public String text;
        public BoatRecord boat;
        public BoatStatusRecord boatStatus;
        public int boatVariant = 0;
        public PersonRecord person;
    }

    class BoatString implements Comparable {

        public String name;
        public int seats;
        public boolean sortBySeats;
        public boolean sortByRigger;
        public boolean sortByType;
        public Object record;
        public int variant;
        public Color[] colors;
        public String type;
        public String rigger;

        private String normalizeString(String s) {
            if (s == null) {
                return "";
            }
            s = s.toLowerCase();
            if (s.indexOf("ä") >= 0) {
                s = EfaUtil.replace(s, "ä", "a", true);
            }
            if (s.indexOf("Ä") >= 0) {
                s = EfaUtil.replace(s, "Ä", "a", true);
            }
            if (s.indexOf("à") >= 0) {
                s = EfaUtil.replace(s, "à", "a", true);
            }
            if (s.indexOf("á") >= 0) {
                s = EfaUtil.replace(s, "á", "a", true);
            }
            if (s.indexOf("â") >= 0) {
                s = EfaUtil.replace(s, "â", "a", true);
            }
            if (s.indexOf("ã") >= 0) {
                s = EfaUtil.replace(s, "ã", "a", true);
            }
            if (s.indexOf("æ") >= 0) {
                s = EfaUtil.replace(s, "æ", "ae", true);
            }
            if (s.indexOf("ç") >= 0) {
                s = EfaUtil.replace(s, "ç", "c", true);
            }
            if (s.indexOf("è") >= 0) {
                s = EfaUtil.replace(s, "è", "e", true);
            }
            if (s.indexOf("é") >= 0) {
                s = EfaUtil.replace(s, "é", "e", true);
            }
            if (s.indexOf("è") >= 0) {
                s = EfaUtil.replace(s, "è", "e", true);
            }
            if (s.indexOf("é") >= 0) {
                s = EfaUtil.replace(s, "é", "e", true);
            }
            if (s.indexOf("ê") >= 0) {
                s = EfaUtil.replace(s, "ê", "e", true);
            }
            if (s.indexOf("ì") >= 0) {
                s = EfaUtil.replace(s, "ì", "i", true);
            }
            if (s.indexOf("í") >= 0) {
                s = EfaUtil.replace(s, "í", "i", true);
            }
            if (s.indexOf("î") >= 0) {
                s = EfaUtil.replace(s, "î", "i", true);
            }
            if (s.indexOf("ñ") >= 0) {
                s = EfaUtil.replace(s, "ñ", "n", true);
            }
            if (s.indexOf("ö") >= 0) {
                s = EfaUtil.replace(s, "ö", "o", true);
            }
            if (s.indexOf("Ö") >= 0) {
                s = EfaUtil.replace(s, "Ö", "o", true);
            }
            if (s.indexOf("ò") >= 0) {
                s = EfaUtil.replace(s, "ò", "o", true);
            }
            if (s.indexOf("ó") >= 0) {
                s = EfaUtil.replace(s, "ó", "o", true);
            }
            if (s.indexOf("ô") >= 0) {
                s = EfaUtil.replace(s, "ô", "o", true);
            }
            if (s.indexOf("õ") >= 0) {
                s = EfaUtil.replace(s, "õ", "o", true);
            }
            if (s.indexOf("ø") >= 0) {
                s = EfaUtil.replace(s, "ø", "o", true);
            }
            if (s.indexOf("ü") >= 0) {
                s = EfaUtil.replace(s, "ü", "u", true);
            }
            if (s.indexOf("Ü") >= 0) {
                s = EfaUtil.replace(s, "Ü", "u", true);
            }
            if (s.indexOf("ù") >= 0) {
                s = EfaUtil.replace(s, "ù", "u", true);
            }
            if (s.indexOf("ú") >= 0) {
                s = EfaUtil.replace(s, "ú", "u", true);
            }
            if (s.indexOf("û") >= 0) {
                s = EfaUtil.replace(s, "û", "u", true);
            }
            if (s.indexOf("ß") >= 0) {
                s = EfaUtil.replace(s, "ß", "ss", true);
            }
            return s;
        }

        public int compareTo(Object o) {
            BoatString other = (BoatString) o;
            String sThis = (sortBySeats ? (seats < 10 ? "0" : "") + seats : "") + 
                    (sortByRigger ? rigger : "") + 
                    (seats == SEATS_OTHER || sortByType ? type + "#" : "") +
                    normalizeString(name);
            String sOther = (sortBySeats ? (other.seats < 10 ? "0" : "") + other.seats : "") + 
                    (sortByRigger ? other.rigger : "") + 
                    (other.seats == SEATS_OTHER || sortByType ? other.type + "#" : "") +
                    normalizeString(other.name);
            return sThis.compareTo(sOther);
        }
    }


}
