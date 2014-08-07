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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import com.vmware.vcloud.api.rest.schema.AvailableNetworksType;
import com.vmware.vcloud.api.rest.schema.DeployVAppParamsType;
import com.vmware.vcloud.api.rest.schema.InstantiateVAppTemplateParamsType;
import com.vmware.vcloud.api.rest.schema.InstantiationParamsType;
import com.vmware.vcloud.api.rest.schema.LinkType;
import com.vmware.vcloud.api.rest.schema.NetworkConfigSectionType;
import com.vmware.vcloud.api.rest.schema.NetworkConfigurationType;
import com.vmware.vcloud.api.rest.schema.NetworkConnectionSectionType;
import com.vmware.vcloud.api.rest.schema.ObjectFactory;
import com.vmware.vcloud.api.rest.schema.OrgVdcNetworkType;
import com.vmware.vcloud.api.rest.schema.QueryResultRecordType;
import com.vmware.vcloud.api.rest.schema.QueryResultRecordsType;
import com.vmware.vcloud.api.rest.schema.QueryResultVAppTemplateRecordType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.TaskType;
import com.vmware.vcloud.api.rest.schema.TasksInProgressType;
import com.vmware.vcloud.api.rest.schema.VAppNetworkConfigurationType;
import com.vmware.vcloud.api.rest.schema.VAppTemplateType;
import com.vmware.vcloud.api.rest.schema.VAppType;
import com.vmware.vcloud.api.rest.schema.VdcType;
import com.vmware.vcloud.api.rest.schema.VmType;
import com.vmware.vcloud.api.rest.schema.ovf.CimBoolean;
import com.vmware.vcloud.api.rest.schema.ovf.CimString;
import com.vmware.vcloud.api.rest.schema.ovf.MsgType;
import com.vmware.vcloud.api.rest.schema.ovf.RASDType;
import com.vmware.vcloud.api.rest.schema.ovf.SectionType;
import com.vmware.vcloud.api.rest.schema.ovf.VirtualHardwareSectionType;

/**
 * VMCreateSample
 * 
 * This sample will create a VM from a vApp template in the specified VDC, power on
 * the vApp, and print out the IPs for each VM in the vApp. A vCHS vApp template name is used to
 * specify the vApp to deploy. For example one of the templates in the vCHS System Catalog can be
 * used. This vApp template name is specified by the command line option -vchstemplatename. The name of
 * the deployed vApp is specified by the -targetvappname. The command line option -vdcname is used
 * to specify the VDC where the vApp will be deployed. The deployed vApp will be connected to the
 * network specified by the -orgnet option.
 *
 * Templates typically do not have any network configuration. Because of this, the process to
 * connect a VM (which essentially duplicates the settings of the template it is created from) to
 * the -orgnet network requires a couple of extra steps. The vApp is first created, which also
 * creates any Vms the template defines. The network configuration updates must be done while the 
 * vApp (and subsequently Vm's) are in a powered off state. After the vApp creation task is 
 * complete, the vApp network configuration will be updated. It is possible to provide the network 
 * configuration details during the create step, but because each child Vm may initially derive 
 * it's network configuration details from the template it's created from, it's possible the 
 * template network details do not match the details provided during the vApp create. Therefore the 
 * vApp is created without network details initially to avoid each Vm from failing to be created 
 * due to different network details then the vApp.
 * 
 * Once the vApp (and Vm's) are created, the vApp network details must be updated before any of the
 * Vm children can be modified. After the vApp network details have been updated, each child
 * Vm of the vApp will need it's network details updated to match that of the vApp so that the Vm
 * network can connect to the vApp network properly. This is why each Vm's network details are
 * modified too.
 * 
 * Steps: 
 *   1) Log in to vCHS
 *   2) Get the list of the compute services
 *   3) Find the VDC within the compute services
 *   4) Get the VDC's session link from vCHS
 *   5) Get a vCloud session for the VDC
 *   6) Get the vApp template from vCloud (using query service)
 *   7) Get the vApp instantiate link from the VDC
 *   8) Initialize the vApp on the VDC
 *   9) Get the vApp information to pull the VM children from
 *  10) Put the network details to the vApp with the network name specified with the command line
 *      option -orgnet
 *  11) Put the network information to each child VM with the same network details as the vApp
 *      to connect each Vm to the vApp network
 *  11) Deploy the vApp (which will also turn on each child Vm)
 *  12) Wait for the vApp to be powered on
 *  13) Once the vApp is ready, retrieve each child Vm's IP information and display it
 *  
 * Parameters:
 *   url [required] : url of the vCHS web service. 
 *   username [required] : username for the vCHS authentication.
 *   password [required] : password for the vCHS authentication.
 *   vchsversion [required] : version of vCHS API.
 *   vcloudversion [required] : version of vCloud API.
 *   vdcname [required] : name of the VDC.
 *   targetvappname [required] : name of the vApp to be created.
 *   orgnet [required] : name of the org network
 *   templatename [required] : name of the template to create the VM from
 *  
 * Argument Line: Creates and turns on a VM using the vApp template on a particular VDC and
 * displays its internal ip it is assigned.
 * 
 * --url [vchs webservice url] --username [vchs username] --password [vchs password]
 * --vchsversion [vchs version] --targetvappname [vapp name] --vdcname [vdc name]
 * --vcloudversion [vcloud version] --orgnet [org network name] --vchstemplatename [template name]
 */
