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

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;

public class Vcd {
    // The vCloud API Href
    String vcdSessHref;

    // The vCloud API VDC Href
    String vdcHref;

    // The token to be used for subsequent requests to the vCloud API
    String vcdToken;

    /**
     * Creates an org.apache.http.client.methods.HttpGet object adding in the http headers for a
     * vCloud GET
     * 
     * @param url
     *            the url to make the get request to
     * @return the instance of HttpGet populated with the correct headers
     */
    public HttpGet get(String url, DefaultSampleCommandLineOptions options) {
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader(HttpHeaders.ACCEPT, SampleConstants.APPLICATION_PLUS_XML_VERSION
                + options.vcdVersion);
        httpGet.setHeader(SampleConstants.VCD_AUTHORIZATION_HEADER, vcdToken);

        return httpGet;
    }

    /**
     * Creates an org.apache.http.client.methods.HttpPut object adding in the http headers for a
     * vCloud PUT
     * 
     * @param url
     *            the url to make the get request to
     * @return the instance of HttpGet populated with the correct headers
     */
    public HttpPut put(String url, DefaultSampleCommandLineOptions options) {
        HttpPut httpPut = new HttpPut(url);
        httpPut.setHeader(HttpHeaders.ACCEPT, SampleConstants.APPLICATION_PLUS_XML_VERSION
                + options.vcdVersion);
        httpPut.setHeader(SampleConstants.VCD_AUTHORIZATION_HEADER, vcdToken);

        return httpPut;
    }

    /**
     * Creates an org.apache.http.client.methods.HttpPost object adding in the http headers for a
     * vCloud POST
     * 
     * @param url
     *            the url to make the post request to
     * @return the instance of HttpPost populated with the correct headers
     */
    public HttpPost post(String url, DefaultSampleCommandLineOptions options) {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader(HttpHeaders.ACCEPT, SampleConstants.APPLICATION_PLUS_XML_VERSION
                + options.vcdVersion);
        httpPost.setHeader(SampleConstants.VCD_AUTHORIZATION_HEADER, vcdToken);

        return httpPost;
    }
}