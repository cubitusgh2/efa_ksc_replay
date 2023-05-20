/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data;

import de.nmichael.efa.util.*;
import de.nmichael.efa.Daten;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.ex.EfaModifyException;
import java.util.*;

// @i18n complete

public class BoatReservations extends StorageObject {

    public static final String DATATYPE = "efa2boatreservations";

    public BoatReservations(int storageType, 
            String storageLocation,
            String storageUsername,
            String storagePassword,
            String storageObjectName) {
        super(storageType, storageLocation, storageUsername, storagePassword, storageObjectName, DATATYPE, International.getString("Bootsreservierungen"));
        BoatReservationRecord.initialize();
        dataAccess.setMetaData(MetaData.getMetaData(DATATYPE));
    }

    public DataRecord createNewRecord() {
        return new BoatReservationRecord(this, MetaData.getMetaData(DATATYPE));
    }

    public BoatReservationRecord createBoatReservationsRecord(UUID id) {
        AutoIncrement autoIncrement = getProject().getAutoIncrement(false);

        int tries = 0;
        int val = 0;
        try {
            while (tries++ < 100) {
                // usually autoincrement should always give a unique new id.
                // but in case our id's got out of sync, we try up to 100 times to fine a
                // new unique reservation id.
                val = autoIncrement.nextAutoIncrementIntValue(data().getStorageObjectType());
                if (val <= 0) {
                    break;
                }
                if (data().get(BoatReservationRecord.getKey(id, val)) == null) {
                    break;
                }
            }
        } catch (Exception e) {
            Logger.logdebug(e);
        }
        if (val > 0) {
            return createBoatReservationsRecord(id, val);
        }
        return null;
    }

    public BoatReservationRecord createBoatReservationsRecord(UUID id, int reservation) {
        BoatReservationRecord r = new BoatReservationRecord(this, MetaData.getMetaData(DATATYPE));
        r.setBoatId(id);
        r.setReservation(reservation);
        return r;
    }

