package com.wansheng.visitor.oa.mock;
import com.wansheng.visitor.oa.OaAdapter;import java.util.UUID;import org.springframework.context.annotation.Profile;import org.springframework.stereotype.Component;
@Component @Profile("oa-mock") class MockOaAdapter implements OaAdapter{public Submission submit(OaApplication a){return new Submission("MOCK-"+UUID.randomUUID(),"PROCESSING");}}
