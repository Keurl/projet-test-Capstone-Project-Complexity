package fr.launchmycraft.library.authentication;

import com.google.gson.Gson;

import fr.launchmycraft.launcher.LoggerUtils;
import fr.launchmycraft.library.util.Util;

import java.io.IOException;
import java.util.HashMap;

public class Account {
 
    public Account(String user, String pass, HashMap<String, String> details)           
    {
        username = user;
        password = pass;
        this.details = details;
    }
    
    HashMap<String, String> details;
    
    public Account(String user, String atoken, String ctoken, LoginProfile profi, HashMap<String, String> details)
    {
    	accesstoken = atoken;
    	profile = profi;
    	username = user;
    	clienttoken = ctoken;
    	this.details = details;
    }
    
    public String username;
    public String password;
    LoginProfile profile;
    
    String accesstoken;
    String clienttoken;
    
    int mode;
    
    public LoginResult doLogin() throws IOException, LoginException
    {
    	if (profile != null)
    	{
    		//On se log depuis un token
    		//donc on le renvoie
    		LoginResult r = new LoginResult();
    		r.accessToken = accesstoken;
    		r.clientToken = clienttoken;   		
    		r.selectedProfile = profile;
    		r.loggedIn = true;
    		return r;
    	}
    	
        Gson gson = new Gson();  
        
        //On fait la requ�te
        LoginRequest request = new LoginRequest(username, password);
        //On la r�cup�re sous forme de String
        String requestJSON = gson.toJson(request, LoginRequest.class);
        
        if (details.containsKey("debug"))
        {
        	LoggerUtils.println("Envoi des donn�es de connexion : " + requestJSON);
        }
              
        //On envoie le post
        String response = Util.doPOST(Util.getAuthUrl(details), requestJSON, "application/json");
        if (details.containsKey("debug"))
        {
        	LoggerUtils.println("R�ponse du serveur : " + response);
        }
        LoginResult result = gson.fromJson(response, LoginResult.class);
        
        //On v�rifie que tout va bien sinon on chie des exceptions
        if (result.error != null)
        {
        	if (result.cause.equals("UserMigratedException"))
        	{
        		throw new LoginException("compte migr�, utilisez votre adresse email");
        	}
        	else if (result.error.equals("ForbiddenOperationException"))
        	{
        		throw new LoginException("mauvais couple identifiant/mot de passe");
        	}
        	else
        	{
        		throw new LoginException(result.errorMessage);
        	}      
        }
        else if (result.selectedProfile == null)
        {
        	throw new LoginException("Licence Minecraft non pr�sente sur le compte.");
        }
        
        result.loggedIn = true;
        
        return result;
    }   
}