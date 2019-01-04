package fr.launchmycraft.library.util;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.regex.Pattern;

import javax.activation.MimetypesFileTypeMap;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import fr.launchmycraft.launcher.LoggerUtils;
import fr.launchmycraft.launcher.OperatingSystem;
import fr.launchmycraft.library.Configuration;
import fr.launchmycraft.library.authentication.Credentials;
import fr.launchmycraft.library.authentication.RefreshRequest;
import fr.launchmycraft.library.authentication.RefreshResult;
import fr.launchmycraft.library.versionning.VersionDetails;

@SuppressWarnings("deprecation")
public class Util {
	
	public static String getLauncherGetUrl(long id)
	{
		return "https://launchmycraft.fr/api/get/" + id;
	}
	
	public static String getLastBootstrapVersionUrl()
	{
		return "https://launchmycraft.fr/getlauncher/lastbootstrap";
	}
	
	@SuppressWarnings({ "resource" })
	public static String doGET(String u, String type) throws IOException
    {
        HttpClient httpClient = new DefaultHttpClient();          
        HttpGet request = new HttpGet(u);
      
        request.addHeader("content-type", type);
        
        HttpResponse response = httpClient.execute(request);
        
        HttpEntity entity = response.getEntity();
        return EntityUtils.toString(entity, "UTF-8"); 
    }
	
	public static int getLatestCGURevision() throws NumberFormatException, IOException
	{
		return Integer.parseInt(doGET("https://launchmycraft.fr/getlauncher/latestcgu", null));
	}
    
    @SuppressWarnings("resource")
	public static String getHash(final File file, String hash) throws FileNotFoundException, IOException, NoSuchAlgorithmException {
        DigestInputStream stream = null;
        
            stream = new DigestInputStream(new FileInputStream(file), MessageDigest.getInstance(hash));
            final byte[] buffer = new byte[65536];

            int read = stream.read(buffer);
            while(read >= 1)
                read = stream.read(buffer);
        return String.format("%1$032x", new Object[] { new BigInteger(1, stream.getMessageDigest().digest()) });
    }
	
	public static String getDefaultLogoUrl()
	{
		return "https://launchmycraft.fr/assets/launcher/logo.png";
	}
	
	public static File getLogFile(boolean hasPaid, String identifier) throws JsonSyntaxException, IOException
	{
		return new File(OperatingSystem.getBaseWorkingDirectory(hasPaid, identifier), "/" + "last.log");
	}
	
	public static Image getThemeImage(String id) throws IOException
	{
		URL url = new URL("https://launchmycraft.fr/assets/themes/" + id + ".png");
		BufferedImage bimg = ImageIO.read(url);
		Image img = bimg;
		
		return img;
	}
	
	public static File getBootstrapFile() throws URISyntaxException, ClassNotFoundException
	{
	
		try
		{
			Class<?> bootstrapClass = Class.forName("fr.launchmycraft.executable.bootstrap.BootstrapCore");
			return new File(bootstrapClass.getProtectionDomain().getCodeSource().getLocation().toURI());
		}
		catch (Exception ex)
		{
			Class<?> bootstrapClass = Class.forName("fr.launchmycraft.launcher.bootstrap.BootstrapCore");
			return new File(bootstrapClass.getProtectionDomain().getCodeSource().getLocation().toURI());
		}	
	}
	

	public static String getAvatarUrl(String name, long size)
	{
		return "https://minotar.net/helm/" + name + "/" + size + ".png";
	}
	
	public static String getAssetsRoot(String index, HashMap<String, String> h, long l, boolean hasPaid, String identifier) throws JsonSyntaxException, IOException
	{
		if (index.equals("legacy"))
		{
			return new File(getResourcesFolder(h, l, hasPaid, identifier), "/" + "virtual" + "/" + "legacy").getAbsolutePath();
		}
		else
		{
			return getResourcesFolder(h, l, hasPaid, identifier).getAbsolutePath();
		}
	}
	
