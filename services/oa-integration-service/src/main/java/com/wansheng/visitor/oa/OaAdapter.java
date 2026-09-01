package com.wansheng.visitor.oa;

public interface OaAdapter {
    Submission submit(OaApplication application);
    record OaApplication(String visitId) {}
    record Submission(String externalProcessId, String status) {}
}

