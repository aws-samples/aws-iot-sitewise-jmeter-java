package com.amazon.SiteWiseSigV4Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.io.UnsupportedEncodingException;
import java.io.File; 
import java.io.FileNotFoundException;  

import java.util.Scanner;
import java.net.URL;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import org.apache.http.util.EntityUtils;
import org.apache.http.entity.StringEntity;

import java.io.IOException;

public class App {

    static String sendSigV4Post(String url, String amazonDate, String amazonAuth, String amazonToken, String payloadJSON) throws IOException {

            String result = "";
            HttpPost post = new HttpPost(url);

            post.addHeader("content-type", "application/json");
            post.addHeader("x-amz-date", amazonDate);
            post.addHeader("Authorization", amazonAuth);
            post.addHeader("x-amz-security-token", amazonToken);
            
            post.setEntity(new StringEntity(payloadJSON));

            try (CloseableHttpClient httpClient = HttpClients.createDefault();
                CloseableHttpResponse response = httpClient.execute(post)){

                result = EntityUtils.toString(response.getEntity());
            }

            return result;
        }

    static String readJSONFile(String fileName){
        String data = "";
        try {
            File fileObject = new File(fileName);
            Scanner readerObject = new Scanner(fileObject);
            while (readerObject.hasNextLine()) {
                data += readerObject.nextLine();
                if(readerObject.hasNext()) { 
                    data += "\n";
                }
            }
            readerObject.close();
        } catch (FileNotFoundException e) {
            System.out.println("Something is wrong");
            e.printStackTrace();
        }
        return data;
    }

    static byte[] getSignatureKey(String key, String dateStamp, String regionName, String serviceName) {
        try {
            byte[] kSecret = ("AWS4" + key).getBytes("UTF-8");
            byte[] kDate = HmacSHA256(dateStamp, kSecret);
            byte[] kRegion = HmacSHA256(regionName, kDate);
            byte[] kService = HmacSHA256(serviceName, kRegion);
            byte[] kSigning = HmacSHA256("aws4_request", kService);
            return kSigning;
        } catch (UnsupportedEncodingException  e) {
             return "ERR".getBytes();
        }
    }

    static byte[] HmacSHA256(String data, byte[] key) {
        try {
            String algorithm = "HmacSHA256";
            Mac mac = Mac.getInstance(algorithm);
            try {
                mac.init(new SecretKeySpec(key, algorithm));
                try {
                    return  mac.doFinal(data.getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    System.out.println("Something is wrong");
                    return "ERR".getBytes();
                }
            } catch(InvalidKeyException e) {
                System.out.println("Something is wrong");
                return "ERR".getBytes();
            }
        } catch(NoSuchAlgorithmException e) {
            System.out.println("Something is wrong");
            return "ERR".getBytes();
            }
    }

    static String getAmazonDate(LocalDateTime dt) {
       
        String amazonDate = String.valueOf(dt.getYear());

        System.out.println("amazonDate:" + amazonDate);

        amazonDate += (dt.getMonthValue() < 10) ? "0" + dt.getMonthValue() : dt.getMonthValue();
        amazonDate += (dt.getDayOfMonth() < 10) ? "0" + dt.getDayOfMonth() : dt.getDayOfMonth();
        amazonDate += "T";
        amazonDate += (dt.getHour() < 10)   ? "0" + dt.getHour()   : dt.getHour();
        amazonDate += (dt.getMinute() < 10) ? "0" + dt.getMinute() : dt.getMinute();
        amazonDate += (dt.getSecond() < 10) ? "0" + dt.getSecond() : dt.getSecond();
        amazonDate += "Z";
        
        System.out.println("amazonDate:" + amazonDate);
        return amazonDate;
    }
    final static char[] hexArray = "0123456789abcdef".toCharArray();

    static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
    static byte[] getSha256(String text) {
        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return hash;

        } catch (NoSuchAlgorithmException e) {

            return "ERR".getBytes();


        }
        
       
    }

    public static void main(String[] args) throws Exception{

        System.out.println("args: " + args[0]);

        // Make sure we get one parameter
        String fileName = args[0];

        String AWS_ACCESS_KEY_ID = System.getenv("AWS_ACCESS_KEY_ID");
        String AWS_SECRET_ACCESS_KEY = System.getenv("AWS_SECRET_ACCESS_KEY");
        String AWS_SESSION_TOKEN = System.getenv("AWS_SESSION_TOKEN");

        String AWS_REGION = "eu-west-1";
        String serviceName = "iotsitewise";

        // Get method from sampler
        // TESTING Used for JMeter script -> String method = sampler.getMethod();
        String method = "POST"; // TESTING
        System.out.println("Method: " + method);

        // Get body from sampler
        // TESTING Used for JMeter script ->  String body = method.equalsIgnoreCase("POST") ? sampler.getArguments().getArgument(0).getValue() : "";
        String body = readJSONFile(args[0]);
        System.out.println("body:\n" + body);
            
        // Retrieve URL from HTTP Request
        // TESTING Used for JMeter script ->  URL url = sampler.getUrl();
        URL url = new URL("https://data.iotsitewise."+ AWS_REGION +".amazonaws.com/properties"); // TESTING

        // Get host from URL
        String host = url.getHost(); 
        System.out.println("Host: " + host);
        String canonicalUri = url.getPath();
        System.out.println("Path: " + canonicalUri);
        
        String canonicalQuerystring = "";
        
        LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);
        String amazonDate = getAmazonDate(currentTime);
        String datestamp = amazonDate.substring(0, 8);
        String canonicalHeaders = "content-type:application/json\n" 
                                + "host:" + host + "\n" 
                                + "x-amz-date:" + amazonDate + "\n" 
                                + "x-amz-security-token:" + AWS_SESSION_TOKEN + "\n";

        String signedHeaders = "content-type;host;x-amz-date;x-amz-security-token";
        String payloadHash = bytesToHex(getSha256(body));
        System.out.println("payloadHash: " + payloadHash);

        String canonicalRequest = method + "\n" + canonicalUri + "\n" + canonicalQuerystring + "\n" + canonicalHeaders + "\n" + signedHeaders + "\n" + payloadHash;
        System.out.println("Canonical request:\n" + canonicalRequest);

        String algorithm = "AWS4-HMAC-SHA256";
        String credentialScope = datestamp + "/" + AWS_REGION + "/" + serviceName + "/aws4_request";
        String stringToSign = algorithm + "\n" + amazonDate + "\n" + credentialScope + "\n" + bytesToHex(getSha256(canonicalRequest));
        System.out.println("String to sign:\n" + stringToSign);

        byte[] signingKey = getSignatureKey(AWS_SECRET_ACCESS_KEY, datestamp, AWS_REGION, serviceName);
        String signature = bytesToHex(HmacSHA256(stringToSign, signingKey));
        String amazonAuth = "AWS4-HMAC-SHA256 Credential=" + AWS_ACCESS_KEY_ID + "/" + credentialScope + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature;
        System.out.println("###########################################");
        System.out.println("-H \"Authorization: " + amazonAuth + "\"");
        System.out.println("-H \"x-amz-date: " + amazonDate + "\"");
        System.out.println("-H \"x-amz-security-token: " + AWS_SESSION_TOKEN+ "\"");

        try {

            String result = sendSigV4Post("https://data.iotsitewise."+ AWS_REGION +".amazonaws.com/properties", amazonDate, amazonAuth, AWS_SESSION_TOKEN, body );
            
            System.out.println("sendSigV4Post result: " + result);
        
        } catch (IOException e) {
        
            e.printStackTrace();
        
        }

    }
}

