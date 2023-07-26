package de.nmichael.efa.data.sync;

public class KanuEfbPersonAuditStatistics {

	private int personsInDB_total =0;
	private int personsInDB_invalid =0;
	private int personsInDB_member =0;
	private int personsInDB_nonMember=0;
	private int personsInDB_in_efb_sync_group=0;
	private int personsInDB_mitEFBID=0;
	
	private int personsInDB_Audited=0;

	
	private void incrementPersonsInDB_totalIfTrue(Boolean value) {
		if(value) {personsInDB_total++;};
	}
	private void incrementPersonsInDB_invalidIfTrue(Boolean value) {
		if(value) {personsInDB_invalid++;};
	}
	private void incrementPersonsInDB_memberIfTrue(Boolean value) {
		if(value) {personsInDB_member++;};
	}
	private void incrementPersonsInDB_nonMemberIfTrue(Boolean value) {
		if(value) {personsInDB_nonMember++;};
	}
	private void incrementPersonsInDB_in_efb_sync_groupIfTrue(Boolean value) {
		if(value) {personsInDB_in_efb_sync_group++;};
	}
	private void incrementPersonsInDB_mitEFBIDIfTrue(Boolean value) {
		if(value) {personsInDB_mitEFBID++;};
	}
	private void incrementPersonsInDB_auditedIfTrue(Boolean value) {
		if(value) {personsInDB_Audited++;};
	}



	
	
	private int getPersonsInDB_total() {
		return personsInDB_total;
	}

	private void setPersonsInDB_total(int personsInDB_total) {
		this.personsInDB_total = personsInDB_total;
	}

	private int getPersonsInDB_invalid() {
		return personsInDB_invalid;
	}

	private void setPersonsInDB_invalid(int personsInDB_invalid) {
		this.personsInDB_invalid = personsInDB_invalid;
	}

	private int getPersonsInDB_member() {
		return personsInDB_member;
	}

	private void setPersonsInDB_member(int personsInDB_member) {
		this.personsInDB_member = personsInDB_member;
	}

	private int getPersonsInDB_nonMember() {
		return personsInDB_nonMember;
	}

	private void setPersonsInDB_nonMember(int personsInDB_nonMember) {
		this.personsInDB_nonMember = personsInDB_nonMember;
	}

	private int getPersonsInDB_in_efb_sync_group() {
		return personsInDB_in_efb_sync_group;
	}

	private void setPersonsInDB_in_efb_sync_group(int personsInDB_in_efb_sync_group) {
		this.personsInDB_in_efb_sync_group = personsInDB_in_efb_sync_group;
	}

	private int getPersonsInDB_mitEFBID() {
		return personsInDB_mitEFBID;
	}

	private void setPersonsInDB_mitEFBID(int personsInDB_mitEFBID) {
		this.personsInDB_mitEFBID = personsInDB_mitEFBID;
	}

	private int getPersonsInDB_Audited() {
		return personsInDB_Audited;
	}

	private void setPersonsInDB_Audited(int personsInDB_Audited) {
		this.personsInDB_Audited = personsInDB_Audited;
	}
	
	
}
