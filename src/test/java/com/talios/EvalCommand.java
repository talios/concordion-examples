package com.talios;

import org.concordion.api.Element;
import org.concordion.api.Evaluator;
import org.concordion.api.ResultRecorder;
import org.concordion.internal.CommandCall;
import org.concordion.internal.command.AbstractCommand;
import org.concordion.internal.util.Check;

public class EvalCommand extends AbstractCommand {

    @Override
    public void execute(CommandCall commandCall, Evaluator evaluator, ResultRecorder resultRecorder) {
        Check.isFalse(commandCall.hasChildCommands(), "Nesting commands inside an 'eval' is not supported");

        Element element = commandCall.getElement();

        String evalString = element.getText();

        Check.notNull(evalString, "The body must be set for an element containing smx:eval");

        try {
            Object result = evaluator.evaluate(convertToCamel(evalString) + "()");

            if (result != null) {
                Element exampleContainer = new Element("div");
                Element exampleContent = new Element("p");
                Element exampleTitle = new Element("h3");

                element.moveChildrenTo(exampleTitle);
                exampleContainer.appendChild(exampleTitle);
                exampleContainer.appendChild(exampleContent);
                exampleContainer.addStyleClass("example");

                exampleContent.appendText(result.toString());

                element.appendChild(exampleContainer);
            } else {
                element.addStyleClass("success");
            }
        } catch (Exception e) {
            element.addStyleClass("failure");
            throw new RuntimeException(e);
        }
    }


    public String convertToCamel(String s) {
        StringBuilder sb = new StringBuilder();
        String[] parts = s.split(" ");
        for (String part : parts) {
            if (sb.length() == 0) {
                sb.append(part.toLowerCase());
            } else {
                sb.append(part.substring(0, 1).toUpperCase()).append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

}