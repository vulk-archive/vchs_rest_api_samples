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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import com.vmware.ares.pub.api.ComputeType;
import com.vmware.ares.pub.api.LinkType;
import com.vmware.ares.pub.api.ServiceListType;
import com.vmware.ares.pub.api.ServiceType;
import com.vmware.ares.pub.api.VdcReferenceType;
import com.vmware.vcloud.api.rest.schema.QueryResultRecordType;
import com.vmware.vcloud.api.rest.schema.QueryResultRecordsType;
import com.vmware.vcloud.api.rest.schema.QueryResultVAppTemplateRecordType;

/**
 * VDCListSample
 * 
 * This sample will retrieve all the VDC's across all regions in both VPC and DC and also list the 
 * templates available in the VMware System Catalog in each VDC.
 * 
 * Parameters:
 * 
 * url              [required] : url of the vCHS web service.
 * username         [required] : username for the vCHS authentication.
 * password         [required] : password for the  vCHS authentication.
 * vchsversion      [required] : version of vCHS API.
 * vcloudversion    [required] : version of vCloud API.
 * 
 * Argument Line:
 * 
 * Displays to the console all the VDC's across all regions in both VPC and DC and available 
 * vApp templates under each VDC.
 * 
 * --url [vchs webservice url] --username [vchs username] --password [vchs password] 
 * --vchsversion [vchs version] --vcloudversion [vcloud version]
 */
public class VDCListSample {
    private Vchs vchs = null;
    private DefaultSampleCommandLineOptions options = null;

    /**
     * @param args
     *            any arguments passed by the command line, if none, defaults are used where
     *            applicable.
     */
    public static void main(String[] args) {
        // Creating an instance of this sample
        VDCListSample sample = new VDCListSample();
        sample.run(args);
    }

    /**
     * Called by the static main method on the instance of this class with the
     * command line args array.
     * 
     * @param args the arguments passed on the command line
     */
    private void run(String[] args){
        options = new DefaultSampleCommandLineOptions();

        // process arguments
        options.parseOptions(args);

        // Log in to vCHS API, getting a session in response if login is successful
        System.out.print("\nConnecting to vCHS...");

        // Instance of Vchs for this sample
        vchs = new Vchs();

        // Log in to vCHS, passing the command line arguments
        if (vchs.login(options)) {
            System.out.println("Success\n");

            // Retrieve the collection of compute services which can be of type dedicated cloud or vpc
            // and has VDC in it.
            System.out.print("Retrieving compute services...");

            // collection of compute services
            Collection<ServiceType> computeServices = getComputeServices();

            System.out.println("Success\n");

            // Collection of VDC
            Collection<VdcServiceReference> allVdcs = new ArrayList<VdcServiceReference>();

            // Loop through each of the compute service
            for (ServiceType service : computeServices) {
                // For each compute service, get the collection of VDC reference
                Collection<VdcReferenceType> vdcs = getVdcRefs(service, options.vchsVersion,
                        vchs.vchsToken);

                // vdcs stores the collection of VDC reference and compute service in which they fall
                VdcServiceReference ref = new VdcServiceReference(vdcs, service);
                allVdcs.add(ref);
            }

            // Should be > 0, but be sure
            if (allVdcs.size() > 0) {
                // Loop through each returned VDC and print out its name to the console
                System.out.println("Available VDCs");
                System.out.println("----------------\n");

                // Loop through each of the VdcServiceReference containing compute service and list of VDC
                // reference in it
                for (VdcServiceReference ref : allVdcs) {
                    Collection<VdcReferenceType> vdcs = ref.getVdcs();
                    ServiceType service = ref.getService();

                    // Loop through each of the VDC
                    for (VdcReferenceType vdc : vdcs) {
                        // Make sure VDC is active before listing templates for it
                        if(vdc.getStatus().equalsIgnoreCase("ACTIVE")){
                            System.out.println(vdc.getName() + "\t" + service.getServiceType() + "\t"
                                    + service.getRegion());

                            // To display the name of vApp Template available under VDC
                            listSystemTemplates(vdc);
                            System.out.println();
                        }
                    }
                }
            }
        }
    }

