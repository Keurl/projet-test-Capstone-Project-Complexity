package fr.launchmycraft.library;

import java.util.HashMap;

import com.google.gson.annotations.SerializedName;

import fr.launchmycraft.library.authentication.Credentials;

public class Configuration {
	
	@SerializedName("storedCredentials")
	public HashMap<Long, Credentials> storedCredentials = new HashMap<Long, Credentials>();
	
	@SerializedName("javaPath")
	public HashMap<Long, String> javaPath = new HashMap<Long, String>();

	@SerializedName("cguRevision")
	public int cguRevision = 0;
}
