package com.talios;

import nu.xom.Builder;
import org.concordion.api.*;
import org.concordion.internal.CommandCall;
import org.concordion.internal.RunListener;
import org.concordion.internal.command.*;
import org.concordion.internal.runner.DefaultConcordionRunner;
import org.concordion.internal.util.Announcer;
import org.concordion.internal.util.Check;

import java.io.File;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.Scanner;

public class InlineCommand extends AbstractCommand {

    private Announcer<RunListener> listeners = Announcer.to(RunListener.class);

    public void addRunListener(RunListener runListener) {
        listeners.addListener(runListener);
    }

    public void removeRunListener(RunListener runListener) {
        listeners.removeListener(runListener);
    }

    @Override
    public void execute(CommandCall commandCall, Evaluator evaluator, ResultRecorder resultRecorder) {
        Check.isFalse(commandCall.hasChildCommands(), "Nesting commands inside an 'run' is not supported");

        Element element = commandCall.getElement();

        String href = element.getAttributeValue("href");

        Check.notNull(href, "The 'href' attribute must be set for an element containing concordion:run");

        String runnerType = commandCall.getExpression();

        String expression = element.getAttributeValue("concordion:params");
        if (expression != null)
            evaluator.evaluate(expression);

        String concordionRunner = System.getProperty("concordion.runner." + runnerType);

        if (concordionRunner == null && "concordion".equals(runnerType)) {
            concordionRunner = DefaultConcordionRunner.class.getName();
        }
        if (concordionRunner == null) {
            try {
                Class.forName(runnerType);
                concordionRunner = runnerType;
            } catch (ClassNotFoundException e1) {
                // OK, we're reporting this in a second.
            }
        }

        Check.notNull(concordionRunner, "The runner '" + runnerType + "' cannot be found. "
                + "Choices: (1) Use 'concordion' as your runner (2) Ensure that the 'concordion.runner." + runnerType
                + "' System property is set to a name of an org.concordion.Runner implementation "
                + "(3) Specify a full class name of an org.concordion.Runner implementation");
        try {
            Class<?> clazz = Class.forName(concordionRunner);
            Runner runner = (Runner) clazz.newInstance();
            for (Method method : runner.getClass().getMethods()) {
                String methodName = method.getName();
                if (methodName.startsWith("set") && methodName.length() > 3 && method.getParameterTypes().length == 1) {
                    String variableName = methodName.substring(3, 4).toLowerCase() + method.getName().substring(4);
                    Object variableValue = evaluator.evaluate(variableName);
                    if (variableValue == null) {
                        try {
                            variableValue = evaluator.getVariable(variableName);
                        } catch (Exception e) {
                        }
                    }
                    if (variableValue != null) {
                        try {
                            method.invoke(runner, variableValue);
                        } catch (Exception e) {
                        }
                    }
                }
            }
            try {
                Result result = runner.execute(commandCall.getResource(), href).getResult();

                Resource res = commandCall.getResource().getRelativeResource(href);

                final String source = System.getProperty("concordion.output.dir") + res.getPath();

                StringBuilder sb = new StringBuilder();
                Scanner scanner = new Scanner(new File(source));
                while (scanner.hasNextLine()) {
                    sb.append(scanner.nextLine()).append("\n");
                }

                final nu.xom.Element rootElement = new Builder().build(
                        new StringReader(sb.toString())).getRootElement();

                final nu.xom.Element bodyElement = rootElement.getChildElements("body").get(0);

                commandCall.getElement().appendChild(new Element((nu.xom.Element) bodyElement.copy()));

                if (result == Result.SUCCESS) {
                    announceSuccess(element);
                } else if (result == Result.IGNORED) {
                    announceIgnored(element);
                } else {
                    announceFailure(element);
                }
                resultRecorder.record(result);
            } catch (Throwable e) {
                announceFailure(e, element, runnerType);
                resultRecorder.record(Result.FAILURE);
            }
        } catch (Exception e) {
            announceFailure(e, element, runnerType);
            resultRecorder.record(Result.FAILURE);
        }

    }

    private void announceIgnored(Element element) {
        listeners.announce().ignoredReported(new RunIgnoreEvent(element));
    }

    private void announceSuccess(Element element) {
        listeners.announce().successReported(new RunSuccessEvent(element));
    }

    private void announceFailure(Element element) {
        listeners.announce().failureReported(new RunFailureEvent(element));
    }

    private void announceFailure(Throwable throwable, Element element, String expression) {
        listeners.announce().throwableCaught(new ThrowableCaughtEvent(throwable, element, expression));
    }
}