public class VMCreateSample {
    private static final String VM_NETWORK_URL = "/networkConnectionSection/";
    private static final int SUCCESS = 4;
    private static final int FAIL = -1;

    private Vchs vchs = null;
    private Vcd vcd = null;
    private VMCreateCommandLineOptions options = null;

    /**
     * @param args
     *            any arguments passed by the command line, if none, defaults are used where
     *            applicable.
     */
    public static void main(String[] args) {
        VMCreateSample sample = new VMCreateSample();
        sample.run(args);
    }

    /**
     * Called by the static main method on the instance of this class with the command line args
     * array.
     * 
     * @param args
     *            the arguments passed on the command line
     */
    private void run(String[] args) {
        // Create instance of CommandLineOptions for use in this sample
        options = new VMCreateCommandLineOptions();
        options.parseOptions(args);

        System.out.print("\nConnecting to vCHS...");

        // Instance of Vchs for this sample
        vchs = new Vchs();

        // Log in to vCHS, passing the command line arguments
        if (vchs.login(options)) {
            System.out.println("Success\n");

            // Retrieve a vcloudSession for the VDC specified on the command line.
            // options.vdcName contains the name of a VDC corresponding to the retrieved vCloud
            // session
            String vcdSessionHref = vchs.getVCloudDSessionHref(options);

            // Retrieve the vCloud API EndPoint for the VDC.
            vcd = HttpUtils.getVCDEndPoint(vchs, options, vcdSessionHref);

            // Retrieve the VCD hostname from the VDC href
            String vcdBaseUrl = HttpUtils.getHostname(vcd.vdcHref);

            // Retrieve the VDC object from vCloud
            VdcType vdc = getVdc(vcd.vdcHref);

            // Retrieve the VAppTemplateType that matches the command line arg --templatename
            // passed in
            VAppTemplateType template = getVAppTemplate(vcdBaseUrl);

            // Retrieve the url to perform initializevApp method
            String instantiateHref = getInstantiateVAppLink(vdc);

            // Initialize the vApp template using the vApp template
            VAppType vApp = createVApp(instantiateHref, template.getHref(), vdc);

            // Wait for the vApp creation to complete by monitoring the vApp task
            System.out.print("Waiting for vApp creation to finish...");
            waitForTasks(vApp.getTasks());
            System.out.println("Success\n");

            // After it's completed and it's task is finished, GET the vApp again to retrieve it's
            // updated state, which will add the VM as a child and more links among other things.
            System.out.print("Refreshing vApp state...");
            vApp = getVApp(vApp);
            System.out.println("Success\n");

            // Find the Vm that was created as part of the vApp
            System.out.print("Looking for vApp child Vm...");
            VmType vm = getVmFromVApp(vApp);
            System.out.println("Found\n");

            // Update the ovf:VirtualHardwareSection of the Vm to connect it to
            // the vApp network. Use the Vdc available networks to find the matching network,
            // and if found, retrieve the network details
            System.out.print("Looking up vApp VDC network details...");
            OrgVdcNetworkType network = getVAppVdcNetwork(vcdBaseUrl, vdc);
            System.out.println("Found\n");

            // Update the vApp NetworkConfigSection to use the options.networkName
            // This is to cover the scenario where a template used to create a vApp already
            // has a network associated with it. Templates will often not have a network
            // associated with the template, unless it is provisioned for a specific VDC which
            // already has an established network.
            // NOTE: The vApp must be updated first, then it's children Vms.

            // Update the vApp with same network details as the Vms
            System.out.print("Updating the vApp network...");
            TaskType vAppNetworkUpdateTask = updateVAppNetwork(vApp, vdc);
            waitForTaskCompletion(vAppNetworkUpdateTask);
            System.out.println("Success\n");

            // Update the ovf:VirtualHardwareSeciont with the network details for each Vm child
            // of the vApp, matching the network name using the command line options.networkName
            System.out.print("Updating vApp Vm network...");
            TaskType networkUpdateTask = updateVMWithNetworkDetails(vm);
            waitForTaskCompletion(networkUpdateTask);
            System.out.println("Success\n");

            // Now we need to power on the vApp, and wait on it powering on
            System.out.print("Deploying and powering on vApp...");
            TaskType deployTask = deploy(vApp);
            waitForTaskCompletion(deployTask);
            System.out.println("Success\n");

            // Get the IP info for the deployed vApp
            System.out.println("Displaying vApp " + vApp.getName() + " IP details:\n");
            displayIPDetails(vApp);
            System.out.println("Done");
        }
    }