    /**
     * This method retrieves a vCloud API EndPoint to get access to its organization, then its 
     * catalog and finally display names of vApp templates available to the console
     * 
     * @param vdc
     *            the reference to VDC for which the available vApp template to be displayed
     */
    private void listSystemTemplates(VdcReferenceType vdc) {
        String vcloudSessionHref = null;

        // Retrieve the List of Links associated with VDC Reference
        List<LinkType> vdcLinks = vdc.getLink();

        // Iterate through the list of links associated VDC Reference
        for (LinkType link : vdcLinks) {
            if (link.getType().equals("application/xml;class=vnd.vmware.vchs.vcloudsession")) {
                vcloudSessionHref = link.getHref();

                // Found it, break out of loop
                break;
            }
        }

        System.out.println();
        System.out.println("  Available templates");
        System.out.println("  -------------------");

        if (null != vcloudSessionHref){
            // Retrieve the vCloud API EndPoint for the VDC.
            Vcd vcd = HttpUtils.getVCDEndPoint(vchs, options, vcloudSessionHref);

            // getQueryResults() will use the vCloud Query API service and get a list of all the system
            // templates.
            //
            // About filtering templates:
            //   filter=isPublished==true  : retrieve only vCHS system templates.
            //   filter=isPublished==false : retrieve user uploaded templates.
            //   (without isPublished filter query parameter) : retrieve all templates.
            // Example: type=vAppTemplate&filter=isPublished==true
            //
            QueryResultRecordsType queryResults = HttpUtils.getQueryResults(HttpUtils.getHostname(vcd.vdcHref), "type=vAppTemplate", options, vcd.vcdToken);

            if (null != queryResults && queryResults.getRecord().size() > 0) {
                List<JAXBElement<? extends QueryResultRecordType>> records = queryResults.getRecord();
                for(JAXBElement<? extends QueryResultRecordType> record : records) {
                // We should have only one record with the name matching templateName
                    QueryResultVAppTemplateRecordType qrrt = (QueryResultVAppTemplateRecordType) record.getValue();
                    // Print the name of the system template to the console
                    System.out.println("  " + qrrt.getName());
                }
            } else {
                System.out.println("  None");
            }
        } else {
            System.out.println("  None");
        }
    }

    /**
     * Retrieves a collection of compute services
     * 
     * @param session
     *            the vCHS API login session
     * 
     * @return Collection of compute services
     */
    private Collection<ServiceType> getComputeServices() {
        // Collection of only compute service types
        Collection<ServiceType> computeServices = null;

        // invoke the serviceList API
        HttpResponse response = HttpUtils.httpInvoke(vchs.get(vchs.vchsServiceListHref, options));

        // Make sure the response status is 200 OK
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new RuntimeException("Failed : HTTP error code : "
                    + response.getStatusLine().getStatusCode());
        }

        // Parse the response entity to get the vCHS services
        ServiceListType serviceList = HttpUtils.unmarshal(response.getEntity(),
                ServiceListType.class);
        List<ServiceType> services = serviceList.getService();
        computeServices = new ArrayList<ServiceType>(services.size());

        // Filter the return services to retrieve only the compute services
        // E.g. compute:dedicated or compute:vpc
        for (ServiceType service : services) {
            if (service.getHref() != null
                    && service.getServiceType().startsWith(SampleConstants.COMPUTE_SERVICE_TYPE)) {
                computeServices.add(service);
            }
        }

        return computeServices;
    }

    /**
     * This method will retrieve a collection of VdcReferenceType instances.
     * 
     * @param computeService
     *            the compute service type to get the vCloud API href from
     * @param version
     *            the vCHS API version to make requests against
     * @param authorization
     *            the vCHS API authorization token
     * 
     * @return Collection of VDC References in the Service
     */
    private Collection<VdcReferenceType> getVdcRefs(ServiceType computeService, String version,
            String authorization) {
        // Collection of References to VDC
        Collection<VdcReferenceType> vdcRefs = null;

        // Get href to compute service
        String href = computeService.getHref();

        // Invoke the computeService API
        HttpResponse computeTypeResponse = HttpUtils.httpInvoke(vchs.get(href, options));

        // Make sure the response status is 200 OK
        if (computeTypeResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new RuntimeException("Failed : HTTP error code : "
                    + computeTypeResponse.getStatusLine().getStatusCode());
        }

        // Get the compute object which contains the collection of VDC references.
        ComputeType compute = HttpUtils.unmarshal(computeTypeResponse.getEntity(),
                ComputeType.class);
        vdcRefs = compute.getVdcRef();

        return vdcRefs;
    }

    /**
     * Encapsulates a collection of VDC's with the service they fall under
     */
    private final class VdcServiceReference{
        private Collection<VdcReferenceType> vdcs;
        private ServiceType service;

        public VdcServiceReference(Collection<VdcReferenceType> vdcs, ServiceType service) {
            this.vdcs = vdcs;
            this.service = service;
        }

        public Collection<VdcReferenceType> getVdcs() {
            return vdcs;
        }

        public ServiceType getService() {
            return service;
        }
    }
}