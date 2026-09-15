package com.harborlight.authdemo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Solution walkthrough: "Reading CSP violation reports".
 *
 * Every browser that blocks something under this policy POSTs a report
 * here automatically (that's what report-uri in the policy tells it to
 * do) — no frontend code required. Logging it is enough to see, in
 * production, exactly what a real page tried to do that the policy
 * stopped: is it a genuine attack, or did a legitimate script just not
 * get its nonce?
 *
 * Takes the body as a raw String rather than deserializing into a DTO:
 * browsers send this with Content-Type: application/csp-report (legacy
 * report-uri) or application/reports+json (newer Reporting API) — neither
 * is a media type Jackson's converter recognizes by default, and a report
 * you're just going to log doesn't need strict typing anyway.
 */
@RestController
public class CspReportController {

    private static final Logger log = LoggerFactory.getLogger(CspReportController.class);

    @PostMapping(path = "/csp-reports")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void report(@RequestBody String rawReport) {
        log.warn("CSP violation report received: {}", rawReport);
    }
}
