package de.nmichael.efa.data.sync;

import java.util.UUID;

import de.nmichael.efa.data.types.DataTypeDate;

public class KanuEfbPersonAuditElement {

	private String vorname;
	private String nachname;
	private DataTypeDate geburtsdatum;
	private UUID personUUID;
	
	private String efaKanuEFBID;
	private String efbKanuEFBID;
	private String personenStatus; //Mitglied oder nicht?
	private Boolean isKanuEFBSyncEinwilligung=false; //ist das Mitglied in der Gruppe, die für die Abfrage der Daten bei KanuEFB erforderlich ist?
	private Boolean isAktuellGueltig=false;
	private String efbKanuResult;
	
	public KanuEfbPersonAuditElement(String firstName, String lastName, DataTypeDate birthday, UUID recID ) {
		this.vorname = firstName;
		this.nachname = lastName;
		this.geburtsdatum = birthday;
		this.personUUID = recID;
	}
	
	
	public String getKey() {
		String result="";
		result= result + (getVorname()==null ? "": getVorname());
		result = result + (getNachname()==null ? "": getNachname());
		result = result + (getGeburtsdatum() ==null ? "": getGeburtsdatum().toString());
		return result;
	}

	public String getVorname() {
		return vorname;
	}

	public void setVorname(String vorname) {
		this.vorname = vorname;
	}

	public String getNachname() {
		return nachname;
	}

	public void setNachname(String nachname) {
		this.nachname = nachname;
	}

	public DataTypeDate getGeburtsdatum() {
		return geburtsdatum;
	}

	public void setGeburtsdatum(DataTypeDate geburtsdatum) {
		this.geburtsdatum = geburtsdatum;
	}

	public UUID getPersonUUID() {
		return personUUID;
	}

	public void setPersonUUID(UUID personUUID) {
		this.personUUID = personUUID;
	}

	public String getEfaKanuEFBID() {
		return efaKanuEFBID;
	}

	public void setEfaKanuEFBID(String efaKanuEFBID) {
		this.efaKanuEFBID = efaKanuEFBID;
	}

	public String getEfbKanuEFBID() {
		return efbKanuEFBID;
	}

	public void setEfbKanuEFBID(String efbKanuEFBID) {
		this.efbKanuEFBID = efbKanuEFBID;
	}

	public String getPersonenStatus() {
		return personenStatus;
	}

	public void setPersonenStatus(String personenStatus) {
		this.personenStatus = personenStatus;
	}

	public Boolean getIsKanuEFBSyncEinwilligung() {
		return isKanuEFBSyncEinwilligung;
	}

	public void setIsKanuEFBSyncEinwilligung(Boolean isKanuEFBSyncEinwilligung) {
		this.isKanuEFBSyncEinwilligung = isKanuEFBSyncEinwilligung;
	}

	public Boolean getIsAktuellGueltig() {
		return isAktuellGueltig;
	}

	public void setIsAktuellGueltig(Boolean isAktuellGueltig) {
		this.isAktuellGueltig = isAktuellGueltig;
	}

	public String getEfbKanuResult() {
		return efbKanuResult;
	}

	public void setEfbKanuResult(String efbKanuResult) {
		this.efbKanuResult = efbKanuResult;
	}
	
	
}
