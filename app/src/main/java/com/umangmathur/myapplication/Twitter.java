package com.umangmathur.myapplication;


import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import cz.msebera.android.httpclient.HttpException;
import cz.msebera.android.httpclient.HttpHost;
import cz.msebera.android.httpclient.HttpRequestInterceptor;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.HttpVersion;
import cz.msebera.android.httpclient.client.protocol.RequestExpectContinue;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.impl.DefaultHttpClientConnection;
import cz.msebera.android.httpclient.message.BasicHttpEntityEnclosingRequest;
import cz.msebera.android.httpclient.params.HttpParams;
import cz.msebera.android.httpclient.params.HttpProtocolParams;
import cz.msebera.android.httpclient.params.SyncBasicHttpParams;
import cz.msebera.android.httpclient.protocol.BasicHttpContext;
import cz.msebera.android.httpclient.protocol.ExecutionContext;
import cz.msebera.android.httpclient.protocol.HttpContext;
import cz.msebera.android.httpclient.protocol.HttpProcessor;
import cz.msebera.android.httpclient.protocol.HttpRequestExecutor;
import cz.msebera.android.httpclient.protocol.ImmutableHttpProcessor;
import cz.msebera.android.httpclient.protocol.RequestConnControl;
import cz.msebera.android.httpclient.protocol.RequestContent;
import cz.msebera.android.httpclient.protocol.RequestTargetHost;
import cz.msebera.android.httpclient.protocol.RequestUserAgent;
import cz.msebera.android.httpclient.util.EntityUtils;


/***
 * This class is not written by me. Hence the deprecated classes.
 * Due to scarcity of time and complexity of OAuth, I was unable to devote time to write my own implementation.
 */


public class Twitter implements MyConstants {