	public static String getAuthServerUrl(HashMap<String, String> d)
	{
		String url = "";
		if (d.containsKey("customauthserver"))
		{
			url = (d.get("customauthserver").startsWith("http://") ? d.get("customauthserver") : "http://" + d.get("authserver"));
		}
		else
		{
			url = "https://authserver.mojang.com/";
		}
		
		if (!url.endsWith("/"))
		{
			url += "/";
		}
	
		LoggerUtils.println("Adresse du serveur d'authentification : " + url);
		return url;
	}
	
	
	public static String getRefreshUrl(HashMap<String, String> d)
	{
		return getAuthServerUrl(d) + "refresh";
	}
	
	public static String refreshCredentials(Credentials credentials, HashMap<String, String> c) throws MalformedURLException, IOException
	{
		LoggerUtils.println("### Actualisation de la session...");
		//On construit le payload
		RefreshRequest request = new RefreshRequest();
		request.accessToken = credentials.accesstoken;
		request.clientToken = credentials.clienttoken;
		String requestString = new Gson().toJson(request, RefreshRequest.class);
		
		if (c.containsKey("debug"))
		{
			LoggerUtils.println("Donn�es envoy�es : " + requestString);
		}
		
		//On envoie le tout et on lit la r�ponse
		String response = Util.doPOST(Util.getRefreshUrl(c), requestString, "application/json");
		if (c.containsKey("debug"))
		{
			LoggerUtils.println("R�ponse du serveur : " + response);
		}
		RefreshResult result = new Gson().fromJson(response, RefreshResult.class);
	     
	    if (result.error != null || result.accessToken == null || result.accessToken.equals(""))
	    {
	   	 return null;
	    }
	    else
	    {
	   	 return result.accessToken;
	    }
	}
	
	public static String getContentType(String filename)
    {
        String g = URLConnection.guessContentTypeFromName(filename);
        if( g == null)
        {
            g = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(filename);
        }
        return g;
    }
	
	public static String getAssetsIndex(VersionDetails d)
	{
		if (d.assets != null)
		{
			return d.assets;
		}
		else
		{
			return "legacy";
		}
	}
	
	public static void computeLoginButtonAvaibility(JButton loginButton, JTextField usernameField, JPasswordField passwordField, HashMap<String, String> launcherDetails)
	{
		if (launcherDetails.containsKey("hidepassword"))
		{
			loginButton.setEnabled(!usernameField.getText().isEmpty());
			return;
		}
		
		loginButton.setEnabled(false);
		if (!usernameField.getText().equals(""))
		{
			if (Util.isCrackedAllowed(launcherDetails) || !new String(passwordField.getPassword()).equals(""))
			{
				loginButton.setEnabled(true);
			}
		}
	}
	
	public static File getNativesDir(HashMap<String, String> d, long id, boolean hasPaid, String identifier) throws JsonSyntaxException, IOException
	{
		return new File(getLibrariesFolder(d, id, hasPaid, identifier), "/" + "natives" + "/" + OperatingSystem.getCurrentPlatform().getName().toLowerCase());
	}
	
	public static String getLibrariesUrl()
	{
		return "https://libraries.minecraft.net/";
	}
	
	public static void unpackArchive(File zfile, File outputFolder) throws Exception
    {
		try {
			ZipFile zipFile = new ZipFile(zfile);
			zipFile.extractAll(outputFolder.getAbsolutePath());
		} catch (ZipException e) {
			e.printStackTrace();
			throw new Exception("le fichier ZIP n'en est pas un");
		}	
    }
	
	public static File getLibrariesFolder(HashMap<String, String> d, long id, boolean hasPaid, String identifier) throws JsonSyntaxException, IOException
	{
		return new File(getGameDirectory(d, id, hasPaid, identifier), "/" + "libraries");
	}
	
