package org.hfoss.posit.android.bluetooth;

public class SelectFind implements Comparable<SelectFind> {

	// Data stored
	private String mGuid = "";
	private boolean mState = false;
	
	private Long mId = null;
	private String mName = "";
	private String mDesc = "";

	public SelectFind(String guid, boolean state, Long id, String name, String desc) {
		mGuid = guid;
		mState = state;
		mName = name;
		mDesc = desc;
		mId = id;
	}

	public void setState(boolean value) {
		this.mState = value;
	}

	public boolean getState() {
		return this.mState;
	}
	
	public void toggleState() {
		this.mState = !this.mState;
	}

	public String getGuid() {
		return mGuid;
	}

	public void setGuid(String guid) {
		mGuid = guid;
	}
	
	public String getName() {
		return mName;
	}
	
	public String getDesc() {
		return mDesc;
	}
	
	public Long getId() {
		return mId;
	}

	// Compare on its name
	public int compareTo(SelectFind other) {
		if (this.mGuid != null)
			return this.mGuid.compareTo(other.getGuid());
		else
			throw new IllegalArgumentException();
	}

}
