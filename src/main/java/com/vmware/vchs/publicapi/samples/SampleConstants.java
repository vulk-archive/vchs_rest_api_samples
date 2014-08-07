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

/**
 * This class provides common constants that will be used in samples.
 */
public final class SampleConstants {
    /*
     * Prevent this class from being instantiated
     */
    private SampleConstants() {
    }

    /*
     * Content-Type header for vCHS API
     */
    static final String APPLICATION_XML_VERSION = "application/xml;version=";

    /*
     * Content-Type header for VCD API
     */
    static final String APPLICATION_PLUS_XML_VERSION = "application/*+xml;version=";

    /*
     * Content-Type for vCHS API ServiceList
     */
    static final String APPLICATION_XML_SERVICE_LIST = "application/xml;class=vnd.vmware.vchs.servicelist";

    /*
     * vCHS Authorization header string
     */
    static final String VCHS_AUTHORIZATION_HEADER = "x-vchs-authorization";

    /*
     * VCD Authorization header string
     */
    static final String VCD_AUTHORIZATION_HEADER = "x-vcloud-authorization";

    /*
     * Content-Type header for VCD session
     */
    static final String APPLICATION_XML_VCD_SESSION = "application/xml;class=vnd.vmware.vchs.vcloudsession";

    /*
     * Default vCHS Public API entry point
     */
    static final String DEFAULT_HOSTNAME = "vchs.vmware.com";

    /*
     * Default vCHS Public API Version to make calls to
     */
    static final String DEFAULT_VCHS_VERSION = "5.6";

    /*
     * Default VCD API Version to make calls to
     */
    static final String DEFAULT_VCD_VERSION = "5.6";

    /*
     * vCHS Public API Sessions url
     */
    static final String SESSION_URL = "/api/vchs/sessions";

    /*
     * VCloud Public API Versions url
     */
    static final String VERSION_URL = "/api/versions";

    /*
     * The string value representing a Compute Service Type
     */
    static final String COMPUTE_SERVICE_TYPE = "compute";

    /*
     * The string value representing an Org
     */
    static final String ORG = "application/vnd.vmware.vcloud.org+xml";

    static final String CONTENT_TYPE_EDGE_GATEWAY = "application/vnd.vmware.admin.edgeGatewayServiceConfiguration+xml";
}