    /**
     * This method will update the passed in vApp network by adding an additional
     * NetworkConfigSection that uses the command line options.networkName. It makes a PUT call
     * to the edit link of the NetworkConfigSection section of the vApp passed in. The
     * passed in VdcType is used to look up the parent network which is required for the update
     * process of the vApp NetworkConfigSection.
     * 
     * NOTE: This step, updating the vApp network section, must be done BEFORE any of the vApp's
     * children Vm's network sections are updated.
     * 
     * @param vApp the vApp instance to update network configuration for
     * @param vdc the VDC this vApp is deployed to
     * @return a TaskType instance if successful, otherwise an exception is thrown
     */
    private TaskType updateVAppNetwork(VAppType vApp, VdcType vdc) {
        List<JAXBElement<? extends SectionType>> sections = vApp.getSection();
        for(JAXBElement<? extends SectionType> section : sections) {
            if(section.getName().toString().contains("NetworkConfigSection")) {
                NetworkConfigSectionType ncst = (NetworkConfigSectionType)section.getValue();
                // Find the EDIT link to make a PUT call to to update the network config info
                List<LinkType> links = ncst.getLink();
                String editHref = null;
                for(LinkType link : links) {
                    if (link.getRel().equalsIgnoreCase("edit")) {
                        editHref = link.getHref();
                        break;
                    }
                }

                if (null != editHref) {
                    VAppNetworkConfigurationType vappNet = new VAppNetworkConfigurationType();

                    // Use the network name passed on the command line, the same name that will
                    // be applied to each vApp Vm child network configuration so they match
                    vappNet.setNetworkName(options.networkName);

                    // Newly constructed network configuration
                    NetworkConfigurationType networkConfiguration = new NetworkConfigurationType();
                    ReferenceType networkReference = new ReferenceType();

                    // Get the parent network that the VDC refers to
                    networkReference.setHref(getParentNetworkHrefFromVdc(vdc));
                    networkConfiguration.setParentNetwork(networkReference);

                    // hard coded.. 'bridged', 'natRouted' or 'isolated'
                    networkConfiguration.setFenceMode("bridged");
                    vappNet.setConfiguration(networkConfiguration);

                    // Add the newly configured network to the existing vApp configuration
                    ncst.getNetworkConfig().add(vappNet);

                    // Make the PUT call to update the vApp network configuration
                    HttpPut updateVAppNetwork = vcd.put(editHref, options);
                    OutputStream os = null;
                    ObjectFactory objectFactory = new ObjectFactory();
                    JAXBElement<NetworkConfigSectionType> networkConfigSectionType = objectFactory.createNetworkConfigSection(ncst);

                    JAXBContext jaxbContexts = null;
                    try {
                        jaxbContexts = JAXBContext.newInstance(NetworkConfigSectionType.class);
                    } catch (JAXBException ex) {
                        throw new RuntimeException("Problem creating JAXB Context: ", ex);
                    }

                    try {
                        javax.xml.bind.Marshaller marshaller = jaxbContexts.createMarshaller();
                        marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                        os = new ByteArrayOutputStream();
                        // Marshal the object via JAXB to XML
                        marshaller.marshal(networkConfigSectionType, os);
                    } catch (JAXBException e) {
                        throw new RuntimeException("Problem marshalling VirtualHardwareSection", e);
                    }

                    // Set the Content-Type header for NetworkConfigSection
                    ContentType contentType = ContentType.create(
                            "application/vnd.vmware.vcloud.networkConfigSection+xml", "ISO-8859-1");
                    StringEntity update = new StringEntity(os.toString(), contentType);
                    updateVAppNetwork.setEntity(update);

                    // Invoke the HttoPut to update the VirtualHardwareSection of the Vm
                    HttpResponse response = HttpUtils.httpInvoke(updateVAppNetwork);

                    if(response.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED) {
                        TaskType taskType = HttpUtils.unmarshal(response.getEntity(),  TaskType.class);
                        return taskType;
                    }
                }
            }
        }

        throw new RuntimeException("Problem trying to update vApp " + vApp.getName() + " network config section with " + options.networkName + " as the network name");
    }