    public BoatReservationRecord[] getBoatReservations(UUID boatId) {
        try {
            DataKey[] keys = data().getByFields(BoatReservationRecord.IDX_BOATID, new Object[] { boatId });
            if (keys == null || keys.length == 0) {
                return null;
            }
            BoatReservationRecord[] recs = new BoatReservationRecord[keys.length];
            for (int i=0; i<keys.length; i++) {
                recs[i] = (BoatReservationRecord)data().get(keys[i]);
            }
            return recs;
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    public BoatReservationRecord[] getBoatReservations(UUID boatId, long now, long lookAheadMinutes) {
        BoatReservationRecord[] reservations = getBoatReservations(boatId);

        Vector<BoatReservationRecord> activeReservations = new Vector<BoatReservationRecord>();
        for (int i = 0; reservations != null && i < reservations.length; i++) {
            BoatReservationRecord r = reservations[i];
            if (r.getReservationValidInMinutes(now, lookAheadMinutes) >= 0) {
                activeReservations.add(r);
            }
        }

        if (activeReservations.size() == 0) {
            return null;
        }
        BoatReservationRecord[] a = new BoatReservationRecord[activeReservations.size()];
        for (int i=0; i<a.length; i++) {
            a[i] = activeReservations.get(i);
        }
        return a;
    }

    public int purgeObsoleteReservations(UUID boatId, long now) {
        BoatReservationRecord[] reservations = getBoatReservations(boatId);
        int purged = 0;

        for (int i = 0; reservations != null && i < reservations.length; i++) {
            BoatReservationRecord r = reservations[i];
            if (r.isObsolete(now)) {
                try {
                    data().delete(r.getKey());
                    purged++;
                } catch(Exception e) {
                    Logger.log(e);
                }
            }
        }
        return purged;
    }

    
    private String buildOverlappingReservationInfo(BoatReservationRecord reservation) {
        String result = "";

        if (reservation.getType().equals(BoatReservationRecord.TYPE_WEEKLY)) {
            result = "\n\n" + reservation.getBoatName() + " / " + reservation.getPersonAsName() + " (" + Daten.efaTypes.getValueWeekday(reservation.getDayOfWeek()) + " " + reservation.getTimeFrom() + " -- " + reservation.getTimeTo() + ")"
                    + "\n" + International.getString("Reservierungsgrund") + ": " + reservation.getReason()
                    + "\n" + International.getString("Telefon für Rückfragen") + ": " + reservation.getContact();
        } else if (reservation.getType().equals(BoatReservationRecord.TYPE_ONETIME)) {
            result = "\n\n" + reservation.getBoatName() + " / " + reservation.getPersonAsName() + " (" + reservation.getDateFrom().getWeekdayAsString() + " " + reservation.getDateFrom() + " " + reservation.getTimeFrom() + " -- " + reservation.getDateTo().getWeekdayAsString() + " " + reservation.getDateTo() + " " + reservation.getTimeTo() + ")"
                    + "\n" + International.getString("Reservierungsgrund") + ": " + reservation.getReason()
                    + "\n" + International.getString("Telefon für Rückfragen") + ": " + reservation.getContact();
        }

        return result;
    }

    public void preModifyRecordCallback(DataRecord record, boolean add, boolean update, boolean delete) throws EfaModifyException {
        if (add || update) {
            assertFieldNotEmpty(record, BoatReservationRecord.BOATID);
            assertFieldNotEmpty(record, BoatReservationRecord.RESERVATION);
            assertFieldNotEmpty(record, BoatReservationRecord.TYPE);

            BoatReservationRecord r = ((BoatReservationRecord)record);
            BoatReservationRecord[] br = this.getBoatReservations(r.getBoatId());
            for (int i=0; br != null && i<br.length; i++) {
                if (br[i].getReservation() == r.getReservation()) {
                    continue;
                }
                if (!r.getType().equals(br[i].getType())) {
                    checkMixedTypeReservations(r, br[i]); //throws an EfaModifyException, if overlapping
                    continue; // if no exception is thrown, we're done here and proceed to the next item on the list.
                }
                if (r.getType().equals(BoatReservationRecord.TYPE_WEEKLY)) {
                    assertFieldNotEmpty(record, BoatReservationRecord.DAYOFWEEK);
                    assertFieldNotEmpty(record, BoatReservationRecord.TIMEFROM);
                    assertFieldNotEmpty(record, BoatReservationRecord.TIMETO);
                    if (!r.getDayOfWeek().equals(br[i].getDayOfWeek())) {
                        continue;
                    }
                    if (DataTypeTime.isRangeOverlap(r.getTimeFrom(), r.getTimeTo(),
                            br[i].getTimeFrom(), br[i].getTimeTo())) {
                        throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
                                International.getString("Die Reservierung überschneidet sich mit einer anderen Reservierung.") + buildOverlappingReservationInfo(br[i]),
                                Thread.currentThread().getStackTrace());
                        
                    }
                }
                if (r.getType().equals(BoatReservationRecord.TYPE_ONETIME)) {
                    assertFieldNotEmpty(record, BoatReservationRecord.DATEFROM);
                    assertFieldNotEmpty(record, BoatReservationRecord.DATETO);
                    assertFieldNotEmpty(record, BoatReservationRecord.TIMEFROM);
                    assertFieldNotEmpty(record, BoatReservationRecord.TIMETO);
                    if (DataTypeDate.isRangeOverlap(r.getDateFrom(),
                                                    r.getTimeFrom(),
                                                    r.getDateTo(),
                                                    r.getTimeTo(),
                                                    br[i].getDateFrom(),
                                                    br[i].getTimeFrom(),
                                                    br[i].getDateTo(),
                                                    br[i].getTimeTo())) {
                        throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
                                International.getString("Die Reservierung überschneidet sich mit einer anderen Reservierung.") + buildOverlappingReservationInfo(br[i]),
                                Thread.currentThread().getStackTrace());

                    }
                }
            }
            if (r.getType().equals(BoatReservationRecord.TYPE_ONETIME) &&
                r.getDayOfWeek() != null) {
                r.setDayOfWeek(null);
            }
        }
    }

    
    /*
     * Returns a list of Weekdays (as EFA Types) which are within a period defined by two dates.
     * 
     * Intention: determine whether a one-time reservation overlaps with a weekly reservation.
     */
    private ArrayList<String> getWeekdaysOfTimePeriod(DataTypeDate from, DataTypeDate to) {
    	ArrayList<String> result=new ArrayList<String>();
    	
    	result.add(from.getWeekdayAsEfaType());
    	
    	int daysAdded=1;
    	
    	DataTypeDate nextDay=from;
    	nextDay.addDays(1);
    	
    	while (daysAdded <7 && nextDay.compareTo(to) <=0) {
    		result.add(nextDay.getWeekdayAsEfaType());
    		nextDay.addDays(1);
    		daysAdded = daysAdded+1;
    	}
    	    	
    	return result;
    	
    }    
    
