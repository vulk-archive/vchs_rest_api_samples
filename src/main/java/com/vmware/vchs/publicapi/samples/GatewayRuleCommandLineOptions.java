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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * This class defines the command line arguments that can be processed for samples. Future samples
 * can add their argument names and instance variables to hold argument values in this class.
 */
public class GatewayRuleCommandLineOptions extends DefaultSampleCommandLineOptions {
    static final String OPTION_INTERNALIP = "internalip";
    static final String OPTION_EXTERNALIP = "externalip";
    static final String OPTION_EDGEGATEWAY = "edgegateway";

    // Apache CLI Option array for GatewayRuleSample command line options
    Option[] options = new Option[] {
        new Option(OPTION_INTERNALIP, true, "NAT rule internal IP address."),
        new Option(OPTION_EXTERNALIP, true, "NAT rule external IP address."),
        new Option(OPTION_EDGEGATEWAY, true, "The EdgeGateway where these NAT and Firewall rules are applied.")
    };

    /*
     * variables to hold the internal and external ips and edge gateway passed in via the command
     * line
     */
    String internalIp;
    String externalIp;
    String edgeGateway;

    @Override
    public Options getOptions() {
        Options opts = super.getOptions();
        for (Option opt : options) {
            opts.addOption(opt);
        }

        return opts;
    }

    @Override
    protected CommandLine parseOptions(String[] args) {
        CommandLine cl = super.parseOptions(args);

        // Use the super CommandLine returned instance to parse GatewayRuleSample command
        // line options
        // use the returned CommandLine response to parse this sample's specific arguments
        if (cl.hasOption(OPTION_INTERNALIP)) {
            internalIp = cl.getOptionValue(OPTION_INTERNALIP);
        }

        if (cl.hasOption(OPTION_EXTERNALIP)) {
            externalIp = cl.getOptionValue(OPTION_EXTERNALIP);
        }

        if (cl.hasOption(OPTION_EDGEGATEWAY)) {
            edgeGateway = cl.getOptionValue(OPTION_EDGEGATEWAY);
        }

        return cl;
    } 
}