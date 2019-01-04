package fr.launchmycraft.library.versionning;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import fr.launchmycraft.library.util.Util;

public class VersionDetails {
    
	@SerializedName("id")
    public String id = "";
	
	@SerializedName("minecraftArguments")
    public String minecraftArguments = "";
	
	@SerializedName("libraries")
    public ArrayList<Library> libraries = new ArrayList<Library>();
    
    @SerializedName("mainClass")
    public String mainClass = "";
    
    @SerializedName("rules")
    public ArrayList<VersionRule> rules = new ArrayList<VersionRule>();
    
    @SerializedName("assets")
    public String assets;
    
    @SerializedName("inheritsFrom")
    public String inheritsFrom;
    
    public void heritage(VersionDetails newData) {
    	
    	if (this.id != null) {newData.id = this.id;}
    	if (minecraftArguments != null) {newData.minecraftArguments = minecraftArguments;}
    	if (mainClass != null) {newData.mainClass = mainClass;}
    	if (rules != null) {newData.rules.addAll(rules);}
    	if (assets != null) {newData.assets = assets;}
    	
    }
    
    public VersionDetails resolveDependencies(HashMap<String, String> details, long id, boolean hasPaid, String identifier) throws Exception
    {
    	if (inheritsFrom == null || inheritsFrom.isEmpty())
    	{
    		return this;
    	}
    	
    	//Le nouvel objet
    	VersionDetails newData = null;
    	
    	//Le fichier du parent
    	File parentFile = Util.getJsonFile(details, inheritsFrom, id, hasPaid, identifier);
    	
    	//Le fichier existe, on le r�cup�re
    	if (parentFile.exists())
    	{
    		newData = new Gson().fromJson(Util.getFileContent(new FileInputStream(parentFile), true, true), VersionDetails.class);
    	}
    	//Le fichier n'existe pas
    	else
    	{
    		throw new Exception("Impossible de r�soudre la d�pendance " + inheritsFrom + " - fichier inexistant");
    	}
    	
    	//On r�sout les d�pendances du parent
    	newData = newData.resolveDependencies(details, id, hasPaid, identifier);
    	heritage(newData);
    	//On effectue l'h�ritage
    	/*if (this.id != null) {newData.id = this.id;}
    	if (minecraftArguments != null) {newData.minecraftArguments = minecraftArguments;}
    	if (mainClass != null) {newData.mainClass = mainClass;}
    	if (rules != null) {newData.rules.addAll(rules);}
    	if (assets != null) {newData.assets = assets;}*/
    	newData.inheritsFrom = null;
    	
    	//Les librairies
    	for (Library lib : libraries)
    	{
    		lib.splitNameAndVersion();
    		
    		//On regarde si le newData le contient
    		Library libNew = null;
    		
    		for (Library libNewData : newData.libraries)
    		{
    			libNewData.splitNameAndVersion();
    			    			
    			if (lib.getNameOnly().equals(libNewData.getNameOnly()))
    			{
    				libNew = libNewData;
    				break;
    			}
    		}
    		
    		if (libNew == null)
    		{
    			//On ne l'a pas trouv�e, on la rajoute
    			newData.libraries.add(lib);
    		}
    		else
    		{
    			//On l'a trouv�e, on regarde si la version est sup�rieure		
    			if (isVersionGreater(lib.getVersion(), libNew.getVersion()))
    			{
    				//On vire l'ancienne et on met la nouvelle � la place
    				newData.libraries.remove(libNew);
    				newData.libraries.add(lib);
    			}
    		}
    	}
    	
    	return newData;
    }
    
    boolean isVersionGreater(String v1, String v2)
    {
    	String[] v1Split = v1.split("\\.");
    	String[] v2Split = v2.split("\\.");
    	
    	int count = (v1Split.length > v2Split.length) ? v1Split.length : v2Split.length;
    	
    	for (int i = 0; i < count; i++)
    	{
    		if (Integer.parseInt(v1Split[i]) > Integer.parseInt(v2Split[i]))
    		{
    			return true;
    		}
    	}
    	
    	return false;
    }
    
}