    private boolean isOneTimeReservationOverlappingWithWeeklyReservation(BoatReservationRecord oneTimeRes, BoatReservationRecord periodRes) {
    	
    	boolean overLapping=false;
    	
      	
    	ArrayList <String> theWeekDays= getWeekdaysOfTimePeriod(oneTimeRes.getDateFrom(), oneTimeRes.getDateTo());
    	
    	//Wenn der Wochentag der Weekly-Reservierung in der ermittelten Liste der Wochentage ist,
    	//und der Zeitraum eine Überdeckung hat, dann liegt ein Konflikt mit der Reserierung vor.
   	
    	                    
    	if (theWeekDays.contains(periodRes.getDayOfWeek())) {
    		
    		if (theWeekDays.size()==1) {
    			// Reservierung betrifft nur einen einzigen Tag.
    			// dann müssen wir auch nur diesen einen Tag vergleiche - wirkt sich
    			// bei der Zeitraumsbetrachtung aus.

    			if (theWeekDays.indexOf(periodRes.getDayOfWeek())==0) {
    				// Reservierung betrifft einen einzigen Tag, und der ist Gegenstand
    				// einer wöchentlichen Reservierung
    				
        			overLapping= DataTypeTime.isRangeOverlap(oneTimeRes.getTimeFrom(), oneTimeRes.getTimeTo(),
        					periodRes.getTimeFrom(), periodRes.getTimeTo());	
    				
    			}
    			
    		} else {
    		
    			// Position der Fundstelle abprüfen.
        		int pos = theWeekDays.indexOf(periodRes.getDayOfWeek());
        		
        		if (pos==0) {
        			// am Ersten Tag der Reservierung müssen wir die Startzeit von R bis 23:59 nehmen
        		
        			overLapping= DataTypeTime.isRangeOverlap(oneTimeRes.getTimeFrom(), new DataTypeTime(23,59,59),
        					periodRes.getTimeFrom(), periodRes.getTimeTo());
        			
        		}
        		else if (pos==theWeekDays.size()-1) {
        			// am letzten Tag der Reservierung müssen wir 00:00 bis zur Endezeit von R nehmen
        			overLapping= DataTypeTime.isRangeOverlap(new DataTypeTime(00,00,00),oneTimeRes.getTimeTo(),
        					periodRes.getTimeFrom(), periodRes.getTimeTo());
        			
        		}
        		else {
        			// es ist ein Tag in der Mitte -> Zeit von oneTimeRes= 00:00 - 23:59 
        			// damit haben wir definitiv eine Überlappung, wenn der Wochentag betroffen ist.
        			overLapping=true;
        		}
    		}
    	}
    	
    	return overLapping;
    	
    }    

    private void checkMixedTypeReservations (BoatReservationRecord oneReservation, BoatReservationRecord otherReservation) throws EfaModifyException {
        // das hier ist der interessante Fall: 
        
        // die neue Reservierung ist einmalig, die vorhandene Reservierung ist eine wöchentliche  
        if (oneReservation.getType()== BoatReservationRecord.TYPE_ONETIME) {
            // wir ermitteln die Wochentage, die von der NEUEN Reservierung betroffen sind.
      	
            if (isOneTimeReservationOverlappingWithWeeklyReservation (oneReservation, otherReservation)) {
            	
            	String CONFLICT_HANDLING_TYPE = Daten.efaConfig.getWeeklyReservationConflictBehaviour();
            	
            	if (CONFLICT_HANDLING_TYPE.equalsIgnoreCase(Daten.efaConfig.WEEKLY_RESERVATION_CONFLICT_IGNORE)) {
            		return; //ignore the conflict
            	} else if ((CONFLICT_HANDLING_TYPE.equalsIgnoreCase(Daten.efaConfig.WEEKLY_RESERVATION_CONFLICT_STRICT)) ||
            			(CONFLICT_HANDLING_TYPE.equalsIgnoreCase(Daten.efaConfig.WEEKLY_RESERVATION_CONFLICT_PRIORITIZE_WEEKLY))) {

            		// both handling types are the same if the NEW reservation type is one-time
	            	throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
	                        International.getString("Die_Reservierung_ueberschneidet_sich_mit_einer_anderen_Reservierung") + buildOverlappingReservationInfo(otherReservation),
	                        Thread.currentThread().getStackTrace());
	            	
            	} 	            	
            }        	
        }
        else if (oneReservation.getType()==BoatReservationRecord.TYPE_WEEKLY) {
        	// wir haben eine neue wöchentliche Reservierung, 
        	// die sich mit einer vorhandenen einmaligen Reservierung beißen kann.
        	
        	// ähnlicher Aufruf, nur dass nun die Neue Reservierung Weekly ist, und die bestehende Reservierung onetime...
        	// also die für die Prüfung einfach vertauschen.
        	
        	 if (isOneTimeReservationOverlappingWithWeeklyReservation (otherReservation,oneReservation)) {
                	
             	String CONFLICT_HANDLING_TYPE = Daten.efaConfig.getWeeklyReservationConflictBehaviour();
            	
             	if (CONFLICT_HANDLING_TYPE.equalsIgnoreCase(Daten.efaConfig.WEEKLY_RESERVATION_CONFLICT_IGNORE)) {
             		return; //ignore the conflict
             		
             	} else if (CONFLICT_HANDLING_TYPE.equalsIgnoreCase(Daten.efaConfig.WEEKLY_RESERVATION_CONFLICT_STRICT)) {
             	
             		throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
 	                       International.getString("Die_Reservierung_ueberschneidet_sich_mit_einer_anderen_Reservierung") + buildOverlappingReservationInfo(otherReservation),
 	                       Thread.currentThread().getStackTrace());
             	
        	 	} else if (CONFLICT_HANDLING_TYPE.equalsIgnoreCase(Daten.efaConfig.WEEKLY_RESERVATION_CONFLICT_PRIORITIZE_WEEKLY)) {
        	 		// new reservation is weekly. this is prioritized - we ignore an EXISTING one-time reservation for the same boat
        		    return;
        	 	}
             return;
        	 }
        	
        	
        }    	
    }
    
}

