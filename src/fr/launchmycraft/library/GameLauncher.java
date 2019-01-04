package fr.launchmycraft.library;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

import javax.swing.JOptionPane;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import fr.launchmycraft.launcher.LoggerUtils;
import fr.launchmycraft.launcher.OperatingSystem;
import fr.launchmycraft.library.authentication.Account;
import fr.launchmycraft.library.authentication.Credentials;
import fr.launchmycraft.library.authentication.LoginException;
import fr.launchmycraft.library.authentication.LoginResult;
import fr.launchmycraft.library.resourcing.ResourcesUpdater;
import fr.launchmycraft.library.util.JavaProcessLauncher;
import fr.launchmycraft.library.util.Util;
import fr.launchmycraft.library.util.listeners.FileEndedListener;
import fr.launchmycraft.library.util.listeners.FileProgressionListener;
import fr.launchmycraft.library.util.listeners.GameLaunchedListener;
import fr.launchmycraft.library.util.listeners.StateListener;
import fr.launchmycraft.library.util.network.DownloadJob;
import fr.launchmycraft.library.util.network.Downloadable;
import fr.launchmycraft.library.util.network.HTTPDownloader;
import fr.launchmycraft.library.versionning.Library;
import fr.launchmycraft.library.versionning.VersionDetails;
import fr.launchmycraft.library.versionning.VersionsList;

public class GameLauncher 
{
	
	FileProgressionListener progressListener;
	StateListener stateListener;
	GameLaunchedListener gameLaunchedListener;
	boolean forceUpdate;
	//boolean crackedEnabled;
	long launcherId;
	
	boolean hasPaid;
	String identifier;
	
	HashMap<String, String> launcherDetails;
	
	//String username;
	//String password;
	
	Account account;
	
	class SeparatorStringBuilder
    { 
        StringBuilder builder = new StringBuilder();
        String separator = System.getProperty("path.separator");
        
        public void append(String str)
        {
            if (builder.length() > 0)
            {
                builder.append(separator);
            }
            
            builder.append(str);
        }
        
        @Override
        public String toString()
        {
            return builder.toString();
        }
    }
	
	Configuration config;
		
	public GameLauncher(long launcherId, boolean forceUpdate, HashMap<String, String> details, Account account, FileProgressionListener progressListener, StateListener stateListener, GameLaunchedListener gameLaunchedListener, boolean hasPaid, String identifier)
	{
		this.progressListener = progressListener;
		this.stateListener = stateListener;
		this.gameLaunchedListener = gameLaunchedListener;
		this.forceUpdate = forceUpdate;
		//this.crackedEnabled = cracked;
		this.launcherId = launcherId;
		this.account = account;
		this.launcherDetails = details;
		this.hasPaid = hasPaid;
		this.identifier = identifier;
	}
	
	public String fenetre(String minecraftArguments) {
        if (launcherDetails.containsKey("width"))
        {
        	LoggerUtils.println("Ajout de la largeur personnalisée : " + launcherDetails.get("width"));
        	minecraftArguments += " --width " + launcherDetails.get("width");
        }
        else
        {
        	LoggerUtils.println("Largeur par défaut : " + 854);
        	minecraftArguments += " --width " + 854;
        }
        
        if (launcherDetails.containsKey("height"))
        {
        	LoggerUtils.println("Ajout de la hauteur personnalisée : " + launcherDetails.get("height"));
        	minecraftArguments += " --height " + launcherDetails.get("height");
        }
        else
        {
        	LoggerUtils.println("Hauteur par défaut : " + 480);
        	minecraftArguments += " --height " + 480;
        }
        //Plein ecran
        if (launcherDetails.containsKey("fullscreen") && launcherDetails.get("fullscreen").equals("1"))
        {
        	LoggerUtils.println("Plein écran activé");
        	minecraftArguments += " --fullscreen";
        }
        
        return minecraftArguments;
	}
	