	public static String getLogoTextUrl(String t)
	{
		t = URLEncoder.encode(t);
		return "https://launchmycraft.fr/tools/logocreator/" + t + "/38";
	}
	
	public static File getVersionFolder(long id, HashMap<String, String> details, boolean hasPaid, String identifier) throws JsonSyntaxException, IOException
	{
		if (!isCustomJarEnabled(details) || hasPaid)
		{
			return new File(getGameDirectory(details, id, hasPaid, identifier), "versions" + "/" + details.get("gameversion"));
		}
		else
		{
			return new File(getGameDirectory(details, id, hasPaid, identifier), "versions" + "/" + getLauncherIdentifier(id));
		}
	}
	
	public static String getLauncherIdentifier(long id)
	{
		return "launcher" + id;
	}
	
	public static boolean isCustomJarEnabled(HashMap<String, String> d)
	{
		return d.containsKey("customjarurl");
	}
	
	public static String getOnlineJsonUrl(String v)
	{
		return "https://s3.amazonaws.com/Minecraft.Download/versions/" + v + "/" + v + ".json";
	}
	
	/*public static String getLibrariesUrl()
	{
		return "https://s3.amazonaws.com/Minecraft.Download/libraries/";
	}*/
	
	public static boolean isCustomResourcesEnabled(HashMap<String, String> details)
	{
		return details.containsKey("customresourcesurl");
	}
	
	public static File getJarFile(HashMap<String, String> details, long id, boolean hasPaid, String identifier) throws JsonSyntaxException, IOException
	{
		if (!isCustomJarEnabled(details) || hasPaid)
		{
			return new File(getVersionFolder(id, details, hasPaid, identifier), "/" + details.get("gameversion") + ".jar");
		}
		else
		{
			return new File(getVersionFolder(id, details, hasPaid, identifier), "/" + getLauncherIdentifier(id) + ".jar");
		}
	} 
	
	public static String getIndexUrl(String index)
	{
		return "https://s3.amazonaws.com/Minecraft.Download/indexes/" + index + ".json";
	}
	
	public static File getResourcesFolder(HashMap<String, String> details, long id, boolean hasPaid, String identifier) throws JsonSyntaxException, IOException
	{
		return new File(getGameDirectory(details, id, hasPaid, identifier), "/" + "assets");
	}
	
	public static String getValidMd5(String etag) {
        if(etag == null)
            etag = "-";
        else if(etag.startsWith("\"") && etag.endsWith("\""))
            etag = etag.substring(1, etag.length() - 1);

        return etag;
    }
	
	public static String getResourcesUrl()
	{
		return "http://resources.download.minecraft.net/";
	}
	
	public static String getOnlineJarFile(HashMap<String, String> d)
	{
		String s = "";
		if (Util.isCustomJarEnabled(d))
		{
			s= d.get("customjarurl");		
		}
		else
		{
			
			s = "https://s3.amazonaws.com/Minecraft.Download/versions/" + d.get("gameversion") + "/" + d.get("gameversion") + ".jar";
		}
		LoggerUtils.println("T�l�chargement de : " + s);
		return s;
	}
	
	public static File getJarRevisionFile(HashMap<String, String> d, long i, boolean hasPaid, String identifier) throws JsonSyntaxException, IOException
	{
		return new File(getJarFile(d, i , hasPaid, identifier).getParentFile(), "revision.txt");
	}
	
	public static File getResourcesRevisionFile(HashMap<String, String> d, long i, boolean hasPaid, String identifier) throws JsonSyntaxException, IOException
	{
		return new File(getGameDirectory(d, i, hasPaid, identifier), "ressourcesRevision.txt");
	}
	
	public static File getCustomResourcesZipFile(HashMap<String, String> d, long i, boolean hasPaid, String identifier) throws JsonSyntaxException, IOException
	{
		return new File(getGameDirectory(d, i, hasPaid, identifier), "customRessources.zip");
	}
	
