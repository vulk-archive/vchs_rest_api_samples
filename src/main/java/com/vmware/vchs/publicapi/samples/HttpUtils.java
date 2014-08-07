/*
 * Copyright (c) 2013 VMware, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.    You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS,  WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.vmware.vchs.publicapi.samples;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.JAXB;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.vmware.ares.pub.api.VCloudSessionType;
import com.vmware.ares.pub.api.VdcLinkType;
import com.vmware.vcloud.api.rest.schema.QueryResultRecordsType;

/**
 * This class provides the common http functionality using the Apache HttpClient library.
 */
public class HttpUtils {
    /**
     * Executes an http request using the passed in request parameter.
     * 
     * @param request
     *            the HttpRequestBase subclass to make a request with
     * @return the response of the request
     */
    public static HttpResponse httpInvoke(HttpRequestBase request) {
        HttpResponse httpResponse = null;
        HttpClient httpClient = null;

        try {
            // Create a fresh HttpClient.. some samples will make calls to two (or more)
            // urls in a single run, sharing a static non-multithreaded instance causes
            // exceptions. This prevents that by ensuring each call to httpInvoke gets
            // its own instance.
            httpClient = createHttpClient();
            httpResponse = httpClient.execute(request);
        } catch (ClientProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return httpResponse;
    }

    /**
     * This method will parse the passed in String which is presumably a complete URL and return the
     * base URL e.g. https://vchs.vmware.api/ from the component parts of the passed in URL.
     * 
     * @param vDCUrl
     *            the VDC Href to parse
     * @return the base url of the vCloud
     */
    public static String getHostname(String completeUrl) {
        URL baseUrl = null;

        try {
            // First create a URL object
            baseUrl = new URL(completeUrl);
            // Now use the URL implementation to provide the component parts, leaving out
            // some parts so that we can build just the base URL without the path and query string
            return new URL(baseUrl.getProtocol(), baseUrl.getHost(), baseUrl.getPort(), "")
                    .toString();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid URL: " + completeUrl);
        }
    }

    /**
     * This method returns an HttpClient instance wrapped to trust all HTTPS certificates.
     * 
     * @return HttpClient a new instance of HttpClient
     */
    static HttpClient createHttpClient() {
        HttpClient base = new DefaultHttpClient();

        try {
            SSLContext ctx = SSLContext.getInstance("TLS");

            // WARNING: This creates a TrustManager that trusts all certificates and should not be used in production code.
            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] certs,
                        String authType) {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] certs,
                        String authType) {
                }
            }
            };

            ctx.init(null, trustAllCerts, null);
            SSLSocketFactory ssf = new SSLSocketFactory(ctx);
            ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            ClientConnectionManager ccm = base.getConnectionManager();
            SchemeRegistry sr = ccm.getSchemeRegistry();
            sr.register(new Scheme("https", 443, ssf));

            return new DefaultHttpClient(ccm, base.getParams());
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * This method will unmarshal the passed in entity using the passed in class type
     * 
     * @param entity
     *            the entity to unmarshal
     * @param clazz
     *            the class type to base the unmarshal from
     * @return unmarshal an instance of the provided class type
     */
    public static <T> T unmarshal(HttpEntity entity, Class<T> clazz) {
        InputStream is = null;

        try {
            String s = EntityUtils.toString(entity);
            // Uncomment this to print out all the XML responses to the console, useful for
            // debugging
             //System.out.println(s);
            is = new ByteArrayInputStream(s.getBytes());
            return JAXB.unmarshal(is, clazz);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (null != is) {
                    is.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Gets the string content from the passed in InputStream
     * 
     * @param is
     *            response stream from GET/POST method call
     * @return String content of the passed in InputStream
     */
    public static String getContent(InputStream is) {
        if (is != null) {
            StringBuilder sb = new StringBuilder();
            String line;

            BufferedReader reader = null;

            try {
                reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (null != reader) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        throw new RuntimeException(e);
                    }
                }
            }

            return sb.toString();
        }

        return "";
    }

    /**
     * This method is to get the vCloud API EndPoint for the VDC.
     * 
     * @param href
     *            href to vcloudsession for VDC
     * @return an object containing the link to vCloud API EndPoint for VDC and Authorization Token
     *         for vCloud API Call.
     */
    static Vcd getVCDEndPoint(Vchs vchs, DefaultSampleCommandLineOptions options, String vcdSessionHref) {
        // Create post request to get vCloudSession details
        HttpResponse response = HttpUtils.httpInvoke(vchs.post(vcdSessionHref, options));

        // Make sure the response status is 201 CREATED
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
            throw new RuntimeException("\nFailed : HTTP error code : "
                    + response.getStatusLine().getStatusCode());
        }

        // Contains the details regarding vCloud API End point for VDC and authorization token to
        // be used
        VCloudSessionType vCSession = HttpUtils.unmarshal(response.getEntity(),
                VCloudSessionType.class);

        // This link is for vCloud API End point for VDC
        VdcLinkType vdcLink = vCSession.getVdcLink();

        if (vdcLink == null) {
            throw new RuntimeException("\nCould not find vCloud EndPoint for VDC");
        }

        // Set the vCloud token for subsequent requests to vCloud
        Vcd vcd = new Vcd();
        vcd.vcdToken = vdcLink.getAuthorizationToken();
        vcd.vdcHref = vdcLink.getHref();
        return vcd;
    }

    /**
     * This method uses the vCloud API Query service
     * 
     * @param baseVcdUrl
     * @param queryParameters
     * @return
     */
    public static QueryResultRecordsType getQueryResults(String baseVcdUrl, String queryParameters,
            DefaultSampleCommandLineOptions options, String vCloudToken) {
        URL url = null;
        QueryResultRecordsType results = null;

        try {
            // Construct the URL from the baseVcdUrl to utilize the vCloud Query API to find a
            // matching template
            url = new URL(baseVcdUrl + "/api/query?" + queryParameters);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid URL: " + baseVcdUrl);
        }

        HttpGet httpGet = new HttpGet(url.toString());
        httpGet.setHeader(HttpHeaders.ACCEPT, SampleConstants.APPLICATION_PLUS_XML_VERSION
                + options.vcdVersion);

        httpGet.setHeader(SampleConstants.VCD_AUTHORIZATION_HEADER, vCloudToken);

        HttpResponse response = HttpUtils.httpInvoke(httpGet);

        // make sure the status is 200 OK
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            results = HttpUtils.unmarshal(response.getEntity(), QueryResultRecordsType.class);
        }

        return results;
    }
}