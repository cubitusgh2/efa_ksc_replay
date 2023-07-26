package de.nmichael.efa.data.sync;

import java.util.Hashtable;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.data.GroupRecord;
import de.nmichael.efa.data.Logbook;
import de.nmichael.efa.data.PersonRecord;
import de.nmichael.efa.data.Persons;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataKeyIterator;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;

public class KanuEfbAuditPersons extends KanuEfbSyncTask {

    private int countSyncUsers = 0;
    private static String STR_EFA_EFB_SYNC_GROUPNAME = "KANU_EFB_SYNC_EINWILLIGUNG";
	private Hashtable <String, KanuEfbPersonAuditElement> personList = new Hashtable <String, KanuEfbPersonAuditElement>(); 
    public KanuEfbAuditPersons(Logbook logbook, AdminRecord admin) {
        super(logbook, admin);
    }
    
    public void run() {
        setRunning(true);
        try {
            Thread.sleep(1000);
        } catch(Exception e) {
        }
        int i = 0;
        thisSync = System.currentTimeMillis();
        logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Beginne Personen-Audit mit Kanu-eFB ...");
        logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Startzeit des Audit: " +
                EfaUtil.getTimeStamp(thisSync) + " (" + thisSync + ")");

        while(true) {
            if (!login()) {
                break;
            }
            setCurrentWorkDone(++i);
            if (!syncUsers()) {
                break;
            }
            setCurrentWorkDone(++i);

            break;
        }
        syncDone();
        setCurrentWorkDone(++i);
        if (i == getAbsoluteWork()) {
            Daten.project.setClubKanuEfbLastSync(thisSync);
            StringBuilder msg = new StringBuilder();
            if (countErrors == 0) {
                if (countWarnings == 0) {
                    msg.append("Personen-Audit mit Kanu-eFB erfolgreich beendet.");
                } else {
                    msg.append("Personen-Auditmit Kanu-eFB mit Warnungen beendet.");
                }
            } else {
                msg.append("Personen-Audit mit Kanu-eFB mit Fehlern beendet.");
            }
            msg.append(" [");
            msg.append(countSyncUsers  + " Personen, ");
            msg.append(countWarnings   + " Warnungen, ");
            msg.append(countErrors     + " Fehler");
            msg.append("]");
            logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, msg.toString());
            successfulCompleted = true;
        } else {
            logInfo(Logger.ERROR, Logger.MSG_SYNC_ERRORABORTSYNC, "Personen-Audit mit Kanu-eFB wegen Fehlern abgebrochen.");
            successfulCompleted = false;
        }
        setDone();
    }

    public int getAbsoluteWork() {
        return 3;// login, syncusers, logout
    }

    public String getSuccessfullyDoneMessage() {
        if (successfulCompleted) {
            return LogString.operationSuccessfullyCompleted(International.getString("Personen-Audit")) +
                   "\n"   + countSyncUsers + " Personen geprüft." +
                   "\n\n" + countWarnings + " Warnungen" +
                   "\n"   + countErrors + " Fehler";
        } else {
            return LogString.operationFailed(International.getString("Personen-Audit"));
        }
    }    

    private boolean syncUsers() {
        
    	long iPersonCount=0;
    	
    	try {
            logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Prüfe Personen ...");

            // ask eFB to sync all users from eFB -> efa
            // this is deprecated
            StringBuilder request = new StringBuilder();
            buildRequestHeader(request, "SyncUsers");
            logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Sende Synchronisierungs-Anfrage für alle Personen ...");
           
            
            try {
            	iPersonCount=buildRequestContentsForUsers(request);
            } catch (Exception e) {
                e.printStackTrace();
            	return false; 
            }

            buildRequestFooter(request);
            
            if (iPersonCount==0) {
                logInfo(Logger.WARNING, Logger.MSG_SYNC_SYNCINFO, "Es wurden keine Personen gefunden, für die ein Audit durchgeführt werden könnte.");

            	return false;
            } else {
	            KanuEfbXmlResponse response = sendRequest(request.toString(), true);
	            if (!handleSyncUserResponse(response)) {
	                return false;
	            }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }    
    
    private long buildRequestContentsForUsers (StringBuilder request) throws Exception{
        // transmit all efa users without eFB ID's to eFB
  
        Persons persons = Daten.project.getPersons(false);
        int reqCnt = 0;
        DataKeyIterator it = persons.data().getStaticIterator();
        DataKey k = it.getFirst();
        while (k != null) {
            PersonRecord r = (PersonRecord)persons.data().get(k);
            
            KanuEfbPersonAuditElement pae = new KanuEfbPersonAuditElement(r.getFirstName(), r.getLastName(), r.getBirthday(),r.getId());
            
            pae.setEfaKanuEFBID(r.getEfbId());
            pae.setIsAktuellGueltig(r.isValidAt(thisSync));
            pae.setPersonenStatus(r.getStatusName());
            pae.setIsKanuEFBSyncEinwilligung(personGroupListContains(r, STR_EFA_EFB_SYNC_GROUPNAME));
            personList.put(pae.getKey(), pae);
            
            if (r != null && pae.getIsAktuellGueltig() 
            		// && r.isStatusMember()  
            		//&& pae.getIsKanuEFBSyncEinwilligung()
            		// && (r.getEfbId() == null || r.getEfbId().length() == 0)
            		) {
                if (Logger.isTraceOn(Logger.TT_SYNC)) {
                    logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCINFO, "  erstelle Synchronisierungs-Anfrage für Person: " + r.getQualifiedName());
                }
                request.append("<person>");
                request.append("<firstName>"+(r.getFirstName() == null ? "" : r.getFirstName() )+"</firstName>");
                request.append("<lastName>"+(r.getLastName() == null ? "" : r.getLastName() )+"</lastName>");
                if (r.getBirthday() != null && r.getBirthday().isSet()) {
                    request.append("<dateOfBirth>"+r.getBirthday().toString()+"</dateOfBirth>");
                }
                request.append("</person>\n");
                reqCnt++;
            }
            k = it.getNext();
        }
        return reqCnt;
    }
    
	private boolean personGroupListContains(PersonRecord rec, String groupName) {
		
        GroupRecord[] groupList = rec.getGroupList();
        if (groupList != null) {
			for (int i=0; i<groupList.length; i++) {
				if (groupList[i].getName().equalsIgnoreCase(groupName)){
					return true;
				}
			}
        }
		return false;
	}

    private boolean handleSyncUserResponse(KanuEfbXmlResponse response) throws Exception {
        Persons persons = Daten.project.getPersons(false);
        if (response != null && response.isResponseOk("SyncUsers")) {
            logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Synchronisierungs-Antwort erhalten für " + response.getNumberOfRecords() + " Personen ...");
            for (int i = 0; i < response.getNumberOfRecords(); i++) {
                Hashtable<String, String> fields = response.getFields(i);
                boolean ok = false;
                String personName = "<unknown>";
                String firstName = fields.get("firstname");
                String lastName = fields.get("lastname");
                String dateOfBirth = fields.get("dateofbirth");
                String efbId = fields.get("id");
                if (firstName != null && lastName != null) {
                    firstName = firstName.trim();
                    lastName = lastName.trim();
                    personName = PersonRecord.getFullName(firstName, lastName, "", false);
                    PersonRecord[] plist = persons.getPersons(personName, thisSync);
                    PersonRecord p = (plist != null && plist.length == 1 ? plist[0] : null);

                    // try to match person on date of birth
                    for (int pi = 0; p == null && plist != null && pi < plist.length; pi++) {
                        if (plist[pi].getBirthday() != null && dateOfBirth != null
                                && plist[pi].getBirthday().isSet()) {
                            DataTypeDate bday = DataTypeDate.parseDate(dateOfBirth);
                            if (bday != null && bday.isSet()) {
                                if (bday.equals(plist[pi].getBirthday())) {
                                    p = plist[pi];
                                } else {
                                    if (plist[i].getBirthday().getDay() < 1
                                            && plist[i].getBirthday().getMonth() < 1
                                            && plist[i].getBirthday().getYear() == bday.getYear()) {
                                        p = plist[pi];
                                    }
                                }
                            }
                        }
                    }
                    if (efbId != null && p != null) {
                        efbId = efbId.trim();
                       
                         KanuEfbPersonAuditElement pae=personList.get(p.getFirstName()+p.getLastName()+p.getBirthday().toString());

                         if (pae != null) {
                        	 pae.setEfbKanuEFBID(efbId);
                         }
                        		 
                        /*
                        hier ist die Änderung.
                        if (!efbId.equals(p.getEfbId())) {
                            p.setEfbId(efbId);
                            persons.data().update(p);
                            countSyncUsers++;
                        } */
                        ok = true;
                    }
                }
                if (Logger.isTraceOn(Logger.TT_SYNC)) {
                    if (ok) {
                        logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCINFO, "  Synchronisierungs-Antwort für Person: " + personName + " (EfbId=" + efbId + ")");
                    } else {
                        logInfo(Logger.DEBUG, Logger.MSG_SYNC_WARNINCORRECTRESPONSE, "  Synchronisierungs-Antwort für unbekannte Person: " + personName);
                    }
                }
            }
            logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, countSyncUsers + " neue Personen synchronisiert.");
            return true;
        } else {
            logInfo(Logger.ERROR, Logger.MSG_SYNC_ERRORINVALIDRESPONSE, "Ungültige Synchronisierungs-Antwort.");
            return false;
        }
    }    
    
    
}
