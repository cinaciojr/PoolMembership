package org.inacio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public abstract class PayPalBaseRequest {

	private static final Logger log = Logger.getLogger(PayPalBaseRequest.class.getName());

	protected ServiceEnvironment env;
	protected RequestEnvelope requestEnvelope;
	public static int HTTP_CONNECTION_TIMEOUT = 3000;
	public static int HTTP_READ_TIMEOUT = 7000;
	public static boolean DISABLE_SSL_CERT_CHECK = false;	
	
	protected String makeRequest(APICredential credentialObj, String apiMethod, String postData) throws Exception {
		String responseString = "";
		try {
			if(DISABLE_SSL_CERT_CHECK){
				// Create a trust manager that does not validate certificate chains 
				TrustManager[] trustAllCerts = new TrustManager[] { 
						new X509TrustManager() { 
							public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; } 
							public void checkClientTrusted( java.security.cert.X509Certificate[] certs, String authType) { } 
							public void checkServerTrusted( java.security.cert.X509Certificate[] certs, String authType) { } 
						} };
				// Install the all-trusting trust manager 

				SSLContext sc = SSLContext.getInstance("TLS");

				sc.init(null, trustAllCerts, new java.security.SecureRandom()); 
				HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory()); 
				HttpsURLConnection.setDefaultHostnameVerifier( new HostnameVerifier(){
					public boolean verify(String string,SSLSession ssls) {
						return true;
					}
				});
			}
			URL url = new URL(EndPointUrl.get(this.env) + apiMethod);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();			
			log.log(Level.INFO,"Connection established: "+url.getHost()+url.getPath());
			connection.setDoOutput(true);
			// set timeouts
			connection.setConnectTimeout(HTTP_CONNECTION_TIMEOUT);
			connection.setReadTimeout(HTTP_READ_TIMEOUT);
			// method is always POST
			connection.setRequestMethod("POST");
			// set HTTP headers
			connection.setRequestProperty("X-PAYPAL-SECURITY-USERID", credentialObj.getAPIUsername());
			//log.info("X-PAYPAL-SECURITY-USERID"+" : "+credentialObj.getAPIUsername());
			connection.setRequestProperty("X-PAYPAL-SECURITY-PASSWORD", credentialObj.getAPIPassword());
			//log.info("X-PAYPAL-SECURITY-PASSWORD"+" : "+credentialObj.getAPIPassword());
			connection.setRequestProperty("X-PAYPAL-SECURITY-SIGNATURE", credentialObj.getSignature());
			//log.info("X-PAYPAL-SECURITY-SIGNATURE"+" : "+credentialObj.getSignature());
			connection.setRequestProperty("X-PAYPAL-REQUEST-DATA-FORMAT", "NV");
			//log.info("X-PAYPAL-REQUEST-DATA-FORMAT"+" : "+"NV");
			connection.setRequestProperty("X-PAYPAL-RESPONSE-DATA-FORMAT", "NV");
			//log.info("X-PAYPAL-RESPONSE-DATA-FORMAT"+" : "+"NV");
			connection.setRequestProperty("X-PAYPAL-APPLICATION-ID", credentialObj.getAppId());
			//log.info("X-PAYPAL-APPLICATION-ID"+" : "+credentialObj.getAppId());
			//connection.setRequestProperty("X-PAYPAL-REQUEST-SOURCE", "GAE-JAVA_Toolkit");
			//log.info("X-PAYPAL-REQUEST-SOURCE"+" : "+"GAE-JAVA_Toolkit");
			

			OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
			writer.write(postData);
			//log.log(Level.INFO,"Data sent: "+postData);
			writer.close();

			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				String inputLine;
				BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
				while ((inputLine = reader.readLine()) != null) {
					responseString += inputLine;
				}
				reader.close();
				if(responseString.length() <= 0){
					throw new Exception(responseString);
				}
				if(log.isLoggable(Level.INFO)) {
					log.info("Received Response: " + responseString);
				}
			} else {
				// Server returned HTTP error code.
				throw new Exception(Integer.toString(connection.getResponseCode()));
			}
		} catch (MalformedURLException e) {
			// ...
			throw e;
		} catch (IOException e) {
			// ...
			throw e;
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new Exception(Integer.toString(503));
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new Exception(Integer.toString(503));
		} 
		return responseString;
	}
}
