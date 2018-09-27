# HCI-Sample-Plugin
MAVEN based project to create HCI Plugin.

Steps: 
1. Git clone the project. 

2. Write the plugin class with the corressponding package in the src/main/java with the package name as discussed.
  
3. modify plugin.json in the Root Folder to include the plugin name and description. 
 
4. Create the jar -> mvn clean package 
 
5. A project jar will be created in the target directory and also being copied to the plugin-test directory. 
 
6. Run the following two commands. 
	1. cd plugin-test directory. 
	2. Run command -> **plugin-test -jar SamplePluginProject-1.0-SNAPSHOT.jar -a output.file** . This command will generate a sample JSON file that will contain all the defaults property that will be acting as input to the PLUGIN and will be shown in the UI to provide values to the plugin. 
	3. Run command -> **plugin-test -jar SamplePluginProject-1.0-SNAPSHOT.jar -c  output.file -p com.hds.ensemble.sdk.plugin.samplepluginproject.FileReaderPlugin**. This command will run the plugin test with the values as provided in file output.file (i.e. changing the input to the Plugin E.g. directory location or everything that is needed to run the project). This will also run the TEST HARNESS and show the result whether the input is successful (or plugin test is successful or not). 