	public String auth(String minecraftArguments, LoginResult result) {
	
	    //Auth
	    if (result.loggedIn)
	    {
	    	LoggerUtils.println("Authentifié ; ajout des informations...");
	        //Username
	        minecraftArguments = minecraftArguments.replace("${auth_player_name}", result.selectedProfile.name);
	        
	        //AccessToken
	        minecraftArguments = minecraftArguments.replace("${auth_access_token}", result.accessToken);
	        
	        //Session
	        minecraftArguments = minecraftArguments.replace("${auth_session}", "token:" + result.accessToken + ":" + result.selectedProfile.id);
	        
	        //UUID
	        minecraftArguments = minecraftArguments.replace("${auth_uuid}", result.selectedProfile.id);
		}
	    else
	    {          
	    	LoggerUtils.println("Non authentifié ; ajout des informations par défaut...");
	        minecraftArguments = minecraftArguments.replace("${auth_player_name}", account.username);
	        minecraftArguments = minecraftArguments.replace("${auth_uuid}", new UUID(0L, 0L).toString());
	    }
	    
	    return minecraftArguments;
	}
	
	public void libraryPath(JavaProcessLauncher processLauncher, VersionDetails versionDetails) throws JsonSyntaxException, IOException {	
	    //Library Path
	    File nativeDir = Util.getNativesDir(launcherDetails, launcherId, hasPaid, identifier);
	    //On supprime le dossier des natives
	    nativeDir.delete();
	    //On le refait
	    nativeDir.mkdirs();
	    
	    //Et on les unpack
	    for (Library lib : versionDetails.libraries)
	    {                  
	        if (lib.getNatives() != null && lib.getNatives().get(OperatingSystem.getCurrentPlatform().getName()) != null)
	        {
	            try
	            {
	                String classifier = lib.getNatives().get(OperatingSystem.getCurrentPlatform().getName());
	                File file = new File(Util.getLibrariesFolder(launcherDetails, launcherId, hasPaid, identifier) + "/" + lib.getArtifactPath(classifier));                     
	           
	                LoggerUtils.println("Extraction de la librairie native : " + file.getAbsolutePath() + " dans " + nativeDir.getAbsolutePath());
	                Util.unpackArchive(file, nativeDir);
	            }
	            catch (Exception ex)
	            {}                      
	        }                      
	    }
	    processLauncher.addCommand(new String[] {"-Djava.library.path=" + nativeDir.getAbsolutePath()});
	
	    //Main Class
	    processLauncher.addCommand(new String[] {versionDetails.mainClass});
	    
	    //return processLauncher;
	}
	
	
	public void dependencies(VersionDetails versionDetails, FileEndedListener endedListener) throws InterruptedException, Exception{
		 ArrayList<Downloadable> libList = new ArrayList<>();
	     LoggerUtils.println(versionDetails.libraries.size() + " bibliothèques à  vérifier...");
	     for (int i = 0; i < versionDetails.libraries.size(); i++)
	     {
	         Library lib = versionDetails.libraries.get(i);
	         
	         String classifier = null;
	         
	         if (lib.getNatives() != null && lib.getNatives().get(OperatingSystem.getCurrentPlatform().getName()) != null)
	         {
	             classifier = lib.getNatives().get(OperatingSystem.getCurrentPlatform().getName());
	         }    
	         
	         File libFile = new File(Util.getLibrariesFolder(launcherDetails, launcherId, hasPaid, identifier) + "/" + lib.getArtifactPath(classifier));
	         
	         if ((!libFile.exists() || forceUpdate))
	         {
	         	stateListener.onStateChanged(false, "Mise à jour des bibliothèques...");
	             libFile.getParentFile().mkdirs();
	             libList.add(new Downloadable(Util.getLibrariesUrl() +  lib.getArtifactPath(classifier), libFile));
	         }
	     }
	     
	     LoggerUtils.println(libList.size() + " bibliothèques à mettre à jour...");
	     
	     DownloadJob libJob = new DownloadJob(libList);
	     libJob.setNewFileListener(endedListener);
	     libJob.setLibraryMode(true);
	     if (!libJob.downloadAll())
	     {
	         throw new Exception("impossible de vérifier les bibliothèques");                   
	         
	     }
	}
	
