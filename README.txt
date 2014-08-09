VMware vCloud API with vCloud Hybrid Service Extensions Samples README
``````````````````````````````````````````````````````````````````````````````````````````````
This README describes the vCloud API with vCloud Hybrid Service Extensions Java code samples.

Sample directory: 

    .src\main\java\com\vmware\vchs\publicapi\samples

Sample Program Structure
------------------------------------------------

Every sample performs the following basic tasks:

* Authenticating and connecting to the vCHS server
* Obtaining access to the REST APIs to perform operations
* Obtaining the vCloud API endpoint to perform operations on the vCloud API


Building Samples
################

You must build the samples before you can run them. The samples root directory contains the build.bat and build.sh scripts. Both scripts perform the same tasks depending on whether you are running on a Windows or Linux/Mac environment. 

The scripts remove the build/classes directory if it exists, ensuring a clean build. Then, the scripts compile the sample source classes to the build/classes directory. VMware provides all dependent libraries for the samples in the lib directory found in the samples root directory.


Running Samples
###############

Before you run the samples, you must configure the JAVA_HOME environment variable and the CLASSPATH to the dependent libraries. Use the run.bat (Windows) or run.sh (Linux or Mac) script to run the samples. 

The following information explains how to run and pass the parameters to each sample:

1. VDCListSample
------------------------------------------------
Windows:
run.bat com.vmware.vchs.publicapi.samples.VDCListSample --url <url to vCHS Public API> --username <vchs username> --password <vchs password> --vchsversion 5.6 --vcloudversion 5.6

Linux/Mac:
./run.sh com.vmware.vchs.publicapi.samples.VDCListSample --url <url to vCHS Public API> --username <vchs username> --password <vchs password> --vchsversion 5.6 --vcloudversion 5.6

2. VMCreateSample
------------------------------------------------
Windows:
run.bat com.vmware.vchs.publicapi.samples.VMCreateSample --url <url to vCHS Public API> --username <vchs username> --password <vchs password> --targetvappname <name of vApp> --vchsversion 5.6 --vdcname <name of VDC> --vcloudversion 5.6 --orgnet <name of network> --vchstemplatename <name of template catalog>

Linux/Mac:
./run.sh com.vmware.vchs.publicapi.samples.VMCreateSample --url <url to vCHS Public API> --username <vchs username> --password <vchs password> --targetvappname <name of vApp> --vchsversion 5.6 --vdcname <name of VDC> --vcloudversion 5.6 --orgnet <name of network> --vchstemplatename <name of template catalog>

3. GatewayRuleSample
------------------------------------------------
Windows:
run.bat com.vmware.vchs.publicapi.samples.GatewayRuleSample --url <url to vCHS Public API> --username <vchs username> --password <vchs password> --vchsversion 5.6 --internalip <internal ip> --externalip <external ip> --vdcname <name of VDC> --vcloudversion 5.6 --edgegateway <edge gateway>

Linux/Mac:
./run.sh com.vmware.vchs.publicapi.samples.GatewayRuleSample --url <url to vCHS Public API> --username <vchs username> --password <vchs password> --vchsversion 5.6 --internalip <internal ip> --externalip <external ip> --vdcname <name of VDC> --vcloudversion 5.6 --edgegateway <edge gateway>
