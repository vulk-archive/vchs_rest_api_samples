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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import com.vmware.ares.pub.api.ComputeType;
import com.vmware.ares.pub.api.LinkType;
import com.vmware.ares.pub.api.ServiceListType;
import com.vmware.ares.pub.api.ServiceType;
import com.vmware.ares.pub.api.VCloudSessionType;
import com.vmware.ares.pub.api.VdcLinkType;
import com.vmware.ares.pub.api.VdcReferenceType;
import com.vmware.vcloud.api.rest.schema.FirewallRuleProtocols;
import com.vmware.vcloud.api.rest.schema.FirewallRuleType;
import com.vmware.vcloud.api.rest.schema.FirewallServiceType;
import com.vmware.vcloud.api.rest.schema.GatewayConfigurationType;
import com.vmware.vcloud.api.rest.schema.GatewayFeaturesType;
import com.vmware.vcloud.api.rest.schema.GatewayInterfaceType;
import com.vmware.vcloud.api.rest.schema.GatewayInterfacesType;
import com.vmware.vcloud.api.rest.schema.GatewayNatRuleType;
import com.vmware.vcloud.api.rest.schema.GatewayType;
import com.vmware.vcloud.api.rest.schema.IpRangeType;
import com.vmware.vcloud.api.rest.schema.IpRangesType;
import com.vmware.vcloud.api.rest.schema.NatRuleType;
import com.vmware.vcloud.api.rest.schema.NatServiceType;
import com.vmware.vcloud.api.rest.schema.NetworkServiceType;
import com.vmware.vcloud.api.rest.schema.ObjectFactory;
import com.vmware.vcloud.api.rest.schema.QueryResultEdgeGatewayRecordType;
import com.vmware.vcloud.api.rest.schema.QueryResultRecordType;
import com.vmware.vcloud.api.rest.schema.QueryResultRecordsType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.TaskType;
import com.vmware.vcloud.api.rest.schema.VdcType;

/**
 * GatewayRuleSample
 * 
 * This sample will add a NAT rule and a Firewall Rule to the EdgeGateway of the VDC where this
 * vApp is deployed.
 * 
 * The command line option --vdcname is used to specify the VDC where the gateway resides.
 * 
 * The command line option --internalip is the IP address assigned to an existing vApp
 * VM instance.
 * 
 * The command line option --externalip is the external IP address of the EdgeGateway of the VDC 
 * where the vApp resides.
 * 
 * The command line option --edgegateway is the name of the gateway that the vApp connects to in
 * the VDC where the vApp resides.
 * 
 * Steps:
 *   1) Log in to vCHS
 *   2) Get the list of the compute services
 *   3) Find the VDC within the compute services
 *   4) Get the VDC's session link from vCHS
 *   5) Get a vCloud session for the VDC
 *   6) Get a VCD instance from the vCloud session Href
 *   7) Get the VDC EdgeGateway Href
 *   8) Add the gateway rules to the VDC
 *   
 * Parameters:
 * url              [required] : url of the vCHS web service.
 * username         [required] : username for the vCHS authentication.
 * password         [required] : password for the  vCHS authentication.
 * vchsversion      [required] : version of vCHS API.
 * vcloudversion    [required] : version of vCloud API.
 * vdcname          [required] : name of the VDC.
 * internalip       [required] : the internal IP address of the deployed vApp
 * externalip       [required] : the external public IP assigned to the vApp gateway
 * edgegateway      [required] : the Org gateway from which the vApp network is connected to
 * 
 * Argument Line:
 * 
 * Creates a vApp from a vApp template, deploys it and powers it on, and displays its internal
 * IP address.
 * 
 * --url [vchs webservice url] --username [vchs username] --password [vchs password]
 * --vchsversion [vchs version] --vappname [vapp name] --vdcname [vdc name]
 * --vcloudversion [vcloud version] --internalip [vApp assigned ip] 
 * --externalip [vApp gateway] --edgegateway [vApp gateway]
 */
public class GatewayRuleSample {
    private Vchs vchs = null;
    private Vcd vcd = null;
    private GatewayRuleCommandLineOptions options = null;

    /**
     * @param args
     *            any arguments passed by the command line, if none, defaults are used where
     *            applicable.
     */
    public static void main(String[] args) {
        // Creating an instance of this sample
        GatewayRuleSample sample = new GatewayRuleSample();
        sample.run(args);
    }

