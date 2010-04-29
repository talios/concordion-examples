package com.talios;

import org.concordion.api.Resource;
import org.concordion.api.ResultSummary;
import org.concordion.internal.ConcordionBuilder;
import org.testng.Reporter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Unit test for simple App.
 */
@Test
public class AppTest {

    public static final java.lang.String NAMESPACE_TALIOS = "http://www.talios.com/20108/concordion";

    @DataProvider(name = "specificationList")
     public Object[][] specificationList() {
              return new Object[][]{{
                      new SpecificationInfo(AppTest.class, "specifications/Specifications.html")
              }};
    }

    @Test(dataProvider = "specificationList")
       public void specification(SpecificationInfo info) throws IOException {
           Reporter.log("Running concordion specification " + info.getHtmlFilePath() + " against fixture " + info.getFixtureClass());

           Object fixture;
           try {
               fixture = info.getFixtureClass().newInstance();
           } catch (Exception e) {
               throw new RuntimeException("Can't instantiate fixture class: " + info.getFixtureClass(), e);
           }

           System.setProperty("concordion.runner.concordion", BasicConcordionRunner.class.getName());

           ResultSummary resultSummary = new ConcordionBuilder()
                   .withCommand(NAMESPACE_TALIOS, "inline", new InlineCommand())
                   .withCommand(NAMESPACE_TALIOS, "eval", new EvalCommand())
                   .build()
                   .process(new Resource("/" + info.getHtmlFilePath()), fixture);

           ByteArrayOutputStream baos = new ByteArrayOutputStream();
           resultSummary.print(new PrintStream(baos));
           Reporter.log(baos.toString());

           resultSummary.assertIsSatisfied();
       }

        public static class SpecificationInfo {
        public SpecificationInfo(Class fixtureClass, String htmlFile) {
            this.fixtureClass = fixtureClass;
            this.htmlFile = htmlFile;
        }

        public String getHtmlFilePath() {
            return htmlFile;
        }


        public Class getFixtureClass() {
            return fixtureClass;
        }


        private String htmlFile;
        private Class fixtureClass;

        public String toString() {
            return "{htmlFile: " + htmlFile + ", fixtureClass: " + fixtureClass.getName() + "}";
        }
    }
}
