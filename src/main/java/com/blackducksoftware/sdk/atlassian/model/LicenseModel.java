package com.blackducksoftware.sdk.atlassian.model;

/**
 *  @author jatoui
 *  @title Solutions Architect
 *  @email jatoui@blackducksoftware.com
 *  @company Black Duck Software
 *  @year 2012
 **/

public class LicenseModel {

	private String id;
	
	private String name;
	
	private String text;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@Override
	public boolean equals(Object license)
	{
		
		if (license == null) return false;
	    if (license == this) return true;
	    if (!(license instanceof LicenseModel))return false;
    
	    LicenseModel licenseModel = (LicenseModel)license;
	    
	    
	    if(licenseModel.getId() != null)
	    	return licenseModel.getId().equals(this.getId());
	 
	    else
	    	return licenseModel.getText().equals(this.getText());
	}
	
	
}
