package com.talios;

import nu.xom.Builder;
import org.concordion.api.*;
import org.concordion.internal.CommandCall;
import org.concordion.internal.RunListener;
import org.concordion.internal.command.*;
import org.concordion.internal.runner.DefaultConcordionRunner;
import org.concordion.internal.util.Announcer;
import org.concordion.internal.util.Check;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.Scanner;

public class ScreenshotCommand extends AbstractCommand {

    private Announcer<RunListener> listeners = Announcer.to(RunListener.class);

    public void addRunListener(RunListener runListener) {
        listeners.addListener(runListener);
    }

    public void removeRunListener(RunListener runListener) {
        listeners.removeListener(runListener);
    }

    @Override
    public void execute(CommandCall commandCall, Evaluator evaluator, ResultRecorder resultRecorder) {
        Check.isFalse(commandCall.hasChildCommands(), "Nesting commands inside an 'screenshot' is not supported");

        Element element = commandCall.getElement();
        String href = element.getAttributeValue("href");

        Check.notEmpty(href, "href cannot be null");

        Resource res = commandCall.getResource().getRelativeResource(href);
        final String source = System.getProperty("concordion.output.dir") + res.getPath();

        try {
            saveBackgroundImage(new File(source));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Element imgElement = new Element("img");
        imgElement.addAttribute("src", href);
        imgElement.addAttribute("width", "640");


        element.appendChild(imgElement);

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

    private void saveBackgroundImage(final File output) throws IOException {
        BufferedImage image = getBackgroundImage();
        ImageIO.write(image, "jpg", output);
    }

    public BufferedImage getBackgroundImage() {
        try {
            Robot rbt = new Robot();
            Toolkit tk = Toolkit.getDefaultToolkit();
            Dimension dim = tk.getScreenSize();
            BufferedImage background = rbt.createScreenCapture(new Rectangle(0, 0, (int) dim.getWidth(), (int) dim.getHeight()));
            return background;
            //return new ImageIcon(background);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}