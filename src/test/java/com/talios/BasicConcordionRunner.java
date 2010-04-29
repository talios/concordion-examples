package com.talios;

import org.concordion.api.*;
import org.concordion.internal.ConcordionBuilder;

public class BasicConcordionRunner implements Runner {

    public RunnerResult execute(Resource resource, String href) throws Exception {

        Result result = Result.SUCCESS;
        Resource hrefResource = resource.getParent().getRelativeResource(href);
        String name = hrefResource.getPath().replaceFirst("/", "").replace("/", ".").replaceAll("\\.html$", "");
        Class<?> concordionClass;
        try {
            concordionClass = Class.forName(name);
        } catch (ClassNotFoundException e) {
            try {
                concordionClass = Class.forName(name + "Test");
            }catch (ClassNotFoundException e2) {
                return new RunnerResult(Result.FAILURE);
            }
        }

        ResultSummary resultSummary = new ConcordionBuilder()
                .withCommand(AppTest.NAMESPACE_TALIOS, "inline", new InlineCommand())
                .withCommand(AppTest.NAMESPACE_TALIOS, "eval", new EvalCommand())
                .build()
                .process(hrefResource, concordionClass.newInstance());

        if (resultSummary.getFailureCount() > 0) {
            result = Result.FAILURE;
        }
        if (resultSummary.getExceptionCount() > 0) {
            result = Result.EXCEPTION;
        }

        return new RunnerResult(result);

    }
}
