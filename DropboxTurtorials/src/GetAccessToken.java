
import com.dropbox.core.*;
import com.dropbox.core.json.JsonReader;

import java.io.*;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import lejos.hardware.lcd.LCD;

/**
 * An example command-line application that runs through the web-based OAuth
 * flow (using {@link DbxWebAuth}).
 * 
 * By Mark Crosbie http://thinkbricks.net
 * Modified from the Dropbox tutorial code
 */
public class GetAccessToken {
	
	// The access token is saved in this file on the EV3 SDcard
	public static final String accessTokenFilename = "./access-token.json";
	
    public static void main(String[] args) throws IOException {
    	
    	LCD.clear();
    	
    	System.out.println("-- Get Dropbox auth token --");
    	System.out.println("1. Log into Dropbox and go to the App Console in your account");
    	System.out.println("2. Click on the app you created for the EV3");
    	System.out.print("3. Enter the App key value here: ");
        String appKey = new BufferedReader(new InputStreamReader(System.in)).readLine();
        if (appKey == null) {
            System.exit(1); return;
        }
        appKey = appKey.trim();
        
        System.out.println("");
        System.out.print("4. Enter the App secret value here: ");
        String appSecret = new BufferedReader(new InputStreamReader(System.in)).readLine();
        if (appSecret == null) {
            System.exit(1); return;
        }
        appSecret = appSecret.trim();
        System.out.println("");
        
    	System.out.println("5. Starting Dropbox auth flow");
    	DbxAppInfo appInfo = new DbxAppInfo(appKey, appSecret);

        String userLocale = Locale.getDefault().toString();
        DbxRequestConfig config = new DbxRequestConfig("JavaTutorial/1.0", userLocale);
        DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(config, appInfo);

        // Run through Dropbox API authorization process
        String authorizeUrl = webAuth.start();
        System.out.println("6. Go to " + authorizeUrl);
        System.out.println("7. Click \"Allow\" (you might have to log in first).");
        System.out.println("8. Copy the authorization code.");
        System.out.print("9. Enter the authorization code here: ");

        String code = new BufferedReader(new InputStreamReader(System.in)).readLine();
        if (code == null) {
            System.exit(1); return;
        }
        code = code.trim();

        DbxAuthFinish authFinish;
        try {
            authFinish = webAuth.finish(code);
        }
        catch (DbxException ex) {
            System.err.println("Error in DbxWebAuth.start: " + ex.getMessage());
            System.exit(1); return;
        }

        System.out.println("10. Authorization complete.");
        System.out.println("- User ID: " + authFinish.userId);
        System.out.println("- Access Token: " + authFinish.accessToken);

        // Save auth information to output file.
        DbxAuthInfo authInfo = new DbxAuthInfo(authFinish.accessToken, appInfo.host);
        try {
            DbxAuthInfo.Writer.writeToFile(authInfo, accessTokenFilename);
            System.out.println("Saved authorization information to \"" + accessTokenFilename + "\".");
        }
        catch (IOException ex) {
            System.err.println("Error saving to <auth-file-out>: " + ex.getMessage());
            System.err.println("Dumping to stderr instead:");
            DbxAuthInfo.Writer.writeToStream(authInfo, System.err);
            System.exit(1); return;
        }
    }
}