package com.workoss.dev;


import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.Options;
import com.microsoft.playwright.junit.OptionsFactory;
import com.microsoft.playwright.junit.UsePlaywright;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UsePlaywright(PlaywrightTest.CustomOptions.class)
public class PlaywrightTest {

    public static class CustomOptions implements OptionsFactory {
        @Override
        public Options getOptions() {
            return new Options()
                    .setHeadless(false)
                    .setContextOptions(new Browser.NewContextOptions()
                                              .setBaseURL("https://www.baidu.com"))
                    .setApiRequestOptions(new APIRequest.NewContextOptions()
                                                  .setBaseURL("https://playwright.dev"));
        }
    }

    @Test
    void testWithRequest(Page page, APIRequestContext  request){
        page.navigate("/");
        assertThat(page).hasURL(Pattern.compile("baidu"));

        APIResponse response = request.get("/");
        assertTrue(response.text().contains("Playwright"));
    }
}
