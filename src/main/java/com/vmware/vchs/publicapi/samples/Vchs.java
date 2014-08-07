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

import java.io.IOException;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

import sun.misc.BASE64Encoder;

import com.vmware.ares.pub.api.ComputeType;
import com.vmware.ares.pub.api.LinkType;
import com.vmware.ares.pub.api.ServiceListType;
import com.vmware.ares.pub.api.ServiceType;
import com.vmware.ares.pub.api.SessionType;
import com.vmware.ares.pub.api.VdcReferenceType;

public class Vchs {
    // The vCHS Public API token in response to authentication with the username and password and
    // used for subsequent requests.
   String vchsToken;

   // The serviceListHref to vchs
   String vchsServiceListHref;

   /**
     * Logs in to vCHS and retrieves the authorization token and serviceList href. It will
     * return true if login is a success, otherwise a runtime exception will be thrown
     * 
     * @return true if login was successful
     */
    public boolean login(DefaultSampleCommandLineOptions options) {
        HttpPost httpPost = post(options.vchsHostname + SampleConstants.SESSION_URL, options);

        // Set the Basic Auth header for login only
        httpPost.setHeader(
                HttpHeaders.AUTHORIZATION,
                "Basic "
                        + new BASE64Encoder().encode(new String(options.vchsUsername + ":"
                                + options.vchsPassword).getBytes()));
        httpPost.setHeader(HttpHeaders.ACCEPT, SampleConstants.APPLICATION_XML_VERSION
                + options.vchsVersion);
        HttpResponse response;

        try {
            HttpClient httpClient = HttpUtils.createHttpClient();
            response = httpClient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + response.getStatusLine().getStatusCode());
            }

            SessionType sessionType = HttpUtils.unmarshal(response.getEntity(), SessionType.class);
            List<LinkType> linklist = sessionType.getLink();
            for (LinkType link : linklist) {
                if (link.getType() != null
                        && link.getType().equals(SampleConstants.APPLICATION_XML_SERVICE_LIST)) {
                    vchsServiceListHref = link.getHref();
                    // Found it, break out of loop
                    break;
                }
            }

            if (vchsServiceListHref == null) {
                throw new RuntimeException("Could not find Href for the Service List");
            }

            // Extracting SAML Token to make further calls
            Header[] vchsHeader = response.getHeaders(SampleConstants.VCHS_AUTHORIZATION_HEADER);
            vchsToken = vchsHeader[0].getValue();

            return true;
        } catch (ClientProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieve the vCloud session href for the VDC vdcName.
     *  
     * It will first make a call to the vCHS API to get a list of
     * all services. It then iterates through the services to find the specific VDC.
     * 
     * If the VDC is found, the corresponding vCloud session href is returned.
     */
    String getVCloudDSessionHref(DefaultSampleCommandLineOptions options) {
        System.out.print("Retrieving vCloud session href...");

        // invoke the serviceList API to retrieve the list of services
        HttpResponse response = HttpUtils.httpInvoke(get(vchsServiceListHref, options));

        // Make sure the response status is 200 OK
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new RuntimeException("\nFailed : HTTP error code : "
                    + response.getStatusLine().getStatusCode());
        }

        // Parse the response entity to get the vCHS serviceList
        ServiceListType serviceList = HttpUtils.unmarshal(response.getEntity(),
                ServiceListType.class);

        // services is the List of the services under serviceList
        List<ServiceType> services = serviceList.getService();

        // Iterate through services to find the href for 
        // the VDC provided as parameter vdcName
        for (ServiceType service : services) {
            if (service.getHref() != null) {
                // search this service for the VDC by vdcName
                String vdcVCloudSessionHref = findVDCByName(service.getHref(), options);
                if (vdcVCloudSessionHref != null) {  // If not null then VDC is found
                    System.out.println("Success\n");
                    return vdcVCloudSessionHref;
                }
            }
        }

        // VDC not found
        throw new RuntimeException("\nCould not find VDC: " + options.vdcName);
    }

    /**
     * This method finds a VDC by name within a service.
     * 
     * @param serviceHref
     *            Href to the service.
     * @param vdcName
     *            name of the vdc
     * @return vCloudSession Href for VDC
     *          null if the VDC is not found
     */
    private String findVDCByName(String serviceHref, DefaultSampleCommandLineOptions options) {
        String vcloudSessionHref = null;

        // Invoke the computeService API
        HttpResponse response = HttpUtils.httpInvoke(get(serviceHref, options));

        // To Make sure the response status is 200 OK
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new RuntimeException("\nFailed : HTTP error code : "
                    + response.getStatusLine().getStatusCode());
        }

        // Get the compute object which contains the collection of VDC references.
        ComputeType compute = HttpUtils.unmarshal(response.getEntity(), ComputeType.class);
        List<VdcReferenceType> vdcRef = compute.getVdcRef();

        // Iterating through all the VDC under the service to find the desired VDC and get its
        // vCloudSession Href
        for (VdcReferenceType vdc : vdcRef) {
            if (vdc.getName().equalsIgnoreCase(options.vdcName)) {
                List<LinkType> vdcLinks = vdc.getLink();

                for (LinkType link : vdcLinks) {
                    // To filter vcloudsession link
                    if (link.getType()
                            .equals("application/xml;class=vnd.vmware.vchs.vcloudsession")) {
                        vcloudSessionHref = link.getHref();
                    }
                }

            }
        }

        return vcloudSessionHref;
    }

    
    /**
     * Creates an org.apache.http.client.methods.HttpGet object adding in the http headers for a
     * VCHS GET
     * 
     * @param url
     *            the url to make the get request to
     * @return the instance of HttpGet populated with the correct headers
     */
    public HttpGet get(String url, DefaultSampleCommandLineOptions options) {
        HttpGet httpGet = new HttpGet(url);

        httpGet.setHeader(HttpHeaders.ACCEPT, SampleConstants.APPLICATION_XML_VERSION
                + options.vchsVersion);
        httpGet.setHeader(SampleConstants.VCHS_AUTHORIZATION_HEADER, vchsToken);

        return httpGet;
    }

    /**
     * Creates an org.apache.http.client.methods.HttpPost object adding in the http headers for a
     * VCHS POST
     * 
     * @param url
     *            the url to make the post request to
     * @return the instance of HttpPost populated with the correct headers
     */
    public HttpPost post(String url, DefaultSampleCommandLineOptions options) {
        HttpPost httpPost = new HttpPost(url);
        
        httpPost.setHeader(HttpHeaders.ACCEPT, SampleConstants.APPLICATION_XML_VERSION
                + options.vchsVersion);
        httpPost.setHeader(SampleConstants.VCHS_AUTHORIZATION_HEADER, vchsToken);

        return httpPost;
    }
}