	public void majJAR(Gson gson) throws JsonSyntaxException, NumberFormatException, FileNotFoundException, IOException, Exception {
		
		//Mise a jour du JAR
		stateListener.onStateChanged(false, "Calcul des différences...");
		
		if (!Util.getJarFile(launcherDetails, launcherId, hasPaid, identifier).exists() || !Util.getJsonFile(launcherDetails, launcherId, hasPaid, identifier).exists() || forceUpdate || !Util.isRevisionUpToDate(Util.isCustomJarEnabled(launcherDetails), Util.getJarRevisionFile(launcherDetails, launcherId, hasPaid, identifier), Long.parseLong(launcherDetails.get("customjarrevision")), false))
		{
			stateListener.onStateChanged(false, "Récupération des informations de la version...");
			
			VersionDetails details = gson.fromJson(Util.doGET(Util.getOnlineJsonUrl(launcherDetails.get("gameversion")), null), VersionDetails.class);
			
			//Mise a jour du jeu
			stateListener.onStateChanged(false, "Mise à  jour du jeu...");
			
			Util.getVersionFolder(launcherId, launcherDetails, hasPaid, identifier).mkdirs();
			
			HTTPDownloader downloader = new HTTPDownloader(Util.getOnlineJarFile(launcherDetails), Util.getJarFile(launcherDetails, launcherId, hasPaid, identifier));
			downloader.setDownloadProgressionListener(progressListener);
			downloader.downloadFile();
			
			//Sauvegarde du JSON
			stateListener.onStateChanged(false, "Enregistrement des paramètres de la version...");
			if (!Util.getJsonFile(launcherDetails, launcherId, hasPaid, identifier).exists())
			{
				Util.getJsonFile(launcherDetails, launcherId, hasPaid, identifier).getParentFile().mkdirs();
				Util.getJsonFile(launcherDetails, launcherId, hasPaid, identifier).createNewFile();
			}
			Util.writeToFile(Util.getJsonFile(launcherDetails, launcherId, hasPaid, identifier), gson.toJson(details, VersionDetails.class));
			
			//On met a jour la revision
			if (Util.isCustomJarEnabled(launcherDetails))
			{
				Util.writeToFile(Util.getJarRevisionFile(launcherDetails, launcherId, hasPaid, identifier), launcherDetails.get("customjarrevision"));
			}
		}
		
	}
	
	public final void paramOS(JavaProcessLauncher processLauncher) throws JsonSyntaxException, IOException {
		final OperatingSystem os = OperatingSystem.getCurrentPlatform();
	    if(os.equals(OperatingSystem.OSX))
	    {
	        processLauncher.addCommand(new String[] {"-Xdock:icon=" + new File(Util.getResourcesFolder(launcherDetails, launcherId, hasPaid, identifier), "icons/minecraft.icns").getAbsolutePath() + " -Xdock:name=" + "Minecraft" });
	    }
	    else if(os.equals(OperatingSystem.WINDOWS))
	    {
	        processLauncher.addCommand(new String[] {"-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump" });
	
	    }
	    
	    //32-64bits
	    final boolean is32Bit = "32".equals(System.getProperty("sun.arch.data.model"));
	    final String defaultArgument = is32Bit ? "-Xmx512M" : "-Xmx1G";
	    processLauncher.addSplitCommand(defaultArgument);  
	    
	    //GC Shit
	    processLauncher.addCommand(new String[] {"-XX:+UseConcMarkSweepGC", "-XX:+CMSIncrementalMode", "-XX:-UseAdaptiveSizePolicy"});
	    
	    //Arguments personnalise
	    if (launcherDetails.containsKey("arguments"))          
	    {
	    	LoggerUtils.println("Ajout des arguments personnalisés : " + launcherDetails.get("arguments"));
	    	processLauncher.addSplitCommand(launcherDetails.get("arguments"));
	    }
	}
	
	public void classpaff(JavaProcessLauncher processLauncher,VersionDetails versionDetails) throws JsonSyntaxException, IOException {		
		
		SeparatorStringBuilder classPath = new SeparatorStringBuilder();
	    classPath.append(Util.getJarFile(launcherDetails, launcherId, hasPaid, identifier).getAbsolutePath());
	    
	    for (Library lib : versionDetails.libraries)
	    {               
	        String classifier = null;
	
	        if (lib.getNatives() != null && lib.getNatives().get(OperatingSystem.getCurrentPlatform().getName()) != null) {
	            classifier = lib.getNatives().get(OperatingSystem.getCurrentPlatform().getName());
	        }
	
	        classPath.append(new File(Util.getLibrariesFolder(launcherDetails, launcherId, hasPaid, identifier).getAbsolutePath() + "/" + lib.getArtifactPath(classifier)).getAbsolutePath());
	        
	    }
	    processLauncher.addCommand(new String[] {"-cp", "\"" + classPath.toString() + "\""});
		
	}
	