    public static JSONObject startTwitterAuthentication() throws IOException, GeneralSecurityException, HttpException, JSONException {
        JSONObject jsonresponse = new JSONObject();
        // this particular request uses POST
        String get_or_post = "POST";
        // I think this is the signature method used for all Twitter API calls
        String oauth_signature_method = "HMAC-SHA1";
        // generate any fairly random alphanumeric string as the "nonce". Nonce = Number used ONCE.
        String uuid_string = UUID.randomUUID().toString();
        uuid_string = uuid_string.replaceAll("-", "");
        String oauth_nonce = uuid_string; // any relatively random alphanumeric string will work here
        // get the timestamp
        Calendar tempcal = Calendar.getInstance();
        long ts = tempcal.getTimeInMillis();// get current time in milliseconds
        String oauth_timestamp = (new Long(ts / 1000)).toString(); // then divide by 1000 to get seconds
        // assemble the proper parameter string, which must be in alphabetical order, using your consumer key
        String parameter_string = "oauth_consumer_key=" + TWITTER_CONSUMER_KEY + "&oauth_nonce=" + oauth_nonce + "&oauth_signature_method=" + oauth_signature_method + "&oauth_timestamp=" + oauth_timestamp + "&oauth_version=1.0";
        System.out.println("parameter_string=" + parameter_string); // print out parameter string for error checking, if you want
        // specify the proper twitter API endpoint at which to direct this request
        String twitter_endpoint = "https://api.twitter.com/oauth/request_token";
        String twitter_endpoint_host = "api.twitter.com";
        String twitter_endpoint_path = "/oauth/request_token";
        // assemble the string to be signed. It is METHOD & percent-encoded endpoint & percent-encoded parameter string
        // Java's native URLEncoder.encode function will not work. It is the wrong RFC specification (which does "+" where "%20" should be)...
        // the encode() function included in this class compensates to conform to RFC 3986 (which twitter requires)
        String signature_base_string = get_or_post + "&" + encode(twitter_endpoint) + "&" + encode(parameter_string);
        // now that we've got the string we want to sign (see directly above) HmacSHA1 hash it against the consumer secret
        String oauth_signature = "";
        oauth_signature = computeSignature(signature_base_string, TWITTER_CONSUMER_SECRET + "&");  // note the & at the end. Normally the user access_token would go here, but we don't know it yet for request_token
        // each request to the twitter API 1.1 requires an "Authorization: BLAH" header. The following is what BLAH should look like
        String authorization_header_string = "OAuth oauth_consumer_key=\"" + TWITTER_CONSUMER_KEY + "\",oauth_signature_method=\"HMAC-SHA1\",oauth_timestamp=\"" +
                oauth_timestamp + "\",oauth_nonce=\"" + oauth_nonce + "\",oauth_version=\"1.0\",oauth_signature=\"" + encode(oauth_signature) + "\"";
        System.out.println("authorization_header_string=" + authorization_header_string);    // print out authorization_header_string for error checking
        String oauth_token = "";
        String oauth_token_secret = "";
        //String oauth_callback_confirmed = "";
        // I'm using Apache HTTPCore to make the connection and process the request. In theory, you could use HTTPClient, but HTTPClient defaults to the wrong RFC encoding, which has to be tweaked.
        HttpParams params = new SyncBasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setUserAgent(params, "HttpCore/1.1");
        HttpProtocolParams.setUseExpectContinue(params, false);
        HttpProcessor httpproc = new ImmutableHttpProcessor(new HttpRequestInterceptor[]{
                // Required protocol interceptors
                new RequestContent(),
                new RequestTargetHost(),
                // Recommended protocol interceptors
                new RequestConnControl(),
                new RequestUserAgent(),
                new RequestExpectContinue()});
        HttpRequestExecutor httpexecutor = new HttpRequestExecutor();
        HttpContext context = new BasicHttpContext(null);
        HttpHost host = new HttpHost(twitter_endpoint_host, 443); // use 80 if you want regular HTTP (not HTTPS)
        DefaultHttpClientConnection conn = new DefaultHttpClientConnection();
        context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
        context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, host);
        // initialize the HTTPS connection
        SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(null, null, null);
        SSLSocketFactory ssf = sslcontext.getSocketFactory();
        Socket socket = ssf.createSocket();
        socket.connect(new InetSocketAddress(host.getHostName(), host.getPort()), 0);
        conn.bind(socket, params);
        BasicHttpEntityEnclosingRequest request2 = new BasicHttpEntityEnclosingRequest("POST", twitter_endpoint_path);
        request2.setEntity(new StringEntity("", "application/x-www-form-urlencoded", "UTF-8"));
        request2.setParams(params);
        request2.addHeader("Authorization", authorization_header_string); // this is where we're adding that required "Authorization: BLAH" header.
        httpexecutor.preProcess(request2, httpproc, context);
        HttpResponse response2 = httpexecutor.execute(request2, conn, context);
        response2.setParams(params);
        httpexecutor.postProcess(response2, httpproc, context);
        if (response2.getStatusLine().toString().indexOf("200") == -1) // if it wasn't a successful request, then this error code will be something other than 200, return indicating as such
        {
            jsonresponse.put("response_status", "error");
            jsonresponse.put("message", "Twitter request_token request failed. Response was !200.");
        } else // returned 200 (good)
        {
            String responseBody = EntityUtils.toString(response2.getEntity());
            System.out.println("endpoint startTwitterAuthentication responsebody=" + responseBody); // print out the response

            if (responseBody.indexOf("oauth_callback_confirmed=") == -1) // if this were true, that would be weird. Successful (200) response, but no oauth_callback_confirmed?
            {
                jsonresponse.put("response_status", "error");
                jsonresponse.put("message", "Twitter request_token request failed. response was 200 but did not contain oauth_callback_confirmed");
            } else {
                // this assumes that oauth_callback_confirmed is always the last of the three values returned. I don't know 100% that is true, but it seems to be.
                String occ_val = responseBody.substring(responseBody.indexOf("oauth_callback_confirmed=") + 25);
                if (!occ_val.equals("true")) {
                    jsonresponse.put("response_status", "error");
                    jsonresponse.put("message", "Twitter request_token response was 200 and contained oauth_callback_confirmed but it was not \"true\".");
                } else // everything seems a-ok. look for values and return them.
                {
                    // using the tokenizer takes away the need for the values to be in any particular order.
                    StringTokenizer st = new StringTokenizer(responseBody, "&");
                    String currenttoken = "";
                    while (st.hasMoreTokens()) {
                        currenttoken = st.nextToken();
                        if (currenttoken.startsWith("oauth_token="))
                            oauth_token = currenttoken.substring(currenttoken.indexOf("=") + 1);
                        else if (currenttoken.startsWith("oauth_token_secret="))
                            oauth_token_secret = currenttoken.substring(currenttoken.indexOf("=") + 1);
                        else if (currenttoken.startsWith("oauth_callback_confirmed=")) {
                            //oauth_callback_confirmed = currenttoken.substring(currenttoken.indexOf("=") + 1);
                        } else {
                            System.out.println("Warning... twitter returned a key we weren't looking for.");
                        }
                    }
                    if (oauth_token.equals("") || oauth_token_secret.equals("")) // if either key is empty, that's weird and bad
                    {
                        jsonresponse.put("response_status", "error");
                        jsonresponse.put("message", "oauth tokens in response were invalid");
                    } else // otherwise, we're all good. Return the values (did not include oauth_token_confirmed here. no need)
                    {
                        jsonresponse.put("response_status", "success");
                        jsonresponse.put("oauth_token", oauth_token);
                        //jsonresponse.put("oauth_token_secret", oauth_token);
                    }
                }
            }
        }
        conn.close();
        return jsonresponse;
    }

    public static JSONObject getTwitterAccessTokenFromAuthorizationCode(String verifier_or_pin, String oauth_token) throws IOException, GeneralSecurityException, HttpException, JSONException {
        JSONObject jsonresponse = new JSONObject();
        // this particular request uses POST
        String get_or_post = "POST";
        // I think this is the signature method used for all Twitter API calls
        String oauth_signature_method = "HMAC-SHA1";
        // generate any fairly random alphanumeric string as the "nonce". Nonce = Number used ONCE.
        String uuid_string = UUID.randomUUID().toString();
        uuid_string = uuid_string.replaceAll("-", "");
        String oauth_nonce = uuid_string; // any relatively random alphanumeric string will work here
        // get the timestamp
        Calendar tempcal = Calendar.getInstance();
        long ts = tempcal.getTimeInMillis();// get current time in milliseconds
        String oauth_timestamp = (new Long(ts / 1000)).toString(); // then divide by 1000 to get seconds
        // the parameter string must be in alphabetical order
        String parameter_string = "oauth_consumer_key=" + TWITTER_CONSUMER_KEY + "&oauth_nonce=" + oauth_nonce + "&oauth_signature_method=" + oauth_signature_method +
                "&oauth_timestamp=" + oauth_timestamp + "&oauth_token=" + encode(oauth_token) + "&oauth_version=1.0";
        System.out.println("parameter_string=" + parameter_string);
        String twitter_endpoint = "https://api.twitter.com/oauth/access_token";
        String twitter_endpoint_host = "api.twitter.com";
        String twitter_endpoint_path = "/oauth/access_token";
        String signature_base_string = get_or_post + "&" + encode(twitter_endpoint) + "&" + encode(parameter_string);
        String oauth_signature = "";
        oauth_signature = computeSignature(signature_base_string, TWITTER_CONSUMER_SECRET + "&");  // note the & at the end. Normally the user access_token would go here, but we don't know it yet
        System.out.println("oauth_signature=" + encode(oauth_signature));
        String authorization_header_string = "OAuth oauth_consumer_key=\"" + TWITTER_CONSUMER_KEY + "\",oauth_signature_method=\"HMAC-SHA1\",oauth_timestamp=\"" + oauth_timestamp +
                "\",oauth_nonce=\"" + oauth_nonce + "\",oauth_version=\"1.0\",oauth_signature=\"" + encode(oauth_signature) + "\",oauth_token=\"" + encode(oauth_token) + "\"";
        // System.out.println("authorization_header_string=" + authorization_header_string);
        String access_token = "";
        String access_token_secret = "";
        String user_id = "";
        String screen_name = "";
        HttpParams params = new SyncBasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setUserAgent(params, "HttpCore/1.1");
        HttpProtocolParams.setUseExpectContinue(params, false);
        HttpProcessor httpproc = new ImmutableHttpProcessor(new HttpRequestInterceptor[]{
                // Required protocol interceptors
                new RequestContent(),
                new RequestTargetHost(),
                // Recommended protocol interceptors
                new RequestConnControl(),
                new RequestUserAgent(),
                new RequestExpectContinue()});
        HttpRequestExecutor httpexecutor = new HttpRequestExecutor();
        HttpContext context = new BasicHttpContext(null);
        HttpHost host = new HttpHost(twitter_endpoint_host, 443);
        DefaultHttpClientConnection conn = new DefaultHttpClientConnection();
        context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
        context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, host);
        SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(null, null, null);
        SSLSocketFactory ssf = sslcontext.getSocketFactory();
        Socket socket = ssf.createSocket();
        socket.connect(new InetSocketAddress(host.getHostName(), host.getPort()), 0);
        conn.bind(socket, params);
        BasicHttpEntityEnclosingRequest request2 = new BasicHttpEntityEnclosingRequest("POST", twitter_endpoint_path);
        // this time, we've got to include the oauth_verifier value with the request
        request2.setEntity(new StringEntity("oauth_verifier=" + encode(verifier_or_pin), "application/x-www-form-urlencoded", "UTF-8"));
        request2.setParams(params);
        request2.addHeader("Authorization", authorization_header_string);
        httpexecutor.preProcess(request2, httpproc, context);
        HttpResponse response2 = httpexecutor.execute(request2, conn, context);
        System.out.println("getTwitterAccessTokenFromAuthorizationCode response.getStatusLine()=" + response2.getStatusLine());
        response2.setParams(params);
        httpexecutor.postProcess(response2, httpproc, context);
        String responseBody = EntityUtils.toString(response2.getEntity());
        if (response2.getStatusLine().toString().indexOf("200") == -1) // response from twitter wasn't 200, that's bad
        {
            jsonresponse.put("response_status", "error");
            jsonresponse.put("message", "getTwitterAccessTokenFromAuthorizationCode request failed. Response was !200.");
        } else {
            StringTokenizer st = new StringTokenizer(responseBody, "&");
            String currenttoken = "";
            while (st.hasMoreTokens()) {
                currenttoken = st.nextToken();
                if (currenttoken.startsWith("oauth_token="))
                    access_token = currenttoken.substring(currenttoken.indexOf("=") + 1);
                else if (currenttoken.startsWith("oauth_token_secret="))
                    access_token_secret = currenttoken.substring(currenttoken.indexOf("=") + 1);
                else if (currenttoken.startsWith("user_id="))
                    user_id = currenttoken.substring(currenttoken.indexOf("=") + 1);
                else if (currenttoken.startsWith("screen_name="))
                    screen_name = currenttoken.substring(currenttoken.indexOf("=") + 1);
            }
        }
        if (access_token.equals("") || access_token_secret.equals("")) // if either of these values is empty, that's bad
        {
            jsonresponse.put("response_status", "error");
            jsonresponse.put("message", "code into access token failed. oauth tokens in response were invalid");
        } else {
            jsonresponse.put("response_status", "success");
            jsonresponse.put("access_token", access_token);
            jsonresponse.put("access_token_secret", access_token_secret);
            jsonresponse.put("user_id", user_id);
            jsonresponse.put("screen_name", screen_name);
        }
        conn.close();
        return jsonresponse;
    }

    public static JSONObject verifyCredentials(String access_token, String access_token_secret) throws IOException, GeneralSecurityException, HttpException, JSONException {
        JSONObject jsonresponse = new JSONObject();
        String oauth_token = access_token;
        String oauth_token_secret = access_token_secret;
        // generate authorization header
        String get_or_post = "GET";
        String oauth_signature_method = "HMAC-SHA1";
        String uuid_string = UUID.randomUUID().toString();
        uuid_string = uuid_string.replaceAll("-", "");
        String oauth_nonce = uuid_string; // any relatively random alphanumeric string will work here
        // get the timestamp
        Calendar tempcal = Calendar.getInstance();
        long ts = tempcal.getTimeInMillis();// get current time in milliseconds
        String oauth_timestamp = (new Long(ts / 1000)).toString(); // then divide by 1000 to get seconds
        // the parameter string must be in alphabetical order
        // this time, I add 3 extra params to the request, "lang", "result_type" and "q".
        String parameter_string = "oauth_consumer_key=" + TWITTER_CONSUMER_KEY + "&oauth_nonce=" + oauth_nonce + "&oauth_signature_method=" + oauth_signature_method +
                "&oauth_timestamp=" + oauth_timestamp + "&oauth_token=" + encode(oauth_token) + "&oauth_version=1.0";
        //System.out.println("parameter_string=" + parameter_string);
        String twitter_endpoint = "https://api.twitter.com/1.1/account/verify_credentials.json";
        String twitter_endpoint_host = "api.twitter.com";
        String twitter_endpoint_path = "/1.1/account/verify_credentials.json";
        String signature_base_string = get_or_post + "&" + encode(twitter_endpoint) + "&" + encode(parameter_string);
        // this time the base string is signed using twitter_consumer_secret + "&" + encode(oauth_token_secret) instead of just twitter_consumer_secret + "&"
        String oauth_signature = "";
        oauth_signature = computeSignature(signature_base_string, TWITTER_CONSUMER_SECRET + "&" + encode(oauth_token_secret));  // note the & at the end. Normally the user access_token would go here, but we don't know it yet for request_token
        String authorization_header_string = "OAuth oauth_consumer_key=\"" + TWITTER_CONSUMER_KEY + "\",oauth_signature_method=\"HMAC-SHA1\",oauth_timestamp=\"" + oauth_timestamp +
                "\",oauth_nonce=\"" + oauth_nonce + "\",oauth_version=\"1.0\",oauth_signature=\"" + encode(oauth_signature) + "\",oauth_token=\"" + encode(oauth_token) + "\"";
        //System.out.println("authorization_header_string=" + authorization_header_string);
        HttpParams params = new SyncBasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setUserAgent(params, "HttpCore/1.1");
        HttpProtocolParams.setUseExpectContinue(params, false);
        HttpProcessor httpproc = new ImmutableHttpProcessor(new HttpRequestInterceptor[]{
                // Required protocol interceptors
                new RequestContent(),
                new RequestTargetHost(),
                // Recommended protocol interceptors
                new RequestConnControl(),
                new RequestUserAgent(),
                new RequestExpectContinue()});
        HttpRequestExecutor httpexecutor = new HttpRequestExecutor();
        HttpContext context = new BasicHttpContext(null);
        HttpHost host = new HttpHost(twitter_endpoint_host, 443);
        DefaultHttpClientConnection conn = new DefaultHttpClientConnection();
        context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
        context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, host);
        SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(null, null, null);
        SSLSocketFactory ssf = sslcontext.getSocketFactory();
        Socket socket = ssf.createSocket();
        socket.connect(new InetSocketAddress(host.getHostName(), host.getPort()), 0);
        conn.bind(socket, params);
        // the following line adds 3 params to the request just as the parameter string did above. They must match up or the request will fail.
        BasicHttpEntityEnclosingRequest request2 = new BasicHttpEntityEnclosingRequest("GET", twitter_endpoint_path);
        request2.setParams(params);
        request2.addHeader("Authorization", authorization_header_string); // always add the Authorization header
        httpexecutor.preProcess(request2, httpproc, context);
        HttpResponse response2 = httpexecutor.execute(request2, conn, context);
        response2.setParams(params);
        httpexecutor.postProcess(response2, httpproc, context);
        if (response2.getStatusLine().toString().indexOf("500") != -1) {
            jsonresponse.put("response_status", "error");
            jsonresponse.put("message", "Twitter auth error.");
        } else {
            // if successful, the response should be a JSONObject of tweets
            JSONObject jo = new JSONObject(EntityUtils.toString(response2.getEntity()));
            if (jo.has("errors")) {
                jsonresponse.put("response_status", "error");
                String message_from_twitter = jo.getJSONArray("errors").getJSONObject(0).getString("message");
                if (message_from_twitter.equals("Invalid or expired token") || message_from_twitter.equals("Could not authenticate you"))
                    jsonresponse.put("message", "Twitter auth error.");
                else
                    jsonresponse.put("message", jo.getJSONArray("errors").getJSONObject(0).getString("message"));
            } else {
                jsonresponse.put("twitter_jo", jo); // this is the full result object from Twitter
            }
            conn.close();
        }
        return jsonresponse;
    }

    public static String encode(String value) throws UnsupportedEncodingException {
        String encoded = URLEncoder.encode(value, "UTF-8");
        StringBuilder buf = new StringBuilder(encoded.length());
        char focus;
        for (int i = 0; i < encoded.length(); i++) {
            focus = encoded.charAt(i);
            if (focus == '*') {
                buf.append("%2A");
            } else if (focus == '+') {
                buf.append("%20");
            } else if (focus == '%' && (i + 1) < encoded.length()
                    && encoded.charAt(i + 1) == '7' && encoded.charAt(i + 2) == 'E') {
                buf.append('~');
                i += 2;
            } else {
                buf.append(focus);
            }
        }
        return buf.toString();
    }

    private static String computeSignature(String baseString, String keyString) throws GeneralSecurityException, UnsupportedEncodingException {
        byte[] keyBytes = keyString.getBytes();
        SecretKey secretKey = new SecretKeySpec(keyBytes, "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(secretKey);
        byte[] text = baseString.getBytes();
        return new String(Base64.encodeBase64(mac.doFinal(text))).trim();
    }

}