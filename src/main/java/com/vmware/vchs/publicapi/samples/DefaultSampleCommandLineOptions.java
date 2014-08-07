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
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

/**
 * This class defines the common command line arguments that are processed by samples. Sample
 * classes should extend this class to process more options specific to the sample.
 */
public class DefaultSampleCommandLineOptions {
    static final String OPTION_USERNAME = "username";
    static final String OPTION_PASSWORD = "password";
    static final String OPTION_HOSTNAME = "url";
    static final String OPTION_VCHS_VERSION = "vchsversion";
    static final String OPTION_VDC_NAME = "vdcname";
    static final String OPTION_VCD_VERSION = "vcloudversion";

    // Command line arguments
    Option[] options = new Option[] {
        new Option(OPTION_USERNAME, true, "The username to log in with."),
        new Option(OPTION_PASSWORD, true, "The password for username to log in with."),
        new Option(OPTION_HOSTNAME, true, "The vCHS Server URL to log in to if the default " + SampleConstants.DEFAULT_HOSTNAME + " is not to be used."),
        new Option(OPTION_VCHS_VERSION, true, "The version of the vCHS API to run this sample against if the default of " + SampleConstants.DEFAULT_VCHS_VERSION + " is not to be used."),
        new Option(OPTION_VCD_VERSION, true, "The version of the VCloud API to run this sample against if the default of " + SampleConstants.DEFAULT_VCD_VERSION + " is not to be used"),
        new Option(OPTION_VDC_NAME, true, "The VDC under which vApp to be created")
    };

    /*
     * The host (url) that will be used to make vCHS Public API Rest calls to
     */
    String vchsHostname = SampleConstants.DEFAULT_HOSTNAME;

    /*
     * The version of the vCHS Public API to make Rest calls against
     */
    String vchsVersion = SampleConstants.DEFAULT_VCHS_VERSION;

    /*
     * The vCHS Public API username encoded with the password for authentication purposes
     */
    String vchsUsername;

    /*
     * The vCHS Public API password encoded with the username for authentication purposes
     */
    String vchsPassword;

    /*
     * The vCloud API version to make rest calls against
     */
    String vcdVersion;

    /*
     * The name of the VCD
     */
    String vdcName;

    /**
     * This method returns the Apache Commons Cli Options instance that represents
     * the common options all vCHS Rest API Samples may need. Samples can provide their
     * own subclass of this class and override this method, call super on this method to
     * get the Options object back, then add their options to the returned Options from this
     * method.
     * 
     * @return
     */
    protected Options getOptions(){
        Options opts = new Options();

        for(Option opt : options){
            opts.addOption(opt);
        }

        return opts;
    }

    /**
     * This method will process the passed in command line args (typically from a main() method) and
     * process those args with the passed in Options instance. Subclasses of this class can call
     * super.parseOptions() and use the CommandLine instance returned to match any of the subclass
     * specific options processed by the CommandLineParser object in this method.
     * 
     * @param args
     *            the command line String[] args to process
     * @param options
     *            the Apache Command Line Options instance to parse the args against
     * @return an instance of the Apache cli CommandLine for subclasses to use to match up specific
     *         sublcass samples options with
     */
    protected CommandLine parseOptions(String[] args) {
        CommandLineParser parser = new PosixParser();
        HelpFormatter help = new HelpFormatter();
        CommandLine cl = null;

        try {
            cl = parser.parse(getOptions(), args);

            if (cl.hasOption(OPTION_USERNAME)) {
                vchsUsername = cl.getOptionValue(OPTION_USERNAME);
            }

            if (cl.hasOption(OPTION_PASSWORD)) {
                vchsPassword = cl.getOptionValue(OPTION_PASSWORD);
            }

            if (cl.hasOption(OPTION_HOSTNAME)) {
                vchsHostname = cl.getOptionValue(OPTION_HOSTNAME);

                // remove trailing / if it exists
                if (vchsHostname.endsWith("/")) {
                    vchsHostname = vchsHostname.substring(0, vchsHostname.length() - 1);
                }

                if (cl.hasOption(OPTION_VCHS_VERSION)) {
                    vchsVersion = cl.getOptionValue(OPTION_VCHS_VERSION);
                }
            }

            if (cl.hasOption(OPTION_VDC_NAME)) {
                vdcName = cl.getOptionValue(OPTION_VDC_NAME);
            }

            if (cl.hasOption(OPTION_VCD_VERSION)) {
                vcdVersion = cl.getOptionValue(OPTION_VCD_VERSION);
            }
        } catch (org.apache.commons.cli.ParseException e) {
            help.printHelp("vCHS Sample command line syntax", getOptions());
            System.exit(1);
        }

        return cl;
    }
}