	public static boolean isRevisionUpToDate(boolean enabledBoolean, File revisionFile, long lastVersion, boolean invert) throws IOException
	{
		if (!enabledBoolean)
		{
			if (invert == false)
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		else
		{
			//On fait le fichier
			if (!revisionFile.exists())
			{
				revisionFile.getParentFile().mkdirs();
				revisionFile.createNewFile();
				Util.writeToFile(revisionFile, "0");
			}
			
            long actualRevision = Long.parseLong(Util.getFileContent(new FileInputStream(revisionFile), false, false));
            
            if (actualRevision == lastVersion)
            {
                return true;
            }
            else
            {
                return false;
            }
		}
	}
	
	public static File getJsonFile(HashMap<String, String> details, long id, boolean hasPaid, String identifier) throws JsonSyntaxException, IOException
	{
		return getJsonFile(details, details.get("gameversion"), id, hasPaid, identifier);
	}
	
	public static File getJsonFile(HashMap<String, String> details, String version, long id, boolean hasPaid, String identifier) throws JsonSyntaxException, IOException
	{
		return new File(getGameDirectory(details, id, hasPaid, identifier), "/" + "versions" + "/" + version + "/" + version + ".json");
	}
	
	public static File getGameDirectory(HashMap<String, String> details, long id, boolean hasPaid, String identifier) throws JsonSyntaxException, IOException
	{
		if (hasPaid)
		{
			return OperatingSystem.getBaseWorkingDirectory(hasPaid, identifier);
		}
		else if (isCustomResourcesEnabled(details))
		{
			return new File(OperatingSystem.getBaseWorkingDirectory(hasPaid, identifier), "/" + "launchers" + "/" + "launcher" + id);
		}
		else
		{
			return new File(OperatingSystem.getBaseWorkingDirectory(hasPaid, identifier), "/" + "common");
		}
	}
	
	public static String getOnlineVersionsListUrl()
	{
		return "http://s3.amazonaws.com/Minecraft.Download/versions/versions.json";
	}
	
	@SuppressWarnings("resource")
	public static String doPOST(String u, String payload, String type) throws MalformedURLException, IOException
    {
        if (payload == null)
        {
            payload = "";
        }
        HttpClient httpClient = new DefaultHttpClient();          
        HttpPost request = new HttpPost(u);
        
        StringEntity params = new StringEntity(payload);
        
        request.addHeader("content-type", type);
        
        request.setEntity(params);
        HttpResponse response = httpClient.execute(request);
        
        HttpEntity entity = response.getEntity();
        return EntityUtils.toString(entity, "UTF-8");
    }
	
	public static String getAuthUrl(HashMap<String, String> d)
	{
		return getAuthServerUrl(d) + "authenticate";
	}
	
	public static String getAuthVersion()
	{
		return "1";
	}
	
	public static Credentials getStoredCredentials(long launcherid, boolean hasPaid, String identifier) throws JsonSyntaxException, FileNotFoundException, IOException
	{
		Configuration configuration = getConfiguration(hasPaid, identifier);	
		if (configuration.storedCredentials.containsKey(launcherid))
		{
			return configuration.storedCredentials.get(launcherid);			
		}
		else
		{
			return null;
		}
	}
	
	public static void setStoredCredentials(long launcherid, Credentials credentials, boolean hasPaid, String identifier) throws JsonSyntaxException, FileNotFoundException, IOException
	{
		Configuration configuration = getConfiguration(hasPaid, identifier);	
		configuration.storedCredentials.put(launcherid, credentials);
		saveConfiguration(configuration, hasPaid, identifier);
	}
	
	public static void saveConfiguration(Configuration configuration, boolean hasPaid, String identifier) throws IOException
	{
		File configurationFile = getConfigurationFile(hasPaid, identifier);
		if (configurationFile.exists())
		{
			configurationFile.delete();
			configurationFile.getParentFile().mkdirs();
			configurationFile.createNewFile();
		}
		Gson gson = new Gson();
		writeToFile(configurationFile, gson.toJson(configuration, Configuration.class));		
	}
	
	public static void writeToFile(File f, String c) throws FileNotFoundException, IOException
    {
		if (!f.exists())
		{
			f.getParentFile().mkdirs();
			f.createNewFile();
		}
        FileOutputStream fop = new FileOutputStream(f);
        if (!f.exists())
        {
            f.mkdirs();
            f.createNewFile();
        }
        
        byte[] contentInBytes = c.getBytes();
 
		fop.write(contentInBytes);
		fop.flush();
		fop.close();    
	
    }
	
	public static Configuration getConfiguration(boolean hasPaid, String identifier) throws JsonSyntaxException, FileNotFoundException, IOException
	{
		if (getConfigurationFile(hasPaid, identifier).exists())
		{
			return new Gson().fromJson(getFileContent(new FileInputStream(getConfigurationFile(hasPaid, identifier)), true, false), Configuration.class);
		}
		else
		{
			return new Configuration();
		}
	}
	
	public static File getConfigurationFile(boolean hasPaid, String identifier) throws JsonSyntaxException, IOException
	{
		return new File(OperatingSystem.getBaseWorkingDirectory(hasPaid, identifier), "config.json");
	}
		
	public static JLabel getLogoJLabel(HashMap<String, String> launcherDetails, boolean resize) throws Exception
	{
		JLabel logo = new JLabel();
		if (launcherDetails.containsKey("serverlogo"))
		{
			Util.setJLabelLogo(logo, launcherDetails.get("serverlogo"), resize);
		}
		else
		{
			if (launcherDetails.containsKey("servername"))
			{
				Util.setJLabelLogo(logo, getLogoTextUrl(launcherDetails.get("servername")), resize);
			}
			else
			{
				Util.setJLabelLogo(logo, Util.getDefaultLogoUrl(), false);
			}
		}			
		
		return logo;
	}
	
	public static String getFileContent(FileInputStream fis, boolean shouldAddLines, boolean replaceArch) throws IOException
    {
        BufferedReader br =new BufferedReader( new InputStreamReader(fis));
        
            StringBuilder sb = new StringBuilder();
            String line;
            while(( line = br.readLine()) != null ) 
            {
                sb.append( line );
                if (shouldAddLines)
                {
                    sb.append( '\n' );
                }
            }
            
        String str = sb.toString();
        if (replaceArch)
        {
        	str = str.replaceAll(Pattern.quote("${arch}"), getArchIdentifier());
        }
        return str;
        
    }
	
	public static String getArchIdentifier()
	{
		String arch = "32";
	    boolean is32Bit = "32".equals(System.getProperty("sun.arch.data.model"));
	    if (!is32Bit)
	    {
	    	arch = "64";
	    }
	    return arch;
	}
	
	public static boolean isCrackedAllowed(HashMap<String, String> parm)
	{
		return (parm.containsKey("allowcracked") && parm.get("allowcracked").equals("1"));
	}
	
	
	public static void setJLabelLogo(JLabel base, String u, boolean shouldEnableResize) throws Exception
	{
		try
		{
			LoggerUtils.println("Chargement de l'image : " + u);
			URL url = new URL(u);
			BufferedImage bimg = ImageIO.read(url);
			Image img = bimg;		
			
			if (bimg.getWidth() > 250 && shouldEnableResize)
			{
				img = bimg.getScaledInstance(250, -1, Image.SCALE_DEFAULT);
			}
			
			if (bimg.getHeight() > 60 && shouldEnableResize)
			{
				img = img.getScaledInstance(250, -1, Image.SCALE_DEFAULT);
			}
			
			base.setIcon(new ImageIcon(img));     
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			LoggerUtils.println("Impossible de charger l'image : " + u);
		}
		  
	}
}
