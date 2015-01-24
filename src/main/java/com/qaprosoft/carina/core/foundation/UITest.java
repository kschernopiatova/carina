/*
 * Copyright 2013 QAPROSOFT (http://qaprosoft.com/).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qaprosoft.carina.core.foundation;

import java.lang.reflect.Method;

import org.apache.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;
import org.testng.xml.XmlTest;

import com.qaprosoft.carina.core.foundation.listeners.UITestListener;
import com.qaprosoft.carina.core.foundation.report.ReportContext;
import com.qaprosoft.carina.core.foundation.utils.Configuration;
import com.qaprosoft.carina.core.foundation.utils.Configuration.DriverMode;
import com.qaprosoft.carina.core.foundation.utils.Configuration.Parameter;
import com.qaprosoft.carina.core.foundation.utils.SpecialKeywords;
import com.qaprosoft.carina.core.foundation.utils.android.recorder.utils.AdbExecutor;
import com.qaprosoft.carina.core.foundation.utils.naming.TestNamingUtil;
import com.qaprosoft.carina.core.foundation.webdriver.DriverFactory;
import com.qaprosoft.carina.core.foundation.webdriver.DriverPool;

@Listeners({ UITestListener.class })
public class UITest extends AbstractTest
{
    private static final Logger LOGGER = Logger.getLogger(UITest.class);
    
    protected WebDriver driver;
    
	protected WebDriver extraDriver;
	protected static ThreadLocal<WebDriver> webDrivers = new ThreadLocal<WebDriver>();

    private static AdbExecutor executor = new AdbExecutor(Configuration.get(Parameter.ADB_HOST), Configuration.get(Parameter.ADB_PORT));
    private int adb_pid = 0;
    
    Throwable init_throwable;

	
	@Override
	protected boolean isUITest()
	{
		return true;
	}
	
    @BeforeSuite(alwaysRun = true)
    public void executeBeforeTestSuite(ITestContext context) throws Throwable
    {
    	super.executeBeforeTestSuite(context);
    	
    	DriverMode driverMode = Configuration.getDriverMode(Parameter.DRIVER_MODE);		

	    if (driverMode == DriverMode.SUITE_MODE/*  && getDriver() == null*/) //there is no need to verify on null as it is start point for all our tests 
	    {
	    	LOGGER.debug("Initialize driver in UITest->BeforeSuite.");
	    	if (!initDriver(context.getSuite().getName())) {
	    		throw init_throwable;
	    	}
	    }

    }
    
    @BeforeClass(alwaysRun = true)
    public void executeBeforeTestClass(ITestContext context) throws Throwable {
    	super.executeBeforeTestClass(context);
    	
    	DriverMode driverMode = Configuration.getDriverMode(Parameter.DRIVER_MODE);

	    if (driverMode == DriverMode.SUITE_MODE)
	    {
	    	//get from DriverPool.single_driver because everything it is deleted somehow!!
			driver = DriverPool.getSingleDriver();
			if (driver != null) {
     	    	setDriver(driver);
			}
	    }
		if (driverMode == DriverMode.CLASS_MODE && getDriver() == null)
	    {
	    	LOGGER.debug("Initialize driver in UITest->BeforeClass.");
	    	if (!initDriver(this.getClass().getName())) {
	    		throw init_throwable;
	    	}
	    }
    }
    
    @BeforeMethod(alwaysRun = true)
    public void executeBeforeTestMethod(XmlTest xmlTest, Method testMethod, ITestContext context) throws Throwable
    {
		super.executeBeforeTestMethod(xmlTest, testMethod, context);
		quitExtraDriver(); //quit from extra Driver to be able to proceed with single method_mode for mobile automation
		
		DriverMode driverMode = Configuration.getDriverMode(Parameter.DRIVER_MODE);
		
	    if (driverMode == DriverMode.SUITE_MODE)
	    {
	    	//get from DriverPool.single_driver because everything it is deleted somehow!!
			driver = DriverPool.getSingleDriver();

			if (driver != null) {
     	    	setDriver(driver);
			}
	    }
	    

	    
		String test = TestNamingUtil.getCanonicalTestNameBeforeTest(xmlTest, testMethod);
    	if (driverMode == DriverMode.METHOD_MODE && getDriver() == null)
    	{
    		LOGGER.debug("Initialize driver in UItest->BeforeMethod.");
	    	if (!initDriver(test)) {
	    		throw init_throwable;
	    	}	    		
    	}
    	if (driverMode == DriverMode.METHOD_MODE || driverMode == DriverMode.CLASS_MODE) {
			startRecording();
    	}		    	

    	if (browserVersion.isEmpty() && getDriver() != null)
    		browserVersion = DriverFactory.getBrowserVersion(getDriver());
	}    

    @AfterMethod(alwaysRun = true)
    public void executeAfterTestMethod(ITestResult result)
    {
    	try
    	{	    
    		quitExtraDriver(); 
    		DriverMode driverMode = Configuration.getDriverMode(Parameter.DRIVER_MODE);
			
	    	if (driverMode == DriverMode.METHOD_MODE || driverMode == DriverMode.CLASS_MODE) {
				stopRecording(TestNamingUtil.getCanonicalTestName(result));
	    	}
	    	
	    	if (driverMode == DriverMode.METHOD_MODE) {
	    		LOGGER.debug("Deinitialize driver in @AfterMethod.");
				quitDriver();
			}	    	
    	}
		catch (Exception e)
		{
		    LOGGER.error("Exception in executeAfterTestMethod");
		    e.printStackTrace();
		}
    	
		super.executeAfterTestMethod(result);    	
    }
    
    @AfterClass(alwaysRun = true)
    public void executeAfterTestClass(ITestContext context) throws Throwable {
		quitExtraDriver();
	    if (Configuration.getDriverMode(Parameter.DRIVER_MODE) == DriverMode.CLASS_MODE && getDriver() != null)
	    {
	    	LOGGER.debug("Deinitialize driver in UITest->AfterClass.");
			quitDriver();
	    }
    	
		super.executeAfterTestClass(context);    	
    }
   
    @AfterSuite(alwaysRun = true)
    public void executeAfterTestSuite(ITestContext context)
    {
    	quitExtraDriver();
	    if (Configuration.getDriverMode(Parameter.DRIVER_MODE) == DriverMode.SUITE_MODE && getDriver() != null)
	    {
	    	LOGGER.debug("Deinitialize driver in UITest->AfterSuite.");
			quitDriver();
			stopRecording(null);
	    }
	    
		super.executeAfterTestSuite(context);	    
	}
    
	
	// --------------------------------------------------------------------------
	// Web Drivers
	// --------------------------------------------------------------------------
	protected WebDriver createExtraDriver(String driverName, DesiredCapabilities capabilities, String selenium_host) {
		if (extraDriver != null) {
			LOGGER.warn("Extra Driver is already initialized! Existing extraDriver will be closed!");
			extraDriver.quit();
		}
		if (capabilities == null && selenium_host == null) {
			extraDriver = DriverFactory.create(driverName);	
		}
		else {
			extraDriver = DriverFactory.create(driverName, capabilities, selenium_host);
		}
    	
    	if (extraDriver == null ) {
    		Assert.fail("Unable to initialize extra driver: " + driverName + "!");
    	}
		return extraDriver;		
	}
	
	protected WebDriver createExtraDriver(final String driverName) {
		return createExtraDriver(driverName, null, null);
	}

	protected WebDriver getExtraDriver() {
		return extraDriver;
	}

	protected void quitExtraDriver() {
		if (extraDriver != null) {
			extraDriver.quit();
			extraDriver = null;
		}
	}
	
	protected WebDriver getDriver() {
		long threadId = Thread.currentThread().getId();
		WebDriver drv = DriverPool.getDriverByThread(threadId);
		if (drv == null) {
			LOGGER.debug("Unable to find valid driver using threadId: " + threadId);
		}
		return drv;
	}
	 
	protected void setDriver(WebDriver driver) {
		webDrivers.set(driver);
	}
    
	protected static void quitDriver() {
		long threadId = Thread.currentThread().getId();
		WebDriver drv = DriverPool.getDriverByThread(threadId);
		
		try {
			if (drv == null) {
				LOGGER.error("Unable to find valid driver using threadId: " + threadId);
			}

			LOGGER.debug("Driver exiting..." + drv);
	    	DriverPool.deregisterDriverByThread(threadId);
			drv.quit();
	    	LOGGER.debug("Driver exited..." + drv);
		} catch (Exception e) {
    		LOGGER.warn("Error discovered during driver quit: " + e.getMessage());
    		LOGGER.debug("======================================================================================================================================");
		} finally {
    		//TODO analyze how to forcibly kill session on device
	    	webDrivers.remove();
		}
    }
	
	protected void startRecording() {
		if (Configuration.getBoolean(Parameter.VIDEO_RECORDING)) {
			executor.dropFile(SpecialKeywords.VIDEO_FILE_NAME);
			adb_pid = executor.startRecording(SpecialKeywords.VIDEO_FILE_NAME);
		}
	}
	
	protected void stopRecording(String test) {
		if (Configuration.getBoolean(Parameter.VIDEO_RECORDING) && adb_pid != 0) {
			executor.stopRecording(adb_pid); //stop recording
			pause(3); //very often video from device is black. trying to wait before pulling the file
			
			String videoDir = ReportContext.getBaseDir().getAbsolutePath();			
			if (test != null) {
				videoDir = ReportContext.getTestDir(test).getAbsolutePath();
			} 
			
			executor.pullFile(SpecialKeywords.VIDEO_FILE_NAME, videoDir + "/video.mp4");
		}	
	}
	
	protected synchronized boolean initDriver(String name) {
		int maxCount = Configuration.getInt(Parameter.RETRY_COUNT) + 1; //1 - is default run without retry
    	boolean init = false;
    	int count = 0;
    	while (!init & count++ < maxCount) {
    		try {
    			WebDriver drv = DriverFactory.create(name);
    			DriverPool.registerDriver2Thread(drv, Thread.currentThread().getId());
    			driver = drv;
    			setDriver(drv);
    			init = true;
    		}
    		catch (Throwable thr) {
    			LOGGER.error(String.format("Driver initialization '%s' FAILED! Retry %d of %d time - %s", name, count, maxCount, thr.getMessage()));
    			init_throwable = thr;
    		}
    	}

    	return init;
	}
}