	public String server(String minecraftArguments) {
		
		if (launcherDetails.containsKey("enableautoconnection") && launcherDetails.get("enableautoconnection").equals("1"))
        {
        	LoggerUtils.println("Connexion automatique activée");
        	String port = "25565";
            if (launcherDetails.get("serveraddress").contains(":"))
            {
                port = launcherDetails.get("serveraddress").substring(launcherDetails.get("serveraddress").lastIndexOf(":"));  
                launcherDetails.put("serveraddress", launcherDetails.get("serveraddress").replace(port, ""));
                port = port.replace(":", "");
            }
            
            minecraftArguments += " --port " + port + " --server " + launcherDetails.get("serveraddress");
        }
		return minecraftArguments;
	}
	
	public void game(Process p,boolean crashDetectorEnabled, boolean hasStopped) throws IOException {
		
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        
        String resultLine = in.readLine();  
        
        while (resultLine != null) 
        {             	
        	LoggerUtils.println(resultLine);
        	//DÃ©tecteur de crash
        	if (crashDetectorEnabled && resultLine.contains("Stopping!"))
        	{
        		hasStopped = true;
        	}
            resultLine = in.readLine();
        }
        
        //DÃ©tecteur de crash               
        if (crashDetectorEnabled && !hasStopped)
        {
        	LoggerUtils.println("### Le jeu a crashé ! ###");
        	int dialogResult = JOptionPane.showOptionDialog(null, "On dirait que le jeu a crashé.\nSi vous voulez de l'aide pour résoudre ce souci, allez faire un tour sur le forum.\nOn vous y demandera les logs du launcher ; voulez-vous les enregistrer ?", "Oups !", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new Object[] {"Oui", "Non"}, "Oui");
        	if (dialogResult == 0)
        	{
        		LoggerUtils.openSavePrompt(launcherId, hasPaid, identifier);
        	}
        }
                 
		//Fermeture
        gameLaunchedListener.onGameClosed();
		
	}
	
	
	
	
	public GameLauncher launchGame()
	{
		new Worker().start();	
		return this;
	}
	
	class Worker extends Thread
	{
		boolean isLogin = false;;
		
		
		public Gson credentials(LoginResult result) throws JsonSyntaxException, FileNotFoundException, IOException {
			
			Credentials credentials = new Credentials();
			credentials.username = account.username;
			if (result.selectedProfile != null && result.accessToken != null && result.clientToken != null)
			{
				credentials.accesstoken = result.accessToken;
				credentials.clienttoken = result.clientToken;
				credentials.profilename = result.selectedProfile.name;
				credentials.profileid = result.selectedProfile.id;
			}
			Util.setStoredCredentials(launcherId, credentials, hasPaid, identifier);
			
			Gson gson = new Gson();
			
			isLogin = false;
			//Telechargement de la liste des versions
			stateListener.onStateChanged(false, "Téléchargement de la liste des versions...");
			
			VersionsList versionsList = gson.fromJson(Util.doGET(Util.getOnlineVersionsListUrl(), null), VersionsList.class);
			
			//Vï¿½rification de la version
			stateListener.onStateChanged(false, "Détermination de la version à télécharger...");
			
			//On regarde si y'en a pas et auquel cas on met la derniï¿½re version release
			if (!launcherDetails.containsKey("gameversion"))
			{
				launcherDetails.put("gameversion", versionsList.latest.get("release"));
			}
			
			LoggerUtils.println("Version " + launcherDetails.get("gameversion"));
			
			return gson;
			
		}
		