    /**
     * Called by the static main method on the instance of this class with the
     * command line args array.
     * 
     * @param args the arguments passed on the command line
     */
    private void run(String[] args){
        // Create instance of CommandLineOptions for use in this sample
        options = new GatewayRuleCommandLineOptions();
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

            // Retrieve the Link to Edge Gateways i.e, the list of Edge Gateway that is to be
            // used to add NAT and firewall Rules
            System.out.print("Retrieving the href for the EdgeGateway...");
            String edgeGatewaysHref = getEdgeGatewaysHref(vcd.vdcHref);
            System.out.println("Success\n");

            // Find the details regarding the network on which nat rules are to be applied, the link
            // to action to be performed to update gateways and then add nat and firewall rules to
            // gateway
            System.out.print("Adding NAT and Firewall rules...");
            addRules(edgeGatewaysHref);
        }
    }
    /**
     * This method is to get Href for EdgeGateways, the list of EdgeGateway
     * 
     * @param vdcHref
     *            the href to the VDC
     * 
     * @return the href to the EdgeGateways for VDC
     */
    private String getEdgeGatewaysHref(String vdcHref) {
        HttpResponse response = HttpUtils.httpInvoke(vcd.get(vdcHref,options));
        VdcType vdc = HttpUtils.unmarshal(response.getEntity(), VdcType.class);
        List<com.vmware.vcloud.api.rest.schema.LinkType> linklist = vdc.getLink();

        String edgegatewaysHref = null;

        // Iterating through the links associated with VDC and getting one for EdgeGateways
        for (com.vmware.vcloud.api.rest.schema.LinkType link : linklist) {
            if (link.getRel() != null && link.getRel().equals("edgeGateways")) {
                edgegatewaysHref = link.getHref();
                break;
            }
        }

        if (null == edgegatewaysHref){
            throw new RuntimeException("Could not find edge gateways for VDC Href: " + vdcHref);
        }

        return edgegatewaysHref;
    }

    /**
     * This method will retrieve information to configure gateways and use the details to add Nat
     * and Firewall Rules. It will use the gateway specified by name.
     * 
     * @param edgeGatewaysHref
     *            the Href to the edgegateways
     */
    private void addRules(String edgeGatewaysHref) {
        // invoking API for EdgeGateways
        HttpResponse response = HttpUtils.httpInvoke(vcd.get(edgeGatewaysHref, options));
        QueryResultRecordsType queryRecords = HttpUtils.unmarshal(response.getEntity(),
                QueryResultRecordsType.class);
        List<JAXBElement<? extends QueryResultRecordType>> Records = queryRecords.getRecord();
        String gatewayHref = null;

        // Iterating through the EdgeGateway to find href for the gateway to be used to add rules
        for (JAXBElement<? extends QueryResultRecordType> qResult : Records) {
            QueryResultEdgeGatewayRecordType rslt = new QueryResultEdgeGatewayRecordType();
            rslt = (QueryResultEdgeGatewayRecordType) qResult.getValue();

            if (rslt.getName().equalsIgnoreCase(options.edgeGateway)) {
                gatewayHref = rslt.getHref();
                // Found, break from loop
                break;
            }
        }

        // Make sure returned gatewayHref is not null
        if (null != gatewayHref) {
            // Before configuring the gateway rules need to find the url to perform the service
            // configuration action
            String serviceConfHref = getServiceConfHref(gatewayHref);

            // Retrieving the Href for the network,on which nat rules to be applied on
            String networkHref = getNetworkHref(gatewayHref);
            if (networkHref != null) {
                // Performing the main action of sample that is adding nat and firewall rules
                configureRules(networkHref, serviceConfHref);
            } else {
                throw new RuntimeException("\nFailed to find network to be used to apply rules.");
            }
        } else {
            throw new RuntimeException("Could not find gateway Href for edge gateways: " + edgeGatewaysHref);
        }
    }

    /**
     * This method is to get Href for the Gateway Service Configuration action i.e, Link to update
     * gateway
     * 
     * @param gatewayHref
     *            the href to the gateway to be used
     * @return the href of the service configuration action for the gateway.
     */
    private String getServiceConfHref(String gatewayHref) {
        HttpResponse response = HttpUtils.httpInvoke(vcd.get(gatewayHref, options));
        GatewayType gateway = HttpUtils.unmarshal(response.getEntity(), GatewayType.class);
        List<com.vmware.vcloud.api.rest.schema.LinkType> links = gateway.getLink();

        String serviceConfHref = null;

        // Iterating through the Links associated with Gateway's action to retrieve one that is to
        // perform edgeGatewayServiceConfiguration
        for (com.vmware.vcloud.api.rest.schema.LinkType link : links) {
            if (link.getType() != null
                    && link.getType().contains("edgeGatewayServiceConfiguration")) {
                serviceConfHref = link.getHref();
                break;
            }
        }

        if (null == serviceConfHref){
            throw new RuntimeException("Could not find service configuration for gateway Href: "
                + gatewayHref);
        }

        return serviceConfHref;
    }

    /**
     * This method is to get Href for network on which the Nat rules need to be Applied.
     * 
     * @param gatewayHref
     *            the href to the gateway to be used
     * @return href the interface on which the rules need to be applied
     */
    private String getNetworkHref(String gatewayHref) {
        // Represents the Gateway
        HttpResponse response = HttpUtils.httpInvoke(vcd.get(gatewayHref, options));
        GatewayType gateway = HttpUtils.unmarshal(response.getEntity(), GatewayType.class);

        // Retrieving the configuration for the Gateway
        GatewayConfigurationType gatewayConfig = gateway.getConfiguration();

        // Retrieving the Gateway Interfaces
        GatewayInterfacesType gatewayInterfaces = gatewayConfig.getGatewayInterfaces();
        List<GatewayInterfaceType> gatewayInterfaceList = gatewayInterfaces.getGatewayInterface();
        String networkHref = null;

        // Iterating through Gateway Interface list to select a Gateway Interface to which the
        // externalIp provided belongs
        for (GatewayInterfaceType gatewayInterface : gatewayInterfaceList) {
            if (gatewayInterface.getInterfaceType().equals("uplink")) {
                for (int i = 0; i < gatewayInterface.getSubnetParticipation().size(); i++) {
                    IpRangesType ipRanges = gatewayInterface.getSubnetParticipation().get(i)
                            .getIpRanges();
                    List<IpRangeType> ipRange = ipRanges.getIpRange();

                    for (IpRangeType ipR : ipRange) {
                        long startAddress = 0l;
                        long endAddresss = 0l;
                        long ipToTest = 0l;

                        try {
                            startAddress = ipToLong(InetAddress.getByName(ipR.getStartAddress()));
                            endAddresss = ipToLong(InetAddress.getByName(ipR.getEndAddress()));
                            ipToTest = ipToLong(InetAddress.getByName(options.externalIp));
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }

                        if (ipToTest >= startAddress && ipToTest <= endAddresss) {
                            // The Network that is to be used to apply NAT Rules
                            networkHref = gatewayInterface.getNetwork().getHref();
                            break;
                        }
                    }
                }
            }
        }

        if (null == networkHref){
            throw new RuntimeException("Could not find network for gateway Href: " + gatewayHref);
        }

        return networkHref;
    }

    /**
     * This method converts ip to long which is later used to find the ip within iprange
     * 
     * @param ip
     *            ip address
     * @return result
     */
    private static final long ipToLong(InetAddress ip) {
        byte[] octets = ip.getAddress();
        long result = 0;
        for (byte octet : octets) {
            result <<= 8;
            result |= octet & 0xff;
        }

        return result;
    }

    /**
     * This method is to configure NAT and Firewall Rules to the EdgeGateway
     * 
     * @param networkHref
     *            the href to the network on which nat rules to be applied
     * @param serviceConfHref
     *            the href to the service configure action of gateway
     * @return
     */
    private void configureRules(String networkHref, String serviceConfHref) {
        // NAT Rules
        NatServiceType natService = new NatServiceType();

        // To Enable the service using this flag
        natService.setIsEnabled(Boolean.TRUE);

        // Configuring Destination nat
        NatRuleType dnatRule = new NatRuleType();

        // Setting Rule type Destination Nat DNAT
        dnatRule.setRuleType("DNAT");
        dnatRule.setIsEnabled(Boolean.TRUE);
        GatewayNatRuleType dgatewayNat = new GatewayNatRuleType();
        ReferenceType refd = new ReferenceType();
        refd.setHref(networkHref);

        // Network on which nat rules to be applied
        dgatewayNat.setInterface(refd);

        // Setting Original IP
        dgatewayNat.setOriginalIp(options.externalIp);
        dgatewayNat.setOriginalPort("any");

        dgatewayNat.setTranslatedIp(options.internalIp);

        // To allow all ports and all protocols
        // dgatewayNat.setTranslatedPort("any");
        // dgatewayNat.setProtocol("Any");

        // To allow only https use Port 443 and TCP protocol
        dgatewayNat.setTranslatedPort("any");
        dgatewayNat.setProtocol("TCP");

        // To allow only ssh use Port 22 and TCP protocol
        // dgatewayNat.setTranslatedPort("22");
        // dgatewayNat.setProtocol("TCP");
        // Setting Destination IP
        dnatRule.setGatewayNatRule(dgatewayNat);
        natService.getNatRule().add(dnatRule);

        // Configuring Source nat
        NatRuleType snatRule = new NatRuleType();
        
        // Setting Rule type Source Nat SNAT
        snatRule.setRuleType("SNAT");
        snatRule.setIsEnabled(Boolean.TRUE);
        GatewayNatRuleType sgatewayNat = new GatewayNatRuleType();
        //ReferenceType refd = new ReferenceType();
        //refd.setHref(networkHref);

        // Network on which nat rules to be applied
        sgatewayNat.setInterface(refd);

        // Setting Original IP
        sgatewayNat.setOriginalIp(options.internalIp);
        //sgatewayNat.setOriginalPort("any");

        sgatewayNat.setTranslatedIp(options.externalIp);

        // Setting Source IP
        snatRule.setGatewayNatRule(sgatewayNat);
        natService.getNatRule().add(snatRule);


        // Firewall Rules
        FirewallServiceType firewallService = new FirewallServiceType();

        // Enable or disable the service using this flag
        firewallService.setIsEnabled(Boolean.TRUE);

        // Default action of the firewall set to drop
        firewallService.setDefaultAction("drop");

        // Flag to enable logging for default action
        firewallService.setLogDefaultAction(Boolean.FALSE);

        // Firewall Rule settings
        FirewallRuleType firewallInRule = new FirewallRuleType();
        firewallInRule.setIsEnabled(Boolean.TRUE);
        firewallInRule.setMatchOnTranslate(Boolean.FALSE);
        firewallInRule.setDescription("Allow incoming https access");
        firewallInRule.setPolicy("allow");
        FirewallRuleProtocols firewallProtocol = new FirewallRuleProtocols();
        firewallProtocol.setAny(Boolean.TRUE);
        firewallInRule.setProtocols(firewallProtocol);
        firewallInRule.setDestinationPortRange("any");
        firewallInRule.setDestinationIp(options.externalIp);
        firewallInRule.setSourcePortRange("Any");
        firewallInRule.setSourceIp("external");
        firewallInRule.setEnableLogging(Boolean.FALSE);
        firewallService.getFirewallRule().add(firewallInRule);

        // To create the HttpPost request Body
        ObjectFactory objectFactory = new ObjectFactory();
        GatewayFeaturesType gatewayFeatures = new GatewayFeaturesType();
        JAXBElement<NetworkServiceType> serviceType = objectFactory
                .createNetworkService(natService);
        JAXBElement<NetworkServiceType> firewallserviceType = objectFactory
                .createNetworkService(firewallService);
        gatewayFeatures.getNetworkService().add(serviceType);
        gatewayFeatures.getNetworkService().add(firewallserviceType);
        JAXBContext jaxbContexts = null;

        try {
            jaxbContexts = JAXBContext.newInstance(GatewayFeaturesType.class);
        } catch (JAXBException ex) {
            ex.printStackTrace();
        }

        OutputStream os = null;
        JAXBElement<GatewayFeaturesType> gateway_Features = objectFactory
                .createEdgeGatewayServiceConfiguration(gatewayFeatures);

        try {
            javax.xml.bind.Marshaller marshaller = jaxbContexts.createMarshaller();
            marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            os = new ByteArrayOutputStream();

            // Marshal the JAXB class to XML
            marshaller.marshal(gateway_Features, os);
        } catch (JAXBException e) {
            e.printStackTrace();
        }

        HttpPost httpPost = vcd.post(serviceConfHref, options);
        ContentType contentType = ContentType.create(SampleConstants.CONTENT_TYPE_EDGE_GATEWAY,
                "ISO-8859-1");
        StringEntity rules = new StringEntity(os.toString(), contentType);
        httpPost.setEntity(rules);
        InputStream is = null;

        // Invoking api to add rules to gateway
        HttpResponse response = HttpUtils.httpInvoke(httpPost);

        // Make sure the response code is 202 ACCEPTED
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED) {
            // System.out.println("ResponseCode : " + response.getStatusLine().getStatusCode());
            System.out.println("\nRequest To update Gateway initiated sucessfully");
            System.out.print("\nUpdating EdgeGateways to add NAT and Firewall Rules...");
            taskStatus(response);
        }
    }

    /**
     * This method is to get the status of Configure NAT and Fire-wall Rules
     * 
     * @param response
     *            the response of gateway configure action
     */
    private void taskStatus(HttpResponse response) {
        // Represents the task configuring NAT and Firewall Rules
        TaskType task = HttpUtils.unmarshal(response.getEntity(), TaskType.class);
        String taskHref = task.getHref();
        HttpGet httpGet = vcd.get(taskHref, options);
        HttpResponse resp = null;

        // Check task status until it shows either success or error.
        while (!(task.getStatus()).equals("success") && !(task.getStatus()).equals("error")) {
            try {
                // Wait 10 seconds before requesting the status again
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            resp = HttpUtils.httpInvoke(httpGet);
            System.out.print(".");
            task = HttpUtils.unmarshal(resp.getEntity(), TaskType.class);
        }

        if ((task.getStatus()).equals("success")) {
            System.out.println("Success");
        } else {
            throw new RuntimeException(task.getStatus());
        }
    }
}