    /**
     * This will use the passed in vm  to find the VirtualHardwareSection and modify it to attach
     * the Vm network to the vApp network the Vm is a child of. The first step is to search the
     * Vm section types for VirtualHardwareSection. If found, look in the attributes of the section
     * to find the VirtualHardwareSection Href value. The VirtualHardwareSection found in the Vm
     * has a number of extra LinkType links that can not be part of the request body when the PUT
     * call to update the network settings is made. Therefore, the Href is used to first GET the
     * VirtualHardwareSection again which responds without the links so it can be used to PUT back
     * to the same Href to update the Vm network settings. With the newly acquired 
     * VirtualHardwareSection, a search through the items is done to find the network item. This
     * is denoted by a resource type value of "10". If found the command line options.networkName
     * is set as the value of the connection, and the value POOL is assigned to the ipAddressingMode
     * attribute. With those values set, the updated VirtualHardwareSection is then sent via a PUT
     * request to change the Vm network settings.
     * 
     * @param vm the Vm to change network settings on
     */
    private TaskType updateVMWithNetworkDetails(VmType vm) {
        // the Href to use for GET and PUT calls for the VirtualHardwareSection
        String hardwareHref = null;
        // With the VDC network details, we can now update the Vm network section to connect the Vm to the vApp network
        for(JAXBElement<? extends SectionType> st : vm.getSection()) {
            if (st.getName().toString().contains("VirtualHardwareSection")) {
                VirtualHardwareSectionType hardware = (VirtualHardwareSectionType)st.getValue();

                // Try to find the Href attribute which is used for GET and PUT
                Map<QName, String> map = hardware.getOtherAttributes();
                Set<QName> keys = map.keySet();
                for(QName key : keys){
                    if (key.toString().endsWith("href")) {
                        hardwareHref = map.get(key);
                        break;
                    }
                }

                // Make sure VirtualHardwareSection href was found. This is used for the GET below
                // and PUT to later update the network settings of the Vm.
                if (null != hardwareHref) {
                    // It's necessary to GET the VirtualHardwareSection again because the current
                    // hardware variables from the Vm contains links that can not be sent as part
                    // of the PUT body. This GET call will only get the details without the links
                    // that the Vm section provides.
                    HttpResponse response = HttpUtils.httpInvoke(vcd.get(hardwareHref, options));
                    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                        hardware = HttpUtils.unmarshal(response.getEntity(),  VirtualHardwareSectionType.class);

                        // Find the RASDType that has a ResourceType of 10, which indicates the
                        // VMs network info
                        for(RASDType rasType : hardware.getItem()) {
                            if (rasType.getResourceType().getValue().equals("10")) {
                                CimBoolean c = new CimBoolean();
                                c.setValue(true);

                                rasType.setAutomaticAllocation(c);
                                // Get the first CimString
                                CimString cs = rasType.getConnection().get(0);

                                // Set the network name
                                cs.setValue(options.networkName);

                                // Look in the list of attributes for the ip addressing mode
                                map = cs.getOtherAttributes();
                                keys = map.keySet();
                                for(QName key : keys) {
                                    if (key.toString().endsWith("ipAddressingMode")) {
                                        // Set it to POOL
                                        map.put(key,  "POOL");
                                        break;
                                    }
                                }

                                break;
                            }
                        }
                    }

                    // Now do a PUT with update data
                    com.vmware.vcloud.api.rest.schema.ovf.ObjectFactory objectFactory = new com.vmware.vcloud.api.rest.schema.ovf.ObjectFactory();
                    JAXBElement<VirtualHardwareSectionType> hardwareSection = objectFactory.createVirtualHardwareSection(hardware);

                    JAXBContext jaxbContexts = null;
                    try {
                        jaxbContexts = JAXBContext.newInstance(VirtualHardwareSectionType.class);
                    } catch (JAXBException ex) {
                        throw new RuntimeException("Problem creating JAXB Context: ", ex);
                    }

                    // Create HttpPut request to update the VirtualHardwareSection
                    HttpPut updateVmNetwork = vcd.put(hardwareHref, options);
                    OutputStream os = null;

                    try {
                        javax.xml.bind.Marshaller marshaller = jaxbContexts.createMarshaller();
                        marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                        os = new ByteArrayOutputStream();
                        // Marshal the object via JAXB to XML
                        marshaller.marshal(hardwareSection, os);
                    } catch (JAXBException e) {
                        throw new RuntimeException("Problem marshalling VirtualHardwareSection", e);
                    }

                    // Set the Content-Type header for VirtualHardwareSection
                    ContentType contentType = ContentType.create(
                            "application/vnd.vmware.vcloud.virtualHardwareSection+xml", "ISO-8859-1");
                    StringEntity update = new StringEntity(os.toString(), contentType);
                    updateVmNetwork.setEntity(update);

                    // Invoke the HttoPut to update the VirtualHardwareSection of the Vm
                    response = HttpUtils.httpInvoke(updateVmNetwork);

                    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED){
                        // Update was good, return the TaskType
                        TaskType taskType = HttpUtils.unmarshal(response.getEntity(),  TaskType.class);
                        return taskType;
                    }

                    break;
                }
            }
        }

        throw new RuntimeException("Could not update Vm VirtualHardwareSection");
    }

    /**
     * Uses the passed in VdcType to search the available networks of the Vdc for a matching
     * network with the options.networkName command line argument. If one is found, a GET
     * request is made to retrieve the network details and returned.
     * 
     * @param vcdBaseUrl the vcd href
     * @param vdc the Vdc containing the available networks to search
     * @return the matched OrgVdcNetworkType instance
     */
    private OrgVdcNetworkType getVAppVdcNetwork(String vcdBaseUrl, VdcType vdc){
        AvailableNetworksType l = vdc.getAvailableNetworks();
        List<ReferenceType> networks = l.getNetwork();

        for(ReferenceType rt : networks){
            if (rt.getName().equalsIgnoreCase(options.networkName)){
                HttpResponse response = HttpUtils.httpInvoke(vcd.get(rt.getHref(), options));

                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                    OrgVdcNetworkType network = HttpUtils.unmarshal(response.getEntity(), OrgVdcNetworkType.class);
                    return network;
                }
            }
        }

        throw new RuntimeException("Could not find a matching Org network for the Vdc");
    }

    /**
     * Searches through the passed in vApp children element for a VmType Vm that matches the name
     * of the command line options.vappName. If found it is returned, otherwise a RuntimeException
     * is thrown. 
     * 
     * @param vApp the vApp to search for a matching Vm
     * @return the VmType instance
     */
    private VmType getVmFromVApp(VAppType vApp){
        // Get the status of initialization operation and IP details
        if (null != vApp.getChildren()) {
            List<VmType> vms = vApp.getChildren().getVm();

            for(VmType vm : vms){
                List<LinkType> links = vm.getLink();

                for(LinkType link : links){
                    // If there is a rel="up", we use that to get the ID and match it to the
                    // passed in vApp id
                    if (link.getRel().equalsIgnoreCase("up")){
                        // make GET request to get the up vApp to compare it to the
                        // passed in vApp
                        HttpResponse response = HttpUtils.httpInvoke(vcd.get(vApp.getHref(), options));
                        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                            VAppType upVApp = HttpUtils.unmarshal(response.getEntity(), VAppType.class);
                            if(upVApp.getName().equalsIgnoreCase(options.vappName)){
                                return vm;
                            }
                        }
                    }
                }

                if (vm.getName().equalsIgnoreCase(options.vappName)){
                    return vm;
                }
            }
        }

        throw new RuntimeException("Could not find a matching Vm in vApp " + vApp.getName());
    }

    /**
     * Uses the passed in vAppType to make a GET request to the vCloud API to retrieve an updated
     * copy of itself. This is typically done after a task completes on the vApp which may result
     * in a state change of the vApp, such as creating a vApp.
     * 
     * @param vApp the vApp to refresh from the API
     * @return a new instance of VAppType
     */
    private VAppType getVApp(VAppType vApp){
        HttpResponse response = HttpUtils.httpInvoke(vcd.get(vApp.getHref(), options));
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
            VAppType updatedVApp = HttpUtils.unmarshal(response.getEntity(), VAppType.class);
            return updatedVApp;
        }
        
        throw new RuntimeException(vApp.getName() + " with Href " + vApp.getHref() + " are invalid.");
    }

    /**
     * This will retrieve the VdcType from vCloud via the provided href
     * 
     * @param href
     *            the vCloud href
     * @return a VdcType instance
     */
    private VdcType getVdc(String href) {
        // Request the VDC details using the vCloud API End point for the VDC
        HttpResponse response = HttpUtils.httpInvoke(vcd.get(href, options));

        // Make sure response status is 200
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new RuntimeException("\nFailed : HTTP error code : "
                    + response.getStatusLine().getStatusCode());
        }

        VdcType vdc = HttpUtils.unmarshal(response.getEntity(), VdcType.class);

        if (null == vdc) {
            throw new RuntimeException("Problem retreiving the VdcType from: " + href);
        }

        return vdc;
    }

    /**
     * This method will search through the VdcType instance passed in to find the network that
     * matches the command line argument provided --orgnetwork. This is set on the VM to be created
     * as the parent network.
     * 
     * @param vdc
     *            the VdcType instance to find the network within.
     */
    private String getParentNetworkHrefFromVdc(VdcType vdc) {
        AvailableNetworksType networks = vdc.getAvailableNetworks();
        List<ReferenceType> refs = networks.getNetwork();
        String parentNetworkHref = null;

        for (ReferenceType ref : refs) {
            // For each ReferenceType, check if it is the same name as the options.networkName
            // (command line option --networkname)
            if (ref.getName().equalsIgnoreCase(options.networkName)) {
                // Found it, break from loop
                parentNetworkHref = ref.getHref();
                break;
            }
        }

        if (null == parentNetworkHref) {
            throw new RuntimeException("Could not find parent network for Vdc: " + vdc.getName());
        }

        return parentNetworkHref;
    }

    /**
     * Retrieves the VAppTemplateType from the vCloud using the provided baseVdcUrl. It does so by
     * using the vCloud Query service API to search for vAppTemplate and filter on the name of the
     * template, provided by the templateName passed in.
     * 
     * @param baseVdcUrl
     *            the base url to the vCloud API to make REST calls to
     * @return the instance of VAppTemplateType if found, null if not
     */
    private VAppTemplateType getVAppTemplate(String baseVcdUrl) {
        System.out.print("Searching for template " + options.templateName + "...");

        // Query the vCloud Query API to search for a vAppTemplate matching the
        // options.templateName (command line option --templatename)
        QueryResultRecordsType queryResults = HttpUtils.getQueryResults(baseVcdUrl,
                "type=vAppTemplate&filter=name==" + options.templateName, options, vcd.vcdToken);

        List<JAXBElement<? extends QueryResultRecordType>> rslt = queryResults.getRecord();

        VAppTemplateType vat = null;

        // We should have only one record with the name matching templateName
        if (rslt.size() == 1) {
            QueryResultVAppTemplateRecordType qrrt = (QueryResultVAppTemplateRecordType) rslt
                    .get(0).getValue();
            String templateHref = qrrt.getHref();

            // invoke the GET request to the template href to get the VAppTemplateType
            HttpGet httpGet = vcd.get(templateHref, options);
            HttpResponse response = HttpUtils.httpInvoke(httpGet);

            // make surethe status is 200 OK
            if (null != response && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                // unmarshal the response entity into a VAppTemplateType
                vat = HttpUtils.unmarshal(response.getEntity(), VAppTemplateType.class);
                System.out.println("found.\n");
            }
        }

        if (null == vat) {
            throw new RuntimeException("Template not found: " + options.templateName);
        }

        return vat;
    }

    /**
     * This method is to get the href for instantiateVApp
     * 
     * @param vdcHref
     *            the href for the vCloud API End point for the VDC.
     * @return href to perform instantiate vApp action
     */
    private String getInstantiateVAppLink(VdcType vdc) {
        // List of links associated with the VDC
        List<com.vmware.vcloud.api.rest.schema.LinkType> linklist = vdc.getLink();
        String instantiateHref = null;

        // Iterating through the Links to find the instantiateVAppTemplate action Link
        for (com.vmware.vcloud.api.rest.schema.LinkType link : linklist) {
            if (link.getType().contains("instantiateVAppTemplate")) {
                instantiateHref = link.getHref();
                break;
            }
        }

        if (null == instantiateHref) {
            throw new RuntimeException("Could not find instantiateHref for VDC: " + vdc.getName());
        }

        return instantiateHref;
    }

    /**
     * This method will initialize and deploy a vApp using the instantiationHref, vappTemplateHref,
     * vmNetworkName and vdc provided.
     * 
     * @param instantiateHref
     *            the href to instantiatevApp action
     * @param vappTempalteHref
     *            the href to the vApp Template to be used to create vApp
     * @param vmNetworkName
     *            the network name for VM
     * @param VdcType
     *            the vdc
     * @return VappType if the initialize vapp succeeds, null otherwise
     */
    private VAppType createVApp(final String instantiateHref, final String vappTempalteHref, VdcType vdc) {
        System.out.print("Attempting to create vApp...");
        ReferenceType vappReference = new ReferenceType();
        vappReference.setHref(vappTempalteHref);

        // Create an InstantiateVAppTemplateParamsType object and initialize it
        InstantiateVAppTemplateParamsType instvApp = new InstantiateVAppTemplateParamsType();

        // Set the name of vApp using the options.vappname (command line option --targetvappname)
        instvApp.setName(options.vappName);

        // do not deploy this vApp.. we still need to update network info which requires the
        // vApp to be undeployed and not powered on.
        instvApp.setDeploy(Boolean.FALSE);
        instvApp.setPowerOn(Boolean.FALSE);

        // vApp reference to be used
        instvApp.setSource(vappReference);
        instvApp.setDescription("VM creation using VMCreateSample");
        instvApp.setAllEULAsAccepted(Boolean.TRUE);

        InstantiationParamsType instParams = new InstantiationParamsType();

        instvApp.setInstantiationParams(instParams);

        JAXBContext jaxbContexts = null;
        try {
            jaxbContexts = JAXBContext.newInstance(InstantiateVAppTemplateParamsType.class);
        } catch (JAXBException ex) {
            throw new RuntimeException("Problem creating JAXB Context: ", ex);
        }

        // Create HttpPost request to perform InstantiatevApp action
        HttpPost instantiateVAppPost = vcd.post(instantiateHref, options);
        com.vmware.vcloud.api.rest.schema.ObjectFactory obj = new com.vmware.vcloud.api.rest.schema.ObjectFactory();
        JAXBElement<InstantiateVAppTemplateParamsType> instvAppTemplate = obj
                .createInstantiateVAppTemplateParams(instvApp);
        OutputStream os = null;

        try {
            javax.xml.bind.Marshaller marshaller = jaxbContexts.createMarshaller();
            marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            os = new ByteArrayOutputStream();
            // Marshal the object via JAXB to XML
            marshaller.marshal(instvAppTemplate, os);
        } catch (JAXBException e) {
            throw new RuntimeException("Problem marshalling instantiation vApp template", e);
        }

        // Set the Content-Type header for the VM vApp template parameters
        ContentType contentType = ContentType.create(
                "application/vnd.vmware.vcloud.instantiateVAppTemplateParams+xml", "ISO-8859-1");
        StringEntity vapp = new StringEntity(os.toString(), contentType);
        instantiateVAppPost.setEntity(vapp);

        // Invoke the HttoPost to initiate the VM creation process
        HttpResponse response = HttpUtils.httpInvoke(instantiateVAppPost);

        VAppType vApp = null;

        // Make sure response status is 201 Created
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
            vApp = HttpUtils.unmarshal(response.getEntity(), VAppType.class);
        }

        if (null == vApp) {
            throw new RuntimeException("Could not instatiate or deploy the vApp.");
        }

        return vApp;
    }

    /**
     * This method will make a POST call to the vApp deploy URL to deploy the vApp and it's
     * children Vms.
     * 
     * @param vApp the vApp instance to deploy
     * @return a TaskType that can be used to monitor the status of the task
     */
    private TaskType deploy(VAppType vApp) {
        String deployHref = null;

        // Search the list of links for the vApp rel="deploy" to get the correct Href
        for(LinkType link : vApp.getLink()){
            if (link.getRel().equalsIgnoreCase("deploy")) {
                deployHref = link.getHref();
                break;
            }
        }

        // Only proceed if we found a valid deploy Href
        if (null != deployHref) {
            DeployVAppParamsType deployParams = new DeployVAppParamsType();
            deployParams.setPowerOn(Boolean.TRUE);
            
            // Create HttpPost request to perform InstantiatevApp action
            HttpPost instantiateVAppPost = vcd.post(deployHref, options);

            ObjectFactory objFactory = new ObjectFactory();
            JAXBElement<DeployVAppParamsType> deployParamsType = objFactory.createDeployVAppParams(deployParams);

            OutputStream os = null;

            JAXBContext jaxbContexts = null;
            try {
                jaxbContexts = JAXBContext.newInstance(DeployVAppParamsType.class);
            } catch (JAXBException ex) {
                throw new RuntimeException("Problem creating JAXB Context: ", ex);
            }

            try {
                javax.xml.bind.Marshaller marshaller = jaxbContexts.createMarshaller();
                marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                os = new ByteArrayOutputStream();
                // Marshal the object via JAXB to XML
                marshaller.marshal(deployParamsType, os);
            } catch (JAXBException e) {
                throw new RuntimeException("Problem marshalling instantiation vApp template", e);
            }

            HttpPost deployPost = vcd.post(deployHref, options);;
            // Set the Content-Type header for the VM vApp template parameters
            ContentType contentType = ContentType.create(
                    "application/vnd.vmware.vcloud.deployVAppParams+xml", "ISO-8859-1");
            StringEntity deployEntity = new StringEntity(os.toString(), contentType);
            deployPost.setEntity(deployEntity);

            // Invoke the HttoPost to initiate the VM creation process
            HttpResponse response = HttpUtils.httpInvoke(deployPost);

            // Make sure response status is 201 Created
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED) {
                TaskType taskType = HttpUtils.unmarshal(response.getEntity(),  TaskType.class);
                return taskType;
            }
        }

        throw new RuntimeException("Could not deploy " + vApp.getName());
    }

    /**
     * This method will retrieve the internal ip value for the passed in Vm
     * using the Vms NetworkConfigSection to obtain the ip.
     * 
     * @param vm the VmType to obtain the internal ip from
     * @return the internal ip if found, otherwise "none" is returned
     */
    private String getIpForVm(VmType vm) {
        // Request the NetworkConnection information for the VM to extract IP from it.
        HttpResponse response = HttpUtils.httpInvoke(vcd.get(vm.getHref() + VM_NETWORK_URL, options));

        // Make sure response is ok
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            NetworkConnectionSectionType nwsc = HttpUtils.unmarshal(response.getEntity(),
                    NetworkConnectionSectionType.class);
            if (null != nwsc) {
                String ip = nwsc.getNetworkConnection().get(0).getIpAddress();
                return ip;
            }
        }

        return "none";
    }

    /**
     * Iteratres through the passed in VAppType's children Vms, attempting to display
     * each Vm's internal IP information.
     * 
     * @param vApp the VAppType to display it's children Vm's internal ip
     */
    private void displayIPDetails(VAppType vApp) {
        List<VmType> vms = vApp.getChildren().getVm();
        for (VmType vm : vms) {
            // Try to get the IP
            String ip = getIpForVm(vm);
            System.out.println("Vm " + vm.getName() + " internal ip: " + ip + "\n");
        }
    }

    /**
     * Continually makes a GET request to the passed in Taks's Href with a 10 second delay between
     * each request to avoid sending too many requests to the API too fast.
     * 
     * @param task the to wait on
     */
    public void waitForTaskCompletion(TaskType task){
        int retry = 0;

        while ((!task.getStatus().equals("success") && !task.getStatus().equals("error")) && retry++ < 10) {
            HttpResponse response = HttpUtils.httpInvoke(vcd.get(task.getHref(), options));
            task = HttpUtils.unmarshal(response.getEntity(), TaskType.class);

            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // If the retry count reached 10 or the status is error, display task details to help
        // with figuring out what went wrong.
        if (retry == 10 || task.getStatus().equals("error")){
            System.out.println("\n         Task details : " + task.getDetails());
            System.out.println("      Task description : " + task.getDescription());
            System.out.println("        Task Operation : " + task.getOperation());
            System.out.println("        Task error msg : " +task.getError().getMessage());
            System.out.println(" Task major error code : " + task.getError().getMajorErrorCode());
            System.out.println("Task error stack trace : " + task.getError().getStackTrace());

            throw new RuntimeException("Could not complete creation of vApp");
        }
    }

    /**
     * Loops through the list of tasks provided by the tasksInProgressType parameter, calling
     * the waitForTaskCompletion method with each individual task.
     * 
     * @param tasksInProgressType a collection of tasks to wait for
     */
    public void waitForTasks(TasksInProgressType tasksInProgressType) {
        List<TaskType> tasks = tasksInProgressType.getTask();
        for (TaskType task : tasks) {
            this.waitForTaskCompletion(task);
        }
    }
}