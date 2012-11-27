/**
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.warp.ftest.failure;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.warp.ClientAction;
import org.jboss.arquillian.warp.ServerAssertion;
import org.jboss.arquillian.warp.Warp;
import org.jboss.arquillian.warp.WarpTest;
import org.jboss.arquillian.warp.exception.ServerWarpExecutionException;
import org.jboss.arquillian.warp.ftest.FaviconIgnore;
import org.jboss.arquillian.warp.ftest.TestingServlet;
import org.jboss.arquillian.warp.servlet.BeforeServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;

/**
 * @author Lukas Fryc
 */
@RunWith(Arquillian.class)
@WarpTest
@RunAsClient
public class TestServerAssertionFailurePropagation {

    @Drone
    WebDriver browser;

    @ArquillianResource
    URL contextPath;

    @Deployment
    public static WebArchive createDeployment() {

        return ShrinkWrap
                .create(WebArchive.class, "test.war")
                .addClass(TestingServlet.class)
                .addAsWebResource(new File("src/main/webapp/index.html"))
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test(expected = AssertionError.class)
    public void testAssertionErrorPropagation() {

        Warp
            .execute(new ClientAction() {
                public void action() {
                    browser.navigate().to(contextPath + "index.html");
                }})
            .filter(FaviconIgnore.class)
            .verify(new ServerAssertion() {
                private static final long serialVersionUID = 1L;

                @BeforeServlet
                public void beforeServlet() {
                    fail("AssertionError should be correctly handled and propagated to the client-side");
                }
            }
        );
    }

    @Test
    public void testCheckedExceptionPropagation() {

        try {
            Warp
                .execute(new ClientAction() {
                    public void action() {
                        browser.navigate().to(contextPath + "index.html");
                    }})
                .filter(FaviconIgnore.class)
                .verify(new ServerAssertion() {
                    private static final long serialVersionUID = 1L;

                    @BeforeServlet
                    public void beforeServlet() throws Exception {
                        throw new IOException();
                    }
                }
            );
        } catch (ServerWarpExecutionException e) {
            assertTrue(e.getCause() instanceof IOException);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRuntimeExceptionPropagation() {
        Warp
            .execute(new ClientAction() {
                public void action() {
                    browser.navigate().to(contextPath + "index.html");
                }})
            .filter(FaviconIgnore.class)
            .verify(new ServerAssertion() {
                private static final long serialVersionUID = 1L;

                @BeforeServlet
                public void beforeServlet() {
                    throw new IllegalArgumentException();
                }
            }
        );
    }
}
