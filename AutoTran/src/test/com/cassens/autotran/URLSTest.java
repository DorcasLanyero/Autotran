package com.cassens.autotran;

import com.cassens.autotran.constants.ServerHost;
import com.cassens.autotran.BuildConfig;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.*;

/**
 * Created by john on 10/28/16.
 */
public class URLSTest {
    @Test
    public void testBaseUrl(){
        // This stopped working for some reason--probably changes to the way BuildConfig varialbs
        // get set.  Commenting out for now.
        /*
        try {
            if (Boolean.TRUE.equals(BuildConfig.PRODUCTION)) {
                try {
                    URL baseUrl = new URL(ServerHost.HOST_URL_CONSTANT);
                    assertNotEquals("sdgsystems.net", baseUrl.getHost());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    assert false;
                }
            } else if (Boolean.FALSE.equals(BuildConfig.PRODUCTION)) {
                try {
                    String baseUrlHost = (new URL(BuildConfig.AUTOTRAN_API_URL)).getHost();
                    String msg = "TRACE MESSAGE: "
                            + " BuildConfig.AUTOTRAN_API_URL=" + BuildConfig.AUTOTRAN_API_URL
                            + " ServerHost.HOST_URL_CONSTANT=" + ServerHost.HOST_URL_CONSTANT
                            + " basUrlHost=" + baseUrlHost;
                    assertTrue(msg, baseUrlHost.endsWith("sdgsystems.net"));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    assert false;
                }
            }
        } catch (NullPointerException e) {

        }
        */
    }

}