		@Override
		public void run()
		{
			try
			{			
				//Config
				config = Util.getConfiguration(hasPaid, identifier);
				
				//Connexion au compte
				isLogin = true;
				stateListener.onStateChanged(false, "Connexion au compte...");
				
				LoginResult result = new LoginResult();
				
				try
				{
					result = account.doLogin();
				}
				catch (LoginException ex)
				{
					//On regarde si ï¿½a accepte les versions crackï¿½es ET SI y'a pas de mot de passe
					if (!Util.isCrackedAllowed(launcherDetails) || (Util.isCrackedAllowed(launcherDetails) && !account.password.equals("")))
					{
						throw new Exception(ex.getLocalizedMessage());
					}
				}
				//Gson gson = credentials(result);
				//SetStoredCredentials
				Credentials credentials = new Credentials();
				credentials.username = account.username;
				if (result.selectedProfile != null && result.accessToken != null && result.clientToken != null)
				{
					credentials.accesstoken = result.accessToken;
					credentials.clienttoken = result.clientToken;
					credentials.profilename = result.selectedProfile.name;
					credentials.profileid = result.selectedProfile.id;
				}
				Util.setStoredCredentials(launcherId, credentials, hasPaid, identifier);
				
				Gson gson = new Gson();
				
				isLogin = false;
				//Telechargement de la liste des versions
				stateListener.onStateChanged(false, "Téléchargement de la liste des versions...");
				
				VersionsList versionsList = gson.fromJson(Util.doGET(Util.getOnlineVersionsListUrl(), null), VersionsList.class);
				
				//Vï¿½rification de la version
				stateListener.onStateChanged(false, "Détermination de la version à télécharger...");
				
				//On regarde si y'en a pas et auquel cas on met la derniï¿½re version release
				if (!launcherDetails.containsKey("gameversion"))
				{
					launcherDetails.put("gameversion", versionsList.latest.get("release"));
				}
				
				LoggerUtils.println("Version " + launcherDetails.get("gameversion"));
				
				//Mise a jour du JAR
				//majJAR(gson);
				stateListener.onStateChanged(false, "Calcul des différences...");
				
				if (!Util.getJarFile(launcherDetails, launcherId, hasPaid, identifier).exists() || !Util.getJsonFile(launcherDetails, launcherId, hasPaid, identifier).exists() || forceUpdate || !Util.isRevisionUpToDate(Util.isCustomJarEnabled(launcherDetails), Util.getJarRevisionFile(launcherDetails, launcherId, hasPaid, identifier), Long.parseLong(launcherDetails.get("customjarrevision")), false))
				{
					stateListener.onStateChanged(false, "Récupération des informations de la version...");
					
					VersionDetails details = gson.fromJson(Util.doGET(Util.getOnlineJsonUrl(launcherDetails.get("gameversion")), null), VersionDetails.class);
					
					//Mise a jour du jeu
					stateListener.onStateChanged(false, "Mise à  jour du jeu...");
					
					Util.getVersionFolder(launcherId, launcherDetails, hasPaid, identifier).mkdirs();
					
					HTTPDownloader downloader = new HTTPDownloader(Util.getOnlineJarFile(launcherDetails), Util.getJarFile(launcherDetails, launcherId, hasPaid, identifier));
					downloader.setDownloadProgressionListener(progressListener);
					downloader.downloadFile();
					
					//Sauvegarde du JSON
					stateListener.onStateChanged(false, "Enregistrement des paramètres de la version...");
					if (!Util.getJsonFile(launcherDetails, launcherId, hasPaid, identifier).exists())
					{
						Util.getJsonFile(launcherDetails, launcherId, hasPaid, identifier).getParentFile().mkdirs();
						Util.getJsonFile(launcherDetails, launcherId, hasPaid, identifier).createNewFile();
					}
					Util.writeToFile(Util.getJsonFile(launcherDetails, launcherId, hasPaid, identifier), gson.toJson(details, VersionDetails.class));
					
					//On met a jour la revision
					if (Util.isCustomJarEnabled(launcherDetails))
					{
						Util.writeToFile(Util.getJarRevisionFile(launcherDetails, launcherId, hasPaid, identifier), launcherDetails.get("customjarrevision"));
					}
				}
				
				
				
				FileEndedListener endedListener = new FileEndedListener(){
					@Override
					public void onFileEnded(int fileIndex, int fileCount) {
						progressListener.onFileProgressChanged(fileIndex, fileCount);			
					}
                	
                };
                
                VersionDetails versionDetails = gson.fromJson(Util.getFileContent(new FileInputStream(Util.getJsonFile(launcherDetails, launcherId, hasPaid, identifier)), true, true), VersionDetails.class);
                
                //Ressouces
                ResourcesUpdater.updateResources(versionDetails, progressListener, endedListener, stateListener, launcherDetails, forceUpdate, launcherId, hasPaid, identifier);            
                
                //Refresh des dÃ©tails aprÃ¨s ressources perso
                versionDetails = gson.fromJson(Util.getFileContent(new FileInputStream(Util.getJsonFile(launcherDetails, launcherId, hasPaid, identifier)), true, true), VersionDetails.class);
                
                stateListener.onStateChanged(false, "Vérification des bibliothèques...");
                
                //Bibliothï¿½ques
                
                
                versionDetails = versionDetails.resolveDependencies(launcherDetails, launcherId, hasPaid, identifier);
                //On résoud les dépendances
                //dependencies(versionDetails, endedListener);
                ArrayList<Downloadable> libList = new ArrayList<>();
                LoggerUtils.println(versionDetails.libraries.size() + " bibliothèques à  vérifier...");
                for (int i = 0; i < versionDetails.libraries.size(); i++)
                {
                    Library lib = versionDetails.libraries.get(i);
                    
                    String classifier = null;
                    
                    if (lib.getNatives() != null && lib.getNatives().get(OperatingSystem.getCurrentPlatform().getName()) != null)
                    {
                        classifier = lib.getNatives().get(OperatingSystem.getCurrentPlatform().getName());
                    }    
                    
                    File libFile = new File(Util.getLibrariesFolder(launcherDetails, launcherId, hasPaid, identifier) + "/" + lib.getArtifactPath(classifier));
                    
                    if ((!libFile.exists() || forceUpdate))
                    {
                    	stateListener.onStateChanged(false, "Mise à jour des bibliothèques...");
                        libFile.getParentFile().mkdirs();
                        libList.add(new Downloadable(Util.getLibrariesUrl() +  lib.getArtifactPath(classifier), libFile));
                    }
                }
                
                LoggerUtils.println(libList.size() + " bibliothèques à mettre à jour...");
                
                DownloadJob libJob = new DownloadJob(libList);
                libJob.setNewFileListener(endedListener);
                libJob.setLibraryMode(true);
                if (!libJob.downloadAll())
                {
                    throw new Exception("impossible de vérifier les bibliothèques");                   
                   
                }
                
                //Lancement du jeu
                stateListener.onStateChanged(false, "Exécution du jeu...");
                
                String javaDir = (config.javaPath.containsKey(launcherId)) ? config.javaPath.get(launcherId) : OperatingSystem.getCurrentPlatform().getJavaDir();
                JavaProcessLauncher processLauncher = new JavaProcessLauncher(javaDir, Util.getGameDirectory(launcherDetails, launcherId, hasPaid, identifier));
                
                LoggerUtils.println("Chemin vers Java : " + javaDir);
                
                //Arguments                      
                //Truc zarb de l'OS
                //paramOS(processLauncher);
                final OperatingSystem os = OperatingSystem.getCurrentPlatform();
                if(os.equals(OperatingSystem.OSX))
                {
                    processLauncher.addCommand(new String[] {"-Xdock:icon=" + new File(Util.getResourcesFolder(launcherDetails, launcherId, hasPaid, identifier), "icons/minecraft.icns").getAbsolutePath() + " -Xdock:name=" + "Minecraft" });
                }
                else if(os.equals(OperatingSystem.WINDOWS))
                {
                    processLauncher.addCommand(new String[] {"-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump" });

                }
                
                //32-64bits
                final boolean is32Bit = "32".equals(System.getProperty("sun.arch.data.model"));
                final String defaultArgument = is32Bit ? "-Xmx512M" : "-Xmx1G";
                processLauncher.addSplitCommand(defaultArgument);  
                
                //GC Shit
                processLauncher.addCommand(new String[] {"-XX:+UseConcMarkSweepGC", "-XX:+CMSIncrementalMode", "-XX:-UseAdaptiveSizePolicy"});
                
                //Arguments personnalise
                if (launcherDetails.containsKey("arguments"))          
                {
                	LoggerUtils.println("Ajout des arguments personnalisés : " + launcherDetails.get("arguments"));
                	processLauncher.addSplitCommand(launcherDetails.get("arguments"));
                }
           
                //ClassPath
                
                //classpaff(processLauncher, versionDetails);
                SeparatorStringBuilder classPath = new SeparatorStringBuilder();
                
                classPath.append(Util.getJarFile(launcherDetails, launcherId, hasPaid, identifier).getAbsolutePath());
                
                for (Library lib : versionDetails.libraries)
                {               
                    String classifier = null;

                    if (lib.getNatives() != null && lib.getNatives().get(OperatingSystem.getCurrentPlatform().getName()) != null) {
                        classifier = lib.getNatives().get(OperatingSystem.getCurrentPlatform().getName());
                    }

                    classPath.append(new File(Util.getLibrariesFolder(launcherDetails, launcherId, hasPaid, identifier).getAbsolutePath() + "/" + lib.getArtifactPath(classifier)).getAbsolutePath());
                    
                }
                processLauncher.addCommand(new String[] {"-cp", "\"" + classPath.toString() + "\""});
               
                
                // Library Path
                //libraryPath(processLauncher,versionDetails);
                 
                //Library Path
                File nativeDir = Util.getNativesDir(launcherDetails, launcherId, hasPaid, identifier);
                //On supprime le dossier des natives
                nativeDir.delete();
                //On le refait
                nativeDir.mkdirs();
                
                //Et on les unpack
                for (Library lib : versionDetails.libraries)
                {                  
                    if (lib.getNatives() != null && lib.getNatives().get(OperatingSystem.getCurrentPlatform().getName()) != null)
                    {
                        try
                        {
                            String classifier = lib.getNatives().get(OperatingSystem.getCurrentPlatform().getName());
                            File file = new File(Util.getLibrariesFolder(launcherDetails, launcherId, hasPaid, identifier) + "/" + lib.getArtifactPath(classifier));                     
                       
                            LoggerUtils.println("Extraction de la librairie native : " + file.getAbsolutePath() + " dans " + nativeDir.getAbsolutePath());
                            Util.unpackArchive(file, nativeDir);
                        }
                        catch (Exception ex)
                        {}                      
                    }                      
                }
                processLauncher.addCommand(new String[] {"-Djava.library.path=" + nativeDir.getAbsolutePath()});

                //Main Class
                processLauncher.addCommand(new String[] {versionDetails.mainClass});
                
                //Arguments pour le jeu
                String minecraftArguments = versionDetails.minecraftArguments;
                
                //Auth
                
                //minecraftArguments = auth(minecraftArguments,result);
                
                if (result.loggedIn)
                {
                	LoggerUtils.println("Authentifié ; ajout des informations...");
                    //Username
                    minecraftArguments = minecraftArguments.replace("${auth_player_name}", result.selectedProfile.name);
                    
                    //AccessToken
                    minecraftArguments = minecraftArguments.replace("${auth_access_token}", result.accessToken);
                    
                    //Session
                    minecraftArguments = minecraftArguments.replace("${auth_session}", "token:" + result.accessToken + ":" + result.selectedProfile.id);
                    
                    //UUID
                    minecraftArguments = minecraftArguments.replace("${auth_uuid}", result.selectedProfile.id);
				}
                else
                {          
                	LoggerUtils.println("Non authentifié ; ajout des informations par défaut...");
                    minecraftArguments = minecraftArguments.replace("${auth_player_name}", account.username);
                    minecraftArguments = minecraftArguments.replace("${auth_uuid}", new UUID(0L, 0L).toString());
                }
                
                //Version
                minecraftArguments = minecraftArguments.replace("${version_name}", launcherDetails.get("gameversion"));
                
                //GameDir
                minecraftArguments = minecraftArguments.replace("${game_directory}", "\"" + Util.getGameDirectory(launcherDetails, launcherId, hasPaid, identifier).getAbsolutePath() + "\"");
                
                //AssetsDir
                minecraftArguments = minecraftArguments.replace("${assets_root}", "\"" + Util.getAssetsRoot(Util.getAssetsIndex(versionDetails), launcherDetails, launcherId, hasPaid, identifier) + "\"");
                minecraftArguments = minecraftArguments.replace("${game_assets}", "\"" + new File(Util.getResourcesFolder(launcherDetails, launcherId, hasPaid, identifier), "/" + "virtual" + "/" + "legacy").getAbsolutePath() + "\"");
                
                //AssetsIndex
                minecraftArguments = minecraftArguments.replace("${assets_index_name}", Util.getAssetsIndex(versionDetails));
                
                //UserProperties
                minecraftArguments = minecraftArguments.replace("${user_properties}", new Gson().toJson(new HashMap<String, Collection<String>>()));
                
                //UserType
                minecraftArguments = minecraftArguments.replace("${user_type}", "legacy");
                              
                //Le serveur
                
                //minecraftArguments = server(minecraftArguments);
                
                if (launcherDetails.containsKey("enableautoconnection") && launcherDetails.get("enableautoconnection").equals("1"))
                {
                	LoggerUtils.println("Connexion automatique activée");
                	String port = "25565";
                    if (launcherDetails.get("serveraddress").contains(":"))
                    {
                        port = launcherDetails.get("serveraddress").substring(launcherDetails.get("serveraddress").lastIndexOf(":"));  
                        launcherDetails.put("serveraddress", launcherDetails.get("serveraddress").replace(port, ""));
                        port = port.replace(":", "");
                    }
                    
                    minecraftArguments += " --port " + port + " --server " + launcherDetails.get("serveraddress");
                }     
                
                //La taille de la fenetre
                //minecraftArguments = fenetre(minecraftArguments);
                if (launcherDetails.containsKey("width"))
                {
                	LoggerUtils.println("Ajout de la largeur personnalisée : " + launcherDetails.get("width"));
                	minecraftArguments += " --width " + launcherDetails.get("width");
                }
                else
                {
                	LoggerUtils.println("Largeur par défaut : " + 854);
                	minecraftArguments += " --width " + 854;
                }
                
                if (launcherDetails.containsKey("height"))
                {
                	LoggerUtils.println("Ajout de la hauteur personnalisée : " + launcherDetails.get("height"));
                	minecraftArguments += " --height " + launcherDetails.get("height");
                }
                else
                {
                	LoggerUtils.println("Hauteur par défaut : " + 480);
                	minecraftArguments += " --height " + 480;
                }
                
                //Plein ï¿½cran
                if (launcherDetails.containsKey("fullscreen") && launcherDetails.get("fullscreen").equals("1"))
                {
                	LoggerUtils.println("Plein écran activé");
                	minecraftArguments += " --fullscreen";
                }
                       
                //Ajout de tout ï¿½a
                processLauncher.addSplitCommand(minecraftArguments);
             
                           
                //Exï¿½cution
                if (launcherDetails.containsKey("debug") && launcherDetails.get("debug").equals("1"))
                {       
                    LoggerUtils.println("Exécution de la ligne de commande :");
                    LoggerUtils.println(processLauncher.getCommandLine());
                }
                
                //DÃ©tecteur de crash
                boolean hasStopped = false;
                boolean crashDetectorEnabled = launcherDetails.containsKey("crashdetectorenabled");
                
                LoggerUtils.println("\n--- LOGS DU JEU  ---\n");
                Process p = processLauncher.start();                           
                
                //On cache la fenï¿½tre
                gameLaunchedListener.onGameLaunched();
                
                //Le jeu
                //game(p, crashDetectorEnabled, hasStopped);
                BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                
                String resultLine = in.readLine();  
                
                while (resultLine != null) 
                {             	
                	LoggerUtils.println(resultLine);
                	//DÃ©tecteur de crash
                	if (crashDetectorEnabled && resultLine.contains("Stopping!"))
                	{
                		hasStopped = true;
                	}
                    resultLine = in.readLine();
                }
                
                //DÃ©tecteur de crash               
                if (crashDetectorEnabled && !hasStopped)
                {
                	LoggerUtils.println("### Le jeu a crashé ! ###");
                	int dialogResult = JOptionPane.showOptionDialog(null, "On dirait que le jeu a crashé.\nSi vous voulez de l'aide pour résoudre ce souci, allez faire un tour sur le forum.\nOn vous y demandera les logs du launcher ; voulez-vous les enregistrer ?", "Oups !", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new Object[] {"Oui", "Non"}, "Oui");
                	if (dialogResult == 0)
                	{
                		LoggerUtils.openSavePrompt(launcherId, hasPaid, identifier);
                	}
                }
                         
				//Fermeture
                gameLaunchedListener.onGameClosed();
                
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				stateListener.onStateChanged(true, ex.getLocalizedMessage(), isLogin);
			}
		}
	}
}
