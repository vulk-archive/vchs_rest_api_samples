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
 * This class adds three command line arguments to the DefaultSampleCommandLineOptions default
 * command line options specific to the VMCreateSample.  
 */
public class VMCreateCommandLineOptions extends DefaultSampleCommandLineOptions {
    static final String OPTION_VAPP_NAME = "targetvappname";
    static final String OPTION_NETWORK_NAME = "orgnet";
    static final String OPTION_TEMPLATE_NAME = "vchstemplatename";

    // Command line arguments
    Option[] options = new Option[] {
        new Option(OPTION_VAPP_NAME, true, "The name of vApp to be created"),
        new Option(OPTION_NETWORK_NAME, true, "The network to be used by vApp"),
        new Option(OPTION_TEMPLATE_NAME, true, "The template to be used to create vApp")
    };

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
        // use the returned CommandLine response to parse this samples specific arguments
        if (cl.hasOption(OPTION_VAPP_NAME)) {
            vappName = cl.getOptionValue(OPTION_VAPP_NAME);
        }

        if (cl.hasOption(OPTION_NETWORK_NAME)) {
            networkName = cl.getOptionValue(OPTION_NETWORK_NAME);
        }

        if (cl.hasOption(OPTION_TEMPLATE_NAME)) {
            templateName = cl.getOptionValue(OPTION_TEMPLATE_NAME);
        }

        return cl;
    }

    // variables to hold vApp name, template name and network name to create a VM and get its ip
    // passed in via the command line
    String vappName;
    String templateName;
    String networkName;
}