package fr.launchmycraft.library.resourcing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import fr.launchmycraft.launcher.LoggerUtils;
import fr.launchmycraft.library.util.Util;
import fr.launchmycraft.library.util.listeners.FileEndedListener;
import fr.launchmycraft.library.util.listeners.FileProgressionListener;
import fr.launchmycraft.library.util.listeners.StateListener;
import fr.launchmycraft.library.util.network.DownloadJob;
import fr.launchmycraft.library.util.network.Downloadable;
import fr.launchmycraft.library.util.network.HTTPDownloader;
import fr.launchmycraft.library.versionning.VersionDetails;

public class ResourcesUpdater {
	
	static String createUrl(AssetObject object)
	{
		return Util.getResourcesUrl() + object.hash.substring(0, 2) + "/" + object.hash;
	}
	
	public static void legacyOrNot(String assetsIndex, ArrayList<Resoursable> filesToCheck, AssetIndex index, HashMap<String, String> launcherDetails, long launcherId, boolean hasPaid, String identifier) throws JsonSyntaxException, IOException {
		
		if (assetsIndex.equals("legacy"))
		{
			LoggerUtils.println("Mode \"legacy\" utilisé.");
			//C'est legacy
			for (String key : index.objects.keySet())
			{
				AssetObject object = index.objects.get(key);
				
				//C'est legacy donc le fichier en question c'est la key
				File objectFile = new File(Util.getResourcesFolder(launcherDetails, launcherId, hasPaid, identifier), "/" + "virtual" + "/" + "legacy" + "/" + key);
				
				//On l'ajoute au job
				//On prend l'URL du fichier
				Resoursable able = new Resoursable(object.hash, createUrl(object), objectFile);
				filesToCheck.add(able);
			}
			
		}
		else
		{
			LoggerUtils.println("Mode normal utilisé.");
			//C'est pas legacy
			for (String key : index.objects.keySet())
			{
				AssetObject object = index.objects.get(key);				
				
				File objectFile = new File(Util.getResourcesFolder(launcherDetails, launcherId, hasPaid, identifier), "/" + "objects" + "/" + object.hash.substring(0, 2) + "/" + object.hash);
				
				//On l'ajoute au job
				//On prend l'URL du fichier
				Resoursable able = new Resoursable(object.hash, createUrl(object), objectFile);
				filesToCheck.add(able);
			}
		}
		
	}
	
