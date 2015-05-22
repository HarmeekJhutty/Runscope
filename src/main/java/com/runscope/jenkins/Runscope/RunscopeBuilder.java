package com.runscope.jenkins.Runscope;

import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import hudson.Launcher;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * RunscopeBuilder {@link Builder}.
 *
 * @author Harmeek Jhutty
 * @email  hjhutty@redeploy.io
 */
public class RunscopeBuilder extends Builder {

    private static final String SCHEME = "https";
    private static final String API_HOST = "api.runscope.com";
    private static final String RUNSCOPE_HOST = "www.runscope.com";
    private static final String DISPLAY_NAME = "Runscope Configuration";
    private static final String TEST_TRIGGER = "trigger";
    private static final String TEST_RESULTS = "results";
    private static final String TEST_RESULTS_PASS = "pass";
    private static final String TEST_RESULTS_WORKING = "working";
    private static final String TEST_RESULTS_QUEUED = "queued";
	
    private final String triggerEndPoint;
    private final String accessToken;
    private final String bucketKey;
    private String triggerUrl;  
    private int timeout = 60;
    
    public String resp;

    @DataBoundConstructor
    public RunscopeBuilder(String triggerEndPoint, String accessToken, String bucketKey, int timeout) {
		this.triggerEndPoint = triggerEndPoint;
		this.accessToken = accessToken;
		this.bucketKey = bucketKey;
		this.timeout = timeout;
	}

	/**
	 * @return the triggerEndPoint
	 */
	public String getTriggerEndPoint() {
		return triggerEndPoint;
	}
	
	/**
	 * @return the accessToken
	 */
	public String getAccessToken() {
		return accessToken;
	}

	/**
	 * @return the bucketKey
	 */
	public String getBucketKey() {
		return bucketKey;
	}
	
	/**
	 * @return the timeout
	 */
	public Integer getTimeout() {
		return timeout;
	}

    /* 
     * @see hudson.tasks.BuildStepCompatibilityLayer#perform(hudson.model.AbstractBuild, hudson.Launcher, hudson.model.BuildListener)
     */
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
    	
    	PrintStream logger = listener.getLogger();
 
    	logger.println("Test Trigger Configuration:");
    	logger.println("Trigger End Point:" + triggerEndPoint);
    	logger.println("Access Token:" + accessToken);
    	logger.println("Bucket Key:" + bucketKey);
    	
    	String resultsUrl = new RunscopeTrigger(logger, triggerEndPoint, accessToken, timeout, TEST_TRIGGER).process();
        logger.println("Test Results URL:" + resultsUrl);
        
        String apiResultsUrl = resultsUrl.replace(SCHEME + "://" + RUNSCOPE_HOST + "/radar/" + bucketKey, SCHEME + "://" + API_HOST + "/buckets/" + bucketKey + "/radar");
        logger.println("API URL:" + apiResultsUrl);
        
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
            ex.printStackTrace();
        }
        
        boolean flag = true;
            
        while(flag){
        	String res = new RunscopeTrigger(logger, apiResultsUrl, accessToken, timeout, TEST_RESULTS).process();
        	logger.println("Response recieved:" + res);
        	
        	if( TEST_RESULTS_WORKING.equalsIgnoreCase(res) ||  TEST_RESULTS_QUEUED.equalsIgnoreCase(res)  ) {
        		 try {
        	        	TimeUnit.SECONDS.sleep(1);
        	        } catch(InterruptedException ex) {
        	            Thread.currentThread().interrupt();
        	        }
        		flag = true;
        	} else {
        		if(!TEST_RESULTS_PASS.equalsIgnoreCase(res))
                	build.setResult(Result.FAILURE);
        		flag = false;
        	}    	
        }
        
        return true;
    }
    
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link RunscopeBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension 
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        /**
         * This name is used in the configuration screen.
         */
        public String getDisplayName() {
            return DISPLAY_NAME;
        }

    }
}

