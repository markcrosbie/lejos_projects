//Include the Dropbox SDK.
import com.dropbox.core.*;
import java.io.*;
import java.util.Locale;

public class PutFile {
	
	public static String accessTokenFile = "token.txt";
	public static String accessToken;
	
 public static void main(String[] args) throws IOException, DbxException {
     // Get your app key and secret from the Dropbox developers website.
     final String APP_KEY = "pyt64c3vt3qittd";
     final String APP_SECRET = "5v814ptyyq1k1sa";

     DbxAppInfo appInfo = new DbxAppInfo(APP_KEY, APP_SECRET);

     DbxRequestConfig config = new DbxRequestConfig("JavaTutorial/1.0",
         Locale.getDefault().toString());
     DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(config, appInfo);

     // No access token - let's get one from the user
      
     DbxClient client = new DbxClient(config, accessToken);

     System.out.println("Linked account: " + client.getAccountInfo().displayName);

     File inputFile = new File("snapshot_0.yuyv");
     FileInputStream inputStream = new FileInputStream(inputFile);
     try {
         DbxEntry.File uploadedFile = client.uploadFile("/snapshot_0.yuyv",
             DbxWriteMode.add(), inputFile.length(), inputStream);
         System.out.println("Uploaded: " + uploadedFile.toString());
     } finally {
         inputStream.close();
     }

     DbxEntry.WithChildren listing = client.getMetadataWithChildren("/");
     System.out.println("Files in the root path:");
     for (DbxEntry child : listing.children) {
         System.out.println("	" + child.name + ": " + child.toString());
     }

     FileOutputStream outputStream = new FileOutputStream("snapshot_0.yuyv");
     try {
         DbxEntry.File downloadedFile = client.getFile("/magnum-opus.txt", null,
             outputStream);
         System.out.println("Metadata: " + downloadedFile.toString());
     } finally {
         outputStream.close();
     }
 }
}