	public static void verifAndDl(ArrayList<Resoursable> filesToCheck,boolean forceUpdate, ArrayList<Downloadable> filesToDownload, FileEndedListener endedListener,  final StateListener stateListener) throws InterruptedException, Exception {
		
		ResourceJob hashJob = new ResourceJob(filesToCheck, forceUpdate);
		if (!hashJob.checkAll())
		{
			throw new Exception("impossible de vérifier les ressources");
		}
					
		for (Resoursable file : hashJob.getFilesToDownload())
		{
			filesToDownload.add(new Downloadable(file.url, file.fileToCheck));
		}
		
		LoggerUtils.println(filesToDownload.size() + " fichiers à mettre à jour.");
		
		
		//On telecharge tout ca			
		stateListener.onStateChanged(false, "Mise à jour des ressources...");	
		
		DownloadJob downJob = new DownloadJob(filesToDownload);
		downJob.setNewFileListener(endedListener);
		
		if (!downJob.downloadAll())
		{
			throw new Exception("impossible de télécharger les ressources");
		}
		
	}
	
	
	public static void updateResources(VersionDetails versionDetails, FileProgressionListener progressListener, FileEndedListener endedListener,  final StateListener stateListener, HashMap<String, String> launcherDetails, boolean forceUpdate, long launcherId, boolean hasPaid, String identifier) throws Exception
	{
		//Les ressources
        boolean resUp2Date = Util.isRevisionUpToDate(Util.isCustomResourcesEnabled(launcherDetails), Util.getResourcesRevisionFile(launcherDetails, launcherId, hasPaid, identifier), Long.parseLong(launcherDetails.get("customresourcesrevision")), true);

        
		if (!resUp2Date || forceUpdate)
		{
			stateListener.onStateChanged(false, "Vérification des ressources...");		
			String assetsIndex = Util.getAssetsIndex(versionDetails);
			
			//On tï¿½lï¿½charge le JSON si il existe pas
			File indexFile = new File(Util.getResourcesFolder(launcherDetails, launcherId, hasPaid, identifier), "/" + "indexes" + "/" + assetsIndex + ".json");
			if (!indexFile.exists())
			{
				indexFile.getParentFile().mkdirs();
				//Il existe pas, on le tï¿½lï¿½charge
				Util.writeToFile(indexFile, Util.doGET(Util.getIndexUrl(assetsIndex), null));
			}
			//On ï¿½tablit la liste des fichiers ï¿½ tï¿½lï¿½charger
			ArrayList<Downloadable> filesToDownload = new ArrayList<>();
			ArrayList<Resoursable> filesToCheck = new ArrayList<>();
			//On lit le JSON
			AssetIndex index = new Gson().fromJson(Util.getFileContent(new FileInputStream(indexFile), false, true), AssetIndex.class);
			//On regarde si c'est legacy
			
			legacyOrNot(assetsIndex, filesToCheck, index, launcherDetails, launcherId, resUp2Date, assetsIndex);
			
			/*if (assetsIndex.equals("legacy"))
			{
				LoggerUtils.println("Mode \"legacy\" utilisé.");
				//C'est legacy
				for (String key : index.objects.keySet())
				{
					AssetObject object = index.objects.get(key);
					
					//C'est legacy donc le fichier en question c'est la key
					File objectFile = new File(Util.getResourcesFolder(launcherDetails, launcherId, hasPaid, identifier), "/" + "virtual" + "/" + "legacy" + "/" + key);
					
					//On l'ajoute au job
					//On prend l'URL du fichier
					Resoursable able = new Resoursable(object.hash, createUrl(object), objectFile);
					filesToCheck.add(able);
				}
				
			}
			else
			{
				LoggerUtils.println("Mode normal utilisé.");
				//C'est pas legacy
				for (String key : index.objects.keySet())
				{
					AssetObject object = index.objects.get(key);				
					
					File objectFile = new File(Util.getResourcesFolder(launcherDetails, launcherId, hasPaid, identifier), "/" + "objects" + "/" + object.hash.substring(0, 2) + "/" + object.hash);
					
					//On l'ajoute au job
					//On prend l'URL du fichier
					Resoursable able = new Resoursable(object.hash, createUrl(object), objectFile);
					filesToCheck.add(able);
				}
			}*/
					
			//On vï¿½rifie tout ï¿½a
			/*ResourceJob hashJob = new ResourceJob(filesToCheck, forceUpdate);
			if (!hashJob.checkAll())
			{
				throw new Exception("impossible de vérifier les ressources");
			}
						
			for (Resoursable file : hashJob.getFilesToDownload())
			{
				filesToDownload.add(new Downloadable(file.url, file.fileToCheck));
			}
			
			LoggerUtils.println(filesToDownload.size() + " fichiers à mettre à jour.");
			
			
			//On telecharge tout ca			
			stateListener.onStateChanged(false, "Mise à jour des ressources...");	
			
			DownloadJob downJob = new DownloadJob(filesToDownload);
			downJob.setNewFileListener(endedListener);
			
			if (!downJob.downloadAll())
			{
				throw new Exception("impossible de télécharger les ressources");
			}*/
			
            verifAndDl(filesToCheck, forceUpdate, filesToDownload, endedListener, stateListener);
            //Custom Resources					
            if (Util.isCustomResourcesEnabled(launcherDetails) && (!resUp2Date || forceUpdate))
            {
            	stateListener.onStateChanged(false, "Mise à jour des ressources personnalisées...");
            	
            	//On supprime le fichier si il existe
            	if (Util.getCustomResourcesZipFile(launcherDetails, launcherId, hasPaid, identifier).exists())
                {
            		LoggerUtils.println("Suppression de l'ancienne archive...");
                    Util.getCustomResourcesZipFile(launcherDetails, launcherId, hasPaid, identifier).delete();
                }
            	
            	LoggerUtils.println("Téléchargement de " + launcherDetails.get("customresourcesurl"));
            	HTTPDownloader downloader = new HTTPDownloader(launcherDetails.get("customresourcesurl"), Util.getCustomResourcesZipFile(launcherDetails, launcherId, hasPaid, identifier));
            	downloader.setDownloadProgressionListener(progressListener);
            	downloader.downloadFile();
            	
            	stateListener.onStateChanged(false, "Installation des ressources personnalisées...");
            	Util.unpackArchive(Util.getCustomResourcesZipFile(launcherDetails, launcherId, hasPaid, identifier), Util.getGameDirectory(launcherDetails, launcherId, hasPaid, identifier));
                LoggerUtils.println("Nettoyage...");
            	Util.getCustomResourcesZipFile(launcherDetails, launcherId, hasPaid, identifier).delete();
            	
            	//On incrï¿½mente la rï¿½vision
            	Util.writeToFile(Util.getResourcesRevisionFile(launcherDetails, launcherId, hasPaid, identifier), launcherDetails.get("customresourcesrevision"));
            }   
		}			
	}

}
